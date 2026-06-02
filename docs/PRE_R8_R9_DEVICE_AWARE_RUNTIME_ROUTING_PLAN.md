# Pre-R8/R9 Device-Aware Runtime Routing Plan

Date: 2026-06-02

Status: planning contract only. No runtime code is changed in this slice.

## Product Intent

On first launch, ChromaLab should understand the phone well enough to recommend
and later run the best safe model/runtime path for that device.

For flagship Android devices, especially Qualcomm Snapdragon 8 Gen 3 and newer
8-series devices, the app should prefer GPU or NPU when the model package and
runtime evidence support it. If a model is compiled for a specific processor,
the app should visibly mark it as the best match for that phone and use the
matching accelerator rather than treating it like a generic CPU/GPU bundle.

This must not change chromatographic math or let VLM output become numeric
authority.

## Current Gap

Local code already has partial device-aware selection:

- `ModelDeviceSelector.detectDeviceTarget(...)` detects:
  - `QUALCOMM_SM8750`;
  - `QUALCOMM_QCS8275`;
  - `GOOGLE_TENSOR_G5`;
  - `GENERIC`.
- Chromatogram pipeline selection rejects nonmatching device-specific bundles.
- Model registry includes:
  - generic `gemma4-e2b`;
  - `gemma4-e2b-qualcomm-sm8750`;
  - `gemma4-e2b-qualcomm-qcs8275`;
  - `gemma4-e2b-google-tensor-g5`;
  - generic `gemma4-e4b`.

But the routing is not production-grade yet:

1. Chat activation does not enforce `matchesCurrentDeviceTarget(...)`.
2. `LiteRTEngine` uses `Backend.GPU()` then `Backend.CPU()` only.
3. The installed `litertlm-android:0.12.0` AAR exposes `Backend.NPU`, but the
   app does not route NPU-targeted bundles through it.
4. Download smoke checks do not prove runtime load or first-token success.
5. A downloaded but broken device-specific bundle can block the generic E2B
   fallback.
6. UI does not clearly label "best for this phone", "runtime verified", or
   "fallback selected".

## Device And Accelerator Model

The app needs three separate concepts:

1. Device profile:
   - SoC target;
   - flagship tier;
   - memory/storage;
   - ABI;
   - GPU/OpenCL/Vulkan availability;
   - LiteRT backend support;
   - native library directory for NPU backend.
2. Model target:
   - generic;
   - exact Qualcomm target;
   - exact Tensor target;
   - deployment mode: `FAST`, `FULL_ANALYSIS`, `GENERAL`;
   - supported backends: `NPU`, `GPU`, `CPU`, `VULKAN`, `GGUF_CPU`.
3. Runtime health:
   - download verified;
   - local file current/stale;
   - backend load verified;
   - first-token verified;
   - vision load verified if needed;
   - failure reason and fallback.

## Initial Device Target Matrix

| Device class | Detection signals | Preferred path | Fallback path |
| --- | --- | --- | --- |
| Snapdragon 8 Elite / SM8750 | `sm8750`, `snapdragon 8 elite` | `gemma4-e2b-qualcomm-sm8750` on LiteRT `NPU` after smoke pass | generic `gemma4-e2b` on GPU, then CPU |
| Qualcomm QCS8275 / Dragonwing IQ8 | `qcs8275`, `dragonwing iq8` | `gemma4-e2b-qualcomm-qcs8275` on LiteRT `NPU` after smoke pass | generic `gemma4-e2b` on GPU, then CPU |
| Snapdragon 8 Gen 3 / SM8650 | `sm8650`, `snapdragon 8 gen 3`, flagship Adreno signal | no current exact `.litertlm` bundle; use generic `gemma4-e2b` on GPU first | generic `gemma4-e2b` CPU; GGUF Vulkan path only if preflight passes |
| Snapdragon 8 Elite Gen 5 / newer 8-series | exact future SoC signal when known | exact future package only if registered and smoke-passed | generic E2B GPU/CPU; do not reuse SM8750/QCS8275 package blindly |
| Google Tensor G5 | `tensor g5`, `gs501` | `gemma4-e2b-google-tensor-g5` after smoke pass | generic E2B GPU/CPU |
| Unknown / midrange / weak device | none | generic E2B CPU/GPU only if memory-safe | lighter model or no model; deterministic pipeline remains separate |

