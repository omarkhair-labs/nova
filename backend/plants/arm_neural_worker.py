"""Standalone, opt-in ARM MagicTouch runtime. No Django imports or credentials."""
from hashlib import sha256
from pathlib import Path
import sys

import mediapipe as mp
import numpy as np
from mediapipe.tasks.python.components.containers.keypoint import NormalizedKeypoint

MODEL_BYTES = 6227884
MODEL_SHA256 = "e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25"


def infer(model, rgb, x, y):
    if mp.__version__ != "0.10.18":
        raise ValueError("Unexpected ARM neural runtime.")
    with Path(model).open("rb") as handle:
        body = handle.read(MODEL_BYTES + 1)
    if len(body) != MODEL_BYTES or sha256(body).hexdigest() != MODEL_SHA256:
        raise ValueError("Unexpected neural model.")
    if rgb.ndim != 3 or rgb.dtype != np.uint8:
        raise ValueError("Expected RGB byte array.")
    h, w, channels = rgb.shape
    if channels != 3 or not (24 <= h <= 384 and 24 <= w <= 384 and 0 <= x < w and 0 <= y < h):
        raise ValueError("Unexpected image or point.")
    vision = mp.tasks.vision
    opts = vision.InteractiveSegmenterOptions(
        base_options=mp.tasks.BaseOptions(model_asset_buffer=body),
        output_confidence_masks=True, output_category_mask=False)
    roi_class = vision.InteractiveSegmenterRegionOfInterest
    roi = roi_class(format=roi_class.Format.KEYPOINT,
                    keypoint=NormalizedKeypoint(x=(x+.5)/w, y=(y+.5)/h))
    with vision.InteractiveSegmenter.create_from_options(opts) as model:
        result = model.segment(mp.Image(image_format=mp.ImageFormat.SRGB,
                                        data=np.ascontiguousarray(rgb)), roi)
        if result.confidence_masks is None or len(result.confidence_masks) != 1:
            raise ValueError("Unexpected neural output.")
        confidence = result.confidence_masks[0].numpy_view().squeeze().copy()
    if confidence.shape != (h, w) or not np.isfinite(confidence).all() or confidence.min() < 0 or confidence.max() > 1:
        raise ValueError("Unexpected confidence geometry.")
    return (confidence >= .5).astype(np.uint8)


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "--check":
        rgb = np.zeros((128, 128, 3), dtype=np.uint8)
        rgb[25:103, 25:103] = [65, 145, 45]
        mask = infer(sys.argv[2], rgb, 64, 64)
        assert mask.shape == (128, 128)
        print("Isolated ARM MagicTouch inference passed.")
    elif len(sys.argv) == 6:
        model, source, target, x, y = sys.argv[1:]
        if Path(source).stat().st_size > 384 * 384 * 3 + 1024:
            raise ValueError("Input too large.")
        rgb = np.load(source, allow_pickle=False)
        mask = infer(model, rgb, int(x), int(y))
        with Path(target).open("xb") as output:
            np.save(output, mask, allow_pickle=False)
    else:
        raise SystemExit("Invalid arguments.")
