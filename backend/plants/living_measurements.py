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


def _sun_slopes(elevation_deg, azimuth_deg, normal_deg):
    angle = radians((azimuth_deg - normal_deg) % 360)
    facing = cos(angle)
    if facing <= 1e-8 or not 0 < elevation_deg < 90:
        return None
    return sin(angle) / facing, tan(radians(elevation_deg)) / facing


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
    slopes = _sun_slopes(sun_elevation_deg, sun_azimuth_deg, outward_normal_deg)
    hit = False
    if slopes:
        lateral, rise = slopes
        x = along_m + inward_m * lateral
        z = height_m + inward_m * rise
        hit = window_left_m <= x <= window_right_m and sill_m <= z <= head_m
    return {"source": "geometric_simulation", "unobstructed_direct_ray": hit,
            "illuminance_lux": None, "model": "single_vertical_aperture_v1"}


def aperture_footprint(*, wall_span, room_depth, height_m, window_left_m, window_right_m,
                       sill_m, head_m, sun_elevation_deg, sun_azimuth_deg, outward_normal_deg):
    """Intersect the admitted beam with a horizontal plane and the rectangular room.

    Returns vertices in (along-wall, inward) order. Uses the same ray slopes as
    direct_sun. Coordinates are simulated metres in the reported room frame.
    """
    slopes = _sun_slopes(sun_elevation_deg, sun_azimuth_deg, outward_normal_deg)
    if slopes is None or height_m >= head_m:
        return []
    lateral, rise = slopes
    near = max(0., (sill_m - height_m) / rise)
    far = min(room_depth, (head_m - height_m) / rise)
    if near >= far:
        return []
    polygon = [
        (window_left_m - near*lateral, near),
        (window_right_m - near*lateral, near),
        (window_right_m - far*lateral, far),
        (window_left_m - far*lateral, far),
    ]
    for boundary, direction in ((0., 1), (wall_span, -1)):
        clipped = []
        for start, end in zip(polygon, polygon[1:] + polygon[:1]):
            inside_start = direction * (start[0] - boundary) >= 0
            inside_end = direction * (end[0] - boundary) >= 0
            if inside_start != inside_end:
                fraction = (boundary - start[0]) / (end[0] - start[0])
                clipped.append((boundary, start[1] + fraction * (end[1] - start[1])))
            if inside_end:
                clipped.append(end)
        polygon = clipped
    return polygon if len(polygon) >= 3 else []
