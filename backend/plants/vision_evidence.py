"""Offline photo evidence research; no diagnosis, network, database or API writes."""
from io import BytesIO

from PIL import Image, ImageFilter, ImageOps, ImageStat

MAX_BYTES = 10 * 1024 * 1024
MAX_PIXELS = 20_000_000
POLICY_VERSION = "capture-quality-research-01"


def inspect_photo(data: bytes) -> dict:
    """Report reproducible image-level proxies, not plant or soil measurements.

    Thresholds are provisional engineering defaults requiring real-world calibration.
    A photo passing this gate is not proof that it contains a plant.
    """
    if not isinstance(data, bytes) or not data or len(data) > MAX_BYTES:
        raise ValueError("Invalid photo size.")
    try:
        with Image.open(BytesIO(data)) as source:
            if source.format not in {"JPEG", "PNG", "WEBP"}:
                raise ValueError("Unsupported photo format.")
            if source.width * source.height > MAX_PIXELS:
                raise ValueError("Photo resolution exceeds limit.")
            if getattr(source, "n_frames", 1) != 1:
                raise ValueError("Animated photos are not supported.")
            upright = ImageOps.exif_transpose(source)
            width, height = upright.size
            gray = upright.convert("L")
            gray.thumbnail((512, 512))
            histogram = gray.histogram()
            count = gray.width * gray.height
            dark = sum(histogram[:24]) / count
            bright = sum(histogram[232:]) / count
            contrast = ImageStat.Stat(gray).stddev[0]
            # Edge-energy proxy; scene texture and background affect this value.
            edges = gray.filter(ImageFilter.FIND_EDGES)
            if gray.width > 2 and gray.height > 2:
                edges = edges.crop((1, 1, gray.width - 1, gray.height - 1))
            edge_mean = ImageStat.Stat(edges).mean[0]
    except (OSError, Image.DecompressionBombError) as exc:
        raise ValueError("Unreadable photo.") from exc

    flags = []
    if min(width, height) < 480:
        flags.append("low_resolution")
    if dark > 0.65:
        flags.append("predominantly_dark")
    if bright > 0.65:
        flags.append("predominantly_bright")
    if contrast < 12 or edge_mean < 3:
        flags.append("low_detail_proxy")

    return {
        "policy_version": POLICY_VERSION,
        "scope": "image_quality_only",
        "width": width,
        "height": height,
        "metrics": {
            "dark_pixel_fraction": round(dark, 4),
            "bright_pixel_fraction": round(bright, 4),
            "luminance_stddev": round(contrast, 3),
            "edge_mean": round(edge_mean, 3),
        },
        "capture_flags": flags,
        "status": "review_capture" if flags else "no_quality_flags",
        "plant_presence": "not_assessed",
        "health": "not_assessed",
        "soil_moisture": "not_measured",
        "environmental_illuminance": "not_measured",
    }
