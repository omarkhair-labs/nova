from datetime import datetime, timezone
from unittest.mock import patch

from django.test import SimpleTestCase

from .living_daylight import window_scenario


class DaylightGeometryTests(SimpleTestCase):
    def setUp(self):
        self.room = {"width_m":4., "length_m":3., "height_m":2.8,
                     "windows":[{"id":"top","x_m":2.,"y_m":0.}],
                     "geometry_source":"reported"}
        self.params = {"top_normal_deg":0., "width_m":2., "sill_m":.5, "head_m":2.4,
                       "latitude":0., "longitude":0.,
                       "timestamps":[datetime(2026,3,20,9,tzinfo=timezone.utc)]}

    @patch("plants.living_daylight.solar_position", return_value=(45.,0.))
    def test_same_scenario_distinguishes_window_screen_and_clear_positions(self, _solar):
        def sample(point, screen=None):
            result = window_scenario(self.room,"top",point,**self.params,screen=screen)
            self.assertIsNone(result["illuminance_lux"])
            self.assertEqual(result["placement_suitability"],"unassessed")
            return result["samples"][0]
        clear=sample([2.,1.5,0.])
        blocked=sample([2.,1.5,0.],[.6,1.,1.])
        beyond=sample([2.,2.1,0.],[.6,1.,1.])
        outside=sample([.5,1.5,0.],[.6,1.,1.])
        self.assertTrue(clear["direct_ray"])
        self.assertFalse(blocked["direct_ray"])
        self.assertTrue(beyond["direct_ray"])
        self.assertFalse(outside["direct_ray"])
        self.assertEqual(clear["footprint_xy_m"],blocked["footprint_xy_m"])
        self.assertEqual(blocked["shadow_xy_m"],beyond["shadow_xy_m"])
        self.assertGreaterEqual(len(blocked["shadow_xy_m"]),3)

    @patch("plants.living_daylight.solar_position", return_value=(45.,90.))
    def test_screen_ray_uses_the_same_coordinates_on_another_wall(self, _solar):
        room={**self.room,"windows":[{"id":"east","x_m":4.,"y_m":1.5}]}
        blocked=window_scenario(room,"east",[2.5,1.5,0.],**self.params,screen=[.6,1.,1.])
        clear=window_scenario(room,"east",[1.9,1.5,0.],**self.params,screen=[.6,1.,1.])
        self.assertFalse(blocked["samples"][0]["direct_ray"])
        self.assertTrue(clear["samples"][0]["direct_ray"])
        for x,y in blocked["samples"][0]["shadow_xy_m"]:
            self.assertTrue(0<=x<=4 and 0<=y<=3)

    def test_recorded_ceiling_is_a_geometry_constraint(self):
        for point,changes in [([2.,1.,2.9],{}),([2.,1.,0.],{"head_m":3.}),
                              ([2.,1.,0.],{"screen":[.6,1.,3.]}),
                              ([2.,1.,float("nan")],{})]:
            with self.assertRaises(ValueError):
                window_scenario(self.room,"top",point,**{**self.params,**changes})
