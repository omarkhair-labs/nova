"""Acquire versioned classifier artifacts, independently of the evaluation data."""
from pathlib import Path
from hashlib import sha256
import requests
import argparse
_cache_parser=argparse.ArgumentParser(description=__doc__)
_cache_parser.add_argument("cache",type=Path)
r=_cache_parser.parse_args().cache.resolve()
if r.is_relative_to(Path(__file__).resolve().parents[3]):
    raise ValueError("Use a private cache outside the repository.")
r.mkdir(parents=True,exist_ok=True)
base="https://huggingface.co/m3r1al/verdant-plant-model/resolve/239d591ffc13815a054492d3924966a76740dc01/"
for name, expected, limit in [
    ("config.json","84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e",200000),
    ("species_dinov2_v12_int8.onnx","63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e",25000000),
]:
    path=r/name
    if not path.exists():
        temp=path.with_suffix(".partial")
        with requests.get(base+name,stream=True,timeout=(15,45)) as response:
            response.raise_for_status()
            total=0
            with temp.open("wb") as f:
                for chunk in response.iter_content(262144):
                    total+=len(chunk)
                    if total>limit: raise ValueError("oversize artifact")
                    f.write(chunk)
        if sha256(temp.read_bytes()).hexdigest()!=expected: raise ValueError("artifact mismatch")
        temp.replace(path)
    assert sha256(path.read_bytes()).hexdigest()==expected
    print("verified",name,path.stat().st_size,flush=True)
