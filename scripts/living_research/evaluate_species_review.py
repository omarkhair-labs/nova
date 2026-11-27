"""Evaluate the shipped two-view decision on separate public observations.

Run with the QA Django environment plus ONNX Runtime and a pinned model directory.
Images and per-observation results stay in the supplied local cache.
"""
import argparse
import json
import os
import sys
import time
from pathlib import Path

import requests

from screen_houseplants import OBSERVATIONS

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "backend"))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "nova_backend.settings")
import django
django.setup()
from plants.species_recognition import KNOWN, candidate, load_engine
from plants.living_vision import _open


def cached(path, url, params=None):
    if not path.exists():
        for attempt in range(3):
            try:
                result = requests.get(url, params=params, timeout=25)
                result.raise_for_status()
                path.write_bytes(result.content)
                break
            except requests.RequestException:
                if attempt == 2:
                    raise
                time.sleep(2 ** attempt)
        time.sleep(.5)
    return path.read_bytes()


def main():
    args = argparse.ArgumentParser()
    args.add_argument("model", type=Path)
    args.add_argument("cache", type=Path)
    opts = args.parse_args()
    opts.cache.mkdir(parents=True, exist_ok=True)
    engine = load_engine(opts.model)
    records = []
    for taxon in [*KNOWN.values(), "Monstera adansonii", "Aloe ferox", "Ficus benjamina", "Pinus pinea"]:
        # Fixed ordering and cut-off. Never condition inclusion on classifier output.
        data = cached(opts.cache / (taxon + ".json"), "https://api.inaturalist.org/v1/observations",
                      {"taxon_name": taxon, "quality_grade": "research", "photo_license": "cc-by,cc0",
                       "id_below": 417000000, "per_page": 100, "order_by": "id", "order": "desc"})
        observers = set()
        for obs in json.loads(data)["results"]:
            if obs["id"] in OBSERVATIONS or obs["taxon"]["name"] != taxon or obs["user"]["id"] in observers:
                continue
            photos = [p for p in obs["photos"] if p.get("license_code") in ("cc-by", "cc0")][:2]
            if len(photos) < 2:
                continue
            images = [_open(cached(opts.cache / (str(p["id"]) + ".jpg"), p["url"].replace("/square.", "/medium.")),
                            {"JPEG", "PNG", "WEBP"}).convert("RGB") for p in photos]
            result = candidate(images, engine)
            records.append(dict(observation=obs["id"], taxon=taxon, candidate=result, photos=[p["id"] for p in photos]))
            observers.add(obs["user"]["id"])
            if len(observers) == 6:
                break
        (opts.cache / "results.json").write_text(json.dumps(records, indent=2))
        batch = [r for r in records if r["taxon"] == taxon]
        print(taxon, len(batch), sum(r["candidate"] == taxon for r in batch),
              sum(r["candidate"] not in (None, taxon) for r in batch), flush=True)


if __name__ == "__main__":
    main()
