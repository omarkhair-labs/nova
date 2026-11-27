"""Second, separately held-out DINOv2 classification study.
First cohort is now entirely training; its previous test results are NOT new evidence.
Learn class-balanced ridge heads including explicit confusable and nonplant classes.
Choose the complete policy only on the new calibration partition.
"""
import argparse
from hashlib import sha256
import json
from pathlib import Path
import numpy as np
import onnxruntime as ort
from PIL import Image,ImageOps,ImageChops,ImageStat

p=argparse.ArgumentParser(description=__doc__)
p.add_argument("cache",type=Path)
args=p.parse_args()
ROOT=args.cache.resolve()
if ROOT.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
BASE_HASH="63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e"
FEATURE_HASH="1c568d631b86eed16f53a33911f0f4db1fa4b2b1a8fa197087d539dafaa4edc3"
CONFIG_HASH="84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e"
FEATURE="/Div_output_0"
KNOWN={"Monstera deliciosa":"monstera-deliciosa","Ficus elastica":"rubber-plant","Crassula ovata":"jade-plant","Epipremnum aureum":"pothos-golden","Aloe vera":"aloe-vera"}
ORIGINAL={**{v:k for k,v in KNOWN.items()},"zz-plant":"Zamioculcas zamiifolia"}
NOVEL={"Ficus microcarpa","Philodendron hederaceum","Haworthiopsis attenuata","Agave americana"}
for name,digest in [("species_dinov2_v12_int8.onnx",BASE_HASH),("research_embedding.onnx",FEATURE_HASH),("config.json",CONFIG_HASH)]:
    assert sha256((ROOT/name).read_bytes()).hexdigest()==digest,name
cfg=json.loads((ROOT/"config.json").read_text())["species_head"]
labels=cfg["labels"]
old=json.loads((ROOT/"cohort.json").read_text())
fresh=json.loads((ROOT/"cohort_v2.json").read_text())
fresh+=json.loads((ROOT/"cohort_v2_extension.json").read_text())
assert len({r["observer"] for r in old+fresh})==len(old+fresh)
photo_ids=[p["id"] for r in old+fresh for p in r["photos"]]
assert len(set(photo_ids))==len(photo_ids)
cache=ROOT/"features";cache.mkdir(exist_ok=True)
session=None
def infer(photo,directory):
    global session
    path=directory/(str(photo["id"])+".jpg")
    digest=sha256(path.read_bytes()).hexdigest()
    with Image.open(path) as im:
        im=ImageOps.exif_transpose(im).convert("RGB");w,h=im.size
        if min(w,h)<128 or max(w,h)>5*min(w,h):raise ValueError("dimensions")
        thumb=im.resize((32,32),Image.Resampling.BILINEAR)
        dst=cache/(digest+".npz")
        if dst.exists():
            with np.load(dst,allow_pickle=False) as f:
                logits,embedding=f["logits"],f["embedding"]
        else:
            if session is None:
                o=ort.SessionOptions();o.intra_op_num_threads=1;o.inter_op_num_threads=1
                o.enable_cpu_mem_arena=False;o.enable_mem_pattern=False
                session=ort.InferenceSession(str(ROOT/"research_embedding.onnx"),o,providers=["CPUExecutionProvider"])
            scale=cfg["resize_shortest_edge"]/min(w,h)
            im=im.resize((round(w*scale),round(h*scale)),Image.Resampling.BILINEAR)
            x,y=(im.width-224)//2,(im.height-224)//2
            rgb=np.asarray(im.crop((x,y,x+224,y+224)),dtype=np.float32)/255
            pixels=((rgb-cfg["mean"])/cfg["std"]).astype(np.float32).transpose(2,0,1)[None]
            logits,embedding=session.run([cfg["output_name"],FEATURE],{cfg["input_name"]:pixels})
            logits,embedding=logits[0],embedding[0]
            norm=np.linalg.norm(embedding)
            if not .5<norm<1.5:raise ValueError("embedding norm")
            embedding=embedding/norm
            np.savez(dst,logits=logits,embedding=embedding)
    if logits.shape!=(110,) or embedding.shape!=(768,) or not np.isfinite(logits).all() or not np.isfinite(embedding).all():raise ValueError("tensor")
    if abs(float(np.linalg.norm(embedding))-1.)>.01:raise ValueError("norm")
    return logits,embedding,thumb,digest