The key rule: exact device-specific packages are exact-match only. A QCS8275
package is not a Snapdragon 8 Elite package.

## Phased Implementation Plan

### Step 1 - Device Capability Profiler

Goal:

Create one startup profiler that records device capabilities before model
selection.

Fields:

- `Build.MANUFACTURER`, `BRAND`, `MODEL`, `HARDWARE`, `BOARD`, `PRODUCT`;
- `SUPPORTED_ABIS`;
- RAM total/available;
- storage available;
- detected SoC target;
- flagship tier;
- package `applicationInfo.nativeLibraryDir`;
- LiteRT AAR backend classes available: CPU/GPU/NPU;
- Vulkan/OpenCL preflight if cheap and safe;
- low-RAM/conservative flag.

Output:

- `DeviceCapabilityProfile`;
- exported `DeviceCapabilityDiagnostic`;
- startup log line;
- safe user-facing model recommendation string.

Validation:

- unit tests for SM8750, SM8650, QCS8275, Tensor G5, unknown devices;
- no Android runtime algorithm changes.

### Step 2 - Model Capability Matrix

Goal:

Extend model metadata so every model declares what it can run on and which
accelerators are valid.

New metadata should express:

- exact target or generic target;
- preferred backend order;
- whether target match is strict;
- whether runtime smoke is required;
- deployment mode;
- minimum RAM/storage;
- stale-file hash/size guard if known.

Example intent:

- `gemma4-e2b-qualcomm-sm8750`: strict SM8750, backend order `NPU`, then generic
  fallback model, not GPU/CPU on the same bundle.
- `gemma4-e2b`: generic, backend order `GPU`, `CPU`.
- `gemma4-e4b`: generic full-analysis, backend order `GPU`, `CPU`, memory gated.

Validation:

- catalog tests;
- nonmatching strict bundle rejected for chat and pipeline;
- generic fallback stays available.

### Step 3 - Unified Runtime Selector

Goal:

Replace separate chat/pipeline selection behavior with one selector that works
for both:

- chat;
- chromatogram semantic/VLM assistance;
- validation fixture mode.

The selector should return:

- recommended model id;
- recommended backend;
- reason;
- rejected candidates;
- fallback chain;
- user-visible label:
  - "Best for this phone";
  - "Recommended generic fallback";
  - "Downloaded but not runtime verified";
  - "Not for this processor".

Rules:

- user explicit selection is allowed only when device target is compatible, or
  it must show a clear warning and not silently load;
- exact NPU package wins only after target match and runtime smoke pass;
- if exact package fails, generic E2B fallback is selected automatically if
  downloaded and compatible;
- E2B remains FAST/weaker-device production baseline.

Validation:

- chat and pipeline use identical target compatibility decisions;
- QCS8275 cannot load on SM8750 by default;
- SM8750 exact package outranks generic E2B only after smoke pass.

### Step 4 - LiteRT Backend Router

Goal:

Run each LiteRT package through the backend it was built for.

Backend routing:

- strict Qualcomm packages: try `Backend.NPU(nativeLibraryDir)` first;
- generic packages: try `Backend.GPU()` then `Backend.CPU()`;
- conservative devices: CPU first unless GPU was smoke-passed;
- if NPU crashes or fails, mark that exact package unhealthy and fallback to
  generic E2B, not to GPU/CPU on the NPU-only package unless evidence proves it
  works.

Required diagnostics:

- backend attempted;
- `Engine.initialize()` result;
- first-token result;
- first-response latency;
- timeout;
- sanitized exception;
- fallback selected.

Validation:

- `LiteRTEngine` backend-order unit tests where possible;
- Android validation smoke on attached SM8750 device.

### Step 5 - Runtime Smoke Gate

