"""Register comparable front-facing visits using static scene texture and an explicit ROI.

Inputs are local, upright images of the same identified plant, selected by an operator.
Each background mask marks stationary features only. ROI is in before-image coordinates.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np


def register(before, after, before_static, after_static, roi):
    if before.shape != after.shape or any(x.shape != before.shape[:2] for x in (before_static, after_static, roi)):
        raise ValueError("Use the same upright frame dimensions and matched masks.")
    if np.any((before_static > 0) & (roi > 0)) or not np.any(roi):
        raise ValueError("Mark stationary background separately from the plant region.")
    if max(before.shape[:2]) > 2048:
        raise ValueError("Downsample images and masks to a common frame of at most 2048 pixels per side.")
    def abstain(reason):
        return {"status": "not_comparable", "reason": reason}
    gray1 = cv2.cvtColor(before, cv2.COLOR_BGR2GRAY)
    gray2 = cv2.cvtColor(after, cv2.COLOR_BGR2GRAY)
    orb = cv2.ORB_create(nfeatures=2000)
    k1, d1 = orb.detectAndCompute(gray1, before_static)
    k2, d2 = orb.detectAndCompute(gray2, after_static)
    if d1 is None or d2 is None:
        return abstain("insufficient_background_texture")
    matcher = cv2.BFMatcher(cv2.NORM_HAMMING)
    pairs = matcher.knnMatch(d2, d1, k=2)
    matches = sorted((pair[0] for pair in pairs if len(pair) == 2 and pair[0].distance < .75*pair[1].distance),
                     key=lambda m: m.distance)
    unique = {}
    for m in matches:
        unique.setdefault(m.trainIdx, m)
    matches = list(unique.values())
    if len(matches) < 24:
        return abstain("insufficient_unique_matches")
    src = np.float32([k2[m.queryIdx].pt for m in matches])
    dst = np.float32([k1[m.trainIdx].pt for m in matches])
    check = np.arange(len(matches)) % 4 == 0
    transform, inliers = cv2.estimateAffinePartial2D(src[~check], dst[~check], ransacReprojThreshold=2.0)
    if transform is None or inliers.mean() < .70:
        return abstain("unstable_registration")
    height, width = gray1.shape
    hull = cv2.convexHull(dst[~check][inliers.ravel().astype(bool)])
    if cv2.contourArea(hull) < .12 * height * width:
        return abstain("poor_spatial_support")
    supported = np.zeros_like(roi)
    cv2.fillConvexPoly(supported, np.int32(hull), 255)
    if np.mean(supported[roi > 0] > 0) < .95:
        return abstain("region_outside_registration_support")
    predicted = cv2.transform(src[check, None, :], transform)[:, 0, :]
    err = np.linalg.norm(predicted-dst[check], axis=1)
    if np.quantile(err, .75) > 2.5 or not .85 <= np.linalg.norm(transform[0, :2]) <= 1.18:
        return abstain("held_out_error_or_view_scale")
    warp = lambda img, flags: cv2.warpAffine(img, transform, (width, height), flags=flags)
    valid = warp(np.full_like(roi, 255), cv2.INTER_NEAREST) > 0
    selected = roi > 0
    if valid[selected].mean() < .98:
        return abstain("incomplete_region_overlap")
    aligned = warp(after, cv2.INTER_LINEAR)
    overlap = (before_static > 0) & (warp(after_static, cv2.INTER_NEAREST) > 0) & valid
    delta = cv2.absdiff(before, aligned).mean(axis=2)
    if overlap.sum() < 128 or np.median(delta[overlap]) > 18:
        return abstain("background_photometry_changed")
    return {"status": "aligned", "model": "static-orb-similarity-v1", "matrix_after_to_before": transform.tolist(),
            "test_matches": int(check.sum()), "test_p75_error_px": float(np.quantile(err, .75)),
            "mean_absolute_rgb_difference_roi": float(delta[selected & valid].mean()),
            "growth": None, "symptom_progression": None}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("before", "after", "before_static", "after_static", "roi"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    frames = [cv2.imread(str(path), cv2.IMREAD_COLOR if i < 2 else cv2.IMREAD_GRAYSCALE)
              for i, path in enumerate(vars(args).values())]
    if any(img is None for img in frames):
        parser.error("Could not read one of the supplied frames.")
    print(json.dumps(register(*frames), indent=2))
