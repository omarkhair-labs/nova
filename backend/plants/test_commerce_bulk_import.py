"""Commercial catalogue ZIP import: real draft model, gallery and SKU integrity."""
from io import BytesIO
from tempfile import TemporaryDirectory
from unittest.mock import patch
from zipfile import ZIP_DEFLATED, ZipFile

from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from openpyxl import Workbook, load_workbook
from PIL import Image

from .commerce_bulk_import import HEADER
from .commerce_models import (CommerceProduct, CommerceProductPhoto, CommerceSKU,
                              CommerceOrderRequest, CommerceStockMovement)
from .models import CatalogProduct, PlantUnit
from .test_ceo_demo import DemoFixture, SETTINGS


def sample_rows():
    return [
        ("fern-home", "FERN-S", "Fern draft", "plant", "A staff-reviewed product candidate",
         "Small", 155.00, "EGP", "images/fern.webp", "Fern seen from the front",
         "images/detail.webp|Fern leaf close-up", ""),
        ("fern-home", "FERN-L", "Fern draft", "plant", "A staff-reviewed product candidate",
         "Large", 250.00, "EGP", "images/fern.webp", "Fern seen from the front",
         "images/detail.webp|Fern leaf close-up", ""),
        ("care-supply", "CARE-01", "Care supply draft", "care", "Staff draft",
         "Standard", 45.00, "EGP", "", "", "", ""),
    ]


def package_bytes(rows=None, images=None):
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "Products"
    sheet.append(HEADER)
    for row in rows if rows is not None else sample_rows():
        sheet.append(row)
    xlsx = BytesIO()
    workbook.save(xlsx)
    workbook.close()
    result = BytesIO()
    photo_names = ["images/fern.webp", "images/detail.webp"] if images is None else list(images)
    with ZipFile(result, "w", compression=ZIP_DEFLATED) as archive:
        archive.writestr("products.xlsx", xlsx.getvalue())
        for name in photo_names:
            buffer = BytesIO()
            Image.new("RGB", (128, 160), "#678f60").save(buffer, format="WEBP")
            archive.writestr(name, buffer.getvalue())
    return result.getvalue()


