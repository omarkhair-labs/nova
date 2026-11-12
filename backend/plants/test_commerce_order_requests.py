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
            kind="plant", created_by=self.staff, qa_listed=True,
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

    def test_map_pin_is_snapshotted_and_visible_to_owner_and_authorized_staff(self):
        response = self.submit(
            delivery_latitude="30.0444200", delivery_longitude="31.2357100",
            delivery_pin_confirmed=True, building_details="QA building floor 2",
            landmark="QA side entrance",
        )
        self.assertEqual(response.data["customer"]["latitude"], "30.0444200")
        self.assertEqual(response.data["customer"]["longitude"], "31.2357100")
        self.assertTrue(response.data["customer"]["pin_confirmed"])
        order = CommerceOrderRequest.objects.get(pk=response.data["id"])
        self.assertEqual(order.building_details, "QA building floor 2")
        self.assertEqual(order.landmark, "QA side entrance")
        self.client.force_login(self.staff)
        details = self.client.get(reverse("company:commerce-qa-request", args=[order.pk]))
        self.assertEqual(details.status_code, 200)
        self.assertContains(details, "View delivery pin on map")
        self.assertContains(details, "30.0444200")

    def test_invalid_or_unconfirmed_location_is_rejected_without_request(self):
        for payload in (
            {"delivery_latitude": "30.0", "delivery_pin_confirmed": True},
            {"delivery_latitude": "91.0", "delivery_longitude": "31.0", "delivery_pin_confirmed": True},
            {"delivery_latitude": "30.0", "delivery_longitude": "181.0", "delivery_pin_confirmed": True},
            {"delivery_latitude": "30.0", "delivery_longitude": "31.0", "delivery_pin_confirmed": False},
            {"delivery_pin_confirmed": True},
        ):
            response = self.api.post(reverse("plantupia-commerce-qa-requests"),
                self.body(**payload), format="json")
            self.assertEqual(response.status_code, 400, response.data)
        self.assertFalse(CommerceOrderRequest.objects.exists())

    def test_replaying_id_with_different_pin_is_not_a_second_order(self):
        request_id = str(uuid.uuid4())
        one = self.submit(client_id=request_id, delivery_latitude="30.0000000",
            delivery_longitude="31.0000000", delivery_pin_confirmed=True)
        again = self.submit(client_id=request_id, delivery_latitude="30.0000000",
            delivery_longitude="31.0000000", delivery_pin_confirmed=True)
        self.assertEqual(one.data["id"], again.data["id"])
        change_pin = self.api.post(reverse("plantupia-commerce-qa-requests"),
            self.body(client_id=request_id, delivery_latitude="30.1000000",
                delivery_longitude="31.0000000", delivery_pin_confirmed=True), format="json")
        self.assertEqual(change_pin.status_code, 400)
        self.assertEqual(CommerceOrderRequest.objects.count(), 1)

    def test_qa_skus_are_visible_only_while_enabled_and_do_not_include_delivery_data(self):
        response = self.api.get(reverse("plantupia-commerce-qa-catalog"))
        self.assertEqual(response.status_code, 200)
        row = response.data["results"][0]
        self.assertEqual(row["sku"], "INT-QA-001")
        self.assertTrue(row["qa_only"])
        self.assertNotIn("delivery_address", str(response.data))
        self.api.force_authenticate(self.staff)
        self.assertEqual(self.api.get(reverse("plantupia-commerce-qa-catalog")).status_code, 403)
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "0"}):
            self.assertEqual(self.api.get(reverse("plantupia-commerce-qa-catalog")).status_code, 404)

    def test_full_staff_tracking_updates_reach_customer_and_never_mark_paid(self):
        response = self.submit()
        order_id = response.data["id"]
        inbox = reverse("company:commerce-qa-requests")
        detail = reverse("company:commerce-qa-request", args=[order_id])
        alerts = reverse("company:commerce-qa-updates")
        self.client.force_login(self.staff)
        self.assertEqual(self.client.get(alerts).json()["new_requests"], 1)
        initial = self.client.get(detail)
        self.assertEqual(initial.status_code, 200)
        self.assertEqual(self.client.get(alerts).json()["new_requests"], 0)
        form = initial.context["form"]
        self.assertEqual(self.client.post(detail, {
            "review_token": form.initial["review_token"], "action": "review",
            "qa_shipping_fee": "45.00", "review_note": "Delivery fee estimated for QA",
            "confirmed": "on",
        }).status_code, 302)
        for action, target in (("prepare", "qa_preparing"),
                               ("dispatch", "qa_out_for_delivery"),
                               ("deliver", "qa_delivered")):
            screen = self.client.get(detail)
            self.assertEqual(screen.status_code, 200)
            signed = screen.context["form"].initial["review_token"]
            update = self.client.post(detail, {
                "review_token": signed, "action": action,
                "review_note": "QA status progressed", "confirmed": "on",
                "tracking_reference": "LOCAL-QA-12" if action == "dispatch" else "",
            })
            self.assertEqual(update.status_code, 302, update.content)
            self.assertEqual(CommerceOrderRequest.objects.get(pk=order_id).status, target)
        self.api.force_authenticate(self.owner)
        tracked = self.api.get(reverse("plantupia-commerce-qa-request", args=[order_id]))
        self.assertEqual(tracked.status_code, 200)
        self.assertEqual(tracked.data["status"], "qa_delivered")
        self.assertEqual(tracked.data["shipping_fee"], "45.00")
        self.assertEqual(tracked.data["final_total"], "286.00")
        self.assertEqual(tracked.data["qa_tracking_reference"], "LOCAL-QA-12")
        self.assertEqual([x["status"] for x in tracked.data["tracking"]],
            ["qa_submitted", "qa_reviewed", "qa_preparing", "qa_out_for_delivery", "qa_delivered"])
        self.assertFalse(tracked.data["charged"])
        self.assertFalse(tracked.data["delivery_initiated"])
        self.assertEqual(tracked.data["payment_status"], "not_collected")
        self.assertEqual(stock_balance(self.sku.pk), 7)
        self.assertFalse(DemoOrder.objects.exists())

        requested = self.api.post(reverse("plantupia-commerce-qa-request-action", args=[order_id]),
                                  {"action": "return"}, format="json")
        self.assertEqual(requested.status_code, 200, requested.data)
        self.assertEqual(requested.data["status"], "qa_return_requested")
        self.client.force_login(self.staff)
        screen = self.client.get(detail)
        self.assertContains(screen, "Customer requested a return")
        submitted = self.client.post(detail, {
            "review_token": screen.context["form"].initial["review_token"],
            "action": "return", "review_note": "QA return completed",
            "confirmed": "on",
        })
        self.assertEqual(submitted.status_code, 302, submitted.content)
        self.assertEqual(CommerceOrderRequest.objects.get(pk=order_id).status, "qa_returned")

    def test_no_skipping_tracking_steps_or_reusing_stale_staff_tokens(self):
        order_id = self.submit().data["id"]
        detail = reverse("company:commerce-qa-request", args=[order_id])
        self.client.force_login(self.staff)
        first = self.client.get(detail)
        token = first.context["form"].initial["review_token"]
        self.assertEqual(self.client.post(detail, {
            "review_token": token, "action": "review", "confirmed": "on",
        }).status_code, 302)
        skipped = self.client.post(detail, {
            "review_token": token, "action": "deliver", "confirmed": "on",
        })
        self.assertEqual(skipped.status_code, 200)
        self.assertEqual(CommerceOrderRequest.objects.get(pk=order_id).status, "qa_reviewed")
        current = self.client.get(detail)
        form = current.context["form"]
        self.assertIn("prepare", dict(form.fields["action"].choices))
        self.assertNotIn("deliver", dict(form.fields["action"].choices))
        current_token = form.initial["review_token"]
        self.assertEqual(self.client.post(detail, {
            "review_token": current_token, "action": "prepare", "confirmed": "on",
        }).status_code, 302)
        replay = self.client.post(detail, {
            "review_token": current_token, "action": "prepare", "confirmed": "on",
        })
        self.assertEqual(replay.status_code, 200)
        self.assertEqual(CommerceOrderRequest.objects.get(pk=order_id).status, "qa_preparing")

    def test_customer_cancel_is_idempotent_and_cannot_act_as_another_user(self):
        order_id = self.submit().data["id"]
        action = reverse("plantupia-commerce-qa-request-action", args=[order_id])
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.post(action, {"action": "cancel"}, format="json").status_code, 404)
        self.client.force_login(self.staff)
        self.assertEqual(self.client.get(reverse("company:commerce-qa-request", args=[order_id])).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-qa-updates")).json()["new_requests"], 0)
        self.api.force_authenticate(self.owner)
        first = self.api.post(action, {"action": "cancel"}, format="json")
        self.assertEqual(first.status_code, 200, first.data)
        self.assertTrue(first.data["cancel_requested"])
        self.assertEqual(self.client.get(reverse("company:commerce-qa-updates")).json()["new_requests"], 1)
        second = self.api.post(action, {"action": "cancel"}, format="json")
        self.assertEqual(second.status_code, 200)
        self.assertEqual(first.data["tracking"], second.data["tracking"])
        self.assertFalse(CommerceOrderRequest.objects.get(pk=order_id).reviewed_by_id)

    def test_only_staff_receives_private_new_order_count(self):
        self.submit()
        alert = reverse("company:commerce-qa-updates")
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(alert).status_code, 403)
        self.client.force_login(self.reviewer)
        self.assertEqual(self.client.get(alert).status_code, 403)
        self.client.force_login(self.staff)
        response = self.client.get(alert)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["new_requests"], 1)
        self.assertNotIn("Test QA Customer", response.content.decode())

    def test_staff_without_order_permission_cannot_view_or_review(self):
        self.submit()
        self.client.force_login(self.reviewer)
        self.assertEqual(self.client.get(reverse("company:commerce-qa-requests")).status_code, 403)
