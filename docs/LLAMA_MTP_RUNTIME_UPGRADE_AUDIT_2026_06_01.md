# Llama / MTMD / MTP Runtime Upgrade Audit

Date: 2026-06-01

## Scope

This audit covers runtime libraries that ChromaLab builds or links for local
LLM/VLM execution:

- vendored `ggml-org/llama.cpp`;
- `libmtmd`, built from the vendored `llama.cpp/tools/mtmd` tree;
- GGUF text-only `draft-mtp` speculative decoding support;
- Google LiteRT-LM Android dependency.

It does not promote any OCR/VLM model to scientific or numeric authority. Model
catalog changes for newer Qwen MTP model families remain separate from this
runtime-library upgrade.

## Summary

| Element | Previous local state | Current upstream | Action | Validation |
| --- | --- | --- | --- | --- |
| `llama.cpp` submodule | `b9219`, commit `45b455e66fc09abed65b7d52d42a4a29ba0d45d6` | `b9464`, commit `5dcb71166686799f0d873eab7386234302d05ecf` | Updated submodule to `b9464` | `:androidApp:assembleValidation` passed |
| `libmtmd` | Built manually from vendored `tools/mtmd` | Same upstream tree as `llama.cpp b9464` | Updated implicitly with submodule | Native Android validation build passed |
| GGUF MTP runtime | `draft-mtp` through vendored `llama.cpp b9219` | `b9464` includes newer speculative fixes, including `n_outputs_max` / draft auto-enable cleanup | Updated implicitly with submodule | Native bridge compiled with current `common/speculative` API |
| LiteRT-LM Android | Floating `latest.release` dependency | Google Maven latest/release `0.12.0` | Pinned to `0.12.0` through version catalog | Android validation build resolved dependency |

## Element Review

### 1. `llama.cpp`

Repository: <https://github.com/ggml-org/llama.cpp>

Local before:

- tag: `b9219`
- commit: `45b455e66fc09abed65b7d52d42a4a29ba0d45d6`
- date: 2026-05-18
- subject: `common : remove hf cache migration (#23266)`

Upstream selected:

- tag/head: `b9464`
- commit: `5dcb71166686799f0d873eab7386234302d05ecf`
- date: 2026-06-01
- subject: `speculative : fix n_outputs_max and remove draft-simple auto-enable (#23988)`

Action:

- Updated `androidApp/libs/llama.cpp` submodule to `b9464`.
- Updated Android CMake metadata from `0.0.9198` to `0.0.9464`.
- Updated user-facing README version reference to `b9464`.

Risk:

- `llama.cpp` APIs are fast moving. This update was accepted only because the
  Android JNI bridge compiled against the current headers and `common`
  speculative API.

### 2. `libmtmd`

Repository/documentation: <https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md>

Current upstream note:

- Multimodal input is handled through `libmtmd`.
- Official supported entrypoints are `llama-mtmd-cli` and `llama-server`.
- Local file usage remains a base `.gguf` plus `--mmproj`.

Action:

- No separate package exists in this repo. `mtmd_lib` is built directly from
  `androidApp/libs/llama.cpp/tools/mtmd`.
- Updating the submodule updates the MTMD sources used by the Android bridge.

ChromaLab constraint:

- MTMD remains vision/audio infrastructure only.
- Missing or mismatched `mmproj` must remain a hard runtime diagnostic for GGUF
  image tasks.
- MTMD output must not create chromatographic numeric metrics.

### 3. GGUF MTP / `draft-mtp`

Upstream references:

- `llama.cpp` speculative decoding code is part of the same submodule.
- Recent upstream discussion still uses `--spec-type draft-mtp` and
  `--spec-draft-n-max`.

Action:

- Updated MTP support by updating `llama.cpp` to `b9464`.
- Kept ChromaLab's existing safety rule: MTP is text-only for GGUF chat.
- MTP remains disabled for `mmproj` vision paths and strict chromatogram
  analysis until quality/memory validation exists.

Current ChromaLab behavior that must remain:

- MTP may accelerate text-only chat.
- MTP must not run with multimodal `mmproj` chromatogram analysis.
- MTP must not alter graph geometry, calibration, trace, or peak metrics.

### 4. LiteRT-LM Android

Repository: <https://github.com/google-ai-edge/LiteRT-LM>

Google Maven metadata:

- `com.google.ai.edge.litertlm:litertlm-android`
- latest/release: `0.12.0`
- metadata `lastUpdated`: `20260518174626`

Action:

- Replaced floating `implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")`
  with version-catalog dependency `libs.litertlm.android`.
- Pinned `litertlm = "0.12.0"` in `gradle/libs.versions.toml`.

Reason:

- `latest.release` is acceptable in Google examples, but it makes ChromaLab
  builds non-reproducible. Pinning the currently latest Maven release gives the
  same modern runtime with deterministic builds.

## Not Updated In This Slice

### Qwen MTP model catalog

The repo still contains Qwen3.5 MTP model entries in `ModelRegistry`. Newer
Qwen3.6 MTP packages exist upstream, but changing model catalog entries is a
separate product decision because it requires:

- URL and file-size verification for every quant;
- Android download smoke tests;
- device memory budget updates;
- MTP A/B performance validation;
- confirmation that the models remain text-only and cannot enter chromatogram
  numeric analysis.

This runtime slice keeps the model catalog unchanged.

## Validation

Commands run:

```powershell
.\gradlew.bat :androidApp:assembleValidation
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
```

Result:

- All commands passed after the `llama.cpp b9464` and LiteRT-LM `0.12.0` updates.
