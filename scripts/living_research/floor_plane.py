"""Recover positions on a flat photographed floor from independent surveyed controls.

Input JSON has controls and checks, each a list of {pixel:[u,v], metres:[x,y]}.
Targets are floor-contact pixels. All points must use one undistorted upright photo.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np


def floor_positions(controls, checks, targets, tolerance_m=.05):
    if not (4 <= len(controls) <= 64 and 2 <= len(checks) <= 64 and 1 <= len(targets) <= 1000):
        raise ValueError("Supply 4+ floor controls, 2+ separate check marks, and target points.")
    def coordinates(items):
        values = np.asarray(items, dtype=np.float64)
        if values.shape != (len(items), 2) or not np.isfinite(values).all():
            raise ValueError("Expected pairs of finite coordinates.")
        return values
    pixels = coordinates([p["pixel"] for p in controls])
    world = coordinates([p["metres"] for p in controls])
    test = coordinates([p["pixel"] for p in checks])
    observed = coordinates([p["metres"] for p in checks])
    target = coordinates(targets)
    if not np.isfinite(tolerance_m) or not 0 < tolerance_m <= 1:
        raise ValueError("Expected a positive reference tolerance up to one metre.")
    for frame in (np.vstack([pixels, test]), np.vstack([world, observed])):
        if len(np.unique(frame, axis=0)) != len(frame):
            raise ValueError("Each control and independent check must be distinct.")
    matrix, _ = cv2.findHomography(pixels, world, method=0)
    if matrix is None or not np.isfinite(matrix).all() or np.linalg.matrix_rank(matrix) != 3:
        return {"status": "abstained", "reason": "degenerate_controls"}
    hull = cv2.convexHull(pixels.astype(np.float32))
    if any(cv2.pointPolygonTest(hull, tuple(pt), False) < 0 for pt in np.vstack([test, target])):
        return {"status": "abstained", "reason": "outside_surveyed_footprint"}
    def project(points):
        h = np.column_stack((points, np.ones(len(points)))) @ matrix.T
        if np.any(np.abs(h[:, 2]) < 1e-10):
            raise ValueError("Ill-conditioned floor projection.")
        return h[:, :2] / h[:, 2:]
    error = np.linalg.norm(project(test)-observed, axis=1)
    result = {"check_errors_m": error.tolist(), "max_error_m": float(error.max()), "tolerance_m": tolerance_m}
    if error.max() > tolerance_m:
        return {**result, "status": "abstained", "reason": "reference_disagreement"}
    return {**result, "status": "estimated", "source": "surveyed_planar_homography",
            "floor_xy_m": project(target).tolist()}


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("survey", type=Path)
    args = p.parse_args()
    print(json.dumps(floor_positions(**json.loads(args.survey.read_text())), indent=2))
