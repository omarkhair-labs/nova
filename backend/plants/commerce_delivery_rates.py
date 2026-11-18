"""Optional simple merchant-managed city delivery charges for direct checkout.

No prices are supplied by code or seeded by migration. An unconfigured city
must never silently become free delivery. Price changes affect future orders.
"""
from decimal import Decimal, InvalidOperation

from django.core.exceptions import ValidationError
from django.db import transaction

from .commerce_models import CommerceDeliveryCityRate
from .commerce_storefront import cart_json, customer_only
from . import company_services as company


def city_key(city):
    value = str(city or "").strip()
    if not 2 <= len(value) <= 120:
        raise ValidationError("Enter a valid city or governorate.")
    return value.casefold()


def _valid_fee(amount):
    try:
        fee = Decimal(str(amount))
    except (ValueError, TypeError, InvalidOperation) as exc:
        raise ValidationError("Enter a valid EGP delivery fee.") from exc
    if (not fee.is_finite() or fee < 0 or fee > Decimal("9999999.99") or
            fee.as_tuple().exponent < -2):
        raise ValidationError("Delivery fee must be an EGP amount with at most 2 decimals.")
    return fee


@transaction.atomic
def set_city_rate(*, staff, city, fee, active):
    """Explicit company decision, never a fabricated tariff or silent fallback."""
    company.require(staff, "plants.change_commerceorder")
    key = city_key(city)
    amount = _valid_fee(fee)
    row = CommerceDeliveryCityRate.objects.select_for_update().filter(city_key=key).first()
    if row is None:
        row = CommerceDeliveryCityRate(
            city_key=key, city_label=city.strip(), fee=amount, active=active,
            updated_by=staff,
        )
    else:
        row.city_label = city.strip()
        row.fee = amount
        row.active = active
        row.revision += 1
        row.updated_by = staff
    row.full_clean()
    row.save()
    company.audit(staff, row,
        f"Delivery rate reviewed: {row.city_label}, EGP {row.fee}, "
        f"active={row.active}, revision={row.revision}.")
    return row


def rate_for_city(city, *, lock=False):
    qs = CommerceDeliveryCityRate.objects.filter(city_key=city_key(city), active=True)
    if lock:
        qs = qs.select_for_update()
    return qs.first()


def delivery_price(*, user, city, cart_fingerprint):
    """Server-side total for a persisted cart; never accept the client's amount."""
    customer_only(user)
    cart = cart_json(user)
    if (cart["fingerprint"] != cart_fingerprint or
            not cart["all_items_available"]):
        raise ValidationError("Your saved cart changed; refresh it before reviewing delivery.")
    key = city_key(city)
    rate = CommerceDeliveryCityRate.objects.filter(city_key=key, active=True).first()
    result = {
        "available": rate is not None,
        "city": city.strip(),
        "cart_fingerprint": cart["fingerprint"],
        "subtotal": cart["subtotal"],
        "currency": "EGP",
        "shipping_fee": None,
        "final_total": None,
        "rate_revision": None,
    }
    if rate:
        result.update(
            shipping_fee=str(rate.fee),
            final_total=str(Decimal(cart["subtotal"]) + rate.fee),
            rate_revision=rate.revision,
        )
    return result
