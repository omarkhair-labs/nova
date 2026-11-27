"""Native ARM candidate smoke: use public plant photographs, no account or database."""
import json
import os
import platform
from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.request import urlopen

import cv2
import numpy as np
from PIL import Image

from hosted_living import MANIFEST, ROOT
from plants.living_neural_outline import propose_neural_outline, neural_outline_available
from plants.living_segmentation import NoOutline
from plants.room_sfm import reconstruct

DATA = ("https://huggingface.co/datasets/JackTrum938/PAA-Plant-Dataset/resolve/"
        "19e7f2f2307462aba37b8571acc7a7c0f72a8608/Cabbage/20260328-21/Camera/images/")


def point(mask):
    distance = cv2.distanceTransform(mask.astype(np.uint8), cv2.DIST_L2, 5)
    y, x = np.unravel_index(distance.argmax(), distance.shape)
    if distance[y, x] < 5:
        raise ValueError("No distinct test prompt.")
    return int(x), int(y)


def main():
    assert platform.machine() == "aarch64"
    capabilities = json.loads(MANIFEST.read_text())
    assert capabilities["neural_runtime"] == "0.10.18" and capabilities["sfm_engine"] == "colmap-cli"
    os.environ.update(PLANTUPIA_NEURAL_RUNTIME_VERSION="0.10.18", PLANTUPIA_ROOM_SFM_ENGINE="colmap-cli",
                      PLANTUPIA_ROOM_SFM_THREADS="2", OMP_NUM_THREADS="2", OPENBLAS_NUM_THREADS="2")
    cv2.setNumThreads(1)
    with TemporaryDirectory() as temp:
        folder = Path(temp)
        views = folder / "views"
        views.mkdir()
        # Eight adjacent, overlapping viewpoints; no supplied poses or model.
        for n in (1, 4, 7, 10, 13, 16, 19, 22):
            name = f"{n}.jpg"
            with urlopen(DATA + name, timeout=30) as response:
                raw = response.read(4 * 1024 * 1024 + 1)
            assert 0 < len(raw) <= 4 * 1024 * 1024
            (views / name).write_bytes(raw)
        model = str(ROOT / "magic_touch.tflite")
        assert neural_outline_available(model)
        candidates = 0
        for path in sorted(views.glob("*.jpg"))[:3]:
            with Image.open(path) as original:
                photo = original.convert("RGB")
                photo.thumbnail((640, 640))
            rgb = np.asarray(photo)
            hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
            green = (hsv[:,:,0] >= 30) & (hsv[:,:,0] <= 95) & (hsv[:,:,1] > 65)
            background = (hsv[:,:,1] < 35)
            try:
                fg, bg = point(green), point(background)
                outline = propose_neural_outline(photo, [fg], [bg], model)
                assert 3 <= len(outline) <= 64
                candidates += 1
            except (NoOutline, ValueError):
                continue
        print("MagicTouch actual photo candidates:", candidates, flush=True)
        assert candidates >= 1, "No real neural polygon passed existing geometry checks."
        report = reconstruct(views, folder / "scene.ply", single_camera=True, with_surface=True)
        assert report["registered_views"] >= 6 and report["exported_points"] >= 30
        print("ARM COLMAP:", json.dumps(report), flush=True)
    print("ARM compatibility smoke passed. This is not botanical or field acceptance.", flush=True)


if __name__ == "__main__":
    main()
