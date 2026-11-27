"""Inspection steps grounded in an already accepted, private colour observation.

No diagnosis, treatment, verified identity, or new biological measurement.
The old outline's source digest must match the retained original.
"""
from hashlib import sha256
from math import isfinite

from .vision_evidence import inspect_photo

METHOD = "reviewed-outline-inspection-01"
PARTS = {"plant", "leaf", "stem", "flower", "exposed_root", "visible_damage"}


def review_outline(trace, raw):
    part = trace.annotation.get("part")
    if part not in PARTS:
        raise ValueError("Select a recorded plant region, not a pot or soil outline.")
    if not trace.source_sha256 or sha256(raw).hexdigest() != trace.source_sha256:
        raise ValueError("The outline no longer matches the retained photograph.")
    quality = inspect_photo(raw)
    patches = trace.measurements.get("visible_patches") or []
    # Consume only the bounded output of the existing colour analysis. Do not
    # recompute an illness score, trust client patch values, or infer absence.
    evidence = []
    if isinstance(patches, list):
        for patch in patches[:8]:
            if not isinstance(patch, dict):
                continue
            appearance = patch.get("appearance")
            fraction = patch.get("region_fraction")
            if (appearance in {"yellow_like", "brown_like"} and type(fraction) in (int,float)
                    and isfinite(fraction) and .004 <= fraction <= .45):
                evidence.append({"appearance": appearance, "fraction_of_outline": fraction})
    result = {"trace_id": trace.pk, "operator_label": part, "scope": "uncalibrated_visible_appearance",
              "capture_flags": quality["capture_flags"], "patches": evidence,
              "health": "not_assessed", "cause": "not_inferred"}
    if quality["status"] != "no_quality_flags":
        result["status"] = "review_capture"
        steps = ["Retake this part in even, indirect light with clear detail before interpreting its colour."]
    elif not evidence:
        result["status"] = "no_local_patch_candidate"
        steps = ["No local yellow or brown patch was selected by this colour rule. Inspect the part directly; this does not rule out a problem."]
    else:
        result["status"] = "visible_patch_candidate"
        steps = ["Inspect the marked colour patches directly under even light. Check whether the pattern is a natural marking, reflection or visible change.",
                 "Compare the same part with an earlier photograph taken in comparable light."]
        if part in {"plant", "leaf", "visible_damage"}:
            steps.append("Inspect both leaf surfaces and note whether the appearance is at an edge, tip or within the blade.")
        else:
            steps.append("Photograph the same marked section from a second angle and note its texture and distribution.")
    return result, [{"hypothesis": None, "inspection": step} for step in steps]
