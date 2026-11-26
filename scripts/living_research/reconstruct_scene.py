"""Reconstruct an opt-in local image sequence for Plantupia's native point viewer.

Needs PyCOLMAP 4.2.1. The PLY is an unscaled research scene. Keep original
images, private reconstructions and temporary feature databases outside Git.
"""
import argparse
import json
from hashlib import file_digest
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np
import pycolmap as colmap


def write_scene(model, path):
    points = [
        point for point in model.points3D.values()
        if point.track.length() >= 3 and 0 <= point.error <= 2 and np.isfinite(point.xyz).all()
    ]
    if len(points) < 30:
        raise ValueError("Insufficient well-supported points for a scene.")
    available = len(points)
    if available > 6_000:
        by_error = sorted(points, key=lambda point: point.error)
        # Keep the low-error population while sampling across the complete set.
        points = [by_error[index] for index in np.linspace(0, available - 1, 6_000, dtype=int)]
    with path.open("x", encoding="ascii") as stream:
        stream.write(f"""ply
format ascii 1.0
comment plantupia_sparse_sfm_unscaled
element vertex {len(points)}
property float x
property float y
property float z
property uchar red
property uchar green
property uchar blue
end_header
""")
        for point in points:
            coordinates = " ".join(f"{value:.8g}" for value in point.xyz)
            colors = " ".join(str(int(value)) for value in point.color)
            stream.write(f"{coordinates} {colors}\n")
    return available, len(points)


def reconstruct(images, output, *, single_camera):
    files = sorted(path for path in images.iterdir() if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png"})
    if not 5 <= len(files) <= 80:
        raise ValueError("Supply 5 to 80 overlapping photographs.")
    if output.exists() or output.with_suffix(".json").exists():
        raise ValueError("Choose a new output name for the scene and its report.")
    with TemporaryDirectory(prefix="plantupia-sfm-") as temp:
        work = Path(temp)
        model_dir = work / "models"
        model_dir.mkdir()
        database = work / "matches.db"
        extraction = colmap.FeatureExtractionOptions(max_image_size=1600, num_threads=4, use_gpu=False)
        matching = colmap.FeatureMatchingOptions(num_threads=4, use_gpu=False)
        mapping = colmap.IncrementalPipelineOptions(num_threads=4, random_seed=42)
        colmap.extract_features(
            database, images, image_names=[path.name for path in files],
            camera_mode=colmap.CameraMode.SINGLE if single_camera else colmap.CameraMode.AUTO,
            extraction_options=extraction, device=colmap.Device.cpu,
        )
        colmap.match_exhaustive(database, matching_options=matching, device=colmap.Device.cpu)
        models = colmap.incremental_mapping(database, images, model_dir, options=mapping)
        best = max(models.values(), key=lambda m: (m.num_reg_images(), m.num_points3D()), default=None)
        if best is None or best.num_reg_images() < max(5, .65 * len(files)):
            raise ValueError("Too few views registered together. Add overlap and keep the scene still.")
        eligible, exported = write_scene(best, output)
        with output.open("rb") as stream:
            sha256 = file_digest(stream, "sha256").hexdigest()
        info = dict(
            algorithm="COLMAP incremental structure from motion", pycolmap=colmap.__version__,
            supplied_views=len(files), registered_views=best.num_reg_images(), all_sparse_points=best.num_points3D(),
            points_with_three_views_and_error_up_to_two_px=eligible, exported_points=exported,
            mean_reprojection_px=best.compute_mean_reprojection_error(), scene_sha256=sha256, scale="unknown",
        )
        output.with_suffix(".json").write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
        return info


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image_directory", type=Path)
    parser.add_argument("output_ply", type=Path)
    parser.add_argument("--single-camera", action="store_true", help="all views use identical intrinsics and dimensions")
    args = parser.parse_args()
    if args.output_ply.suffix.lower() != ".ply":
        parser.error("The scene output must end in .ply.")
    try:
        print(json.dumps(reconstruct(args.image_directory, args.output_ply, single_camera=args.single_camera), indent=2))
    except ValueError as exc:
        parser.error(str(exc))
