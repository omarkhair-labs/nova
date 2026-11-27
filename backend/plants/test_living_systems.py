from io import BytesIO
from unittest import TestCase

from PIL import Image, ImageDraw

from .living_vision import candidate_green_mask, region_features, compare_regions
from .living_space import build_room, living_snapshot


def png(image):
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


class LivingVisionTests(TestCase):
    def setUp(self):
        self.image = Image.new("RGB", (640, 480), "green")
        draw = ImageDraw.Draw(self.image)
        draw.rectangle((320, 0, 639, 479), fill="yellow")
        self.data = png(self.image)
        self.mask = png(Image.new("L", self.image.size, 255))

    def test_color_bins_and_explicit_unknown(self):
        result = region_features(self.data, self.mask)
        self.assertAlmostEqual(result["color_fractions"]["green_like"], .5)
        self.assertAlmostEqual(result["color_fractions"]["yellow_like"], .5)
        self.assertEqual(result["health"], "not_assessed")

    def test_candidate_is_color_only(self):
        result = region_features(self.data, candidate_green_mask(self.data))
        self.assertAlmostEqual(result["image_fraction"], .5)
        self.assertEqual(result["region_identity"], "caller_supplied_not_verified")

    def test_empty_and_wrong_dimensions(self):
        for mask in (Image.new("L", self.image.size), Image.new("L", (2, 2), 255)):
            with self.assertRaises(ValueError):
                region_features(self.data, png(mask))

    def test_comparison_refuses_unmatched_or_low_quality(self):
        result = region_features(self.data, self.mask)
        self.assertEqual(compare_regions(result, result, same_plant=False, comparable_capture=True)["status"], "not_comparable")
        result["quality"]["status"] = "review_capture"
        self.assertEqual(compare_regions(result, result, same_plant=True, comparable_capture=True)["status"], "not_comparable")

    def test_comparable_result_is_not_growth_or_diagnosis(self):
        result = region_features(self.data, self.mask)
        result["quality"]["status"] = "no_quality_flags"
        delta = compare_regions(result, result, same_plant=True, comparable_capture=True)
        self.assertEqual(delta["color_delta_fraction"]["green_like"], 0)
        self.assertEqual(delta["growth"], "not_measured")


class LivingSpaceTests(TestCase):
    def room(self):
        return build_room("room", 6, 4, [{"plant_id": "p", "x_m": 3, "y_m": 4}], [{"id": "w", "x_m": 0, "y_m": 0}])

    def test_metric_geometry_not_light_measurement(self):
        room = self.room()
        self.assertEqual(room["area_m2"], 24)
        self.assertEqual(room["plants"][0]["nearest_window_distance_m"], 5)
        self.assertEqual(room["plants"][0]["illuminance"], "not_measured")

    def test_reject_invalid_position(self):
        with self.assertRaises(ValueError):
            build_room("room", 6, 4, [{"plant_id": "p", "x_m": 7, "y_m": 0}], [])

    def test_snapshot_keeps_provenance(self):
        result = living_snapshot(self.room(), [{"plant_id": "p", "evidence_id": "e", "observed_at": "2026-11-26T08:00:00+02:00"}])
        self.assertEqual(result["plants"][0]["latest_supplied_observation"]["evidence_id"], "e")
        self.assertFalse(result["live_sync"])

    def test_snapshot_rejects_foreign_plant_and_missing_offset(self):
        for key, stamp in (("other", "2026-11-26T08:00:00Z"), ("p", "2026-11-26T08:00:00")):
            with self.assertRaises(ValueError):
                living_snapshot(self.room(), [{"plant_id": key, "evidence_id": "e", "observed_at": stamp}])
