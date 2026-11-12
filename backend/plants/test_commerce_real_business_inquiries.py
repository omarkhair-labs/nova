"""Actual B2B discovery/quotation separate from QA RFQs and purchase orders."""
import uuid

from django.test import TestCase, override_settings
from django.urls import reverse

from .commerce_models import CommerceBusinessInquiry, CommerceBusinessRFQ, CommerceOrder
from .models import DemoOrder, InstallationBatch
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class RealBusinessInquiryTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.url = reverse("plantupia-real-business-inquiries")
        self.body = {
            "client_id": str(uuid.uuid4()),
            "kind": "installation",
            "company_name": "Real office client",
            "contact_name": "Project director",
            "phone": "+201001234567",
            "email": "project@example.org",
            "city": "Cairo",
            "quantity": 80,
            "scope": "Supply plants and plan maintenance and installation for a real office.",
        }

    def test_submission_uses_real_nonbinding_records_only(self):
        old_qa = CommerceBusinessRFQ.objects.count()
        old_installations = InstallationBatch.objects.count()
        response = self.api.post(self.url, self.body, format="json")
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(response.data["status"], "received")
        self.assertEqual(response.data["kind"], "installation")
        self.assertIsNone(response.data["quoted_amount"])
        self.assertEqual(CommerceBusinessInquiry.objects.count(), 1)
        self.assertEqual(CommerceBusinessRFQ.objects.count(), old_qa)
        self.assertEqual(CommerceOrder.objects.count(), 0)
        self.assertEqual(DemoOrder.objects.count(), 0)
        self.assertEqual(InstallationBatch.objects.count(), old_installations)

    def test_company_quote_and_customer_accept_requires_current_revision(self):
        record = self.api.post(self.url, self.body, format="json").data
        oid = record["id"]
        detail = reverse("company:business-inquiry", args=[oid])
        quote = reverse("company:business-inquiry-quote", args=[oid])
        self.assertEqual(self.client.get(reverse("company:business-inquiries")).status_code, 200)
        self.assertEqual(self.client.get(detail).status_code, 200)
        self.assertEqual(self.client.post(quote, {
            "action": "quote", "quote_version": "0", "amount": "45000.00",
            "note": "Includes a proposed supply and installation scope only.",
        }).status_code, 302)
        accepted = reverse("plantupia-real-business-accept", args=[oid])
        self.assertEqual(self.api.post(accepted, {"quote_version": 0}, format="json").status_code, 400)
        ok = self.api.post(accepted, {"quote_version": 1}, format="json")
        self.assertEqual(ok.status_code, 200, ok.data)
        self.assertEqual(ok.data["status"], "accepted")
        self.assertEqual(ok.data["quoted_amount"], "45000.00")
        self.assertEqual(self.api.post(accepted, {"quote_version": 1}, format="json").status_code, 200)
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=oid).quote_version, 1)
        self.assertEqual(CommerceOrder.objects.count(), 0)
        self.assertEqual(self.client.post(quote, {
            "action": "quote", "quote_version": "1", "amount": "47000.00",
            "note": "Trying to rewrite a quote already accepted by the customer.",
        }).status_code, 302)
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=oid).quoted_amount, 45000)

    def test_idempotency_and_customer_scoping(self):
        first = self.api.post(self.url, self.body, format="json")
        replay = self.api.post(self.url, self.body, format="json")
        self.assertEqual(first.status_code, 201)
        self.assertEqual(replay.status_code, 201)
        self.assertEqual(first.data["id"], replay.data["id"])
        changed = dict(self.body, quantity=90)
        self.assertEqual(self.api.post(self.url, changed, format="json").status_code, 400)
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(self.url).data["results"], [])
        self.assertEqual(self.api.post(
            reverse("plantupia-real-business-accept", args=[first.data["id"]]),
            {"quote_version": 1}, format="json").status_code, 404)
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:business-inquiries")).status_code, 403)
        self.assertEqual(CommerceBusinessInquiry.objects.count(), 1)


    def test_accepted_installation_quote_creates_one_draft_project_only(self):
        from .models import Space
        record = self.api.post(self.url, self.body, format="json").data
        oid = record["id"]
        url = reverse("company:business-inquiry-installation", args=[oid])
        before = InstallationBatch.objects.count()
        # A submitted or merely quoted RFQ cannot create an installation.
        self.assertEqual(self.client.post(url).status_code, 302)
        self.assertEqual(InstallationBatch.objects.count(), before)
        quote_url = reverse("company:business-inquiry-quote", args=[oid])
        self.client.post(quote_url, {
            "action": "quote", "quote_version": "0", "amount": "45000.00",
            "note": "Physical installation proposed, contingent on signed agreement.",
        })
        self.assertEqual(self.client.post(url).status_code, 302)
        self.assertEqual(InstallationBatch.objects.count(), before)
        self.api.post(
            reverse("plantupia-real-business-accept", args=[oid]),
            {"quote_version": 1}, format="json",
        )
        self.assertEqual(self.client.post(url).status_code, 302)
        batch = InstallationBatch.objects.get(source_b2b_inquiry_id=oid)
        self.assertEqual(batch.status, "draft")
        self.assertEqual(batch.prepared_by_id, self.staff.pk)
        self.assertEqual(batch.space.owner_id, self.staff.pk)
        self.assertEqual(batch.manifest_rows.count(), 0)
        self.assertEqual(batch.invitations.count(), 0)
        self.assertEqual(batch.space.plants.count(), 0)
        self.assertIn(str(oid), batch.space.description)
        self.assertEqual(self.client.post(url).status_code, 302)
        self.assertEqual(InstallationBatch.objects.count(), before + 1)
        self.assertEqual(Space.objects.filter(pk=batch.space_id).count(), 1)

    def test_wholesale_and_unauthorized_users_cannot_create_installations(self):
        body = dict(self.body, kind="wholesale")
        oid = self.api.post(self.url, body, format="json").data["id"]
        self.client.post(reverse("company:business-inquiry-quote", args=[oid]), {
            "action": "quote", "quote_version": "0", "amount": "10000.00",
            "note": "Wholesale-only supply; not an installation project.",
        })
        self.api.post(reverse("plantupia-real-business-accept", args=[oid]),
                      {"quote_version": 1}, format="json")
        count = InstallationBatch.objects.count()
        self.client.post(reverse("company:business-inquiry-installation", args=[oid]))
        self.assertEqual(InstallationBatch.objects.count(), count)
        self.client.force_login(self.owner)
        self.assertEqual(
            self.client.post(reverse("company:business-inquiry-installation", args=[oid])).status_code,
            403,
        )
        self.assertEqual(InstallationBatch.objects.count(), count)
