"""Anonymous read-only window onto explicitly approved AND published Commerce SKUs.

The public web never uses customer sessions, private carts, signed tokens, QA
products or service credentials. This does not provide checkout/order creation.
"""
from io import BytesIO

from django.http import Http404, HttpResponse
from django.shortcuts import get_object_or_404
from PIL import Image, UnidentifiedImageError
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .commerce_models import CommerceProduct
from .commerce_storefront import listed_skus, available_units


class PublicCommerceCatalogView(APIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    http_method_names = ["get", "head", "options"]

    def get(self, request):
        # Small, intentionally curated editorial gateway, not a customer cart.
        # Never reveal exact warehouse counts, staff details or product drafts.
        skus = (listed_skus()
                .filter(currency="EGP", price__gt=0)
                .order_by("product__name", "code")
                [:36])
        rows = []
        for sku in skus:
            product = sku.product
            rows.append({
                "sku_id": str(sku.pk),
                "product_id": str(product.pk),
                "name": product.name,
                "description": product.description,
                "kind": product.kind,
                "option": sku.option_label,
                "price": str(sku.price),
                "currency": sku.currency,
                "in_stock": available_units(sku.pk) > 0,
                "has_image": bool(product.cover_photo),
                "image_alt": product.image_description if product.cover_photo else "",
            })
        response = Response({
            "results": rows,
            "source": "reviewed_published_commerce_skus",
            "checkout_available_on_web": False,
        })
        response["Cache-Control"] = "no-store"
        response["X-Content-Type-Options"] = "nosniff"
        return response


class PublicCommerceCoverView(APIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    http_method_names = ["get", "head", "options"]

    def get(self, request, product_id):
        product = get_object_or_404(
            CommerceProduct,
            pk=product_id,
            catalog_review_status=CommerceProduct.CatalogReviewStatus.APPROVED,
            storefront_published_at__isnull=False,
        )
        if not product.cover_photo:
            raise Http404
        try:
            with product.cover_photo.open("rb") as source:
                content = source.read(4 * 1024 * 1024 + 1)
            if not content or len(content) > 4 * 1024 * 1024:
                raise Http404
            with Image.open(BytesIO(content)) as image:
                if image.format not in ("JPEG", "PNG", "WEBP"):
                    raise Http404
                kind = image.format
                image.verify()
        except (OSError, ValueError, UnidentifiedImageError, Image.DecompressionBombError) as exc:
            raise Http404 from exc
        media_type = {"JPEG": "image/jpeg", "PNG": "image/png",
                      "WEBP": "image/webp"}[kind]
        response = HttpResponse(content, content_type=media_type)
        response["Cache-Control"] = "public, max-age=60"
        response["X-Content-Type-Options"] = "nosniff"
        return response
