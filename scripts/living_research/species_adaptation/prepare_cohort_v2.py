"""Freeze a new evaluation cohort after retiring the first cohort to training.
Public CC-BY/CC0 images only; no observer crosses from old or new partitions.
"""
import concurrent.futures
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
import time
import requests
from PIL import Image,ImageOps

import argparse
_cache_parser=argparse.ArgumentParser(description=__doc__)
_cache_parser.add_argument("cache",type=Path)
ROOT=_cache_parser.parse_args().cache.resolve()
if ROOT.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
ROOT.mkdir(parents=True,exist_ok=True)
CACHE=ROOT/"photos_v2"
CACHE.mkdir(exist_ok=True)
KNOWN=["Monstera deliciosa","Ficus elastica","Crassula ovata","Epipremnum aureum","Aloe vera"]
SEEN_NEG=["Monstera adansonii","Aloe ferox","Ficus benjamina","Pinus pinea","Felis catus","Danaus plexippus"]
NOVEL_NEG=["Ficus microcarpa","Philodendron hederaceum","Haworthiopsis attenuata"]
OLD=json.loads((ROOT/"cohort.json").read_text(encoding="utf-8"))
USED={r["observer"] for r in OLD}
OLD_IDS={r["observation"] for r in OLD}|{413420520,410412033,383528659,410531874,365827536,402548560,415395832,415238064,416536432,410858366,344852928,321816909,416786734,416786634,416771035,416769464}
def get(url,**kwargs):
    for i in range(3):
        try:
            r=requests.get(url,timeout=(10,25),**kwargs);r.raise_for_status();return r
        except requests.RequestException:
            if i==2:raise
            time.sleep(i+1)
plan=ROOT/"cohort_v2.json"
if not plan.exists():
    rows=[]
    for name in KNOWN+SEEN_NEG+NOVEL_NEG:
        partitions=["calibration"]*3+["test"]*4 if name in KNOWN else ["calibration"]*3+["test"]*3 if name in SEEN_NEG else ["test"]*4
        chosen=[]
        for page in range(1,5):
            data=get("https://api.inaturalist.org/v1/observations",params=dict(taxon_name=name,quality_grade="research",photo_license="cc-by,cc0",id_below=405000000,per_page=100,page=page,order_by="id",order="desc")).json()["results"]
            for o in data:
                if o["id"] in OLD_IDS or o["user"]["id"] in USED or o.get("taxon",{}).get("name")!=name:continue
                p=[p for p in o["photos"] if p.get("license_code") in ("cc0","cc-by") and p.get("url")]
                if len(p)<2:continue
                chosen.append(dict(observation=o["id"],observer=o["user"]["id"],taxon=name,partition=partitions[len(chosen)],photos=[dict(id=x["id"],url=x["url"].replace("/square.","/medium."),license=x["license_code"],attribution=x.get("attribution")) for x in p[:2]]))
                USED.add(o["user"]["id"])
                if len(chosen)==len(partitions):break
            if len(chosen)==len(partitions) or len(data)<100:break
            time.sleep(.5)
        rows.extend(chosen)
        print("planned_v2",name,len(chosen),"/",len(partitions),flush=True)
    plan.write_text(json.dumps(rows,indent=2),encoding="utf-8")
else:rows=json.loads(plan.read_text(encoding="utf-8"))
assert len(set(r["observer"] for r in OLD+rows))==len(OLD+rows)
def acquire(photo):
    path=CACHE/(str(photo["id"])+".jpg")
    try:
        if not path.exists():
            body=get(photo["url"]).content
            if not 1024<=len(body)<=2000000:raise ValueError("size")
            with Image.open(BytesIO(body)) as im: im.verify()
            path.write_bytes(body)
        body=path.read_bytes()
        with Image.open(BytesIO(body)) as im:
            im=ImageOps.exif_transpose(im)
            if min(im.size)<128 or max(im.size)>5*min(im.size):raise ValueError("dimensions")
        return photo["id"],dict(sha256=sha256(body).hexdigest(),error=None)
    except Exception as e:return photo["id"],dict(sha256=None,error=type(e).__name__)
photos={p["id"]:p for row in rows for p in row["photos"]}
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    manifest={}
    for i,(key,value) in enumerate(pool.map(acquire,photos.values()),1):
        manifest[key]=value
        if i%20==0 or i==len(photos):print("download_v2",i,len(photos),"valid",sum(v["error"] is None for v in manifest.values()),flush=True)
(ROOT/"download_manifest_v2.json").write_text(json.dumps(manifest,indent=2),encoding="utf-8")
print("READY_V2",len(rows),len(photos),flush=True)
