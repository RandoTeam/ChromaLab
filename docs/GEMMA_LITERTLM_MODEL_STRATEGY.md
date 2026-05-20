# Gemma LiteRT-LM Model Strategy

ChromaLab's autonomous semantic layer is local-first. Model use must support the image-analysis pipeline without becoming the numeric source of truth.

## Primary Model

`Gemma-4-E4B LiteRT-LM` is the primary FULL_ANALYSIS semantic/VLM model target.

Use it for:

- hard local crop OCR fallback;
- text-region classification;
- overlay judging;
- warning explanation;
- report explanation grounding with cited knowledge entries.

## Fallback Model

`Gemma-4-E2B LiteRT-LM` is the FAST-mode and weaker-device fallback.

Use it when:

- memory pressure is high;
- FULL_ANALYSIS is not selected;
- the semantic task is short classification or warning explanation;
- E4B cannot be loaded within device budget.

## Optional Models

GGUF variants are benchmark/compatibility fallback only, not the default production path.

## Hard Boundaries

All models are forbidden from producing final numeric chromatographic measurements:

- RT;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index;
- calibration coefficients;
- integration boundaries.

The model may only explain, classify, judge overlays, and recommend deterministic retries. Deterministic CV/OCR/calibration/trace/peak evidence remains authoritative.
