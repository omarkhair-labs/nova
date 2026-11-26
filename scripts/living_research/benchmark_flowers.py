"""Compare automatic flower foreground masks on Oxford 17 annotated photographs.

python benchmark_flowers.py 17flowers.tgz trimaps.tgz u2netp.onnx results.json
Local research dependencies: opencv-python-headless==4.11.0.86, Pillow and numpy.
No archive extraction or image redistribution. Selection precedes inference.
"""
import argparse
import hashlib
import json
import platform
import tarfile
import time
from collections import defaultdict
from io import BytesIO
from pathlib import Path

import cv2
import numpy as np
from PIL import Image


def digest(path):
    with path.open("rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()


def salient(rgb, net):
    resized = cv2.resize(rgb, (320, 320), interpolation=cv2.INTER_AREA).astype(np.float32)
    resized /= max(1, resized.max())
    resized -= np.float32([.485, .456, .406])
    resized /= np.float32([.229, .224, .225])
    net.setInput(resized.transpose(2, 0, 1)[None])
    prediction = net.forward("1959").squeeze()
    span = np.ptp(prediction)
    if span < 1e-6:
        return np.zeros(rgb.shape[:2], dtype=bool)
    score = (prediction - prediction.min()) / span
    return cv2.resize(score, (rgb.shape[1], rgb.shape[0])) >= .5


def grabcut(rgb):
    h, w = rgb.shape[:2]
    labels = np.zeros((h, w), np.uint8)
    # Same fixed frame for every photo. No ground-truth box used as a prompt.
    x, y = max(1, w // 20), max(1, h // 20)
    cv2.setRNGSeed(42)
    cv2.grabCut(rgb, labels, (x, y, w-2*x, h-2*y), None, None, 3, cv2.GC_INIT_WITH_RECT)
    return (labels & 1) == 1


def read_image(archive, name):
    with Image.open(BytesIO(archive.extractfile(name).read())) as im:
        return np.array(im if name.endswith(".png") else im.convert("RGB"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("images", "trimaps", "weights", "output"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    cv2.setNumThreads(1)
    net = cv2.dnn.readNetFromONNX(str(args.weights))
    rows = []
    groups = defaultdict(list)
    with tarfile.open(args.images) as photos, tarfile.open(args.trimaps) as masks:
        for name in sorted(masks.getnames()):
            if name.endswith(".png"):
                # Some annotations use extra palette indices with different semantics.
                if set(np.unique(read_image(masks, name))) != {0, 1, 3}:
                    continue
                number = int(Path(name).stem.split("_")[-1])
                groups[(number-1)//80].append(name)
        for names in groups.values():
            for i in np.linspace(0, len(names)-1, 3, dtype=int):
                name = names[i]
                rgb = read_image(photos, "jpg/" + Path(name).stem + ".jpg")
                truth = read_image(masks, name)
                assert set(np.unique(truth)) <= {0, 1, 3} and truth.shape == rgb.shape[:2]
                valid, fg = truth != 0, truth == 1
                scale = min(1., 512/max(rgb.shape[:2]))
                small = cv2.resize(rgb, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA)
                for model, method in [("fixed_frame_grabcut", grabcut), ("u2netp_saliency", lambda x: salient(x, net))]:
                    start = time.perf_counter()
                    pred = cv2.resize(method(small).astype("uint8"), (rgb.shape[1], rgb.shape[0]),
                                      interpolation=cv2.INTER_NEAREST).astype(bool)
                    elapsed = (time.perf_counter()-start)*1000
                    tp = int((pred & fg).sum())
                    fp = int((pred & valid & ~fg).sum())
                    fn = int((~pred & fg).sum())
                    rows.append(dict(image=Path(name).stem, model=model, tp=tp, fp=fp, fn=fn,
                                     iou=tp/(tp+fp+fn), ms=round(elapsed, 2)))
    summary = {}
    for model in sorted({row["model"] for row in rows}):
        selected = [r for r in rows if r["model"] == model]
        summary[model] = dict(n=len(selected), mean_iou=float(np.mean([r["iou"] for r in selected])),
                              median_ms=float(np.median([r["ms"] for r in selected])),
                              p95_ms=float(np.percentile([r["ms"] for r in selected], 95)))
    report = dict(summary=summary, rows=rows, sha256={p.name: digest(p) for p in (args.images, args.trimaps, args.weights)},
                  opencv=cv2.__version__, numpy=np.__version__, platform=platform.system(), processor=platform.machine())
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
