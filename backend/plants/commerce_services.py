"""Staff-only commercial draft catalogue operations. No checkout, reservation or payment actions."""
import hashlib
import json
import uuid

from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied, ValidationError
from django.db import transaction
from django.db.models import Sum

from . import company_services as company
from .commerce_models import CommerceSKU, CommerceStockLocation, CommerceStockMovement


def stock_balance(sku_id, location_id=None):
    qs = CommerceStockMovement.objects.filter(sku_id=sku_id)
    if location_id is not None:
        qs = qs.filter(location_id=location_id)
    return qs.aggregate(total=Sum("quantity_delta"))["total"] or 0


def record_counted_stock(*, staff, sku_id, location_id, quantity_delta, reason, reference_note, client_id):
    """One audited physical count change, never installed-plant stock or sold units.

    Lock the actor + SKU to serialize duplicate retries and competing stock decrements.
    Reads only the movement ledger. This operation does NOT reserve stock for orders.
    """
    company.require(staff, "plants.add_commercestockmovement")
    try:
        identity = uuid.UUID(str(client_id))
        delta = int(quantity_delta)
    except (TypeError, ValueError, AttributeError) as exc:
        raise ValidationError("A valid quantity and client UUID are required.") from exc
    if delta == 0 or abs(delta) > 100000:
        raise ValidationError("Count changes must be nonzero and at most 100,000 units per entry.")
    if reason not in CommerceStockMovement.Reason.values:
        raise ValidationError("Choose an approved stock-count reason.")
    if reason == CommerceStockMovement.Reason.OPENING and delta < 0:
        raise ValidationError("An opening count cannot be negative.")
    note = (reference_note or "").strip()
    if not note or len(note) > 200:
        raise ValidationError("A stock count needs a reference note (200 characters maximum).")

    payload = {"sku": str(sku_id), "location": str(location_id),
               "delta": delta, "reason": reason, "note": note}
    fingerprint = hashlib.sha256(json.dumps(payload, sort_keys=True).encode("utf-8")).hexdigest()
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=staff.pk)
        receipt = CommerceStockMovement.objects.filter(recorded_by=staff, client_id=identity).first()
        if receipt is not None:
            if receipt.fingerprint != fingerprint:
                raise ValidationError("This retry identity was already used for a different stock change.")
            return receipt

        sku = CommerceSKU.objects.select_for_update().get(pk=sku_id)
        location = CommerceStockLocation.objects.get(pk=location_id, active=True)
        current = stock_balance(sku.pk, location.pk)
        if current + delta < 0:
            raise ValidationError("The stock count cannot become negative for this SKU/location.")
        movement = CommerceStockMovement(
            sku=sku, location=location, recorded_by=staff,
            quantity_delta=delta, reason=reason,
            reference_note=note, client_id=identity, fingerprint=fingerprint,
        )
        movement.full_clean()
        movement.save()
        company.audit(staff, movement, "Commerce counted stock change (draft inventory only; no sale or reservation)")
        return movement
