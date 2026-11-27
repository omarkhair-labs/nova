"""Only isolated QA can opt into these real-source example care guides."""
from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import BotanicalSpecies, CareGuideVersion, PlantUnit, Space
from .qa_demo_care import SAMPLE_CARE_GUIDES


class DemoCareGuideContractTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.staff = User.objects.create_user(
            username="qa_barebones_review", email="qa_reviewer@example.test",
            password="test-only", is_staff=True,
        )
        self.owner = User.objects.create_user(
            username="qa_barebones_owner", email="qa_owner@example.test",
            password="test-only",
        )
        self.stranger = User.objects.create_user(
            username="qa_barebones_stranger", email="qa_stranger@example.test",
            password="test-only",
        )
        self.space = Space.objects.create(
            owner=self.owner, name="Synthetic QA room",
        )
        self.api = APIClient()

    def test_three_distinct_source_backed_qa_examples_have_explicit_disclaimers(self):
        self.assertEqual(len(SAMPLE_CARE_GUIDES), 3)
        self.assertEqual(len({x["key"] for x in SAMPLE_CARE_GUIDES}), 3)
        for sample in SAMPLE_CARE_GUIDES:
            self.assertTrue(sample["source_url"].startswith("https://www.rhs.org.uk/"))
            self.assertIn("SYNTHETIC QA", sample["source_reference"])
            self.assertIn("NOT APPROVED BY PLANTUPIA", sample["instructions"])
            self.assertNotIn("every 7 days", sample["instructions"].lower())

    def test_qa_published_guids_display_only_to_authorized_owner_with_unknown_fallback(self):
        plant_ids = []
        for index, sample in enumerate(SAMPLE_CARE_GUIDES):
            species = BotanicalSpecies.objects.create(
                key=sample["key"], scientific_name=sample["scientific_name"],
            )
            guide = CareGuideVersion(
                species=species, version=1, source_url=sample["source_url"],
                source_reference=sample["source_reference"],
                instructions=sample["instructions"],
                context_notes=sample["context_notes"],
                reviewer=self.staff, reviewed_at=timezone.now(), published=True,
            )
            guide.full_clean()
            guide.save()
            plant = PlantUnit.objects.create(
                space=self.space, label=f"Synthetic plant {index + 1:02}",
                species_label=sample["scientific_name"],
                verified_species=species,
            )
            plant_ids.append(plant.pk)
        unknown = PlantUnit.objects.create(
            space=self.space, label="Synthetic unknown species",
        )
        self.api.force_authenticate(self.owner)
        for plant_id, source in zip(plant_ids, SAMPLE_CARE_GUIDES):
            response = self.api.get(reverse("plantupia-care-guide", args=[plant_id]))
            self.assertEqual(response.status_code, 200, response.content)
            payload = response.json()
            self.assertEqual(payload["state"], "available")
            self.assertEqual(payload["guide"]["source_url"], source["source_url"])
            self.assertIn("NOT APPROVED BY PLANTUPIA", payload["guide"]["instructions"])
        unknown_result = self.api.get(reverse("plantupia-care-guide", args=[unknown.pk]))
        self.assertEqual(unknown_result.status_code, 200)
        self.assertEqual(unknown_result.json()["state"], "unverified")
        self.assertIsNone(unknown_result.json()["guide"])
        self.api.force_authenticate(self.stranger)
        for plant_id in plant_ids + [unknown.pk]:
            self.assertEqual(
                self.api.get(reverse("plantupia-care-guide", args=[plant_id])).status_code,
                404,
            )
