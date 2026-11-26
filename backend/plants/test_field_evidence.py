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

from .living_measurements import aperture_footprint, direct_sun, projected_region
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

    def test_annotated_region_is_private_replayable_and_reproducible(self):
        with TemporaryDirectory() as media, self.settings(MEDIA_ROOT=media):
            event = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="observation", observed_at=timezone.now(),
                photo=SimpleUploadedFile("frame.png", png("RGB", (64, 64), "green"), content_type="image/png"))
            url = reverse("plantupia-trace-region", args=[self.plant.pk, event.pk])
            trace = dict(submission_id="8ec9aed3-1bda-41cc-9538-dabc9870d3ef", part="leaf",
                         vertices=[[10,10],[19,10],[19,19],[10,19]])
            evidence = reverse("plantupia-field-evidence", args=[self.plant.pk, event.pk])
            legacy = self.client.get(evidence)
            self.assertEqual(legacy.status_code, 200)
            self.assertEqual((legacy.data["quality"]["width"], legacy.data["quality"]["height"]), (64, 64))
            event.refresh_from_db()
            self.assertIsNone(event.capture_quality)
            result = self.client.post(url, trace, format="json")
            self.assertEqual(result.status_code, 201)
            geometry = result.data["measurements"]["geometry"]
            self.assertEqual(geometry["projected_area_px2"], 100)
            self.assertEqual(geometry["traced_outline_length_px"], 36)
            self.assertIsNone(geometry["projected_area_cm2"])
            self.assertEqual(self.client.post(url, trace, format="json").status_code, 200)
            calibrated = {**trace, "submission_id": "61e091d9-339c-4a8d-ac31-67c31e63119d",
                "point_reference": {"face_on_coplanar": True, "points": [[4,4],[34,4],[39,5],[39,35]],
                                    "reference_cm": 3, "check_cm": 3}}
            response = self.client.post(url, calibrated, format="json")
            self.assertEqual(response.status_code, 201, response.data)
            g = response.data["measurements"]["geometry"]
            self.assertEqual(g["calibration"]["status"], "accepted")
            self.assertAlmostEqual(g["projected_area_cm2"], 1.0)
            rejected = {**calibrated, "submission_id": "098097c4-0492-47b7-8af1-193a988dbb8c",
                "point_reference": {**calibrated["point_reference"], "check_cm": 5}}
            response = self.client.post(url, rejected, format="json")
            self.assertEqual(response.status_code, 201)
            self.assertIsNone(response.data["measurements"]["geometry"]["projected_area_cm2"])
            self.assertEqual(response.data["measurements"]["geometry"]["calibration"]["reason"],
                             "independent_scale_check_failed")
            self.assertEqual(self.client.post(url, {**trace, "part": "pot"}, format="json").status_code, 409)
            self.assertEqual(self.client.post(url, {**trace, "submission_id": "1699c321-846e-43cb-8830-1cba9c070c9a",
                "vertices": [[1,1],[8,8],[1,8],[8,1]]}, format="json").status_code, 400)
            self.client.force_authenticate(self.other)
            self.assertEqual(self.client.post(url, trace, format="json").status_code, 404)

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

    def test_room_layout_history_stays_private_and_excludes_backdated_late_care(self):
        stamp = timezone.now()
        PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care", observed_at=stamp)
        self.assertEqual(self.client.post(self.room, self.geometry, format="json").status_code, 201)
        later = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care",
                                          observed_at=timezone.now())
        revision = self.geometry | {"base_revision": 1, "width_m": 5}
        self.assertEqual(self.client.post(self.room, revision, format="json").status_code, 201)
        current = self.client.get(self.room).data
        self.assertEqual([layout["revision"] for layout in current["layout_revisions"]], [2, 1])
        self.assertEqual(current["room"]["plants"][0]["latest_event_id"], later.pk)
        old_url = self.room + "?revision=1"
        historical = self.client.get(old_url).data
        self.assertTrue(historical["historical"])
        self.assertEqual(historical["room"]["width_m"], 4)
        self.assertNotEqual(historical["room"]["plants"][0]["latest_event_id"], later.pk)
        PlantEvent.objects.filter(pk=later.pk).update(observed_at=stamp)
        self.assertNotEqual(self.client.get(old_url).data["room"]["plants"][0]["latest_event_id"], later.pk)
        self.assertEqual(self.client.get(self.room + "?revision=3").status_code, 404)
        self.assertEqual(self.client.get(self.room + "?revision=-1").status_code, 400)
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.get(old_url).status_code, 404)
        self.client.force_authenticate(self.owner)
        self.plant.space = Space.objects.create(owner=self.other, name="New location")
        self.plant.save()
        self.assertEqual(self.client.get(old_url).data["room"]["plants"], [])

    def test_daylight_uses_current_window_time_and_orientation(self):
        room = {**self.geometry, "windows": [dict(id="east", x_m=4, y_m=1.5)]}
        self.client.post(self.room, room, format="json")
        url = reverse("plantupia-daylight", args=[self.site.pk])
        scenario = dict(revision=1, window_id="east", top_normal_deg=0, point=[3.7, 1.5, 1.1],
                        width_m=2, sill_m=.4, head_m=2.4, latitude=0, longitude=0,
                        timestamps=["2026-03-20T07:00:00Z", "2026-03-20T19:00:00Z"])
        response = self.client.post(url, scenario, format="json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual([x["direct_ray"] for x in response.data["samples"]], [True,False])
        self.assertEqual(response.data["plane_height_m"], 1.1)
        day, night = response.data["samples"]
        self.assertGreaterEqual(len(day["footprint_xy_m"]), 3)
        self.assertTrue(all(0 <= x <= 4 and 0 <= y <= 3 for x, y in day["footprint_xy_m"]))
        self.assertEqual(night["footprint_xy_m"], [])
        self.assertIsNone(response.data["illuminance_lux"])
        self.assertEqual(self.client.post(url, {**scenario,"revision":2}, format="json").status_code, 409)
        self.assertEqual(self.client.post(url, {**scenario,"timestamps":["2026-03-20T07:00:00"]}, format="json").status_code, 400)
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.post(url, scenario, format="json").status_code, 404)

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


    def test_aperture_footprint_matches_ray_intersection_and_clips_to_room(self):
        aperture = dict(height_m=0, window_left_m=.5, window_right_m=1.5, sill_m=.5,
                        head_m=2, sun_elevation_deg=45, sun_azimuth_deg=0, outward_normal_deg=0)
        footprint = aperture_footprint(wall_span=2, room_depth=1.25, **aperture)
        self.assertEqual({round(x, 3) for x, _ in footprint}, {.5, 1.5})
        self.assertEqual({round(y, 3) for _, y in footprint}, {.5, 1.25})
        self.assertTrue(direct_sun(inward_m=1, along_m=1, **aperture)["unobstructed_direct_ray"])
        self.assertFalse(direct_sun(inward_m=.25, along_m=1, **aperture)["unobstructed_direct_ray"])
        oblique = aperture_footprint(wall_span=2, room_depth=1.25, **{**aperture, "sun_azimuth_deg": 40})
        self.assertTrue(any(x == 0 for x, _ in oblique))
        self.assertTrue(all(0 <= x <= 2 and 0 <= y <= 1.25 for x, y in oblique))
        self.assertEqual(aperture_footprint(wall_span=2, room_depth=1.25, **{**aperture, "height_m": 3}), [])
