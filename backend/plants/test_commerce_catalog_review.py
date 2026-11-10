"""Commercial source-of-truth review: a real content gate, not a QA sale."""
from io import BytesIO
from tempfile import TemporaryDirectory

from django.contrib.admin.models import LogEntry
from django.core.exceptions import ValidationError
from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from django.urls import reverse
from PIL import Image

from .commerce_models import CommerceProduct, CommerceSKU, CommerceOrderRequest
from .commerce_publication import product_readiness_issues, review_catalog_product
from .models import CatalogProduct, DemoOrder
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CommerceCatalogReviewTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        temp = TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        media = override_settings(MEDIA_ROOT=temp.name)
        media.enable()
        self.addCleanup(media.disable)
        self.product = CommerceProduct.objects.create(
            name="Plantupia real product candidate", kind="plant",
            description="Accurate growing conditions and item contents",
            created_by=self.staff,
        )
        self.sku = CommerceSKU.objects.create(
            product=self.product, code="REAL-001", option_label="Standard",
            price="199.00",
        )
        self.url = reverse("company:commerce-product-review", args=[self.product.pk])
        self.client.force_login(self.staff)

    def attach_real_photo(self):
        bitmap = Image.new("RGB", (140, 150), "green")
        data = BytesIO()
        bitmap.save(data, format="JPEG")
        self.product.cover_photo.save("real-cover.jpg", ContentFile(data.getvalue()), save=False)
        self.product.image_description = "One plant photographed on a plain background"
        self.product.save(update_fields=["cover_photo", "image_description"])

    def approve(self):
        return self.client.post(self.url, {"action": "approve"})

    def test_missing_photo_blocks_review_and_offers_clear_issues(self):
        response = self.client.get(reverse("company:commerce-product", args=[self.product.pk]))
        self.assertContains(response, "actual cover photograph")
        self.assertContains(response, "Needs product review")
        self.assertNotContains(response, "Approve product data</button>")
        self.assertEqual(self.approve().status_code, 302)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")
        self.assertIsNone(self.product.catalog_reviewed_at)

    def test_approval_records_actor_but_never_creates_a_checkout_or_public_listing(self):
        self.attach_real_photo()
        self.assertEqual(product_readiness_issues(self.product), [])
        self.assertEqual(self.approve().status_code, 302)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "approved")
        self.assertEqual(self.product.catalog_reviewed_by, self.staff)
        self.assertIsNotNone(self.product.catalog_reviewed_at)
        self.assertTrue(LogEntry.objects.filter(
            object_id=str(self.product.pk), change_message__icontains="Approved commercial product",
        ).exists())
        self.assertEqual(CommerceOrderRequest.objects.count(), 0)
        self.assertEqual(DemoOrder.objects.count(), 0)
        response = self.api.get(reverse("plantupia-catalog"))
        self.assertEqual(response.status_code, 200)
        self.assertNotIn("REAL-001", str(response.data))
        self.assertEqual(CatalogProduct.objects.filter(sku="REAL-001").count(), 0)

    def test_edit_product_details_invalidates_previous_approval(self):
        self.attach_real_photo()
        self.approve()
        result = self.client.post(reverse("company:commerce-product-edit", args=[self.product.pk]), {
            "name": "Plantupia revised product", "kind": "plant",
            "description": "Edited company product details", "image_description": self.product.image_description,
            "reference_species": "",
        })
        self.assertEqual(result.status_code, 302, result.content)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")
        self.assertIsNone(self.product.catalog_reviewed_by_id)
        self.assertIsNone(self.product.catalog_reviewed_at)

    def test_sku_edit_and_added_variant_invalidate_review(self):
        self.attach_real_photo()
        self.approve()
        changed = self.client.post(reverse("company:commerce-sku-edit", args=[self.sku.pk]), {
            "code": "REAL-001", "option_label": "Revised standard",
            "price": "215.00", "currency": "EGP",
        })
        self.assertEqual(changed.status_code, 302, changed.content)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")
        self.approve()
        added = self.client.post(reverse("company:commerce-sku-create", args=[self.product.pk]), {
            "code": "REAL-002", "option_label": "Large",
            "price": "260.00", "currency": "EGP",
        })
        self.assertEqual(added.status_code, 302, added.content)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")

    def test_no_skus_cannot_be_approved(self):
        self.attach_real_photo()
        self.sku.delete()
        with self.assertRaises(ValidationError):
            review_catalog_product(staff=self.staff, product_id=self.product.pk, action="approve")
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")

    def test_staff_can_reopen_approval_without_creating_qa_sales(self):
        self.attach_real_photo()
        self.approve()
        self.assertEqual(self.client.post(self.url, {"action": "reopen"}).status_code, 302)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")
        self.assertIsNone(self.product.catalog_reviewed_by_id)
        self.assertFalse(CommerceOrderRequest.objects.exists())

    def test_customer_cannot_review_product(self):
        self.attach_real_photo()
        self.client.force_login(self.owner)
        self.assertEqual(self.client.post(self.url, {"action": "approve"}).status_code, 403)
        self.product.refresh_from_db()
        self.assertEqual(self.product.catalog_review_status, "draft")
