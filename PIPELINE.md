# ChromaLab Technical Pipeline

This document describes the current runtime pipeline. The same calculation engine must be used regardless of whether the signal came from camera, gallery, or a digital file.

## High-Level Flow

```text
Input
  -> Image/File Processing
  -> DigitalSignal(time, intensity)
  -> CalculationEngine
  -> Results UI
  -> Export / Report
```

AI models are supporting services, not a replacement for deterministic calculation:

```text
Model Manager
  -> VLM helpers for image understanding
  -> LLM chat workspace
  -> Shared local model pool
```

## Input Paths

### Camera

1. User captures a chromatogram through the Android capture flow.
2. The same Smart Scan UI can import an existing photo from the gallery.
3. ML Kit document scanner/crop flow prepares the image.
4. Processing flow detects graph region, axes, and curve.
5. The extracted curve becomes a `DigitalSignal`.

### File Import

Current state:

- desktop file import bridge exists for local development;
- production-grade CSV/TXT/PDF/mzML parsing is still roadmap work.

Required target:

```text
CSV/TXT/PDF/mzML -> DigitalSignal -> CalculationEngine
```

## Image And Signal Processing

Main stages:

1. Normalize image orientation and source metadata.
2. Prepare curve mask.
3. Detect graph region.
4. Detect axis structure and labels.
5. Extract curve points.
6. Convert pixels to real units.
7. Save or pass `DigitalSignal`.

### Stage 1-3 Shadow Parity

Current benchmark tooling includes a PC-side Stage 1-3 shadow parity harness:

```text
python tools/benchmark/run_stage123_shadow_parity.py --clean
```

It writes schema-backed records under
`benchmark/examples/stage123_shadow_parity/` and a summary under
`benchmark/reports/stage123_shadow_parity/`.

This harness is not part of the runtime pipeline. It does not change graph
selection, plotArea selection, calibration, trace extraction, peak calculations,
report gates, validators, model policy, or `CalculationEngine`. It exists so a
future Rust/Kotlin replacement can be measured before promotion.

R3 adds a PC-side Stage 1 image-preparation candidate:

```text
python tools/benchmark/run_r3_image_preparation_candidate.py --clean
```

It writes records under `benchmark/examples/r3_image_preparation_candidate/`
and a contact sheet under `benchmark/reports/r3_image_preparation_candidate/`.
It is still shadow-only and is not used by runtime analysis.

R4 adds a Rust Stage 1 image-preparation parity bridge:

```text
python tools/benchmark/run_r4_rust_stage1_image_preparation_parity.py --clean
```

It writes records under
`benchmark/examples/r4_rust_stage1_image_preparation_parity/` and Rust
per-fixture reports under
`benchmark/reports/r4_rust_stage1_image_preparation_parity/`. R4 reached 8/8
selected-variant parity and 8/8 PASS/REVIEW status parity against R3. JPEG
fixtures keep `NORMALIZED_SHA_MISMATCH` review notes because Pillow and Rust
decode normalized RGB bytes differently. R4 remains shadow-only and is not used
by runtime analysis.

R5 adds a PC-side Stage 2 graph discovery candidate:

```text
python tools/benchmark/run_r5_stage2_graph_discovery_candidate.py --clean
```

It writes records under `benchmark/examples/r5_stage2_graph_discovery_candidate/`
and graph discovery overlays under
`benchmark/reports/r5_stage2_graph_discovery_candidate/`. R5 reached 8/8
graph-count pass against current fixture metadata, but graphPanel localization
remains candidate-only and REVIEW until Stage 3 plotArea/layout scoring. R5
remains shadow-only and is not used by runtime analysis.

R6 adds a PC-side Stage 3 plotArea/layout candidate:

```text
python tools/benchmark/run_r6_stage3_plotarea_layout_candidate.py --clean
```

