"""Privacy and provider boundaries for Plantupia AI; no live API key required."""
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space
from django.utils import timezone


class PlantCareAssistantTests(TestCase):
    def setUp(self):
        user = get_user_model()
        self.owner = user.objects.create_user(
            email="plant-owner@example.test", username="plant-owner", password="test-only"
        )
        self.other = user.objects.create_user(
            email="plant-stranger@example.test", username="plant-stranger", password="test-only"
        )
        self.plant = PlantUnit.objects.create(
            space=Space.objects.create(owner=self.owner, name="My space"),
            label="Lilly", species_label="Pothos", placement_note="near window",
        )
        PlantEvent.objects.create(
            plant=self.plant, actor=self.owner, kind="observation",
            note="Yellow leaves after watering", observed_at=timezone.now()
        )
        self.url = reverse("plantupia-ai-ask")
        self.client = APIClient()

    def test_auth_required(self):
        resp = self.client.post(self.url, {"message": "مرحبا"}, format="json")
        self.assertEqual(resp.status_code, 401)

    @patch.dict("os.environ", {"PLANTUPIA_AI_API_KEY": "mock-only"})
    @patch("plants.assistant._provider_answer", return_value="افحص التربة والصرف.")
    def test_explicit_context_and_no_automatic_writes(self, provider):
        self.client.force_authenticate(user=self.owner)
        resp = self.client.post(self.url, {
            "message": "ورقها اصفر", "include_plant_context": True,
            "plant_id": str(self.plant.pk)
        }, format="json")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["scope"], "plant_context")
        self.assertFalse(resp.data["saved_action"])
        self.assertEqual(provider.call_args.args[1]["recent_user_reported_events"][0]["kind"], "observation")
        self.assertEqual(PlantEvent.objects.filter(plant=self.plant).count(), 1)

    @patch.dict("os.environ", {"PLANTUPIA_AI_API_KEY": "mock-only"})
    @patch("plants.assistant._provider_answer", return_value="نصيحة عامة")
    def test_no_context_when_not_authorized_by_consent(self, provider):
        self.client.force_authenticate(user=self.owner)
        response = self.client.post(self.url, {
            "message": "ازاي اهتم بالنبات؟", "include_plant_context": False,
            "plant_id": str(self.plant.pk),
        }, format="json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["scope"], "general")
        self.assertIsNone(provider.call_args.args[1])

    @patch.dict("os.environ", {"PLANTUPIA_AI_API_KEY": "mock-only"})
    @patch("plants.assistant._provider_answer")
    def test_foreign_plant_cannot_be_sent_to_provider(self, provider):
        self.client.force_authenticate(user=self.other)
        response = self.client.post(self.url, {
            "message": "حل", "include_plant_context": True,
            "plant_id": str(self.plant.pk),
        }, format="json")
        self.assertEqual(response.status_code, 404)
        provider.assert_not_called()

    @patch.dict("os.environ", {"PLANTUPIA_AI_API_KEY": ""})
    def test_disabled_without_server_secret(self):
        self.client.force_authenticate(user=self.owner)
        response = self.client.post(self.url, {
            "message": "اهلا", "include_plant_context": False,
        }, format="json")
        self.assertEqual(response.status_code, 503)

    def test_bad_message_and_nonboolean_consent_rejected(self):
        self.client.force_authenticate(user=self.owner)
        self.assertEqual(self.client.post(self.url, {"message": ""}, format="json").status_code, 400)
        self.assertEqual(self.client.post(self.url, {
            "message": "hi", "include_plant_context": "true",
        }, format="json").status_code, 400)
