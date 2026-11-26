"""Planar mask measurements and direct-sun scenarios with explicit reference frames."""
from math import cos, isfinite, radians, sin, tan


from .living_vision import _open


def positive(value):
    if type(value) not in (int, float) or not isfinite(value) or value <= 0:
        raise ValueError("Expected a finite positive measurement.")
    return float(value)


def projected_region(mask_png, *, reference=None):
    """One operator-labelled region, after rectification. Count interior pixels only."""
    mask = _open(mask_png, {"PNG"}).convert("L").point(lambda v: 255 if v >= 128 else 0)
    bbox = mask.getbbox()
    if not bbox:
        raise ValueError("The annotated region is empty.")
    area = mask.histogram()[255]
    left, top, right, bottom = bbox
    result = {"source": "operator_mask", "bbox_xyxy_exclusive": list(bbox),
              "projected_area_px2": area, "box_fill_fraction": area / ((right-left)*(bottom-top)),
              "projected_area_cm2": None}
    if reference:
        # Independent check segment, recorded in the same rectified object plane.
        if reference.get("rectified_coplanar") is not True:
            return result
        scale = positive(reference["reference_cm"]) / positive(reference["reference_px"])
        held_out = positive(reference["check_cm"])
        error = abs(positive(reference["check_px"]) * scale - held_out) / held_out
        result["scale_check_relative_error"] = error
        if error <= 0.03:  # provisional engineering threshold
            result.update(projected_area_cm2=area * scale**2, cm_per_pixel=scale)
    return result


def direct_sun(*, inward_m, along_m, height_m, window_left_m, window_right_m,
               sill_m, head_m, sun_elevation_deg, sun_azimuth_deg, outward_normal_deg):
    """Ray from sample point to sun through one vertical rectangular aperture.

    Along-wall coordinate increases clockwise from the outward normal in plan.
    Angles clockwise from true north. No external obstructions, frames or glazing.
    """
    args = locals().copy()
    if any(type(v) not in (int, float) or not isfinite(v) for v in args.values()):
        raise ValueError("Expected finite metric coordinates and angles.")
    if inward_m <= 0 or height_m < 0 or not 0 <= sill_m < head_m or window_left_m >= window_right_m:
        raise ValueError("Invalid point or aperture.")
    if not -90 <= sun_elevation_deg <= 90:
        raise ValueError("Invalid solar elevation.")
    angle = radians((sun_azimuth_deg - outward_normal_deg) % 360)
    faces_sun = cos(angle) > 1e-8 and 0 < sun_elevation_deg < 90
    hit = False
    if faces_sun:
        travel = inward_m / cos(angle)
        x = along_m + travel * sin(angle)
        z = height_m + travel * tan(radians(sun_elevation_deg))
        hit = window_left_m <= x <= window_right_m and sill_m <= z <= head_m
    return {"source": "geometric_simulation", "unobstructed_direct_ray": hit,
            "illuminance_lux": None, "model": "single_vertical_aperture_v1"}
