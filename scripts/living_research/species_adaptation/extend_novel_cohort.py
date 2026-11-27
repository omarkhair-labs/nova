"""Availability-only replacement for missing Haworthiopsis, before any v2 inference.
Agave americana is a new rosette lookalike taxon, held out entirely from training.
"""
from pathlib import Path
from hashlib import sha256
from io import BytesIO
import json,time,requests
from PIL import Image,ImageOps
import argparse
_cache_parser=argparse.ArgumentParser(description=__doc__)
_cache_parser.add_argument("cache",type=Path)
ROOT=_cache_parser.parse_args().cache.resolve()
if ROOT.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
ROOT.mkdir(parents=True,exist_ok=True)
old=json.loads((ROOT/"cohort.json").read_text())
fresh=json.loads((ROOT/"cohort_v2.json").read_text())
assert not any(r["taxon"]=="Haworthiopsis attenuata" for r in fresh)
assert not (ROOT/"ridge_locked_policy.json").exists() and not (ROOT/"ridge_results.json").exists()
used={r["observer"] for r in old+fresh}
def get(url,**kwargs):
    for i in range(3):
        try:r=requests.get(url,timeout=(10,25),**kwargs);r.raise_for_status();return r
        except requests.RequestException:
            if i==2:raise
            time.sleep(i+1)
out=[]
for page in range(1,5):
    rows=get("https://api.inaturalist.org/v1/observations",params=dict(taxon_name="Agave americana",quality_grade="research",photo_license="cc-by,cc0",id_below=405000000,per_page=100,page=page,order_by="id",order="desc")).json()["results"]
    for o in rows:
        if o["user"]["id"] in used or o.get("taxon",{}).get("name")!="Agave americana":continue
        photos=[p for p in o["photos"] if p.get("license_code") in ("cc0","cc-by") and p.get("url")]
        if len(photos)<2:continue
        selected=[dict(id=p["id"],url=p["url"].replace("/square.","/medium."),license=p["license_code"],attribution=p.get("attribution")) for p in photos[:2]]
        out.append(dict(observation=o["id"],observer=o["user"]["id"],taxon="Agave americana",partition="test",photos=selected))
        used.add(o["user"]["id"])
        if len(out)==4:break
    if len(out)==4 or len(rows)<100:break
    time.sleep(.5)
(ROOT/"cohort_v2_extension.json").write_text(json.dumps(out,indent=2),encoding="utf-8")
for row in out:
    for p in row["photos"]:
        dst=ROOT/"photos_v2"/(str(p["id"])+".jpg")
        data=get(p["url"]).content
        assert 1024<=len(data)<=2000000
        with Image.open(BytesIO(data)) as im:
            im=ImageOps.exif_transpose(im);assert min(im.size)>=128 and max(im.size)<=5*min(im.size)
        dst.write_bytes(data)
print("NOVEL_EXTENSION",len(out),"observations",flush=True)
