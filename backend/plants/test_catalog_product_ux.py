"""SKU option, staff-photo and immutable QA order tests.

The fixtures here are synthetic; none of their media or prices represent real
Plantupia inventory.
"""
from io import BytesIO
from tempfile import TemporaryDirectory

from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.exceptions import ValidationError
from django.test import TestCase, override_settings
from django.urls import reverse
from PIL import Image

from .models import CatalogProduct, CatalogProductPhoto, DemoOrderLine
from .test_ceo_demo import DemoFixture, SETTINGS


def sample_photo():
    content = BytesIO()
    Image.new("RGB", (180, 220), "#7a9279").save(content, format="JPEG")
    return SimpleUploadedFile("actual-qa-demo.jpg", content.getvalue(), content_type="image/jpeg")


@override_settings(**SETTINGS)
class CatalogProductUxTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        override = override_settings(MEDIA_ROOT=temporary.name, STORAGES={
            "default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
            "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
        })
        override.enable()
        self.addCleanup(override.disable)

    def _other_sku(self):
        self.product.sku = "PQA-PLANT-SMALL"
        self.product.family_key = "qa-fern"
        self.product.variant_label = "Small"
        self.product.image_description = "Synthetic QA plant on a plain studio background"
        self.product.save()
        return CatalogProduct.objects.create(
            name="FICTIONAL QA indoor plant", description="Synthetic medium option",
            kind="plant", sku="PQA-PLANT-MEDIUM", family_key="qa-fern",
            variant_label="Medium", proposed_price="175.00", qa_stock=3,
        )

    def test_catalog_exposes_real_option_identifiers_without_fabricating_images(self):
        sibling = self._other_sku()
        response = self.api.get(reverse("plantupia-catalog"))
        self.assertEqual(response.status_code, 200)
        rows = response.json()["results"]
        by_sku = {row["sku"]: row for row in rows if row["sku"]}
        self.assertEqual(by_sku["PQA-PLANT-SMALL"]["family_key"], "qa-fern")
        self.assertEqual(by_sku["PQA-PLANT-MEDIUM"]["variant_label"], "Medium")
        self.assertEqual(by_sku["PQA-PLANT-MEDIUM"]["price"], "175.00")
        self.assertEqual(by_sku["PQA-PLANT-MEDIUM"]["id"], str(sibling.pk))
        self.assertFalse(by_sku["PQA-PLANT-MEDIUM"]["has_image"])
        self.assertEqual(by_sku["PQA-PLANT-MEDIUM"]["photos"], [])

    def test_order_preserves_selected_sku_and_variant_after_catalog_edit(self):
        selected = self._other_sku()
        quote = self.api.post(reverse("plantupia-cart"), {
            "product_id": str(selected.pk), "quantity": 2,
        }, format="json").json()
        response = self.api.post(reverse("plantupia-demo-orders"),
            self.checkout_body(quote), format="json")
        self.assertEqual(response.status_code, 200, response.data)
        line = DemoOrderLine.objects.get(order_id=response.data["id"])
        self.assertEqual(line.sku_snapshot, "PQA-PLANT-MEDIUM")
        self.assertEqual(line.variant_snapshot, "Medium")
        selected.variant_label = "Renamed later"
        selected.sku = "PQA-PLANT-MEDIUM-NEW"
        selected.save()
        line.refresh_from_db()
        self.assertEqual((line.sku_snapshot, line.variant_snapshot),
                         ("PQA-PLANT-MEDIUM", "Medium"))

    def test_sku_must_be_unique_when_not_blank(self):
        self._other_sku()
        duplicate = CatalogProduct(name="FICTIONAL QA duplicate", kind="planter",
            description="QA fixture", sku="PQA-PLANT-MEDIUM",
            proposed_price="15.00", qa_stock=1)
        with self.assertRaises(ValidationError):
            duplicate.full_clean()

    def test_plant_nutrition_requires_source_provenance_before_qa_activation(self):
        item = CatalogProduct(
            name="FICTIONAL QA · Test fertilizer", kind="accessory",
            subcategory="nutrition", description="Synthetic QA testing only.",
            proposed_price="12.00", qa_stock=2, active=False,
        )
        item.full_clean()
        item.save()
        item.active = True
        with self.assertRaises(ValidationError):
            item.full_clean()
        item.nutrition_label = "Synthetic label for test only; no real product recommendation"
        item.nutrition_source_url = "http://example.test/not-secure"
        item.nutrition_source_reference = "Synthetic QA label"
        with self.assertRaises(ValidationError):
            item.full_clean()
        item.nutrition_source_url = "https://example.test/fictional-fertilizer"
        item.nutrition_use_notes = "Synthetic source text, NOT actual dosing advice"
        item.nutrition_cautions = "Synthetic precautions, no real usage recommended"
        item.full_clean()
        item.save()
        response = self.api.get(reverse("plantupia-catalog"))
        self.assertEqual(response.status_code, 200)
        returned = next(p for p in response.data["results"] if p["id"] == str(item.pk))
        self.assertEqual(returned["subcategory"], "nutrition")
        self.assertFalse(returned["nutrition"]["plant_specific_recommendation"])
        self.assertEqual(returned["nutrition"]["source_url"], item.nutrition_source_url)
        self.assertEqual(returned["nutrition"]["label"], item.nutrition_label)

    def test_nutrition_metadata_cannot_be_attached_to_unrelated_kind(self):
        item = CatalogProduct(
            name="FICTIONAL QA · Plant with fertilizer metadata", kind="plant",
            subcategory="nutrition", description="Synthetic invalid record",
            proposed_price="25.00", qa_stock=1,
        )
        with self.assertRaises(ValidationError):
            item.full_clean()
        item.subcategory = ""
        item.nutrition_label = "Wrongly attached label"
        with self.assertRaises(ValidationError):
            item.full_clean()
        self.product.refresh_from_db()
        self.assertEqual(self.product.subcategory, "")
        self.assertEqual(self.api.get(reverse("plantupia-catalog")).data["results"][0]["subcategory"], "")

    def test_gallery_endpoint_scoped_to_requested_product(self):
        photo = CatalogProductPhoto.objects.create(
            product=self.product, image=sample_photo(), alt_text="Synthetic QA studio pot", position=0)
        response = self.api.get(reverse("plantupia-catalog-image", args=[self.product.pk]),
            {"photo_id": str(photo.pk)})
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()["image_base64"])
        wrong = CatalogProduct.objects.create(name="FICTIONAL QA pot", kind="planter",
            description="Different QA product", proposed_price="25.00", qa_stock=2)
        self.assertEqual(self.api.get(reverse("plantupia-catalog-image", args=[wrong.pk]),
            {"photo_id": str(photo.pk)}).status_code, 404)

    def test_staff_can_add_authorized_photo_but_customer_cannot_use_company_form(self):
        url = reverse("company:catalog-photos", args=[self.product.pk])
        self.assertEqual(self.client.get(url).status_code, 200)
        response = self.client.post(url, {
            "photo": sample_photo(), "alt_text": "Synthetic studio plant for QA", "position": "1",
        })
        self.assertEqual(response.status_code, 302)
        self.assertEqual(self.product.gallery.count(), 1)
        self.client.force_login(self.owner)
        self.client.post(url, {
            "photo": sample_photo(), "alt_text": "Unauthorized", "position": "1",
        })
        self.assertEqual(self.product.gallery.count(), 1)
