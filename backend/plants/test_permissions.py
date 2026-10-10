from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space, SpaceMember


class PlantupiaPermissionTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.owner = User.objects.create_user(email="client1@example.test", username="client1", password="test-pass-only")
        self.stranger = User.objects.create_user(email="client2@example.test", username="client2", password="test-pass-only")
        self.space = Space.objects.create(owner=self.owner, name="Client office")
        self.plant = PlantUnit.objects.create(space=self.space, label="Reception planting")
        self.client = APIClient()

    def test_login_required(self):
        response = self.client.get(reverse("plantupia-plants"))
        self.assertEqual(response.status_code, 401)

    def test_other_account_cannot_read_plant_history(self):
        self.client.force_authenticate(user=self.stranger)
        response = self.client.get(reverse("plantupia-events", args=[self.plant.pk]))
        self.assertEqual(response.status_code, 404)

    def test_owner_can_name_and_record_care(self):
        self.client.force_authenticate(user=self.owner)
        response = self.client.put(
            reverse("plantupia-nickname", args=[self.plant.pk]),
            {"nickname": "Loza"}, format="json",
        )
        self.assertEqual(response.status_code, 200)
        response = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            {
                "kind": "care",
                "evidence_type": "observed",
                "note": "Checked light and soil",
                "observed_at": timezone.now().isoformat(),
            },
            format="json",
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(PlantEvent.objects.filter(plant=self.plant).count(), 1)

    def test_viewer_cannot_write_to_shared_space(self):
        SpaceMember.objects.create(space=self.space, user=self.stranger, role=SpaceMember.Role.VIEWER)
        self.client.force_authenticate(user=self.stranger)
        response = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"kind": "observation", "observed_at": timezone.now().isoformat()},
            format="json",
        )
        self.assertEqual(response.status_code, 403)

    def test_list_returns_object_envelope(self):
        self.client.force_authenticate(user=self.owner)
        response = self.client.get(reverse("plantupia-plants"))
        self.assertEqual(response.status_code, 200)
        self.assertIn("results", response.json())
        self.assertEqual(len(response.json()["results"]), 1)
