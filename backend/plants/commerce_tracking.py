"""Minimal, audited, in-app QA tracking. No courier/payment integration or fake collected funds."""
from decimal import Decimal, InvalidOperation

from django.core.exceptions import PermissionDenied, ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import commerce_checkout as checkout, company_services as company
from .commerce_models import CommerceOrderRequest, CommerceOrderStatusEvent


# Every staff transition is deliberate; no skipping a stage.
ALLOWED_NEXT = {
    CommerceOrderRequest.Status.REVIEWED: {
        "prepare": CommerceOrderRequest.Status.PREPARING,
        "cancel": CommerceOrderRequest.Status.CANCELLED,
    },
    CommerceOrderRequest.Status.PREPARING: {
        "dispatch": CommerceOrderRequest.Status.OUT_FOR_DELIVERY,
        "cancel": CommerceOrderRequest.Status.CANCELLED,
    },
    CommerceOrderRequest.Status.OUT_FOR_DELIVERY: {
        "deliver": CommerceOrderRequest.Status.DELIVERED,
    },
    CommerceOrderRequest.Status.RETURN_REQUESTED: {
        "return": CommerceOrderRequest.Status.RETURNED,
    },
}
STATUS_LABEL = {
    CommerceOrderRequest.Status.SUBMITTED: "Request received",
    CommerceOrderRequest.Status.REVIEWED: "Staff reviewed request",
    CommerceOrderRequest.Status.PREPARING: "Preparing your order (QA)",
    CommerceOrderRequest.Status.OUT_FOR_DELIVERY: "Out for delivery (QA simulation)",
    CommerceOrderRequest.Status.DELIVERED: "Delivery marked complete (QA simulation)",
    CommerceOrderRequest.Status.CANCELLED: "Request cancelled (QA)",
    CommerceOrderRequest.Status.RETURN_REQUESTED: "Return requested (QA)",
    CommerceOrderRequest.Status.RETURNED: "Return marked complete (QA)",
    CommerceOrderRequest.Status.DECLINED: "Request declined (QA)",
}


def append_event(order, actor, kind, note=""):
    return CommerceOrderStatusEvent.objects.create(
        order=order, actor=actor, kind=kind,
        label=STATUS_LABEL.get(kind, kind), note=(note or "").strip()[:320],
    )


def timeline_json(order):
    return [
        {"status": event.kind, "label": event.label, "note": event.note,
         "at": event.created_at.isoformat()}
        for event in order.status_events.all()
    ]


def quoted_total(order):
    if order.qa_shipping_fee is None:
        return None
    return order.provisional_subtotal + order.qa_shipping_fee


def quote_shipping(value):
    """Blank = pending. Zero must be explicitly provided to mean a QA free-delivery quote."""
    if value is None or str(value).strip() == "":
        return None
    try:
        amount = Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError) as exc:
        raise ValidationError("Enter a valid shipping amount or leave it pending.") from exc
    if not amount.is_finite() or amount < 0 or amount > Decimal("9999999.99"):
        raise ValidationError("Invalid QA shipping fee.")
    if amount.as_tuple().exponent < -2:
        raise ValidationError("Use no more than two decimal places.")
    return amount


@transaction.atomic
def advance(*, staff, order_id, action, review_token, note="", tracking_reference=""):
    checkout.require_qa()
    company.require(staff, "plants.change_commerceorderrequest")
    order = get_object_or_404(
        CommerceOrderRequest.objects.select_for_update().prefetch_related("lines"),
        pk=order_id,
    )
    target = ALLOWED_NEXT.get(order.status, {}).get(action)
    if target is None:
        raise ValidationError("This step is not available for the request's current status.")
    company.verify_token(staff, "commerce-qa-review", order, checkout.review_state(order), review_token)
    note = (note or "").strip()
    if len(note) > 320:
        raise ValidationError("Update note is too long.")
    ref = (tracking_reference or "").strip()
    if len(ref) > 120:
        raise ValidationError("Tracking reference is too long.")
    if action != "dispatch" and ref:
        raise ValidationError("Tracking reference can only be entered for dispatch.")
    # Explicitly not a courier ID assertion: QA operator-entered display reference only.
    if action == "dispatch":
        order.qa_tracking_reference = ref
    order.status = target
    order.last_status_at = timezone.now()
    order.full_clean()
    order.save(update_fields=["status", "last_status_at", "qa_tracking_reference"])
    append_event(order, staff, target, note)
    company.audit(staff, order, "QA tracking update; no real dispatch or payment recorded")
    return order


@transaction.atomic
def customer_request_action(*, customer, order_id, action):
    checkout.require_qa()
    if customer.is_staff or not customer.is_active:
        raise PermissionDenied
    order = get_object_or_404(
        CommerceOrderRequest.objects.select_for_update(),
        pk=order_id, customer=customer,
    )
    if action == "cancel":
        if order.status not in (order.Status.SUBMITTED, order.Status.REVIEWED):
            raise ValidationError("Cancellation requests are available only before preparation.")
        if not order.customer_cancel_requested_at:
            order.customer_cancel_requested_at = timezone.now()
            order.save(update_fields=["customer_cancel_requested_at"])
            CommerceOrderStatusEvent.objects.create(
                order=order, actor=customer, kind="qa_cancel_requested",
                label="Customer requested cancellation (QA)",
            )
            company.audit(customer, order, "Customer requested cancellation of QA order")
    elif action == "return":
        if order.status not in (order.Status.DELIVERED, order.Status.RETURN_REQUESTED):
            raise ValidationError("You can request a return only after the QA delivery stage.")
        if not order.customer_return_requested_at:
            order.customer_return_requested_at = timezone.now()
            order.status = order.Status.RETURN_REQUESTED
            order.last_status_at = timezone.now()
            order.full_clean()
            order.save(update_fields=["customer_return_requested_at", "status", "last_status_at"])
            append_event(order, customer, order.Status.RETURN_REQUESTED)
            company.audit(customer, order, "Customer requested QA return; no funds refunded")
    else:
        raise ValidationError("Choose cancel or return.")
    return order
