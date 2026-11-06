"""Staff-only QA review queue. No acceptance, payment or shipment action exists."""
from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.db.models import Q
from django.http import Http404
from django.shortcuts import get_object_or_404, redirect

from . import commerce_checkout as checkout, company_services as company
from .commerce_models import CommerceOrderRequest
from .company_views import page, paginated, portal


class QARequestReviewForm(forms.Form):
    review_token = forms.CharField(widget=forms.HiddenInput)
    action = forms.ChoiceField(choices=[
        ("review", "Mark staff-reviewed (not accepted)"),
        ("decline", "Decline this QA request"),
    ])
    review_note = forms.CharField(max_length=500, required=False,
        widget=forms.Textarea(attrs={"rows": 3}),
        help_text="Internal context; shown to the requesting account after decision.")
    confirmed = forms.BooleanField(
        label="I understand this is QA only and does NOT confirm payment, shipping or a sale"
    )


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
        "title": "B2C request review (QA)", "section": "commerce_qa_requests", "commerce_qa_requests_enabled": True,
        "records": paginated(request, qs.order_by("-created_at")), "state": state, "query": query,
    })


@portal("plants.view_commerceorderrequest")
def qa_request_detail(request, order_id):
    checkout.require_qa()
    order = get_object_or_404(
        CommerceOrderRequest.objects.select_related("customer", "reviewed_by").prefetch_related("lines"),
        pk=order_id,
    )
    form = QARequestReviewForm(request.POST or None, initial={
        "review_token": company.action_token(
            request.user, "commerce-qa-review", order, checkout.review_state(order)
        ),
    })
    if request.method == "POST":
        company.require(request.user, "plants.change_commerceorderrequest")
        if order.status != order.Status.SUBMITTED:
            form.add_error(None, "This QA request was already reviewed. Reload its current status.")
        elif form.is_valid():
            try:
                checkout.review_qa_request(
                    staff=request.user, order_id=order.pk,
                    action=form.cleaned_data["action"], review_note=form.cleaned_data["review_note"],
                    review_token=form.cleaned_data["review_token"],
                )
            except ValidationError as exc:
                form.add_error(None, exc)
            else:
                messages.success(request, "QA decision stored. No sale, payment or shipping was accepted.")
                return redirect("company:commerce-qa-request", order_id=order.pk)
    return page(request, "commerce_qa_request", {
        "title": f"QA customer request {str(order.pk)[:8]}",
        "section": "commerce_qa_requests", "order": order, "form": form, "commerce_qa_requests_enabled": True,
    })
