"""Frozen DINOv2 embedding retrieval versus the current strict two-view classifier.

Study only: train/calibrate/test by separate observers. No test-driven threshold
changes; abstentions count against coverage. Never publish the reference photos.
"""
from hashlib import sha256
import json
from pathlib import Path
import sys
import time
import numpy as np
import onnx
import onnxruntime as ort
from PIL import Image, ImageChops, ImageStat, ImageOps

import argparse
_cache_parser=argparse.ArgumentParser(description=__doc__)
_cache_parser.add_argument("cache",type=Path)
ROOT=_cache_parser.parse_args().cache.resolve()
if ROOT.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
ROOT.mkdir(parents=True,exist_ok=True)
MODEL=ROOT/"species_dinov2_v12_int8.onnx"
MODEL_HASH="63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e"
CONFIG_HASH="84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e"
KNOWN={"monstera-deliciosa":"Monstera deliciosa","rubber-plant":"Ficus elastica","jade-plant":"Crassula ovata","pothos-golden":"Epipremnum aureum","aloe-vera":"Aloe vera","zz-plant":"Zamioculcas zamiifolia"}
REVERSE={v:k for k,v in KNOWN.items()}
FEATURE="/Div_output_0"
assert sha256(MODEL.read_bytes()).hexdigest()==MODEL_HASH
body=(ROOT/"config.json").read_bytes()
assert sha256(body).hexdigest()==CONFIG_HASH
cfg=json.loads(body)["species_head"]
labels=cfg["labels"]
model=onnx.load(MODEL)
features=[x for x in model.graph.value_info if x.name==FEATURE]
assert len(features)==1 and features[0].type.tensor_type.shape.dim[-1].dim_value==768
model.graph.output.append(features[0])
derived=ROOT/"research_embedding.onnx"
onnx.save(model,derived)
del model
options=ort.SessionOptions()
options.intra_op_num_threads=1
options.inter_op_num_threads=1
options.enable_cpu_mem_arena=False
options.enable_mem_pattern=False
session=ort.InferenceSession(str(derived),options,providers=["CPUExecutionProvider"])
print("MODEL OUTPUTS",[(x.name,x.shape) for x in session.get_outputs()],flush=True)
records=json.loads((ROOT/"cohort.json").read_text())
assert len({r["observer"] for r in records})==len(records)
ids=[p["id"] for r in records for p in r["photos"]]
assert len(set(ids))==len(ids),"duplicate photo identifiers"
cache=ROOT/"features"; cache.mkdir(exist_ok=True)
def infer(photo):
    path=ROOT/"photos"/(str(photo["id"])+".jpg")
    data=path.read_bytes()
    digest=sha256(data).hexdigest()
    with Image.open(path) as im:
        im=ImageOps.exif_transpose(im).convert("RGB")
        w,h=im.size
        if min(w,h)<128 or max(w,h)>5*min(w,h):raise ValueError("dimensions")
        thumb=im.resize((32,32),Image.Resampling.BILINEAR)
        dst=cache/(digest+".npz")
        if dst.exists():
            with np.load(dst,allow_pickle=False) as f:
                return f["logits"],f["embedding"],thumb,digest
        scale=cfg["resize_shortest_edge"]/min(w,h)
        im=im.resize((round(w*scale),round(h*scale)),Image.Resampling.BILINEAR)
        x,y=(im.width-224)//2,(im.height-224)//2
        rgb=np.asarray(im.crop((x,y,x+224,y+224)),dtype=np.float32)/255
        pixels=((rgb-cfg["mean"])/cfg["std"]).astype(np.float32).transpose(2,0,1)[None]
    logits,embedding=session.run([cfg["output_name"],FEATURE],{cfg["input_name"]:pixels})
    logits,embedding=logits[0],embedding[0]
    if logits.shape!=(110,) or embedding.shape!=(768,) or not np.isfinite(logits).all() or not np.isfinite(embedding).all():raise ValueError("invalid tensor")
    norm=np.linalg.norm(embedding)
    if not .5<norm<1.5:raise ValueError("invalid embedding")
    embedding=embedding/norm
    np.savez(dst,logits=logits,embedding=embedding)
    return logits,embedding,thumb,digest
valid=[]
failures=[]
seen_hashes=set()
for i,row in enumerate(records,1):
    try:
        photos=[infer(p) for p in row["photos"]]
        hashes={x[3] for x in photos}
        if len(hashes)!=2 or hashes&seen_hashes:raise ValueError("duplicate bytes")
        if max(ImageStat.Stat(ImageChops.difference(photos[0][2],photos[1][2])).mean)<5:raise ValueError("near duplicate")
        seen_hashes|=hashes
        valid.append(dict(observation=row["observation"],taxon=row["taxon"],partition=row["partition"],vectors=[p[1] for p in photos],logits=[p[0] for p in photos]))
    except (OSError,ValueError,RuntimeError) as exc:
        failures.append(dict(observation=row["observation"],taxon=row["taxon"],partition=row["partition"],error=str(exc)[:80]))
    if i%10==0 or i==len(records): print("extracted",i,"/",len(records),"valid",len(valid),flush=True)
