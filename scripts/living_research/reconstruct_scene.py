"""Reconstruct an opt-in local image sequence for Plantupia's native point viewer.

Needs PyCOLMAP 4.2.1. The PLY is an unscaled research scene. Keep original
images, private reconstructions and temporary feature databases outside Git.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "backend"))
from plants.room_sfm import main, reconstruct


if __name__ == "__main__":
    main()
