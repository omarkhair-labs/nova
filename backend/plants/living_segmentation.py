"""Bounded interactive outline proposal. A mask is not a botanical identification."""
from math import hypot
from threading import Lock

METHOD = "reviewed-grabcut-points-v1"
AUTO_METHOD = "reviewed-grabcut-frame-v1"
MAX_SIDE = 384
ENGINE_LOCK = Lock()


class NoOutline(ValueError):
    """Valid input, but one faithful simple outline could not be proposed."""


class VisionUnavailable(RuntimeError):
    pass


def vision_available():
    try:
        import cv2
        import numpy
        return hasattr(cv2, "grabCut") and hasattr(numpy, "asarray")
    except (ImportError, OSError):
        return False


def propose_outline(image, foreground, background):
    """Input: upright PIL image and 1-6 foreground/background points in its pixel grid."""
    if not vision_available():
        raise VisionUnavailable("The local vision engine is not installed.")
    import cv2
    import numpy as np

    width, height = image.size
    if min(width, height) < 24 or not (1 <= len(foreground) <= 6 and 1 <= len(background) <= 6):
        raise ValueError("Use a readable photograph and 1 to 6 points of each kind.")
    all_points = list(foreground) + list(background)
    if any(len(p) != 2 or any(type(v) is not int for v in p) or
           not (0 <= p[0] < width and 0 <= p[1] < height) for p in all_points):
        raise ValueError("Points must lie in the original upright photograph.")
    scale = min(1., MAX_SIDE / max(width, height))
    w, h = max(1, round(width * scale)), max(1, round(height * scale))
    if min(w, h) < 16:
        raise NoOutline("The photo is too narrow for a stable outline.")

    def mapped(points):
        return [(round(x * (w-1) / (width-1)), round(y * (h-1) / (height-1))) for x, y in points]

    fg, bg = mapped(foreground), mapped(background)
    if any(hypot(a[0]-b[0], a[1]-b[1]) < 7 for a in fg for b in bg):
        raise NoOutline("Move the included and excluded points farther apart.")
    rgb = np.asarray(image.convert("RGB").resize((w, h)), dtype=np.uint8)
    lab = cv2.cvtColor(rgb, cv2.COLOR_RGB2LAB).astype(np.float32)
    yy, xx = np.ogrid[:h, :w]

    def scores(points):
        colors = []
        cost = np.full((h, w), np.inf, dtype=np.float32)
        for x, y in points:
            sample = np.median(lab[max(0,y-1):y+2, max(0,x-1):x+2], axis=(0,1))
            colors.append(sample)
            chroma = np.sum((lab - sample)**2, axis=2)
            spatial = ((xx-x)**2 + (yy-y)**2) * (18. / max(w,h))**2
            cost = np.minimum(cost, chroma + spatial)
        return cost, colors

    fcost, fcolors = scores(fg)
    bcost, bcolors = scores(bg)
    if max(float(np.linalg.norm(a-b)) for a in fcolors for b in bcolors) < 9.:
        raise NoOutline("Those points look too similar. Mark a clearer edge or trace manually.")
    labels = np.where(fcost < bcost, cv2.GC_PR_FGD, cv2.GC_PR_BGD).astype(np.uint8)
    if min(np.count_nonzero(labels == cv2.GC_PR_FGD), np.count_nonzero(labels == cv2.GC_PR_BGD)) < 10:
        raise NoOutline("Not enough distinct image regions for a proposal.")
    for pts, label in ((fg, cv2.GC_FGD), (bg, cv2.GC_BGD)):
        for x, y in pts:
            cv2.circle(labels, (x,y), 2, label, -1)
    if not ENGINE_LOCK.acquire(blocking=False):
        raise VisionUnavailable("The local vision engine is busy. Try again or trace manually.")
    try:
        cv2.setRNGSeed(42)
        cv2.grabCut(rgb, labels, None, None, None, 2, cv2.GC_INIT_WITH_MASK)
    except cv2.error as exc:
        raise NoOutline("A reliable outline could not be proposed.") from exc
    finally:
        ENGINE_LOCK.release()
    return _outline_from_labels(labels, width, height, fg, bg)


def propose_frame_outline(image):
    """Fit one foreground proposal, abstaining on clutter or low contrast."""
    if not vision_available():
        raise VisionUnavailable("The local vision engine is not installed.")
    import cv2
    import numpy as np

    width, height = image.size
    scale = min(1., MAX_SIDE / max(width, height))
    w, h = round(width * scale), round(height * scale)
    if min(w, h) < 24:
        raise NoOutline("The photograph is too narrow for an automatic outline.")
    rgb = np.asarray(image.convert("RGB").resize((w, h)), dtype=np.uint8)
    if max(np.std(rgb, axis=(0, 1))) < 7:
        raise NoOutline("Too little contrast. Mark the part on the photograph.")
    x, y = max(1, w // 20), max(1, h // 20)
    labels = np.zeros((h, w), dtype=np.uint8)
    if not ENGINE_LOCK.acquire(blocking=False):
        raise VisionUnavailable("The local vision engine is busy. Try again.")
    try:
        cv2.setRNGSeed(42)
        cv2.grabCut(rgb, labels, (x, y, w-2*x, h-2*y), None, None, 3, cv2.GC_INIT_WITH_RECT)
    except cv2.error as exc:
        raise NoOutline("Mark the part to help find its boundary.") from exc
    finally:
        ENGINE_LOCK.release()
    return _outline_from_labels(labels, width, height, [], [])


def _outline_from_labels(labels, width, height, fg, bg):
    import cv2
    import numpy as np

    h, w = labels.shape
    mask = ((labels & 1) == 1).astype(np.uint8)
    _, components, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    ids = {int(components[y,x]) for x,y in fg}
    if not fg:
        areas = stats[1:, cv2.CC_STAT_AREA]
        if not areas.size or areas.max() < .85 * areas.sum():
            raise NoOutline("Several regions or no clear foreground. Mark one part to continue.")
        ids = {1 + int(areas.argmax())}
    if len(ids) != 1 or 0 in ids:
        raise NoOutline("Mark one connected visible part at a time.")
    selected = next(iter(ids))
    component = (components == selected).astype(np.uint8)
    area = int(stats[selected, cv2.CC_STAT_AREA])
    if not max(24, w*h*.001) <= area <= w*h*.92:
        raise NoOutline("The proposed part is too small or too broad. Try different points.")
    contours, _ = cv2.findContours(component, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if len(contours) != 1:
        raise NoOutline("The proposed part is not one simple outline.")
    perimeter = cv2.arcLength(contours[0], True)
    for fraction in (.001, .002, .004, .008, .015, .025):
        approx = cv2.approxPolyDP(contours[0], max(.55, fraction * perimeter), True)
        if not 3 <= len(approx) <= 64:
            continue
        polygon = approx[:,0,:]
        rendered = np.zeros_like(component)
        cv2.fillPoly(rendered, [polygon], 1)
        intersection = int(np.count_nonzero(rendered & component))
        union = int(np.count_nonzero(rendered | component))
        if not union or intersection / union < .92:
            continue
        if any(not rendered[y,x] for x,y in fg) or any(rendered[y,x] for x,y in bg):
            continue
        points = [[round(int(x)*(width-1)/(w-1)), round(int(y)*(height-1)/(h-1))] for x,y in polygon]
        if len(set(map(tuple, points))) != len(points):
            continue
        return points
    raise NoOutline("The selected region has holes or fine boundaries that cannot be represented faithfully. Trace manually.")
