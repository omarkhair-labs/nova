"""QA-only B2C order request safety, snapshot and staff review regression tests."""
import os
import uuid
from decimal import Decimal
from unittest.mock import patch

from django.test import TestCase, override_settings
from django.urls import reverse

from .commerce_models import (
    CommerceProduct, CommerceSKU, CommerceStockLocation,
    CommerceOrderRequest, CommerceOrderRequestLine,
)
from .commerce_services import record_counted_stock, stock_balance
from .models import DemoOrder, PlantUnit, InstallationBatch
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CommerceQARequestsTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.flag_commerce = patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "1"})
        self.flag_commerce.start()
        self.addCleanup(self.flag_commerce.stop)
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.product_draft = CommerceProduct.objects.create(
            name="QA intended houseplant", description="Internal draft for order flow testing",
            kind="plant", created_by=self.staff,
        )
        self.sku = CommerceSKU.objects.create(
            product=self.product_draft, code="INT-QA-001",
            option_label="Large", price=Decimal("120.50"),
        )
        self.warehouse = CommerceStockLocation.objects.create(code="count-room", name="QA count room")
        record_counted_stock(
            staff=self.staff, sku_id=self.sku.pk, location_id=self.warehouse.pk,
            quantity_delta=7, reason="opening", reference_note="Actual QA stock count",
            client_id=uuid.uuid4(),
        )

    def body(self, **override):
        data = {
            "client_id": str(uuid.uuid4()),
            "lines": [{"sku_id": str(self.sku.pk), "quantity": 2}],
            "contact_name": "Test QA Customer",
            "contact_phone": "+20 101 234 5678",
            "city": "QA Cairo",
            "delivery_address": "Mock internal QA delivery address, not a real location",
            "payment_preference": "cod",
            "confirm_qa_no_payment": True,
        }
        data.update(override)
        return data

    def submit(self, **override):
        response = self.api.post(reverse("plantupia-commerce-qa-requests"), self.body(**override), format="json")
        self.assertEqual(response.status_code, 201, getattr(response, "data", ""))
        return response

    def test_draft_request_has_immutable_prices_and_never_charges_or_delivers(self):
        before_plants = PlantUnit.objects.count()
        before_installations = InstallationBatch.objects.count()
        r = self.submit()
        body = r.data
        self.assertEqual(body["status"], "qa_submitted")
        self.assertEqual(body["payment_status"], "not_collected")
        self.assertIs(body["charged"], False)
        self.assertIs(body["delivery_initiated"], False)
        self.assertIsNone(body["shipping_fee"])
        self.assertIsNone(body["final_total"])
        self.assertEqual(body["provisional_subtotal"], "241.00")
        self.assertEqual(body["lines"][0]["sku"], "INT-QA-001")
        self.assertEqual(body["lines"][0]["quantity"], 2)
        self.assertEqual(stock_balance(self.sku.pk), 7)  # Not reserved or deducted.
        self.assertFalse(DemoOrder.objects.exists())
        self.assertEqual(PlantUnit.objects.count(), before_plants)
        self.assertEqual(InstallationBatch.objects.count(), before_installations)
        self.sku.price = Decimal("135.25")
        self.sku.code = "INT-QA-CHANGED"
        self.sku.save()
        old = CommerceOrderRequestLine.objects.get()
        self.assertEqual(old.sku_code_snapshot, "INT-QA-001")
        self.assertEqual(old.unit_price_snapshot, Decimal("120.50"))
        self.assertEqual(old.line_subtotal_snapshot, Decimal("241.00"))

    def test_retry_idempotency_and_modified_payload_rejected(self):
        client_id = str(uuid.uuid4())
        one = self.submit(client_id=client_id)
        same = self.submit(client_id=client_id)
        self.assertEqual(one.data["id"], same.data["id"])
        self.assertEqual(CommerceOrderRequest.objects.count(), 1)
        self.assertEqual(CommerceOrderRequestLine.objects.count(), 1)
        modified = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(client_id=client_id, payment_preference="manual_transfer"), format="json")
        self.assertEqual(modified.status_code, 400)
        self.assertEqual(CommerceOrderRequest.objects.count(), 1)

    def test_customer_can_only_read_own_request_not_another_account(self):
        order_id = self.submit().data["id"]
        detail_url = reverse("plantupia-commerce-qa-request", args=[order_id])
        self.assertEqual(self.api.get(detail_url).status_code, 200)
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(detail_url).status_code, 404)
        self.assertEqual(self.api.get(reverse("plantupia-commerce-qa-requests")).data["results"], [])
        self.assertEqual(CommerceOrderRequest.objects.count(), 1)

    def test_invalid_cart_duplicate_sku_and_payment_confirmation_are_rejected(self):
        duplicate = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(lines=[{"sku_id": str(self.sku.pk), "quantity": 1},
                             {"sku_id": str(self.sku.pk), "quantity": 2}]), format="json")
        self.assertEqual(duplicate.status_code, 400)
        no_consent = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(confirm_qa_no_payment=False), format="json")
        self.assertEqual(no_consent.status_code, 400)
        too_many = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(lines=[{"sku_id": str(self.sku.pk), "quantity": 8}]), format="json")
        self.assertEqual(too_many.status_code, 400)
        bad_payment = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(payment_preference="card_automatic"), format="json")
        self.assertEqual(bad_payment.status_code, 400)
        self.assertEqual(CommerceOrderRequest.objects.count(), 0)

    def test_qa_company_queue_and_human_review_not_payment_acceptance(self):
        order_id = self.submit(payment_preference="manual_transfer").data["id"]
        self.client.force_login(self.staff)
        url = reverse("company:commerce-qa-request", args=[order_id])
        queue = self.client.get(reverse("company:commerce-qa-requests"))
        self.assertEqual(queue.status_code, 200)
        details = self.client.get(url)
        self.assertEqual(details.status_code, 200)
        self.assertContains(details, "Manual InstaPay/bank transfer")
        token = details.context["form"].initial["review_token"]
        result = self.client.post(url, {
            "review_token": token, "action": "review",
            "review_note": "QA review only; no payment collected", "confirmed": "on",
        })
        self.assertEqual(result.status_code, 302, getattr(result, "content", b""))
        order = CommerceOrderRequest.objects.get(pk=order_id)
        self.assertEqual(order.status, "qa_reviewed")
        self.assertEqual(order.reviewed_by, self.staff)
        self.assertEqual(self.client.post(url, {
            "review_token": token, "action": "review", "confirmed": "on",
        }).status_code, 200)
        self.api.force_authenticate(self.owner)
        mine = self.api.get(reverse("plantupia-commerce-qa-request", args=[order_id]))
        self.assertEqual(mine.status_code, 200)
        self.assertEqual(mine.data["payment_status"], "not_collected")
        self.assertEqual(mine.data["status"], "qa_reviewed")
        self.assertFalse(mine.data["charged"])
        self.assertFalse(mine.data["delivery_initiated"])
        self.assertEqual(stock_balance(self.sku.pk), 7)

    def test_qa_request_feature_is_hidden_when_disabled(self):
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "0"}):
            self.assertEqual(self.api.get(reverse("plantupia-commerce-qa-requests")).status_code, 404)
            self.assertEqual(self.client.get(reverse("company:commerce-qa-requests")).status_code, 404)

    def test_staff_cannot_create_customer_qa_requests_and_customer_cannot_see_company(self):
        self.api.force_authenticate(self.staff)
        response = self.api.post(reverse("plantupia-commerce-qa-requests"), self.body(), format="json")
        self.assertEqual(response.status_code, 403)
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:commerce-qa-requests")).status_code, 403)
        self.assertFalse(CommerceOrderRequest.objects.exists())

    def test_staff_without_order_permission_cannot_view_or_review(self):
        self.submit()
        self.client.force_login(self.reviewer)
        self.assertEqual(self.client.get(reverse("company:commerce-qa-requests")).status_code, 403)
