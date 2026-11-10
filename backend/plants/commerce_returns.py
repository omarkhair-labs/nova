"""Actual order return intake and external refund verification.

A return request is not authorization, physical receipt is not a restock,
and finance refund verification is not an automatic bank payout.
"""
from decimal import Decimal, InvalidOperation

from django.core.exceptions import ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company
from .commerce_models import (
    CommerceOrder, CommerceReturnCase, CommerceRefundRecord,
    CommercePaymentRecord, CommerceRealOrderEvent,
)
from .commerce_storefront import customer_only

REASONS = ("damaged", "incorrect_item", "quality_issue", "other")


def _required(value, name, lo, hi):
    value = (value or "").strip()
    if not lo <= len(value) <= hi:
        raise ValidationError(f"{name} must be {lo}–{hi} characters.")
    return value


@transaction.atomic
def request_return(*, user, order_id, reason, note):
    customer_only(user)
    if reason not in REASONS:
        raise ValidationError("Choose an actual return reason.")
    note = _required(note, "Return description", 10, 500)
    order = get_object_or_404(CommerceOrder.objects.select_for_update(),
                              pk=order_id, customer=user)
    existing = CommerceReturnCase.objects.filter(order=order).first()
    if existing:
        if existing.reason != reason or existing.customer_note != note:
            raise ValidationError("A return case already exists for this order. Contact company support.")
        return order
    if order.status != CommerceOrder.Status.DELIVERED:
        raise ValidationError("Returns are available only after a delivery is recorded.")
    CommerceReturnCase.objects.create(
        order=order, reason=reason, customer_note=note, requested_by=user,
    )
    order.status = CommerceOrder.Status.RETURN_REQUESTED
    order.last_status_at = timezone.now()
    order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=user, kind="return_requested",
        label="You submitted a return request; the company has not approved it yet.",
    )
    company.audit(user, order, "Customer requested a real delivered-order return; no stock or refund action.")
    return order


@transaction.atomic
def decide_return(*, staff, order_id, action, note):
    company.require(staff, "plants.change_commerceorder")
    if action not in ("approve", "decline"):
        raise ValidationError("Choose approve or decline.")
    note = _required(note, "Return decision note", 8, 500)
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    case = get_object_or_404(CommerceReturnCase.objects.select_for_update(), order=order)
    wanted = (CommerceReturnCase.Decision.APPROVED if action == "approve"
              else CommerceReturnCase.Decision.DECLINED)
    if case.decision == wanted:
        if case.staff_note != note:
            raise ValidationError("This return decision was already recorded with different evidence.")
        return order
    if case.decision != CommerceReturnCase.Decision.REQUESTED or order.status != CommerceOrder.Status.RETURN_REQUESTED:
        raise ValidationError("Only a new return request can be reviewed.")
    case.decision = wanted
    case.staff_note = note
    case.reviewed_by = staff
    case.reviewed_at = timezone.now()
    case.save(update_fields=["decision", "staff_note", "reviewed_by", "reviewed_at"])
    if action == "decline":
        order.status = CommerceOrder.Status.DELIVERED
        order.last_status_at = timezone.now()
        order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind=("return_approved" if action == "approve" else "return_declined"),
        label=("Plantupia approved the return for physical intake." if action == "approve"
               else "Plantupia declined the return request; contact support to appeal."),
    )
    company.audit(staff, case, f"Operator {action}d return case; no stock or refund inferred.")
    return order


@transaction.atomic
def receive_return(*, staff, order_id, evidence_reference):
    company.require(staff, "plants.change_commerceorder")
    ref = _required(evidence_reference, "Physical return receipt reference", 8, 180)
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    case = get_object_or_404(CommerceReturnCase.objects.select_for_update(), order=order)
    if case.decision == CommerceReturnCase.Decision.RECEIVED:
        if case.received_evidence_reference != ref:
            raise ValidationError("A different physical receipt was already recorded.")
        return order
    if case.decision != CommerceReturnCase.Decision.APPROVED or order.status != CommerceOrder.Status.RETURN_REQUESTED:
        raise ValidationError("Approve the return before recording physical receipt.")
    case.decision = CommerceReturnCase.Decision.RECEIVED
    case.received_evidence_reference = ref
    case.received_at = timezone.now()
    case.save(update_fields=["decision", "received_evidence_reference", "received_at"])
    order.status = CommerceOrder.Status.RETURNED
    order.last_status_at = case.received_at
    order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="return_received",
        label="Plantupia recorded physical receipt of the returned items.",
    )
    company.audit(staff, case, "Physical return received; no stock automatically restocked, no refund inferred.")
    return order


@transaction.atomic
def verify_external_refund(*, staff, order_id, amount, reference, note):
    company.require(staff, "plants.add_commercerefundrecord")
    company.require(staff, "plants.change_commerceorder")
    try:
        paid = Decimal(str(amount))
    except (InvalidOperation, ValueError, TypeError) as exc:
        raise ValidationError("Provide the actual matched external EGP refund amount.") from exc
    reference = _required(reference, "External refund statement reference", 6, 120)
    note = _required(note, "Independent refund evidence", 20, 500)
    if not paid.is_finite() or paid <= 0 or paid.as_tuple().exponent < -2:
        raise ValidationError("A positive EGP amount with at most two decimals is required.")
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    previous = CommerceRefundRecord.objects.filter(order=order).first()
    if previous:
        if previous.amount != paid or previous.refund_reference != reference or previous.evidence_note != note:
            raise ValidationError("This order has a different previously verified refund.")
        return order
    if order.status != CommerceOrder.Status.RETURNED or order.payment_status != CommerceOrder.PaymentStatus.VERIFIED:
        raise ValidationError("Physical return and previously verified payment are required before any refund verification.")
    payment = get_object_or_404(CommercePaymentRecord, order=order)
    if paid != payment.amount:
        raise ValidationError("This full-order return requires a refund matching the exact prior verified payment.")
    CommerceRefundRecord.objects.create(
        order=order, amount=paid, refund_reference=reference,
        evidence_note=note, verified_by=staff,
    )
    order.payment_status = CommerceOrder.PaymentStatus.REFUNDED
    order.save(update_fields=["payment_status", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="refund_verified",
        label="Plantupia finance recorded verification of an external refund.",
    )
    company.audit(staff, order, "Authorized reviewer matched external refund reference; no gateway payout triggered.")
    return order
