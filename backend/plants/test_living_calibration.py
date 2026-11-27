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

class MarkerPlaneTests(TestCase):
    def test_known_tagged_rectangle_and_missing_marker(self):
        try:
            import cv2
            import numpy as np
        except ImportError:
            self.skipTest("optional field vision dependencies")
        from PIL import Image
        from .marker_measurement import measure_markers
        gray = np.full((300, 520), 255, dtype=np.uint8)
        dictionary = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_4X4_50)
        for i, x in enumerate((40, 380)):
            gray[100:180, x:x+80] = cv2.aruco.generateImageMarker(dictionary, i, 80)
        outline = [[160, 110], [280, 110], [280, 150], [160, 150]]
        result = measure_markers(Image.fromarray(gray), outline, 2.)
        self.assertEqual(result["status"], "accepted")
        self.assertEqual(result["ids"], [0, 1])
        self.assertAlmostEqual(result["projected_area_cm2"], 3., delta=.09)
        gray[:, 360:] = 255
        self.assertEqual(measure_markers(Image.fromarray(gray), outline, 2.)["status"], "rejected")
