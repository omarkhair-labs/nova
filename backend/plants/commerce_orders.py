"""Single real Plantupia commercial order/quote/reservation lifecycle.

A customer's submission requests an exact delivery quote using the persisted
commercial SKU cart. The company quotes shipping. The customer then accepts
that specific quote, causing location-based stock reservation exactly once.
No money is moved and no courier delivery is claimed by these actions.
"""
import hashlib
import json
import uuid
from datetime import timedelta
from decimal import Decimal, InvalidOperation

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import Sum
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company
from .commerce_models import (
    CommerceCartLine, CommerceOrder, CommerceOrderLine, CommerceProduct,
    CommerceSKU, CommerceStockLocation, CommerceStockMovement,
    CommerceStockReservation,
)
from .commerce_storefront import cart_json, cart_lines, customer_only


def _uuid(value):
    try:
        return uuid.UUID(str(value))
    except (ValueError, TypeError, AttributeError) as exc:
        raise ValidationError("A valid request UUID is required.") from exc


def _request_fingerprint(data):
    fields = (
        "cart_fingerprint", "contact_name", "contact_phone", "city",
        "delivery_address", "building_details", "landmark", "customer_note",
        "delivery_latitude", "delivery_longitude", "delivery_pin_confirmed",
        "payment_preference",
    )
    clean = {key: str(data.get(key, "")).strip() for key in fields}
    return hashlib.sha256(
        json.dumps(clean, sort_keys=True, ensure_ascii=False).encode("utf-8")
    ).hexdigest()


def location_available(sku_id, location_id):
    counted = CommerceStockMovement.objects.filter(
        sku_id=sku_id, location_id=location_id,
    ).aggregate(n=Sum("quantity_delta"))["n"] or 0
    reserved = CommerceStockReservation.objects.filter(
        sku_id=sku_id, location_id=location_id, released_at__isnull=True,
    ).aggregate(n=Sum("quantity"))["n"] or 0
    return int(counted) - int(reserved)


def total_available(sku_id):
    """No optimistic reservations. Available units exclude all active holds."""
    counted = CommerceStockMovement.objects.filter(sku_id=sku_id).aggregate(
        n=Sum("quantity_delta")
    )["n"] or 0
    reserved = CommerceStockReservation.objects.filter(
        sku_id=sku_id, released_at__isnull=True,
    ).aggregate(n=Sum("quantity"))["n"] or 0
    return max(int(counted) - int(reserved), 0)


def order_json(order):
    lines = [
        {
            "sku_id": str(line.sku_id),
            "sku": line.sku_code_snapshot,
            "name": line.product_name_snapshot,
            "variant": line.option_snapshot,
            "quantity": line.quantity,
            "unit_price": str(line.unit_price_snapshot),
            "subtotal": str(line.line_subtotal_snapshot),
        }
        for line in order.lines.all()
    ]
    final_total = (order.subtotal + order.shipping_fee
                   if order.shipping_fee is not None else None)
    return {
        "id": str(order.pk),
        "status": order.status,
        "created_at": order.created_at.isoformat(),
        "updated_at": order.updated_at.isoformat(),
        "customer": {
            "name": order.contact_name, "phone": order.contact_phone,
            "city": order.city, "address": order.delivery_address,
            "building_details": order.building_details, "landmark": order.landmark,
            "latitude": str(order.delivery_latitude) if order.delivery_latitude is not None else None,
            "longitude": str(order.delivery_longitude) if order.delivery_longitude is not None else None,
            "pin_confirmed": order.delivery_pin_confirmed,
        },
        "payment_preference": order.payment_preference,
        "payment_status": order.payment_status,
        "shipping_fee": str(order.shipping_fee) if order.shipping_fee is not None else None,
        "shipping_quote_version": order.shipping_quote_version,
        "shipping_quote_expires_at": (
            order.shipping_quote_expires_at.isoformat()
            if order.shipping_quote_expires_at else None
        ),
        "subtotal": str(order.subtotal),
        "final_total": str(final_total) if final_total is not None else None,
        "stock_reserved": bool(order.confirmed_at and order.status not in (
            CommerceOrder.Status.CANCELLED, CommerceOrder.Status.DECLINED)),
        "charged": order.payment_status != CommerceOrder.PaymentStatus.UNPAID,
        "delivery_initiated": order.status in (
            CommerceOrder.Status.DISPATCHED, CommerceOrder.Status.DELIVERED),
        "lines": lines,
        "notice": (
            "The company must quote shipping; your order is not confirmed until you accept that exact total."
            if order.status == CommerceOrder.Status.QUOTE_PENDING
            else "Payment and delivery are not verified merely because a quote was accepted."
        ),
    }


