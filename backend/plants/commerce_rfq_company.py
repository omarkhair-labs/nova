"""Business QA enquiries: one inbox, one staff quote action."""
from decimal import Decimal
from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.shortcuts import redirect, get_object_or_404
from django.utils import timezone

from . import commerce_checkout as checkout, commerce_rfq as service
from . import company_services as company
from .commerce_models import CommerceBusinessRFQ
from .company_views import page, portal, paginated


class QuoteForm(forms.Form):
    token = forms.CharField(widget=forms.HiddenInput)
    action = forms.ChoiceField(choices=(("quote", "Send QA quote"), ("decline", "Decline request")))
    amount = forms.DecimalField(required=False, min_value=Decimal("0.01"), max_digits=12,
        decimal_places=2, label="Quoted EGP amount",
        help_text="Required when sending a quote; draft nonbinding amount.")
    note = forms.CharField(required=False, max_length=800,
        widget=forms.Textarea(attrs={"rows": 3}),
        label="Scope / delivery / assumptions")
    confirmed = forms.BooleanField(label="This is a nonbinding QA quotation, not a purchase order")


@portal("plants.view_commercebusinessrfq", methods=("GET",))
def rfq_list(request):
    checkout.require_qa()
    qs = CommerceBusinessRFQ.objects.select_related("requester")
    return page(request, "business_qa_rfqs", {
        "title": "Business quotes / RFQ", "section": "business_rfqs",
        "records": paginated(request, qs.order_by("-created_at")),
        "new_count": qs.filter(staff_first_seen_at__isnull=True).count(),
    })


@portal("plants.view_commercebusinessrfq")
def rfq_detail(request, rfq_id):
    checkout.require_qa()
    rfq = get_object_or_404(CommerceBusinessRFQ.objects.select_related(
        "requester", "replied_by"), pk=rfq_id)
    if request.method == "GET" and rfq.staff_first_seen_at is None:
        CommerceBusinessRFQ.objects.filter(
            pk=rfq.pk, staff_first_seen_at__isnull=True,
        ).update(staff_first_seen_at=timezone.now())
    form = QuoteForm(request.POST or None, initial={
        "token": company.action_token(request.user, "commerce-rfq", rfq, service.review_state(rfq))
    }) if rfq.status == CommerceBusinessRFQ.Status.SUBMITTED else None
    if request.method == "POST":
        company.require(request.user, "plants.change_commercebusinessrfq")
        if form is None:
            messages.error(request, "This enquiry was already answered.")
        elif form.is_valid():
            try:
                service.staff_reply(
                    staff=request.user, rfq_id=rfq.pk, action=form.cleaned_data["action"],
                    amount=form.cleaned_data["amount"], note=form.cleaned_data["note"],
                    token=form.cleaned_data["token"],
                )
            except ValidationError as exc:
                form.add_error(None, exc)
            else:
                messages.success(request, "QA quote reply stored. No sales order was created.")
                return redirect("company:business-rfq", rfq_id=rfq.pk)
    return page(request, "business_qa_rfq", {
        "title": "Business RFQ " + str(rfq.pk)[:8],
        "section": "business_rfqs", "rfq": rfq, "form": form,
    })
