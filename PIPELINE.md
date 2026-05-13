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

The app should not silently use a text-only GGUF model for image analysis. If image inference is requested without `mmproj`, it must fail clearly and fall back where possible.

For full chromatogram analysis, GGUF models must satisfy the same VLM contract as LiteRT: graph bounds JSON, axis-label JSON, and usable image input. OCR/document-only GGUF families such as PaddleOCR-VL, DeepSeek-OCR, and dots.mocr stay available as model downloads, but they are not selected automatically for the full chromatogram pipeline until they have a validated adapter for that contract.

## Chat Pipeline

```text
Chat session
  -> selected local model
  -> per-chat generation settings
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

Chat uses the same model pool as chromatogram analysis. A model can be downloaded/imported once and reused by the analysis pipeline or chat.
Only chat-capable models are exposed to chat. OCR/document-only GGUF families such as PaddleOCR-VL, DeepSeek-OCR, and dots.mocr remain available for specialized analysis tasks but are hidden from chat selection and are not passed to the chat runtime as normal assistants.
GGUF chat/text prompts must be formatted for the active model family before native inference. For example, Qwen-family GGUF models receive ChatML instead of a generic raw `User:` / `Assistant:` transcript. A blank native GGUF response is treated as a runtime error, not as a successful assistant message.

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
- Chat MVP is functional but not yet a full assistant with attachments, streaming, or search.