def customer_orders(user):
    customer_only(user)
    return CommerceOrder.objects.filter(customer=user).prefetch_related(
        "lines").order_by("-created_at", "pk")[:100]


def customer_order(user, order_id):
    customer_only(user)
    return get_object_or_404(
        CommerceOrder.objects.prefetch_related("lines"),
        pk=order_id, customer=user,
    )


def request_shipping_quote(*, user, data):
    """Submit cart + delivery info once; no reservation or payment until quote acceptance."""
    customer_only(user)
    client_id = _uuid(data["client_id"])
    signature = _request_fingerprint(data)
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user.pk)
        replay = CommerceOrder.objects.filter(customer=user, client_id=client_id).first()
        if replay:
            if replay.fingerprint != signature:
                raise ValidationError("The same retry ID was already used for different order details.")
            return replay

        cart = cart_json(user)
        if (cart["fingerprint"] != data["cart_fingerprint"] or
                not cart["all_items_available"]):
            raise ValidationError("Your cart or the available items changed. Review the latest cart.")
        lines = cart_lines(user)
        if not 1 <= len(lines) <= 20:
            raise ValidationError("Add between 1 and 20 exact SKU options before requesting a quote.")

        order = CommerceOrder(
            customer=user, client_id=client_id, fingerprint=signature,
            cart_fingerprint=cart["fingerprint"],
            contact_name=data["contact_name"].strip(),
            contact_phone=data["contact_phone"].strip(),
            city=data["city"].strip(),
            delivery_address=data["delivery_address"].strip(),
            building_details=data.get("building_details", "").strip(),
            landmark=data.get("landmark", "").strip(),
            customer_note=data.get("customer_note", "").strip(),
            delivery_latitude=data["delivery_latitude"],
            delivery_longitude=data["delivery_longitude"],
            delivery_pin_confirmed=True,
            payment_preference=data["payment_preference"],
            subtotal=Decimal(cart["subtotal"]),
        )
        order.full_clean()
        order.save()
        CommerceOrderLine.objects.bulk_create([
            CommerceOrderLine(
                order=order, sku=line.sku,
                product_name_snapshot=line.sku.product.name,
                sku_code_snapshot=line.sku.code,
                option_snapshot=line.sku.option_label,
                unit_price_snapshot=line.sku.price,
                quantity=line.quantity,
                line_subtotal_snapshot=line.sku.price * line.quantity,
            )
            for line in lines
        ])
        company.audit(user, order,
            "Customer requested an actual delivery quote. No stock reserved, money received, or shipment.")
        return order


def quote_shipping(*, staff, order_id, fee, version):
    company.require(staff, "plants.change_commerceorder")
    try:
        shipping_fee = Decimal(str(fee))
        expected = int(version)
    except (InvalidOperation, ValueError, TypeError) as exc:
        raise ValidationError("Enter a valid EGP shipping fee and current quote version.") from exc
    if (not shipping_fee.is_finite() or shipping_fee < 0 or
            shipping_fee > Decimal("9999999.99") or shipping_fee.as_tuple().exponent < -2):
        raise ValidationError("The shipping fee must be a nonnegative EGP amount (2 decimals maximum).")

    with transaction.atomic():
        order = get_object_or_404(CommerceOrder.objects.select_for_update(), pk=order_id)
        if order.status not in (CommerceOrder.Status.QUOTE_PENDING, CommerceOrder.Status.QUOTED):
            raise ValidationError("Only a pending or expired shipping quote can be revised.")
        if order.shipping_quote_version != expected:
            raise ValidationError("The quote changed. Reload before setting a shipping fee.")
        order.shipping_fee = shipping_fee
        order.shipping_quote_version += 1
        order.shipping_quote_expires_at = timezone.now() + timedelta(days=3)
        order.status = CommerceOrder.Status.QUOTED
        order.quoted_by = staff
        order.quoted_at = timezone.now()
        order.last_status_at = order.quoted_at
        order.save(update_fields=[
            "shipping_fee", "shipping_quote_version", "shipping_quote_expires_at",
            "status", "quoted_by", "quoted_at", "last_status_at", "updated_at",
        ])
        company.audit(staff, order, f"Company quoted exact delivery fee EGP {shipping_fee}; awaiting customer consent.")
        return order


