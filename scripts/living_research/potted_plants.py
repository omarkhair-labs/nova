"""COCO potted-plant localization probe. Run with --help for the input contract."""
import argparse
import hashlib
import json
from collections import defaultdict
from pathlib import Path
from random import Random
from time import perf_counter
from urllib.request import urlopen

import cv2
import numpy as np


class PottedPlantDetector:
    """OpenCV Zoo's YOLOX export. Coordinates refer to the original upright RGB image."""
    def __init__(self, weights):
        self.net = cv2.dnn.readNetFromONNX(str(weights))
        grids, strides = [], []
        for stride in (8, 16, 32):
            side = 640 // stride
            yy, xx = np.indices((side, side))
            grids.append(np.column_stack((xx.ravel(), yy.ravel())))
            strides.append(np.full((side*side, 1), stride))
        self.grid = np.concatenate(grids)
        self.strides = np.concatenate(strides)

    def detect(self, rgb):
        h, w = rgb.shape[:2]
        scale = 640 / max(h, w)
        image = cv2.resize(rgb, (max(1, int(w*scale)), max(1, int(h*scale))))
        frame = np.full((640, 640, 3), 114, dtype=np.uint8)
        frame[:image.shape[0], :image.shape[1]] = image
        self.net.setInput(cv2.dnn.blobFromImage(frame))
        output = self.net.forward()[0]
        # Keep only anchors whose most likely COCO category is potted plant.
        score = output[:, 4] * output[:, 5 + 58]
        keep = (score >= .35) & (output[:, 5:].argmax(axis=1) == 58)
        if not keep.any():
            return []
        centers = (output[keep, :2] + self.grid[keep]) * self.strides[keep] / scale
        sizes = np.exp(output[keep, 2:4]) * self.strides[keep] / scale
        lower = np.clip(centers-sizes/2, 0, [w, h])
        upper = np.clip(centers+sizes/2, 0, [w, h])
        boxes = np.hstack((lower, upper-lower)).tolist()
        scores = score[keep].tolist()
        indices = cv2.dnn.NMSBoxes(boxes, scores, .35, .5)
        return [{"bbox_xywh": boxes[i], "score": scores[i]} for i in indices
                if boxes[i][2] > 0 and boxes[i][3] > 0]


def iou(a, b):
    w = max(0, min(a[0]+a[2], b[0]+b[2])-max(a[0], b[0]))
    h = max(0, min(a[1]+a[3], b[1]+b[3])-max(a[1], b[1]))
    return w*h / (a[2]*a[3]+b[2]*b[3]-w*h)


def main():
    p = argparse.ArgumentParser(description="Compare two YOLOX exports on a stratified COCO val2017 probe. "
        "Missing public photographs are fetched from COCO's S3 bucket into the specified local cache.")
    p.add_argument("annotations", type=Path)
    p.add_argument("image_cache", type=Path)
    p.add_argument("output", type=Path)
    p.add_argument("weights", nargs="+", type=Path)
    args = p.parse_args()
    data = json.loads(args.annotations.read_bytes())
    assert any(c["id"] == 64 and c["name"] == "potted plant" for c in data["categories"])
    truth = defaultdict(list)
    crowds = set()
    for ann in data["annotations"]:
        if ann["category_id"] == 64:
            if ann["iscrowd"]:
                crowds.add(ann["image_id"])
            else:
                truth[ann["image_id"]].append(ann["bbox"])
    rng = Random(20261126)
    ids = sorted(im["id"] for im in data["images"] if im["id"] not in crowds)
    selected = [idx for group in (0, 1, 2) for idx in rng.sample(
        [i for i in ids if min(len(truth[i]), 2) == group], 12)]
    del data
    args.image_cache.mkdir(parents=True, exist_ok=True)
    report = {"selection": "12 each of zero, one, multiple annotated plants; crowd images excluded",
              "iou_threshold": .5, "score_threshold": .35, "opencv": cv2.__version__, "models": {}}
    cv2.setNumThreads(1)
    for weights in args.weights:
        detector = PottedPlantDetector(weights)
        rows = []
        for idx in selected:
            name = f"{idx:012}.jpg"
            file = args.image_cache / name
            if not file.exists():
                with urlopen("https://s3.amazonaws.com/images.cocodataset.org/val2017/" + name, timeout=40) as stream:
                    file.write_bytes(stream.read())
            rgb = cv2.cvtColor(cv2.imread(str(file)), cv2.COLOR_BGR2RGB)
            start = perf_counter()
            predictions = detector.detect(rgb)
            ms = (perf_counter()-start)*1000
            unmatched = list(truth[idx])
            tp = 0
            for pred in sorted(predictions, key=lambda d: -d["score"]):
                if unmatched:
                    best = max(range(len(unmatched)), key=lambda j: iou(pred["bbox_xywh"], unmatched[j]))
                    if iou(pred["bbox_xywh"], unmatched[best]) >= .5:
                        tp += 1
                        unmatched.pop(best)
            rows.append(dict(image_id=idx, annotated=len(truth[idx]), tp=tp,
                             fp=len(predictions)-tp, fn=len(unmatched), ms=round(ms, 2), detections=predictions))
        tp, fp, fn = (sum(row[key] for row in rows) for key in ("tp", "fp", "fn"))
        report["models"][weights.name] = dict(sha256=hashlib.sha256(weights.read_bytes()).hexdigest(),
            tp=tp, fp=fp, fn=fn, precision=tp/(tp+fp) if tp+fp else None, recall=tp/(tp+fn),
            empty_predictions=sum(not r["detections"] for r in rows), median_ms=float(np.median([r["ms"] for r in rows])),
            rows=rows)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
