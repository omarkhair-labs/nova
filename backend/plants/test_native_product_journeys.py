"""C0 Journey contract: real Django models and authenticated routes, no public AI secret.

These are integration tests of server persistence/permissions, NOT assertions that
a remote Oracle QA API, device reminders, Nova video or media playback work.
"""
from datetime import timedelta

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space


class PlantupiaCoreJourneysTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.user = User.objects.create_user(
            username="living_journey_owner", email="owner@journey.example.test", password="test-only",
        )
        self.other = User.objects.create_user(
            username="living_journey_other", email="other@journey.example.test", password="test-only",
        )
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

    def test_zero_to_plant_care_reschedule_human_outcome_and_private_history(self):
        spaces = reverse("plantupia-spaces")
        plants = reverse("plantupia-plants")
        self.assertEqual(self.client.get(spaces).json()["results"], [])

        start = self.client.post(
            spaces,
            {"name": "South-facing balcony", "location_note": "Privacy-sensitive location"},
            format="json",
        )
        self.assertEqual(start.status_code, 201, start.content)
        space_id = start.json()["id"]
        created = self.client.post(
            plants,
            {"space": space_id, "label": "Basil", "species_label": "Unknown",
             "placement_note": "Near the kitchen window"},
            format="json",
        )
        self.assertEqual(created.status_code, 201, created.content)
        plant_id = created.json()["id"]
        events = reverse("plantupia-events", args=[plant_id])

        first_date = timezone.localdate() + timedelta(days=5)
        care = self.client.post(
            events,
            {"kind": "care", "note": "I checked the soil; did not water",
             "observed_at": timezone.now().isoformat(),
             "follow_up_on": first_date.isoformat()},
            format="json",
        )
        self.assertEqual(care.status_code, 201, care.content)
        source_id = care.json()["id"]
        self.assertEqual(self.client.get(events, {"pending_followups": "1"}).json()["results"][0]["id"], source_id)

        new_date = first_date + timedelta(days=4)
        rescheduled = self.client.patch(
            reverse("plantupia-follow-up-date", args=[plant_id, source_id]),
            {"follow_up_on": new_date.isoformat()}, format="json",
        )
        self.assertEqual(rescheduled.status_code, 200, rescheduled.content)
        self.assertEqual(rescheduled.json()["follow_up_on"], new_date.isoformat())
        self.assertEqual(
            self.client.get(plants).json()["results"][0]["next_follow_up_on"],
            new_date.isoformat(),
        )

        outcome = self.client.post(
            events,
            {"kind": "outcome", "note": "I inspected again; soil remains damp",
             "follow_up_for": source_id,
             "observed_at": timezone.now().isoformat()},
            format="json",
        )
        self.assertEqual(outcome.status_code, 201, outcome.content)
        self.assertEqual(outcome.json()["follow_up_for"], source_id)
        self.assertEqual(self.client.get(events, {"pending_followups": "1"}).json()["results"], [])
        self.assertEqual(PlantEvent.objects.filter(plant_id=plant_id).count(), 3)
        self.assertTrue(PlantEvent.objects.get(pk=source_id).follow_up_completed_at)

        self.client.force_authenticate(user=self.other)
        self.assertEqual(self.client.get(events).status_code, 404)
        self.assertEqual(self.client.get(plants).json()["results"], [])
        self.assertEqual(self.client.get(spaces).json()["results"], [])
        self.assertEqual(
            self.client.patch(
                reverse("plantupia-follow-up-date", args=[plant_id, source_id]),
                {"follow_up_on": (new_date + timedelta(days=2)).isoformat()},
                format="json",
            ).status_code, 404,
        )

    def test_one_seven_and_hundred_real_inventory_rows_stay_isolated_by_space(self):
        household = Space.objects.create(owner=self.user, name="Home")
        office = Space.objects.create(owner=self.user, name="Test office")
        private_other = Space.objects.create(owner=self.other, name="Other account")
        PlantUnit.objects.create(space=household, label="First plant")
        self.assertEqual(len(self.client.get(reverse("plantupia-plants")).json()["results"]), 1)

        PlantUnit.objects.bulk_create([
            PlantUnit(space=household, label=f"Household {i}") for i in range(2, 8)
        ])
        PlantUnit.objects.bulk_create([
            PlantUnit(space=office, label=f"Office {i}") for i in range(100)
        ])
        PlantUnit.objects.create(space=private_other, label="Never yours")
        endpoint = reverse("plantupia-plants")
        home = self.client.get(endpoint, {"space": str(household.pk)})
        work = self.client.get(endpoint, {"space": str(office.pk)})
        self.assertEqual(home.status_code, 200, home.content)
        self.assertEqual(work.status_code, 200, work.content)
        self.assertEqual(len(home.json()["results"]), 7)
        self.assertEqual(len(work.json()["results"]), 100)
        self.assertEqual(len(self.client.get(endpoint).json()["results"]), 107)
        self.assertNotIn("Never yours", str(self.client.get(endpoint).json()))
