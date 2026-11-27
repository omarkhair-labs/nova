"""Small authenticated B2B QA quotation loop. No legal acceptance or payment."""
import hashlib
import json
import uuid
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied, ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import commerce_checkout as checkout, company_services as company
from .commerce_models import CommerceBusinessRFQ, CommerceSKU


def rfq_json(rfq):
    return {
        "id": str(rfq.pk), "company": rfq.company_name,
        "sku": rfq.sku_snapshot, "quantity": rfq.quantity,
        "scope": rfq.scope, "city": rfq.city,
        "status": rfq.status, "quoted_amount": str(rfq.quoted_amount)
            if rfq.quoted_amount is not None else None,
        "quote_note": rfq.quote_note, "created_at": rfq.created_at.isoformat(),
        "accepted_at": rfq.accepted_at.isoformat() if rfq.accepted_at else None,
        "qa_only": True,
        "notice": "Quote enquiry only. No contract, invoice, checkout, payment or installation.",
    }


def signature(data):
    payload = {
        "company_name": data["company_name"].strip(),
        "contact_name": data["contact_name"].strip(),
        "phone": data["phone"].strip(), "email": data.get("email", "").strip().lower(),
        "city": data["city"].strip(), "scope": data["scope"].strip(),
        "sku_id": str(data.get("sku_id")) if data.get("sku_id") else None,
        "quantity": int(data["quantity"]),
    }
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode("utf-8")).hexdigest()


@transaction.atomic
def submit(*, requester, data):
    checkout.require_qa()
    if not requester.is_authenticated or not requester.is_active or requester.is_staff:
        raise PermissionDenied
    if data.get("confirm_qa_no_contract") is not True:
        raise ValidationError("Confirm this is a nonbinding QA enquiry.")
    client_id = uuid.UUID(str(data["client_id"]))
    fingerprint = signature(data)
    get_user_model().objects.select_for_update().get(pk=requester.pk)
    prior = CommerceBusinessRFQ.objects.filter(requester=requester, client_id=client_id).first()
    if prior:
        if prior.fingerprint != fingerprint:
            raise ValidationError("Retry identifier belongs to a different RFQ.")
        return prior
    sku = (get_object_or_404(CommerceSKU, pk=data["sku_id"])
           if data.get("sku_id") else None)
    if sku is not None and not sku.product.qa_listed:
        raise ValidationError("This SKU is not enabled for isolated QA enquiries.")
    rfq = CommerceBusinessRFQ(
        requester=requester, client_id=client_id, fingerprint=fingerprint,
        company_name=data["company_name"].strip(),
        contact_name=data["contact_name"].strip(), phone=data["phone"].strip(),
        email=data.get("email", "").strip().lower(), city=data["city"].strip(),
        sku=sku, sku_snapshot=sku.code if sku else "",
        quantity=int(data["quantity"]), scope=data["scope"].strip(),
    )
    rfq.full_clean()
    rfq.save()
    company.audit(requester, rfq, "New B2B QA quote enquiry, nonbinding")
    return rfq


def customer_list(user):
    checkout.require_qa()
    if not user.is_authenticated or not user.is_active or user.is_staff:
        raise PermissionDenied
    return CommerceBusinessRFQ.objects.filter(requester=user).order_by("-created_at")[:100]


@transaction.atomic
def accept(*, requester, rfq_id):
    checkout.require_qa()
    if not requester.is_authenticated or not requester.is_active or requester.is_staff:
        raise PermissionDenied
    rfq = get_object_or_404(CommerceBusinessRFQ.objects.select_for_update(),
        pk=rfq_id, requester=requester)
    if rfq.status == rfq.Status.ACCEPTED:
        return rfq
    if rfq.status != rfq.Status.QUOTED:
        raise ValidationError("Only a received quote can be acknowledged.")
    rfq.status, rfq.accepted_at = rfq.Status.ACCEPTED, timezone.now()
    rfq.full_clean()
    rfq.save(update_fields=("status", "accepted_at"))
    company.audit(requester, rfq, "Customer acknowledged QA quotation for follow-up; not binding")
    return rfq


def review_state(rfq):
    return {"id": str(rfq.pk), "status": rfq.status, "amount": str(rfq.quoted_amount),
            "note": rfq.quote_note, "request": rfq.scope, "sku": rfq.sku_snapshot,
            "quantity": rfq.quantity}


@transaction.atomic
def staff_reply(*, staff, rfq_id, action, amount, note, token):
    checkout.require_qa()
    company.require(staff, "plants.change_commercebusinessrfq")
    rfq = get_object_or_404(CommerceBusinessRFQ.objects.select_for_update(), pk=rfq_id)
    if rfq.status != rfq.Status.SUBMITTED:
        raise ValidationError("This enquiry was already answered.")
    company.verify_token(staff, "commerce-rfq", rfq, review_state(rfq), token)
    note = (note or "").strip()
    if len(note) > 800:
        raise ValidationError("Reply is too long.")
    if action == "quote":
        if amount is None:
            raise ValidationError("Provide a QA quotation amount.")
        try:
            value = Decimal(str(amount))
        except Exception as exc:
            raise ValidationError("Invalid amount.") from exc
        if not value.is_finite() or not (0 < value < Decimal("1000000000")):
            raise ValidationError("Invalid quote amount.")
        rfq.quoted_amount = value
        rfq.status = rfq.Status.QUOTED
    elif action == "decline":
        rfq.status = rfq.Status.DECLINED
    else:
        raise ValidationError("Choose quote or decline.")
    rfq.quote_note = note
    rfq.replied_by = staff
    rfq.replied_at = timezone.now()
    rfq.full_clean()
    rfq.save(update_fields=("quoted_amount", "status", "quote_note", "replied_by", "replied_at"))
    company.audit(staff, rfq, "B2B QA quotation reply; no binding sale, PO or payment")
    return rfq
