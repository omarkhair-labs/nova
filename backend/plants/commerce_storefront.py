"""Canonical customer Shop catalogue and persisted SKU cart.

This is the future sole customer Shop data source. Product content review,
actual publication, cart intent and paid order creation remain distinct states.
No test/fake catalogue or QA-only order is created by these operations.
"""
import hashlib
import json
import uuid
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied, ValidationError
from django.db import transaction
from django.db.models import Sum
from django.shortcuts import get_object_or_404

from .commerce_models import CommerceCartLine, CommerceProduct, CommerceSKU, CommerceStockMovement


def customer_only(user):
    if not user.is_authenticated or not user.is_active or user.is_staff:
        raise PermissionDenied("The customer Shop requires a signed-in customer account.")


def listed_skus():
    """A reviewed product is not automatically a published customer offer."""
    return CommerceSKU.objects.select_related("product", "product__reference_species").filter(
        product__catalog_review_status=CommerceProduct.CatalogReviewStatus.APPROVED,
        product__storefront_published_at__isnull=False,
    )


def available_units(sku_id):
    counted = CommerceStockMovement.objects.filter(sku_id=sku_id).aggregate(
        total=Sum("quantity_delta")
    )["total"] or 0
    # Reservations will subtract from available stock during the unified
    # checkout cut; do not pretend the cart has already reserved anything.
    return max(int(counted), 0)


def sku_json(sku, *, stock=None):
    product = sku.product
    if stock is None:
        stock = available_units(sku.pk)
    return {
        "id": str(sku.pk),  # exact sellable SKU identity, not a demo product id
        "product_id": str(product.pk),
        "name": product.name,
        "description": product.description,
        "kind": product.kind,
        "sku": sku.code,
        "family_key": product.catalog_key or str(product.pk),
        "variant_label": sku.option_label,
        "price": str(sku.price),
        "currency": sku.currency,
        "available_units": stock,
        "in_stock": stock > 0,
        "has_image": bool(product.cover_photo),
        "image_description": product.image_description,
        "photos": [
            {"id": str(photo.pk), "alt_text": photo.alt_text}
            for photo in product.gallery.all() if photo.image
        ],
        "reference_species": {
            "key": product.reference_species.key,
            "scientific_name": product.reference_species.scientific_name,
            "identity_verified": False,
        } if product.reference_species_id else None,
    }


def catalog_json(user, *, search=""):
    customer_only(user)
    skus = listed_skus().prefetch_related("product__gallery").order_by("product__name", "code")
    if search:
        from django.db.models import Q
        skus = skus.filter(Q(product__name__icontains=search[:100]) |
                           Q(code__icontains=search[:100]))
    rows = [sku_json(sku) for sku in skus[:100]]
    return {
        "results": rows,
        "source": "commerce_sku",
        "currency": "EGP",
        "checkout_ready": False,
        "notice": "Real reviewed product data. Orders and payments are not activated until production checkout is ready.",
    }


def cart_lines(user):
    return list(CommerceCartLine.objects.filter(customer=user)
                .select_related("sku", "sku__product", "sku__product__reference_species")
                .prefetch_related("sku__product__gallery").order_by("sku__code", "pk")[:21])


def cart_json(user):
    customer_only(user)
    lines = cart_lines(user)
    entries, digest_rows = [], []
    total = Decimal("0.00")
    all_available = bool(lines) and len(lines) <= 20
    for line in lines:
        sku = line.sku
        product = sku.product
        stock = available_units(sku.pk)
        listed = (
            product.catalog_review_status == CommerceProduct.CatalogReviewStatus.APPROVED
            and product.storefront_published_at is not None
        )
        valid = listed and sku.currency == "EGP" and stock >= line.quantity
        entries.append({
            "product": sku_json(sku, stock=stock),
            "quantity": line.quantity,
            "available": valid,
            "line_total": str(sku.price * line.quantity),
            "notice": "" if valid else "Item or quantity is no longer available. Please adjust your cart.",
        })
        total += sku.price * line.quantity
        all_available = all_available and valid
        digest_rows.append({
            "sku": str(sku.pk), "qty": line.quantity, "price": str(sku.price),
            "currency": sku.currency, "published": listed, "stock": stock,
        })
    fingerprint = hashlib.sha256(
        json.dumps(digest_rows, separators=(",", ":"), sort_keys=True).encode("utf-8")
    ).hexdigest()
    return {
        "results": entries,
        "fingerprint": fingerprint,
        "subtotal": str(total),
        "currency": "EGP",
        "all_items_available": bool(all_available),
        "checkout_ready": False,
        "notice": "Persistent cart; stock is not reserved and no order or payment has been created.",
    }


def set_cart_quantity(*, user, sku_id, quantity):
    """Set one exact commercial SKU quantity, lock customer and SKU.

    Repeated identical mutations are idempotent by desired state. Checkout
    rechecks stock atomically in the next release cut.
    """
    customer_only(user)
    try:
        sku_id = uuid.UUID(str(sku_id))
        quantity = int(quantity)
    except (ValueError, TypeError, AttributeError) as exc:
        raise ValidationError("Select a valid exact SKU and quantity.") from exc
    if not 0 <= quantity <= 10:
        raise ValidationError("Choose 0 to remove or between 1 and 10 units.")
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user.pk)
        if quantity == 0:
            CommerceCartLine.objects.filter(customer=user, sku_id=sku_id).delete()
            return cart_json(user)
        sku = get_object_or_404(
            CommerceSKU.objects.select_related("product").select_for_update(),
            pk=sku_id,
        )
        if (sku.product.catalog_review_status != CommerceProduct.CatalogReviewStatus.APPROVED
                or sku.product.storefront_published_at is None):
            raise ValidationError("This product is not currently available in the Shop.")
        if sku.currency != "EGP" or sku.price <= 0:
            raise ValidationError("The product price is not valid for purchase.")
        if available_units(sku.pk) < quantity:
            raise ValidationError("The selected quantity is not currently available.")
        existing = CommerceCartLine.objects.filter(customer=user, sku=sku).first()
        if existing is None:
            if CommerceCartLine.objects.filter(customer=user).count() >= 20:
                raise ValidationError("The cart supports up to 20 different exact SKU options.")
            entry = CommerceCartLine(customer=user, sku=sku, quantity=quantity)
            entry.full_clean()
            entry.save()
        elif existing.quantity != quantity:
            existing.quantity = quantity
            existing.full_clean()
            existing.save(update_fields=["quantity", "updated_at"])
        return cart_json(user)
