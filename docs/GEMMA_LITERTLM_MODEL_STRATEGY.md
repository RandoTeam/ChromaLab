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

## Phase 8C Availability Policy

The app must not treat VLM availability as a hard blocker for deterministic geometry:

- if `Gemma-4-E4B LiteRT-LM` is available, use it for FULL_ANALYSIS semantic/VLM tasks;
- if E4B is unavailable and `Gemma-4-E2B LiteRT-LM` is available, use E2B for FAST/fallback semantic tasks;
- if neither model is available, record model availability diagnostics and continue deterministic graphPanel, plotArea, axis, tick, calibration, trace, peak, and report-gate attempts;
- do not claim VLM-assisted evidence when no VLM ran;
- do not bundle large model files in this repository.

Validation packages discover local models under app-private `files/models/<model-id>/`. Missing models produce `modelAvailabilityDiagnostics` and may produce `VLM_MODEL_UNAVAILABLE`, `MODEL_NOT_CONFIGURED`, `MODEL_ASSET_MISSING`, or `MODEL_LOAD_FAILED` depending on the observed state.
# Knowledge-Grounded Semantic Layer

Date: 2026-05-20

Gemma-4-E4B LiteRT-LM remains documented as the primary FULL_ANALYSIS semantic/VLM assistant model. Gemma-4-E2B LiteRT-LM remains the FAST/fallback model for lower memory or shorter semantic classification tasks.

For Phase 6C, model prompts that use scientific/domain knowledge must receive only bounded Knowledge Pack snippets:

- `entry_id`
- `version`
- `short_text`
- `allowed_use`
- `forbidden_use`
- `source_ref`

Model output must include:

- `used_entry_ids`
- `decision`
- `confidence`
- `unsupported_claims`
- `explanation`

If scientific explanation lacks `used_entry_ids`, the output is REVIEW or rejected. If the model uses a knowledge entry for a forbidden purpose, the output is rejected. Knowledge never creates measured chromatographic metrics.

## Phase 9 Android Validation Update

Date: 2026-05-20

Real-device validation on `com.chromalab.app.validation` found `Gemma-4-E2B LiteRT-LM` installed at:

```text
files/models/gemma4-e2b/gemma-4-E2B-it.litertlm
```

`Gemma-4-E4B LiteRT-LM` was not installed in the validation package during Phase 9, so E4B FULL_ANALYSIS remains a required follow-up validation item.

The validation fixture model-enabled path now defers Gemma activation until deterministic X/Y calibration completes. This preserves the hard boundary that model loading or VLM timeout cannot change graphPanel, plotArea, tick, calibration, trace, peak, or report numeric evidence.

## Runtime Acceleration R1 Catalog Update

As of 2026-06-02, the built-in model catalog includes smoke-check metadata for:

- generic `Gemma-4-E2B LiteRT-LM` as the FAST/weaker-device baseline;
- `Gemma-4-E2B LiteRT-LM` Qualcomm SM8750 bundle;
- `Gemma-4-E2B LiteRT-LM` Qualcomm QCS8275 / Dragonwing IQ8 bundle;
- `Gemma-4-E2B LiteRT-LM` Google Tensor G5 bundle;
- generic `Gemma-4-E4B LiteRT-LM` as the FULL_ANALYSIS candidate.

These entries are not auto-selected by device yet. R1 only makes the catalog
explicit and requires download smoke checks for these large LiteRT bundles:
metadata validation, HTTPS URL validation, free-space headroom, remote
`X-Linked-Size` / `Content-Length` verification, and partial `.download` cleanup
on preflight failure. Device-specific selection is reserved for R2.

## Runtime Acceleration R2 Device Selection

As of 2026-06-02, automatic chromatogram model discovery can distinguish generic
Gemma 4 E2B from device-specific E2B bundles. A device-specific bundle is used
only when Android device signals explicitly match the target:

- `sm8750` / `snapdragon 8 elite` -> Qualcomm SM8750 E2B;
- `qcs8275` / `dragonwing iq8` -> Qualcomm QCS8275 E2B;
- `tensor g5` / `gs501` -> Google Tensor G5 E2B.

If no matching device-specific bundle is downloaded, the generic
`gemma4-e2b` FAST baseline remains the fallback. Nonmatching device-specific
bundles are rejected for automatic pipeline use and recorded in model discovery
diagnostics.
