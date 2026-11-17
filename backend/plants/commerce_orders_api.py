"""Canonical customer Shop: direct order for known city fees, quote for exceptions.

Prices and inventory are verified by the backend. A successful order does not
mean payment or physical dispatch.
"""
import re
from decimal import Decimal, ROUND_HALF_UP

from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers
from rest_framework.response import Response

from .commerce_storefront_api import CommerceCustomerView
from . import commerce_orders as orders, commerce_payments as payments, commerce_returns as returns


class DeliveryCoordinateField(serializers.DecimalField):
    """Normalize map SDK doubles to our stored seven-decimal precision."""
    def __init__(self, *, limit, **kwargs):
        self.coordinate_limit = Decimal(limit)
        super().__init__(max_digits=10, decimal_places=7, **kwargs)

    def validate_precision(self, value):
        # DRF has already rejected non-finite values. Check the original range
        # before rounding so an invalid pin cannot round onto a valid boundary.
        if not -self.coordinate_limit <= value <= self.coordinate_limit:
            raise serializers.ValidationError("Choose a valid delivery map coordinate.")
        rounded = value.quantize(Decimal("0.0000001"), rounding=ROUND_HALF_UP)
        return super().validate_precision(rounded)


class RealOrderQuoteInput(serializers.Serializer):
    client_id = serializers.UUIDField()
    cart_fingerprint = serializers.RegexField(regex=r"^[0-9a-f]{64}$", max_length=64)
    contact_name = serializers.CharField(min_length=2, max_length=160)
    contact_phone = serializers.CharField(min_length=7, max_length=32)
    city = serializers.CharField(min_length=2, max_length=120)
    delivery_address = serializers.CharField(min_length=5, max_length=320)
    delivery_latitude = DeliveryCoordinateField(limit=90)
    delivery_longitude = DeliveryCoordinateField(limit=180)
    delivery_pin_confirmed = serializers.BooleanField()
    building_details = serializers.CharField(max_length=180, required=False, allow_blank=True)
    landmark = serializers.CharField(max_length=180, required=False, allow_blank=True)
    customer_note = serializers.CharField(max_length=500, required=False, allow_blank=True)
    payment_preference = serializers.ChoiceField(choices=("cod", "manual_transfer"))

    def validate_contact_phone(self, value):
        if not re.fullmatch(r"[+0-9 ()-]{7,32}", value):
            raise serializers.ValidationError("Use standard phone digits and punctuation.")
        return value

    def validate(self, attrs):
        if not (-90 <= attrs["delivery_latitude"] <= 90 and
                -180 <= attrs["delivery_longitude"] <= 180 and
                attrs["delivery_pin_confirmed"]):
            raise serializers.ValidationError("Confirm the actual delivery pin on the map.")
        return attrs


class CommerceOrdersView(CommerceCustomerView):
    def get(self, request):
        return Response({"results": [orders.order_json(o) for o in orders.customer_orders(request.user)]})

    def post(self, request):
        payload = RealOrderQuoteInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            order = orders.request_shipping_quote(user=request.user, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order), status=201)


class CommerceDeliveryPriceView(CommerceCustomerView):
    """Authenticated, server-calculated current total; no speculative price."""
    def get(self, request):
        city = str(request.query_params.get("city", "")).strip()
        fingerprint = str(request.query_params.get("cart_fingerprint", "")).strip()
        if not re.fullmatch(r"[0-9a-f]{64}", fingerprint):
            raise serializers.ValidationError("Refresh the saved cart before reviewing delivery.")
        from .commerce_delivery_rates import delivery_price
        try:
            price = delivery_price(
                user=request.user, city=city, cart_fingerprint=fingerprint,
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(price)


class RealDirectOrderInput(RealOrderQuoteInput):
    rate_revision = serializers.IntegerField(min_value=1)


class CommerceDirectOrderView(CommerceCustomerView):
    """One Place Order action: no staff-quote loop for known-fee cities."""
    def post(self, request):
        payload = RealDirectOrderInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            order = orders.place_direct_order(
                user=request.user, data=payload.validated_data,
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order), status=201)


class CommerceOrderDetailView(CommerceCustomerView):
    def get(self, request, order_id):
        return Response(orders.order_json(orders.customer_order(request.user, order_id)))


class AcceptShippingQuoteInput(serializers.Serializer):
    quote_version = serializers.IntegerField(min_value=1)


class CommerceOrderAcceptQuoteView(CommerceCustomerView):
    def post(self, request, order_id):
        payload = AcceptShippingQuoteInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            order = orders.accept_shipping_quote(
                user=request.user, order_id=order_id,
                quote_version=payload.validated_data["quote_version"],
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order))


class CommerceOrderCancelView(CommerceCustomerView):
    def post(self, request, order_id):
        try:
            order = orders.cancel_customer_order(user=request.user, order_id=order_id)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order))

class ManualTransferReferenceInput(serializers.Serializer):
    reference = serializers.CharField(min_length=4, max_length=120, trim_whitespace=True)


class CommerceOrderTransferClaimView(CommerceCustomerView):
    def post(self, request, order_id):
        payload = ManualTransferReferenceInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            order = payments.claim_manual_transfer(
                user=request.user, order_id=order_id,
                reference=payload.validated_data["reference"],
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order))

class RealOrderReturnInput(serializers.Serializer):
    reason = serializers.ChoiceField(choices=("damaged", "incorrect_item", "quality_issue", "other"))
    note = serializers.CharField(min_length=10, max_length=500, trim_whitespace=True)


class CommerceOrderReturnView(CommerceCustomerView):
    def post(self, request, order_id):
        payload = RealOrderReturnInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            order = returns.request_return(
                user=request.user, order_id=order_id,
                reason=payload.validated_data["reason"],
                note=payload.validated_data["note"],
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(orders.order_json(order))
