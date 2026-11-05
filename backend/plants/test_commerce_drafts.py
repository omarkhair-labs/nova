"""Commercial catalogue is a staff-only draft, never an alias of fictional QA orders."""
import uuid
from decimal import Decimal

from django.core.exceptions import PermissionDenied, ValidationError
from django.test import TestCase, override_settings
from django.urls import reverse

from .commerce_models import CommerceProduct, CommerceSKU, CommerceStockLocation, CommerceStockMovement
from .commerce_services import record_counted_stock, stock_balance
from .models import DemoOrder, CatalogProduct, InstallationBatch, PlantUnit
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CommerceDraftIsolationTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.product_draft = CommerceProduct.objects.create(
            name="Authorized specimen draft", description="Proposed physical item",
            kind="plant", created_by=self.staff,
        )
        self.sku = CommerceSKU.objects.create(
            product=self.product_draft, code="P-COM-001",
            option_label="Medium", price=Decimal("450.00"),
        )
        self.warehouse = CommerceStockLocation.objects.create(
            code="qa-storage", name="Test stock location",
        )

    def count(self, delta, *, client_id=None, reason="correction", note="Independent physical count"):
        return record_counted_stock(
            staff=self.staff, sku_id=self.sku.pk, location_id=self.warehouse.pk,
            quantity_delta=delta, reason=reason, reference_note=note,
            client_id=client_id or uuid.uuid4(),
        )

    def test_a_commercial_draft_is_not_a_qa_shop_listing_or_customer_plant(self):
        before_qa = CatalogProduct.objects.count()
        before_plant = PlantUnit.objects.count()
        self.count(8, reason="opening")
        self.assertEqual(CatalogProduct.objects.count(), before_qa)
        self.assertEqual(PlantUnit.objects.count(), before_plant)
        self.assertFalse(DemoOrder.objects.exists())
        self.assertEqual(InstallationBatch.objects.count(), 1)
        listing = self.api.get(reverse("plantupia-catalog"))
        self.assertEqual(listing.status_code, 200)
        self.assertNotIn("P-COM-001", str(listing.data))

    def test_counted_stock_is_idempotent_and_a_retry_cannot_change_payload(self):
        identity = uuid.uuid4()
        first = self.count(7, client_id=identity, reason="opening", note="Signed count A")
        replay = self.count(7, client_id=identity, reason="opening", note="Signed count A")
        self.assertEqual(first.pk, replay.pk)
        self.assertEqual(stock_balance(self.sku.pk), 7)
        self.assertEqual(CommerceStockMovement.objects.count(), 1)
        with self.assertRaises(ValidationError):
            self.count(70, client_id=identity, reason="opening", note="Signed count A")
        self.assertEqual(stock_balance(self.sku.pk), 7)

    def test_cannot_create_negative_stock_or_false_opening_count(self):
        self.count(5, reason="opening")
        with self.assertRaises(ValidationError):
            self.count(-6)
        with self.assertRaises(ValidationError):
            self.count(-1, reason="opening")
        with self.assertRaises(ValidationError):
            self.count(0)
        self.assertEqual(stock_balance(self.sku.pk), 5)
        self.assertEqual(CommerceStockMovement.objects.count(), 1)
        self.count(-3)
        self.assertEqual(stock_balance(self.sku.pk), 2)

    def test_counts_are_per_sku_and_location(self):
        second = CommerceStockLocation.objects.create(code="second-location", name="Second location")
        self.count(5, reason="opening")
        record_counted_stock(staff=self.staff, sku_id=self.sku.pk, location_id=second.pk,
            quantity_delta=3, reason="opening", reference_note="Second signed count", client_id=uuid.uuid4())
        self.assertEqual(stock_balance(self.sku.pk), 8)
        self.assertEqual(stock_balance(self.sku.pk, second.pk), 3)
        self.assertEqual(stock_balance(self.sku.pk, self.warehouse.pk), 5)

    def test_regular_customer_cannot_access_company_drafts_or_modify_stock(self):
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-stock")).status_code, 403)
        with self.assertRaises(PermissionDenied):
            record_counted_stock(staff=self.owner, sku_id=self.sku.pk, location_id=self.warehouse.pk,
                quantity_delta=1, reason="opening", reference_note="No access", client_id=uuid.uuid4())
        self.assertEqual(stock_balance(self.sku.pk), 0)

    def test_staff_without_explicit_commerce_permissions_cannot_read_drafts(self):
        self.client.force_login(self.reviewer)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-stock")).status_code, 403)

    def test_staff_authoring_flow_is_draft_only_and_can_record_count(self):
        self.client.force_login(self.staff)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-product", args=[self.product_draft.pk])).status_code, 200)
        response = self.client.post(reverse("company:commerce-product-create"), {
            "name": "Real inventory candidate", "description": "Internal draft only", "kind": "planter",
            "reference_species": "", "image_description": "",
        })
        self.assertEqual(response.status_code, 302, response.content)
        self.assertTrue(CommerceProduct.objects.filter(name="Real inventory candidate").exists())
        add_option = self.client.post(reverse("company:commerce-sku-create", args=[self.product_draft.pk]), {
            "code": "p-com-002", "option_label": "Small", "price": "120.00", "currency": "EGP",
        })
        self.assertEqual(add_option.status_code, 302, add_option.content)
        self.assertTrue(CommerceSKU.objects.filter(code="P-COM-002").exists())
        self.assertEqual(self.client.get(reverse("company:commerce-stock")).status_code, 200)
        receipt = self.client.post(reverse("company:commerce-stock-count", args=[self.sku.pk]), {
            "location": str(self.warehouse.pk), "quantity_delta": "4", "reason": "opening",
            "reference_note": "QA warehouse physical count", "client_id": str(uuid.uuid4()),
        })
        self.assertEqual(receipt.status_code, 302, receipt.content)
        self.assertEqual(stock_balance(self.sku.pk), 4)

    def test_photo_alt_and_botanical_reference_rules_remain_strict(self):
        self.product_draft.kind = "planter"
        self.product_draft.reference_species = self.species
        with self.assertRaises(ValidationError):
            self.product_draft.full_clean()
        self.product_draft.reference_species = None
        self.product_draft.full_clean()
        bad = CommerceSKU(product=self.product_draft, code="X-INVALID", price=Decimal("-1.00"))
        with self.assertRaises(ValidationError):
            bad.full_clean()
