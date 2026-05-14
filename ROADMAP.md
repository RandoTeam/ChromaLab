# ChromaLab Roadmap

This roadmap tracks the current product direction after alpha 2. ChromaLab is becoming a modular offline AI analysis app, with chromatography as the first serious domain module.

## Status Summary

Completed or mostly working:

- Android/KMP project foundation.
- Capture from camera and gallery.
- Image-to-signal processing pipeline.
- Deterministic chromatogram calculation engine.
- Model manager for LiteRT-LM and GGUF.
- Hugging Face model search MVP.
- Local LLM chat MVP.
- Gallery-style local chat UI with model chip, picker, runtime controls, streaming, stop state, and assistant telemetry.
- Debug APK release workflow.

Needs cleanup before broader alpha testing:

- Replace stale calculation tests with current `CalculationEngine` tests.
- Validate the full photo-to-report flow on real chromatograms.
- Harden GGUF multimodal model pairing and error messages.
- Improve final report language and scientific interpretation.

## Alpha 2 Scope - v0.0.3-alpha

### Chromatography

- [x] Auto flow from processed signal to calculation screen.
- [x] Algorithm settings panel connected to real calculations.
- [x] `CalculationEngine` applies boundary method.
- [x] `CalculationEngine` applies negative-area clamp.
- [x] `CalculationEngine` applies max peak width.
- [x] `CalculationEngine` applies integration mode.
- [x] Report/export includes the relevant calculation parameters.
- [x] Area percent calculation uses absolute integrated area.
- [ ] Replace stale `commonTest` files with current calculation regression tests.
- [ ] Validate against a reference set of real chromatograms.
- [ ] Add explicit user-facing warnings for weak axis extraction and low-confidence curve extraction.

### AI Model Platform

- [x] Unified model manager state for downloaded, custom, active, and importing models.
- [x] LiteRT-LM activation path for stable Android inference.
- [x] GGUF activation path through llama.cpp JNI.
- [x] Text-only GGUF inference path for chat.
- [x] GGUF image inference requires a valid `mmproj`.
- [x] Hugging Face search UI.
- [x] Search sorting by downloads, likes, and update time.
- [x] Compatibility metadata: runtime, RAM estimate, vision support.
- [ ] More precise device compatibility scoring.
- [ ] Better model pairing UX for base GGUF + `mmproj`.
- [ ] More model-family prompt presets.

### Local Chat

- [x] Chats tab in bottom navigation.
- [x] Multiple chat sessions.
- [x] Per-chat generation settings.
- [x] Model selection per chat from the shared downloaded model pool.
- [x] Android inference through the active local model engine.
- [x] Desktop persistence stub.
- [x] Gallery-style top app bar, model chip, and separate model picker.
- [x] Capability-gated accelerator controls and thinking toggle.
- [x] Streaming text output with stop state.
- [x] Assistant telemetry with model, backend, accelerator, duration, and token stats.
- [ ] Chat title auto-generation.
- [ ] Attach image/file context to chat.
- [ ] Search across chat history.

## Next MVP Phase

### 1. Stabilize Chromatography Core

- [ ] Build the desktop/emulator-first calibration bench from `docs/CHROMATOGRAM_CALIBRATION_BENCH_PLAN.md`.
- [ ] Rewrite calculation tests for the current API:
  - boundary methods;
  - clamp negative;
  - max width;
  - trapezoidal vs interpolated integration;
  - noise method and S/N threshold;
  - area percent.
- [ ] Add synthetic chromatogram fixtures with known expected peaks.
- [ ] Add a small real-photo validation set.
- [ ] Make the report explain:
  - what was detected;
  - what was uncertain;
  - which parameters affected the result;
  - what should be checked manually.

### 2. Improve File Import

- [ ] CSV/TXT parser with delimiter and column auto-detection.
- [ ] Manual column mapping screen.
- [ ] Basic PDF raster fallback through the image pipeline.
- [ ] Define mzML/netCDF scope separately; do not block alpha on it.

### 3. Harden AI Runtime

- [ ] Add clearer model activation diagnostics.
- [ ] Validate downloaded/custom model files before activation.
- [ ] Cache Hugging Face search results.
- [ ] Add cancel/retry UI for downloads and activation.
- [ ] Add memory-pressure handling for chat sessions.

### 4. App UX Settings

- [x] Add theme mode setting: follow Android system, force light, force dark.
- [x] Lock Android UI to portrait until landscape layouts are intentionally designed and validated.

### 5. Release Quality

- [ ] Clean outdated tests.
- [ ] Add release checklist.
- [ ] Add screenshots and known-issues section to GitHub releases.
- [ ] Run device testing on at least:
  - current development phone;
  - one lower-memory Android device;
  - one emulator for UI smoke checks.

## Later Modules

These should be added as separate modules once the core model/runtime architecture is stable:

- ion ratio and multi-channel chromatogram comparison;
- calibration curves and QC;
- project/sample hierarchy;
- template-based compound identification;
- local document/report assistant;
- other small domain analyzers that can reuse the same local model pool.

## Release Track

- `v0.0.1` - phase 1 preview.
- `v0.0.2-alpha` - calculation engine alpha.
- `v0.0.3-alpha` - model platform, chat MVP, calculation-settings fix.
