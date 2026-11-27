"""Opt-in build/runtime for the existing, isolated Oracle #253 preview.

Model binaries are downloaded and verified during image build, never committed
or fetched during a customer's request. Ordinary Nova images stay unchanged.
"""
import hashlib
import json
import os
import platform
import signal
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import urlopen

ROOT = Path("/opt/plantupia-models")
ARM_NEURAL_PYTHON = Path("/opt/plantupia-neural/bin/python")
MANIFEST = ROOT / "runtime.json"
QA_HOST = "253.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"
QA_DB = "imfiwc6cb0pkuyuzu1y1rxsu"
QA_REDIS = "c2qliy8a8vxhklp5yjypi1j4"
SPECIES_ROOT = ("https://huggingface.co/m3r1al/verdant-plant-model/resolve/"
                "239d591ffc13815a054492d3924966a76740dc01/")
MAGIC_URL = ("https://storage.googleapis.com/mediapipe-models/interactive_segmenter/"
             "magic_touch/float32/1/magic_touch.tflite")


def download(path, url, expected, max_bytes):
    with urlopen(url, timeout=90) as response:
        body = response.read(max_bytes + 1)
    if len(body) > max_bytes or hashlib.sha256(body).hexdigest() != expected:
        raise RuntimeError("Unexpected model artifact: " + path.name)
    path.write_bytes(body)


def prepare():
    arch = platform.machine()
    if arch not in ("x86_64", "aarch64"):
        raise RuntimeError("Unreviewed Linux architecture: " + arch)
    full = arch == "x86_64"
    arm_candidate = arch == "aarch64" and os.environ.get("PLANTUPIA_HOSTED_ARM_VISION_QA") == "1"
    neural = full or arm_candidate
    # Preserve the working backend/ONNX environment. The legacy ARM MediaPipe
    # protobuf and NumPy constraints belong in a separate virtual environment.
    packages = [
        "numpy==2.2.6", "onnxruntime==1.31.1",
        ("opencv-contrib-python" if full else "opencv-python-headless") + "==4.11.0.86",
    ]
    if full:
        packages += ["mediapipe==0.10.32", "matplotlib==3.10.8", "pycolmap==4.2.1"]
    subprocess.run([sys.executable, "-m", "pip", "install", "--no-cache-dir",
                    "--only-binary=:all:", *packages], check=True)
    subprocess.run([sys.executable, "-m", "pip", "check"], check=True)
    if arm_candidate:
        subprocess.run([sys.executable, "-m", "venv", str(ARM_NEURAL_PYTHON.parent.parent)], check=True)
        subprocess.run([str(ARM_NEURAL_PYTHON), "-m", "pip", "install", "--no-cache-dir", "--only-binary=:all:",
                        "numpy==1.26.4", "opencv-contrib-python==4.11.0.86",
                        "mediapipe==0.10.18", "protobuf==4.25.8", "jax==0.4.38",
                        "jaxlib==0.4.38", "matplotlib==3.10.8"], check=True)
        subprocess.run([str(ARM_NEURAL_PYTHON), "-m", "pip", "check"], check=True)
    species = ROOT / "species"
    species.mkdir(parents=True, exist_ok=True)
    model = species / "species_dinov2_v12_int8.onnx"
    download(model, SPECIES_ROOT + model.name,
             "63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e",
             32 * 1024 * 1024)
    config_path = species / "config.json"
    download(config_path, SPECIES_ROOT + config_path.name,
             "84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e",
             128 * 1024)
    import cv2
    import numpy as np
    import onnxruntime as ort
    assert hasattr(cv2, "grabCut") and hasattr(cv2, "aruco")
    options = ort.SessionOptions()
    options.intra_op_num_threads = 1
    options.inter_op_num_threads = 1
    session = ort.InferenceSession(str(model), options, providers=["CPUExecutionProvider"])
    config = json.loads(config_path.read_text())["species_head"]
    output = session.run([config["output_name"]], {
        config["input_name"]: np.zeros((1, 3, 224, 224), dtype=np.float32)
    })[0]
    assert output.size and np.isfinite(output).all()
    del session
    if full:
        import pycolmap
        assert hasattr(pycolmap, "IncrementalPipelineOptions")
    if arm_candidate:
        from plants.room_colmap_cli import colmap_version
        colmap_version()
    if neural:
        magic = ROOT / "magic_touch.tflite"
        download(magic, MAGIC_URL,
                 "e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25",
                 7 * 1024 * 1024)
        if arm_candidate:
            subprocess.run([str(ARM_NEURAL_PYTHON), "/app/plants/arm_neural_worker.py", "--check", str(magic)],
                           timeout=45, check=True, env=dict(os.environ, OPENBLAS_NUM_THREADS="1", OMP_NUM_THREADS="1"))
        else:
            import mediapipe as mp
            assert mp.__version__ == "0.10.32"
            opts = mp.tasks.vision.InteractiveSegmenterOptions(
                base_options=mp.tasks.BaseOptions(model_asset_buffer=magic.read_bytes()),
                output_confidence_masks=True, output_category_mask=False,
            )
            with mp.tasks.vision.InteractiveSegmenter.create_from_options(opts):
                pass
    MANIFEST.write_text(json.dumps({"architecture": arch, "species": True,
                                    "neural": neural, "sfm": neural,
                                    "neural_runtime": ("0.10.18" if arm_candidate else "0.10.32") if neural else "",
                                    "sfm_engine": "colmap-cli" if arm_candidate else "pycolmap" if full else ""}))
    print("Plantupia hosted QA image checked: " + MANIFEST.read_text(), flush=True)


