"""Measure explicitly traced visible regions in the upright source image."""
from io import BytesIO
from math import hypot

from PIL import Image, ImageDraw

from .living_vision import _category, _open, region_features
from .living_measurements import projected_region
from .living_calibration import planar_reference


def _orientation(a, b, c):
    return (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0])


def _crosses(a, b, c, d):
    if any(max(a[k], b[k]) < min(c[k], d[k]) or max(c[k], d[k]) < min(a[k], b[k]) for k in (0, 1)):
        return False
    return _orientation(a, b, c)*_orientation(a, b, d) <= 0 and _orientation(c, d, a)*_orientation(c, d, b) <= 0


def polygon(points, width, height):
    if not 3 <= len(points) <= 64:
        raise ValueError("Trace each region with 3 to 64 vertices.")
    if any(len(p) != 2 or not all(type(n) is int for n in p) or not (0 <= p[0] < width and 0 <= p[1] < height)
           for p in points):
        raise ValueError("Trace coordinates must lie in the upright image.")
    if len(set(map(tuple, points))) != len(points):
        raise ValueError("Trace each vertex once.")
    edges = list(zip(points, points[1:] + points[:1]))
    if any(_crosses(a,b,c,d) for i,(a,b) in enumerate(edges) for j,(c,d) in enumerate(edges)
           if j > i+1 and (i,j) != (0,len(edges)-1)):
        raise ValueError("The trace crosses itself.")
    if abs(sum(a[0]*b[1]-a[1]*b[0] for a,b in edges)) < 1:
        raise ValueError("The trace encloses no area.")
    return [tuple(p) for p in points]


def visible_patches(crop, mask, box):
    """Connected colour changes inside the selected outline, in original upright coordinates."""
    from collections import deque
    crop = crop.copy()
    crop.thumbnail((144, 144))
    allowed = mask.resize(crop.size, Image.Resampling.NEAREST)
    width, height = crop.size
    labels = [(_category(*pixel) if inside else "") for pixel, inside in
              zip(crop.convert("HSV").getdata(), allowed.getdata())]
    total = sum(bool(label) for label in labels)
    patches = []
    for index, label in enumerate(labels):
        if label not in ("yellow_like", "brown_like"):
            continue
        labels[index] = ""
        stack = deque([index])
        xs, ys = [], []
        while stack:
            i = stack.pop()
            x, y = i % width, i // width
            xs.append(x)
            ys.append(y)
            for nx, ny in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
                j = ny * width + nx
                if 0 <= nx < width and 0 <= ny < height and labels[j] == label:
                    labels[j] = ""
                    stack.append(j)
        # Broad colour fields and isolated noise do not describe a local patch.
        fraction = len(xs) / total
        if len(xs) >= 8 and .004 <= fraction <= .45:
            sx, sy = (box[2] - box[0]) / width, (box[3] - box[1]) / height
            patches.append({"appearance": label, "region_fraction": round(fraction, 4),
                            "bounds": [box[0] + int(min(xs)*sx), box[1] + int(min(ys)*sy),
                                       min(box[2]-1, box[0]+int((max(xs)+1)*sx)),
                                       min(box[3]-1, box[1]+int((max(ys)+1)*sy))]})
    return sorted(patches, key=lambda p: -p["region_fraction"])[:8]


def measure_trace(data, points, reference=None, point_reference=None, marker_edge_cm=None):
    image = _open(data, {"JPEG", "PNG", "WEBP"})
    shape = polygon(points, image.width, image.height)
    # Rasterize the original grid for pixel metrics, then sample only bounded RGB for colour.
    mask = Image.new("L", image.size)
    ImageDraw.Draw(mask).polygon(shape, fill=255)
    buf = BytesIO()
    mask.save(buf, "PNG")
    calibration = None
    if point_reference is not None:
        reference, calibration = planar_reference(point_reference, image.width, image.height, shape)
    geometry = projected_region(buf.getvalue(), reference=reference)
    if marker_edge_cm is not None:
        from .marker_measurement import measure_markers
        calibration = measure_markers(image, points, marker_edge_cm)
        geometry["projected_area_cm2"] = calibration.get("projected_area_cm2")
    if calibration is not None:
        geometry["calibration"] = calibration
    geometry["traced_outline_length_px"] = round(sum(
        hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(shape, shape[1:] + shape[:1])
    ), 2)
    box = mask.getbbox()
    crop = image.crop(box).convert("RGB")
    crop.thumbnail((384, 384))
    sample = mask.crop(box).resize(crop.size, Image.Resampling.NEAREST)
    if not sample.getbbox():
        return {"geometry": geometry, "color_fractions": None}
    photo, bitmap = BytesIO(), BytesIO()
    crop.save(photo, "PNG")
    sample.save(bitmap, "PNG")
    features = region_features(photo.getvalue(), bitmap.getvalue())
    return {"geometry": geometry, "color_fractions": features["color_fractions"],
            "visible_patches": visible_patches(crop, sample, box)}
