"""Nonbinding real Plantupia B2B API, separate from legacy QA RFQs."""
import re
from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers
from rest_framework.response import Response

from .commerce_storefront_api import CommerceCustomerView
from . import commerce_business_real as business


class BusinessInquiryInput(serializers.Serializer):
    client_id = serializers.UUIDField()
    kind = serializers.ChoiceField(choices=("wholesale", "installation"))
    company_name = serializers.CharField(min_length=2, max_length=180)
    contact_name = serializers.CharField(min_length=2, max_length=160)
    phone = serializers.CharField(min_length=7, max_length=32)
    email = serializers.EmailField(required=False, allow_blank=True)
    city = serializers.CharField(min_length=2, max_length=120)
    sku_id = serializers.UUIDField(required=False, allow_null=True)
    quantity = serializers.IntegerField(min_value=1, max_value=10000)
    scope = serializers.CharField(min_length=20, max_length=2000)

    def validate_phone(self, value):
        if not re.fullmatch(r"[+0-9 ()-]{7,32}", value):
            raise serializers.ValidationError("Use a real contact phone with standard punctuation.")
        return value


class BusinessInquiriesView(CommerceCustomerView):
    def get(self, request):
        return Response({"results": [
            business.inquiry_json(record) for record in business.customer_inquiries(request.user)
        ]})

    def post(self, request):
        payload = BusinessInquiryInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            record = business.submit_inquiry(user=request.user, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(business.inquiry_json(record), status=201)


class AcceptBusinessQuoteInput(serializers.Serializer):
    quote_version = serializers.IntegerField(min_value=1)


class BusinessAcceptQuoteView(CommerceCustomerView):
    def post(self, request, inquiry_id):
        payload = AcceptBusinessQuoteInput(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            record = business.accept_quote(
                user=request.user, inquiry_id=inquiry_id,
                version=payload.validated_data["quote_version"],
            )
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(business.inquiry_json(record))
