"""Private, operator-prompted visual registration; no physical growth inference.

One bounded, reciprocal ORB similarity fit uses only background declared
stationary by the operator. The research CLI imports this same implementation.
"""
from math import isfinite
from threading import Lock

from PIL import Image

from .living_segmentation import VisionUnavailable, vision_available

METHOD = "static-orb-similarity-v1"
ALIGNMENT_LOCK = Lock()
MAX_SIDE = 768


def checked_box(box):
    """Normalised corners in the upright image grid, not display coordinates."""
    if (len(box) != 4 or any(type(v) not in (float, int) or not isfinite(v) or not 0 <= v <= 1 for v in box)):
        raise ValueError("Mark a rectangle within each photograph.")
    left, top, right, bottom = box
    if right-left < .10 or bottom-top < .10 or not .025 <= (right-left)*(bottom-top) <= .70:
        raise ValueError("Mark the moving plant area and leave enough fixed background.")
    return left, top, right, bottom


def register(before, after, before_static, after_static, roi):
    import cv2
    import numpy as np
    if before.ndim != 3 or before.shape[-1] != 3 or before.dtype != np.uint8 or after.dtype != np.uint8:
        raise ValueError("Use bounded three-channel uint8 photographs.")
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
    # Keep descriptors off moving foliage and its surrounding patch support.
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (33, 33))
    mask1 = cv2.erode(before_static, kernel)
    mask2 = cv2.erode(after_static, kernel)
    k1, d1 = orb.detectAndCompute(gray1, mask1)
    k2, d2 = orb.detectAndCompute(gray2, mask2)
    if d1 is None or d2 is None:
        return abstain("insufficient_background_texture")
    matcher = cv2.BFMatcher(cv2.NORM_HAMMING)
    def ratio_matches(first, second):
        return [p[0] for p in matcher.knnMatch(first, second, k=2)
                if len(p) == 2 and p[0].distance < .75*p[1].distance]
    reverse = {m.queryIdx: m.trainIdx for m in ratio_matches(d1, d2)}
    matches = sorted((m for m in ratio_matches(d2, d1) if reverse.get(m.trainIdx) == m.queryIdx),
                     key=lambda m: (m.distance, m.queryIdx, m.trainIdx))
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
    if transform is None or inliers is None or not np.isfinite(transform).all() or inliers.mean() < .70:
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
    if (np.quantile(err, .75) > 2.5 or not .85 <= np.linalg.norm(transform[0, :2]) <= 1.18
            or abs(np.degrees(np.arctan2(transform[1, 0], transform[0, 0]))) > 15):
        return abstain("held_out_error_or_view_scale")
    centre = np.array([width/2., height/2.])
    shift = (transform[:, :2] @ centre + transform[:, 2] - centre) / width
    if np.max(np.abs(shift)) > .25:
        return abstain("excessive_view_translation")
    warp = lambda img, flags: cv2.warpAffine(img, transform, (width, height), flags=flags)
    valid = warp(np.full_like(roi, 255), cv2.INTER_NEAREST) > 0
    selected = roi > 0
    if valid[selected].mean() < .98:
        return abstain("incomplete_region_overlap")
    aligned = warp(after, cv2.INTER_LINEAR)
    overlap = (before_static > 0) & (warp(after_static, cv2.INTER_NEAREST) > 0) & valid
    delta = cv2.absdiff(before, aligned).mean(axis=2)
    if overlap.sum() < .15 * width * height or np.median(delta[overlap]) > 18 or np.quantile(delta[overlap], .75) > 35:
        return abstain("background_photometry_changed")
    return {"status": "aligned", "model": "static-orb-similarity-v1", "matrix_after_to_before": transform.tolist(),
            "test_matches": int(check.sum()), "test_p75_error_px": float(np.quantile(err, .75)),
            "mean_absolute_rgb_difference_roi": float(delta[selected & valid].mean()),
            "growth": None, "symptom_progression": None}


def align_retained_photos(before, after, before_box, after_box):
    """Return a temporary image-width-normalised similarity transform.

    Both retained photos have already passed the bounded upright image loader.
    The caller verifies plant identity, chronology, access and distinct bytes.
    """
    boxes = [checked_box(before_box), checked_box(after_box)]
    if not vision_available():
        raise VisionUnavailable("The local alignment engine is not installed.")
    import cv2
    import numpy as np

    def abstain(reason):
        return {"status": "not_comparable", "reason": reason,
                "growth": "not_inferred", "health": "not_assessed"}

    if min(*before.size, *after.size) < 240:
        return abstain("insufficient_image_resolution")
    ratios = [im.width/im.height for im in (before, after)]
    if not .45 <= ratios[0] <= 2.25 or abs(ratios[1]/ratios[0]-1) > .005:
        return abstain("different_frame_geometry")
    scale = min(1., MAX_SIDE / max(before.size), MAX_SIDE / max(after.size))
    w, h = round(before.width*scale), round(before.height*scale)
    # Match the earlier image geometry without unnecessarily upsampling a
    # lower-resolution second photo.
    if w > after.width or h > after.height:
        factor = min(after.width/w, after.height/h)
        w, h = round(w*factor), round(h*factor)
    if min(w,h) < 240:
        return abstain("insufficient_image_resolution")

    def exclusion(box):
        x0, y0, x1, y1 = box
        mask = np.zeros((h,w), np.uint8)
        mask[int(y0*h):min(h, int(np.ceil(y1*h))),
             int(x0*w):min(w, int(np.ceil(x1*w)))] = 255
        return mask

    if not ALIGNMENT_LOCK.acquire(blocking=False):
        raise VisionUnavailable("The alignment engine is busy; try again or use reference marks.")
    try:
        frames = [cv2.cvtColor(np.asarray(im.resize((w,h), Image.Resampling.BILINEAR).convert("RGB")),
                               cv2.COLOR_RGB2BGR) for im in (before, after)]
        moving = [exclusion(box) for box in boxes]
        result = register(*frames, 255-moving[0], 255-moving[1], moving[0])
    except cv2.error as exc:
        raise VisionUnavailable("The local alignment could not be computed.") from exc
    finally:
        ALIGNMENT_LOCK.release()
    if result["status"] != "aligned":
        return abstain(result["reason"])
    matrix = np.array(result["matrix_after_to_before"])
    centre = np.array([w/2., h/2.])
    shift = (matrix[:, :2] @ centre + matrix[:, 2] - centre) / w
    return {"status": "aligned", "model": METHOD,
            "basis": "operator_marked_moving_regions",
            "frame_width": w, "frame_height": h,
            "transform": {"a": float(matrix[0,0]), "b": float(matrix[1,0]),
                          "x": float(shift[0]), "y": float(shift[1])},
            "test_matches": result["test_matches"],
            "test_p75_error_px": result["test_p75_error_px"],
            "growth": "not_inferred", "health": "not_assessed"}
