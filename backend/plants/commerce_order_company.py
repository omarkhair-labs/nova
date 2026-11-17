"""One real Company B2C order inbox; checkout quote is actual customer data."""
from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404, redirect

from . import commerce_orders as orders, commerce_fulfillment as fulfillment, commerce_payments as payments, commerce_returns as returns, company_services as company
from .commerce_models import CommerceOrder
from .company_views import page, paginated, portal


ORDER_NEXT = {
    CommerceOrder.Status.QUOTE_PENDING: ("Review new customer request", "Check the items, delivery address and availability. Record the delivery fee so the customer can review the final total.", "quote"),
    CommerceOrder.Status.QUOTED: ("Waiting for customer confirmation", "The delivery fee is recorded. The customer must accept the final total before preparation begins.", "quote"),
    CommerceOrder.Status.CONFIRMED: ("Prepare the order", "The customer accepted the total and stock is reserved.", "fulfillment"),
    CommerceOrder.Status.PREPARING: ("Record actual delivery handover", "Record dispatch when Plantupia's team or an external carrier has taken the items.", "fulfillment"),
    CommerceOrder.Status.DISPATCHED: ("Record proof of delivery", "Enter the reference to the retained delivery record.", "fulfillment"),
    CommerceOrder.Status.RETURN_REQUESTED: ("Review the return", "Check the current decision and record the physical receipt when it arrives.", "returns"),
}


class ShippingDecisionForm(forms.Form):
    action = forms.ChoiceField(choices=(("quote", "Set shipping fee"), ("decline", "Decline")))
    version = forms.IntegerField(min_value=0, widget=forms.HiddenInput)
    shipping_fee = forms.DecimalField(
        min_value=0, max_digits=10, decimal_places=2, required=False,
        help_text="Exact EGP delivery cost. Enter 0 only for truly free shipping.",
    )

    def clean(self):
        values = super().clean()
        if values.get("action") == "quote" and values.get("shipping_fee") is None:
            raise forms.ValidationError("A confirmed delivery fee is required.")
        return values


@portal("plants.view_commerceorder", methods=("GET",))
def real_orders(request):
    status = request.GET.get("status", "").strip()
    statuses = dict(CommerceOrder.Status.choices)
    qs = CommerceOrder.objects.select_related("customer").order_by("-created_at", "pk")
    if status in statuses:
        qs = qs.filter(status=status)
    return page(request, "commerce_orders", {
        "title": "Customer requests & orders", "section": "commerce_orders",
        "records": paginated(request, qs),
        "status_filter": status if status in statuses else "",
        "status_label": statuses.get(status, "Recent orders"),
        "pending": CommerceOrder.objects.filter(status=CommerceOrder.Status.QUOTE_PENDING).count(),
        "quoted": CommerceOrder.objects.filter(status=CommerceOrder.Status.QUOTED).count(),
        "confirmed": CommerceOrder.objects.filter(status=CommerceOrder.Status.CONFIRMED).count(),
    })


@portal("plants.view_commerceorder", methods=("GET",))
def real_order_detail(request, order_id):
    order = get_object_or_404(
        CommerceOrder.objects.select_related("customer", "quoted_by")
        .prefetch_related("lines", "stock_reservations__location", "stock_reservations__sku", "status_events__actor",
                          "fulfillment_receipt", "transfer_claim", "verified_payment", "return_case", "verified_refund"),
        pk=order_id,
    )
    total = order.subtotal + order.shipping_fee if order.shipping_fee is not None else None
    payment_blocker = payments.reconciliation_blocker(order) if order.payment_status == CommerceOrder.PaymentStatus.UNPAID else ""
    can_reconcile = (
        request.user.has_perm("plants.add_commercepaymentrecord")
        and order.payment_status == CommerceOrder.PaymentStatus.UNPAID
        and not payment_blocker and total is not None and total > 0
    )
    can_refund = (
        request.user.has_perm("plants.add_commercerefundrecord")
        and order.status == CommerceOrder.Status.RETURNED
        and order.payment_status == CommerceOrder.PaymentStatus.VERIFIED
    )
    if can_refund:
        next_step = ("Record the external refund", "Use the independently verified payout reference.", "returns")
    elif can_reconcile:
        next_step = ("Reconcile the payment", "Match the accepted total against the independent settlement record.", "payment")
    else:
        next_step = ORDER_NEXT.get(order.status)
    return page(request, "commerce_order", {
        "title": f"Order {str(order.pk)[:8]}",
        "section": "commerce_orders",
        "order": order,
        "total": total,
        "payment_blocker": payment_blocker,
        "can_reconcile": can_reconcile,
        "can_refund": can_refund,
        "next_step": next_step,
    })


