"""G2 QA-gated Plantupia handovers. Raw invitation secrets are never stored.

This is an internal pilot, not a commercial service or an Android QR parser.
Only an identified customer can activate a reviewed installation; QR alone
does not authorize access to private plant records.
"""
import hashlib
import os
import re
import secrets
from datetime import timedelta

from django.core.cache import cache
from django.db import transaction
from django.utils import timezone

from .models import HandoverInvitation, InstallationBatch, Space, SpaceMember


class HandoverError(Exception):
    def __init__(self, code, http_status):
        self.code = code
        self.http_status = http_status
        super().__init__(code)


def pilot_enabled():
    # Explicit opt-in for isolated non-production QA. No default activation.
    return os.getenv("PLANTUPIA_HANDOVER_QA_ENABLED", "0") == "1"


def _digest(raw_token):
    if not isinstance(raw_token, str) or not re.fullmatch(r"[A-Za-z0-9_-]{40,160}", raw_token):
        raise HandoverError("invalid_invitation", 404)
    return hashlib.sha256(raw_token.encode("ascii")).hexdigest()


def _nonstaff_extra_members(batch, recipient):
    return SpaceMember.objects.filter(
        space_id=batch.space_id, user__is_staff=False,
    ).exclude(user_id=recipient.pk).exists()


@transaction.atomic
def issue_pilot_invitation(*, batch_id, recipient, staff, mode, valid_for=timedelta(hours=24)):
    """Explicit staff operation. Return raw token ONCE to a secure delivery channel.

    Caller is responsible for confirming actual commercial ownership mode.
    No token is generated while the isolated handover flag is off.
    """
    if not pilot_enabled():
        raise HandoverError("handover_disabled", 503)
    if not staff.is_authenticated or not staff.is_staff:
        raise HandoverError("staff_only", 403)
    if not recipient.is_active or recipient.is_staff or recipient.pk == staff.pk:
        raise HandoverError("invalid_recipient", 400)
    if mode not in HandoverInvitation.Mode.values:
        raise HandoverError("invalid_handover_mode", 400)
    if not timedelta(minutes=1) <= valid_for <= timedelta(days=7):
        raise HandoverError("invalid_expiration", 400)

    batch = InstallationBatch.objects.select_for_update().select_related(
        "space__owner", "prepared_by",
    ).get(pk=batch_id)
    if batch.status != InstallationBatch.Status.READY or not batch.space.owner.is_staff:
        raise HandoverError("inventory_not_ready", 409)
    if not batch.manifest_rows.exists() or batch.manifest_rows.exclude(
        plant__space_id=batch.space_id,
    ).exists():
        raise HandoverError("inventory_not_ready", 409)
    if _nonstaff_extra_members(batch, recipient):
        raise HandoverError("unexpected_membership", 409)
    now = timezone.now()
    HandoverInvitation.objects.filter(
        batch=batch, claimed_at__isnull=True, revoked_at__isnull=True,
    ).update(revoked_at=now)
    raw_token = secrets.token_urlsafe(32)
    invitation = HandoverInvitation(
        batch=batch, intended_recipient=recipient, issued_by=staff,
        token_digest=_digest(raw_token), mode=mode, expires_at=now + valid_for,
    )
    invitation.full_clean()
    invitation.save()
    return raw_token, invitation


def throttle_account(user):
    """Best-effort account-scoped throttle, including invalid tokens."""
    key = f"plantupia-handover-attempts:{user.pk}"
    if cache.add(key, 1, timeout=60):
        return
    try:
        count = cache.incr(key)
    except ValueError:
        cache.set(key, 1, timeout=60)
        count = 1
    if count > 30:
        raise HandoverError("try_again_later", 429)


def _load_valid_invitation(token, user, *, for_update=False):
    digest = _digest(token)
    qs = HandoverInvitation.objects.select_related(
        "batch__space", "intended_recipient",
    )
    if for_update:
        qs = qs.select_for_update()
    invitation = qs.filter(token_digest=digest).first()
    # Do not distinguish strangers, nonexistent tokens and revoked tokens.
    if (invitation is None or invitation.intended_recipient_id != user.pk
            or invitation.revoked_at is not None):
        raise HandoverError("invalid_invitation", 404)
    if invitation.claimed_at is None and invitation.expires_at <= timezone.now():
        raise HandoverError("invitation_expired", 410)
    return invitation


def _summary(invitation, *, claimed):
    batch = invitation.batch
    return {
        "state": "claimed" if claimed else "ready",
        "plant_count": batch.manifest_rows.count(),
        "space_label": batch.space.name,
        "ownership_mode": invitation.mode,
    }


def preview_handover(*, token, user):
    if not pilot_enabled():
        raise HandoverError("handover_disabled", 503)
    throttle_account(user)
    invitation = _load_valid_invitation(token, user)
    batch = invitation.batch
    if invitation.claimed_at is None and batch.status != InstallationBatch.Status.READY:
        raise HandoverError("invalid_invitation", 404)
    if invitation.claimed_at is not None and invitation.claimed_by_id != user.pk:
        raise HandoverError("invalid_invitation", 404)
    return _summary(invitation, claimed=invitation.claimed_at is not None)


@transaction.atomic
def claim_handover(*, token, user):
    if not pilot_enabled():
        raise HandoverError("handover_disabled", 503)
    throttle_account(user)
    invitation = _load_valid_invitation(token, user, for_update=True)
    batch = InstallationBatch.objects.select_for_update().select_related(
        "space__owner",
    ).get(pk=invitation.batch_id)
    space = Space.objects.select_for_update().get(pk=batch.space_id)

    if invitation.claimed_at is not None:
        # No privilege resurrection after a customer later loses access.
        permitted = (
            space.owner_id == user.pk if invitation.mode == HandoverInvitation.Mode.CUSTOMER_OWNED
            else SpaceMember.objects.filter(space=space, user=user).exists()
        )
        if (invitation.claimed_by_id != user.pk or not permitted
                or batch.status != InstallationBatch.Status.CLAIMED):
            raise HandoverError("claim_not_available", 409)
        return {"space_id": str(space.pk), **_summary(invitation, claimed=True)}

    if (batch.status != InstallationBatch.Status.READY or not space.owner.is_staff
            or not batch.manifest_rows.exists()
            or batch.manifest_rows.exclude(plant__space_id=space.pk).exists()
            or _nonstaff_extra_members(batch, user)):
        raise HandoverError("claim_not_available", 409)

    # Two *explicit* pilot modes; legal ownership still requires company approval.
    if invitation.mode == HandoverInvitation.Mode.CUSTOMER_OWNED:
        SpaceMember.objects.filter(space=space).delete()
        space.owner = user
        space.save(update_fields=("owner",))
    else:
        SpaceMember.objects.update_or_create(
            space=space, user=user, defaults={"role": SpaceMember.Role.CARER},
        )
    now = timezone.now()
    invitation.claimed_at = now
    invitation.claimed_by = user
    invitation.save(update_fields=("claimed_at", "claimed_by"))
    batch.status = InstallationBatch.Status.CLAIMED
    batch.save(update_fields=("status",))
    # Any older invitation for this batch becomes unusable.
    HandoverInvitation.objects.filter(
        batch=batch, claimed_at__isnull=True, revoked_at__isnull=True,
    ).exclude(pk=invitation.pk).update(revoked_at=now)
    return {"space_id": str(space.pk), **_summary(invitation, claimed=True)}
