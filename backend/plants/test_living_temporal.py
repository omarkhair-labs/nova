from datetime import timedelta
from types import SimpleNamespace
from unittest import TestCase

from django.utils import timezone
from .living_temporal import compare_projected_areas


class ProjectedVisitTests(TestCase):
    def setUp(self):
        now = timezone.now()
        def trace(pk, event_id, date, area, digest):
            return SimpleNamespace(pk=pk, event_id=event_id, event=SimpleNamespace(plant_id="same",
                observed_at=date), source_sha256=digest, annotation={"part": "leaf"},
                measurements={"geometry": {"projected_area_cm2": area,
                    "calibration": {"method": "face-on-two-segment-v1", "status": "accepted",
                    "plane_basis": "operator_reported_face_on_coplanar", "scale_check_relative_error": .01}}})
        self.a = trace(1, "earlier", now-timedelta(days=7), 10., "first")
        self.b = trace(2, "later", now, 12., "second")

    def test_describes_change_without_inferred_growth(self):
        result = compare_projected_areas(self.a, self.b, same_part=True, comparable_capture=True)
        self.assertEqual(result["status"], "comparable_by_operator_report")
        self.assertEqual(result["difference_cm2"], 2.)
        self.assertAlmostEqual(result["relative_difference"], .2)
        self.assertEqual(result["growth"], "not_inferred")
        self.assertEqual(result["uncertainty"], "not_quantified")

    def test_refuses_unconfirmed_reused_or_uncalibrated_photos(self):
        self.assertEqual(compare_projected_areas(self.a, self.b, same_part=False,
            comparable_capture=True)["status"], "not_comparable")
        self.assertEqual(compare_projected_areas(self.b, self.a, same_part=True,
            comparable_capture=True)["reason"], "distinct_ordered_visits_required")
        self.b.source_sha256 = self.a.source_sha256
        self.assertEqual(compare_projected_areas(self.a, self.b, same_part=True,
            comparable_capture=True)["reason"], "different_source_photographs_required")
        self.b.source_sha256 = "second"
        self.b.measurements["geometry"]["calibration"]["status"] = "rejected"
        self.assertEqual(compare_projected_areas(self.a, self.b, same_part=True,
            comparable_capture=True)["reason"], "independent_scale_required")