@portal("plants.change_commerceorder", methods=("POST",))
def real_order_shipping_decision(request, order_id):
    company.require(request.user, "plants.view_commerceorder")
    form = ShippingDecisionForm(request.POST)
    if form.is_valid():
        try:
            if form.cleaned_data["action"] == "quote":
                orders.quote_shipping(
                    staff=request.user, order_id=order_id,
                    fee=form.cleaned_data["shipping_fee"],
                    version=form.cleaned_data["version"],
                )
                messages.success(request, "Shipping fee recorded; awaiting customer acceptance of the exact total.")
            else:
                orders.decline_quote_request(staff=request.user, order_id=order_id)
                messages.success(request, "Request declined; no payment or stock reservation occurred.")
        except ValidationError as exc:
            for error in exc.messages:
                messages.error(request, error)
    else:
        for errors in form.errors.values():
            for error in errors:
                messages.error(request, error)
    return redirect("company:commerce-order", order_id=order_id)

@portal("plants.change_commerceorder", methods=("POST",))
def real_order_fulfillment(request, order_id):
    company.require(request.user, "plants.view_commerceorder")
    action = request.POST.get("action", "")
    try:
        if action == "prepare":
            fulfillment.start_preparation(staff=request.user, order_id=order_id)
            messages.success(request, "Preparation recorded. Stock remains reserved.")
        elif action == "dispatch":
            fulfillment.dispatch_order(
                staff=request.user, order_id=order_id,
                carrier_name=request.POST.get("carrier_name", ""),
                tracking_reference=request.POST.get("tracking_reference", ""),
                dispatch_note=request.POST.get("dispatch_note", ""),
            )
            messages.success(request, "Carrier handover recorded and held inventory consumed once.")
        elif action == "delivered":
            fulfillment.record_delivery(
                staff=request.user, order_id=order_id,
                proof_reference=request.POST.get("proof_reference", ""),
            )
            messages.success(request, "Delivery evidence reference recorded; payment state unchanged.")
        else:
            raise ValidationError("Choose a valid fulfillment action.")
    except ValidationError as exc:
        for error in exc.messages:
            messages.error(request, error)
    return redirect("company:commerce-order", order_id=order_id)

@portal("plants.add_commercepaymentrecord", methods=("POST",))
def real_order_verify_payment(request, order_id):
    company.require(request.user, "plants.view_commerceorder")
    try:
        payments.verify_payment(
            staff=request.user, order_id=order_id,
            amount=request.POST.get("amount", ""),
            settlement_reference=request.POST.get("settlement_reference", ""),
            evidence_note=request.POST.get("evidence_note", ""),
        )
        messages.success(request, "Finance reviewer verified independent settlement evidence.")
    except ValidationError as exc:
        for error in exc.messages:
            messages.error(request, error)
    return redirect("company:commerce-order", order_id=order_id)

@portal("plants.change_commerceorder", methods=("POST",))
def real_order_return_decision(request, order_id):
    company.require(request.user, "plants.view_commerceorder")
    action = request.POST.get("action", "")
    try:
        if action in ("approve", "decline"):
            returns.decide_return(
                staff=request.user, order_id=order_id, action=action,
                note=request.POST.get("staff_note", ""),
            )
        elif action == "receive":
            returns.receive_return(
                staff=request.user, order_id=order_id,
                evidence_reference=request.POST.get("evidence_reference", ""),
            )
        else:
            raise ValidationError("Choose a valid return action.")
        messages.success(request, "Return case updated without inferring restock or refund.")
    except ValidationError as exc:
        for error in exc.messages:
            messages.error(request, error)
    return redirect("company:commerce-order", order_id=order_id)


@portal("plants.add_commercerefundrecord", methods=("POST",))
def real_order_refund_verification(request, order_id):
    company.require(request.user, "plants.view_commerceorder")
    try:
        returns.verify_external_refund(
            staff=request.user, order_id=order_id,
            amount=request.POST.get("amount", ""),
            reference=request.POST.get("refund_reference", ""),
            note=request.POST.get("refund_note", ""),
        )
        messages.success(request, "External refund evidence verified; no gateway payout initiated.")
    except ValidationError as exc:
        for error in exc.messages:
            messages.error(request, error)
    return redirect("company:commerce-order", order_id=order_id)
