"""Evidence-based operator fulfillment of real CommerceOrder only.

Dispatch consumes precisely the previously reserved location stock in one
transaction. This module records operator-attested references; it does not
invent a courier integration, proof document, collected cash or a refund.
"""
import hashlib
import json
import uuid
from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import Sum
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company
from .commerce_models import (
    CommerceOrder, CommerceOrderLine, CommerceRealOrderEvent,
    CommerceFulfillmentReceipt, CommerceSKU, CommerceStockLocation,
    CommerceStockMovement, CommerceStockReservation,
)


def _required(value, label, max_length):
    value = (value or "").strip()
    if not value or len(value) > max_length:
        raise ValidationError(f"{label} is required (maximum {max_length} characters).")
    return value


@transaction.atomic
def start_preparation(*, staff, order_id):
    company.require(staff, "plants.change_commerceorder")
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    if order.status == CommerceOrder.Status.PREPARING:
        return order
    if order.status != CommerceOrder.Status.CONFIRMED:
        raise ValidationError("Only a customer-confirmed order can enter preparation.")
    if not CommerceStockReservation.objects.filter(
        order=order, released_at__isnull=True
    ).exists():
        raise ValidationError("Confirmed order has no active warehouse holds.")
    order.status = CommerceOrder.Status.PREPARING
    order.last_status_at = timezone.now()
    order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="preparing",
        label="Plantupia started preparing the reserved items.",
    )
    company.audit(staff, order, "Operator started preparation; warehouse holds still active.")
    return order


@transaction.atomic
def dispatch_order(*, staff, order_id, carrier_name, tracking_reference, dispatch_note):
    company.require(staff, "plants.change_commerceorder")
    carrier = _required(carrier_name, "Carrier name", 120)
    tracking = _required(tracking_reference, "Handover/tracking reference", 120)
    note = _required(dispatch_note, "Physical handover evidence note", 300)
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    if order.status in (CommerceOrder.Status.DISPATCHED, CommerceOrder.Status.DELIVERED):
        receipt = CommerceFulfillmentReceipt.objects.get(order=order)
        if receipt.carrier_name != carrier or receipt.tracking_reference != tracking or receipt.dispatch_note != note:
            raise ValidationError("This order was already dispatched using different evidence.")
        return order
    if order.status != CommerceOrder.Status.PREPARING:
        raise ValidationError("Prepare and verify the customer-confirmed order before dispatch.")

    held = list(CommerceStockReservation.objects.select_for_update().filter(
        order=order, released_at__isnull=True,
    ).order_by("sku_id", "location_id"))
    if not held:
        raise ValidationError("No reserved warehouse units to dispatch.")
    lines = list(CommerceOrderLine.objects.filter(order=order))
    expected = {line.sku_id: line.quantity for line in lines}
    actual = {}
    for reservation in held:
        actual[reservation.sku_id] = actual.get(reservation.sku_id, 0) + reservation.quantity
    if actual != expected:
        raise ValidationError("Active stock reservations do not match the exact order lines.")

    sku_ids = sorted(expected)
    location_ids = sorted({record.location_id for record in held})
    list(CommerceSKU.objects.select_for_update().filter(pk__in=sku_ids).order_by("pk"))
    list(CommerceStockLocation.objects.select_for_update().filter(
        pk__in=location_ids, active=True).order_by("pk"))
    if (CommerceStockLocation.objects.filter(pk__in=location_ids, active=True).count()
            != len(location_ids)):
        raise ValidationError("A warehouse used by this order is no longer active.")

    now = timezone.now()
    for reservation in held:
        counted = CommerceStockMovement.objects.filter(
            sku_id=reservation.sku_id, location_id=reservation.location_id,
        ).aggregate(n=Sum("quantity_delta"))["n"] or 0
        all_holds = CommerceStockReservation.objects.filter(
            sku_id=reservation.sku_id, location_id=reservation.location_id,
            released_at__isnull=True,
        ).aggregate(n=Sum("quantity"))["n"] or 0
        if counted < all_holds:
            raise ValidationError("Physical warehouse count is less than current reserved units.")
    receipt = CommerceFulfillmentReceipt.objects.create(
        order=order, carrier_name=carrier, tracking_reference=tracking,
        dispatch_note=note, dispatched_by=staff,
    )
    for reservation in held:
        payload = {
            "order": str(order.pk), "reservation": str(reservation.pk),
            "quantity": -reservation.quantity, "tracking": tracking,
        }
        fingerprint = hashlib.sha256(
            json.dumps(payload, sort_keys=True).encode("utf-8")
        ).hexdigest()
        CommerceStockMovement.objects.create(
            sku_id=reservation.sku_id, location_id=reservation.location_id,
            recorded_by=staff, quantity_delta=-reservation.quantity,
            reason=CommerceStockMovement.Reason.DISPATCH,
            reference_note=f"Order {str(order.pk)[:8]} dispatch: {tracking}"[:200],
            client_id=uuid.uuid4(), fingerprint=fingerprint,
        )
        reservation.released_at = now
        reservation.save(update_fields=["released_at"])
    order.status = CommerceOrder.Status.DISPATCHED
    order.last_status_at = now
    order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="dispatched",
        label="Plantupia recorded handover to the delivery carrier.",
    )
    company.audit(staff, receipt, f"Physical handover recorded: {carrier} / {tracking}; location stock consumed exactly once.")
    return order


@transaction.atomic
def record_delivery(*, staff, order_id, proof_reference):
    company.require(staff, "plants.change_commerceorder")
    proof = _required(proof_reference, "Retained delivery proof reference", 180)
    if len(proof) < 8:
        raise ValidationError("Provide a verifiable retained delivery proof reference.")
    order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
    if order.status == CommerceOrder.Status.DELIVERED:
        receipt = CommerceFulfillmentReceipt.objects.get(order=order)
        if receipt.delivery_proof_reference != proof:
            raise ValidationError("Delivery evidence was already recorded with another reference.")
        return order
    if order.status != CommerceOrder.Status.DISPATCHED:
        raise ValidationError("Only a dispatched order can be marked delivered.")
    receipt = CommerceFulfillmentReceipt.objects.select_for_update().get(order=order)
    receipt.delivery_proof_reference = proof
    receipt.delivered_by = staff
    receipt.delivered_at = timezone.now()
    receipt.save(update_fields=["delivery_proof_reference", "delivered_by", "delivered_at"])
    order.status = CommerceOrder.Status.DELIVERED
    order.last_status_at = receipt.delivered_at
    order.save(update_fields=["status", "last_status_at", "updated_at"])
    CommerceRealOrderEvent.objects.create(
        order=order, actor=staff, kind="delivered",
        label="Plantupia recorded delivery with a retained proof reference.",
    )
    company.audit(staff, receipt, f"Operator recorded retained delivery evidence {proof}; payment state unchanged.")
    return order
