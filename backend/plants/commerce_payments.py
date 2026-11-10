"""Manual payment reconciliation for the genuine Plantupia customer order.

A customer's transfer reference is an unverified claim. Only an authorized
finance reviewer may attest an independent bank settlement or a delivered
COD cash receipt. There is no payment gateway or automatic bank integration.
"""
from decimal import Decimal, InvalidOperation

from django.core.exceptions import ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404

from . import company_services as company
from .commerce_models import (
    CommerceOrder, CommerceTransferClaim, CommercePaymentRecord,
    CommerceRealOrderEvent,
)
from .commerce_storefront import customer_only


@transaction.atomic
def claim_manual_transfer(*, user, order_id, reference):
    customer_only(user)
    reference = (reference or "").strip()
    if not 4 <= len(reference) <= 120:
        raise ValidationError("Enter the transfer reference exactly as shown on your bank/InstaPay receipt.")
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id, customer=user)
    if order.payment_preference != CommerceOrder.PaymentPreference.MANUAL_TRANSFER:
        raise ValidationError("This order was not configured for manual bank/InstaPay transfer.")
    if order.status not in (
        CommerceOrder.Status.CONFIRMED, CommerceOrder.Status.PREPARING,
        CommerceOrder.Status.DISPATCHED, CommerceOrder.Status.DELIVERED,
    ) or order.payment_status != CommerceOrder.PaymentStatus.UNPAID:
        raise ValidationError("A transfer reference can only be recorded for an unpaid confirmed order.")
    existing = CommerceTransferClaim.objects.filter(order=order).first()
    if existing:
        if existing.reference != reference:
            raise ValidationError("This order already has a different transfer reference; contact company support.")
        return order
    CommerceTransferClaim.objects.create(order=order, claimed_by=user, reference=reference)
    CommerceRealOrderEvent.objects.create(
        order=order, actor=user, kind="transfer_claimed",
        label="You supplied a transfer reference; Plantupia has not verified payment yet.",
    )
    company.audit(user, order, "Customer claimed manual transfer; no payment verified or bank reconciliation inferred.")
    return order


@transaction.atomic
def verify_payment(*, staff, order_id, amount, settlement_reference, evidence_note):
    company.require(staff, "plants.add_commercepaymentrecord")
    company.require(staff, "plants.change_commerceorder")
    try:
        paid = Decimal(str(amount))
    except (InvalidOperation, ValueError, TypeError) as exc:
        raise ValidationError("Provide the independently matched exact EGP settlement amount.") from exc
    settlement_reference = (settlement_reference or "").strip()
    evidence_note = (evidence_note or "").strip()
    if not paid.is_finite() or paid <= 0 or paid.as_tuple().exponent < -2:
        raise ValidationError("A valid positive EGP amount (maximum two decimals) is required.")
    if not 6 <= len(settlement_reference) <= 120:
        raise ValidationError("Record the actual bank statement or COD cash-receipt reference.")
    if not 20 <= len(evidence_note) <= 500:
        raise ValidationError("Explain the independently reconciled evidence in 20–500 characters.")
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    current = CommercePaymentRecord.objects.filter(order=order).first()
    if current:
        if (current.amount == paid and current.settlement_reference == settlement_reference
                and current.evidence_note == evidence_note):
            return order
        raise ValidationError("This order already has a verified settlement with different evidence.")
    if order.payment_status != CommerceOrder.PaymentStatus.UNPAID:
        raise ValidationError("Only an unpaid order may receive its first verified settlement.")
    if order.shipping_fee is None or paid != order.subtotal + order.shipping_fee:
        raise ValidationError("Settlement must match the customer's accepted exact total including shipping.")
    if order.status not in (
        CommerceOrder.Status.CONFIRMED, CommerceOrder.Status.PREPARING,
        CommerceOrder.Status.DISPATCHED, CommerceOrder.Status.DELIVERED,
    ):
        raise ValidationError("Only a customer-confirmed commercial order may be settled.")
    if order.payment_preference == CommerceOrder.PaymentPreference.COD:
        if order.status != CommerceOrder.Status.DELIVERED:
            raise ValidationError("COD may be verified only after delivery evidence is recorded.")
    elif order.payment_preference == CommerceOrder.PaymentPreference.MANUAL_TRANSFER:
        if not CommerceTransferClaim.objects.filter(order=order).exists():
            raise ValidationError("Customer must first submit a transfer reference for independent matching.")
    else:
        raise ValidationError("Unsupported payment preference.")
    CommercePaymentRecord.objects.create(
        order=order, method=order.payment_preference, amount=paid,
        settlement_reference=settlement_reference, evidence_note=evidence_note,
        verified_by=staff,
    )
    order.payment_status = CommerceOrder.PaymentStatus.VERIFIED
    order.save(update_fields=["payment_status", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="payment_verified",
        label="Plantupia finance recorded verification of the exact order payment.",
    )
    company.audit(staff, order, "Authorized finance reviewer matched exact EGP settlement against independent company records.")
    return order
