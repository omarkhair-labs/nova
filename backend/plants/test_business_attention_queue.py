"""Chapter 07: staff work attention is derived from REAL B2B record states.

No new CRM, fabricated replies or implied binding purchase order.
"""
import uuid

from django.test import TestCase, override_settings
from django.urls import reverse

from .company_roles import create_b2b_commercial_role
from .commerce_models import CommerceBusinessInquiry, CommerceBusinessContactNote
from .models import InstallationBatch
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class RealBusinessAttentionQueueTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        # Do not inherit the QA operator's installation privileges in this test.
        self.staff.groups.clear()
        self.staff.groups.add(create_b2b_commercial_role())
        self.staff.refresh_from_db()
        self.client.force_login(self.staff)

    def enquiry(self, *, kind="installation", company_name="Fictional QA office"):
        payload = {
            "client_id": str(uuid.uuid4()), "kind": kind,
            "company_name": company_name, "contact_name": "Test contact",
            "phone": "+201001234567", "city": "Cairo",
            "quantity": 12 if kind == "installation" else 36,
            "scope": "Synthetic business site requirements; no purchase order or commitment.",
        }
        result = self.api.post(
            reverse("plantupia-real-business-inquiries"), payload, format="json",
        )
        self.assertEqual(result.status_code, 201, result.data)
        return result.data["id"]

    def current_queue(self, status=None):
        uri = reverse("company:business-inquiries")
        if status:
            uri += "?status=" + status
        response = self.client.get(uri)
        self.assertEqual(response.status_code, 200)
        return response

    def test_staff_queue_identifies_work_waiting_and_accepted_follow_up(self):
        inquiry_id = self.enquiry()
        received = self.current_queue()
        self.assertEqual(received.context["pending_count"], 1)
        self.assertEqual(received.context["waiting_count"], 0)
        self.assertEqual(received.context["followup_count"], 0)
        self.assertEqual(
            [str(x.pk) for x in received.context["records"]], [inquiry_id],
        )
        self.assertContains(received, "Review scope and decide on a quotation")

        detail = self.client.get(reverse("company:business-inquiry", args=[inquiry_id]))
        self.assertContains(detail, "Review the need and prepare a quotation")
        quoted = self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": "0",
            "amount": "32500.00",
            "note": "Test scope includes intended office plants, site review still required.",
        })
        self.assertEqual(quoted.status_code, 302)
        waiting = self.current_queue()
        self.assertEqual(waiting.context["waiting_count"], 1)
        self.assertEqual(list(waiting.context["records"]), [])
        self.assertContains(self.current_queue("quoted"), "Waiting for the customer's response")

        accepted = self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]), {
            "quote_version": 1,
        }, format="json")
        self.assertEqual(accepted.status_code, 200, accepted.data)
        action = self.current_queue()
        self.assertEqual(action.context["pending_count"], 0)
        self.assertEqual(action.context["followup_count"], 1)
        self.assertEqual([str(x.pk) for x in action.context["records"]], [inquiry_id])
        self.assertContains(action, "Follow up; authorized staff may prepare installation draft")
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())
        detail = self.client.get(reverse("company:business-inquiry", args=[inquiry_id]))
        self.assertContains(detail, "Prepare the project draft after commercial follow-up")

    def test_linked_draft_is_not_stuck_in_new_business_request_attention(self):
        inquiry_id = self.enquiry()
        self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": "0", "amount": "10000.00",
            "note": "Nonbinding test office installation scope with site review.",
        })
        response = self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]), {
            "quote_version": 1,
        }, format="json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.current_queue().context["followup_count"], 1)
        denied = self.client.post(reverse("company:business-inquiry-installation", args=[inquiry_id]))
        self.assertEqual(denied.status_code, 403)
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])
        self.client.force_login(self.staff)
        created = self.client.post(reverse("company:business-inquiry-installation", args=[inquiry_id]))
        self.assertEqual(created.status_code, 302)
        self.assertEqual(InstallationBatch.objects.filter(
            source_b2b_inquiry_id=inquiry_id).count(), 1)
        latest = self.current_queue()
        self.assertEqual(latest.context["followup_count"], 0)
        self.assertEqual(list(latest.context["records"]), [])
        still_visible = self.current_queue("all")
        self.assertContains(still_visible, "Continue linked installation from Projects")
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=inquiry_id).status, "accepted")

    def test_accepted_wholesale_is_follow_up_not_an_installation(self):
        inquiry_id = self.enquiry(kind="wholesale", company_name="Fictional QA bulk buyer")
        self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": "0", "amount": "6500.00",
            "note": "Test wholesale proposal only; delivery and payment subject to contract.",
        })
        response = self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]), {
            "quote_version": 1,
        }, format="json")
        self.assertEqual(response.status_code, 200)
        inbox = self.current_queue()
        self.assertEqual(inbox.context["followup_count"], 1)
        self.assertContains(inbox, "Follow up on wholesale supply terms")
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())

    def test_installation_may_start_without_unknown_quantity_but_wholesale_cannot(self):
        payload = {
            "client_id": str(uuid.uuid4()), "kind": "installation",
            "company_name": "QA site not yet surveyed",
            "contact_name": "Facility representative", "phone": "+201001234567",
            "city": "Cairo",
            "scope": "Office site with plant requirements not yet measured or decided.",
        }
        unknown = self.api.post(reverse("plantupia-real-business-inquiries"), payload, format="json")
        self.assertEqual(unknown.status_code, 201, unknown.data)
        self.assertIsNone(unknown.data["quantity"])
        row = CommerceBusinessInquiry.objects.get(pk=unknown.data["id"])
        self.assertIsNone(row.quantity)
        self.assertContains(self.current_queue(), "Site quantity not determined yet")
        payload.update(kind="wholesale", client_id=str(uuid.uuid4()))
        missing = self.api.post(reverse("plantupia-real-business-inquiries"), payload, format="json")
        self.assertEqual(missing.status_code, 400)
        payload["quantity"] = 0
        invalid = self.api.post(reverse("plantupia-real-business-inquiries"), payload, format="json")
        self.assertEqual(invalid.status_code, 400)
        self.assertEqual(CommerceBusinessInquiry.objects.count(), 1)

    def test_external_contact_moves_only_staff_work_not_customer_contract(self):
        inquiry_id = self.enquiry(kind="wholesale", company_name="QA telephone follow-up")
        self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": 0, "amount": "4500.00",
            "note": "Synthetic quotation; commercial terms are not contractual.",
        })
        accepted = self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]),
            {"quote_version": 1}, format="json")
        self.assertEqual(accepted.status_code, 200)
        self.assertEqual(self.current_queue().context["followup_count"], 1)
        url = reverse("company:business-external-contact", args=[inquiry_id])
        note = self.client.post(url, {
            "channel": "phone",
            "summary": "Spoke to the buyer on the phone; they will confirm their purchasing timeline.",
            "followup_state": "waiting_outside",
        })
        self.assertEqual(note.status_code, 302)
        row = CommerceBusinessInquiry.objects.get(pk=inquiry_id)
        self.assertEqual(row.status, "accepted")
        self.assertEqual(row.followup_state, "waiting_outside")
        self.assertEqual(row.quoted_amount, 4500)
        self.assertEqual(self.current_queue().context["followup_count"], 0)
        self.assertEqual(self.current_queue().context["outside_count"], 1)
        self.assertContains(self.current_queue("waiting_outside"), "Waiting: Staff recorded an external reply")
        self.assertContains(self.client.get(reverse("company:business-inquiry", args=[inquiry_id])),
            "Spoke to the buyer on the phone")
        done = self.client.post(url, {
            "channel": "whatsapp",
            "summary": "Staff completed the commercial follow-up outside the system.",
            "followup_state": "recorded_done",
        })
        self.assertEqual(done.status_code, 302)
        row.refresh_from_db()
        self.assertEqual(row.followup_state, "recorded_done")
        self.assertEqual(row.status, "accepted")
        self.assertEqual(self.current_queue().context["outside_count"], 0)
        self.assertEqual(self.current_queue().context["followup_count"], 0)
        self.assertEqual(CommerceBusinessContactNote.objects.filter(inquiry_id=inquiry_id).count(), 2)
        for activity in CommerceBusinessContactNote.objects.filter(inquiry_id=inquiry_id):
            self.assertEqual(activity.recorded_by_id, self.staff.id)
        # Staff records are private, and no customer sale or quote acceptance was fabricated.
        customer = self.api.get(reverse("plantupia-real-business-inquiries"))
        self.assertEqual(customer.status_code, 200)
        self.assertNotIn("followup_state", customer.data["results"][0])
        self.assertNotIn("staff_contact_notes", customer.data["results"][0])
        self.assertNotIn("Spoke to the buyer", str(customer.data))
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())
        reopened = self.client.post(url, {
            "channel": "other",
            "summary": "Further real action is now needed after the previous follow-up.",
            "followup_state": "needs_staff",
        })
        self.assertEqual(reopened.status_code, 302)
        self.assertEqual(self.current_queue().context["followup_count"], 1)

    def test_external_notes_do_not_forge_early_completion_and_require_staff_rights(self):
        inquiry_id = self.enquiry()
        url = reverse("company:business-external-contact", args=[inquiry_id])
        invalid = self.client.post(url, {
            "channel": "phone",
            "summary": "Contact made outside; staff reports agreement.",
            "followup_state": "recorded_done",
        })
        self.assertEqual(invalid.status_code, 302)
        self.assertEqual(CommerceBusinessContactNote.objects.count(), 0)
        self.assertEqual(self.current_queue().context["pending_count"], 1)
        informational = self.client.post(url, {
            "channel": "meeting",
            "summary": "Met the facility manager; site dimensions are not confirmed.",
            "followup_state": "needs_staff",
        })
        self.assertEqual(informational.status_code, 302)
        self.assertEqual(CommerceBusinessContactNote.objects.count(), 1)
        self.client.force_login(self.owner)
        denied = self.client.post(url, {
            "channel": "phone", "summary": "Customer cannot record a private staff contact note.",
            "followup_state": "needs_staff",
        })
        self.assertEqual(denied.status_code, 403)
        self.assertEqual(CommerceBusinessContactNote.objects.count(), 1)

    def test_phone_reported_interest_moves_to_nonbinding_follow_up_with_real_provenance(self):
        inquiry_id = self.enquiry(kind="installation")
        self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": 0, "amount": "14000.00",
            "note": "Example installation proposal; subject to independent contract.",
        })
        uri = reverse("company:business-external-response", args=[inquiry_id])
        payload = {
            "channel": "phone", "decision": "interested",
            "summary": "Customer said by phone that they want to discuss installation next week; nothing signed.",
        }
        unconfirmed = self.client.post(uri, payload)
        self.assertEqual(unconfirmed.status_code, 302)
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=inquiry_id).status, "quoted")
        self.assertEqual(CommerceBusinessContactNote.objects.filter(inquiry_id=inquiry_id).count(), 0)
        response = self.client.post(uri, {**payload, "confirm_staff_report": "yes"})
        self.assertEqual(response.status_code, 302)
        row = CommerceBusinessInquiry.objects.get(pk=inquiry_id)
        self.assertEqual(row.status, "accepted")
        self.assertEqual(row.customer_response_source, "staff_phone")
        self.assertEqual(row.followup_state, "needs_staff")
        self.assertEqual(self.current_queue().context["followup_count"], 1)
        self.assertEqual(CommerceBusinessContactNote.objects.filter(inquiry_id=inquiry_id).count(), 1)
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())
        # A draft can be created later by an authorized project operator only.
        self.assertEqual(self.client.post(
            reverse("company:business-inquiry-installation", args=[inquiry_id])).status_code, 403)
        customer_result = self.api.get(reverse("plantupia-real-business-inquiries"))
        self.assertEqual(customer_result.data["results"][0]["customer_response_source"], "staff_phone")
        self.assertIn("Staff reported", customer_result.data["results"][0]["notice"])
        self.assertNotIn("Customer said by phone", str(customer_result.data))
        # Explicit in-app acknowledgement is stronger and updates attribution.
        independent = self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]),
            {"quote_version": 1}, format="json")
        self.assertEqual(independent.status_code, 200)
        self.assertEqual(independent.data["customer_response_source"], "customer_app")
        self.assertEqual(CommerceBusinessInquiry.objects.get(pk=inquiry_id).status, "accepted")

    def test_external_customer_decline_is_differentiated_from_company_decline(self):
        inquiry_id = self.enquiry(kind="wholesale")
        self.client.post(reverse("company:business-inquiry-quote", args=[inquiry_id]), {
            "action": "quote", "quote_version": 0, "amount": "3000.00",
            "note": "Nonbinding wholesale test scope and quantities for review.",
        })
        uri = reverse("company:business-external-response", args=[inquiry_id])
        self.assertEqual(self.current_queue().context["waiting_count"], 1)
        response = self.client.post(uri, {
            "channel": "whatsapp", "decision": "not_proceeding",
            "summary": "Buyer wrote on WhatsApp that the timing is not suitable and will not proceed.",
            "confirm_staff_report": "yes",
        })
        self.assertEqual(response.status_code, 302)
        row = CommerceBusinessInquiry.objects.get(pk=inquiry_id)
        self.assertEqual(row.status, "withdrawn")
        self.assertEqual(row.customer_response_source, "staff_whatsapp")
        self.assertIsNone(row.accepted_at)
        self.assertEqual(self.current_queue().context["waiting_count"], 0)
        self.assertEqual(CommerceBusinessContactNote.objects.filter(inquiry_id=inquiry_id).count(), 1)
        self.assertFalse(InstallationBatch.objects.filter(source_b2b_inquiry_id=inquiry_id).exists())
        self.assertContains(self.current_queue("all"), "Customer no longer proceeding")
        self.assertEqual(CommerceBusinessInquiry.objects.filter(status="declined").count(), 0)
        self.assertEqual(self.api.post(reverse("plantupia-real-business-accept", args=[inquiry_id]),
            {"quote_version": 1}, format="json").status_code, 400)
