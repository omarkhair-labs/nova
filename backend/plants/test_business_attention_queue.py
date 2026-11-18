"""Chapter 07: staff work attention is derived from REAL B2B record states.

No new CRM, fabricated replies or implied binding purchase order.
"""
import uuid

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from .company_roles import create_b2b_commercial_role
from .commerce_models import CommerceBusinessInquiry
from .models import InstallationBatch
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class RealBusinessAttentionQueueTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
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
        self.assertFalse(InstallationBatch.objects.exists())
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
        self.assertFalse(InstallationBatch.objects.exists())
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
        self.assertFalse(InstallationBatch.objects.exists())
