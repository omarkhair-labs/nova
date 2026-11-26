"""Reproducible numerical controls for spatial and temporal estimation."""
import unittest

import cv2
import numpy as np

from floor_plane import floor_positions
from register_visits import register


class GeometryTests(unittest.TestCase):
    def test_known_floor_and_independent_reference_disagreement(self):
        pixels = [(90, 50), (450, 65), (570, 415), (20, 390)]
        metres = [(0, 0), (4, 0), (4, 3), (0, 3)]
        h = cv2.getPerspectiveTransform(np.float32(metres), np.float32(pixels))
        def image(xy):
            return cv2.perspectiveTransform(np.float64([[xy]]), h)[0, 0].tolist()
        controls = [dict(pixel=p, metres=m) for p,m in zip(pixels, metres)]
        checks = [dict(pixel=image(p), metres=p) for p in [(1,1), (3,2)]]
        result = floor_positions(controls, checks, [image((2,1.75))])
        np.testing.assert_allclose(result["floor_xy_m"], [[2,1.75]], atol=1e-5)
        self.assertEqual(floor_positions(controls, checks, [[800,300]])["reason"], "outside_surveyed_footprint")
        checks[0]["metres"] = [1.2,1]
        self.assertEqual(floor_positions(controls, checks, [image((2,1))])["status"], "abstained")

    def test_background_registration_and_abstention(self):
        rng = np.random.default_rng(1729)
        image = rng.integers(0, 255, (480,640,3), dtype=np.uint8)
        roi = np.zeros(image.shape[:2], np.uint8)
        roi[160:320,240:400] = 255
        static = 255 - roi
        offset = np.float32([[1,0,8],[0,1,-5]])
        later = cv2.warpAffine(image, offset, (640,480))
        later_static = cv2.warpAffine(static, offset, (640,480))
        result = register(image, later, static, later_static, roi)
        self.assertEqual(result["status"], "aligned")
        np.testing.assert_allclose(np.array(result["matrix_after_to_before"])[:,2], [-8,5], atol=.6)
        self.assertLess(result["mean_absolute_rgb_difference_roi"], 6)
        self.assertIsNone(result["growth"])
        empty = np.zeros_like(image)
        self.assertEqual(register(empty, empty, static, static, roi)["status"], "not_comparable")


if __name__ == "__main__":
    unittest.main()
