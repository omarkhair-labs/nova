"""One commercial SKU source of truth for customer catalogue/cart; no demo orders."""
from io import BytesIO
from tempfile import TemporaryDirectory
import uuid

from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image

from .commerce_models import (
    CommerceCartLine, CommerceProduct, CommerceProductPhoto, CommerceSKU,
    CommerceStockLocation, CommerceOrderRequest,
)
from .commerce_publication import review_catalog_product
from .commerce_services import record_counted_stock
from .models import CatalogProduct, DemoCartLine, DemoOrder, PlantUnit
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CanonicalCommerceCustomerTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        media = override_settings(MEDIA_ROOT=temporary.name)
        media.enable()
        self.addCleanup(media.disable)
        self.product_real = CommerceProduct.objects.create(
            name="Real indoor fern", description="Actual approved product description",
            kind="plant", created_by=self.staff, catalog_key="real-fern",
        )
        self.sku_small = CommerceSKU.objects.create(
            product=self.product_real, code="FERN-S-REAL", option_label="Small", price="175.00",
        )
        self.sku_large = CommerceSKU.objects.create(
            product=self.product_real, code="FERN-L-REAL", option_label="Large", price="250.00",
        )
        self.location = CommerceStockLocation.objects.create(code="warehouse-real", name="Warehouse")
        for sku in (self.sku_small, self.sku_large):
            record_counted_stock(staff=self.staff, sku_id=sku.pk,
                                 location_id=self.location.pk, quantity_delta=7,
                                 reason="opening", reference_note="Physical count",
                                 client_id=uuid.uuid4())
        picture = Image.new("RGB", (144, 160), "#537b4d")
        raw = BytesIO()
        picture.save(raw, "JPEG")
        self.product_real.cover_photo.save("fern-cover.jpg", ContentFile(raw.getvalue()), save=False)
        self.product_real.image_description = "A real fern in its nursery container"
        self.product_real.save(update_fields=["cover_photo", "image_description"])
        self.image_bytes = raw.getvalue()
        self.photo = CommerceProductPhoto(product=self.product_real, alt_text="Close-up of real fern leaves", position=1)
        self.photo.image.save("fern-leaves.jpg", ContentFile(self.image_bytes))
        self.catalog_url = reverse("plantupia-real-commerce-catalog")
        self.cart_url = reverse("plantupia-real-commerce-cart")

    def publish_after_review(self):
        review_catalog_product(staff=self.staff, product_id=self.product_real.pk, action="approve")
        self.product_real.refresh_from_db()
        self.product_real.storefront_published_at = timezone.now()
        self.product_real.save(update_fields=["storefront_published_at"])

    def add_to_cart(self, sku, quantity):
        return self.api.post(self.cart_url, {
            "sku_id": str(sku.pk), "quantity": quantity,
        }, format="json")

    def test_review_alone_does_not_publish_any_product_or_allow_cart(self):
        review_catalog_product(staff=self.staff, product_id=self.product_real.pk, action="approve")
        catalog = self.api.get(self.catalog_url)
        self.assertEqual(catalog.status_code, 200)
        self.assertEqual(catalog.data["results"], [])
        self.assertEqual(self.add_to_cart(self.sku_small, 1).status_code, 400)
        self.assertFalse(CommerceCartLine.objects.exists())
        self.assertFalse(DemoCartLine.objects.exists())

    def test_canonical_product_variants_show_one_product_with_exact_skus(self):
        self.publish_after_review()
        r = self.api.get(self.catalog_url)
        self.assertEqual(r.status_code, 200, r.data)
        self.assertEqual(len(r.data["results"]), 2)
        self.assertEqual({line["id"] for line in r.data["results"]},
                         {str(self.sku_small.pk), str(self.sku_large.pk)})
        self.assertEqual({line["product_id"] for line in r.data["results"]},
                         {str(self.product_real.pk)})
        self.assertTrue(all(line["available_units"] == 7 for line in r.data["results"]))
        self.assertTrue(all(line["family_key"] == "real-fern" for line in r.data["results"]))
        self.assertEqual(r.data["results"][0]["photos"][0]["alt_text"], "Close-up of real fern leaves")
        self.assertFalse(r.data["checkout_ready"])
        self.assertNotIn("FICTIONAL", str(r.data))
        self.assertNotIn("qa_only", str(r.data))
        search = self.api.get(self.catalog_url + "?q=FERN-L-REAL")
        self.assertEqual(len(search.data["results"]), 1)

    def test_photo_access_only_for_published_product_and_current_customer(self):
        path = reverse("plantupia-real-commerce-image", args=[self.product_real.pk])
        self.assertEqual(self.api.get(path).status_code, 404)
        self.publish_after_review()
        result = self.api.get(path)
        self.assertEqual(result.status_code, 200)
        self.assertTrue(result.data["image_base64"])
        self.assertIn("no-store", result["Cache-Control"])
        thumb = self.api.get(path + "?photo_id=" + str(self.photo.pk))
        self.assertEqual(thumb.status_code, 200)
        self.assertEqual(self.api.get(path + "?photo_id=" + str(uuid.uuid4())).status_code, 404)
        self.api.force_authenticate(self.staff)
        self.assertEqual(self.api.get(path).status_code, 403)

    def test_cart_persists_per_customer_without_fake_order_or_stock_changes(self):
        self.publish_after_review()
        self.assertEqual(self.add_to_cart(self.sku_small, 2).status_code, 200)
        first = self.api.get(self.cart_url).data
        self.assertEqual(first["subtotal"], "350.00")
        self.assertEqual(len(first["results"]), 1)
        self.assertEqual(first["results"][0]["product"]["id"], str(self.sku_small.pk))
        self.assertTrue(first["results"][0]["available"])
        self.assertFalse(first["checkout_ready"])
        self.assertEqual(self.add_to_cart(self.sku_small, 2).status_code, 200)
        self.assertEqual(CommerceCartLine.objects.count(), 1)
        self.assertEqual(self.add_to_cart(self.sku_large, 1).status_code, 200)
        self.assertEqual(self.api.get(self.cart_url).data["subtotal"], "600.00")
        self.assertEqual(self.add_to_cart(self.sku_small, 0).status_code, 200)
        self.assertEqual(CommerceCartLine.objects.count(), 1)
        self.assertEqual(CommerceCartLine.objects.get().sku_id, self.sku_large.pk)
        self.assertEqual(CommerceOrderRequest.objects.count(), 0)
        self.assertEqual(DemoOrder.objects.count(), 0)
        self.assertEqual(DemoCartLine.objects.count(), 0)
        self.assertEqual(CatalogProduct.objects.filter(name="Real indoor fern").count(), 0)
        self.assertEqual(PlantUnit.objects.filter(label="Real indoor fern").count(), 0)

    def test_unpublished_out_of_stock_and_bad_quantities_cannot_enter_cart(self):
        self.publish_after_review()
        self.assertEqual(self.add_to_cart(self.sku_small, 11).status_code, 400)
        self.assertEqual(self.add_to_cart(self.sku_small, 8).status_code, 400)
        self.assertFalse(CommerceCartLine.objects.exists())
        self.assertEqual(self.add_to_cart(self.sku_small, 7).status_code, 200)
        # A physical-count correction can make a previously saved cart stale.
        record_counted_stock(staff=self.staff, sku_id=self.sku_small.pk,
                             location_id=self.location.pk, quantity_delta=-7,
                             reason="correction", reference_note="Fresh count",
                             client_id=uuid.uuid4())
        stale = self.api.get(self.cart_url).data
        self.assertFalse(stale["all_items_available"])
        self.assertFalse(stale["results"][0]["available"])
        self.assertEqual(self.add_to_cart(self.sku_small, 1).status_code, 400)
        self.assertEqual(self.add_to_cart(self.sku_small, 0).status_code, 200)

    def test_carts_do_not_leak_between_customers_or_staff(self):
        self.publish_after_review()
        self.add_to_cart(self.sku_small, 1)
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(self.cart_url).data["results"], [])
        self.assertEqual(self.add_to_cart(self.sku_large, 1).status_code, 200)
        self.api.force_authenticate(self.owner)
        self.assertEqual(self.api.get(self.cart_url).data["results"][0]["product"]["sku"], "FERN-S-REAL")
        self.api.force_authenticate(self.staff)
        self.assertEqual(self.api.get(self.cart_url).status_code, 403)
        self.assertEqual(self.api.get(self.catalog_url).status_code, 403)
        self.assertEqual(self.api.post(self.cart_url, {
            "sku_id": str(self.sku_small.pk), "quantity": 1,
        }, format="json").status_code, 403)

    def test_product_edit_unpublishes_and_marks_existing_cart_item_unavailable(self):
        self.publish_after_review()
        self.add_to_cart(self.sku_small, 2)
        self.client.force_login(self.staff)
        edit = self.client.post(reverse("company:commerce-sku-edit", args=[self.sku_small.pk]), {
            "code": "FERN-S-REAL", "option_label": "Small", "price": "195.00", "currency": "EGP",
        })
        self.assertEqual(edit.status_code, 302, edit.content)
        self.product_real.refresh_from_db()
        self.assertIsNone(self.product_real.storefront_published_at)
        self.assertEqual(self.product_real.catalog_review_status, "draft")
        self.assertEqual(self.api.get(self.catalog_url).data["results"], [])
        cart = self.api.get(self.cart_url).data
        self.assertFalse(cart["results"][0]["available"])
        self.assertEqual(cart["subtotal"], "390.00")
        self.assertEqual(self.add_to_cart(self.sku_large, 1).status_code, 400)
        self.assertEqual(self.add_to_cart(self.sku_small, 0).status_code, 200)
