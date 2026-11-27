from datetime import datetime, timezone
from unittest import TestCase
from unittest.mock import patch

from PIL import Image, ImageDraw

from .living_daylight import window_scenario
from .living_regions import visible_patches


class AppearanceAndLightTests(TestCase):
    def test_patch_bounds_in_original_photo(self):
        image = Image.new("RGB", (100, 100), "green")
        ImageDraw.Draw(image).rectangle((20, 30, 39, 49), fill=(200, 185, 20))
        result = visible_patches(image, Image.new("L", image.size, 255), (100, 200, 300, 400))
        self.assertEqual(result, [{"appearance": "yellow_like", "region_fraction": .04,
                                   "bounds": [140, 260, 180, 300]}])

    @patch("plants.living_daylight.solar_position", return_value=(45., 0.))
    def test_opaque_panel_shades_only_admitted_light(self, _):
        room = {"width_m": 4, "length_m": 5, "windows": [{"id":"north","x_m":2,"y_m":0}],
                "geometry_source": "reported"}
        def sample(x, y, screen=None):
            return window_scenario(room, "north", [x, y, 0], top_normal_deg=0, width_m=2,
                                   sill_m=0, head_m=3, latitude=0, longitude=0,
                                   timestamps=[datetime.now(timezone.utc)], screen=screen)["samples"][0]
        self.assertTrue(sample(2, 2)["direct_ray"])
        self.assertFalse(sample(2, 2, (1, 1, 1.5))["direct_ray"])
        self.assertTrue(sample(2, .5, (1, 1, 1.5))["direct_ray"])
        self.assertTrue(sample(1.2, 2, (1, 1, 1.5))["direct_ray"])
        self.assertGreaterEqual(len(sample(2, 2, (1, 1, 1.5))["shadow_xy_m"]), 3)