Goal:

Do not label a device-specific package as production-ready until it passes a
small runtime smoke test on that device/package id.

Smoke stages:

1. local file exists and size/hash matches current registry;
2. construct backend;
3. create `EngineConfig`;
4. initialize `Engine`;
5. create conversation;
6. generate a tiny text-only response;
7. optionally load one small image prompt for vision-capable use.

Stored result:

- `PASSED`;
- `LOAD_FAILED`;
- `FIRST_TOKEN_FAILED`;
- `TIMEOUT`;
- `CRASH_SUSPECTED`;
- `STALE_FILE`;
- `WRONG_DEVICE_TARGET`.

The result should be cached with model id, file size/hash, app version, runtime
version, device target, and backend.

Validation:

- no model becomes "best for this phone" without smoke pass;
- failing strict package triggers generic fallback;
- stale local file requires re-download.

### Step 6 - User-Facing Model Manager Labels

Goal:

Make the model manager understandable.

Labels:

- "Best for your phone";
- "Recommended fallback";
- "Downloaded, runtime verified";
- "Downloaded, not tested";
- "Wrong processor target";
- "Failed on this phone";
- "Re-download required";
- "NPU";
- "GPU";
- "CPU".

The app should show:

- detected SoC;
- chosen model/backend;
- why this is chosen;
- what fallback will be used if the best package fails.

Validation:

- labels are consistent with selector output;
- no hidden fallback;
- no normal user report exposes private file paths.

### Step 7 - Evidence Export

Goal:

Runtime routing must be auditable.

Export:

- device capability profile;
- model recommendation;
- backend smoke result;
- load diagnostics;
- fallback chain;
- privacy-safe path class;
- runtime health cache entry.

This belongs in technical evidence, not normal user report content.

Validation:

- `RuntimeEvidencePackageValidator` sees selected model/backend;
- failed model path is technical-only and privacy-safe;
- no private Android path in user report.

### Step 8 - Android Acceptance Runs

Goal:

Prove the routing on real devices before claiming R8/R9 readiness.

Required runs:

- SM8750 / Snapdragon 8 Elite:
  - exact SM8750 package NPU smoke;
  - generic E2B GPU/CPU fallback;
  - chat first-token;
  - validation fixture semantic/VLM assistance.
- SM8650 / Snapdragon 8 Gen 3 if available:
  - generic E2B GPU/CPU;
  - verify no SM8750/QCS8275 strict package is chosen.
- QCS8275 if available:
  - exact QCS8275 package NPU smoke.

Acceptance:

- exact packages only when matching;
- GPU/NPU only after smoke pass;
- generic E2B fallback works;
- no crash/no silent failure;
- all failures have exact runtime class.

## Recommended Phase Breakdown

Do not bundle this into one large R8.

Suggested phases:

1. `R8A - Device Capability Profiler`
2. `R8B - Model Capability Matrix And Recommendation Labels`
3. `R8C - Unified Chat/Pipeline Runtime Selector`
4. `R8D - LiteRT Backend Router With NPU Support`
5. `R8E - Runtime Smoke Gate And Fallback Cache`
6. `R8F - Model Manager Device Recommendation UI`
7. `R8G - Runtime Evidence Export For Device Routing`
8. `R9 - Real Device Acceptance Runs`

Each phase should be committed separately and validated before the next starts.

## Sources Checked

- LiteRT-LM repository and v0.12.0 release notes:
  https://github.com/google-ai-edge/LiteRT-LM
- Google Maven metadata for `litertlm-android`, latest `0.12.0`:
  https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/maven-metadata.xml
- Gemma 4 E2B LiteRT-LM model card:
  https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Qualcomm Snapdragon 8 Elite official product page:
  https://www.qualcomm.com/products/mobile/snapdragon/smartphones/snapdragon-8-series-mobile-platforms/snapdragon-8-elite-mobile-platform
- Existing pre-R8/R9 blocker audit:
  `docs/PRE_R8_R9_LITERT_SNAPDRAGON_8_ELITE_AUDIT.md`

