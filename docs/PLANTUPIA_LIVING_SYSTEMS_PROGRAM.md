# Plantupia Living Systems — integrated R&D program

Status: research, not a production promise. Existing integrated product PR #253 and design PR #330 are preserved. No hardware purchases. Keep code on the research branch until reviewed.

## One data spine
Existing Space / PlantUnit / BotanicalSpecies / PlantEvent records remain authoritative. Proposed new entities after review: capture session (consent, camera, pose, timestamp), evidence (image hash, region mask, model/version, observations), hypothesis (support and contradiction), specialist review, room revision (coordinates, measurement method, uncertainty), plant placement revision. Authorize every object through existing space membership; no public sharing without explicit consent. Images private by default, remove EXIF, bound upload dimensions, isolate compute, implement deletion and retention. Do not use client photos for model training without specific permission.

## Engineering tracks and acceptance criteria
1. Capture: guided whole plant / leaf / soil photos, quality gate, scale/color reference where required. Offline baseline exists; test across real phones and lighting.
2. Vision: compare prompted segmentation (e.g. SAM family) and lightweight on-device detector; benchmark plants/leaves/pots/soil masks with expert labels; evaluate occlusion and background false positives. Color-only mask baseline exists, not a detector. Verify weights license, hardware and runtime cost.
3. Phenotyping: region color/shape, projected area, leaf count; calibrated scale and matched pose before physical units. RGB region baseline exists; evaluate color stability against repeated photos. Never present green percentage as health.
4. Health: extract visible symptom observations, alternatives, missing evidence, botanical context; specialist review with no automatic treatment. Evaluate unsupported claims, abstention, precision/recall by species and lighting. No trained health model yet.
5. Time: capture matching, registered images, longitudinal trends, provenance and correction. Guarded color delta exists; registration and growth measurements pending.
6. Space: manual metric room baseline exists; add ARCore supported-device capture, camera poses/planes/depth confidence, tracking-loss recovery, scale validation against tape measurements. Compare photogrammetry with COLMAP for offline reconstruction; separate visual mesh/splat from semantic objects and metric geometry. Support manual fallback.
7. Environment: measured geometry, window orientation, glazing, obstructions, season/time/location; daylight simulation with stated assumptions, compare against reference readings when available; never present RGB exposure or window distance as lux. Distance baseline exists; simulation pending.
8. Twin: versioned spatial graph links actual plant IDs to evidence and service history; 2D/3D views, update events and permissions, stale-data labels. Offline snapshot exists, not a live synchronized twin.
9. Product: native Android guided scan, evidence review, specialist queue, room capture and twin viewer, company operations handoff, accessibility/Arabic/English. Add APIs, migrations, auth tests and per-feature flags only after data contract review.
10. Evaluation: authorized pilot images/rooms, held-out plant and site splits, repeatability, model/provider cost, security, device validation, specialist acceptance. Never infer general clinical/botanical accuracy from synthetic tests.

## References
- ARCore depth and device support: https://developers.google.com/ar/develop/depth and https://developers.google.com/ar/devices
- COLMAP reconstruction: https://colmap.github.io/faq.html
- Plant phenotyping imaging review: https://doi.org/10.3390/s141120078
- RGB stress inference limitations: https://pmc.ncbi.nlm.nih.gov/articles/PMC12944259/

## Execution gate
Run isolated offline tests with Pillow, then existing backend/Android CI. No production migrations, model API calls, deployments or Play publishing. Real-data consent, specialist time and supported phones are required to close the applied validation gate.
