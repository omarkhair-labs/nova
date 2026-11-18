#!/usr/bin/env python3
"""Build sharp, locally hosted cinematic backgrounds from the original scene images.

This is an asset processing pipeline, NOT creation of true native 4K details.
It uses the publicly available FSRCNN x4 model to estimate higher spatial detail,
then downsamples to 2560x1440 and encodes quality WebP without destructive tiny AVIF.
Only run in the dedicated, non-production Plantupia public-web branch.
"""

from pathlib import Path
import argparse
import hashlib
import urllib.request

import cv2
import numpy as np
from PIL import Image, ImageFilter

HERE = Path(__file__).resolve()
WEB = HERE.parents[1]
SOURCE = WEB / "public" / "scene"
DEST = SOURCE / "hd"
MODEL_DIR = WEB / ".cache" / "scene-superres"
MODEL = MODEL_DIR / "FSRCNN_x4.pb"
MODEL_URL = (
    "https://raw.githubusercontent.com/Saafke/FSRCNN_Tensorflow/master/"
    "models/FSRCNN_x4.pb"
)
SCENES = ("space", "life", "time")
OUTPUT_SIZE = (2560, 1440)

def get_model() -> None:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    if MODEL.exists() and MODEL.stat().st_size > 30_000:
        return
    with urllib.request.urlopen(MODEL_URL, timeout=35) as resp:
        content = resp.read()
    if not (30_000 < len(content) < 100_000):
        raise RuntimeError("Unexpected FSRCNN model size; refusing unknown download")
    MODEL.write_bytes(content)

def build_scene(superres, name: str) -> None:
    input_path = SOURCE / (name + ".avif")
    if not input_path.is_file():
        raise RuntimeError(f"Missing expected original cinematic scene: {input_path}")
    original = Image.open(input_path).convert("RGB")
    if original.size != (960, 540):
        raise RuntimeError(f"Unexpected original {name} resolution: {original.size}")

    # cv2.dnn_superres consumes BGR and returns a fourfold RGB-like BGR result.
    arr = np.array(original)
    bgr = cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
    reconstructed = superres.upsample(bgr)
    expected = (original.height * 4, original.width * 4)
    if reconstructed.shape[:2] != expected:
        raise RuntimeError(f"Bad model output for {name}: {reconstructed.shape}")

    rgb = cv2.cvtColor(reconstructed, cv2.COLOR_BGR2RGB)
    image = Image.fromarray(rgb, "RGB").resize(OUTPUT_SIZE, Image.Resampling.LANCZOS)
    # Keep the filter restrained: no halo around foliage or hard concrete edges.
    image = image.filter(ImageFilter.UnsharpMask(radius=0.8, percent=65, threshold=4))

    DEST.mkdir(parents=True, exist_ok=True)
    dest = DEST / (name + ".webp")
    image.save(dest, format="WEBP", quality=88, method=6)
    if dest.stat().st_size < 100_000:
        raise RuntimeError(f"Export suspiciously small: {dest}")
    print(f"{name}: {original.width}x{original.height} -> {image.width}x{image.height} / {dest.stat().st_size:,} bytes")

def main() -> None:
    get_model()
    superres = cv2.dnn_superres.DnnSuperResImpl_create()
    superres.readModel(str(MODEL))
    superres.setModel("fsrcnn", 4)
    for name in SCENES:
        build_scene(superres, name)

if __name__ == "__main__":
    main()
