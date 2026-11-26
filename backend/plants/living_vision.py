"""Offline RGB research baselines. Pixel features are not biological diagnoses."""
from io import BytesIO
from math import isfinite

from PIL import Image, ImageOps

from .vision_evidence import inspect_photo, MAX_BYTES, MAX_PIXELS


def _open(data, formats):
    if not isinstance(data, bytes) or not data or len(data) > MAX_BYTES:
        raise ValueError("Invalid image bytes.")
    try:
        with Image.open(BytesIO(data)) as source:
            if source.format not in formats or source.width * source.height > MAX_PIXELS or getattr(source, "n_frames", 1) != 1:
                raise ValueError("Unsupported image.")
            return ImageOps.exif_transpose(source).copy()
    except (OSError, Image.DecompressionBombError) as exc:
        raise ValueError("Unreadable image.") from exc


def _category(h, s, v):
    # Pillow HSV channels 0..255. Uncalibrated, deliberately conservative bins.
    if v < 45 or s < 45:
        return "other"
    if 45 <= h <= 115:
        return "green_like"
    if 22 <= h < 45 and v >= 100:
        return "yellow_like"
    if 5 <= h < 30 and v < 135:
        return "brown_like"
    return "other"


def candidate_green_mask(data):
    """Color-only candidate mask, not validated plant/leaf detection."""
    image = _open(data, {"JPEG", "PNG", "WEBP"}).convert("HSV")
    output = Image.new("L", image.size)
    output.putdata([255 if _category(*p) == "green_like" else 0 for p in image.getdata()])
    buffer = BytesIO()
    output.save(buffer, format="PNG")
    return buffer.getvalue()


def region_features(data, mask_data):
    """Describe pixels inside an explicitly supplied region; caller verifies region identity."""
    quality = inspect_photo(data)
    image = _open(data, {"JPEG", "PNG", "WEBP"}).convert("HSV")
    mask = _open(mask_data, {"PNG"}).convert("L")
    if image.size != mask.size:
        raise ValueError("Mask dimensions must match upright image.")
    counts = dict.fromkeys(("green_like", "yellow_like", "brown_like", "other"), 0)
    selected = 0
    for p, m in zip(image.getdata(), mask.getdata()):
        if m >= 128:
            selected += 1
            counts[_category(*p)] += 1
    if not selected:
        raise ValueError("Empty region.")
    return {
        "version": "rgb-region-01",
        "scope": "uncalibrated_pixel_features",
        "quality": quality,
        "selected_pixels": selected,
        "image_fraction": selected / (image.width * image.height),
        "color_fractions": {k: n / selected for k, n in counts.items()},
        "region_identity": "caller_supplied_not_verified",
        "health": "not_assessed",
        "soil_moisture": "not_measured",
    }


def compare_regions(before, after, *, same_plant, comparable_capture):
    """Only produce deltas when caller asserts identity and matched capture protocol."""
    if same_plant is not True or comparable_capture is not True:
        return {"status": "not_comparable", "reason": "identity_or_capture_unverified"}
    if any(x.get("version") != "rgb-region-01" or x.get("quality", {}).get("status") != "no_quality_flags" for x in (before, after)):
        return {"status": "not_comparable", "reason": "quality_or_version"}
    keys = ("green_like", "yellow_like", "brown_like", "other")
    values = []
    for x in (before, after):
        fractions = x.get("color_fractions", {})
        if set(fractions) != set(keys) or any(type(v) not in (int, float) or not isfinite(v) or not 0 <= v <= 1 for v in fractions.values()) or abs(sum(fractions.values()) - 1) > 1e-6:
            raise ValueError("Invalid region fractions.")
        values.append(fractions)
    return {
        "status": "comparable_by_report",
        "color_delta_fraction": {k: values[1][k] - values[0][k] for k in keys},
        "growth": "not_measured",
        "cause": "not_inferred",
    }
