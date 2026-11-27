"""Research-only comparison. One oracle positive point for MagicTouch; same positive
and one oracle negative point for GrabCut. Never infer performance on indoor leaves."""
import argparse
from collections import defaultdict
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
import sys
import tarfile
import time
import cv2
import numpy as np
from PIL import Image
import mediapipe as mp
from mediapipe.tasks.python.components.containers.keypoint import NormalizedKeypoint

p = argparse.ArgumentParser()
p.add_argument("images", type=Path)
p.add_argument("trimaps", type=Path)
p.add_argument("model", type=Path)
p.add_argument("backend", type=Path)
p.add_argument("output", type=Path)
p.add_argument("--allow-incomplete", action="store_true")
a = p.parse_args()
sys.path.insert(0, str(a.backend))
from plants.living_segmentation import propose_outline, _outline_from_labels, NoOutline
assert sha256(a.model.read_bytes()).hexdigest() == "e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25"
cv2.setNumThreads(1)
Vision = mp.tasks.vision
options = Vision.InteractiveSegmenterOptions(
    base_options=mp.tasks.BaseOptions(model_asset_path=str(a.model)),
    output_confidence_masks=True, output_category_mask=False)
def read(archive, name):
    with Image.open(BytesIO(archive.extractfile(name).read())) as im:
        return np.array(im if name.endswith(".png") else im.convert("RGB"))
def point(mask):
    dist = cv2.distanceTransform(mask.astype(np.uint8), cv2.DIST_L2, 5)
    y,x = np.unravel_index(dist.argmax(), dist.shape)
    return int(x),int(y)
def score(mask, fg, valid):
    mask = cv2.resize(mask.astype(np.uint8), (fg.shape[1], fg.shape[0]), interpolation=cv2.INTER_NEAREST) > 0
    union = ((mask | fg) & valid).sum()
    return float((mask & fg).sum()/union) if union else 0.
def mask_polygon(points, w, h):
    mask = np.zeros((h,w), np.uint8)
    cv2.fillPoly(mask, [np.array(points,dtype=np.int32)], 1)
    return mask
rows=[]
groups=defaultdict(list)
with tarfile.open(a.trimaps) as labels, Vision.InteractiveSegmenter.create_from_options(options) as model:
    for name in sorted(labels.getnames()):
        if name.endswith(".png") and set(np.unique(read(labels,name))) == {0,1,3}:
            groups[(int(Path(name).stem.split("_")[-1])-1)//80].append(name)
    selected=[names[i] for names in groups.values() for i in np.linspace(0,len(names)-1,3,dtype=int)]
    wanted={"jpg/"+Path(n).stem+".jpg" for n in selected}
    photo_bytes={}
    complete=True
    try:
        with tarfile.open(a.images,mode="r|gz") as photos:
            for member in photos:
                if member.name in wanted and member.isfile():
                    raw=photos.extractfile(member).read()
                    if len(raw)==member.size:
                        photo_bytes[member.name]=raw
    except (EOFError, tarfile.ReadError):
        complete=False
        if not a.allow_incomplete:
            raise
    missing=sorted(wanted-photo_bytes.keys())
    if missing and not a.allow_incomplete:
        raise ValueError(f"Missing selected images: {missing}")
    print("COHORT",len(selected),"AVAILABLE",len(photo_bytes),"COMPLETE",complete,flush=True)
    for name in selected:
        key=Path(name).stem
        source=photo_bytes.get("jpg/"+key+".jpg")
        if source is None: continue
        with Image.open(BytesIO(source)) as image:
            rgb=np.array(image.convert("RGB"))
        truth=read(labels,name)
        assert truth.shape == rgb.shape[:2]
        fg=truth==1
        valid=truth!=0
        scale=min(1.,384/max(rgb.shape[:2]))
        small=cv2.resize(rgb,None,fx=scale,fy=scale,interpolation=cv2.INTER_AREA)
        h,w=small.shape[:2]
        ref=cv2.resize(truth,(w,h),interpolation=cv2.INTER_NEAREST)
        pos,neg=point(ref==1),point(ref==3)
        begin=time.perf_counter()
        roi=Vision.InteractiveSegmenterRegionOfInterest(
            format=Vision.InteractiveSegmenterRegionOfInterest.Format.KEYPOINT,
            keypoint=NormalizedKeypoint(x=(pos[0]+.5)/w,y=(pos[1]+.5)/h))
        result=model.segment(mp.Image(image_format=mp.ImageFormat.SRGB,data=np.ascontiguousarray(small)),roi)
        assert len(result.confidence_masks)==1
        confidence=result.confidence_masks[0].numpy_view().squeeze().copy()
        assert confidence.ndim==2 and np.isfinite(confidence).all()
        raw=(confidence>=0.5).astype(np.uint8)
        mask=cv2.resize(raw,(w,h),interpolation=cv2.INTER_NEAREST)
        neural_iou=score(mask,fg,valid)
        neural_poly=None
        try:
            outline=_outline_from_labels(mask,w,h,[pos],[neg])
            neural_poly=score(mask_polygon(outline,w,h),fg,valid)
        except NoOutline:
            pass
        neural_ms=(time.perf_counter()-begin)*1000
        begin=time.perf_counter()
        classical=None
        try:
            outline=propose_outline(Image.fromarray(small),[pos],[neg])
            classical=score(mask_polygon(outline,w,h),fg,valid)
        except NoOutline:
            pass
        grab_ms=(time.perf_counter()-begin)*1000
        row=dict(image=key, neural_mask_iou=round(neural_iou,5), neural_polygon_iou=None if neural_poly is None else round(neural_poly,5), grabcut_polygon_iou=None if classical is None else round(classical,5), neural_ms=round(neural_ms,2),grabcut_ms=round(grab_ms,2))
        rows.append(row)
        a.output.write_text(json.dumps({"rows":rows,"missing":missing,"archive_complete":complete,"planned_n":len(selected),"model_sha256":sha256(a.model.read_bytes()).hexdigest(),"mediapipe":mp.__version__,"opencv":cv2.__version__},indent=2),encoding="utf-8")
        print(json.dumps(row),flush=True)
print("DONE",len(rows),flush=True)
