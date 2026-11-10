"""Genuine Plantupia order quotes, location stock reservations and cancellation."""
from datetime import timedelta
from io import BytesIO
from tempfile import TemporaryDirectory
import uuid

from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image

from .commerce_models import (
    CommerceProduct, CommerceSKU, CommerceStockLocation, CommerceOrder,
    CommerceOrderLine, CommerceStockReservation, CommerceRealOrderEvent,
)
from .commerce_publication import review_catalog_product
from .commerce_services import record_counted_stock, stock_balance
from .commerce_storefront import available_units
from .models import CatalogProduct, DemoOrder, DemoCartLine, PlantUnit
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class RealCustomerOrderTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        temp = TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        media = override_settings(MEDIA_ROOT=temp.name)
        media.enable()
        self.addCleanup(media.disable)
        self.real = CommerceProduct.objects.create(
            name="Plantupia real stock item", kind="planter",
            description="Exact genuine item description",
            created_by=self.staff, catalog_key="real-pot",
        )
        self.sku = CommerceSKU.objects.create(
            product=self.real, code="REAL-POT-001", option_label="Medium", price="200.00",
        )
        self.location_a = CommerceStockLocation.objects.create(code="cairo-a", name="Cairo stock A")
        self.location_b = CommerceStockLocation.objects.create(code="cairo-b", name="Cairo stock B")
        self.count(self.location_a, 3)
        self.count(self.location_b, 4)
        photo = Image.new("RGB", (140, 140), "#617e51")
        image = BytesIO()
        photo.save(image, format="JPEG")
        self.real.cover_photo.save("real-pot.jpg", ContentFile(image.getvalue()), save=False)
        self.real.image_description = "Actual ceramic planter in a warehouse"
        self.real.save(update_fields=["cover_photo", "image_description"])
        review_catalog_product(staff=self.staff, product_id=self.real.pk, action="approve")
        self.real.refresh_from_db()
        self.real.storefront_published_at = timezone.now()
        self.real.save(update_fields=["storefront_published_at"])
        self.cart_url = reverse("plantupia-real-commerce-cart")
        self.orders_url = reverse("plantupia-real-commerce-orders")

    def count(self, location, qty):
        return record_counted_stock(
            staff=self.staff, sku_id=self.sku.pk, location_id=location.pk,
            quantity_delta=qty, reason="opening" if qty > 0 else "correction",
            reference_note="Verified warehouse count", client_id=uuid.uuid4(),
        )

    def cart(self, qty=2):
        result = self.api.post(self.cart_url,
            {"sku_id": str(self.sku.pk), "quantity": qty}, format="json")
        self.assertEqual(result.status_code, 200, result.data)
        return result.data

    def quote_body(self, *, client_id=None, fingerprint=None, **changes):
        body = {
            "client_id": str(client_id or uuid.uuid4()),
            "cart_fingerprint": fingerprint or self.api.get(self.cart_url).data["fingerprint"],
            "contact_name": "Customer Name",
            "contact_phone": "+201001234567",
            "city": "Cairo",
            "delivery_address": "A real customer-entered delivery address",
            "delivery_latitude": "30.0444200",
            "delivery_longitude": "31.2357100",
            "delivery_pin_confirmed": True,
            "building_details": "Building 4, floor 2",
            "payment_preference": "cod",
        }
        body.update(changes)
        return body

    def request_order(self, **changes):
        result = self.api.post(self.orders_url, self.quote_body(**changes), format="json")
        self.assertEqual(result.status_code, 201, getattr(result, "data", None))
        return result

    def company_quote(self, order_id, fee="35.00", version=0):
        return self.client.post(reverse("company:commerce-order-shipping", args=[order_id]),
                                {"action": "quote", "version": version, "shipping_fee": fee})

    def accept(self, order_id, version=1):
        return self.api.post(reverse("plantupia-real-commerce-accept-quote", args=[order_id]),
                             {"quote_version": version}, format="json")

    def test_real_checkout_quote_requires_map_and_is_not_a_charge(self):
        self.cart()
        before_count = stock_balance(self.sku.pk)
        rejected = self.api.post(self.orders_url, self.quote_body(
            delivery_pin_confirmed=False), format="json")
        self.assertEqual(rejected.status_code, 400)
        result = self.request_order()
        order = CommerceOrder.objects.get()
        self.assertEqual(result.data["status"], "quote_pending")
        self.assertIsNone(result.data["final_total"])
        self.assertIsNone(result.data["shipping_fee"])
        self.assertFalse(result.data["charged"])
        self.assertFalse(result.data["delivery_initiated"])
        self.assertFalse(result.data["stock_reserved"])
        self.assertEqual(order.lines.count(), 1)
        self.assertEqual(order.lines.first().sku_code_snapshot, self.sku.code)
        self.assertEqual(order.subtotal, 400)
        self.assertFalse(CommerceStockReservation.objects.exists())
        self.assertEqual(stock_balance(self.sku.pk), before_count)
        self.assertFalse(DemoOrder.objects.exists())
        self.assertFalse(DemoCartLine.objects.exists())
        self.assertFalse(CatalogProduct.objects.filter(name=self.real.name).exists())
        self.assertFalse(PlantUnit.objects.filter(label=self.real.name).exists())

    def test_company_quote_then_customer_accepts_exact_total_and_stock_reserves(self):
        self.cart(5)
        order_id = self.request_order().data["id"]
        order = CommerceOrder.objects.get(pk=order_id)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-order", args=[order_id])).status_code, 200)
        self.assertEqual(self.company_quote(order_id, fee="50.00").status_code, 302)
        order.refresh_from_db()
        self.assertEqual(order.status, "quoted")
        self.assertEqual(order.shipping_quote_version, 1)
        self.assertEqual(order.payment_status, "unpaid")
        self.assertFalse(CommerceStockReservation.objects.exists())
        expired_version = self.accept(order_id, 2)
        self.assertEqual(expired_version.status_code, 400)
        result = self.accept(order_id, 1)
        self.assertEqual(result.status_code, 200, result.data)
        self.assertEqual(result.data["status"], "confirmed")
        self.assertEqual(result.data["final_total"], "1050.00")
        self.assertFalse(result.data["charged"])
        self.assertTrue(result.data["stock_reserved"])
        self.assertEqual(
            sorted(CommerceStockReservation.objects.values_list("quantity", flat=True)), [2, 3],
        )
        self.assertEqual(available_units(self.sku.pk), 2)
        self.assertEqual(self.accept(order_id, 1).status_code, 200)
        self.assertEqual(CommerceStockReservation.objects.count(), 2)
        self.assertEqual(self.api.get(self.cart_url).data["results"], [])

    def test_two_customer_quotes_cannot_oversell_same_counted_stock(self):
        self.cart(5)
        owner_order_id = self.request_order().data["id"]
        self.api.force_authenticate(self.stranger)
        self.cart(5)
        stranger_order_id = self.request_order().data["id"]
        self.company_quote(owner_order_id, fee="0")
        self.company_quote(stranger_order_id, fee="0")
        self.api.force_authenticate(self.owner)
        self.assertEqual(self.accept(owner_order_id).status_code, 200)
        self.api.force_authenticate(self.stranger)
        denied = self.accept(stranger_order_id)
        self.assertEqual(denied.status_code, 400)
        self.assertEqual(sum(r.quantity for r in CommerceStockReservation.objects.all()), 5)
        self.assertEqual(available_units(self.sku.pk), 2)
        self.assertEqual(CommerceOrder.objects.get(pk=stranger_order_id).status, "quoted")

    def test_stock_counts_cannot_cut_below_live_reservation(self):
        self.cart(5)
        oid = self.request_order().data["id"]
        self.company_quote(oid, fee="30")
        self.accept(oid)
        # A is wholly held (3), B has 2 reserved; reducing B to 1 would contradict the hold.
        response = self.client.post(
            reverse("company:commerce-stock-count", args=[self.sku.pk]),
            {"location": str(self.location_b.pk), "quantity_delta": -3,
             "reason": "correction", "reference_note": "Count changed",
             "client_id": str(uuid.uuid4())},
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(stock_balance(self.sku.pk, self.location_b.pk), 4)
        self.assertEqual(available_units(self.sku.pk), 2)

    def test_cancellation_releases_exact_holds_once(self):
        self.cart(5)
        oid = self.request_order().data["id"]
        self.company_quote(oid, fee="0")
        self.accept(oid)
        result = self.api.post(reverse("plantupia-real-commerce-cancel", args=[oid]), {}, format="json")
        self.assertEqual(result.status_code, 200)
        self.assertEqual(result.data["status"], "cancelled")
        self.assertFalse(result.data["stock_reserved"])
        self.assertEqual(available_units(self.sku.pk), 7)
        self.assertEqual(CommerceStockReservation.objects.filter(released_at__isnull=True).count(), 0)
        self.assertEqual(self.api.post(reverse("plantupia-real-commerce-cancel", args=[oid]),
                                       {}, format="json").status_code, 200)
        self.assertEqual(available_units(self.sku.pk), 7)

    def test_quote_request_retry_is_idempotent_and_immutable(self):
        self.cart(2)
        identity = uuid.uuid4()
        body = self.quote_body(client_id=identity)
        first = self.api.post(self.orders_url, body, format="json")
        again = self.api.post(self.orders_url, body, format="json")
        self.assertEqual(first.status_code, 201)
        self.assertEqual(again.status_code, 201)
        self.assertEqual(first.data["id"], again.data["id"])
        mutated = dict(body, city="Alexandria")
        rejected = self.api.post(self.orders_url, mutated, format="json")
        self.assertEqual(rejected.status_code, 400)
        self.assertEqual(CommerceOrder.objects.count(), 1)
        self.assertEqual(CommerceOrderLine.objects.count(), 1)

    def test_customer_cannot_view_or_cancel_another_customers_order(self):
        self.cart()
        oid = self.request_order().data["id"]
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(reverse("plantupia-real-commerce-order", args=[oid])).status_code, 404)
        self.assertEqual(self.api.post(reverse("plantupia-real-commerce-cancel", args=[oid]),
                                       {}, format="json").status_code, 404)
        self.api.force_authenticate(self.staff)
        self.assertEqual(self.api.get(self.orders_url).status_code, 403)

    def test_price_or_publication_change_blocks_quote_acceptance(self):
        self.cart()
        oid = self.request_order().data["id"]
        self.company_quote(oid, fee="0")
        self.client.post(reverse("company:commerce-sku-edit", args=[self.sku.pk]), {
            "code": self.sku.code, "option_label": self.sku.option_label,
            "price": "210.00", "currency": "EGP",
        })
        rejected = self.accept(oid)
        self.assertEqual(rejected.status_code, 400)
        self.assertFalse(CommerceStockReservation.objects.exists())
        self.assertEqual(CommerceOrder.objects.get(pk=oid).status, "quoted")

    def test_real_order_timeline_is_customer_scoped_and_idempotent(self):
        self.cart(2)
        oid = self.request_order().data["id"]
        initial = self.api.get(reverse("plantupia-real-commerce-order", args=[oid])).data
        self.assertEqual([event["kind"] for event in initial["timeline"]], ["quote_requested"])
        self.assertEqual(self.company_quote(oid, fee="25.00").status_code, 302)
        quoted = self.api.get(reverse("plantupia-real-commerce-order", args=[oid])).data
        self.assertEqual([event["kind"] for event in quoted["timeline"]],
                         ["quote_requested", "shipping_quoted"])
        self.assertEqual(self.accept(oid).status_code, 200)
        self.assertEqual(self.accept(oid).status_code, 200)
        accepted = self.api.get(reverse("plantupia-real-commerce-order", args=[oid])).data
        self.assertEqual([event["kind"] for event in accepted["timeline"]],
                         ["quote_requested", "shipping_quoted", "quote_accepted"])
        self.assertNotIn("delivered", str(accepted["timeline"]).lower())
        self.assertEqual(self.api.post(reverse("plantupia-real-commerce-cancel", args=[oid]),
                                       {}, format="json").status_code, 200)
        self.assertEqual(self.api.post(reverse("plantupia-real-commerce-cancel", args=[oid]),
                                       {}, format="json").status_code, 200)
        self.assertEqual(list(CommerceRealOrderEvent.objects.filter(order_id=oid)
                              .values_list("kind", flat=True)),
                         ["quote_requested", "shipping_quoted", "quote_accepted", "cancelled"])
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(reverse("plantupia-real-commerce-order", args=[oid])).status_code, 404)
