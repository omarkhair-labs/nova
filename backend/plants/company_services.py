"""Company portal boundaries and explicit review operations. No customer API here."""
from django.conf import settings
from django.contrib.admin.models import CHANGE, LogEntry
from django.contrib.contenttypes.models import ContentType
from django.core import signing
from django.core.exceptions import PermissionDenied
from django.db import transaction
from django.db.models import Q
from django.utils import timezone

from .handovers import HandoverError, issue_pilot_invitation, pilot_enabled
from .inventory_import import ImportProblem, digest
from .models import BotanicalSpecies, CareGuideVersion, HandoverInvitation, InstallationBatch, Space

ACCESS = ("plants.access_company_dashboard", "plants.view_installationbatch")
PUBLISH = ("plants.change_careguideversion", "plants.publish_careguideversion")


def enabled():
    if not settings.PLANTUPIA_COMPANY_DASHBOARD_ENABLED:
        return False
    database = settings.DATABASES["default"]
    local = (not settings.IS_PRODUCTION and settings.ENVIRONMENT == "development" and (
        database.get("ENGINE") == "django.db.backends.sqlite3" or (
            database.get("ENGINE") == "django.db.backends.postgresql"
            and database.get("HOST") in ("localhost", "127.0.0.1", "::1"))))
    return local or (settings.IS_PRODUCTION and pilot_enabled())


def require(user, *permissions):
    if not user.is_authenticated or not user.is_active or not user.is_staff or not user.has_perms((*ACCESS, *permissions)):
        raise PermissionDenied


def batches(user):
    qs = InstallationBatch.objects.filter(Q(status="claimed") | Q(space__owner__is_staff=True))
    if not user.is_superuser:
        qs = qs.filter(prepared_by=user).filter(Q(status="claimed") | Q(space__owner=user))
    return qs


def inventory_visible(user, batch):
    # A delivery audit record never restores access to a customer's private plants.
    return batch.space.owner.is_staff and (user.is_superuser or batch.space.owner_id == user.pk)


def require_draft(user, batch):
    require(user)
    if not batches(user).filter(pk=batch.pk).exists() or not inventory_visible(user, batch):
        raise PermissionDenied
    if batch.status != "draft":
        raise ImportProblem("Reviewed inventory is locked. Only DRAFT installations can be changed.")


def audit(user, obj, message):
    LogEntry.objects.create(user_id=user.pk, content_type=ContentType.objects.get_for_model(obj),
        object_id=str(obj.pk), object_repr=f"Company {obj._meta.model_name} {obj.pk}",
        action_flag=CHANGE, change_message=message)


def action_token(user, kind, obj, state):
    return signing.dumps({"actor": user.pk, "kind": kind, "id": str(obj.pk), "state": digest(state)}, salt="plants.company.action.v1", compress=True)


def verify_token(user, kind, obj, state, token):
    try:
        expected = signing.loads(token, salt="plants.company.action.v1", max_age=1800)
    except signing.BadSignature as exc:
        raise ImportProblem("This review expired or is invalid. Reload and review the current record.") from exc
    if expected != {"actor": user.pk, "kind": kind, "id": str(obj.pk), "state": digest(state)}:
        raise ImportProblem("The record changed since your review. Reload before confirming.")


def inventory_state(batch):
    return {"batch": list(InstallationBatch.objects.filter(pk=batch.pk).values()),
            "space": list(Space.objects.filter(pk=batch.space_id).values()),
            "manifest": list(batch.manifest_rows.order_by("pk").values()),
            "plants": list(batch.space.plants.order_by("pk").values())}


def guide_state(guide):
    return list(CareGuideVersion.objects.filter(pk=guide.pk).values())


def invitation_state(batch):
    return list(batch.invitations.order_by("pk").values("id", "mode", "intended_recipient_id", "expires_at", "revoked_at", "claimed_at"))


