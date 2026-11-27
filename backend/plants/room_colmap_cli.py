"""Native COLMAP CPU adapter for ARM. Only registered text-model geometry is used."""
from dataclasses import dataclass
from pathlib import Path
import os
import re
import subprocess
import time

import numpy as np


def colmap_version():
    process = subprocess.run(["colmap", "-h"], text=True, capture_output=True,
                             env=dict(os.environ, QT_QPA_PLATFORM="offscreen"), timeout=12, check=True)
    match = re.search(r"COLMAP\s+([0-9]+\.[0-9]+(?:\.[0-9]+)?)", process.stdout + process.stderr)
    if not match or match.group(1) not in {"3.10", "3.10.0"}:
        raise ValueError("Native COLMAP version is not the reviewed candidate.")
    return match.group(1)


@dataclass
class Rotation:
    value: np.ndarray

    def matrix(self):
        return self.value


@dataclass
class Pose:
    rotation: Rotation
    translation: np.ndarray

    def inverse(self):
        inverse = self.rotation.matrix().T
        return Pose(Rotation(inverse), -(inverse @ self.translation))

    def __mul__(self, other):
        if isinstance(other, Pose):
            r = self.rotation.matrix()
            return Pose(Rotation(r @ other.rotation.matrix()), r @ other.translation + self.translation)
        return np.asarray(other) @ self.rotation.matrix().T + self.translation


def camera_pose(q, t):
    q = np.asarray(q, dtype=float)
    norm = np.linalg.norm(q)
    if not np.isfinite(norm) or norm <= 0:
        raise ValueError("Invalid COLMAP rotation.")
    w, x, y, z = q / norm
    r = np.array([
        [1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)],
        [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)],
        [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)],
    ])
    translation = np.asarray(t, dtype=float)
    if not np.isfinite(translation).all():
        raise ValueError("Invalid COLMAP translation.")
    return Pose(Rotation(r), translation)


@dataclass
class Camera:
    model_name: str
    width: int
    height: int
    params: np.ndarray

    def calibration_matrix(self):
        p = self.params
        if self.model_name in {"SIMPLE_PINHOLE", "SIMPLE_RADIAL", "RADIAL"}:
            fx = fy = p[0]
            cx, cy = p[1:3]
        elif self.model_name in {"PINHOLE", "OPENCV"}:
            fx, fy, cx, cy = p[:4]
        else:
            raise ValueError("Unsupported stereo camera model.")
        return np.array([[fx, 0., cx], [0., fy, cy], [0., 0., 1.]])

    def extra_params_idxs(self):
        start = 3 if self.model_name in {"SIMPLE_PINHOLE", "SIMPLE_RADIAL", "RADIAL"} else 4
        return list(range(start, len(self.params)))


@dataclass
class Observation:
    xy: np.ndarray
    point3D_id: int

    def has_point3D(self):
        return self.point3D_id >= 0


@dataclass
class TrackElement:
    image_id: int
    point2D_idx: int


@dataclass
class Track:
    elements: list

    def length(self):
        return len(self.elements)


@dataclass
class Landmark:
    xyz: np.ndarray
    color: np.ndarray
    error: float
    track: Track


@dataclass
class RegisteredImage:
    image_id: int
    name: str
    camera: Camera
    pose: Pose
    points2D: list

    @property
    def num_points3D(self):
        return sum(p.has_point3D() for p in self.points2D)

    def cam_from_world(self):
        return self.pose


@dataclass
class Model:
    images: dict
    points3D: dict

    def num_reg_images(self):
        return len(self.images)

    def num_points3D(self):
        return len(self.points3D)

    def compute_mean_reprojection_error(self):
        return float(np.mean([p.error for p in self.points3D.values()]))


def records(path):
    if path.stat().st_size > 96 * 1024 * 1024:
        raise ValueError("Reconstruction text exceeds the review limit.")
    return (line.strip() for line in path.read_text(encoding="utf-8").splitlines()
            if not line.lstrip().startswith("#"))


