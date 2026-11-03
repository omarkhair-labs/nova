"""Plantupia V2 library discovery never grants staff-verified identity."""
from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .models import BotanicalSpecies, BotanicalSpeciesAlias, CareGuideVersion, PlantUnit, Space


class BotanicalLibraryTrustTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.owner = User.objects.create_user(username="library-owner", password="unused")
        self.other = User.objects.create_user(username="library-other", password="unused")
        self.reviewer = User.objects.create_user(username="library-reviewer", password="unused", is_staff=True)
        self.space = Space.objects.create(owner=self.owner, name="Library QA Space")
        self.species = BotanicalSpecies.objects.create(
            key="ficus-elastica-qa", scientific_name="Ficus elastica (synthetic QA)",
        )
        BotanicalSpeciesAlias.objects.create(
            species=self.species, name="Rubber Plant", language="en",
        )
        self.draft = BotanicalSpecies.objects.create(
            key="unpublished-qa", scientific_name="Unpublished synthetic type",
        )
        self.guide = CareGuideVersion.objects.create(
            species=self.species, version=1, source_url="https://example.test/library-qa",
            source_reference="Synthetic QA content, NOT botanical advice",
            instructions="Synthetic test guidance only.",
            reviewer=self.reviewer, reviewed_at=timezone.now(), published=True,
        )
        CareGuideVersion.objects.create(
            species=self.draft, version=1, source_url="https://example.test/draft",
            source_reference="Synthetic unpublished fixture",
            instructions="Draft synthetic text, not published.",
            reviewer=self.reviewer, reviewed_at=timezone.now(), published=False,
        )
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.library_url = reverse("plantupia-library-species")
        self.detail_url = reverse("plantupia-library-species-detail", args=[self.species.key])
        self.create_url = reverse("plantupia-plants")

    def test_library_exposes_published_species_and_searches_alias(self):
        response = self.api.get(self.library_url)
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(len(response.data["results"]), 1)
        item = response.data["results"][0]
        self.assertEqual(item["key"], self.species.key)
        self.assertEqual(item["identity_basis"], "reference_only")
        self.assertEqual(item["aliases"][0]["name"], "Rubber Plant")
        self.assertEqual(
            self.api.get(self.library_url, {"q": "rubber"}).data["results"][0]["key"],
            self.species.key,
        )
        self.assertEqual(self.api.get(self.library_url, {"q": "unpublished"}).data["results"], [])

    def test_detail_only_shows_published_source_and_no_fake_verification(self):
        response = self.api.get(self.detail_url)
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.data["guide"]["version"], 1)
        self.assertEqual(response.data["guide"]["source_reference"], self.guide.source_reference)
        self.assertEqual(response.data["identity_basis"], "reference_only")
        self.assertEqual(
            self.api.get(reverse("plantupia-library-species-detail", args=[self.draft.key])).status_code,
            404,
        )

    def test_add_plant_selected_species_is_not_staff_verification(self):
        response = self.api.post(self.create_url, {
            "space": str(self.space.pk),
            "label": "My Rubber",
            "species_label": "will be normalized to curated name",
            "selected_species_key": self.species.key,
        }, format="multipart")
        self.assertEqual(response.status_code, 201, response.content)
        plant = PlantUnit.objects.get(pk=response.data["id"])
        self.assertEqual(plant.selected_species_id, self.species.id)
        self.assertIsNone(plant.verified_species_id)
        self.assertEqual(plant.species_label, self.species.scientific_name)
        self.assertEqual(response.data["identity_state"], "user_selected")
        # Legacy per-plant verified guide remains protected until a trust-aware UI ships.
        guide = self.api.get(reverse("plantupia-care-guide", args=[plant.pk]))
        self.assertFalse(guide.data["identity_verified"])
        self.assertIsNone(guide.data["guide"])

    def test_unknown_species_and_legacy_text_work_without_false_claims(self):
        response = self.api.post(self.create_url, {
            "space": str(self.space.pk),
            "label": "Unknown Plant",
            "species_label": "Could be anything",
        }, format="multipart")
        self.assertEqual(response.status_code, 201, response.content)
        self.assertEqual(response.data["identity_state"], "unknown")
        self.assertIsNone(PlantUnit.objects.get(pk=response.data["id"]).selected_species_id)

    def test_unpublished_and_foreign_space_selection_are_rejected(self):
        other_space = Space.objects.create(owner=self.other, name="Not yours")
        for key in ("unpublished-qa", "no-such-species"):
            response = self.api.post(self.create_url, {
                "space": str(self.space.pk), "label": "Not created",
                "selected_species_key": key,
            }, format="multipart")
            self.assertEqual(response.status_code, 400, response.content)
        response = self.api.post(self.create_url, {
            "space": str(other_space.pk), "label": "Not yours",
            "selected_species_key": self.species.key,
        }, format="multipart")
        self.assertEqual(response.status_code, 404, response.content)
        self.assertEqual(PlantUnit.objects.filter(label="Not created").count(), 0)
        self.assertEqual(PlantUnit.objects.filter(label="Not yours").count(), 0)

    def test_library_auth_and_bounded_query(self):
        self.assertEqual(self.api.get(self.library_url, {"offset": "-1"}).status_code, 400)
        self.assertEqual(self.api.get(self.library_url, {"q": "x" * 81}).status_code, 400)
        self.api.force_authenticate(None)
        self.assertIn(self.api.get(self.library_url).status_code, (401, 403))
        self.assertIn(self.api.get(self.detail_url).status_code, (401, 403))