@transaction.atomic
def review_installation(user, batch_id, token):
    require(user, "plants.change_installationbatch", "plants.view_plantunit")
    batch = InstallationBatch.objects.select_for_update().get(pk=batch_id)
    batch.space = Space.objects.select_for_update().select_related("owner").get(pk=batch.space_id)
    require_draft(user, batch)
    list(batch.space.plants.select_for_update().order_by("pk"))
    verify_token(user, "ready", batch, inventory_state(batch), token)
    rows = batch.manifest_rows
    if (not rows.exists() or rows.exclude(plant__space_id=batch.space_id).exists()
            or rows.count() != batch.space.plants.count()):
        raise ImportProblem("Every plant in this exact Space must appear once in this installation's manifest before READY.")
    batch.status, batch.reviewed_by, batch.reviewed_at = "ready", user, timezone.now()
    batch.full_clean()
    batch.save(update_fields=("status", "reviewed_by", "reviewed_at"))
    audit(user, batch, "Company dashboard: complete inventory reviewed and marked READY")
    return batch


@transaction.atomic
def publish_guide(user, guide_id, token, *, withdraw=False):
    require(user, *PUBLISH)
    header = CareGuideVersion.objects.get(pk=guide_id)
    BotanicalSpecies.objects.select_for_update().get(pk=header.species_id)
    guide = CareGuideVersion.objects.select_for_update().get(pk=guide_id)
    verify_token(user, "guide", guide, guide_state(guide), token)
    if guide.assigned_reviewer_id and guide.assigned_reviewer_id != user.pk and not user.is_superuser:
        raise PermissionDenied
    if withdraw:
        guide.published = False
    else:
        if not guide.ever_published:
            guide.reviewer, guide.reviewed_at = user, timezone.now()
        guide.published = True
    guide.save()
    audit(user, guide, "Company dashboard: publication withdrawn" if withdraw else "Company dashboard: staff confirmed source/content review and published")
    return guide


@transaction.atomic
def issue_invitation(user, batch_id, recipient, mode, token, *, replace=False):
    require(user, "plants.add_handoverinvitation", "plants.view_handoverinvitation")
    batch = InstallationBatch.objects.select_for_update().get(pk=batch_id)
    batch.space = Space.objects.select_for_update().select_related("owner").get(pk=batch.space_id)
    if not batches(user).filter(pk=batch.pk).exists() or not inventory_visible(user, batch):
        raise PermissionDenied
    verify_token(user, "issue", batch, invitation_state(batch), token)
    if batch.invitations.filter(claimed_at__isnull=True, revoked_at__isnull=True).exists() and not replace:
        raise ImportProblem("An unused invitation exists. Explicitly confirm replacement to revoke it and issue a new code.")
    raw, invitation = issue_pilot_invitation(batch_id=batch.pk, recipient=recipient, staff=user, mode=mode)
    audit(user, invitation, "Company dashboard: recipient-bound QA invitation issued; secret withheld")
    return raw, invitation


@transaction.atomic
def revoke_invitation(user, invitation_id, token):
    require(user, "plants.change_handoverinvitation", "plants.view_handoverinvitation")
    if not pilot_enabled():
        raise HandoverError("handover_disabled", 503)
    header = HandoverInvitation.objects.get(pk=invitation_id)
    batch = InstallationBatch.objects.select_for_update().get(pk=header.batch_id)
    if not batches(user).filter(pk=batch.pk).exists():
        raise PermissionDenied
    invitation = HandoverInvitation.objects.select_for_update().get(pk=invitation_id)
    verify_token(user, "revoke", invitation, invitation_state(batch), token)
    if invitation.claimed_at:
        raise ImportProblem("Claimed handovers cannot be revoked through this workflow.")
    if not invitation.revoked_at:
        invitation.revoked_at = timezone.now()
        invitation.save(update_fields=("revoked_at",))
        audit(user, invitation, "Company dashboard: unused QA invitation revoked")
