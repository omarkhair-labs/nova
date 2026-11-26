from io import BytesIO
from tempfile import TemporaryDirectory

from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image
from rest_framework.test import APIClient

from .living_measurements import direct_sun, projected_region
from .models import PlantEvent, PlantUnit, RoomRevision, Space, SpaceMember


def png(mode, size, color):
    buf = BytesIO()
    Image.new(mode, size, color).save(buf, format="PNG")
    return buf.getvalue()


@override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
class FieldWorkflowTests(TestCase):
    def setUp(self):
        user = get_user_model()
        self.owner = user.objects.create_user(email="owner@example.test", username="owner")
        self.other = user.objects.create_user(email="reader@example.test", username="reader")
        self.site = Space.objects.create(owner=self.owner, name="Room")
        self.plant = PlantUnit.objects.create(space=self.site, label="A")
        self.events = reverse("plantupia-events", args=[self.plant.pk])
        self.room = reverse("plantupia-room", args=[self.site.pk])
        self.geometry = dict(width_m=4, length_m=3, base_revision=0, windows=[],
                             plants=[dict(plant_id=str(self.plant.pk), x_m=1, y_m=2)])
        self.client = APIClient()
        self.client.force_authenticate(self.owner)

    def test_existing_photo_submission_replay_and_older_history(self):
        with TemporaryDirectory() as media, self.settings(MEDIA_ROOT=media):
            params = dict(client_submission_id="d628c1db-4181-46b4-b13b-86bd5c8ff862",
                          kind="observation", evidence_type="observed", observed_at=timezone.now().isoformat())
            def upload():
                return self.client.post(self.events, {**params, "photo": SimpleUploadedFile(
                    "frame.png", png("RGB", (32, 32), "black"), content_type="image/png")}, format="multipart")
            first, retry = upload(), upload()
            self.assertEqual((first.status_code, retry.status_code), (201, 200))
            event = PlantEvent.objects.get()
            self.assertIn("predominantly_dark", event.capture_quality["capture_flags"])
            self.assertEqual(self.client.get(self.events).data["results"][0]["capture_quality"], event.capture_quality)
            with event.photo.open("rb") as f:
                self.assertEqual(f.read(), png("RGB", (32, 32), "black"))
            legacy = self.client.post(self.events, {**params, "client_submission_id": "", "capture_quality": {"health": 1}}, format="json")
            self.assertIsNone(legacy.data["capture_quality"])

    def test_room_revisions_replay_conflicts_live_sync_and_transfer(self):
        self.assertEqual(self.client.post(self.room, self.geometry, format="json").status_code, 201)
        self.assertEqual(self.client.post(self.room, self.geometry, format="json").status_code, 200)
        self.assertEqual(self.client.post(self.room, {**self.geometry, "width_m": 5}, format="json").status_code, 409)
        self.assertEqual(RoomRevision.objects.count(), 1)
        event = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care", observed_at=timezone.now())
        response = self.client.get(self.room)
        self.assertEqual(str(response.data["room"]["plants"][0]["latest_event_id"]), str(event.pk))
        self.assertIn("no-store", response["Cache-Control"])
        self.plant.space = Space.objects.create(owner=self.other, name="Other site")
        self.plant.save()
        self.assertEqual(self.client.get(self.room).data["room"]["plants"], [])
        self.assertEqual(self.client.post(self.room, {**self.geometry, "base_revision": 1}, format="json").status_code, 400)

    def test_private_space_and_feature_gate(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.room).status_code, 401)
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.get(self.room).status_code, 404)
        SpaceMember.objects.create(user=self.other, space=self.site, role="viewer")
        self.assertEqual(self.client.get(self.room).data["revision"], 0)
        self.assertEqual(self.client.post(self.room, self.geometry, format="json").status_code, 403)
        with self.settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=False):
            self.assertEqual(self.client.get(self.room).status_code, 404)

    def test_reported_contradiction_and_specialist_correction(self):
        event = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="issue", observed_at=timezone.now())
        args = [self.plant.pk, event.pk]
        evidence = reverse("plantupia-field-evidence", args=args)
        review = reverse("plantupia-evidence-review", args=args)
        options = self.client.post(evidence, dict(symptom="wilting", substrate_report="wet"), format="json").data
        self.assertIsNone(options["diagnosis"])
        self.assertEqual(options["inspection_options"][0]["contradictory_report"], "wet")
        report = dict(submission_id="209330c2-1323-41eb-a5cb-aa161db8efcb", finding="corrected_observation", rationale="Only one visible leaf is folded.")
        self.assertEqual(self.client.post(review, report).status_code, 403)
        self.other.user_permissions.add(Permission.objects.get(codename="review_visual_evidence"))
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.post(review, report).status_code, 404)
        SpaceMember.objects.create(user=self.other, space=self.site, role="carer")
        self.assertEqual(self.client.post(review, report).status_code, 201)
        self.assertEqual(self.client.post(review, report).status_code, 200)
        self.assertEqual(self.client.post(review, {**report, "rationale": "Different text"}).status_code, 409)
        self.assertEqual(self.client.get(evidence).data["review_count"], 1)
        self.assertEqual(PlantEvent.objects.count(), 1)


class MeasurementTests(TestCase):
    def test_known_planar_area_and_bad_scale_check(self):
        image = png("L", (20, 10), 255)
        ref = dict(reference_cm=10, reference_px=20, check_cm=5, check_px=10, rectified_coplanar=True)
        self.assertEqual(projected_region(image, reference=ref)["projected_area_cm2"], 50)
        for other in (None, {**ref, "check_px": 12}, {**ref, "rectified_coplanar": False}):
            self.assertIsNone(projected_region(image, reference=other)["projected_area_cm2"])

    def test_window_ray_front_behind_above_and_night(self):
        data = dict(inward_m=1, along_m=1, height_m=.5, window_left_m=0, window_right_m=2,
                    sill_m=.8, head_m=2, sun_elevation_deg=45, sun_azimuth_deg=180, outward_normal_deg=180)
        self.assertTrue(direct_sun(**data)["unobstructed_direct_ray"])
        for changes in (dict(sun_azimuth_deg=0), dict(sun_elevation_deg=-1), dict(height_m=2.5)):
            result = direct_sun(**{**data, **changes})
            self.assertFalse(result["unobstructed_direct_ray"])
            self.assertIsNone(result["illuminance_lux"])
