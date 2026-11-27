"""Real customer Shop read/cart API; no QA records or checkout mutations."""
import base64
import uuid

from django.core.exceptions import ValidationError as DjangoValidationError
from django.http import Http404
from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .commerce_models import CommerceProduct, CommerceProductPhoto
from . import commerce_storefront as shop


class CommerceCustomerView(APIView):
    permission_classes = [IsAuthenticated]

    def initial(self, request, *args, **kwargs):
        super().initial(request, *args, **kwargs)
        shop.customer_only(request.user)

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        response["Cache-Control"] = "private, no-store"
        response["X-Robots-Tag"] = "noindex, nofollow"
        return response


class CommerceCatalogView(CommerceCustomerView):
    """Published, checked, real SKU choices; never a fictional QA feed."""

    def get(self, request):
        search = request.query_params.get("q", "").strip()
        if len(search) > 100:
            raise serializers.ValidationError("Search text cannot exceed 100 characters.")
        return Response(shop.catalog_json(request.user, search=search))


class CommerceCartMutation(serializers.Serializer):
    sku_id = serializers.UUIDField()
    quantity = serializers.IntegerField(min_value=0, max_value=10)


class CommerceCartView(CommerceCustomerView):
    def get(self, request):
        return Response(shop.cart_json(request.user))

    def post(self, request):
        payload = CommerceCartMutation(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            result = shop.set_cart_quantity(user=request.user, **payload.validated_data)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(exc.messages) from exc
        return Response(result)


class CommerceCatalogPhotoView(CommerceCustomerView):
    """Private, bounded JSON photo bytes for existing Android image transport.

    Only the same actual published product IDs are visible in catalog and photos.
    """

    def get(self, request, product_id):
        product = get_object_or_404(
            CommerceProduct,
            pk=product_id,
            catalog_review_status=CommerceProduct.CatalogReviewStatus.APPROVED,
            storefront_published_at__isnull=False,
        )
        photo_id = request.query_params.get("photo_id")
        if photo_id:
            try:
                parsed = uuid.UUID(photo_id)
            except (ValueError, TypeError, AttributeError) as exc:
                raise Http404 from exc
            field = get_object_or_404(CommerceProductPhoto,
                pk=parsed, product=product).image
        else:
            field = product.cover_photo
        if not field:
            raise Http404
        try:
            with field.open("rb") as source:
                contents = source.read(4 * 1024 * 1024 + 1)
        except (OSError, ValueError) as exc:
            raise Http404 from exc
        if len(contents) > 4 * 1024 * 1024:
            raise serializers.ValidationError("Product image is too large to display.")
        return Response({"image_base64": base64.b64encode(contents).decode("ascii")})
