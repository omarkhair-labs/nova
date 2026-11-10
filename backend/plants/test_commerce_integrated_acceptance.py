"""One coherent actual SKU/order/merchant/finance/B2B acceptance contract.

All data here is isolated test data. This does not constitute real-device
acceptance, authorized merchant settlement, or production activation.
"""
from io import BytesIO
from tempfile import TemporaryDirectory
import uuid

from django.contrib.auth import get_user_model
from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from django.urls import reverse
from PIL import Image

from .company_roles import (
    create_commerce_operator_role, create_finance_reviewer_role,
    create_b2b_commercial_role,
)
from .commerce_models import (
    CommerceProduct, CommerceSKU, CommerceStockLocation, CommerceOrder,
    CommerceStockReservation, CommerceStockMovement, CommercePaymentRecord,
    CommerceRefundRecord, CommerceBusinessInquiry,
)
from .commerce_services import record_counted_stock, stock_balance
from .commerce_storefront import available_units
from .models import DemoOrder
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS, PLANTUPIA_STOREFRONT_PUBLICATION_ENABLED=True)
class CommercialIntegratedAcceptanceTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.groups.add(create_commerce_operator_role())
        self.staff.refresh_from_db()
        self.client.force_login(self.staff)
        User = get_user_model()
        self.finance = User.objects.create_user(
            username="plantupia_finance_acceptance", email="plantupia-finance@unit.test",
            password="not-a-production-password", is_staff=True,
        )
        self.finance.groups.add(create_finance_reviewer_role())
        self.b2b = User.objects.create_user(
            username="plantupia_b2b_acceptance", email="plantupia-b2b@unit.test",
            password="not-a-production-password", is_staff=True,
        )
        self.b2b.groups.add(create_b2b_commercial_role())
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        media = override_settings(MEDIA_ROOT=temporary.name)
        media.enable()
        self.addCleanup(media.disable)
        self.item = CommerceProduct.objects.create(
            name="Actual accepted sample pot", description="Test record for one real CommerceSKU flow",
            kind="planter", created_by=self.staff, catalog_key="acceptance-pot",
        )
        self.sku = CommerceSKU.objects.create(
            product=self.item, code="ACCEPTANCE-POT-01", option_label="Medium", price="200.00",
        )
        location = CommerceStockLocation.objects.create(code="acceptance-warehouse", name="Warehouse")
        record_counted_stock(
            staff=self.staff, sku_id=self.sku.pk, location_id=location.pk,
            quantity_delta=3, reason="opening", reference_note="Accepted local test count",
            client_id=uuid.uuid4(),
        )
        img = Image.new("RGB", (144, 144), "#6d8252")
        buf = BytesIO()
        img.save(buf, format="JPEG")
        self.item.cover_photo.save("sample-pot.jpg", ContentFile(buf.getvalue()), save=False)
        self.item.image_description = "Single photographed sample planter"
        self.item.save(update_fields=["cover_photo", "image_description"])

    def test_one_record_from_android_api_to_real_company_dispatch_finance_return_and_b2b(self):
        from .commerce_models import CommerceRealOrderEvent

        catalog_url = reverse("plantupia-real-commerce-catalog")
        cart_url = reverse("plantupia-real-commerce-cart")
        orders_url = reverse("plantupia-real-commerce-orders")
        publish_url = reverse("company:commerce-product-publication", args=[self.item.pk])
        review_url = reverse("company:commerce-product-review", args=[self.item.pk])

        # Content review and publication are separate staff-controlled gates.
        self.assertEqual(self.api.get(catalog_url).data["results"], [])
        self.assertEqual(self.client.post(publish_url, {"action": "publish"}).status_code, 302)
        self.assertEqual(self.api.get(catalog_url).data["results"], [])
        self.assertEqual(self.client.post(review_url, {"action": "approve"}).status_code, 302)
        self.assertEqual(self.api.get(catalog_url).data["results"], [])
        self.assertEqual(self.client.post(publish_url, {"action": "publish"}).status_code, 302)
        self.assertEqual(self.api.get(catalog_url).data["results"][0]["sku"], self.sku.code)

        # The same cart/quote/order is consumed by customer API and Company.
        add = self.api.post(cart_url, {
            "sku_id": str(self.sku.pk), "quantity": 2,
        }, format="json")
        self.assertEqual(add.status_code, 200, add.data)
        fingerprint = add.data["fingerprint"]
        quote = self.api.post(orders_url, {
            "client_id": str(uuid.uuid4()), "cart_fingerprint": fingerprint,
            "contact_name": "Real test customer", "contact_phone": "+201001234567",
            "city": "Cairo", "delivery_address": "Building 4, real entry",
            "building_details": "Second floor, unit 3", "landmark": "Public landmark",
            "delivery_latitude": "30.0444200", "delivery_longitude": "31.2357100",
            "delivery_pin_confirmed": True, "payment_preference": "cod",
        }, format="json")
        self.assertEqual(quote.status_code, 201, quote.data)
        oid = quote.data["id"]
        self.assertEqual(CommerceOrder.objects.count(), 1)
        self.assertEqual(CommerceStockReservation.objects.count(), 0)
        self.assertEqual(self.client.get(reverse("company:commerce-order", args=[oid])).status_code, 200)
        shipping = self.client.post(reverse("company:commerce-order-shipping", args=[oid]), {
            "action": "quote", "version": 0, "shipping_fee": "25.00",
        })
        self.assertEqual(shipping.status_code, 302)
        accepted = self.api.post(reverse("plantupia-real-commerce-accept-quote", args=[oid]), {
            "quote_version": 1,
        }, format="json")
        self.assertEqual(accepted.status_code, 200, accepted.data)
        self.assertEqual(accepted.data["final_total"], "425.00")
        self.assertTrue(accepted.data["stock_reserved"])
        self.assertEqual(available_units(self.sku.pk), 1)

        # Staff fulfills an actual confirmed order, without claiming payment.
        fulfillment_url = reverse("company:commerce-order-fulfillment", args=[oid])
        self.assertEqual(self.client.post(fulfillment_url, {"action": "prepare"}).status_code, 302)
        handover = {
            "action": "dispatch", "carrier_name": "Actual local test team",
            "tracking_reference": "HANDOVER-TEST-0001",
            "dispatch_note": "Physical items counted and accepted by staff in test.",
        }
        self.assertEqual(self.client.post(fulfillment_url, handover).status_code, 302)
        self.assertEqual(self.client.post(fulfillment_url, handover).status_code, 302)
        self.assertEqual(CommerceStockMovement.objects.filter(reason="dispatch").count(), 1)
        self.assertEqual(stock_balance(self.sku.pk), 1)
        self.assertEqual(CommerceStockReservation.objects.filter(
            order_id=oid, released_at__isnull=True).count(), 0)
        self.assertEqual(self.client.post(fulfillment_url, {
            "action": "delivered", "proof_reference": "POD-TEST-0001",
        }).status_code, 302)
        self.assertEqual(CommerceOrder.objects.get(pk=oid).payment_status, "unpaid")

        # Finance reviewer has no warehouse or installation rights.
        self.client.force_login(self.finance)
        self.assertEqual(self.client.get(reverse("company:commerce-order", args=[oid])).status_code, 200)
        self.assertEqual(self.client.post(fulfillment_url, {"action": "prepare"}).status_code, 403)
        payment_url = reverse("company:commerce-order-payment", args=[oid])
        self.assertEqual(self.client.post(payment_url, {
            "amount": "425.00", "settlement_reference": "CASH-RECEIPT-0001",
            "evidence_note": "Bank-independent company cash ledger and signed receipt match.",
        }).status_code, 302)
        self.assertEqual(CommercePaymentRecord.objects.filter(order_id=oid).count(), 1)
        self.assertEqual(CommerceOrder.objects.get(pk=oid).payment_status, "verified")

        # Customer asks for a return; independent operator records physical receipt.
        returned = self.api.post(reverse("plantupia-real-commerce-return", args=[oid]), {
            "reason": "damaged", "note": "Item condition requires a return in this test.",
        }, format="json")
        self.assertEqual(returned.status_code, 200, returned.data)
        return_url = reverse("company:commerce-order-return", args=[oid])
        self.client.force_login(self.staff)
        self.assertEqual(self.client.post(return_url, {
            "action": "approve", "staff_note": "Damage accepted under documented return test policy.",
        }).status_code, 302)
        self.assertEqual(self.client.post(return_url, {
            "action": "receive", "evidence_reference": "RETURN-TEST-0001",
        }).status_code, 302)
        self.assertEqual(stock_balance(self.sku.pk), 1)

        # Refund is a finance attestation of an external event, NOT a payout.
        self.client.force_login(self.finance)
        refund = self.client.post(reverse("company:commerce-order-refund", args=[oid]), {
            "amount": "425.00", "refund_reference": "BANK-REFUND-TEST-0001",
            "refund_note": "Independently reconciled test external refund ledger reference.",
        })
        self.assertEqual(refund.status_code, 302)
        self.assertEqual(CommerceRefundRecord.objects.filter(order_id=oid).count(), 1)
        self.assertEqual(CommerceOrder.objects.get(pk=oid).payment_status, "refunded")
        self.assertEqual(CommerceRealOrderEvent.objects.filter(
            order_id=oid, kind="refund_verified").count(), 1)

        # B2B is nonbinding and cannot create a second shop or new order.
        inquiry = self.api.post(reverse("plantupia-real-business-inquiries"), {
            "client_id": str(uuid.uuid4()), "kind": "installation",
            "company_name": "Test office project", "contact_name": "Test decision-maker",
            "phone": "+201001234567", "city": "Cairo",
            "quantity": 25,
            "scope": "Commercial plant supply and installation for an office, pending contract.",
        }, format="json")
        self.assertEqual(inquiry.status_code, 201, inquiry.data)
        business_id = inquiry.data["id"]
        self.client.force_login(self.b2b)
        self.assertEqual(self.client.get(reverse("company:business-inquiry", args=[business_id])).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 403)
        self.assertEqual(self.client.post(reverse("company:business-inquiry-quote", args=[business_id]), {
            "action": "quote", "quote_version": 0, "amount": "30000.00",
            "note": "Proposed nonbinding office supply and installation scope.",
        }).status_code, 302)
        follow_up = self.api.post(reverse("plantupia-real-business-accept", args=[business_id]), {
            "quote_version": 1,
        }, format="json")
        self.assertEqual(follow_up.status_code, 200, follow_up.data)
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=business_id).status, "accepted")
        self.assertEqual(CommerceOrder.objects.count(), 1)
        self.assertEqual(DemoOrder.objects.count(), 0)
