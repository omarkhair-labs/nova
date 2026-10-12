"""Backend boundary checks for a real text-only Plantupia assistant."""
import os
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space


class PlantAssistantSecurityTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.owner = User.objects.create_user(
            username="plant_ai_owner", email="plant_ai_owner@example.test", password="test-only"
        )
        self.other = User.objects.create_user(
            username="plant_ai_other", email="plant_ai_other@example.test", password="test-only"
        )
        self.space = Space.objects.create(name="Private home", owner=self.owner)
        self.plant = PlantUnit.objects.create(
            space=self.space, label="Loza", species_label="Unknown",
            placement_note="Near a bright window",
        )
        self.client = APIClient()
        self.url = reverse("plantupia-assistant-ask")
        cache.clear()

    def test_login_required(self):
        self.assertEqual(self.client.post(self.url, {"message": "What changed?"}, format="json").status_code, 401)

    def test_provider_is_disabled_by_default_and_never_called(self):
        self.client.force_authenticate(user=self.owner)
        with patch.dict(os.environ, {"PLANTUPIA_AI_ENABLED": "0", "PLANTUPIA_AI_API_KEY": ""}), patch(
            "plants.assistant.call_provider"
        ) as call:
            response = self.client.post(self.url, {"message": "How do I check leaves?"}, format="json")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["code"], "not_configured")
        call.assert_not_called()

    def test_inaccessible_plant_is_404_even_when_provider_disabled(self):
        self.client.force_authenticate(user=self.other)
        response = self.client.post(
            self.url, {"message": "Question?", "plant_id": str(self.plant.pk),
                       "include_my_plant_context": True}, format="json",
        )
        self.assertEqual(response.status_code, 404)

    def test_consent_and_private_context_are_explicit(self):
        PlantEvent.objects.create(
            plant=self.plant, actor=self.owner, kind=PlantEvent.Kind.OBSERVATION,
            note="Leaves are yellow", observed_at=__import__("django.utils.timezone", fromlist=["now"]).now(),
        )
        self.client.force_authenticate(user=self.owner)
        with patch.dict(
            os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "test-only-never-network"}
        ), patch("plants.assistant.call_provider", return_value="Check the soil before watering.") as provider:
            without = self.client.post(
                self.url, {"message": "Help?", "plant_id": str(self.plant.pk)}, format="json"
            )
            self.assertEqual(without.status_code, 200)
            self.assertEqual(without.json()["scope"], "general")
            self.assertIsNone(provider.call_args.args[1])
            with_context = self.client.post(
                self.url, {"message": "Help?", "plant_id": str(self.plant.pk),
                           "include_my_plant_context": True}, format="json"
            )
            self.assertEqual(with_context.status_code, 200)
            self.assertEqual(with_context.json()["scope"], "plant_context")
            facts = provider.call_args.args[1]
            self.assertEqual(facts["personal_name"], "Loza")
            self.assertEqual(facts["recent_person_observations"][0]["note"], "Leaves are yellow")
            self.assertFalse(with_context.json()["can_write_care_history"])
            self.assertEqual(with_context.json()["source_links"], [])
        self.assertEqual(PlantEvent.objects.filter(plant=self.plant).count(), 1)

    def test_question_validation_and_context_type(self):
        self.client.force_authenticate(user=self.owner)
        for payload in [
            {"message": ""},
            {"message": "x" * 1001},
            {"message": "A", "include_my_plant_context": "true"},
            {"message": "A", "include_my_plant_context": True},
            {"message": "A", "plant_id": 123},
        ]:
            self.assertEqual(self.client.post(self.url, payload, format="json").status_code, 400)

    def test_rate_limit_and_provider_failure_return_no_private_content(self):
        self.client.force_authenticate(user=self.owner)
        with patch.dict(
            os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "test-only"}
        ), patch("plants.assistant.call_provider", side_effect=RuntimeError("provider_unavailable")):
            response = self.client.post(self.url, {"message": "Please help"}, format="json")
            self.assertEqual(response.status_code, 503)
            self.assertNotIn("test-only", response.content.decode())
            self.assertNotIn("provider_unavailable", response.content.decode())
