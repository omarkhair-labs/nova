"""Real Plantupia B2B inquiries and quoted follow-up, never QA or a sale.

A quote accepted for follow-up is not a signed purchase order, payment,
inventory reservation, installation or QR handover.
"""
import hashlib
import json
import uuid
from decimal import Decimal, InvalidOperation

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company
from .commerce_models import CommerceBusinessInquiry, CommerceSKU
from .commerce_storefront import customer_only, listed_skus


def inquiry_json(record):
    return {
        "id": str(record.pk),
        "kind": record.kind,
        "company_name": record.company_name,
        "contact_name": record.contact_name,
        "phone": record.phone,
        "email": record.email,
        "city": record.city,
        "sku": record.sku_code_snapshot or None,
        "quantity": record.quantity,
        "scope": record.scope,
        "status": record.status,
        "quoted_amount": str(record.quoted_amount) if record.quoted_amount is not None else None,
        "currency": "EGP",
        "quote_note": record.quote_note,
        "quote_version": record.quote_version,
        "created_at": record.created_at.isoformat(),
        "quoted_at": record.quoted_at.isoformat() if record.quoted_at else None,
        "accepted_at": record.accepted_at.isoformat() if record.accepted_at else None,
        "notice": "Request/quote for commercial follow-up only; not a confirmed purchase order, payment or installation.",
    }


def customer_inquiries(user):
    customer_only(user)
    return CommerceBusinessInquiry.objects.filter(requester=user).order_by("-created_at", "id")[:100]


@transaction.atomic
def submit_inquiry(*, user, data):
    customer_only(user)
    identity = uuid.UUID(str(data["client_id"]))
    raw = {
        key: str(data.get(key, "")).strip()
        for key in (
            "kind", "company_name", "contact_name", "phone", "email",
            "city", "sku_id", "quantity", "scope",
        )
    }
    fingerprint = hashlib.sha256(
        json.dumps(raw, sort_keys=True, ensure_ascii=False).encode("utf-8")
    ).hexdigest()
    get_user_model().objects.select_for_update().get(pk=user.pk)
    prior = CommerceBusinessInquiry.objects.filter(requester=user, client_id=identity).first()
    if prior:
        if prior.fingerprint != fingerprint:
            raise ValidationError("This request ID was already used for different business details.")
        return prior
    sku = None
    if data.get("sku_id"):
        sku = get_object_or_404(listed_skus(), pk=data["sku_id"])
    enquiry = CommerceBusinessInquiry(
        requester=user, client_id=identity, fingerprint=fingerprint,
        kind=data["kind"], company_name=data["company_name"].strip(),
        contact_name=data["contact_name"].strip(), phone=data["phone"].strip(),
        email=data.get("email", "").strip(), city=data["city"].strip(),
        sku=sku, sku_code_snapshot=sku.code if sku else "",
        quantity=data["quantity"], scope=data["scope"].strip(),
    )
    enquiry.full_clean()
    enquiry.save()
    company.audit(user, enquiry, "Customer filed real nonbinding B2B enquiry; no sale, stock or installation created.")
    return enquiry


@transaction.atomic
def quote_inquiry(*, staff, inquiry_id, amount, note, version):
    company.require(staff, "plants.change_commercebusinessinquiry")
    try:
        value = Decimal(str(amount))
        expected = int(version)
    except (InvalidOperation, ValueError, TypeError) as exc:
        raise ValidationError("Provide a valid quote amount and revision.") from exc
    note = (note or "").strip()
    if not value.is_finite() or value <= 0 or value > Decimal("9999999999.99") or value.as_tuple().exponent < -2:
        raise ValidationError("A positive exact EGP quote with at most two decimal places is required.")
    if not 15 <= len(note) <= 800:
        raise ValidationError("Quote scope, inclusion and exclusions must be described (15–800 characters).")
    enquiry = get_object_or_404(CommerceBusinessInquiry.objects.select_for_update(), pk=inquiry_id)
    if enquiry.status not in (CommerceBusinessInquiry.Status.RECEIVED, CommerceBusinessInquiry.Status.QUOTED):
        raise ValidationError("Only an open enquiry may be quoted or revised.")
    if enquiry.quote_version != expected:
        raise ValidationError("Quote revision changed; reload this request.")
    enquiry.quoted_amount = value
    enquiry.quote_note = note
    enquiry.quote_version += 1
    enquiry.status = CommerceBusinessInquiry.Status.QUOTED
    enquiry.quoted_by = staff
    enquiry.quoted_at = timezone.now()
    enquiry.save(update_fields=[
        "quoted_amount", "quote_note", "quote_version", "status",
        "quoted_by", "quoted_at", "updated_at",
    ])
    company.audit(staff, enquiry, "Staff recorded actual nonbinding EGP business quote; no order or stock.")
    return enquiry


@transaction.atomic
def accept_quote(*, user, inquiry_id, version):
    customer_only(user)
    enquiry = get_object_or_404(
        CommerceBusinessInquiry.objects.select_for_update(), pk=inquiry_id, requester=user,
    )
    if version != enquiry.quote_version:
        raise ValidationError("The quote changed. Review the latest version.")
    if enquiry.status == CommerceBusinessInquiry.Status.ACCEPTED:
        return enquiry
    if enquiry.status != CommerceBusinessInquiry.Status.QUOTED or enquiry.quoted_amount is None:
        raise ValidationError("No current company quote is available for acceptance.")
    enquiry.status = CommerceBusinessInquiry.Status.ACCEPTED
    enquiry.accepted_at = timezone.now()
    enquiry.save(update_fields=["status", "accepted_at", "updated_at"])
    company.audit(user, enquiry, "Customer accepted B2B nonbinding quote for commercial follow-up only.")
    return enquiry


@transaction.atomic
def decline_inquiry(*, staff, inquiry_id):
    company.require(staff, "plants.change_commercebusinessinquiry")
    enquiry = get_object_or_404(CommerceBusinessInquiry.objects.select_for_update(), pk=inquiry_id)
    if enquiry.status == CommerceBusinessInquiry.Status.DECLINED:
        return enquiry
    if enquiry.status not in (CommerceBusinessInquiry.Status.RECEIVED, CommerceBusinessInquiry.Status.QUOTED):
        raise ValidationError("Only an open business enquiry may be declined.")
    enquiry.status = CommerceBusinessInquiry.Status.DECLINED
    enquiry.save(update_fields=["status", "updated_at"])
    company.audit(staff, enquiry, "Company declined business enquiry; no purchase order or stock movement.")
    return enquiry
