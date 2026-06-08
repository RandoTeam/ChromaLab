# ChromaLab Roadmap

## 2026 Public Roadmap

ChromaLab is currently a research-alpha public project. The public repository
presentation is in place, but Phase 9 runtime validation is not accepted as
production autonomous analysis. The next roadmap work should improve real
analysis reliability without weakening validators or hiding blocked/review-only
status.

Current priorities:

1. **Graph layout and axis scale repair**
   - improve single-graph vs stacked-trace vs multi-panel classification;
   - strengthen axis scale evidence for screenshots and photos;
   - keep graph count decisions tied to visual evidence and fixture truth.

2. **Rust CV production integration**
   - continue moving graph/layout/axis primitives from prototypes toward a
     tested production bridge;
   - compare Rust CV results against the current Phase 9J benchmark records;
   - avoid porting weak heuristics blindly from older Kotlin paths.
   - current progress: R2 added a schema-backed Stage 1-3 shadow parity harness;
     R3 added a shadow-only Stage 1 image-preparation candidate with preview
     evidence; R4 added a Rust Stage 1 parity bridge with 8/8 selected-variant
     parity and 8/8 PASS/REVIEW status parity against R3; R5 reached 8/8
     graph-count pass in shadow mode; R6 reached 8/8 layout-class pass with
     REVIEW-only plotArea evidence; R7 added REVIEW-only axis/frame/scale
     evidence with 12 annotated manual-review scale graphs; R8 added
     REVIEW-only calibration strategy parity with 12 selected manual-review
     scoring fits; R9 added PC-side automatic OCR anchor evidence with 12
     candidate graphs and 155 accepted OCR anchors; R10 added a Rust/runtime
     OCR anchor bridge contract with 4/4 scoreable fixture parity and 155
     accepted bridge rows; R11 added shadow integrated calibration closure
     records with 12 selected graph calibration fits from 155 accepted bridge
     anchors; R12 closed runtime evidence/failure package accountability over
     16 tracked fixture-mode records with 0 no-export states and 4/4 blocked
     runs carrying graph failure packages; R13 added Android/runtime OCR-anchor
     bridge rows and validator safety checks; R14 added a named runtime
     OCR-anchor calibration strategy candidate with coordinate-frame safety and
     selected/rejected strategy evidence; R15 added per-graph runtime geometry
     results, resolved physical-panel iteration, and explicit unsupported
     aggregate multi-panel report warnings. R15A attempted the Android evidence
     gate but produced no fixture truth because no adb target was connected and
     the fresh validation APK build failed in the native host shader-generator
     toolchain step. The next slice is an R15A retry; trace extraction evidence
     starts only after Android reruns prove multi-panel propagation is stable.

3. **E2B baseline validation**
   - keep Gemma E2B as the FAST/weaker-device baseline where supported;
   - verify that E2B improves OCR/semantic assistance without degrading graph
     count, calibration, trace, peak metrics, or report gates;
   - keep model disagreement advisory and evidence-recorded.

4. **Report and export polish**
   - improve report readability, evidence summaries, and export separation;
   - keep `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, and `BLOCKED` states visible;
   - do not present incomplete outputs as release-ready.

5. **Security and privacy hardening**
   - keep `SECURITY.md`, `PRIVACY.md`, and `CONTRIBUTING.md` current;
   - review Android storage, exports, diagnostic bundles, local model files, and
     native runtime surfaces;
   - prevent private chromatogram images, logs, local paths, signing data, and
     model files from entering public artifacts.

6. **Ground truth and validation depth**
   - expand fixture truth records and benchmark scoring;
   - add more real graph classes only when expected outcomes are documented;
   - keep Product/QA/Scientific acceptance separate from visual polish.

7. **Knowledge retrieval replacement / TurboVec gate**
   - keep Knowledge retrieval local/offline and citation-gated;
   - current implementation now separates `KnowledgeRetrievalEngine` facade from
     the active `LexicalKnowledgeRetrievalBackend`;
   - TV-2 built PC-only TurboVec indexes for Knowledge Pack v2 with MiniLM and
     BGE, found useful semantic/caveat improvements, and recorded 0 safety
     regressions;
   - TV-3 selected `HYBRID_UNION_RRF` as the safe benchmark target because it
     recovered the natural-language compound-caveat miss, improved selected
     semantic rankings, and preserved safety-critical exact-rule behavior;
   - TV-4 added the Kotlin-side `HYBRID_UNION_RRF_CANDIDATE` policy with
     lexical top-1 pinning for safety-critical exact-rule queries;
   - dense-only MiniLM/BGE remain rejected as promotion targets;
   - next Knowledge retrieval step: TV-5 dense provider promotion or rejection
     gate;
   - do not add TurboVec to Android runtime until benchmark and native packaging
     gates pass.

## Historical Alpha Track

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
- [x] Text-only GGUF MTP speculative decoding through upstream llama.cpp `draft-mtp`.
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
- [x] Per-chat MTP toggle and draft-token setting for GGUF text-only chat.
- [ ] Chat title auto-generation.
- [ ] Attach image/file context to chat.
- [ ] Search across chat history.

## Next MVP Phase

### 1. Stabilize Chromatography Core

- [ ] Build the desktop/emulator-first calibration bench from `docs/CHROMATOGRAM_CALIBRATION_BENCH_PLAN.md`.
  Active phase tracking and completed work-slice commits are recorded in that document.
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
- [ ] Add desktop model runtime path from `docs/DESKTOP_MODEL_RUNTIME_ARCHITECTURE.md`:
  LM Studio/OpenAI-compatible server first, then managed desktop llama.cpp with
  CUDA/Vulkan/CPU backends.

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
- `v0.0.4-beta` - updated llama.cpp runtime and GGUF text-only MTP chat acceleration.
