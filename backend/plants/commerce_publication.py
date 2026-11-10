"""One real commercial catalogue: reviewed content, not a second QA storefront.

Approval is an internal product-data decision. No customer API uses this flag
until the SAME CommerceSKU-backed cart, checkout and fulfillment are complete.
"""
from django.core.exceptions import ValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company
from .commerce_models import CommerceProduct, CommerceSKU


def product_readiness_issues(product):
    """Give the staff member precise reasons a product is not catalog-ready."""
    issues = []
    if not product.name.strip():
        issues.append("Add the actual product name.")
    if not product.description.strip():
        issues.append("Add a checked product description.")
    if not product.cover_photo or not product.image_description.strip():
        issues.append("Add the actual cover photograph and accessible description.")
    else:
        try:
            if not product.cover_photo.storage.exists(product.cover_photo.name):
                issues.append("The cover photograph is missing from persistent storage.")
        except (OSError, ValueError):
            issues.append("The cover photograph cannot be verified in storage.")
    skus = list(product.variants.all())
    if not skus:
        issues.append("Create at least one exact priced SKU / variant.")
    for sku in skus:
        if sku.currency != "EGP" or sku.price <= 0:
            issues.append(f"Fix the price and currency for SKU {sku.code}.")
        if not sku.code.strip():
            issues.append("Every product variant needs an exact SKU code.")
    for photo in product.gallery.all():
        if not photo.alt_text.strip():
            issues.append("Describe each product gallery image.")
        try:
            if not photo.image or not photo.image.storage.exists(photo.image.name):
                issues.append("A gallery image is missing from persistent storage.")
        except (OSError, ValueError):
            issues.append("A gallery image cannot be verified in storage.")
    return issues


@transaction.atomic
def review_catalog_product(*, staff, product_id, action):
    """Approve actual content or return it to draft; never initiate any sale."""
    company.require(staff, "plants.change_commerceproduct")
    if action not in ("approve", "reopen"):
        raise ValidationError("Choose approve or reopen product review.")
    product = get_object_or_404(
        CommerceProduct.objects.select_for_update(), pk=product_id,
    )
    if action == "approve":
        issues = product_readiness_issues(product)
        if issues:
            raise ValidationError(issues)
        product.catalog_review_status = CommerceProduct.CatalogReviewStatus.APPROVED
        product.catalog_reviewed_by = staff
        product.catalog_reviewed_at = timezone.now()
        action_note = "Approved commercial product source data; not a customer listing or a sale."
    else:
        product.catalog_review_status = CommerceProduct.CatalogReviewStatus.DRAFT
        product.catalog_reviewed_by = None
        product.catalog_reviewed_at = None
        product.storefront_published_at = None
        action_note = "Returned commercial product to review and removed any existing customer listing."
    product.save(update_fields=[
        "catalog_review_status", "catalog_reviewed_by", "catalog_reviewed_at",
        "storefront_published_at", "updated_at",
    ])
    company.audit(staff, product, action_note)
    return product


def invalidate_catalog_review(*, staff, product_id, reason):
    """A changed product or SKU must be checked again before future publication."""
    product = CommerceProduct.objects.select_for_update().get(pk=product_id)
    if product.catalog_review_status == CommerceProduct.CatalogReviewStatus.DRAFT:
        return False
    product.catalog_review_status = CommerceProduct.CatalogReviewStatus.DRAFT
    product.catalog_reviewed_by = None
    product.catalog_reviewed_at = None
    product.storefront_published_at = None
    product.save(update_fields=[
        "catalog_review_status", "catalog_reviewed_by", "catalog_reviewed_at",
        "storefront_published_at", "updated_at",
    ])
    company.audit(staff, product, f"Commercial product returned to review after {reason}.")
    return True
