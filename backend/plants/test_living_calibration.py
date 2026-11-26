from unittest import TestCase

from .living_calibration import planar_reference


class ReferenceScaleTests(TestCase):
    region = [[50, 50], [100, 50], [100, 100], [50, 100]]
    reference = {"face_on_coplanar": True, "points": [[30, 30], [130, 30], [140, 35], [140, 135]],
                 "reference_cm": 10, "check_cm": 10}

    def test_independent_references_yield_reproducible_scale(self):
        scale, info = planar_reference(self.reference, 200, 200, self.region)
        self.assertEqual(info["status"], "accepted")
        self.assertEqual(scale["reference_px"], 100)
        self.assertEqual(info["scale_check_relative_error"], 0)

    def test_rejects_geometry_that_cannot_support_metric_area(self):
        for fields, reason in [
            ({"face_on_coplanar": False}, "plane_not_confirmed"),
            ({"check_cm": 8}, "independent_scale_check_failed"),
            ({"points": [[30,30],[130,30],[30,40],[130,40]]}, "references_not_near_perpendicular"),
            ({"points": [[30,30],[31,30],[140,35],[140,135]]}, "reference_too_short"),
        ]:
            with self.subTest(reason=reason):
                scale, result = planar_reference({**self.reference, **fields}, 200, 200, self.region)
                self.assertIsNone(scale)
                self.assertEqual(result["reason"], reason)
        with self.assertRaises(ValueError):
            planar_reference({**self.reference, "points": [[30,30],[30,30],[140,35],[140,135]]},
                             200, 200, self.region)
