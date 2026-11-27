# Living Lens — neural point outlines

Opt-in isolated research/QA, not production acceptance. The existing GrabCut and manual trace paths remain available. Enable Living Systems and set PLANTUPIA_NEURAL_OUTLINE_MODEL to the local reviewed model to offer an additional choice inside the private photograph inspection. First included point prompts MagicTouch; all other marks check the resulting polygon. Same signed actor/event/source/vertices/method review envelope. No trace until explicit acceptance. No diagnosis, identity, organ label or growth claim.

## Provenance

Google MediaPipe MagicTouch v1 [weights](https://storage.googleapis.com/mediapipe-models/interactive_segmenter/magic_touch/float32/1/magic_touch.tflite), 6,227,884 bytes, SHA-256 e24338a717c1b7ad8d159666677ef400babb7f33b8ad60c4d96db4ecf694cd25. [Model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MagicTouch.pdf), Apache-2.0. Python MediaPipe 0.10.32. Keep weights outside Git/APK. Install backend/requirements-neural-outline.txt with backend/requirements-living-vision.txt in isolated Python 3.12. No request-time download or model provider call. Use its single foreground float confidence mask >= .5. In this exact runtime the category-mask polarity was inverted relative to a nonzero-foreground assumption; the erroneous initial probe was discarded after a diagnostic confirmed the foreground confidence mask. No calibrated botanical confidence follows.

## Completed comparison on public images

The [Oxford 17 Flowers](https://www.robots.ox.ac.uk/~vgg/data/flowers/17/index.html) predeclared cohort of 48 images was restored in full after finding an incomplete cache. Archive SHA-256 fe38a60f8b4a95e657551247d3e7d799a3fafcdbc595be504b12839967823d70. Maximum side 384 (OpenCV INTER_AREA). Same oracle foreground mark (deepest annotated pixel) for both methods; one oracle negative mark checked both polygons and guided GrabCut. CPU XNNPACK on a local Windows PC. Valid pixels exclude ambiguous trimap indices. Times include segmentation and polygon checks.

| Method | Candidates / 48 | Mean IoU among candidates | Mean IoU treating abstentions as zero | Median ms |
| --- | ---: | ---: | ---: | ---: |
| MagicTouch raw mask | 48 | .8123 | .8123 | — |
| MagicTouch polygon | 48 | .8087 | .8087 | 163.39 |
| GrabCut point polygon | 39 | .8191 | .6655 | 445.37 |

Worst neural proposed polygon IoU .14129. The model is complementary, not a reliable replacement. Benchmarks use oracle prompts on flowers, not real touch prompts on indoor plant organs, and not phone/live timings. Product uses bounded PIL BOX resize before conversion, checks existing polygon geometry and requires explicit user review. Test the exact product path and a representative field cohort before release.

## Species recognition

Inspected Pl@ntNet MobileNetV4-Hybrid-M [labels](https://huggingface.co/PlantNet/mobilenetv4-hybrid-m-plantclef2024) at revision fea9046c9d25d1dee13c6c429560426670991352, 7,806 species, label SHA-256 1226fcbab5441062429cb7e332b89e657b4f15757f0d28a6db6787584c024c41. Exact scientific binomials (authorship excluded) cover Ficus elastica, Crassula ovata and Aloe vera, not Monstera deliciosa, Epipremnum aureum or Zamioculcas zamiifolia. Label screening only; not exhaustive synonym resolution or inference. Do not replace the indoor DINOv2 classifier with it. Recognition still needs better indoor reference data and observer-disjoint open-set validation.

## Acceptance

Keep model default off, verify real authorized device photos and latency, actor/source/method tampering, outline alignment, abstention, throttling and manual fallback. This is a neural still-photo proposal, not a live neural tracker or automatic organ detector.

## Reproduction and direct integration smoke

The comparison source is `scripts/living_research/compare_magic_touch.py`; the 48-image output is `docs/living-neural-flower-benchmark.json`. Invoke with the local photo tarball, trimap tarball, reviewed model path, repository `backend` path and an output JSON path. No images or model binaries are committed. Direct execution of `propose_neural_outline` from b6253dc, using original photograph coordinates and reference-derived points on three selected public photos, produced polygon IoU of 0.9991 (image_0002), 0.9851 (image_0113) and 0.9954 (image_0909). This is a three-image integration smoke, not an independent validation cohort.
