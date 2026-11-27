"""Register comparable front-facing visits using static scene texture and an explicit ROI.

Inputs are local, upright images of the same identified plant, selected by an operator.
Each background mask marks stationary features only. ROI is in before-image coordinates.
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "backend"))
from plants.living_registration import register


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