def accept_shipping_quote(*, user, order_id, quote_version):
    """Confirm exact shipping total and atomically reserve from actual warehouse locations."""
    customer_only(user)
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user.pk)
        order = get_object_or_404(
            CommerceOrder.objects.select_for_update().prefetch_related("lines"),
            pk=order_id, customer=user,
        )
        if int(quote_version) != order.shipping_quote_version:
            raise ValidationError("Your shipping quote changed. Review the latest total.")
        if order.status == CommerceOrder.Status.CONFIRMED:
            return order  # idempotent same-version acceptance
        if (order.status != CommerceOrder.Status.QUOTED or order.shipping_fee is None
                or not order.shipping_quote_expires_at
                or order.shipping_quote_expires_at <= timezone.now()):
            raise ValidationError("This delivery quote is not available; ask the team for a fresh quote.")
        lines = list(order.lines.all())
        ids = sorted({line.sku_id for line in lines})
        locked_skus = {
            s.pk: s for s in CommerceSKU.objects.select_for_update()
            .select_related("product").filter(pk__in=ids).order_by("pk")
        }
        locations = list(CommerceStockLocation.objects.select_for_update().filter(
            active=True).order_by("code", "pk"))
        planned = []
        for line in lines:
            sku = locked_skus.get(line.sku_id)
            if (sku is None or sku.product.catalog_review_status !=
                    CommerceProduct.CatalogReviewStatus.APPROVED
                    or sku.product.storefront_published_at is None
                    or sku.price != line.unit_price_snapshot or sku.currency != "EGP"):
                raise ValidationError("A product or its price changed after your order request. No stock was reserved.")
            needed = line.quantity
            for location in locations:
                balance = max(location_available(sku.pk, location.pk), 0)
                take = min(needed, balance)
                if take:
                    planned.append(CommerceStockReservation(
                        order=order, sku=sku, location=location, quantity=take,
                    ))
                    needed -= take
                if not needed:
                    break
            if needed:
                raise ValidationError(f"Not enough actual stock for SKU {sku.code}. No order was confirmed.")

        # All SKU/location locks are held; no other checkout or count can take this stock.
        CommerceStockReservation.objects.bulk_create(planned)
        order.status = CommerceOrder.Status.CONFIRMED
        order.confirmed_at = timezone.now()
        order.last_status_at = order.confirmed_at
        order.save(update_fields=["status", "confirmed_at", "last_status_at", "updated_at"])
        # Do not wipe newly edited cart contents; only remove unchanged quote lines.
        for line in lines:
            CommerceCartLine.objects.filter(
                customer=user, sku_id=line.sku_id, quantity=line.quantity,
            ).delete()
        company.audit(user, order,
            "Customer accepted exact subtotal+shipping; actual location-based stock reserved. Payment remains unpaid.")
        return order


def cancel_customer_order(*, user, order_id):
    """Customer may cancel an unfulfilled quote/order, releasing exact reserved units."""
    customer_only(user)
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user.pk)
        order = get_object_or_404(
            CommerceOrder.objects.select_for_update(), pk=order_id, customer=user,
        )
        if order.status == CommerceOrder.Status.CANCELLED:
            return order
        if order.status not in (
            CommerceOrder.Status.QUOTE_PENDING,
            CommerceOrder.Status.QUOTED,
            CommerceOrder.Status.CONFIRMED,
        ):
            raise ValidationError("Contact support for orders already being prepared or delivered.")
        lines = list(order.lines.all())
        # Lock SKUs in the same order as checkout and counted-stock corrections.
        list(CommerceSKU.objects.select_for_update().filter(
            pk__in=[line.sku_id for line in lines]).order_by("pk"))
        CommerceStockReservation.objects.filter(
            order=order, released_at__isnull=True,
        ).update(released_at=timezone.now())
        order.status = CommerceOrder.Status.CANCELLED
        order.cancelled_at = timezone.now()
        order.last_status_at = order.cancelled_at
        order.save(update_fields=["status", "cancelled_at", "last_status_at", "updated_at"])
        company.audit(user, order,
            "Customer cancelled commercial order. Active reservations released; no refund inferred.")
        return order


def decline_quote_request(*, staff, order_id):
    """Explicit operator decline before a customer has accepted a shipping quote."""
    company.require(staff, "plants.change_commerceorder")
    with transaction.atomic():
        order = get_object_or_404(
            CommerceOrder.objects.select_for_update(), pk=order_id,
        )
        if order.status not in (CommerceOrder.Status.QUOTE_PENDING, CommerceOrder.Status.QUOTED):
            raise ValidationError("Only an unconfirmed order can be declined without a return process.")
        order.status = CommerceOrder.Status.DECLINED
        order.last_status_at = timezone.now()
        order.save(update_fields=["status", "last_status_at", "updated_at"])
        company.audit(staff, order,
            "Operator declined shipping quote request. No stock reserved, delivery or payment.")
        return order