seen=set()
failures=[]
def read_rows(rows,directory,tag):
    out=[]
    for i,r in enumerate(rows,1):
        try:
            views=[infer(p,directory) for p in r["photos"]]
            hashes={v[3] for v in views}
            if len(hashes)!=2 or hashes&seen:raise ValueError("duplicate photo")
            if max(ImageStat.Stat(ImageChops.difference(views[0][2],views[1][2])).mean)<5:raise ValueError("near duplicate")
            seen.update(hashes)
            out.append(dict(observation=r["observation"],taxon=r["taxon"],partition=r["partition"],vectors=np.stack([v[1] for v in views]),logits=np.stack([v[0] for v in views])))
        except (OSError,ValueError,RuntimeError) as e:
            failures.append(dict(observation=r["observation"],taxon=r["taxon"],partition=r["partition"],stage=tag,error=str(e)[:100]))
        if i%10==0 or i==len(rows):print(tag,i,len(rows),"usable",len(out),flush=True)
    return out

train=read_rows(old,ROOT/"photos","train")
classes=sorted({r["taxon"] for r in train})
assert set(KNOWN)<=set(classes) and len(classes)>=10
X=np.vstack([r["vectors"] for r in train]).astype(np.float64)
y=np.repeat([classes.index(r["taxon"]) for r in train],2)
counts=np.bincount(y,minlength=len(classes))
assert all(counts>=4)
# Equal total weight for each class; individual observers each have two views.
weights=np.sqrt(len(y)/(len(classes)*counts[y]))
X=np.column_stack((X,np.ones(len(X))))
Xw=X*weights[:,None]
Y=np.eye(len(classes))[y]*weights[:,None]
gram=Xw@Xw.T
heads={}
for alpha in (.05,.2,1.,4.):
    heads[alpha]=(Xw.T@np.linalg.solve(gram+alpha*np.eye(len(Xw)),Y)).astype(np.float32)
refs={t:np.stack([r["vectors"] for r in train if r["taxon"]==t]) for t in KNOWN}
def retrieval(v):
    return max(KNOWN,key=lambda t: float(np.mean(np.sort(np.max(refs[t]@v,axis=1))[-2:])))
def score(row,alpha):
    vectors=row["vectors"]
    features=np.column_stack((vectors,np.ones(len(vectors),dtype=np.float32)))
    outputs=features@heads[alpha]
    ranks=np.argsort(outputs,axis=1)[:,::-1]
    choice=classes[ranks[0,0]] if ranks[0,0]==ranks[1,0] else None
    if choice not in KNOWN:return dict(choice=None,score=-100.,margin=-100.,retrieval=False,original=False)
    return dict(choice=choice,score=float(min(outputs[i,ranks[i,0]] for i in range(2))),
        margin=float(min(outputs[i,ranks[i,0]]-outputs[i,ranks[i,1]] for i in range(2))),
        retrieval=all(retrieval(v)==choice for v in vectors),
        original=all(KNOWN[choice] in [labels[j] for j in np.argsort(l)[-3:]] for l in row["logits"]))
def baseline(row):
    out=[]
    for x in row["logits"]:
        p=np.exp(x-x.max());p/=p.sum();idx=np.argsort(p)[::-1]
        out.append(ORIGINAL.get(labels[idx[0]]) if p[idx[0]]>=.85 and p[idx[0]]-p[idx[1]]>=.20 else None)
    return out[0] if out[0]==out[1] else None