It writes records under `benchmark/examples/r6_stage3_plotarea_layout_candidate/`
and plotArea/layout overlays, detail JSON files, and a contact sheet under
`benchmark/reports/r6_stage3_plotarea_layout_candidate/`. R6 reached 8/8
layout-class pass in shadow mode and 0.62146 mean plotArea IoU on the four P0
fixtures with manual annotations, but every record remains REVIEW. It is not
used by runtime analysis and does not prove axis scale, calibration, trace,
peak, or report readiness.

R7 adds a PC-side Stage 4 axis/frame/scale evidence candidate:

```text
python tools/benchmark/run_r7_stage4_axis_frame_scale_candidate.py --clean
```

It writes records under `benchmark/examples/r7_stage4_axis_frame_scale_candidate/`
and axis/frame/scale overlays, detail JSON files, and a contact sheet under
`benchmark/reports/r7_stage4_axis_frame_scale_candidate/`. R7 uses DR-C4
manual-review tick/text annotations only for scoring candidate evidence. It is
not runtime calibration and is not used by runtime analysis.

R8 adds a PC-side Stage 5 calibration strategy parity candidate:

```text
python tools/benchmark/run_r8_stage5_calibration_strategy_parity_candidate.py --clean
```

It writes records under
`benchmark/examples/r8_stage5_calibration_strategy_parity_candidate/` and
calibration strategy overlays, detail JSON files, and a contact sheet under
`benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/`. R8 uses
DR-C4 manual-review anchors only as scoring truth. It is not runtime
calibration and is not used by runtime analysis.

R9 adds a PC-side Stage 6 automatic OCR anchor candidate:

```text
python tools/benchmark/run_r9_stage6_automatic_ocr_anchor_candidate.py --clean
```

It writes records under
`benchmark/examples/r9_stage6_automatic_ocr_anchor_candidate/` and automatic
OCR anchor overlays, detail JSON files, and a contact sheet under
`benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/`. R9 consumes DRD
and DRE OCR reports and measures PC-side safe OCR anchors against DR-C4 truth.
It is not Android runtime calibration and is not used by runtime analysis.

VLM-assisted stages:

- graph region detection;
- axis structure detection;
- axis label extraction.

Fallback stages:

- classical CV graph/axis detection;
- ML Kit OCR;
- manual review paths where available.

## CalculationEngine

The calculation engine is deterministic and receives:

```text
DigitalSignal + CalculationParams -> CalculationRun
```

Pipeline order:

1. Validate input signal.
2. Optional Savitzky-Golay smoothing.
3. Baseline estimation.
4. Baseline correction.
5. Noise estimation.
6. Peak detection.
7. Peak boundary detection.
8. Overlap classification.
9. Peak integration.
10. Peak metrics and confidence.
11. Run warnings.
12. Immutable `CalculationRun`.

### Calculation Parameters

These parameters are now applied by the engine, not just shown in the UI:

- `baselineMethod`
- `baselineLambda`
- `baselineP`
- `baselineIterations`
- `noiseMethod`
- `minPeakHeight`
- `minPeakProminence`
- `minPeakDistance`
- `minPeakWidth`
- `maxPeakWidth`
- `minSnr`
- `boundaryMethod`
- `boundaryPercentHeight`
- `integrationMethod`
- `clampNegative`
- `useSmoothedForIntegration`

### Boundary Methods

Supported:

- `PROMINENCE_BASES`
- `LOCAL_MINIMA`
- `BASELINE_INTERSECTION`
- `PERCENT_HEIGHT`

Manual boundaries are supported at the algorithm layer and should be wired into UI edits as a later step.

### Integration Modes

Supported:

- `TRAPEZOIDAL`
- `TRAPEZOIDAL_INTERPOLATED`

If `clampNegative` is enabled, negative corrected values are treated as zero during integration.

### Reports

Reports/export should include:

- pipeline and algorithm version;
- calculation parameters;
- signal source;
- peak table;
- warning list;
- quality/confidence information;
- exportable corrected signal and baseline where available.

## Model Runtime Pipeline

### LiteRT-LM

