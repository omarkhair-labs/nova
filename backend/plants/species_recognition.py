"""Local two-view identification for opt-in field use. Model files stay outside Git."""
import json
from functools import lru_cache
from hashlib import sha256
from pathlib import Path

from django.conf import settings
from django.shortcuts import get_object_or_404
from PIL import Image, ImageChops, ImageStat
from rest_framework import serializers
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle

from .living_api import ResearchView
from .living_vision import _open
from .models import PlantEvent
from .species_library import visible_species
from .views import accessible_plants
from .vision_evidence import MAX_BYTES

MODEL = "species_dinov2_v12_int8.onnx"
KNOWN = {
    "monstera-deliciosa": "Monstera deliciosa", "rubber-plant": "Ficus elastica",
    "jade-plant": "Crassula ovata", "pothos-golden": "Epipremnum aureum",
    "aloe-vera": "Aloe vera", "zz-plant": "Zamioculcas zamiifolia",
}


@lru_cache(maxsize=1)
def load_engine(directory):
    import onnxruntime as ort
    root = Path(directory)
    model = (root / MODEL).read_bytes()
    config = (root / "config.json").read_bytes()
    if (sha256(model).hexdigest() != "63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e"
            or sha256(config).hexdigest() != "84b1483e4ba51b833fc40129d96d55dfea9f8a61378f8f5261ffcfc2ce4a7c2e"):
        raise ValueError("Unexpected identification model.")
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    return ort.InferenceSession(model, options, providers=["CPUExecutionProvider"]), json.loads(config)["species_head"]


# Only unambiguous labels contribute to a genus total. Unmapped labels retain
# their probability mass so a weak known label cannot win by renormalization.
GENUS = {label: name.split()[0] for label, name in KNOWN.items()}
GENUS.update(dict.fromkeys(("creeping-fig", "weeping-fig", "fiddle-leaf-fig", "ficus-ginseng"), "Ficus"))
GENUS.update(dict.fromkeys(("monstera-adansonii", "monstera-dubia"), "Monstera"))


def probabilities(image, engine):
    import numpy as np
    session, config = engine
    # Resize the shorter edge, then center crop, matching the pinned model.
    w, h = image.size
    size = config["resize_shortest_edge"]
    scale = size / min(w, h)
    resized = image.resize((round(w * scale), round(h * scale)), Image.Resampling.BILINEAR)
    x, y = (resized.width - 224) // 2, (resized.height - 224) // 2
    rgb = np.asarray(resized.crop((x, y, x + 224, y + 224)), dtype=np.float32) / 255.
    pixels = ((rgb - config["mean"]) / config["std"]).astype(np.float32).transpose(2, 0, 1)[None]
    logits = session.run([config["output_name"]], {config["input_name"]: pixels})[0][0]
    exp = np.exp(logits - logits.max())
    return dict(zip(config["labels"], (exp / exp.sum()).tolist()))


def review(images, engine):
    previews = [im.resize((32, 32), Image.Resampling.BILINEAR) for im in images]
    delta = ImageStat.Stat(ImageChops.difference(*previews)).mean
    if max(delta) < 5 or any(min(im.size) < 128 or max(im.size) > 5 * min(im.size) for im in images):
        return None, None
    species_votes = []
    genus_votes = []
    for image in images:
        scores = probabilities(image, engine)
        first, second = sorted(scores, key=scores.get, reverse=True)[:2]
        species_votes.append(KNOWN.get(first) if scores[first] >= .85 and scores[first] - scores[second] >= .20 else None)
        sums = {}
        for label, genus in GENUS.items():
            sums[genus] = sums.get(genus, 0.) + scores[label]
        winner = max(sums, key=sums.get)
        genus_votes.append(winner if sums[winner] >= .85 else None)
    species = species_votes[0] if species_votes[0] == species_votes[1] else None
    genus = genus_votes[0] if genus_votes[0] == genus_votes[1] else None
    return species, genus


def candidate(images, engine):
    return review(images, engine)[0]


class RecognitionThrottle(UserRateThrottle):
    scope = "plant_species_review"
    rate = "20/min"


class SpeciesRecognitionView(ResearchView):
    throttle_classes = [RecognitionThrottle]

    def post(self, request, plant_id=None, event_id=None):
        if event_id:
            event = get_object_or_404(PlantEvent, pk=event_id, plant_id=plant_id,
                                      plant__in=accessible_plants(request.user))
        if not settings.PLANTUPIA_SPECIES_MODEL_DIR:
            return Response({"detail": "Photo identification is unavailable."}, status=503)
        try:
            engine = load_engine(settings.PLANTUPIA_SPECIES_MODEL_DIR)
        except (ImportError, OSError, ValueError, RuntimeError):
            return Response({"detail": "Photo identification is unavailable."}, status=503)
        if event_id:
            raw = []
            if event.photo:
                with event.photo.open("rb") as photo:
                    raw.append(photo.read(MAX_BYTES + 1))
            photos = event.visit_photographs.exclude(part="soil").order_by("pk")
            raw.extend(bytes(p.jpeg) for p in photos[:2 - len(raw)])
        else:
            files = [request.FILES.get(key) for key in ("whole", "detail")]
            if any(f is None or f.size > MAX_BYTES for f in files):
                raise serializers.ValidationError("Choose two photographs of the same plant.")
            raw = [f.read(MAX_BYTES + 1) for f in files]
        if len(raw) != 2:
            raise serializers.ValidationError("Add a second plant view to this visit.")
        try:
            images = [_open(data, {"JPEG", "PNG", "WEBP"}).convert("RGB") for data in raw]
        except ValueError as exc:
            raise serializers.ValidationError("Choose two readable photographs.") from exc
        name, genus = review(images, engine)
        species = visible_species().filter(scientific_name__iexact=name).first() if name else None
        guides = visible_species().filter(scientific_name__istartswith=genus + " ")[:5] if genus else []
        return Response({"candidate": {"key": species.key if species else None, "scientific_name": name}
                         if name else None, "genus": genus,
                         "genus_guides": [{"key": s.key, "scientific_name": s.scientific_name} for s in guides],
                         "basis": "two_view_model_review"})
