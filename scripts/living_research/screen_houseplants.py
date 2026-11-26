"""Compare a pinned DINOv2 head on the public two-view cohort used by the ViT screen.

Run with a local cache directory outside Git. No customer images or credentials.
"""
import argparse
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path

import numpy as np
import onnxruntime as ort
from PIL import Image, ImageOps
import requests


ROOT = "https://huggingface.co/m3r1al/verdant-plant-model/resolve/239d591ffc13815a054492d3924966a76740dc01/"
MODEL = "species_dinov2_v12_int8.onnx"
OBSERVATIONS = (413420520, 410412033, 383528659, 410531874, 365827536, 402548560,
                415395832, 415238064, 416536432, 410858366, 344852928, 321816909,
                416786734, 416786634, 416771035, 416769464)
LABELS = {"Monstera deliciosa": "monstera-deliciosa", "Ficus elastica": "rubber-plant",
          "Crassula ovata": "jade-plant", "Epipremnum aureum": "pothos-golden",
          "Aloe vera": "aloe-vera", "Zamioculcas zamiifolia": "zz-plant"}


def fetch(cache, name, url, digest=None):
    path = cache / name
    if not path.exists():
        result = requests.get(url, timeout=45)
        result.raise_for_status()
        body = result.content
        if digest and sha256(body).hexdigest() != digest:
            raise ValueError(f"Unexpected artifact: {name}")
        path.write_bytes(body)
    body = path.read_bytes()
    if digest and sha256(body).hexdigest() != digest:
        raise ValueError(f"Unexpected cached artifact: {name}")
    return body


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("cache", type=Path)
    cache = parser.parse_args().cache
    cache.mkdir(exist_ok=True, parents=True)
    config = json.loads(fetch(cache, "config.json", ROOT+"config.json",
                              "84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e"))["species_head"]
    fetch(cache, MODEL, ROOT+MODEL, "63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e")
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    session = ort.InferenceSession(str(cache/MODEL), options, providers=["CPUExecutionProvider"])

    def predict(photo):
        im = ImageOps.exif_transpose(Image.open(BytesIO(photo))).convert("RGB")
        size = config["resize_shortest_edge"]
        w, h = im.size
        im = im.resize((round(w*size/min(w,h)), round(h*size/min(w,h))), Image.Resampling.BILINEAR)
        x, y = (im.width-224)//2, (im.height-224)//2
        rgb = np.asarray(im.crop((x,y,x+224,y+224)), dtype=np.float32)/255.
        pixels = ((rgb-config["mean"])/config["std"]).astype(np.float32).transpose(2,0,1)[None]
        logits = session.run([config["output_name"]], {config["input_name"]: pixels})[0][0]
        exp = np.exp(logits-logits.max())
        probs = exp/exp.sum()
        a,b = np.argsort(probs)[-2:][::-1]
        return {"label": config["labels"][a], "score": float(probs[a]), "margin": float(probs[a]-probs[b])}

    rows = []
    for obs_id in OBSERVATIONS:
        obs = json.loads(fetch(cache, f"{obs_id}.json",
                               f"https://api.inaturalist.org/v1/observations/{obs_id}"))["results"][0]
        views = []
        for photo in obs["photos"]:
            if photo.get("license_code") not in ("cc0", "cc-by"):
                continue
            body = fetch(cache, str(photo["id"])+".jpg", photo["url"].replace("/square.", "/medium."))
            views.append({**predict(body), "photo_id": photo["id"], "sha256": sha256(body).hexdigest()})
            if len(views) == 2:
                break
        accepted = (len(views) == 2 and views[0]["label"] == views[1]["label"]
                    and all(v["score"] >= .85 and v["margin"] >= .20 for v in views))
        species = obs["taxon"]["name"]
        rows.append(dict(observation=obs_id, species=species, expected=LABELS.get(species),
                         views=views, accepted=accepted))
    (cache/"screen.json").write_text(json.dumps(rows, indent=2))
    for row in rows:
        print(row["observation"], row["species"], len(row["views"]), row["accepted"],
              [v["label"] for v in row["views"]])


if __name__ == "__main__":
    main()