Primary stable Android runtime for Google AI Edge/Gemma-style models.

Used for:

- VLM-assisted chromatogram image understanding;
- local chat where a compatible LiteRT model is active.

### GGUF / llama.cpp

Native Android bridge through `androidApp/src/main/cpp/llama_bridge.cpp`.

Modes:

- text-only GGUF: local chat/text inference;
- multimodal GGUF: image inference only when a valid `mmproj` is loaded with the base model.
- text-only MTP: GGUF chat can enable upstream llama.cpp `draft-mtp` speculative decoding with a per-chat draft-token limit.

The app should not silently use a text-only GGUF model for image analysis. If image inference is requested without `mmproj`, it must fail clearly and fall back where possible.

MTP is deliberately scoped to text-only GGUF chat. It is not applied to `mmproj` vision, VLM image chat, or strict chromatogram analysis until those paths have their own quality and memory validation.

For full chromatogram analysis, GGUF models must satisfy the same VLM contract as LiteRT: graph bounds JSON, axis-label JSON, and usable image input. OCR/document-only GGUF families such as PaddleOCR-VL, DeepSeek-OCR, and dots.mocr stay available as model downloads, but they are not selected automatically for the full chromatogram pipeline until they have a validated adapter for that contract.

An already-active OCR/document VLM is not reused for strict chromatogram photo analysis just because it supports image input. The pipeline validates the executed model against the chromatogram-vision allowlist before accepting an active engine; otherwise it unloads that engine and lazy-loads a chromatogram-capable model.

Native GGUF inference logs watchdog warnings for model load, context init, mmproj init, bitmap load, `mtmd_tokenize`, `mtmd_helper_eval_chunks`, and decode stalls. These logs are diagnostic only: they must not trigger a deterministic-only fallback for full chromatogram analysis.

## Chat Pipeline

```text
Chat session
  -> selected local model
  -> per-chat generation settings
  -> Gallery-style chat UI controls
  -> prompt-style formatting for GGUF runtimes
  -> InferenceEngine.inferRaw()
  -> saved message history
```

Generation settings:

- temperature;
- top-p;
- top-k;
- max tokens;
- repeat penalty;
- repeat last N.
- GGUF MTP draft tokens.

Chat uses the same model pool as chromatogram analysis. A model can be downloaded/imported once and reused by the analysis pipeline or chat.
Only chat-capable models are exposed to chat. OCR/document-only GGUF families such as PaddleOCR-VL, DeepSeek-OCR, and dots.mocr remain available for specialized analysis tasks but are hidden from chat selection and are not passed to the chat runtime as normal assistants.
GGUF chat/text prompts must be formatted for the active model family before native inference. For example, Qwen-family GGUF models receive ChatML instead of a generic raw `User:` / `Assistant:` transcript. A blank native GGUF response is treated as a runtime error, not as a successful assistant message.

Chat UI state is intentionally separated from model loading. The chat screen exposes a Gallery-style model chip, model picker, runtime accelerator controls, thinking toggle, streaming/stop state, and assistant telemetry. Selection does not load the model by itself; runtime loading starts when a chat request actually needs inference. Image/file context remains disabled until chat message storage, model capability gating, and runtime routing support it end to end.

## Validation Gates

Current working gates:

```bash
./gradlew :composeApp:compileAndroidMain :composeApp:compileKotlinDesktop --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
```

Known test debt:

- `commonTest` contains old tests targeting previous calculation APIs.
- Before the next public alpha, these tests should be replaced with current `CalculationEngine` regression coverage.

## Alpha 2 Known Risks

- Real-photo validation is still limited.
- Report language needs more professional domain interpretation.
- CSV/TXT/PDF/mzML import is not yet production complete.
- GGUF model compatibility depends on correct model family and `mmproj` pairing.
- Chat MVP has streaming text and telemetry, but is not yet a full assistant with attachments or search.
- Theme mode selection is implemented in settings, and the Android phone UI is locked to portrait until landscape layouts are intentionally designed and validated.
