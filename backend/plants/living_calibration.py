"""Conservative planar image scale from two independently measured references.

This is an operator-confirmed face-on plane, NOT automated rectification, a
3D surface measurement, or a guarantee of biological growth accuracy.
"""
from math import hypot, isfinite


METHOD = "face-on-two-segment-v1"


def planar_reference(reference, width, height, region):
    def rejected(reason):
        return None, {"method": METHOD, "status": "rejected", "reason": reason}

    if reference.get("face_on_coplanar") is not True:
        return rejected("plane_not_confirmed")
    points = reference["points"]
    if (len(points) != 4 or any(len(p) != 2 or any(type(v) is not int for v in p)
            or not (0 <= p[0] < width and 0 <= p[1] < height) for p in points)
            or len(set(map(tuple, points))) != 4):
        raise ValueError("Mark four distinct reference endpoints inside the original photo.")
    a,b,c,d = points
    v1, v2 = (b[0]-a[0], b[1]-a[1]), (d[0]-c[0], d[1]-c[1])
    lengths = (hypot(*v1), hypot(*v2))
    if min(lengths) < max(24., min(width,height)*.04):
        return rejected("reference_too_short")
    if abs(sum(x*y for x,y in zip(v1,v2)) / (lengths[0]*lengths[1])) > .25:
        return rejected("references_not_near_perpendicular")
    # Both references must be local to the region: prevent extrapolation from
    # tiny distant scale marks. This is not proof of physical coplanarity.
    rx, ry = (min(p[i] for p in region) for i in (0,1))
    ex, ey = (max(p[i] for p in region) for i in (0,1))
    span = max(ex-rx, ey-ry, *lengths)
    for x,y in points:
        if hypot(max(rx-x,0,x-ex), max(ry-y,0,y-ey)) > span:
            return rejected("references_too_far_from_region")
    cm1, cm2 = reference["reference_cm"], reference["check_cm"]
    if any(type(v) not in (int,float) or not isfinite(v) or not .001 <= v <= 10000 for v in (cm1,cm2)):
        raise ValueError("Use finite positive known reference lengths.")
    scale = cm1/lengths[0]
    relative_error = abs(lengths[1]*scale-cm2)/cm2
    diagnostic = {"method": METHOD, "status": "accepted" if relative_error <= .03 else "rejected",
                  "scale_check_relative_error": relative_error,
                  "reference_pixel_lengths": list(lengths),
                  "plane_basis": "operator_reported_face_on_coplanar"}
    if relative_error > .03:
        diagnostic["reason"] = "independent_scale_check_failed"
        return None, diagnostic
    return {"rectified_coplanar": True, "reference_cm": cm1, "reference_px": lengths[0],
            "check_cm": cm2, "check_px": lengths[1]}, diagnostic
