# ChromaLab

ChromaLab is an offline-first Android/Kotlin Multiplatform app for chromatogram digitization, chromatographic calculation, and local on-device AI workflows.

The project is no longer only a narrow chromatography calculator. The current direction is a modular offline analysis workspace:

- chromatogram capture and digitization from camera, gallery, or file import;
- deterministic chromatographic calculations with reproducible parameters;
- local VLM/LLM model management with LiteRT-LM and GGUF support;
- an integrated local chat workspace that uses the same downloaded model pool;
- exportable human-readable reports and machine-readable data.

Current public build: **v0.0.3-alpha**.

> ChromaLab is an analysis assistant. Results must be reviewed by a qualified specialist before scientific, industrial, legal, or medical use.

## What Works In Alpha 2

### Chromatogram Input

- Android camera flow through ML Kit document scanner.
- Gallery/image import path.
- Desktop file import bridge for local development.
- Auto-processing flow from image to saved signal and analysis screen.

### Image And Signal Processing

- Document/crop preparation and perspective handling.
- Graph region, axis, and curve extraction pipeline.
- Curve mask preparation with sweep-based fallback logic.
- Digital signal preview and persistence.
- VLM-first helpers for graph region, axis structure, and axis text extraction, with classical CV/OCR fallback.

### Calculation Engine

- Baseline correction: none, manual linear, ALS, SNIP.
- Noise estimation: peak-to-peak, RMS, MAD.
- Peak detection with minimum height, prominence, distance, width, S/N, and max width filtering.
- Boundary methods now applied by the engine:
  - prominence bases;
  - local minima;
  - baseline intersection;
  - percent of height.
- Integration modes now applied by the engine:
  - trapezoidal;
  - interpolated trapezoidal boundaries.
- Negative baseline-corrected regions can be clamped during integration.
- Peak metrics: RT apex, centroid, height, area, width, prominence, S/N, confidence, overlap, tailing/asymmetry, resolution, area percent.
- Reports now include the calculation settings that affect the result.

### AI Models

- Unified model manager for LiteRT-LM and llama.cpp/GGUF runtimes.
- Built-in model catalog and local custom model import.
- Download, delete, activate, deactivate, export, and local-load flows.
- Hugging Face model search for GGUF/LiteRT candidates with sort options:
  - downloads;
  - likes;
  - last update.
- Compatibility metadata in the UI: runtime, estimated RAM, vision support, local availability.
- LiteRT-LM path remains the primary stable Android path for Gemma-style models.
- GGUF path supports text-only chat and multimodal analysis when a valid `mmproj` is present.

### Local LLM Chat MVP

- Dedicated Chats tab.
- Multiple chat sessions.
- Per-chat settings:
  - temperature;
  - top-p;
  - top-k;
  - max tokens;
  - repeat penalty;
  - repeat last N.
- Per-chat model selection from the shared downloaded model pool.
- Android chat inference through the active LiteRT/GGUF engine.
- Desktop chat persistence stub for development.

## Architecture

```text
Capture/File Input
    -> Processing Pipeline
    -> Digital Signal
    -> CalculationEngine
    -> Report / Export

Model Manager
    -> LiteRT-LM runtime
    -> GGUF llama.cpp runtime
    -> VLM helpers
    -> Chat workspace
```

Main modules:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/capture` - capture and import UI.
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing` - image, graph, curve, OCR/VLM, and signal processing.
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/calculation` - deterministic calculation pipeline and reports.
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/settings` - model manager and app settings.
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/chat` - local LLM chat UI and state.
- `androidApp/src/main/cpp` - llama.cpp JNI bridge for GGUF inference.

## Technology

- Kotlin Multiplatform.
- Compose Multiplatform / Jetpack Compose Material 3.
- Android Camera / ML Kit document scanner and OCR.
- Room + bundled SQLite.
- LiteRT-LM for Google AI Edge local model runtime.
- llama.cpp native bridge for GGUF.
- Coroutines and Kotlin serialization.
- Android NDK/CMake for native inference.

## Build

Requirements:

- JDK 17.
- Android SDK 35.
- Android NDK `27.2.12479018`.
- CMake `3.22.1`.

Useful commands:

```bash
./gradlew :composeApp:compileAndroidMain :composeApp:compileKotlinDesktop --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :androidApp:installDebug --no-daemon
```

Debug APK:

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## Known Alpha Limitations

- Old `commonTest` sources still target earlier calculation APIs and need migration before `desktopTest` can be used as a reliable gate.
- CSV/TXT/PDF/mzML import is not yet complete as a production-grade parser path.
- GGUF image analysis requires a matching `mmproj`; text-only GGUF models are for chat/text tasks.
- The report is improving but still needs stronger professional wording, validation summaries, and domain-specific interpretation.
- Validation against a large real chromatogram set is still pending.

## License

Proprietary. Copyright 2026.