def read_model(folder, allowed_names):
    cameras = {}
    for line in records(folder / "cameras.txt"):
        if not line:
            continue
        i, model, w, h, *params = line.split()
        p = np.asarray(params, dtype=float)
        expected = {"SIMPLE_PINHOLE": 3, "PINHOLE": 4, "SIMPLE_RADIAL": 4, "RADIAL": 5, "OPENCV": 8}
        if (not np.isfinite(p).all() or int(w) <= 0 or int(h) <= 0 or
                (model in expected and (len(p) != expected[model] or p[0] <= 0))):
            raise ValueError("Invalid camera geometry.")
        cameras[int(i)] = Camera(model, int(w), int(h), p)
    images = {}
    lines = iter(records(folder / "images.txt"))
    for line in lines:
        if not line:
            continue
        fields = line.split(maxsplit=9)
        if len(fields) != 10:
            raise ValueError("Invalid registered image header.")
        image_id = int(fields[0])
        name = fields[9]
        if name not in allowed_names:
            raise ValueError("Unexpected registered image name.")
        pose = camera_pose([float(v) for v in fields[1:5]], [float(v) for v in fields[5:8]])
        values = next(lines, "").split()
        if len(values) % 3:
            raise ValueError("Invalid image observations.")
        obs = [Observation(np.array([float(values[i]), float(values[i+1])]), int(values[i+2]))
               for i in range(0, len(values), 3)]
        if not all(np.isfinite(p.xy).all() for p in obs):
            raise ValueError("Invalid observation coordinates.")
        images[image_id] = RegisteredImage(image_id, name, cameras[int(fields[8])], pose, obs)
    points = {}
    for line in records(folder / "points3D.txt"):
        if not line:
            continue
        row = line.split()
        if len(row) < 8 or (len(row)-8) % 2:
            raise ValueError("Invalid landmark track.")
        xyz = np.asarray(row[1:4], dtype=float)
        color = np.asarray(row[4:7], dtype=int)
        error = float(row[7])
        track = [TrackElement(int(row[i]), int(row[i+1])) for i in range(8, len(row), 2)]
        if not np.isfinite(xyz).all() or not np.isfinite(error) or not (0 <= color).all() or not (color <= 255).all():
            raise ValueError("Invalid landmark values.")
        points[int(row[0])] = Landmark(xyz, color, error, Track(track))
    for image in images.values():
        for p in image.points2D:
            if p.has_point3D() and p.point3D_id not in points:
                raise ValueError("Registered observation lacks its landmark.")
    for point_id, point in points.items():
        for obs in point.track.elements:
            image = images.get(obs.image_id)
            if image is None or not 0 <= obs.point2D_idx < len(image.points2D) or image.points2D[obs.point2D_idx].point3D_id != point_id:
                raise ValueError("Landmark track does not match registered observations.")
    return Model(images, points)


def reconstruct_models(images, work, files, *, single_camera, threads):
    version = colmap_version()
    deadline = time.monotonic() + 215
    env = dict(os.environ, QT_QPA_PLATFORM="offscreen", OMP_NUM_THREADS=str(threads),
               OPENBLAS_NUM_THREADS=str(threads))

    def run(*args):
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise subprocess.TimeoutExpired(args, 215)
        subprocess.run(["colmap", *args], env=env, timeout=remaining, check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    def option(command, *names):
        help_text = subprocess.run(["colmap", command, "-h"], env=env, timeout=10,
                                   text=True, capture_output=True, check=True).stdout
        for name in names:
            if name in help_text:
                return name
        raise ValueError("Required CPU COLMAP option is unavailable: " + names[0])

    database = str(work / "matches.db")
    paths = str(work / "images.txt")
    Path(paths).write_text("\n".join(f.name for f in files) + "\n", encoding="utf-8")
    model_dir = work / "models"
    model_dir.mkdir(exist_ok=True)
    extract_gpu = option("feature_extractor", "--SiftExtraction.use_gpu", "--FeatureExtraction.use_gpu")
    extract_threads = option("feature_extractor", "--SiftExtraction.num_threads", "--FeatureExtraction.num_threads")
    extract_size = option("feature_extractor", "--SiftExtraction.max_image_size", "--FeatureExtraction.max_image_size")
    match_gpu = option("exhaustive_matcher", "--SiftMatching.use_gpu", "--FeatureMatching.use_gpu")
    match_threads = option("exhaustive_matcher", "--SiftMatching.num_threads", "--FeatureMatching.num_threads")
    run("feature_extractor", "--database_path", database, "--image_path", str(images),
        "--image_list_path", paths, "--ImageReader.single_camera", "1" if single_camera else "0",
        extract_gpu, "0", extract_threads, str(threads), extract_size, "1600")
    run("exhaustive_matcher", "--database_path", database, match_gpu, "0", match_threads, str(threads))
    run("mapper", "--database_path", database, "--image_path", str(images),
        "--output_path", str(model_dir), "--Mapper.num_threads", str(threads))
    models = []
    for folder in sorted(model_dir.iterdir()):
        if not folder.is_dir() or not (folder / "images.bin").is_file():
            continue
        text_dir = work / ("text-" + folder.name)
        text_dir.mkdir()
        run("model_converter", "--input_path", str(folder), "--output_path", str(text_dir), "--output_type", "TXT")
        models.append(read_model(text_dir, {f.name for f in files}))
    return version, models
