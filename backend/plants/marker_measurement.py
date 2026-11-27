"""Rectify a tagged flat measurement plane using two independently detected squares."""
from math import isfinite

METHOD = "aruco-pair-4x4-50-v1"


def measure_markers(image, vertices, edge_cm):
    def reject(reason):
        return {"method": METHOD, "status": "rejected", "reason": reason}
    try:
        import cv2
        import numpy as np
        aruco = cv2.aruco
    except (ImportError, AttributeError):
        return reject("marker_detector_unavailable")
    gray = image.convert("L")
    gray.thumbnail((1800, 1800))
    sx, sy = gray.width / image.width, gray.height / image.height
    detector = aruco.ArucoDetector(aruco.getPredefinedDictionary(aruco.DICT_4X4_50))
    corners, ids, _ = detector.detectMarkers(np.asarray(gray))
    if ids is None or len(ids) != 2 or int(ids[0, 0]) == int(ids[1, 0]):
        return reject("two_distinct_markers_required")
    squares = [c.reshape(4, 2) for c in corners]
    region = np.array(vertices, dtype=np.float32) * np.float32([sx, sy])
    center = region.mean(axis=0)
    for square in squares:
        sides = np.linalg.norm(square - np.roll(square, 1, axis=0), axis=1)
        if min(sides) < 24 or min(sides) / max(sides) < .55:
            return reject("marker_too_small_or_oblique")
        if np.max(np.linalg.norm(region - square.mean(axis=0), axis=1)) > min(sides) * 8:
            return reject("references_too_far_from_region")
        if cv2.pointPolygonTest(region, tuple(map(float, square.mean(axis=0))), False) >= 0:
            return reject("marker_overlaps_region")
    if np.dot(squares[0].mean(axis=0)-center, squares[1].mean(axis=0)-center) >= 0:
        return reject("place_markers_on_opposite_sides")
    target = np.float32([[0, 0], [edge_cm, 0], [edge_cm, edge_cm], [0, edge_cm]])
    areas, errors = [], []
    for i, square in enumerate(squares):
        transform = cv2.getPerspectiveTransform(square, target)
        mapped = cv2.perspectiveTransform(region[None], transform)[0]
        check = cv2.perspectiveTransform(squares[1-i][None], transform)[0]
        lengths = np.linalg.norm(check - np.roll(check, 1, axis=0), axis=1)
        errors.append(float(np.max(np.abs(lengths / edge_cm - 1))))
        areas.append(cv2.contourArea(mapped))
    if not all(isfinite(v) and v > 0 for v in areas):
        return reject("invalid_rectification")
    error = max(*errors, abs(areas[0]-areas[1])/max(areas))
    if not isfinite(error) or error > .03:
        return reject("independent_scale_check_failed")
    return {"method": METHOD, "status": "accepted", "plane_basis": "reported_flat_tagged_part",
            "ids": sorted(int(i) for i in ids.flat), "edge_cm": edge_cm,
            "scale_check_relative_error": error, "projected_area_cm2": sum(areas)/2}
