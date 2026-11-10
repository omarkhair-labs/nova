"""Actual commercial checkout: quote -> customer accepts exact total -> reserve stock.

No payment provider or courier event is fabricated by these customer actions.
"""
import re

from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers
from rest_framework.response import Response

from .commerce_storefront_api import CommerceCustomerView
from . import commerce_orders as orders, commerce_payments as payments


class RealOrderQuoteInput(serializers.Serializer):
    client_id = serializers.UUIDField()
    cart_fingerprint = serializers.RegexField(regex=r"^[0-9a-f]{64}$", max_length=64)
    contact_name = serializers.CharField(min_length=2, max_length=160)
    contact_phone = serializers.CharField(min_length=7, max_length=32)
    city = serializers.CharField(min_length=2, max_length=120)
    delivery_address = serializers.CharField(min_length=5, max_length=320)
    delivery_latitude = serializers.DecimalField(max_digits=10, decimal_places=7)
    delivery_longitude = serializers.DecimalField(max_digits=10, decimal_places=7)
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