def summarize(rows,policy):
    result=dict(n=len(rows),known=0,unknown=0,novel_unknown=0,correct=0,wrong=0,unknown_false_accepts=0,novel_false_accepts=0,abstain=0)
    cases=[]
    for r in rows:
        if policy["kind"]=="baseline":pred=baseline(r)
        elif policy["kind"]=="none":pred=None
        else:
            q=r["scores"][policy["alpha"]]
            pred=q["choice"] if q["score"]>=policy["score"] and q["margin"]>=policy["margin"] and (not policy["retrieval"] or q["retrieval"]) and (not policy["original"] or q["original"]) else None
        name=r["taxon"];known=name in KNOWN
        result["known" if known else "unknown"]+=1
        if name in NOVEL:result["novel_unknown"]+=1
        if pred is None:result["abstain"]+=1
        elif known and pred==name:result["correct"]+=1
        else:
            result["wrong"]+=1
            if not known:result["unknown_false_accepts"]+=1
            if name in NOVEL:result["novel_false_accepts"]+=1
        cases.append(dict(observation=r["observation"],taxon=name,prediction=pred))
    return result,cases

cal=read_rows([r for r in fresh if r["partition"]=="calibration"],ROOT/"photos_v2","calibration")
for r in cal:r["scores"]={a:score(r,a) for a in heads}
candidates=[]
for alpha in heads:
    for threshold in (.15,.25,.35,.45,.55,.65,.75,.85):
        for margin in (0.,.05,.10,.15,.20,.30):
            for ref,orig in ((False,False),(True,False),(False,True),(True,True)):
                policy=dict(kind="ridge",alpha=alpha,score=threshold,margin=margin,retrieval=ref,original=orig)
                m,_=summarize(cal,policy)
                if m["wrong"]==0:candidates.append((m["correct"],int(ref)+int(orig),margin,threshold,alpha,policy))
best=max(candidates,key=lambda t:t[:-1])[-1] if candidates and max(t[0] for t in candidates)>0 else dict(kind="none")
locked=ROOT/"ridge_locked_policy.json"
locked.write_text(json.dumps(best,indent=2),encoding="utf-8")
print("LOCKED",best,flush=True)
# Only now process/score the untouched test partition.
test=read_rows([r for r in fresh if r["partition"]=="test"],ROOT/"photos_v2","test")
for r in test:r["scores"]={a:score(r,a) for a in heads}
base_cal,_=summarize(cal,dict(kind="baseline"));base_test,base_cases=summarize(test,dict(kind="baseline"))
new_cal,_=summarize(cal,best);new_test,new_cases=summarize(test,best)
# Predeclared experimental promotion gate, not a production approval.
passed=(best["kind"]=="ridge" and new_test["known"]>=15 and new_test["unknown"]>=20 and new_test["novel_unknown"]>=9 and new_test["wrong"]==0 and new_test["correct"]>=base_test["correct"]+3)
report=dict(model_sha256=BASE_HASH,embedding_sha256=FEATURE_HASH,train_observations=len(train),training_classes=classes,train_class_photos=dict(zip(classes,counts.tolist())),fresh_planned=len(fresh),failures=failures,selected_policy=best,baseline=dict(calibration=base_cal,test=base_test),ridge=dict(calibration=new_cal,test=new_test),promotion_gate_passed=passed,baseline_cases=base_cases,selected_cases=new_cases,limitations=["Prior cohort retired entirely to training; only fresh test is evaluation.","Observer and exact-byte disjoint, possible pretrained-data overlap and visually similar images.","Five target species; small public-data cohort, not operational recognition validation.","Ridge outputs are scores, not calibrated probabilities."])
(ROOT/"ridge_results.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
if best["kind"]=="ridge":
    artifact=dict(schema="plantupia-research-ridge-v1",base_sha256=BASE_HASH,embedding_sha256=FEATURE_HASH,feature_dimension=768,classes=classes,known=list(KNOWN),policy=best,weights=heads[best["alpha"]].tolist())
    # Retain the reference data only if selected policy needs it; never commit these.
    if best["retrieval"]:artifact["references"]={k:v.tolist() for k,v in refs.items()}
    (ROOT/"ridge_head.json").write_text(json.dumps(artifact,separators=(",",":")),encoding="utf-8")
print(json.dumps({k:v for k,v in report.items() if not k.endswith("_cases")},indent=2),flush=True)
