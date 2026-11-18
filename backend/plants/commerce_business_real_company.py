"""Actual Company B2B inquiry queue, independent of QA RFQs/Installations."""
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.db.models import Exists, OuterRef, Q
from django.shortcuts import get_object_or_404, redirect

from . import commerce_business_real as business, commerce_business_project as project, company_services as company
from .commerce_models import CommerceBusinessInquiry
from .models import InstallationBatch
from .company_views import page, paginated, portal


def business_inquiry_work_queue():
    """One factual queue over the existing B2B record, not another CRM.

    An accepted installation WITH a linked draft already belongs to
    Installations, not the incoming commercial work queue.
    """
    existing_project = InstallationBatch.objects.filter(source_b2b_inquiry=OuterRef("pk"))
    return CommerceBusinessInquiry.objects.select_related("requester").annotate(
        has_linked_project=Exists(existing_project),
    )


def business_inquiry_needs_attention():
    return (
        Q(status=CommerceBusinessInquiry.Status.RECEIVED)
        | Q(status=CommerceBusinessInquiry.Status.ACCEPTED, has_linked_project=False)
    )


@portal("plants.view_commercebusinessinquiry", methods=("GET",))
def real_business_inquiries(request):
    status = request.GET.get("status", "action_needed").strip()
    all_records = business_inquiry_work_queue()
    records = all_records
    if status == "action_needed":
        records = records.filter(business_inquiry_needs_attention())
    elif status in CommerceBusinessInquiry.Status.values:
        records = records.filter(status=status)
    elif status != "all":
        status = "action_needed"
        records = records.filter(business_inquiry_needs_attention())
    records = records.order_by("-created_at", "pk")
    return page(request, "commerce_business_inquiries", {
        "title": "Business requests", "section": "business_inquiries",
        "records": paginated(request, records), "status_filter": status,
        "pending_count": all_records.filter(status=CommerceBusinessInquiry.Status.RECEIVED).count(),
        "followup_count": all_records.filter(
            status=CommerceBusinessInquiry.Status.ACCEPTED, has_linked_project=False).count(),
        "waiting_count": all_records.filter(
            status=CommerceBusinessInquiry.Status.QUOTED).count(),
    })


@portal("plants.view_commercebusinessinquiry", methods=("GET",))
def real_business_inquiry(request, inquiry_id):
    record = get_object_or_404(
        CommerceBusinessInquiry.objects.select_related("requester", "quoted_by", "sku"),
        pk=inquiry_id,
    )
    prepared = (
        InstallationBatch.objects.filter(source_b2b_inquiry=record)
        .filter(prepared_by=request.user).first()
        if not request.user.is_superuser else
        InstallationBatch.objects.filter(source_b2b_inquiry=record).first()
    )
    if record.status == CommerceBusinessInquiry.Status.RECEIVED:
        next_title = "Review the need and prepare a quotation"
        next_explanation = (
            "Check the requested scope and real constraints. Record one clear "
            "nonbinding quotation, or decline if Plantupia cannot provide it."
        )
    elif record.status == CommerceBusinessInquiry.Status.QUOTED:
        next_title = "Waiting for the customer"
        next_explanation = (
            "The quotation is on the customer's record. Do not create an "
            "installation, reserve stock or claim acceptance while it is pending."
        )
    elif record.status == CommerceBusinessInquiry.Status.ACCEPTED:
        if record.kind == CommerceBusinessInquiry.Kind.INSTALLATION:
            next_title = (
                "Continue the linked installation" if prepared else
                "Prepare the project draft after commercial follow-up"
            )
            next_explanation = (
                "The accepted nonbinding quote is not a signed contract. "
                "Once commercial terms have been independently verified, "
                "authorized staff can prepare the linked draft and review physical plants."
            )
        else:
            next_title = "Continue commercial wholesale follow-up"
            next_explanation = (
                "Agree the actual supply terms separately. This acceptance "
                "does not create a purchase order, allocate stock or collect money."
            )
    else:
        next_title = "Enquiry closed"
        next_explanation = "No further quote action is available on this record."
    return page(request, "commerce_business_inquiry", {
        "title": record.company_name, "section": "business_inquiries",
        "record": record, "prepared_installation": prepared,
        "next_title": next_title, "next_explanation": next_explanation,
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
