# Plantupia Living Intelligence — research 01

Status: experimental, not a release decision. No hardware purchases, provider calls, database changes or customer uploads authorized by this slice.

## Existing foundation

The integrated product branch already contains Space, PlantUnit, BotanicalSpecies, CareGuideVersion and PlantEvent records, server-scoped access and a read-only, text-only assistant (backend/plants/assistant.py). Pillow is already a dependency. Keep these authorities and the current Android identity; do not duplicate them.

## Question

Can ordinary phone photos provide repeatable evidence about an individual living plant, and can that evidence accumulate into a spatial record of an installed space? We distinguish image observations from human reports, measurements, hypotheses and expert decisions. A photo alone cannot establish internal soil moisture, illuminance in lux, nutrient levels, disease cause or health score.

## Slice 01: capture quality

The offline Python module vision_evidence.py computes image-level exposure/detail proxies with provisional thresholds, size limits and explicit unknowns. It neither locates plants nor measures the environment. No endpoint or mobile UI is exposed. Unit tests use synthetic images, which test code behavior only, not botanical accuracy.

## Slice 02: real evidence before model selection

With company approval and customer consent, collect a small initial cohort (proposed 10 plants across 3 ornamental species, 3 views per visit: whole plant, affected leaf, soil surface; repeat across 3 visits). Record plant identity, time, capture conditions, and specialist observations; label visible features independently of cause. Retain negative, ambiguous and poor-quality examples. Use only authorized storage; do not commit private photos or identifying records. Split evaluation by plant and installation, not adjacent frames. This cohort is a feasibility pilot, not enough to validate general diagnostic accuracy.

Compare segmentation and multimodal observation baselines on the same held-out cases. Measure visible-feature precision/recall, unsupported-claim rate, abstention, latency, cost and expert disagreement. No disease classifier release until representative validation and specialist approval. Store provenance, model version, image reference, observation region, alternative explanations and review state; no automated care event writes.

## Slice 03: spatial pilot

For one approved room, record measured or supplied dimensions, plant identity and position, window orientation and explicit manual observations. Start with a 2D spatial record, then compare ARCore depth and photogrammetry on supported phones. Validate distances against reference measurements and support a manual fallback. A visual reconstruction is not a simulation or a live twin; add time-stamped state and explicit update mechanism first. Lighting estimates require geometry, orientation and assumptions; do not infer lux from RGB brightness.

## Public research

- Mohanty et al. (2016), Using Deep Learning for Image-Based Plant Disease Detection: https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2016.01419/full — controlled datasets do not establish indoor ornamental performance.
- Google ARCore Depth API: https://developers.google.com/ar/develop/depth — device support and motion requirements.
- Project working law: reality and evidence before tool choice; no new hardware simply to signal green technology.

## Release gates

Real-world validation, consent and data retention review, object-level authorization, provider cost/privacy review if applicable, existing backend and Android CI, specialist acceptance and explicit product approval. No integration or production deployment in this research slice.