@override_settings(**SETTINGS)
class CommerceBulkImportTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        media = override_settings(MEDIA_ROOT=self.temp.name, STORAGES={
            "default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
            "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
        })
        media.enable()
        self.addCleanup(media.disable)
        self.url = reverse("company:commerce-product-import")
        self.pack = package_bytes()

    def upload(self, action, data=None, digest="", confirm=False):
        payload = SimpleUploadedFile("plantupia-products.zip", data if data is not None else self.pack,
                                     content_type="application/zip")
        return self.client.post(self.url, {
            "upload": payload, "action": action, "digest": digest,
            "confirm": "1" if confirm else "",
        }, HTTP_ACCEPT="application/json")

    def test_template_is_downloadable_and_import_is_in_new_commerce_catalog(self):
        response = self.client.get(reverse("company:commerce-product-template"))
        self.assertEqual(response.status_code, 200)
        workbook = load_workbook(BytesIO(response.content), read_only=True)
        self.assertEqual(tuple(next(workbook["Products"].values)), HEADER)
        self.assertIn("Instructions", workbook.sheetnames)
        workbook.close()
        catalog = self.client.get(reverse("company:commerce-products"))
        self.assertContains(catalog, reverse("company:commerce-product-import"))
        self.assertEqual(self.client.get(self.url).status_code, 200)

    def test_preview_is_read_only_and_confirm_creates_one_product_per_key(self):
        original = CatalogProduct.objects.count()
        plants = PlantUnit.objects.count()
        response = self.upload("preview")
        self.assertEqual(response.status_code, 200, response.content)
        data = response.json()
        self.assertEqual(data["products"], 2)
        self.assertEqual(data["skus"], 3)
        self.assertEqual(data["photos"], 2)
        self.assertEqual(CommerceProduct.objects.count(), 0)
        self.assertEqual(CommerceSKU.objects.count(), 0)
        confirm = self.upload("import", digest=data["digest"], confirm=True)
        self.assertEqual(confirm.status_code, 200, confirm.content)
        self.assertEqual(CommerceProduct.objects.count(), 2)
        self.assertEqual(CommerceSKU.objects.count(), 3)
        self.assertEqual(CommerceProductPhoto.objects.count(), 1)
        self.assertEqual(CatalogProduct.objects.count(), original)
        self.assertEqual(PlantUnit.objects.count(), plants)
        self.assertEqual(CommerceStockMovement.objects.count(), 0)
        self.assertEqual(CommerceOrderRequest.objects.count(), 0)
        fern = CommerceProduct.objects.get(catalog_key="fern-home")
        self.assertEqual(fern.variants.count(), 2)
        self.assertTrue(fern.cover_photo)
        self.assertEqual(fern.gallery.first().alt_text, "Fern leaf close-up")
        self.assertEqual(str(CommerceSKU.objects.get(code="FERN-L").price), "250.00")
        image = self.client.get(reverse("company:commerce-product-photo", args=[fern.pk]))
        self.assertEqual(image.status_code, 200)
        self.assertEqual(image["Content-Type"], "image/jpeg")
        gallery = self.client.get(reverse("company:commerce-product-photo", args=[fern.pk])
                                  + "?photo=" + str(fern.gallery.first().pk))
        self.assertEqual(gallery.status_code, 200)
        self.assertEqual(gallery["Cache-Control"], "private, no-store, max-age=0")

    def test_confirm_requires_preview_digest_matching_same_zip(self):
        self.assertEqual(self.upload("import", confirm=True).status_code, 400)
        first = self.upload("preview").json()
        altered = package_bytes(sample_rows()[:2])
        fail = self.upload("import", data=altered, digest=first["digest"], confirm=True)
        self.assertEqual(fail.status_code, 400)
        self.assertEqual(CommerceProduct.objects.count(), 0)
        self.assertEqual(self.upload("import", digest=first["digest"], confirm=False).status_code, 400)

    def test_second_import_rejects_conflicting_keys_and_skus_without_mutation(self):
        digest = self.upload("preview").json()["digest"]
        self.assertEqual(self.upload("import", digest=digest, confirm=True).status_code, 200)
        response = self.upload("preview")
        self.assertEqual(response.status_code, 400)
        self.assertIn("overwrite", response.json()["error"])
        self.assertEqual(CommerceProduct.objects.count(), 2)
        self.assertEqual(CommerceSKU.objects.count(), 3)

    def test_repeated_product_metadata_must_match_across_variants(self):
        rows = sample_rows()
        second = list(rows[1])
        second[2] = "Different product title"
        rows[1] = tuple(second)
        response = self.upload("preview", data=package_bytes(rows))
        self.assertEqual(response.status_code, 400)
        self.assertIn("differ", response.json()["error"])
        self.assertFalse(CommerceProduct.objects.exists())

    def test_bad_image_and_missing_image_do_not_write_anything(self):
        bad = self.upload("preview", data=package_bytes(images=["images/fern.webp"]))
        self.assertEqual(bad.status_code, 400)
        self.assertIn("gallery_images", bad.json()["error"])
        corrupt = BytesIO()
        workbook = Workbook()
        sheet = workbook.active
        sheet.title = "Products"
        sheet.append(HEADER)
        row = list(sample_rows()[0])
        row[10] = ""
        sheet.append(row)
        xlsx = BytesIO()
        workbook.save(xlsx)
        with ZipFile(corrupt, "w") as archive:
            archive.writestr("products.xlsx", xlsx.getvalue())
            archive.writestr("images/fern.webp", b"not-image")
        response = self.upload("preview", data=corrupt.getvalue())
        self.assertEqual(response.status_code, 400)
        self.assertIn("invalid image", response.json()["error"])
        self.assertFalse(CommerceProduct.objects.exists())

    def test_imported_photo_never_becomes_public_to_other_company_users_or_customers(self):
        digest = self.upload("preview").json()["digest"]
        self.upload("import", digest=digest, confirm=True)
        product = CommerceProduct.objects.get(catalog_key="fern-home")
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(self.url).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-product-photo",
                                                 args=[product.pk])).status_code, 403)

    def test_a_failing_variant_validation_rolls_back_all_drafts_and_media(self):
        digest = self.upload("preview").json()["digest"]
        with patch.object(CommerceSKU, "full_clean", side_effect=ValidationError("bad variant")):
            response = self.upload("import", digest=digest, confirm=True)
        self.assertEqual(response.status_code, 400)
        self.assertEqual(CommerceProduct.objects.count(), 0)
        self.assertEqual(CommerceSKU.objects.count(), 0)
        self.assertEqual(CommerceProductPhoto.objects.count(), 0)
        import os
        self.assertEqual(sum(len(files) for _, _, files in os.walk(self.temp.name)), 0)

    def test_new_imports_do_not_leak_into_qa_shop_or_direct_checkout(self):
        import uuid
        from unittest.mock import patch as mock_patch
        from .commerce_models import CommerceStockLocation
        from .commerce_services import record_counted_stock
        digest = self.upload("preview").json()["digest"]
        self.assertEqual(self.upload("import", digest=digest, confirm=True).status_code, 200)
        sku = CommerceSKU.objects.get(code="FERN-S")
        self.assertFalse(sku.product.qa_listed)
        location = CommerceStockLocation.objects.create(code="test-a", name="Test stock")
        record_counted_stock(staff=self.staff, sku_id=sku.pk, location_id=location.pk,
                             quantity_delta=5, reason="opening", reference_note="Test count",
                             client_id=uuid.uuid4())
        with mock_patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "1"}):
            listing = self.api.get(reverse("plantupia-commerce-qa-catalog"))
            self.assertEqual(listing.status_code, 200)
            self.assertNotIn("FERN-S", str(listing.data))
            request = self.api.post(reverse("plantupia-commerce-qa-requests"), {
                "client_id": str(uuid.uuid4()), "lines": [{"sku_id": str(sku.pk), "quantity": 1}],
                "contact_name": "QA customer", "contact_phone": "+201012345678",
                "city": "Cairo", "delivery_address": "QA-only street address",
                "payment_preference": "cod", "confirm_qa_no_payment": True,
            }, format="json")
            self.assertEqual(request.status_code, 400)
            self.assertFalse(CommerceOrderRequest.objects.exists())

    def test_import_can_reference_existing_botanical_species_without_verifying_specimen(self):
        rows = [list(sample_rows()[0])]
        rows[0][10] = ""
        rows[0][11] = self.species.key
        data = package_bytes([tuple(rows[0])])
        preview = self.upload("preview", data=data)
        self.assertEqual(preview.status_code, 200, preview.content)
        commit = self.upload("import", data=data, digest=preview.json()["digest"], confirm=True)
        self.assertEqual(commit.status_code, 200, commit.content)
        self.assertEqual(CommerceProduct.objects.get(catalog_key="fern-home").reference_species, self.species)
