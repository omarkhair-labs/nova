"""Staff-side QA workflow visibility, not a commercial delivery portal."""
import os
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from .models import InstallationBatch, InstallationManifestRow, PlantUnit, Space


@override_settings(STORAGES={
    **settings.STORAGES,
    "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
})
class StaffDeliveryWorkflowUiTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.staff = User.objects.create_superuser(
            username="qa_delivery_admin", email="qa_delivery_admin@example.test", password="test-only",
        )
        self.customer = User.objects.create_user(
            username="qa_delivery_customer", email="qa_delivery_customer@example.test", password="test-only",
        )
        self.space = Space.objects.create(owner=self.staff, name="QA test office")
        self.batch = InstallationBatch.objects.create(
            reference_code="QA-STAFF-UI-01", space=self.space, prepared_by=self.staff,
        )
        for n in (1, 2):
            plant = PlantUnit.objects.create(space=self.space, label=f"QA desk plant {n}")
            InstallationManifestRow.objects.create(
                batch=self.batch, plant=plant, source_key=f"qa-plant-{n}",
            )
        self.client.force_login(self.staff)

    def test_installation_list_explains_draft_review_without_implying_activation(self):
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"}):
            response = self.client.get(reverse("admin:plants_installationbatch_changelist"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "QA-STAFF-UI-01")
        self.assertContains(response, "Review the complete inventory")
        self.assertContains(response, "QA invitation issuing")
        self.assertContains(response, "number of manifest entries")

    def test_issue_page_lists_real_reviewed_inventory_and_prefills_selected_batch(self):
        self.batch.status = InstallationBatch.Status.READY
        self.batch.save(update_fields=("status",))
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"}):
            response = self.client.get(
                reverse("admin:plants_handoverinvitation_issue"),
                {"batch": str(self.batch.pk)},
            )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "QA-STAFF-UI-01")
        self.assertContains(response, "QA test office")
        self.assertContains(response, "Manifest plants")
        self.assertEqual(str(response.context["form"]["batch"].value()), str(self.batch.pk))
        self.assertIsNone(response.context["raw_code"])

    def test_unreviewed_inventory_is_not_displayed_as_ready(self):
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"}):
            response = self.client.get(reverse("admin:plants_handoverinvitation_issue"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "No installations are reviewed and ready")
        self.assertNotContains(response, "QA-STAFF-UI-01")
        self.assertIsNone(response.context["raw_code"])

    def test_disabled_feature_does_not_advertise_issuance_link(self):
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "0"}):
            for name in ("admin:plants_installationbatch_changelist",
                         "admin:plants_handoverinvitation_changelist"):
                response = self.client.get(reverse(name))
                self.assertEqual(response.status_code, 200)
                self.assertNotContains(response, "Issue isolated QA invitation")
                self.assertNotContains(response, "Continue to QA invitation issuing")
