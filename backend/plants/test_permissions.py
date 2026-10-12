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

    def test_space_create_permission_distinguishes_owner_manager_and_carer(self):
        path = reverse("plantupia-spaces")
        self.client.force_authenticate(user=self.owner)
        owner_space = self.client.get(path).json()["results"][0]
        self.assertTrue(owner_space["can_add_plant"])
        membership = SpaceMember.objects.create(
            space=self.space, user=self.stranger, role=SpaceMember.Role.VIEWER,
        )
        self.client.force_authenticate(user=self.stranger)
        self.assertFalse(self.client.get(path).json()["results"][0]["can_add_plant"])
        membership.role = SpaceMember.Role.CARER
        membership.save(update_fields=("role",))
        self.assertFalse(self.client.get(path).json()["results"][0]["can_add_plant"])
        membership.role = SpaceMember.Role.MANAGER
        membership.save(update_fields=("role",))
        self.assertTrue(self.client.get(path).json()["results"][0]["can_add_plant"])

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

    def test_pending_care_checkins_support_older_pages_and_private_scoping(self):
        now = timezone.now()
        for i in range(23):
            PlantEvent.objects.create(
                plant=self.plant, actor=self.owner,
                kind=PlantEvent.Kind.OBSERVATION,
                evidence_type=PlantEvent.Evidence.OBSERVED,
                note=f"Check-in {i}",
                observed_at=now,
                follow_up_on=timezone.localdate() + timedelta(days=i + 1),
            )
        path = reverse("plantupia-events", args=[self.plant.pk])
        self.client.force_authenticate(user=self.owner)
        first = self.client.get(path, {"pending_followups": "1"})
        self.assertEqual(first.status_code, 200)
        self.assertEqual(len(first.json()["results"]), 20)
        self.assertEqual(first.json()["next_offset"], 20)
        second = self.client.get(
            path, {"pending_followups": "1", "offset": 20}
        )
        self.assertEqual(second.status_code, 200)
        self.assertEqual(len(second.json()["results"]), 3)
        self.assertIsNone(second.json()["next_offset"])
        identifiers = [
            row["id"] for row in first.json()["results"] + second.json()["results"]
        ]
        self.assertEqual(len(set(identifiers)), 23)
        self.client.force_authenticate(user=self.stranger)
        self.assertEqual(
            self.client.get(
                path, {"pending_followups": "1", "offset": 20}
            ).status_code,
            404,
        )

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


    def test_self_service_space_and_plant_creation(self):
        self.client.force_authenticate(user=self.stranger)
        create_space = self.client.post(
            reverse("plantupia-spaces"),
            {"name": "  My balcony  ", "location_note": "South window", "owner": self.owner.pk},
            format="json",
        )
        self.assertEqual(create_space.status_code, 201, create_space.content)
        self.assertEqual(create_space.json()["name"], "My balcony")
        space_id = create_space.json()["id"]
        self.assertEqual(Space.objects.get(pk=space_id).owner, self.stranger)
        self.assertEqual(
            len(self.client.get(reverse("plantupia-spaces")).json()["results"]), 1
        )
        create_plant = self.client.post(
            reverse("plantupia-plants"),
            {"space": space_id, "label": "  Basil  ", "species_label": "Ocimum basilicum",
             "placement_note": "Kitchen", "unit_type": "individual"}, format="json",
        )
        self.assertEqual(create_plant.status_code, 201, create_plant.content)
        self.assertEqual(create_plant.json()["label"], "Basil")
        self.assertTrue(create_plant.json()["can_record"])
        self.assertEqual(
            len(self.client.get(reverse("plantupia-plants")).json()["results"]), 1
        )
        plant_id = create_plant.json()["id"]
        care = self.client.post(
            reverse("plantupia-events", args=[plant_id]),
            {"kind": "observation", "note": "New leaves",
             "evidence_type": "observed", "observed_at": timezone.now().isoformat()},
            format="json",
        )
        self.assertEqual(care.status_code, 201, care.content)

    def test_create_requires_auth_and_valid_content(self):
        self.assertEqual(self.client.post(reverse("plantupia-spaces"), {"name": "Room"}).status_code, 401)
        self.assertEqual(
            self.client.post(reverse("plantupia-plants"), {"label": "Rose", "space": str(self.space.pk)}).status_code, 401
        )
        self.client.force_authenticate(user=self.stranger)
        self.assertEqual(
            self.client.post(reverse("plantupia-spaces"), {"name": "   "}, format="json").status_code, 400
        )
        self.assertEqual(
            self.client.post(reverse("plantupia-plants"), {"label": "Rose", "space": str(self.space.pk)}, format="json").status_code, 404
        )
        self.assertEqual(PlantUnit.objects.count(), 1)

    def test_only_owner_or_manager_can_add_plants(self):
        SpaceMember.objects.create(space=self.space, user=self.stranger, role=SpaceMember.Role.VIEWER)
        self.client.force_authenticate(user=self.stranger)
        path = reverse("plantupia-plants")
        self.assertEqual(self.client.post(path, {"label": "Fern", "space": str(self.space.pk)}, format="json").status_code, 404)
        member = SpaceMember.objects.get(space=self.space, user=self.stranger)
        member.role = SpaceMember.Role.CARER
        member.save(update_fields=["role"])
        self.assertEqual(self.client.post(path, {"label": "Fern", "space": str(self.space.pk)}, format="json").status_code, 404)
        member.role = SpaceMember.Role.MANAGER
        member.save(update_fields=["role"])
        response = self.client.post(path, {"label": "Fern", "space": str(self.space.pk)}, format="json")
        self.assertEqual(response.status_code, 201, response.content)
        self.assertEqual(PlantUnit.objects.filter(space=self.space).count(), 2)

    def test_new_plant_accepts_private_photo_without_public_post(self):
        im = Image.new("RGB", (24, 24), (100, 151, 45))
        out = BytesIO()
        im.save(out, format="JPEG")
        metadata = Image.Exif()
        metadata[0x010E] = "Customer private address"
        out = BytesIO()
        im.save(out, format="JPEG", exif=metadata)
        upload = SimpleUploadedFile("new-fern.jpg", out.getvalue(), content_type="image/jpeg")
        self.client.force_authenticate(user=self.owner)
        with TemporaryDirectory() as directory, override_settings(MEDIA_ROOT=directory):
            response = self.client.post(
                reverse("plantupia-plants"),
                {"space": str(self.space.pk), "label": "New fern", "cover_photo": upload},
                format="multipart",
            )
            self.assertEqual(response.status_code, 201, response.content)
            plant = PlantUnit.objects.get(pk=response.json()["id"])
            self.assertTrue(bool(plant.cover_photo))
            with plant.cover_photo.open("rb") as stored:
                saved = Image.open(stored)
                self.assertFalse(saved.getexif())
            from accounts.models import Post
            self.assertEqual(Post.objects.count(), 0)

    def test_manual_check_in_records_real_outcome_once_and_stays_private(self):
        # A user opts into reviewing a real change; the app never predicts
        # a watering date, a plant health score, or a push notification.
        self.client.force_authenticate(user=self.owner)
        follow_date = (timezone.localdate() + timedelta(days=7)).isoformat()
        create = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            {
                "kind": "care", "note": "Moved from the AC draft",
                "observed_at": timezone.now().isoformat(),
                "follow_up_on": follow_date,
            }, format="json",
        )
        self.assertEqual(create.status_code, 201, create.content)
        source_id = create.json()["id"]
        self.assertEqual(create.json()["follow_up_on"], follow_date)
        pending = self.client.get(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"pending_followups": "1"},
        )
        self.assertEqual([x["id"] for x in pending.json()["results"]], [source_id])
        overview = self.client.get(reverse("plantupia-plants")).json()["results"][0]
        self.assertEqual(overview["next_follow_up_on"], follow_date)

        # A user with view-only access can read the scheduled check-in, not close it.
        SpaceMember.objects.create(
            space=self.space, user=self.stranger, role=SpaceMember.Role.VIEWER,
        )
        self.client.force_authenticate(user=self.stranger)
        self.assertEqual(self.client.get(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"pending_followups": "1"},
        ).status_code, 200)
        self.assertEqual(self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"kind": "outcome", "follow_up_for": source_id,
             "note": "Looks better", "observed_at": timezone.now().isoformat()},
            format="json",
        ).status_code, 403)

        self.client.force_authenticate(user=self.owner)
        close_body = {
            "kind": "outcome", "follow_up_for": source_id,
            "note": "New growth is visible today",
            "observed_at": timezone.now().isoformat(),
        }
        result = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            close_body, format="json",
        )
        self.assertEqual(result.status_code, 201, result.content)
        source = PlantEvent.objects.get(pk=source_id)
        self.assertIsNotNone(source.follow_up_completed_at)
        self.assertEqual(PlantEvent.objects.get(pk=result.json()["id"]).follow_up_for, source)
        self.assertEqual(self.client.get(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"pending_followups": "1"},
        ).json()["results"], [])
        self.assertIsNone(
            self.client.get(reverse("plantupia-plants")).json()["results"][0]["next_follow_up_on"]
        )
        duplicate = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            close_body, format="json",
        )
        self.assertEqual(duplicate.status_code, 400)
        self.assertEqual(PlantEvent.objects.filter(plant=self.plant).count(), 2)

    def test_follow_up_cannot_cross_plant_identity_or_precede_today(self):
        self.client.force_authenticate(user=self.owner)
        invalid = self.client.post(
            reverse("plantupia-events", args=[self.plant.pk]),
            {"kind": "observation", "note": "Leaf", "observed_at": timezone.now().isoformat(),
             "follow_up_on": (timezone.localdate() - timedelta(days=1)).isoformat()},
            format="json",
        )
        self.assertEqual(invalid.status_code, 400)
        first = PlantEvent.objects.create(
            plant=self.plant, actor=self.owner, kind=PlantEvent.Kind.CARE,
            observed_at=timezone.now(), follow_up_on=timezone.localdate() + timedelta(days=7),
        )
        other = PlantUnit.objects.create(space=self.space, label="Other plant")
        result = self.client.post(
            reverse("plantupia-events", args=[other.pk]),
            {"kind": "outcome", "follow_up_for": str(first.pk),
             "note": "Must not close another plant", "observed_at": timezone.now().isoformat()},
            format="json",
        )
        self.assertEqual(result.status_code, 400)
        first.refresh_from_db()
        self.assertIsNone(first.follow_up_completed_at)
