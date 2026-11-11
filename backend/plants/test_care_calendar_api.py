"""Real care history calendar: privacy, source evidence, bounded dates."""
from datetime import datetime, timedelta, time
from zoneinfo import ZoneInfo

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember


class CalendarHistoryTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.owner = User.objects.create_user(username="calendar_owner", password="test-only")
        self.other = User.objects.create_user(username="calendar_other", password="test-only")
        self.viewer = User.objects.create_user(username="calendar_viewer", password="test-only")
        space = Space.objects.create(owner=self.owner, name="Private conservatory")
        other_space = Space.objects.create(owner=self.other, name="Other person's plants")
        self.plant = PlantUnit.objects.create(space=space, label="Monstera")
        self.second = PlantUnit.objects.create(space=space, label="Ficus")
        self.private_other = PlantUnit.objects.create(space=other_space, label="Hidden fern")
        self.today = timezone.localdate()
        noon = timezone.make_aware(
            datetime.combine(self.today, time(hour=12, minute=0)),
            ZoneInfo("Africa/Cairo"),
        )
        for kind in ("care", "care", "observation"):
            PlantEvent.objects.create(
                plant=self.plant, actor=self.owner,
                kind=kind, evidence_type="observed",
                note="Private event content that calendar should not leak",
                observed_at=noon,
            )
        PlantEvent.objects.create(
            plant=self.second, actor=self.owner, kind="care",
            observed_at=noon, note="Observed follow-up",
        )
        PlantEvent.objects.create(
            plant=self.private_other, actor=self.other, kind="care",
            observed_at=noon, note="Private stranger note",
        )
        self.api = APIClient()
        self.url = reverse("plantupia-care-calendar-history")
        self.range = {
            "from": self.today.isoformat(),
            "to": self.today.isoformat(),
        }

    def test_owner_sees_real_grouped_events_and_not_other_users(self):
        self.api.force_authenticate(self.owner)
        response = self.api.get(self.url, self.range)
        self.assertEqual(response.status_code, 200, response.data)
        rows = response.data["results"]
        self.assertEqual(len(rows), 3)
        self.assertEqual(sum(row["count"] for row in rows), 4)
        self.assertEqual(
            next(row["count"] for row in rows if row["plant_id"] == str(self.plant.pk) and row["kind"] == "care"),
            2,
        )
        self.assertEqual({row["plant_name"] for row in rows}, {"Monstera", "Ficus"})
        self.assertTrue(all(row["date"] == self.today.isoformat() for row in rows))
        self.assertNotIn("Private event content", str(response.data))
        self.assertEqual(response["Cache-Control"], "private, no-store")

    def test_other_person_cannot_read_owner_event_or_guess_plant_id(self):
        self.api.force_authenticate(self.other)
        response = self.api.get(self.url, self.range)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data["results"]), 1)
        self.assertEqual(response.data["results"][0]["plant_name"], "Hidden fern")
        self.api.force_authenticate(None)
        self.assertIn(self.api.get(self.url, self.range).status_code, (401, 403))

    def test_shared_viewer_can_read_visible_history_but_not_private_other(self):
        SpaceMember.objects.create(
            space=self.plant.space, user=self.viewer, role=SpaceMember.Role.VIEWER,
        )
        PlantNickname.objects.create(plant=self.plant, user=self.viewer, nickname="My window Monstera")
        self.api.force_authenticate(self.viewer)
        response = self.api.get(self.url, self.range)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data["results"]), 3)
        self.assertTrue(
            any(row["plant_name"] == "My window Monstera" for row in response.data["results"])
        )
        self.assertFalse(any(row["plant_name"] == "Hidden fern" for row in response.data["results"]))

    def test_invalid_dates_cannot_trigger_unbounded_history_scan(self):
        self.api.force_authenticate(self.owner)
        for query in [
            {},
            {"from": "2026-02-31", "to": "2026-03-01"},
            {"from": self.today.isoformat(), "to": (self.today + timedelta(days=42)).isoformat()},
            {"from": self.today.isoformat(), "to": (self.today - timedelta(days=1)).isoformat()},
        ]:
            self.assertEqual(self.api.get(self.url, query).status_code, 400)
