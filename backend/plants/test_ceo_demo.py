"""Real persisted QA contracts. Provider mocks here are tests, never demo replies."""
import os
import uuid
import base64
from io import BytesIO
from tempfile import TemporaryDirectory
from concurrent.futures import ThreadPoolExecutor
from datetime import timedelta, datetime, timezone as datetime_timezone
from decimal import Decimal
from threading import Barrier
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.cache import caches
from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import close_old_connections
from django.test import Client, TestCase, TransactionTestCase, override_settings, skipUnlessDBFeature
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient
from PIL import Image

from . import company_services as company, demo_services as demo
from .company_roles import create_qa_roles
from .handovers import issue_pilot_invitation
from .models import (BotanicalSpecies, CareGuideVersion, PlantUnit, Space, InstallationBatch,
                     InstallationManifestRow, CareInspectionPlan, CarePlanEnrollment, CarePlanDecision,
                     DemoOffer, DemoEntitlement, DemoMutation, ServiceRequest, CatalogProduct,
                     DemoCartLine, DemoOrder, DemoOrderLine, PlantEvent, HandoverInvitation)

SETTINGS = dict(PLANTUPIA_CEO_DEMO_ENABLED=True, PLANTUPIA_COMPANY_DASHBOARD_ENABLED=True,
    REDIS_URL="redis://qa-unit-test-only",
    IS_PRODUCTION=False, ENVIRONMENT="development", PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"],
    STORAGES={**settings.STORAGES, "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}})


class DemoFixture:
    def setUp(self):
        for cache in caches.all():
            cache.clear()
        operator, reviewer = create_qa_roles()
        User = get_user_model()
        self.staff = User.objects.create_user(username="qa_ceo_operator", email="ceo-operator@example.test", password="qa-only", is_staff=True)
        self.staff.groups.add(operator)
        self.reviewer = User.objects.create_user(username="qa_ceo_reviewer", email="ceo-reviewer@example.test", password="qa-only", is_staff=True)
        self.reviewer.groups.add(reviewer)
        self.owner = User.objects.create_user(username="qa_ceo_customer", email="ceo-customer@example.test", password="qa-only")
        self.stranger = User.objects.create_user(username="qa_ceo_stranger", email="ceo-stranger@example.test", password="qa-only")
        self.space = Space.objects.create(owner=self.staff, name="FICTIONAL QA private installation")
        self.batch = InstallationBatch.objects.create(space=self.space, prepared_by=self.staff, reference_code="QA-CEO-FLOW")
        self.species = BotanicalSpecies.objects.create(key="qa-ceo-supplied", scientific_name="FICTIONAL QA supplied identity")
        self.guide = CareGuideVersion.objects.create(species=self.species, version=1,
            source_url="https://example.test/qa-source", source_reference="UNAPPROVED QA SOURCE",
            instructions="Synthetic review fixture only; not botanical advice.", published=True,
            reviewer=self.reviewer, reviewed_at=timezone.now(), assigned_reviewer=self.reviewer)
        self.plant = PlantUnit.objects.create(space=self.space, label="FICTIONAL QA plant 01", verified_species=self.species,
            placement_note="Synthetic QA placement", identity_reference="Explicit synthetic test identity, not botanical proof")
        self.unknown = PlantUnit.objects.create(space=self.space, label="FICTIONAL QA unknown 02")
        for index, plant in enumerate((self.plant, self.unknown), 1):
            InstallationManifestRow.objects.create(batch=self.batch, plant=plant, source_key=f"qa-{index}")
        self.plan = CareInspectionPlan.objects.create(plant=self.plant, guide=self.guide, action="leaves", every_days=3,
            start_on=timezone.localdate(), timezone="Africa/Cairo", instructions="QA ONLY: inspect and report what you actually see.",
            prepared_by=self.staff)
        self.ai = DemoOffer.objects.create(key="ai-care", name="AI Care", proposed_monthly_price="79.00", ai_daily_limit=35)
        self.expert = DemoOffer.objects.create(key="expert-care", name="Expert Care", proposed_monthly_price="199.00", human_requests_per_30_days=3)
        self.product = CatalogProduct.objects.create(name="FICTIONAL QA indoor plant", kind="plant", description="Synthetic catalog item; not commercial inventory.", proposed_price="125.50", qa_stock=5)
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.client.force_login(self.staff)
        self.flag = patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"})
        self.flag.start()
        self.addCleanup(self.flag.stop)

    def access(self, offer="expert-care"):
        response = self.api.post(reverse("plantupia-demo-access"), {"offer": offer, "client_id": str(uuid.uuid4()), "confirm_no_payment": True}, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        return response

    def approve(self):
        self.client.force_login(self.reviewer)
        url = reverse("company:plan-review", args=[self.plan.pk])
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        token = response.context["form"].initial["review_token"]
        result = self.client.post(url, {"review_token": token, "reviewed": "on"})
        self.assertEqual(result.status_code, 302, getattr(result, "content", b""))
        self.client.force_login(self.staff)
        self.plan.refresh_from_db()

    def claim(self):
        token = company.action_token(self.staff, "ready", self.batch, company.inventory_state(self.batch))
        company.review_installation(self.staff, self.batch.pk, token)
        code, _ = issue_pilot_invitation(batch_id=self.batch.pk, staff=self.staff, recipient=self.owner, mode="customer_owned")
        response = self.api.post(reverse("plantupia-handover-claim"), {"invitation": code, "confirm": True}, format="json")
        self.assertEqual(response.status_code, 200, response.data)

    def action(self, action, revision, **extra):
        return self.api.post(reverse("plantupia-inspection-action", args=[self.plan.pk]),
            {"action": action, "revision": revision, "client_id": str(uuid.uuid4()), **extra}, format="json")

    def request_body(self, **extra):
        return {"plant_id": str(self.plant.pk), "subject": "QA inspection question", "body": "The tester reports a leaf observation.",
                "share_context": True, "share_placement": False, "client_id": str(uuid.uuid4()), **extra}

    def add_cart(self, quantity=1):
        response = self.api.post(reverse("plantupia-cart"), {"product_id": str(self.product.pk), "quantity": quantity}, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        return response.json()

    def checkout_body(self, cart):
        return {"client_id": str(uuid.uuid4()), "cart_fingerprint": cart["fingerprint"], "confirm_no_charge": True}


@override_settings(**SETTINGS)
class CeoJourneyTests(DemoFixture, TestCase):
    def test_optional_catalog_photo_is_normalized_authenticated_and_not_a_storage_url(self):
        photo = BytesIO()
        image = Image.new("RGB", (32, 24), "green")  # Technical fixture, never demo merchandise imagery.
        exif = Image.Exif()
        exif[0x010E] = "Private camera metadata must be removed"
        image.save(photo, format="JPEG", exif=exif)
        with TemporaryDirectory() as media, override_settings(MEDIA_ROOT=media):
            url = reverse("company:product-edit", args=[self.product.pk])
            response = self.client.post(url, {"name": self.product.name, "description": self.product.description,
                "kind": "plant", "proposed_price": "125.50", "currency": "EGP", "qa_stock": 5, "active": "on",
                "image": SimpleUploadedFile("private-camera.jpg", photo.getvalue(), content_type="image/jpeg")})
            self.assertEqual(response.status_code, 302)
            self.product.refresh_from_db()
            image_url = reverse("plantupia-catalog-image", args=[self.product.pk])
            response = self.api.get(image_url)
            self.assertEqual(response.status_code, 200)
            with Image.open(BytesIO(base64.b64decode(response.json()["image_base64"]))) as sanitized:
                self.assertFalse(sanitized.getexif())
            listing = self.api.get(reverse("plantupia-catalog")).json()["results"][0]
            self.assertTrue(listing["has_image"])
            self.assertNotIn("image_url", listing)
            self.api.force_authenticate(None)
            self.assertIn(self.api.get(image_url).status_code, (401, 403))
            self.api.force_authenticate(self.owner)
            self.product.active = False
            self.product.save()
            self.assertEqual(self.api.get(image_url).status_code, 404)
            with override_settings(PLANTUPIA_CEO_DEMO_ENABLED=False):
                self.assertEqual(self.api.get(image_url).status_code, 404)

    def test_retirement_requires_current_review_and_preserves_approval(self):
        self.approve()
        original_actor, original_time = self.plan.approved_by_id, self.plan.approved_at
        url = reverse("company:plan-review", args=[self.plan.pk])
        token = self.client.get(url).context["form"].initial["review_token"]
        response = self.client.post(url, {"review_token": token, "reviewed": "on", "action": "retire"})
        self.assertEqual(response.status_code, 302)
        self.plan.refresh_from_db()
        self.assertFalse(self.plan.active)
        self.assertEqual((self.plan.approved_by_id, self.plan.approved_at), (original_actor, original_time))
        self.claim()
        self.assertEqual(self.action("opt_in", 0).status_code, 409)
        self.assertFalse(PlantEvent.objects.exists())

    def test_plan_review_claim_opt_in_real_observation_and_original_history(self):
        self.approve()
        original_ids = set(self.space.plants.values_list("pk", flat=True))
        self.claim()
        self.assertEqual(set(self.space.plants.values_list("pk", flat=True)), original_ids)
        plans = self.api.get(reverse("plantupia-inspection-plans")).json()["results"]
        self.assertEqual(len(plans), 1)
        self.assertFalse(plans[0]["enabled"])
        self.assertEqual(plans[0]["reviewer"], self.reviewer.username)
        self.assertEqual(self.action("opt_in", 0).status_code, 200)
        result = self.action("complete", 1, note="QA tester personally inspected the leaves; no watering recorded.")
        self.assertEqual(result.status_code, 200, result.data)
        event = PlantEvent.objects.get(pk=result.data["event_id"])
        self.assertEqual(event.plant_id, self.plant.pk)
        self.assertEqual(event.actor, self.owner)
        self.assertEqual(event.kind, "observation")
        history = self.api.get(reverse("plantupia-events", args=[self.plant.pk])).json()
        self.assertIn(str(event.pk), str(history))
        self.assertEqual(self.api.get(reverse("plantupia-care-guide", args=[self.unknown.pk])).json()["state"], "unverified")
        self.assertEqual(self.client.get(reverse("company:plan-review", args=[self.plan.pk])).status_code, 404)
        self.assertEqual(self.client.get(reverse("company:inventory")).status_code, 200)

    def test_plan_creation_is_real_company_form_and_unknown_identity_has_no_schedule(self):
        url = reverse("company:plan-create")
        data = {"plant": self.plant.pk, "guide": self.guide.pk, "action": "soil", "every_days": 7,
                "start_on": str(timezone.localdate()), "timezone": "Africa/Cairo", "instructions": "QA inspection proposal", "placement_context": "QA only"}
        response = self.client.post(url, data)
        self.assertEqual(response.status_code, 302)
        self.assertEqual(CareInspectionPlan.objects.count(), 2)
        self.assertEqual(CareInspectionPlan.objects.filter(approved_at__isnull=False).count(), 0)
        data["plant"] = self.unknown.pk
        response = self.client.post(url, data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CareInspectionPlan.objects.count(), 2)

    def test_operator_cannot_approve_and_approved_content_is_immutable(self):
        url = reverse("company:plan-review", args=[self.plan.pk])
        token = self.client.get(url).context["form"].initial["review_token"]
        self.assertEqual(self.client.post(url, {"review_token": token, "reviewed": "on"}).status_code, 403)
        self.approve()
        self.plan.instructions = "Changed after approval"
        with self.assertRaises(ValidationError):
            self.plan.save()

    def test_stale_source_review_and_plan_timezone_and_bounds(self):
        self.plan.timezone = "Not/AZone"
        with self.assertRaises(ValidationError):
            self.plan.save()
        self.plan.refresh_from_db()
        self.plan.every_days = 0
        with self.assertRaises(ValidationError):
            self.plan.save()
        self.plan.refresh_from_db()
        self.client.force_login(self.reviewer)
        url = reverse("company:plan-review", args=[self.plan.pk])
        token = self.client.get(url).context["form"].initial["review_token"]
        self.plan.placement_context = "Changed QA context"
        self.plan.save()
        self.assertEqual(self.client.post(url, {"review_token": token, "reviewed": "on"}).status_code, 409)

    def test_plan_idempotency_skips_reschedules_and_no_inferred_events(self):
        self.approve(); self.claim()
        url = reverse("plantupia-inspection-action", args=[self.plan.pk])
        body = {"action": "opt_in", "revision": 0, "client_id": str(uuid.uuid4())}
        first = self.api.post(url, body, format="json")
        self.assertEqual(self.api.post(url, body, format="json").json(), first.json())
        self.assertEqual(CarePlanEnrollment.objects.count(), 1)
        body = {"action": "complete", "revision": 1, "note": "QA tester observed actual leaf condition.", "client_id": str(uuid.uuid4())}
        self.assertEqual(self.api.post(url, body, format="json").status_code, 200)
        self.assertEqual(self.api.post(url, body, format="json").status_code, 200)
        self.assertEqual(PlantEvent.objects.count(), 1)
        self.assertEqual(self.action("complete", 1, note="Stale occurrence").status_code, 409)
        self.assertEqual(self.action("skip", 2).status_code, 200)
        tomorrow = str(timezone.localdate() + timedelta(days=1))
        self.assertEqual(self.action("reschedule", 3, next_due_on=tomorrow).status_code, 200)
        self.assertEqual(self.action("decline", 4).status_code, 200)
        self.assertEqual(self.action("complete", 5, note="No opt-in").status_code, 409)
        self.assertEqual(PlantEvent.objects.count(), 1)
        self.assertEqual(CarePlanDecision.objects.count(), 5)

    def test_withdrawn_source_pauses_plan_and_viewer_cannot_enroll(self):
        from .models import SpaceMember
        self.approve(); self.claim()
        SpaceMember.objects.create(space=self.space, user=self.stranger, role="viewer")
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.action("opt_in", 0).status_code, 403)
        self.api.force_authenticate(self.owner)
        self.guide.published = False
        self.guide.save()
        self.assertFalse(self.api.get(reverse("plantupia-inspection-plans")).json()["results"][0]["valid"])
        self.assertEqual(self.action("opt_in", 0).status_code, 409)

    def test_timezone_local_day_and_dst_do_not_create_duplicate_tasks(self):
        self.plan.timezone = "America/New_York"
        self.plan.save(); self.approve(); self.claim()
        with patch("plants.demo_services.timezone.now", return_value=datetime(2026, 11, 1, 5, 30, tzinfo=datetime_timezone.utc)):
            self.assertEqual(self.action("opt_in", 0).status_code, 200)
            initial = demo.plan_json(self.plan, self.owner)
        with patch("plants.demo_services.timezone.now", return_value=datetime(2026, 11, 1, 6, 30, tzinfo=datetime_timezone.utc)):
            later = demo.plan_json(self.plan, self.owner)
        self.assertEqual(initial["next_due_on"], later["next_due_on"])
        self.assertEqual(CarePlanEnrollment.objects.count(), 1)

    def test_ai_entitlement_is_checked_before_provider_and_revocation_returns_paywall(self):
        self.claim()
        url = reverse("plantupia-assistant-ask")
        data = {"message": "Synthetic QA question", "plant_id": str(self.plant.pk), "include_my_plant_context": False}
        with patch.dict(os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "never-network-test"}), patch("plants.assistant.call_provider", return_value="Unit-test provider transport contract") as provider:
            self.assertEqual(self.api.post(url, data, format="json").status_code, 402)
            provider.assert_not_called()
            self.access("ai-care")
            self.assertEqual(self.api.post(url, data, format="json").status_code, 200)
            self.assertIsNone(provider.call_args.args[1])
            DemoEntitlement.objects.filter(user=self.owner).update(revoked_at=timezone.now())
            self.assertEqual(self.api.post(url, data, format="json").status_code, 402)
            self.assertEqual(provider.call_count, 1)
        self.assertFalse(PlantEvent.objects.exists())

    def test_qa_activation_is_explicit_short_lived_idempotent_and_expiry_honest(self):
        url = reverse("plantupia-demo-access")
        data = {"offer": "ai-care", "client_id": str(uuid.uuid4()), "confirm_no_payment": False}
        self.assertEqual(self.api.post(url, data, format="json").status_code, 400)
        data["confirm_no_payment"] = True
        first = self.api.post(url, data, format="json")
        self.assertEqual(first.status_code, 200)
        self.assertFalse(first.json()["charged"])
        self.assertEqual(self.api.post(url, data, format="json").json(), first.json())
        self.assertEqual(DemoEntitlement.objects.count(), 1)
        grant = DemoEntitlement.objects.get()
        self.assertLess(grant.expires_at - grant.granted_at, timedelta(hours=25))
        DemoEntitlement.objects.update(expires_at=timezone.now() - timedelta(seconds=1))
        self.assertFalse(self.api.get(url).json()["entitled"])
        self.api.force_authenticate(self.stranger)
        self.assertFalse(self.api.get(url).json()["entitled"])

    def test_entitled_ai_unconfigured_is_honest_and_does_not_record_history(self):
        self.access("ai-care"); self.claim()
        with patch.dict(os.environ, {"PLANTUPIA_AI_ENABLED": "0", "PLANTUPIA_AI_API_KEY": ""}), patch("plants.assistant.call_provider") as provider:
            response = self.api.post(reverse("plantupia-assistant-ask"), {"message": "QA leaf question", "plant_id": str(self.plant.pk)}, format="json")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["code"], "not_configured")
        provider.assert_not_called()
        self.assertFalse(PlantEvent.objects.exists())

    def test_support_entitlement_consent_real_staff_reply_and_private_scope(self):
        from accounts.models import Message
        from accounts.messaging.messaging_views import conversations_for
        self.claim()
        body = self.request_body()
        url = reverse("plantupia-service-requests")
        self.assertEqual(self.api.post(url, body, format="json").status_code, 402)
        self.access()
        body["share_context"] = False
        self.assertEqual(self.api.post(url, body, format="json").status_code, 400)
        body["share_context"] = True
        first = self.api.post(url, body, format="json")
        self.assertEqual(first.status_code, 200)
        self.assertEqual(self.api.post(url, body, format="json").json(), first.json())
        ticket = ServiceRequest.objects.get()
        self.assertEqual(Message.objects.filter(conversation=ticket.conversation).count(), 1)
        self.assertFalse(conversations_for(self.owner).filter(pk=ticket.conversation_id).exists())
        self.assertFalse(conversations_for(self.staff).filter(pk=ticket.conversation_id).exists())
        PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="observation", note="PRIVATE QA HISTORY NOT SHARED", observed_at=timezone.now())
        staff_url = reverse("company:support-detail", args=[ticket.pk])
        response = self.client.get(staff_url)
        self.assertContains(response, body["body"])
        self.assertNotContains(response, "PRIVATE QA HISTORY NOT SHARED")
        self.assertNotContains(response, self.plant.placement_note)
        self.assertNotContains(response, self.space.name)
        self.assertEqual(self.client.post(staff_url, {"body": "Unassigned", "client_id": uuid.uuid4()}).status_code, 403)
        self.assertEqual(self.client.post(staff_url, {"action": "claim"}).status_code, 302)
        reply = "A real QA employee typed this reply during the test."
        response = self.client.post(staff_url, {"body": reply, "client_id": uuid.uuid4()})
        self.assertEqual(response.status_code, 302)
        detail = reverse("plantupia-service-request", args=[ticket.pk])
        messages = self.api.get(detail).json()["messages"]
        self.assertEqual(messages[-1]["body"], reply)
        self.assertEqual(messages[-1]["sender"], self.staff.username)
        self.assertTrue(messages[-1]["from_staff"])
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(detail).status_code, 404)

    def test_expired_support_is_read_only_for_customer_and_no_new_request(self):
        self.access(); self.claim()
        response = self.api.post(reverse("plantupia-service-requests"), self.request_body(), format="json")
        ticket_id = response.json()["id"]
        DemoEntitlement.objects.filter(user=self.owner).update(expires_at=timezone.now() - timedelta(seconds=1))
        detail = reverse("plantupia-service-request", args=[ticket_id])
        self.assertEqual(self.api.get(detail).status_code, 200)
        self.assertEqual(self.api.post(detail, {"body": "QA follow-up", "client_id": str(uuid.uuid4())}, format="json").status_code, 402)
        self.assertEqual(self.api.post(reverse("plantupia-service-requests"), self.request_body(), format="json").status_code, 402)

    def test_support_quota_does_not_reset_by_reactivating_qa_access(self):
        self.access(); self.claim()
        for _ in range(3):
            self.assertEqual(self.api.post(reverse("plantupia-service-requests"), self.request_body(), format="json").status_code, 200)
        self.access()
        self.assertEqual(self.api.post(reverse("plantupia-service-requests"), self.request_body(), format="json").status_code, 429)

    def test_qa_catalog_cart_checkout_receipt_account_scope_and_idempotency(self):
        quote = self.add_cart(2)
        self.assertEqual(quote["total_provisional"], "251.00")
        body = self.checkout_body(quote)
        url = reverse("plantupia-demo-orders")
        first = self.api.post(url, body, format="json")
        self.assertEqual(first.status_code, 200, first.data)
        self.assertEqual(self.api.post(url, body, format="json").json(), first.json())
        self.assertEqual(DemoOrder.objects.count(), 1)
        self.assertEqual(DemoOrderLine.objects.get().quantity, 2)
        self.product.refresh_from_db()
        self.assertEqual(self.product.qa_stock, 3)
        self.assertFalse(first.json()["charged"])
        self.assertFalse(first.json()["delivery_initiated"])
        self.assertEqual(self.api.get(url).json()["results"][0]["id"], first.json()["id"])
        self.assertFalse(DemoCartLine.objects.exists())
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(url).json()["results"], [])
        self.assertEqual(self.api.get(reverse("plantupia-cart")).json()["results"], [])

    def test_cart_negative_quantity_stale_price_stock_and_partial_failure_refused(self):
        url = reverse("plantupia-cart")
        self.assertEqual(self.api.post(url, {"product_id": str(self.product.pk), "quantity": -1}, format="json").status_code, 400)
        quote = self.add_cart()
        self.product.proposed_price = Decimal("126.50")
        self.product.save()
        self.assertEqual(self.api.post(reverse("plantupia-demo-orders"), self.checkout_body(quote), format="json").status_code, 409)
        quote = self.api.get(url).json()
        self.product.qa_stock = 0
        self.product.save()
        self.assertEqual(self.api.post(reverse("plantupia-demo-orders"), self.checkout_body(quote), format="json").status_code, 409)
        self.assertFalse(DemoOrder.objects.exists())
        self.assertTrue(DemoCartLine.objects.exists())

    def test_staff_order_preparation_is_separate_fresh_draft_and_no_auto_handover(self):
        quote = self.add_cart(2)
        self.api.post(reverse("plantupia-demo-orders"), self.checkout_body(quote), format="json")
        order = DemoOrder.objects.get()
        url = reverse("company:order-detail", args=[order.pk])
        token = self.client.get(url).context["form"].initial["review_token"]
        result = self.client.post(url, {"review_token": token, "reviewed": "on"})
        self.assertEqual(result.status_code, 302)
        order.refresh_from_db()
        batch = order.installation
        self.assertEqual(batch.status, "draft")
        self.assertEqual(batch.space.plants.count(), 2)
        self.assertFalse(batch.space.plants.filter(pk=self.plant.pk).exists())
        self.assertFalse(batch.space.plants.filter(verified_species__isnull=False).exists())
        self.assertEqual(demo.convert_order(self.staff, order.pk).pk, batch.pk)
        self.assertFalse(HandoverInvitation.objects.exists())

    def test_company_csrf_customer_role_and_feature_production_gates(self):
        client = Client(enforce_csrf_checks=True)
        client.force_login(self.staff)
        for name in ("plan-create", "product-create", "demo-access"):
            self.assertEqual(client.post(reverse("company:" + name), {}).status_code, 403)
        self.client.force_login(self.owner)
        for name in ("plans", "support", "catalog", "orders", "demo-access"):
            self.assertEqual(self.client.get(reverse("company:" + name)).status_code, 403)
        with override_settings(PLANTUPIA_CEO_DEMO_ENABLED=False):
            self.assertEqual(self.api.get(reverse("plantupia-catalog")).status_code, 404)
            self.assertEqual(self.api.post(reverse("plantupia-demo-access"), {}, format="json").status_code, 404)
        with override_settings(IS_PRODUCTION=True), patch("plants.company_services.pilot_enabled", return_value=False):
            self.assertEqual(self.api.get(reverse("plantupia-demo-access")).status_code, 404)


