"""Explicit noncommercial QA B2C request API — NEVER a live checkout."""
import re

from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from . import commerce_checkout as checkout


class QARequestView(APIView):
    permission_classes = [IsAuthenticated]

    def initial(self, request, *args, **kwargs):
        checkout.require_qa()
        super().initial(request, *args, **kwargs)

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        response["Cache-Control"] = "private, no-store"
        response["X-Robots-Tag"] = "noindex, nofollow"
        return response


class QALineInput(serializers.Serializer):
    sku_id = serializers.UUIDField()
    quantity = serializers.IntegerField(min_value=1, max_value=10)


class QAOrderRequestInput(serializers.Serializer):
    client_id = serializers.UUIDField()
    lines = QALineInput(many=True, min_length=1, max_length=20)
    contact_name = serializers.CharField(max_length=160, min_length=2, trim_whitespace=True)
    contact_phone = serializers.CharField(max_length=32, trim_whitespace=True)
    city = serializers.CharField(max_length=120, min_length=2, trim_whitespace=True)
    delivery_address = serializers.CharField(max_length=320, min_length=5, trim_whitespace=True)
    customer_note = serializers.CharField(max_length=500, allow_blank=True, required=False, trim_whitespace=True)
    delivery_latitude = serializers.DecimalField(max_digits=10, decimal_places=7, required=False)
    delivery_longitude = serializers.DecimalField(max_digits=10, decimal_places=7, required=False)
    delivery_pin_confirmed = serializers.BooleanField(default=False)
    building_details = serializers.CharField(max_length=180, required=False, allow_blank=True, trim_whitespace=True)
    landmark = serializers.CharField(max_length=180, required=False, allow_blank=True, trim_whitespace=True)
    payment_preference = serializers.ChoiceField(choices=("cod", "manual_transfer"))
    confirm_qa_no_payment = serializers.BooleanField()

    def validate_contact_phone(self, value):
        if not re.fullmatch(r"[+0-9 ()-]{7,32}", value):
            raise serializers.ValidationError("Use a contact number with digits and standard phone punctuation.")
        return value

    def validate_confirm_qa_no_payment(self, value):
        if value is not True:
            raise serializers.ValidationError(
                "Confirm this is an isolated QA request, not a purchase or payment."
            )
        return value

    def validate(self, attrs):
        lat = attrs.get("delivery_latitude")
        lon = attrs.get("delivery_longitude")
        if (lat is None) != (lon is None):
            raise serializers.ValidationError("A selected map pin needs both coordinates.")
        if lat is not None:
            if not (-90 <= lat <= 90 and -180 <= lon <= 180):
                raise serializers.ValidationError("Invalid geographic coordinate range.")
            if not attrs.get("delivery_pin_confirmed"):
                raise serializers.ValidationError("Confirm your exact pin before submission.")
        elif attrs.get("delivery_pin_confirmed"):
            raise serializers.ValidationError("Pin confirmation requires a map location.")
        return attrs

    def validate_lines(self, value):
        ids = [entry["sku_id"] for entry in value]
        if len(set(ids)) != len(ids):
            raise serializers.ValidationError("A SKU must appear once per QA request.")
        return value


class QACommerceRequestsView(QARequestView):
    def get(self, request):
        orders = checkout.list_customer_requests(request.user)
        return Response({
            "qa_only": True,
            "results": [checkout.customer_order_json(order) for order in orders],
        })

    def post(self, request):
        serializer = QAOrderRequestInput(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            order = checkout.submit_qa_request(request.user, serializer.validated_data)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(checkout.customer_order_json(order), status=201)


class QACommerceRequestView(QARequestView):
    def get(self, request, order_id):
        return Response(checkout.customer_order_json(
            checkout.get_customer_request(request.user, order_id)
        ))


class QACommerceCatalogView(QARequestView):
    """Minimal opt-in QA catalogue for Android form testing; never a public offer."""
    def get(self, request):
        from .commerce_models import CommerceSKU
        from .commerce_services import stock_balance
        checkout.require_qa()
        if request.user.is_staff or not request.user.is_active:
            from django.core.exceptions import PermissionDenied
            raise PermissionDenied
        skus = CommerceSKU.objects.select_related("product").order_by("code")[:100]
        results = []
        for sku in skus:
            balance = stock_balance(sku.pk)
            if balance < 1:
                continue
            results.append({
                "id": str(sku.pk), "sku": sku.code, "product_name": sku.product.name,
                "option": sku.option_label, "price": str(sku.price),
                "currency": sku.currency, "qa_counted_stock": balance,
                "kind": sku.product.kind, "qa_only": True,
            })
        return Response({
            "results": results, "qa_only": True,
            "notice": "Internal QA catalogue only. Prices are provisional; no checkout, payment or delivery."
        })


class QACustomerOrderActionInput(serializers.Serializer):
    action = serializers.ChoiceField(choices=("cancel", "return"))


class QACommerceRequestActionView(QARequestView):
    """Owner-scoped in-app cancellation/return request. No money is moved."""
    def post(self, request, order_id):
        from .commerce_tracking import customer_request_action
        serializer = QACustomerOrderActionInput(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            order = customer_request_action(
                customer=request.user, order_id=order_id,
                action=serializer.validated_data["action"],
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(checkout.customer_order_json(order))


class BusinessRFQInput(serializers.Serializer):
    client_id = serializers.UUIDField()
    company_name = serializers.CharField(max_length=180, min_length=2, trim_whitespace=True)
    contact_name = serializers.CharField(max_length=160, min_length=2, trim_whitespace=True)
    phone = serializers.CharField(max_length=32, min_length=7, trim_whitespace=True)
    email = serializers.EmailField(required=False, allow_blank=True)
    city = serializers.CharField(max_length=120, min_length=2)
    sku_id = serializers.UUIDField(required=False, allow_null=True)
    quantity = serializers.IntegerField(min_value=1, max_value=10000)
    scope = serializers.CharField(max_length=1500, min_length=10)
    confirm_qa_no_contract = serializers.BooleanField()

    def validate_confirm_qa_no_contract(self, value):
        if value is not True:
            raise serializers.ValidationError("Confirm nonbinding QA request.")
        return value


class QACommerceRFQsView(QARequestView):
    def get(self, request):
        from .commerce_rfq import customer_list, rfq_json
        return Response({"qa_only": True,
            "results": [rfq_json(r) for r in customer_list(request.user)]})

    def post(self, request):
        from .commerce_rfq import submit, rfq_json
        payload = BusinessRFQInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            rfq = submit(requester=request.user, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(rfq_json(rfq), status=201)


class QACommerceRFQAcceptView(QARequestView):
    def post(self, request, rfq_id):
        from .commerce_rfq import accept, rfq_json
        data = serializers.Serializer(data=request.data)
        data.is_valid(raise_exception=True)
        try:
            rfq = accept(requester=request.user, rfq_id=rfq_id)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(rfq_json(rfq))
