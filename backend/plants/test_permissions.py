from django.contrib.auth import get_user_model
from io import BytesIO
from tempfile import TemporaryDirectory

from PIL import Image
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.test import TestCase
from datetime import timedelta
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

    def test_plant_list_exposes_real_latest_activity_not_predicted_health(self):
        event = PlantEvent.objects.create(
            plant=self.plant, actor=self.owner, kind=PlantEvent.Kind.ISSUE,
            evidence_type=PlantEvent.Evidence.OBSERVED,
            note="Leaves changed colour", observed_at=timezone.now(),
        )
        self.client.force_authenticate(user=self.owner)
        plant = self.client.get(reverse("plantupia-plants")).json()["results"][0]
        self.assertTrue(plant["can_record"])
        self.assertEqual(plant["last_event_kind"], "issue")
        self.assertIsNotNone(plant["last_event_at"])
        self.assertNotIn("health_score", plant)
        self.assertEqual(event.plant_id, self.plant.pk)

    def test_shared_viewer_sees_history_but_not_care_controls(self):
        SpaceMember.objects.create(
            space=self.space, user=self.stranger, role=SpaceMember.Role.VIEWER
        )
        self.client.force_authenticate(user=self.stranger)
        response = self.client.get(reverse("plantupia-plants"))
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.json()["results"][0]["can_record"])
        response = self.client.post(
            reverse("plantupia-questions", args=[self.plant.pk]),
            {"public_share_consent": True, "public_topic": "A leaf",
             "caption": "What should I observe?"}, format="json",
        )
        self.assertEqual(response.status_code, 403)

    def test_carer_permissions_and_owner_only_plant_filter(self):
        self.client.force_authenticate(user=self.stranger)
        response = self.client.get(
            reverse("plantupia-plants"), {"space": str(self.space.pk)}
        )
        self.assertEqual(response.json()["results"], [])
        membership = SpaceMember.objects.create(
            space=self.space, user=self.stranger, role=SpaceMember.Role.CARER
        )
        response = self.client.get(reverse("plantupia-plants"))
        self.assertTrue(response.json()["results"][0]["can_record"])
        membership.role = SpaceMember.Role.VIEWER
        membership.save(update_fields=("role",))
        response = self.client.get(reverse("plantupia-plants"))
        self.assertFalse(response.json()["results"][0]["can_record"])

    def test_public_plant_question_requires_consent_and_access(self):
        self.client.force_authenticate(user=self.owner)
        body = {"public_topic": "A leaf", "caption": "What changed?"}
        response = self.client.post(
            reverse("plantupia-questions", args=[self.plant.pk]), body,
            format="json",
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            response.json().get("detail"),
            "Confirm public photo sharing before posting.",
        )
        self.client.force_authenticate(user=self.stranger)
        response = self.client.post(
            reverse("plantupia-questions", args=[self.plant.pk]),
            {**body, "public_share_consent": True}, format="json",
        )
        self.assertEqual(response.status_code, 404)

    def test_circle_rejects_invalid_pagination_cursor(self):
        self.client.force_authenticate(user=self.owner)
        response = self.client.get(
            reverse("plantupia-circle"), {"cursor": "not-a-post-id"}
        )
        self.assertEqual(response.status_code, 400)
        self.assertNotIn("space", str(response.json()))

    def test_living_history_supports_older_pages_and_scoping(self):
        now = timezone.now()
        for i in range(27):
            PlantEvent.objects.create(
                plant=self.plant, actor=self.owner,
                kind=PlantEvent.Kind.OBSERVATION,
                evidence_type=PlantEvent.Evidence.OBSERVED,
                note=f"Observation {i}",
                observed_at=now - timedelta(days=i),
            )
        path = reverse("plantupia-events", args=[self.plant.pk])
        self.client.force_authenticate(user=self.owner)
        first = self.client.get(path)
        self.assertEqual(first.status_code, 200)
        self.assertEqual(len(first.json()["results"]), 20)
        self.assertEqual(first.json()["next_offset"], 20)
        second = self.client.get(path, {"offset": 20})
        self.assertEqual(second.status_code, 200)
        self.assertEqual(len(second.json()["results"]), 7)
        self.assertIsNone(second.json()["next_offset"])
        ids = [x["id"] for x in first.json()["results"] + second.json()["results"]]
        self.assertEqual(len(set(ids)), 27)
        for bad in ("-1", "NaN", "1000001"):
            self.assertEqual(self.client.get(path, {"offset": bad}).status_code, 400)
        self.client.force_authenticate(user=self.stranger)
        self.assertEqual(self.client.get(path, {"offset": "20"}).status_code, 404)

    def test_public_image_sanitizes_camera_location_and_preserves_private_record(self):
        # A photo may be consented for public viewing, but its GPS EXIF data
        # and the customer's private plant IDs must remain private.
        source = Image.new("RGB", (16, 16), (16, 112, 51))
        meta = Image.Exif()
        meta[0x010E] = "Private customer entrance"
        payload = BytesIO()
        source.save(payload, format="JPEG", exif=meta)
        upload = SimpleUploadedFile(
            "private-location-photo.jpg", payload.getvalue(),
            content_type="image/jpeg",
        )
        with TemporaryDirectory() as directory, override_settings(MEDIA_ROOT=directory):
            self.client.force_authenticate(user=self.owner)
            create = self.client.post(
                reverse("plantupia-questions", args=[self.plant.pk]),
                {
                    "public_share_consent": "true",
                    "public_topic": "Leaf colour",
                    "caption": "Could this be less light?",
                    "media_type": "image",
                    "image": upload,
                },
                format="multipart",
            )
            self.assertEqual(create.status_code, 201, create.content)
            from accounts.models import Post
            post = Post.objects.get(pk=create.json()["post_id"])
            with post.image.open("rb") as stored:
                image = Image.open(stored)
                self.assertEqual(image.format, "JPEG")
                self.assertFalse(image.getexif())
            self.client.force_authenticate(user=self.stranger)
            circle = self.client.get(reverse("plantupia-circle"))
            self.assertEqual(circle.status_code, 200)
            self.assertEqual(len(circle.json()["results"]), 1)
            public_payload = str(circle.json())
            self.assertNotIn(str(self.plant.pk), public_payload)
            self.assertNotIn(str(self.space.pk), public_payload)
            self.assertNotIn(self.space.name, public_payload)
            self.assertEqual(
                self.client.get(reverse("plantupia-events", args=[self.plant.pk])).status_code,
                404,
            )
