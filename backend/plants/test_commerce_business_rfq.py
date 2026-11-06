"""Compact B2B RFQ QA journey, independent of stock reservations and installations."""
import os
import uuid
from unittest.mock import patch

from django.test import TestCase, override_settings
from django.urls import reverse

from .commerce_models import CommerceBusinessRFQ, CommerceProduct, CommerceSKU
from .models import InstallationBatch, DemoOrder, PlantUnit
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class BusinessRFQFlowTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.flag = patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "1"})
        self.flag.start()
        self.addCleanup(self.flag.stop)
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.product = CommerceProduct.objects.create(
            name="Business QA plant", kind="plant", created_by=self.staff)
        self.sku = CommerceSKU.objects.create(
            code="B2B-QA-001", product=self.product, price="350.00")
        self.url = reverse("plantupia-commerce-b2b-qa-rfqs")

    def body(self, **changes):
        request = {
            "client_id": str(uuid.uuid4()), "company_name": "Demo Office",
            "contact_name": "Procurement QA", "phone": "+20 101 222 3333",
            "city": "Cairo", "quantity": 70,
            "sku_id": str(self.sku.pk),
            "scope": "70 plants for an office reception and regular care.",
            "confirm_qa_no_contract": True,
        }
        request.update(changes)
        return request

    def submit(self, **changes):
        response = self.api.post(self.url, self.body(**changes), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        return response.data

    def test_rfq_quote_and_customer_acknowledgement_do_not_create_sale(self):
        before_plants = PlantUnit.objects.count()
        before_installations = InstallationBatch.objects.count()
        rfq_id = self.submit()["id"]
        self.client.force_login(self.staff)
        inbox = self.client.get(reverse("company:business-rfqs"))
        self.assertEqual(inbox.status_code, 200)
        details_url = reverse("company:business-rfq", args=[rfq_id])
        detail = self.client.get(details_url)
        self.assertEqual(detail.status_code, 200)
        self.assertContains(detail, "Demo Office")
        token = detail.context["form"].initial["token"]
        quoted = self.client.post(details_url, {
            "token": token, "action": "quote", "amount": "18500.00",
            "note": "QA draft estimate only", "confirmed": "on",
        })
        self.assertEqual(quoted.status_code, 302, quoted.content)
        self.api.force_authenticate(self.owner)
        own = self.api.get(self.url)
        self.assertEqual(own.status_code, 200)
        self.assertEqual(own.data["results"][0]["quoted_amount"], "18500.00")
        accepted = self.api.post(reverse("plantupia-commerce-b2b-qa-accept", args=[rfq_id]),
                                 {}, format="json")
        self.assertEqual(accepted.status_code, 200, accepted.data)
        self.assertEqual(accepted.data["status"], "qa_accepted")
        self.assertTrue(accepted.data["qa_only"])
        self.assertEqual(PlantUnit.objects.count(), before_plants)
        self.assertEqual(InstallationBatch.objects.count(), before_installations)
        self.assertFalse(DemoOrder.objects.exists())

    def test_rfq_retry_owner_isolation_and_unsupported_items(self):
        identity = str(uuid.uuid4())
        first = self.submit(client_id=identity)
        second = self.submit(client_id=identity)
        self.assertEqual(first["id"], second["id"])
        modified = self.api.post(self.url, self.body(client_id=identity,
            scope="Different requirement changes the exact request."), format="json")
        self.assertEqual(modified.status_code, 400)
        self.assertEqual(CommerceBusinessRFQ.objects.count(), 1)
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(self.url).data["results"], [])
        self.assertEqual(self.api.post(
            reverse("plantupia-commerce-b2b-qa-accept", args=[first["id"]]), {},
            format="json").status_code, 404)

    def test_nonbinding_gate_and_validation(self):
        self.assertEqual(self.api.post(self.url, self.body(
            confirm_qa_no_contract=False), format="json").status_code, 400)
        self.assertEqual(self.api.post(self.url, self.body(
            quantity=10001), format="json").status_code, 400)
        self.assertEqual(self.api.post(self.url, self.body(
            scope="small"), format="json").status_code, 400)
        self.assertFalse(CommerceBusinessRFQ.objects.exists())
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "0"}):
            self.assertEqual(self.api.get(self.url).status_code, 404)

    def test_nonstaff_cannot_open_company_quote_workspace(self):
        self.submit()
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:business-rfqs")).status_code, 403)
        self.client.force_login(self.reviewer)
        self.assertEqual(self.client.get(reverse("company:business-rfqs")).status_code, 403)
