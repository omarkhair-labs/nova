"""Actual Company B2B inquiry queue, independent of QA RFQs/Installations."""
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404, redirect

from . import commerce_business_real as business, commerce_business_project as project, company_services as company
from .commerce_models import CommerceBusinessInquiry
from .models import InstallationBatch
from .company_views import page, paginated, portal


@portal("plants.view_commercebusinessinquiry", methods=("GET",))
def real_business_inquiries(request):
    status = request.GET.get("status", "").strip()
    records = CommerceBusinessInquiry.objects.select_related("requester").order_by("-created_at", "pk")
    if status in CommerceBusinessInquiry.Status.values:
        records = records.filter(status=status)
    return page(request, "commerce_business_inquiries", {
        "title": "Real business requests", "section": "business_inquiries",
        "records": paginated(request, records), "status_filter": status,
        "pending_count": CommerceBusinessInquiry.objects.filter(status="received").count(),
    })


@portal("plants.view_commercebusinessinquiry", methods=("GET",))
def real_business_inquiry(request, inquiry_id):
    record = get_object_or_404(
        CommerceBusinessInquiry.objects.select_related("requester", "quoted_by", "sku"),
        pk=inquiry_id,
    )
    return page(request, "commerce_business_inquiry", {
        "title": record.company_name, "section": "business_inquiries",
        "record": record,
        "prepared_installation": InstallationBatch.objects.filter(
            source_b2b_inquiry=record,
        ).filter(prepared_by=request.user).first() if not request.user.is_superuser else
            InstallationBatch.objects.filter(source_b2b_inquiry=record).first(),
    })


@portal("plants.change_commercebusinessinquiry", methods=("POST",))
def real_business_quote_decision(request, inquiry_id):
    company.require(request.user, "plants.view_commercebusinessinquiry")
    action = request.POST.get("action", "")
    try:
        if action == "quote":
            business.quote_inquiry(
                staff=request.user, inquiry_id=inquiry_id,
                amount=request.POST.get("amount", ""),
                note=request.POST.get("note", ""),
                version=request.POST.get("quote_version", ""),
            )
            messages.success(request, "Exact nonbinding B2B quote recorded for customer approval.")
        elif action == "decline":
            business.decline_inquiry(staff=request.user, inquiry_id=inquiry_id)
            messages.success(request, "Business enquiry declined. No sale or installation created.")
        else:
            raise ValidationError("Choose quote or decline.")
    except ValidationError as exc:
        for error in exc.messages:
            messages.error(request, error)
    return redirect("company:business-inquiry", inquiry_id=inquiry_id)


@portal("plants.view_commercebusinessinquiry", methods=("POST",))
def create_business_installation(request, inquiry_id):
    """One audited draft project from accepted scope, no implicit inventory."""
    try:
        batch = project.create_draft_installation_from_inquiry(
            staff=request.user, inquiry_id=inquiry_id,
        )
    except ValidationError as exc:
        for item in exc.messages:
            messages.error(request, item)
        return redirect("company:business-inquiry", inquiry_id=inquiry_id)
    messages.success(
        request,
        "Draft project prepared. Add physical plant inventory, review, then issue a recipient-bound handover.",
    )
    return redirect("company:installation", batch_id=batch.pk)
