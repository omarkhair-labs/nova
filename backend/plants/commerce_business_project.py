"""Connect an accepted installation quotation to one traceable draft project.

No RFQ acceptance is a purchase order or staff-reviewed physical inventory.
Project creation is an explicit authorized step, and no handover code is issued.
"""
from django.core.exceptions import ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404

from . import company_services as company
from .commerce_models import CommerceBusinessInquiry
from .models import InstallationBatch, Space


@transaction.atomic
def create_draft_installation_from_inquiry(*, staff, inquiry_id):
    company.require(
        staff,
        "plants.view_commercebusinessinquiry",
        "plants.add_installationbatch",
        "plants.add_space",
    )
    enquiry = get_object_or_404(
        CommerceBusinessInquiry.objects.select_for_update(), pk=inquiry_id,
    )
    if (
        enquiry.kind != CommerceBusinessInquiry.Kind.INSTALLATION
        or enquiry.status != CommerceBusinessInquiry.Status.ACCEPTED
        or enquiry.quoted_amount is None
    ):
        raise ValidationError(
            "Only an accepted installation-scope quotation can prepare a draft project."
        )
    original = InstallationBatch.objects.filter(source_b2b_inquiry=enquiry).first()
    if original is not None:
        # Never disclose another operator's private installation just because a
        # commercial reviewer can see the associated quotation.
        if original.prepared_by_id != staff.pk and not staff.is_superuser:
            raise ValidationError("This installation is assigned to another operator.")
        return original

    space = Space.objects.create(
        owner=staff,
        name=(enquiry.company_name.strip() + " · " + enquiry.city.strip())[:140],
        description=(
            "Business enquiry " + str(enquiry.pk) + "\n"
            + enquiry.scope.strip() + "\n"
            + "Accepted nonbinding quote: EGP " + str(enquiry.quoted_amount)
            + "\n" + enquiry.quote_note.strip()
        ),
        location_note=enquiry.city.strip()[:200],
    )
    batch = InstallationBatch(
        source_b2b_inquiry=enquiry,
        reference_code="B2B-" + enquiry.pk.hex.upper(),
        space=space,
        prepared_by=staff,
        status=InstallationBatch.Status.DRAFT,
    )
    batch.full_clean()
    batch.save()
    company.audit(
        staff, batch,
        "Draft installation prepared from accepted B2B enquiry; "
        "physical plants still require separate inventory and independent review.",
    )
    return batch