def host(value):
    if not value:
        return ""
    return urlsplit(value if "://" in value else "//" + value).hostname or ""


def verified_preview(env):
    """Refuse hosted migrations on a non-253 deployment or non-QA services."""
    try:
        hosts = {host(part.strip()) for key in ("COOLIFY_URL", "COOLIFY_FQDN")
                 for part in env.get(key, "").split(",") if part.strip()}
        allowed = {part.strip() for part in env.get("DJANGO_ALLOWED_HOSTS", "").split(",")}
        return (
            env.get("DJANGO_ENV") == "production"
            and env.get("DJANGO_DEBUG") == "0"
            and env.get("PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED") == "1"
            and env.get("PLANTUPIA_HANDOVER_QA_ENABLED") == "1"
            and hosts == {QA_HOST} and QA_HOST in allowed
            and host(env.get("DATABASE_URL", "")) == QA_DB
            and host(env.get("REDIS_URL", "")) == QA_REDIS
        )
    except ValueError:
        return False


def start():
    enabled = os.environ.get("PLANTUPIA_HOSTED_LIVING_QA") == "1"
    if not enabled and not MANIFEST.exists():
        os.execvp("sh", ["sh", "-c",
            "python manage.py migrate --noinput && python manage.py collectstatic --noinput"
            " && exec daphne -b 0.0.0.0 -p ${PORT:-8000} nova_backend.asgi:application"])
    if not enabled or not MANIFEST.is_file() or not verified_preview(os.environ):
        raise SystemExit("Hosted Living QA refused: expected built image and isolated #253 services.")
    capabilities = json.loads(MANIFEST.read_text())
    if not capabilities.get("species"):
        raise SystemExit("Hosted Living QA model preparation incomplete.")
    os.environ.update({
        "PLANTUPIA_LIVING_SYSTEMS_ENABLED": "1",
        "PLANTUPIA_SPECIES_MODEL_DIR": str(ROOT / "species"),
        "PLANTUPIA_NEURAL_OUTLINE_MODEL": str(ROOT / "magic_touch.tflite") if capabilities["neural"] else "",
        "PLANTUPIA_ROOM_SFM_ENABLED": "1" if capabilities["sfm"] else "0",
        "PLANTUPIA_ROOM_SFM_ENGINE": capabilities.get("sfm_engine", "pycolmap"),
        "PLANTUPIA_NEURAL_RUNTIME_VERSION": capabilities.get("neural_runtime", "0.10.32"),
        "PLANTUPIA_ARM_NEURAL_PYTHON": str(ARM_NEURAL_PYTHON) if capabilities.get("neural_runtime") == "0.10.18" else "",
        "PLANTUPIA_ROOM_SFM_THREADS": "2",
        "OPENCV_FOR_THREADS_NUM": "2",
        "OMP_NUM_THREADS": "2",
        "OPENBLAS_NUM_THREADS": "2",
    })
    print("Verified #253 QA database and Redis. Engines: " + json.dumps(capabilities), flush=True)
    subprocess.run([sys.executable, "manage.py", "migrate", "--noinput"], check=True)
    subprocess.run([sys.executable, "manage.py", "collectstatic", "--noinput"], check=True)
    children = []

    def stop(*_):
        for child in children:
            if child.poll() is None:
                child.terminate()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    try:
        children.append(subprocess.Popen(["daphne", "-b", "0.0.0.0", "-p",
                                          os.environ.get("PORT", "8000"),
                                          "nova_backend.asgi:application"]))
        if capabilities["sfm"]:
            children.append(subprocess.Popen(["nice", "-n", "10", sys.executable,
                                              "manage.py", "process_room_scenes", "--watch"]))
        while all(child.poll() is None for child in children):
            time.sleep(1)
        return next((child.returncode for child in children if child.returncode), 0)
    finally:
        stop()
        for child in children:
            try:
                child.wait(timeout=15)
            except subprocess.TimeoutExpired:
                child.kill()
                child.wait()


if __name__ == "__main__":
    if sys.argv[1:] == ["prepare"]:
        prepare()
    elif sys.argv[1:] == ["start"]:
        sys.exit(start())
    else:
        raise SystemExit("Specify prepare or start.")
