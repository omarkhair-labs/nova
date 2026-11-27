"""Opt-in local MagicTouch point segmentation. Not botanical identification."""
from functools import lru_cache
from hashlib import sha256
from importlib.util import find_spec
from pathlib import Path
import os
import platform
from threading import Lock
import subprocess
from tempfile import TemporaryDirectory

from PIL import Image

from .living_segmentation import MAX_SIDE, NoOutline, VisionUnavailable, _outline_from_labels, vision_available

METHOD = "reviewed-magic-touch-point-v1"
MODEL_SHA256 = "e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25"
MODEL_BYTES = 6227884
RUNTIME_VERSION = "0.10.32"
NEURAL_LOCK = Lock()
ARM_PYTHON = Path("/opt/plantupia-neural/bin/python")


def arm_neural_python():
    if (platform.machine() == "aarch64" and
            os.environ.get("PLANTUPIA_NEURAL_RUNTIME_VERSION") == "0.10.18" and
            os.environ.get("PLANTUPIA_ARM_NEURAL_PYTHON") == str(ARM_PYTHON)):
        return ARM_PYTHON
    return None


def neural_outline_available(model_path):
    if not model_path or not vision_available():
        return False
    try:
        path = Path(model_path)
        arm = arm_neural_python()
        runtime_ok = arm.is_file() and os.access(arm, os.X_OK) if arm else find_spec("mediapipe") is not None
        return path.is_file() and path.stat().st_size == MODEL_BYTES and runtime_ok
    except (OSError, ValueError, ImportError):
        return False


@lru_cache(maxsize=1)
def load_neural_engine(model_path):
    try:
        with Path(model_path).open("rb") as handle:
            body = handle.read(MODEL_BYTES + 1)
        if len(body) != MODEL_BYTES or sha256(body).hexdigest() != MODEL_SHA256:
            raise VisionUnavailable("The configured neural model does not match the reviewed artifact.")
        import mediapipe as mp
        if mp.__version__ != RUNTIME_VERSION:
            raise VisionUnavailable("The configured neural runtime has not been reviewed.")
        vision = mp.tasks.vision
        options = vision.InteractiveSegmenterOptions(
            base_options=mp.tasks.BaseOptions(model_asset_buffer=body),
            output_confidence_masks=True, output_category_mask=False)
        return vision.InteractiveSegmenter.create_from_options(options)
    except VisionUnavailable:
        raise
    except (OSError, ImportError, RuntimeError, ValueError, AttributeError) as exc:
        raise VisionUnavailable("The neural outline engine is unavailable.") from exc


def neural_mask(rgb, seed, engine):
    """Use the foreground float mask, not the legacy category-mask polarity."""
    import mediapipe as mp
    import numpy as np
    from mediapipe.tasks.python.components.containers.keypoint import NormalizedKeypoint
    h, w = rgb.shape[:2]
    x, y = seed
    if rgb.shape != (h,w,3) or not (0 <= x < w and 0 <= y < h):
        raise ValueError("Invalid neural outline image or point.")
    try:
        roi_class = mp.tasks.vision.InteractiveSegmenterRegionOfInterest
        roi = roi_class(format=roi_class.Format.KEYPOINT,
                        keypoint=NormalizedKeypoint(x=(x+.5)/w, y=(y+.5)/h))
        result = engine.segment(mp.Image(image_format=mp.ImageFormat.SRGB,
                                         data=np.ascontiguousarray(rgb, dtype=np.uint8)), roi)
        if result.confidence_masks is None or len(result.confidence_masks) != 1:
            raise VisionUnavailable("Unexpected neural mask output.")
        confidence = result.confidence_masks[0].numpy_view().squeeze().copy()
        if (confidence.shape != (h,w) or not np.isfinite(confidence).all()
                or confidence.min() < 0 or confidence.max() > 1):
            raise VisionUnavailable("Unexpected neural mask geometry.")
        return (confidence >= .5).astype(np.uint8)
    except VisionUnavailable:
        raise
    except (RuntimeError, ValueError, AttributeError, OSError) as exc:
        raise VisionUnavailable("The neural outline could not be computed.") from exc


def isolated_arm_mask(rgb, seed, model_path, python):
    """Private bounded ndarray exchange; no server dependency changes or network calls."""
    import numpy as np
    script = Path(__file__).with_name("arm_neural_worker.py")
    try:
        with TemporaryDirectory(prefix="plantupia-neural-") as root:
            source = Path(root) / "rgb.npy"
            target = Path(root) / "mask.npy"
            np.save(source, rgb, allow_pickle=False)
            # The worker receives only local file paths and a minimal environment,
            # not the Django, database, or provider credentials.
            subprocess.run([str(python), str(script), str(model_path), str(source), str(target),
                            str(seed[0]), str(seed[1])], timeout=25, check=True,
                           env={"PATH": "/usr/local/bin:/usr/bin:/bin", "HOME": root,
                                "MPLCONFIGDIR": root, "OPENBLAS_NUM_THREADS": "1", "OMP_NUM_THREADS": "1",
                                "PYTHONDONTWRITEBYTECODE": "1"},
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if target.stat().st_size > MAX_SIDE * MAX_SIDE + 1024:
                raise VisionUnavailable("Unexpected neural mask size.")
            mask = np.load(target, allow_pickle=False)
            if mask.shape != rgb.shape[:2] or mask.dtype != np.uint8 or not np.isin(mask, (0, 1)).all():
                raise VisionUnavailable("Unexpected neural mask geometry.")
            return mask
    except (OSError, ValueError, subprocess.SubprocessError) as exc:
        raise VisionUnavailable("The neural outline could not be computed. Try the standard outline.") from exc


def propose_neural_outline(image, foreground, background, model_path):
    """First positive mark guides the network; other marks verify the polygon."""
    if not vision_available():
        raise VisionUnavailable("The outline geometry engine is unavailable.")
    import numpy as np
    width, height = image.size
    if min(width,height) < 24 or not (1 <= len(foreground) <= 6 and 1 <= len(background) <= 6):
        raise ValueError("Use a readable photograph and included/excluded points.")
    for point in list(foreground)+list(background):
        if len(point) != 2 or any(type(v) is not int for v in point) or not (0 <= point[0] < width and 0 <= point[1] < height):
            raise ValueError("Points must lie on the original photograph.")
    scale = min(1., MAX_SIDE/max(width,height))
    w, h = round(width*scale), round(height*scale)
    if min(w,h) < 24:
        raise NoOutline("The image is too narrow for a neural outline.")

    def mapped(points):
        return [(round(x*(w-1)/(width-1)), round(y*(h-1)/(height-1))) for x,y in points]

    fg, bg = mapped(foreground), mapped(background)
    if any((a[0]-b[0])**2+(a[1]-b[1])**2 < 49 for a in fg for b in bg):
        raise NoOutline("Move included and excluded points farther apart.")
    rgb = np.asarray(image.convert("RGB").resize((w,h), Image.Resampling.BOX), dtype=np.uint8)
    if not NEURAL_LOCK.acquire(blocking=False):
        raise VisionUnavailable("The neural engine is busy. Try again or use the standard outline.")
    try:
        try:
            arm = arm_neural_python()
            mask = isolated_arm_mask(rgb, fg[0], model_path, arm) if arm else neural_mask(rgb, fg[0], load_neural_engine(model_path))
        except (ImportError, OSError) as exc:
            raise VisionUnavailable("The neural engine is unavailable.") from exc
    finally:
        NEURAL_LOCK.release()
    return _outline_from_labels(mask, width, height, fg, bg)
