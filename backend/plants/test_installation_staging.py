"""G1 review contract: prepared inventory is real, private and not QR-claimable."""
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.db import IntegrityError, transaction
from django.test import TestCase, override_settings
from django.conf import settings
from django.urls import reverse
from rest_framework.test import APIClient

from .models import InstallationBatch, InstallationManifestRow, PlantUnit, Space


@override_settings(STORAGES={**settings.STORAGES, "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}})
class InstallationStagingTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.staff = User.objects.create_superuser(
            username="installation_reviewer", email="reviewer@example.test",
            password="test-only",
        )
        self.customer = User.objects.create_user(
            username="installation_customer", email="customer@example.test",
            password="test-only",
        )
        self.other = User.objects.create_user(
            username="installation_stranger", email="stranger@example.test",
            password="test-only",
        )
        self.space = Space.objects.create(owner=self.staff, name="Internal pilot site")
        self.batch = InstallationBatch.objects.create(
            reference_code="QA-PILOT-001", space=self.space, prepared_by=self.staff,
        )

    def test_staff_owned_draft_is_not_a_client_installation_or_public_inventory(self):
        self.assertEqual(self.batch.status, InstallationBatch.Status.DRAFT)
        PlantUnit.objects.create(space=self.space, label="Real test unit")
        self.assertEqual(self.space.owner_id, self.staff.pk)
        api = APIClient()
        for user in (self.customer, self.other):
            api.force_authenticate(user)
            self.assertEqual(api.get(reverse("plantupia-spaces")).json()["results"], [])
            self.assertEqual(api.get(reverse("plantupia-plants")).json()["results"], [])
        self.assertFalse(any("handover" in x.name for x in self.space._meta.fields))

    def test_nonstaff_owner_and_preparer_are_rejected_before_approval(self):
        private = Space.objects.create(owner=self.customer, name="Actual customer's private space")
        bad_space = InstallationBatch(
            reference_code="BAD-OWNER", space=private, prepared_by=self.staff,
        )
        with self.assertRaises(ValidationError):
            bad_space.full_clean()
        bad_preparer = InstallationBatch(
            reference_code="BAD-PREPARER", space=self.space, prepared_by=self.customer,
        )
        with self.assertRaises(ValidationError):
            bad_preparer.full_clean()

    def test_manifest_can_only_reference_a_plant_from_same_installation(self):
        plant = PlantUnit.objects.create(space=self.space, label="Test A")
        other_space = Space.objects.create(owner=self.staff, name="Different staff space")
        other_plant = PlantUnit.objects.create(space=other_space, label="Test B")
        valid = InstallationManifestRow(
            batch=self.batch, plant=plant, source_key="plant-001",
        )
        valid.full_clean()
        valid.save()
        with self.assertRaises(ValidationError):
            InstallationManifestRow(
                batch=self.batch, plant=other_plant, source_key="plant-002",
            ).full_clean()

    def test_repeated_source_key_and_plant_identity_are_not_duplicate_manifest_records(self):
        p1 = PlantUnit.objects.create(space=self.space, label="First")
        p2 = PlantUnit.objects.create(space=self.space, label="Second")
        InstallationManifestRow.objects.create(batch=self.batch, plant=p1, source_key="001")
        with self.assertRaises(ValidationError):
            InstallationManifestRow(batch=self.batch, plant=p2, source_key="001").full_clean()
        with self.assertRaises(ValidationError):
            InstallationManifestRow(batch=self.batch, plant=p1, source_key="002").full_clean()
        with self.assertRaises(IntegrityError):
            with transaction.atomic():
                InstallationManifestRow.objects.create(
                    batch=self.batch, plant=p2, source_key="001",
                )
        self.assertEqual(InstallationManifestRow.objects.filter(batch=self.batch).count(), 1)

    def test_admin_cannot_mark_ready_without_real_inventory(self):
        self.client.force_login(self.staff)
        url = reverse("admin:plants_installationbatch_changelist")
        response = self.client.post(
            url,
            {"action": "mark_inventory_ready", "_selected_action": [str(self.batch.pk)]},
            follow=True,
        )
        self.assertEqual(response.status_code, 200)
        self.batch.refresh_from_db()
        self.assertEqual(self.batch.status, InstallationBatch.Status.DRAFT)
        self.assertIsNone(self.batch.reviewed_at)
        self.assertIsNone(self.batch.reviewed_by)

    def test_admin_review_only_after_stable_manifest_does_not_claim_customer_space(self):
        plants = [
            PlantUnit.objects.create(space=self.space, label=f"QA plant {number}")
            for number in range(1, 8)
        ]
        for index, plant in enumerate(plants, 1):
            InstallationManifestRow.objects.create(
                batch=self.batch, plant=plant, source_key=f"manifest-{index:03}",
            )
        self.client.force_login(self.staff)
        url = reverse("admin:plants_installationbatch_changelist")
        response = self.client.post(
            url,
            {"action": "mark_inventory_ready", "_selected_action": [str(self.batch.pk)]},
            follow=True,
        )
        self.assertEqual(response.status_code, 200)
        self.batch.refresh_from_db()
        self.assertEqual(self.batch.status, InstallationBatch.Status.READY)
        self.assertEqual(self.batch.reviewed_by_id, self.staff.id)
        self.assertIsNotNone(self.batch.reviewed_at)
        self.assertEqual(
            set(self.batch.manifest_rows.values_list("plant_id", flat=True)),
            {p.pk for p in plants},
        )
        self.assertEqual(self.space.owner_id, self.staff.id)
        api = APIClient()
        api.force_authenticate(self.customer)
        self.assertEqual(api.get(reverse("plantupia-plants")).json()["results"], [])
        with self.assertRaises(ValidationError):
            InstallationManifestRow(
                batch=self.batch, plant=PlantUnit.objects.create(
                    space=self.space, label="Unexpected",
                ), source_key="late",
            ).full_clean()
        second_attempt = self.client.post(
            url,
            {"action": "mark_inventory_ready", "_selected_action": [str(self.batch.pk)]},
            follow=True,
        )
        self.assertEqual(second_attempt.status_code, 200)
        self.batch.refresh_from_db()
        self.assertEqual(self.batch.manifest_rows.count(), 7)
