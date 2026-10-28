"""Bulk catalogue ZIP QA: validation, atomic import and isolation tests."""
import os
from io import BytesIO
from tempfile import TemporaryDirectory
from unittest.mock import patch
from zipfile import ZIP_DEFLATED, ZipFile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from openpyxl import Workbook
from PIL import Image

from .models import CatalogProduct
from .test_ceo_demo import DemoFixture, SETTINGS


def package(name="FICTIONAL QA · Demo fern", image_path="images/fern.webp"):
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "Products"
    sheet.append(["sku", "name", "kind", "description", "proposed_price",
                  "currency", "qa_stock", "active", "image", "demo_only"])
    sheet.append(["PQA-TEST-001", name, "plant", "Concept listing, not merchandise.",
                  120.50, "EGP", 4, True, image_path, True])
    xlsx = BytesIO()
    workbook.save(xlsx)
    photo = BytesIO()
    Image.new("RGB", (160, 200), "#799b61").save(photo, format="WEBP")
    body = BytesIO()
    with ZipFile(body, "w", compression=ZIP_DEFLATED) as archive:
        archive.writestr("products.xlsx", xlsx.getvalue())
        archive.writestr("images/fern.webp", photo.getvalue())
    return SimpleUploadedFile("qa-catalog.zip", body.getvalue(), content_type="application/zip")


@override_settings(**SETTINGS)
class CatalogZipImportTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self._media = TemporaryDirectory()
        self.addCleanup(self._media.cleanup)
        storage_settings = {
            "default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
            "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
        }
        override = override_settings(MEDIA_ROOT=self._media.name, STORAGES=storage_settings)
        override.enable()
        self.addCleanup(override.disable)

    def _upload(self, action, **kwargs):
        with patch.dict(os.environ, {"PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED": "1"}):
            return self.client.post(reverse("company:catalog-zip-import"),
                                    {"upload": package(**kwargs), "action": action})

    def test_preview_parses_photo_but_does_not_create_product(self):
        before = CatalogProduct.objects.count()
        response = self._upload("preview")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Validated 1 products")
        self.assertEqual(CatalogProduct.objects.count(), before)

    def test_commit_creates_authenticated_photo_and_blocks_duplicate_import(self):
        before = CatalogProduct.objects.count()
        response = self._upload("import")
        self.assertEqual(response.status_code, 302)
        self.assertEqual(CatalogProduct.objects.count(), before + 1)
        item = CatalogProduct.objects.get(name="FICTIONAL QA · Demo fern")
        self.assertTrue(item.image)
        self.assertEqual(item.currency, "EGP")
        self.assertEqual(item.proposed_price, 120.50)
        response = self._upload("import")
        self.assertEqual(response.status_code, 400)
        self.assertEqual(CatalogProduct.objects.count(), before + 1)

    def test_missing_photo_cannot_partially_import(self):
        before = CatalogProduct.objects.count()
        response = self._upload("import", image_path="images/missing.webp")
        self.assertEqual(response.status_code, 400)
        self.assertEqual(CatalogProduct.objects.count(), before)

    def test_feature_is_opt_in_not_implicitly_exposed_by_ceo_demo(self):
        with patch.dict(os.environ, {"PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED": "0"}):
            response = self.client.get(reverse("company:catalog-zip-import"))
        self.assertEqual(response.status_code, 404)
