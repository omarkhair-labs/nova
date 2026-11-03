"""Synthetic care topics, version/approval and legacy behavior; no botanical advice."""
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .company_services import action_token, guide_state, publish_guide
from .inventory_import import ImportProblem
from .models import (
    BotanicalSpecies, CareGuideVersion, CareGuideSection, PlantUnit, Space,
)


class StructuredCareGuideTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.owner = User.objects.create_user(
            username="topic-owner", email="topic-owner@example.test",
        )
        self.stranger = User.objects.create_user(
            username="topic-outsider", email="topic-outsider@example.test",
        )
        self.reviewer = User.objects.create_superuser(
            username="topic-reviewer", email="topic-reviewer@example.test", password="not-used",
        )
        self.species = BotanicalSpecies.objects.create(
            key="synthetic-guided-type", scientific_name="Synthetic reference species",
        )
        self.space = Space.objects.create(owner=self.owner, name="QA owner space")
        self.plant = PlantUnit.objects.create(
            space=self.space, label="Home plant", selected_species=self.species,
        )
        self.guide = CareGuideVersion.objects.create(
            species=self.species, version=1,
            source_url="https://example.test/synthetic-reference",
            source_reference="Synthetic test guide — not advice",
            instructions="", published=False,
        )
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.url = reverse("plantupia-care-guide", args=[self.plant.pk])

    def add_topic(self, topic="water", position=0, **extra):
        return CareGuideSection.objects.create(
            guide=self.guide, topic=topic,
            title="Synthetic " + topic, instructions="Observe only; synthetic testing.",
            position=position, **extra,
        )

    def publish(self):
        token = action_token(self.reviewer, "guide", self.guide, guide_state(self.guide))
        return publish_guide(self.reviewer, self.guide.pk, token)

    def test_published_topic_sections_visible_to_owner_but_never_staff_verified(self):
        self.add_topic("water", position=20)
        self.add_topic("fertilizing", position=5,
                       source_url="https://example.test/synthetic-fertilizer",
                       source_reference="Synthetic second source")
        self.publish()
        body = self.api.get(self.url).json()
        self.assertEqual(body["state"], "general")
        self.assertEqual(body["identity_basis"], "user_selected")
        self.assertFalse(body["identity_verified"])
        payload = body["guide"]
        self.assertEqual([s["topic"] for s in payload["sections"]], ["fertilizing", "water"])
        self.assertEqual(payload["sections"][0]["source_url"], "https://example.test/synthetic-fertilizer")
        self.assertEqual(payload["sections"][1]["source_url"], self.guide.source_url)
        self.assertEqual(payload["sections"][1]["source_reference"], self.guide.source_reference)
        self.assertIn("Synthetic water", payload["instructions"])  # legacy Android fallback
        self.assertEqual(payload["legacy_instructions"], "")
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(self.url).status_code, 404)

    def test_latest_published_version_only_and_reference_detail_same_topics(self):
        self.add_topic()
        self.publish()
        response = self.api.get(reverse("plantupia-library-species-detail", args=[self.species.key]))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["guide"]["sections"][0]["topic"], "water")
        new_draft = CareGuideVersion.objects.create(
            species=self.species, version=2, source_url=self.guide.source_url,
            source_reference=self.guide.source_reference, instructions="Unpublished test text",
        )
        CareGuideSection.objects.create(
            guide=new_draft, topic="soil", title="Synthetic soil",
            instructions="This unpublished guidance cannot be visible.",
        )
        self.assertEqual(self.api.get(self.url).data["guide"]["version"], 1)

    def test_draft_topics_cannot_leak_or_publish_without_content(self):
        self.assertEqual(self.api.get(self.url).data["guide"], None)
        with self.assertRaises(ValidationError):
            self.publish()
        self.assertFalse(CareGuideVersion.objects.get(pk=self.guide.pk).published)
        self.add_topic("inspection")
        self.assertEqual(self.api.get(self.url).data["guide"], None)
        self.publish()
        self.assertEqual(self.api.get(self.url).data["guide"]["sections"][0]["topic"], "inspection")

    def test_legacy_published_narrative_is_preserved_full(self):
        self.guide.instructions = "Full synthetic legacy narrative — not botanical advice."
        self.guide.save()
        self.publish()
        data = self.api.get(self.url).data["guide"]
        self.assertEqual(data["sections"], [])
        self.assertEqual(data["instructions"], self.guide.instructions)
        self.assertEqual(data["legacy_instructions"], self.guide.instructions)

    def test_published_sections_and_withdrawn_versions_are_immutable(self):
        topic = self.add_topic()
        self.publish()
        topic.instructions = "Should never change"
        with self.assertRaises(ValidationError):
            topic.save()
        with self.assertRaises(ValidationError):
            topic.delete()
        with self.assertRaises(ValidationError):
            self.add_topic("light")
        old_token = action_token(self.reviewer, "guide", self.guide, guide_state(self.guide))
        publish_guide(self.reviewer, self.guide.pk, old_token, withdraw=True)
        self.guide.refresh_from_db()
        self.assertFalse(self.guide.published)
        with self.assertRaises(ValidationError):
            topic.delete()
        with self.assertRaises(ValidationError):
            topic.save()

    def test_review_token_bound_to_all_topic_content(self):
        self.add_topic()
        old_token = action_token(self.reviewer, "guide", self.guide, guide_state(self.guide))
        self.add_topic("light")
        with self.assertRaises(ImportProblem):
            publish_guide(self.reviewer, self.guide.pk, old_token)
        self.assertFalse(CareGuideVersion.objects.get(pk=self.guide.pk).published)
        self.publish()
        self.assertTrue(CareGuideVersion.objects.get(pk=self.guide.pk).published)

    def test_sections_source_is_https_and_topic_must_be_unique(self):
        self.add_topic()
        with self.assertRaises(ValidationError):
            self.add_topic("light", source_url="http://example.test/no")
        with self.assertRaises(ValidationError):
            self.add_topic("water")

    def test_company_editor_rejects_customer_and_locks_published_guide(self):
        path = reverse("company:guide-section-create", args=[self.guide.pk])
        payload = {
            "topic": "soil", "title": "Synthetic soil", "instructions": "Source-based test text",
            "source_url": "", "source_reference": "", "position": 4,
        }
        with patch("plants.company_views.service.enabled", return_value=True):
            self.client.force_login(self.owner)
            self.assertEqual(self.client.post(path, payload).status_code, 403)
            self.client.force_login(self.reviewer)
            resp = self.client.post(path, payload)
            self.assertEqual(resp.status_code, 302, resp.content)
            self.assertEqual(CareGuideSection.objects.filter(guide=self.guide).count(), 1)
            self.publish()
            self.assertEqual(self.client.post(path, payload).status_code, 409)
            edit_path = reverse("company:guide-section-edit", args=[
                self.guide.pk, self.guide.sections.first().pk,
            ])
            self.assertEqual(self.client.get(edit_path).status_code, 409)
