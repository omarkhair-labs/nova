# Plantupia — frozen visual features and supervised species adaptation (2026-11-27)

**Decision: research only. Neither experimental decision is promoted.** No change to the existing photo identification endpoint, its strict two-view gate, manual selection, or verified identity.

## Actual experiments

The pinned Verdant DINOv2 INT8 export was verified by SHA-256. Its ONNX graph exposes a 768-dimensional normalized visual representation immediately before the classifier head (`/Div_output_0`, concatenated class token and mean patch features). Adding this existing graph value to the output creates the research model; no backbone weights are changed. The original logits remain available. The first study compared reference similarity with the original classifier on separate reference/calibration/test observations. There were 105 observations, 210 readable public photos, globally distinct observer IDs and two distinct photos per observation. Zamioculcas had only three usable reference observations and no eligible calibration/test observations; retrieval therefore evaluated five target species. Six negative taxa included visually similar plants, pine, cats and butterflies. Selection used calibration only.

| First test (44 observations) | Correct of 20 target | False accept of 24 outside scope |
| --- | ---: | ---: |
| Original two-view gate | 3 | 0 |
| Reference retrieval | 10 | 4 |

The four false accepts involved Monstera adansonii (two), Aloe ferox and Ficus benjamina. The apparent improvement is not a safe replacement.

For the second study, the entire first cohort was explicitly retired to training. A class-balanced ridge head was fitted on the frozen 768-dimensional embeddings, including separate confusable and nonplant classes. Hyperparameters and acceptance rules were selected only on 33 new calibration observations, locked before scoring the new test photos. The new cohort excludes all first-cohort observers and exact photo hashes. Three novel negative taxa were held out entirely from training and calibration. Haworthiopsis attenuata had no eligible observation; Agave americana was substituted for availability **before any new inference**, retaining the original acceptance gate. One test observation had an unavailable image and was excluded for both methods.

| Fresh test (49 usable observations) | Correct of 20 target | False accept of 29 outside scope | False accept of 12 novel negatives |
| --- | ---: | ---: | ---: |
| Original two-view gate | 3 | 0 | 0 |
| Trained ridge + reference agreement | 12 | 5 | 5 |

The selected ridge policy used alpha 1.0, minimum score .35, margin .15, two-view agreement and nearest-reference corroboration. It correctly accepted 12/15 calibration targets with zero wrong suggestions, but failed on genuinely novel negatives. The predeclared experimental promotion gate required zero wrong suggestions, at least 15 known, 20 unknown and 9 novel unknown test observations, and at least three more correct suggestions than baseline. **It failed.** No threshold retuning on the inspected test set and no claim of calibrated probability or generalization. The learned layer is a small local research head, not a retrained DINOv2 backbone. Both tests are small; overlap with backbone pretraining is unknown. Availability-driven samples are not a field representative dataset.

## Reproduction (private cache)

Source: [pinned Verdant export](https://huggingface.co/m3r1al/verdant-plant-model/tree/239d591ffc13815a054492d3924966a76740dc01), public iNaturalist CC-BY/CC0 photos, attribution kept locally. All images, observer IDs, URLs, embeddings, trained heads, derived ONNX artifacts and detailed per-observation manifests stay outside Git. This public repository contains code and aggregate results only. Use an isolated Python 3.12 environment with onnx==1.20.1, onnxruntime==1.31.1, numpy==2.4.6, Pillow==13.0.0, requests==2.34.2, ml-dtypes==0.5.4 and the ONNX package dependencies. Run scripts in `scripts/living_research/species_adaptation`, each with an absolute private-cache directory argument, in this order:

1. fetch_model.py
2. prepare_cohort.py
3. evaluate_retrieval.py
4. prepare_cohort_v2.py
5. extend_novel_cohort.py (availability-only extension, applicable to the recorded cohort)
6. train_ridge_head.py

The cache preserves the actual cohort and hashes; repeating public API searches later is not guaranteed to yield the identical dataset. Keep the cache and photo attributions local; do not redistribute photo binaries or embeddings. See `docs/living-species-adaptation-benchmark.json` for the aggregate evidence and cohort manifest hashes.

## Next scientific gate

Collect a broader and more representative indoor plant dataset, including cultivar variation, matched lookalikes and held-out genera, ordinary phone captures and nonplants. Test a learned representation with better open-set rejection on entirely fresh observer-disjoint data. A shortlist is not a confirmed identity. Preserve the manual path and unknown result; no production change until sufficiently validated.