@override_settings(**SETTINGS)
class CeoConcurrencyTests(DemoFixture, TransactionTestCase):
    @skipUnlessDBFeature("has_select_for_update")
    def test_two_customers_competing_for_last_unit_create_only_one_order(self):
        self.product.qa_stock = 1
        self.product.save()
        first = self.add_cart()
        self.api.force_authenticate(self.stranger)
        second = self.add_cart()
        barrier = Barrier(2)
        def run(pair):
            close_old_connections()
            try:
                user_id, quote = pair
                user = get_user_model().objects.get(pk=user_id)
                barrier.wait(timeout=10)
                try:
                    demo.checkout(user, self.checkout_body(quote))
                    return 200
                except demo.DemoProblem as exc:
                    return exc.status_code
            finally:
                close_old_connections()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(run, [(self.owner.pk, first), (self.stranger.pk, second)]))
        self.assertEqual(sorted(results), [200, 409])
        self.assertEqual(DemoOrder.objects.count(), 1)
        self.assertEqual(DemoOrderLine.objects.get().quantity, 1)
        self.product.refresh_from_db()
        self.assertEqual(self.product.qa_stock, 0)
        self.assertEqual(DemoCartLine.objects.count(), 1)

    @skipUnlessDBFeature("has_select_for_update")
    def test_simultaneous_checkout_replay_commits_one_order_and_stock_decrement(self):
        quote = self.add_cart()
        body = self.checkout_body(quote)
        barrier = Barrier(2)
        def run(_):
            close_old_connections()
            try:
                user = get_user_model().objects.get(pk=self.owner.pk)
                barrier.wait(timeout=10)
                return demo.checkout(user, body)["id"]
            finally:
                close_old_connections()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(run, range(2)))
        self.assertEqual(results[0], results[1])
        self.assertEqual(DemoOrder.objects.count(), 1)
        self.product.refresh_from_db()
        self.assertEqual(self.product.qa_stock, 4)

    @skipUnlessDBFeature("has_select_for_update")
    def test_simultaneous_inspection_confirmation_is_one_observation(self):
        self.approve(); self.claim()
        self.assertEqual(self.action("opt_in", 0).status_code, 200)
        data = {"action": "complete", "revision": 1, "note": "Synthetic tester actually checked leaves", "client_id": str(uuid.uuid4())}
        barrier = Barrier(2)
        def run(_):
            close_old_connections()
            try:
                user = get_user_model().objects.get(pk=self.owner.pk)
                barrier.wait(timeout=10)
                return demo.plan_action(user, self.plan.pk, data)["event_id"]
            finally:
                close_old_connections()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(run, range(2)))
        self.assertEqual(results[0], results[1])
        self.assertEqual(PlantEvent.objects.count(), 1)