# Eligibility determined solely by cohort completeness, never predictions.
supported=[]
for name in KNOWN.values():
    counts={p:sum(x["taxon"]==name and x["partition"]==p for x in valid) for p in ("reference","calibration","test")}
    if counts["reference"]>=4 and counts["calibration"]>=2 and counts["test"]>=3:supported.append(name)
assert len(supported)>=3
print("supported",supported,flush=True)
refs={t:np.stack([x["vectors"] for x in valid if x["partition"]=="reference" and x["taxon"]==t]) for t in supported}
def score(v):
    # Each reference observation contributes once, irrespective of photo count.
    pairs=[]
    for t,arr in refs.items():
        obs=np.max(arr@v,axis=1)
        pairs.append((t,float(np.mean(np.sort(obs)[-2:]))))
    return sorted(pairs,key=lambda x:(-x[1],x[0]))
def baseline(logits):
    out=[]
    for x in logits:
        p=np.exp(x-x.max()); p/=p.sum()
        idx=np.argsort(p)[::-1]
        out.append(KNOWN.get(labels[idx[0]]) if p[idx[0]]>=.85 and p[idx[0]]-p[idx[1]]>=.20 else None)
    return out[0] if out[0]==out[1] else None

scored=[]
for row in valid:
    if row["partition"]=="reference":continue
    views=[score(v) for v in row["vectors"]]
    first=views[0][0][0]
    agree=first==views[1][0][0]
    support=all(REVERSE[first] in [labels[j] for j in np.argsort(x)[-3:]] for x in row["logits"])
    scored.append(dict(observation=row["observation"],taxon=row["taxon"],partition=row["partition"],baseline=baseline(row["logits"]),
        retrieval=first if agree else None,similarity=min(v[0][1] for v in views),margin=min(v[0][1]-v[1][1] for v in views),classifier_top3_support=support))

def prediction(row,policy):
    if policy["kind"]=="baseline":return row["baseline"]
    if policy["kind"]=="none":return None
    if not row["retrieval"] or row["similarity"]<policy["similarity"] or row["margin"]<policy["margin"]:return None
    if policy["kind"]=="hybrid" and not row["classifier_top3_support"]:return None
    return row["retrieval"]

def metrics(rows,policy):
    good=wrong=unknown_wrong=known=unknown=0
    for row in rows:
        pred=prediction(row,policy)
        is_known=row["taxon"] in KNOWN.values()
        if is_known:known+=1
        else:unknown+=1
        if pred:
            if pred==row["taxon"] and is_known:good+=1
            else:
                wrong+=1
                if not is_known:unknown_wrong+=1
    return dict(n=len(rows),known=known,unknown=unknown,correct=good,wrong=wrong,unknown_false_accepts=unknown_wrong,abstain=len(rows)-good-wrong)
cal=[r for r in scored if r["partition"]=="calibration"]
test=[r for r in scored if r["partition"]=="test"]
policies=[dict(kind="baseline"),dict(kind="none")]
for kind in ("retrieval","hybrid"):
    for sim in np.arange(.40,.951,.025):
        for margin in (0.,.025,.05,.075,.10,.15,.20):
            policies.append(dict(kind=kind,similarity=round(float(sim),3),margin=margin))
# Select only on calibration: zero false suggestions, maximize correct coverage;
# prefer corroboration and stricter thresholds on ties. Never tune on test.
eligible=[p for p in policies if p["kind"] in ("retrieval","hybrid") and metrics(cal,p)["wrong"]==0]
best=max(eligible,key=lambda p:(metrics(cal,p)["correct"],p["kind"]=="hybrid",p["similarity"],p["margin"]),default=dict(kind="none"))
if metrics(cal,best)["correct"]==0:best=dict(kind="none")
report=dict(model_sha256=MODEL_HASH,feature_name=FEATURE,feature_dimension=768,derived_sha256=sha256(derived.read_bytes()).hexdigest(),supported=supported,planned=len(records),usable=len(valid),failures=failures,
    policies=dict(baseline=dict(calibration=metrics(cal,policies[0]),test=metrics(test,policies[0])),selected=dict(policy=best,calibration=metrics(cal,best),test=metrics(test,best))),
    cases=[{**r,"selected":prediction(r,best)} for r in scored],
    limitations=["small observer-disjoint public cohort; pretrained-data overlap unknown","no ZZ validation cohort; not supported in retrieval","reference-derived labels; not all cultivars or field conditions","threshold selection is calibration, not a calibrated probability"])
(ROOT/"retrieval_results.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps({k:v for k,v in report.items() if k not in ("cases","failures")},indent=2),flush=True)
