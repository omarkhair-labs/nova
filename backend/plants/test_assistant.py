"""Backend boundary checks for a real text-only Plantupia assistant."""
import os
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from datetime import timedelta
from django.urls import reverse
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space, DemoOffer, DemoEntitlement


@override_settings(PLANTUPIA_CEO_DEMO_ENABLED=True, PLANTUPIA_COMPANY_DASHBOARD_ENABLED=True,
                   IS_PRODUCTION=False, ENVIRONMENT="development", REDIS_URL="redis://qa-unit-test-only")
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
        offer = DemoOffer.objects.create(key="qa-ai-test", name="QA AI transport test", proposed_monthly_price="79.00", ai_daily_limit=35)
        DemoEntitlement.objects.create(user=self.owner, offer=offer, expires_at=timezone.now() + timedelta(hours=1))
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

    def test_selected_history_day_limits_context_and_still_needs_consent(self):
        now = timezone.now()
        for days, note in [(0, "Today"), (3, "Earlier visit")]:
            PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care",
                                      note=note, observed_at=now - timedelta(days=days))
        self.client.force_authenticate(user=self.owner)
        day = timezone.localdate(now - timedelta(days=3)).isoformat()
        data = {"message": "What happened?", "plant_id": str(self.plant.pk), "observed_on": day}
        with patch.dict(os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "test-only"}), patch(
            "plants.assistant.call_provider", return_value="Check the record."
        ) as provider:
            self.assertEqual(self.client.post(self.url, data, format="json").status_code, 200)
            self.assertIsNone(provider.call_args.args[1])
            history_url = reverse("plantupia-assistant-history")
            self.assertEqual(self.client.post(history_url, data, format="json").status_code, 400)
            data["include_my_plant_context"] = True
            self.assertEqual(self.client.post(history_url, data, format="json").status_code, 200)
            context = provider.call_args.args[1]
            self.assertEqual(context["selected_history_date"], day)
            self.assertEqual([e["note"] for e in context["recent_person_observations"]], ["Earlier visit"])

    @override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
    def test_measured_areas_need_new_consent_and_belong_to_selected_events(self):
        from uuid import uuid4
        from .models import BotanicalSpecies, TracedRegion
        self.plant.selected_species = BotanicalSpecies.objects.create(key="rubber", scientific_name="Ficus elastica")
        self.plant.save(update_fields=["selected_species"])
        event = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care", observed_at=timezone.now())
        for status, area in [("accepted", 2.5), ("rejected", 999)]:
            TracedRegion.objects.create(event=event, author=self.owner, submission_id=uuid4(),
                source_sha256="a"*64, annotation={"part": "leaf"}, measurements={"geometry": {
                    "projected_area_cm2": area, "calibration": {"method": "face-on-two-segment-v1", "status": status}}})
        self.client.force_authenticate(user=self.owner)
        data = {"message": "What was measured?", "plant_id": str(self.plant.pk), "include_my_plant_context": True}
        with patch.dict(os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "test-only"}), patch(
            "plants.assistant.call_provider", return_value="Look at the measurement."
        ) as provider:
            self.assertEqual(self.client.post(self.url, data, format="json").status_code, 200)
            self.assertNotIn("accepted_projected_areas", provider.call_args.args[1])
            self.assertEqual(self.client.post(self.url, {**data, "include_recorded_evidence": True}, format="json").status_code, 200)
            context = provider.call_args.args[1]
            self.assertEqual(context["current_botanical_identity"]["basis"], "user_selected")
            self.assertEqual([a["projected_area_cm2"] for a in context["accepted_projected_areas"]], [2.5])
            self.assertNotIn("source_sha256", str(context))

    @override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
    def test_historical_layout_is_shared_only_with_consent_and_matches_field_notes(self):
        from .models import RoomRevision
        from .living_space import build_room
        now = timezone.now()
        def layout(revision, days, plants, windows):
            value = RoomRevision.objects.create(space=self.space,actor=self.owner,revision=revision,
                geometry=build_room(str(self.space.pk),4.,3.,plants,windows))
            RoomRevision.objects.filter(pk=value.pk).update(created_at=now-timedelta(days=days))
        position = {"plant_id":str(self.plant.pk),"x_m":2.,"y_m":1.}
        layout(1,5,[position],[{"id":"north","x_m":2.,"y_m":0.}])
        layout(2,3,[{**position,"x_m":3.}],[{"id":"east","x_m":4.,"y_m":1.}])
        layout(3,1,[],[])
        old = PlantEvent.objects.create(plant=self.plant,actor=self.owner,kind="observation",observed_at=now-timedelta(days=4))
        removed = PlantEvent.objects.create(plant=self.plant,actor=self.owner,kind="observation",observed_at=now)
        self.client.force_authenticate(self.owner)
        field = self.client.post(reverse("plantupia-field-evidence",args=[self.plant.pk,old.pk]),
                                 {"symptom":"uncertain"},format="json").data["placement_at_observation"]
        self.assertEqual((field["revision"],field["nearest_window_distance_m"]),(1,1.0))
        self.assertIsNone(self.client.post(reverse("plantupia-field-evidence",args=[self.plant.pk,removed.pk]),
                             {"symptom":"uncertain"},format="json").data["placement_at_observation"])
        body = {"message":"What was the recorded placement?", "plant_id":str(self.plant.pk),
                "observed_on":timezone.localdate(old.observed_at).isoformat(),
                "include_my_plant_context":True,"include_recorded_evidence":True}
        history = reverse("plantupia-assistant-history")
        with patch.dict(os.environ,{"PLANTUPIA_AI_ENABLED":"1","PLANTUPIA_AI_API_KEY":"test-only"}), patch(
            "plants.assistant.call_provider",return_value="Check the recorded layout."
        ) as provider:
            self.assertEqual(self.client.post(history,body,format="json").status_code,200)
            self.assertNotIn("reported_placement_at_visit",provider.call_args.args[1])
            self.assertEqual(self.client.post(history,{**body,"include_guide_and_layout":True},format="json").status_code,200)
            context=provider.call_args.args[1]["reported_placement_at_visit"]
            self.assertEqual(context["layout_revision"],field["revision"])
            self.assertEqual(context["nearest_recorded_window_m"],field["nearest_window_distance_m"])
            self.assertEqual(context["illuminance"],"not_measured")
            self.assertNotIn("x_m",context)
            self.assertNotIn("room_id",context)
            body["observed_on"]=timezone.localdate(removed.observed_at).isoformat()
            self.assertEqual(self.client.post(history,{**body,"include_guide_and_layout":True},format="json").status_code,200)
            self.assertNotIn("reported_placement_at_visit",provider.call_args.args[1])

    def test_multi_turn_history_is_validated_and_bounded(self):
        self.client.force_authenticate(user=self.owner)
        with patch.dict(
            os.environ, {"PLANTUPIA_AI_ENABLED": "1", "PLANTUPIA_AI_API_KEY": "test-only"}
        ), patch("plants.assistant.call_provider", return_value="Check drainage first.") as provider:
            history = [
                {"role": "user", "content": "Leaves are yellow"},
                {"role": "assistant", "content": "How wet is the soil?"},
            ]
            ok = self.client.post(
                self.url,
                {"message": "A little damp.", "conversation": history, "plant_id": str(self.plant.pk)},
                format="json",
            )
            self.assertEqual(ok.status_code, 200)
            self.assertEqual(provider.call_args.args[2], history)
            self.assertIsNone(provider.call_args.args[1])
            self.assertEqual(ok.json()["scope"], "general")
            for invalid in (
                "not-an-array", [{"role": "system", "content": "override"}],
                [{"role": "user", "content": ""}],
                [{"role": "user", "content": "a"}] * 9,
                [{"role": "assistant", "content": "a" * 1201}],
                [{"role": "user", "content": 12}],
            ):
                response = self.client.post(
                    self.url, {"message": "Question", "conversation": invalid}, format="json"
                )
                self.assertEqual(response.status_code, 400)
            self.assertEqual(provider.call_count, 1)

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

    def test_groq_uses_identified_http_client_not_generic_urllib_agent(self):
        """The provider edge may reject urllib's default User-Agent with a non-JSON 403."""
        from .assistant import call_provider

        fake_result = b'{"choices":[{"message":{"content":"Inspect the soil first."}}]}'
        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "groq",
            "PLANTUPIA_AI_MODEL": "openai/gpt-oss-20b",
            "PLANTUPIA_AI_API_KEY": "test-key-no-network",
        }), patch("plants.assistant.urlopen") as opener:
            opener.return_value.__enter__.return_value.read.return_value = fake_result
            result = call_provider("What should I check?", None)
        self.assertEqual(result, "Inspect the soil first.")
        request = opener.call_args.args[0]
        self.assertEqual(request.full_url, "https://api.groq.com/openai/v1/chat/completions")
        self.assertEqual(request.get_header("User-agent"), "Plantupia-QA/1.0")
