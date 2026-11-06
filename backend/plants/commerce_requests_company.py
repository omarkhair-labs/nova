"""Small staff QA inbox with clear order tracking, without a payment gateway."""
from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.db.models import Q
from django.shortcuts import get_object_or_404, redirect
from django.utils import timezone

from . import commerce_checkout as checkout, company_services as company
from . import commerce_tracking as tracking
from .commerce_models import CommerceOrderRequest
from .company_views import page, paginated, portal


class QARequestReviewForm(forms.Form):
    review_token = forms.CharField(widget=forms.HiddenInput)
    action = forms.ChoiceField(choices=[
        ("review", "Review request (QA only)"),
        ("decline", "Decline request"),
    ])
    qa_shipping_fee = forms.DecimalField(
        required=False, min_value=0, max_digits=10, decimal_places=2,
        label="Delivery fee in EGP (optional)",
        help_text="Leave blank if unknown; enter 0 only if staff explicitly confirms free delivery.",
    )
    review_note = forms.CharField(max_length=500, required=False,
        widget=forms.Textarea(attrs={"rows": 3}),
        help_text="A short note displayed to the customer.")
    confirmed = forms.BooleanField(label="This updates QA tracking only, not a real payment or shipment")


class QAProgressForm(forms.Form):
    review_token = forms.CharField(widget=forms.HiddenInput)
    action = forms.ChoiceField(choices=())
    review_note = forms.CharField(max_length=320, required=False,
        widget=forms.Textarea(attrs={"rows": 2}),
        help_text="Optional status update shown to the customer.")
    tracking_reference = forms.CharField(max_length=120, required=False,
        help_text="Optional QA dispatch reference only; not a courier integration.")
    confirmed = forms.BooleanField(label="Confirm this QA tracking change")

    def __init__(self, *args, current_status, **kwargs):
        super().__init__(*args, **kwargs)
        available = tracking.ALLOWED_NEXT.get(current_status, {})
        names = {
            "prepare": "Preparing", "dispatch": "Out for delivery",
            "deliver": "Delivered", "cancel": "Cancelled",
            "return": "Return completed",
        }
        self.fields["action"].choices = [
            (key, names[key] + " (QA)") for key in available
        ]


@portal("plants.view_commerceorderrequest", methods=("GET",))
def qa_requests(request):
    checkout.require_qa()
    qs = CommerceOrderRequest.objects.select_related("customer").prefetch_related("lines")
    state = request.GET.get("status", "")
    if state in CommerceOrderRequest.Status.values:
        qs = qs.filter(status=state)
    query = request.GET.get("q", "").strip()[:120]
    if query:
        qs = qs.filter(Q(contact_name__icontains=query) | Q(city__icontains=query) |
                       Q(customer__username__icontains=query))
    return page(request, "commerce_qa_requests", {
        "title": "B2C orders & tracking (QA)", "section": "commerce_qa_requests",
        "commerce_qa_requests_enabled": True,
        "new_alert_count": CommerceOrderRequest.objects.filter(staff_first_seen_at__isnull=True).count(),
        "records": paginated(request, qs.order_by("-created_at")), "state": state, "query": query,
    })


@portal("plants.view_commerceorderrequest")
def qa_request_detail(request, order_id):
    checkout.require_qa()
    order = get_object_or_404(
        CommerceOrderRequest.objects.select_related("customer", "reviewed_by")
        .prefetch_related("lines", "status_events"), pk=order_id,
    )
    if request.method == "GET" and order.staff_first_seen_at is None:
        CommerceOrderRequest.objects.filter(
            pk=order.pk, staff_first_seen_at__isnull=True,
        ).update(staff_first_seen_at=timezone.now())
    choices = tracking.ALLOWED_NEXT.get(order.status, {})
    if order.status == order.Status.SUBMITTED:
        form = QARequestReviewForm(request.POST or None, initial={
            "review_token": company.action_token(
                request.user, "commerce-qa-review", order, checkout.review_state(order)),
        })
    elif choices:
        form = QAProgressForm(request.POST or None, current_status=order.status, initial={
            "review_token": company.action_token(
                request.user, "commerce-qa-review", order, checkout.review_state(order)),
        })
    else:
        form = None

    if request.method == "POST":
        company.require(request.user, "plants.change_commerceorderrequest")
        if form is None:
            messages.error(request, "No action is available for this tracking state.")
        elif form.is_valid():
            try:
                if order.status == order.Status.SUBMITTED:
                    checkout.review_qa_request(
                        staff=request.user, order_id=order.pk,
                        action=form.cleaned_data["action"],
                        review_note=form.cleaned_data["review_note"],
                        shipping_fee=form.cleaned_data["qa_shipping_fee"],
                        review_token=form.cleaned_data["review_token"],
                    )
                else:
                    tracking.advance(
                        staff=request.user, order_id=order.pk,
                        action=form.cleaned_data["action"],
                        note=form.cleaned_data["review_note"],
                        tracking_reference=form.cleaned_data["tracking_reference"],
                        review_token=form.cleaned_data["review_token"],
                    )
            except ValidationError as exc:
                form.add_error(None, exc)
            else:
                messages.success(request, "Tracking updated for this QA request.")
                return redirect("company:commerce-qa-request", order_id=order.pk)

    return page(request, "commerce_qa_request", {
        "title": f"QA order {str(order.pk)[:8]}",
        "section": "commerce_qa_requests", "order": order, "form": form,
        "can_progress": bool(choices),
        "qa_total": tracking.quoted_total(order),
        "commerce_qa_requests_enabled": True,
    })
