"""Predeclared observer-disjoint, public two-view indoor-plant retrieval cohort.
Do not publish images or observer identifiers. No private photo use.
"""
import concurrent.futures
import json
from hashlib import sha256
from pathlib import Path
from io import BytesIO
import time
import requests
from PIL import Image, ImageOps

import argparse
_cache_parser=argparse.ArgumentParser(description=__doc__)
_cache_parser.add_argument("cache",type=Path)
ROOT=_cache_parser.parse_args().cache.resolve()
if ROOT.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
ROOT.mkdir(parents=True,exist_ok=True)
CACHE=ROOT/"photos"
CACHE.mkdir(exist_ok=True)
KNOWN=["Monstera deliciosa","Ficus elastica","Crassula ovata","Epipremnum aureum","Aloe vera","Zamioculcas zamiifolia"]
UNKNOWN=["Monstera adansonii","Aloe ferox","Ficus benjamina","Pinus pinea","Felis catus","Danaus plexippus"]
# Different fixed historical range from the existing review cohort. Some overlap
# with original model training is possible and cannot be ruled out.
MAX_ID=405000000
EXCLUDED={413420520,410412033,383528659,410531874,365827536,402548560,415395832,415238064,416536432,410858366,344852928,321816909,416786734,416786634,416771035,416769464}

def get(url, **kwargs):
    for i in range(3):
        try:
            res=requests.get(url, timeout=(10,25), **kwargs)
            res.raise_for_status()
            return res
        except requests.RequestException:
            if i==2: raise
            time.sleep(1+i)

plan=ROOT/"cohort.json"
if not plan.exists():
    used=set()
    records=[]
    for taxon in KNOWN+UNKNOWN:
        targets=["reference"]*5+["calibration"]*3+["test"]*4 if taxon in KNOWN else ["calibration"]*3+["test"]*4
        items=[]
        for page in (1,2):
            res=get("https://api.inaturalist.org/v1/observations",params=dict(
                taxon_name=taxon,quality_grade="research",photo_license="cc-by,cc0",
                id_below=MAX_ID,per_page=100,page=page,order_by="id",order="desc"))
            for obs in res.json()["results"]:
                if (obs["id"] in EXCLUDED or obs.get("taxon",{}).get("name")!=taxon
                        or obs["user"]["id"] in used): continue
                photos=[p for p in obs["photos"] if p.get("license_code") in ("cc-by","cc0") and p.get("url")]
                if len(photos)<2: continue
                chosen=[dict(id=p["id"],url=p["url"].replace("/square.","/medium."),
                    license=p["license_code"],attribution=p.get("attribution")) for p in photos[:2]]
                items.append(dict(observation=obs["id"],observer=obs["user"]["id"],taxon=taxon,
                    partition=targets[len(items)],photos=chosen))
                used.add(obs["user"]["id"])
                if len(items)==len(targets):break
            if len(items)==len(targets):break
            time.sleep(.5)
        records.extend(items)
        print("planned",taxon,len(items),"/",len(targets),flush=True)
    plan.write_text(json.dumps(records,indent=2),encoding="utf-8")
else:
    records=json.loads(plan.read_text(encoding="utf-8"))
assert len({r["observer"] for r in records})==len(records)

def acquire(photo):
    path=CACHE/(str(photo["id"])+".jpg")
    try:
        if not path.exists():
            body=get(photo["url"]).content
            if not 1024<=len(body)<=2_000_000:raise ValueError("invalid size")
            with Image.open(BytesIO(body)) as im:
                im.verify()
            path.write_bytes(body)
        body=path.read_bytes()
        with Image.open(BytesIO(body)) as im:
            im=ImageOps.exif_transpose(im)
            if min(im.size)<128 or max(im.size)>5*min(im.size):raise ValueError("unusable image dimensions")
        return photo["id"],sha256(body).hexdigest(),None
    except Exception as exc:
        return photo["id"],None,type(exc).__name__

photos={p["id"]:p for row in records for p in row["photos"]}
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    downloads={}
    for i,result in enumerate(pool.map(acquire,photos.values()),1):
        downloads[result[0]]=dict(sha256=result[1],error=result[2])
        if i%20==0 or i==len(photos): print("download",i,"/",len(photos),"valid",sum(x["error"] is None for x in downloads.values()),flush=True)
(ROOT/"download_manifest.json").write_text(json.dumps(downloads,indent=2),encoding="utf-8")
print("READY",len(records),len(photos),sum(x["error"] is None for x in downloads.values()),flush=True)
