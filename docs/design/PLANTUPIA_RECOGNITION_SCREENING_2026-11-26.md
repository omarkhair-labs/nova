# Native recognition: screening evidence, not a release approval

The camera and discovery flow must not present an unvalidated species guess as a diagnosis or confirmed identity.

## Models screened on the QA PC

- [PlantNet300K ResNet18 INT8](https://huggingface.co/cpoisson/plantnet300k-resnet18), revision `d15cde6e60e07c40369f597467e63bb99a8034b9`. ONNX SHA-256 `0523d1830c9daaf048ddbe9bfb27f2cb148f64baa39564ec4f4d1a0d45923210`. Output: 1081 classes. Thirteen of fourteen queried indoor-plant label strings were absent (including synonym queries). A synthetic checkerboard received a softmax score of about .959 for Sedum album. Rejected for this product scope.
- [House-plant ViT INT8](https://huggingface.co/onnx-community/house-plant-image-detection-ONNX), revision `24d519d5ce1dd1a44b159b4e1bef012d01f30b95`. ONNX SHA-256 `d746a150317b670ed308c243b4aba46f1c0e69c5d1da68e7713b8aca72097c0b`. 47 classes; some are broad categories, not species. RGB 224x224 bilinear, channel values divided by 127.5 then minus one, NCHW; logits converted to softmax scores. CPU ONNX Runtime.

## Screening outcome

Twelve public iNaturalist research-grade observations for six covered taxa, four observations from two unrepresented taxa, and three synthetic controls. Each observation supplied two CC0 or CC-BY images; the controls used one generated image each. The covered taxa were Monstera deliciosa, Ficus elastica, Crassula ovata, Epipremnum aureum, Aloe vera and Zamioculcas zamiifolia. The unrepresented taxa were Quercus robur and Pinus sylvestris. No eligible dog observation was obtained. No private user photos were used.

Provisional two-view gate: agreement of the top class, each softmax score at least .85, each top-two margin at least .20. Zero of the sixteen public observations passed. Synthetic controls also failed the confidence gate. The observed scores are not calibrated probabilities. The model often ranked the correct indoor plant first but failed the acceptance gate. This is a small screening set, not a generalization or accuracy benchmark. Original images are not bundled. Exact observation identifiers: 413420520, 410412033, 383528659, 365827536, 410531874, 402548560, 415395832, 415238064, 416536432, 410858366, 344852928, 321816909, 416786734, 416786634, 416771035, 416769464. Per-photo source hashes, licenses and predictions retained in local research evidence.

## Decision

Do not enable either classifier in production or represent this as an automatic recognition feature. Compare a higher-capacity model and FP32 export on a separate, predeclared validation set; calibrate per class and include unknown taxa, nonplants, occlusions and poor lighting. Explicit user confirmation and an unknown result remain mandatory. Keep manual identity and photo-first creation working. No new network photo upload or model weights in the Android package from this study.

## DINOv2 comparison on the same public cohort

Ran the [pinned Verdant export](https://huggingface.co/m3r1al/verdant-plant-model/tree/239d591ffc13815a054492d3924966a76740dc01) using its versioned configuration and manifest. They describe a 110-class DINOv2-small head even though the bundle README still describes MobileViT. The 23,889,401-byte INT8 artifact had SHA-256 `63fc7e20c9c6fd287dfa38322fa27d0f1c4f8cd84cd26c8bbad48685505a933e`. Reproduce with `scripts/living_research/screen_houseplants.py <private-cache-dir>`. This uses public images only.

On the twelve covered observations, the correct label ranked first in 16 of 24 photos and in both views of seven observations. Zero of twelve passed the previously specified two-view score and margin gate. None of four out-of-scope observations passed either. A pine was labelled dracaena in both views. The validation cohort is small, with possible overlap with iNaturalist training data. No operational identification or confidence calibration follows from these results. Maintain the unknown path, manual selection and separately verified botanical identity.
