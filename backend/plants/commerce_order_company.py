"""One real Company B2C order inbox; checkout quote is actual customer data."""
from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404, redirect

from . import commerce_orders as orders, company_services as company
from .commerce_models import CommerceOrder
from .company_views import page, paginated, portal


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
    qs = CommerceOrder.objects.select_related("customer").order_by("-created_at", "pk")
    if status in CommerceOrder.Status.values:
        qs = qs.filter(status=status)
    return page(request, "commerce_orders", {
        "title": "Actual customer orders", "section": "commerce_orders",
        "records": paginated(request, qs),
        "status_filter": status,
        "pending": CommerceOrder.objects.filter(status=CommerceOrder.Status.QUOTE_PENDING).count(),
        "quoted": CommerceOrder.objects.filter(status=CommerceOrder.Status.QUOTED).count(),
        "confirmed": CommerceOrder.objects.filter(status=CommerceOrder.Status.CONFIRMED).count(),
    })


@portal("plants.view_commerceorder", methods=("GET",))
def real_order_detail(request, order_id):
    order = get_object_or_404(
        CommerceOrder.objects.select_related("customer", "quoted_by")
        .prefetch_related("lines", "stock_reservations__location"),
        pk=order_id,
    )
    return page(request, "commerce_order", {
        "title": f"Order {str(order.pk)[:8]}",
        "section": "commerce_orders",
        "order": order,
        "form": ShippingDecisionForm(initial={"version": order.shipping_quote_version}),
        "total": order.subtotal + order.shipping_fee if order.shipping_fee is not None else None,
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
