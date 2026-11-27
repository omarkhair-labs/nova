from tempfile import TemporaryDirectory
import uuid

from django.contrib.auth.models import Permission
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import EvidenceReview, PlantEvent, PlantUnit, Space, SpaceMember
from .test_ceo_demo import DemoFixture, SETTINGS
from .test_field_evidence import png


@override_settings(**{**SETTINGS, "PLANTUPIA_LIVING_SYSTEMS_ENABLED": True})
class CompanyFieldEvidenceTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.reviewer.user_permissions.add(Permission.objects.get(codename="review_visual_evidence"))
        self.event = PlantEvent.objects.create(plant=self.plant, actor=self.staff,
            kind="issue", observed_at=timezone.now(), note="QA eligible note")
        private = Space.objects.create(owner=self.owner, name="QA separate private space")
        plant = PlantUnit.objects.create(space=private, label="QA inaccessible specimen")
        self.hidden = PlantEvent.objects.create(plant=plant, actor=self.owner,
            kind="issue", observed_at=timezone.now(), note="QA hidden note")
        self.list_url = reverse("company:field-evidence")
        self.detail = reverse("company:field-evidence-detail", args=[self.event.pk])
        self.hidden_url = reverse("company:field-evidence-detail", args=[self.hidden.pk])

    def authorize(self, role="carer"):
        SpaceMember.objects.create(space=self.space, user=self.reviewer, role=role)
        self.client.force_login(self.reviewer)

    def test_dashboard_permission_never_grants_customer_access(self):
        self.assertEqual(self.client.get(self.list_url).status_code, 403)
        self.authorize("viewer")
        page = self.client.get(self.list_url)
        self.assertEqual(page.status_code, 200)
        self.assertNotContains(page, self.event.note)
        self.assertEqual(self.client.get(self.detail).status_code, 404)
        self.assertEqual(self.client.get(self.hidden_url).status_code, 404)

    def test_feature_gate_scope_and_cross_surface_review_replay(self):
        self.authorize()
        queue = self.client.get(self.list_url)
        self.assertContains(queue, self.event.note)
        self.assertNotContains(queue, self.hidden.note)
        self.assertIn("no-store", queue["Cache-Control"])
        self.assertEqual(self.client.get(self.hidden_url).status_code, 404)
        page = self.client.get(self.detail)
        payload = {"submission_id": page.context["submission_id"],
            "finding": "insufficient_evidence", "rationale": "QA test: more context is needed."}
        self.assertRedirects(self.client.post(self.detail, payload), self.detail)
        self.assertRedirects(self.client.post(self.detail, payload), self.detail)
        self.assertEqual(EvidenceReview.objects.filter(event=self.event).count(), 1)
        self.assertEqual(self.client.post(self.detail, {**payload, "rationale": "Different"}).status_code, 409)
        self.assertNotContains(self.client.get(self.list_url), self.event.note)
        self.assertContains(self.client.get(self.list_url+"?status=reviewed"), self.event.note)
        native = APIClient()
        native.force_authenticate(self.reviewer)
        url = reverse("plantupia-field-evidence", args=[self.plant.pk, self.event.pk])
        self.assertEqual(native.get(url).data["reviews"][0]["rationale"], payload["rationale"])
        self.assertEqual(PlantEvent.objects.filter(plant=self.plant).count(), 1)
        with self.settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=False):
            self.assertEqual(self.client.get(self.list_url).status_code, 404)
            self.assertNotContains(self.client.get(reverse("company:care-home")), self.list_url)

    def test_private_photo_preview_and_revoked_or_archived_access(self):
        self.authorize()
        with TemporaryDirectory() as media, self.settings(MEDIA_ROOT=media):
            self.event.photo.save("qa.png", SimpleUploadedFile("qa.png", png("RGB", (32,32), "green"),
                content_type="image/png"))
            photo_url = reverse("company:field-evidence-photo", args=[self.event.pk])
            photo = self.client.get(photo_url)
            self.assertEqual(photo.status_code, 200)
            self.assertEqual(photo["Content-Type"], "image/jpeg")
            self.assertIn("no-store", photo["Cache-Control"])
            self.assertNotContains(self.client.get(self.detail), self.event.photo.name)
            self.plant.is_active = False
            self.plant.save(update_fields=["is_active"])
            self.assertEqual(self.client.get(photo_url).status_code, 404)
            self.assertEqual(self.client.post(self.detail, {"submission_id": str(uuid.uuid4()),
                "finding": "supported_observation", "rationale": "QA"}).status_code, 404)
            self.assertEqual(EvidenceReview.objects.count(), 0)
