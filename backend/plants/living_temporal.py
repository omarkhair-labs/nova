"""Compare two retained projected-area measurements without inferring growth."""
from math import isfinite
from .living_calibration import METHOD


def compare_projected_areas(earlier, later, *, same_part, comparable_capture):
    def refused(reason):
        return {"status": "not_comparable", "reason": reason, "growth": "not_inferred"}

    if same_part is not True or comparable_capture is not True:
        return refused("capture_or_part_not_confirmed")
    if earlier.event.plant_id != later.event.plant_id:
        return refused("different_plant")
    if earlier.event_id == later.event_id or earlier.event.observed_at >= later.event.observed_at:
        return refused("distinct_ordered_visits_required")
    if (not earlier.source_sha256 or not later.source_sha256
            or earlier.source_sha256 == later.source_sha256):
        return refused("different_source_photographs_required")
    part = earlier.annotation.get("part")
    if not part or part != later.annotation.get("part"):
        return refused("different_part_labels")
    from .marker_measurement import METHOD as marker_method
    scales = [t.measurements.get("geometry", {}).get("calibration") or {} for t in (earlier, later)]
    tagged = all(s.get("method") == marker_method for s in scales)
    if tagged and (scales[0].get("ids") != scales[1].get("ids") or scales[0].get("edge_cm") != scales[1].get("edge_cm")):
        return refused("different_tagged_subject")
    method = marker_method if tagged else METHOD
    basis = "reported_flat_tagged_part" if tagged else "operator_reported_face_on_coplanar"
    values, discrepancies = [], []
    for trace in (earlier, later):
        geometry = trace.measurements.get("geometry", {})
        scale = geometry.get("calibration", {})
        area = geometry.get("projected_area_cm2")
        discrepancy = scale.get("scale_check_relative_error")
        if (scale.get("method") != method or scale.get("status") != "accepted"
                or scale.get("plane_basis") != basis
                or type(area) not in (int, float) or not isfinite(area) or area <= 0
                or type(discrepancy) not in (int, float) or not isfinite(discrepancy)
                or not 0 <= discrepancy <= .03):
            return refused("independent_scale_required")
        values.append(area)
        discrepancies.append(discrepancy)
    delta = values[1] - values[0]
    return {
        "status": "comparable_by_operator_report",
        "method": method, "part": part,
        "earlier": {"trace_id": earlier.pk, "event_id": str(earlier.event_id),
                    "observed_at": earlier.event.observed_at.isoformat(),
                    "projected_area_cm2": values[0], "scale_discrepancy": discrepancies[0]},
        "later": {"trace_id": later.pk, "event_id": str(later.event_id),
                  "observed_at": later.event.observed_at.isoformat(),
                  "projected_area_cm2": values[1], "scale_discrepancy": discrepancies[1]},
        "difference_cm2": delta, "relative_difference": delta / values[0],
        "growth": "not_inferred", "uncertainty": "not_quantified",
    }
