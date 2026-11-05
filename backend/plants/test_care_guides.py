"""Synthetic content exercises review/version/access, never approves botanical advice."""
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from datetime import timedelta
from rest_framework.test import APIClient
from .models import BotanicalSpecies, CareGuideVersion, PlantUnit, Space, SpaceMember


class CareGuideTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.owner = users.objects.create_user(username="qa-guide-owner", email="guide-owner@example.test")
        self.reviewer = users.objects.create_user(username="qa-reviewer", email="reviewer@example.test", is_staff=True)
        self.stranger = users.objects.create_user(username="qa-guide-stranger", email="stranger@example.test")
        self.species = BotanicalSpecies.objects.create(key="synthetic-species", scientific_name="Synthetic QA species")
        self.plant = PlantUnit.objects.create(space=Space.objects.create(owner=self.owner, name="Synthetic space"),
                                              label="Synthetic plant", species_label="Unverified free text")
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.url = reverse("plantupia-care-guide", args=[self.plant.pk])

    def guide(self, version=1, **extra):
        return CareGuideVersion.objects.create(species=self.species, version=version,
            source_url="https://example.test/synthetic", source_reference="Synthetic fixture, no real advice",
            instructions="Synthetic instructions", reviewer=self.reviewer, reviewed_at=timezone.now(), **extra)

    def test_unknown_species_free_text_and_unpublished_guide_are_not_verification(self):
        self.guide(published=False)
        self.assertEqual(self.api.get(self.url).json()["state"], "unverified")
        self.plant.verified_species = self.species
        self.plant.save()
        body = self.api.get(self.url).json()
        self.assertTrue(body["identity_verified"])
        self.assertIsNone(body["guide"])

    def test_latest_published_version_and_provenance_only_for_authorized_members(self):
        self.plant.verified_species = self.species
        self.plant.save()
        self.guide(published=True)
        second = self.guide(version=2, published=True)
        self.guide(version=3, published=False)
        body = self.api.get(self.url).json()
        self.assertEqual(body["guide"]["version"], 2)
        self.assertEqual(body["guide"]["source_reference"], second.source_reference)
        self.assertEqual(body["guide"]["reviewer"], self.reviewer.username)
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(self.url).status_code, 404)
        membership = SpaceMember.objects.create(space=self.plant.space, user=self.stranger, role="viewer")
        self.assertEqual(self.api.get(self.url).status_code, 200)
        membership.delete()
        self.assertEqual(self.api.get(self.url).status_code, 404)
        self.api.force_authenticate(None)
        self.assertEqual(self.api.get(self.url).status_code, 401)

    def test_invalid_review_sources_and_future_review_are_rejected(self):
        guide = self.guide(published=False)
        guide.source_url = "http://example.test/source"
        with self.assertRaises(ValidationError):
            guide.full_clean()
        guide.source_url = "https://example.test/source"
        guide.reviewer = self.owner
        with self.assertRaises(ValidationError):
            guide.full_clean()
        guide.reviewer = self.reviewer
        guide.reviewed_at = timezone.now() + timedelta(days=1)
        with self.assertRaises(ValidationError):
            guide.full_clean()

    def test_customer_species_correction_clears_old_botanical_verification(self):
        self.plant.verified_species = self.species
        self.plant.save()
        self.guide(published=True)
        self.assertEqual(self.api.get(self.url).json()["state"], "available")
        response = self.api.patch(reverse("plantupia-plant-detail", args=[self.plant.pk]),
                                  {"species_label": "Identity uncertain"}, format="json")
        self.assertEqual(response.status_code, 200, response.content)
        body = self.api.get(self.url).json()
        self.assertFalse(body["identity_verified"])
        self.assertIsNone(body["guide"])
