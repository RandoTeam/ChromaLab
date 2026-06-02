# Pre-R8/R9 LiteRT Snapdragon 8 Elite Runtime Audit

Date: 2026-06-02

Status: audit-only blocker note. No algorithmic fixes were made.

## Problem

Before R8/R9, the Qualcomm-specific LiteRT-LM Gemma 4 E2B bundles must be
treated as unproven in ChromaLab. The reported runtime symptom is:

- Snapdragon 8 Elite LiteRT variants do not load, crash, or fail in chat;
- two Qualcomm variants are involved;
- generic E2B remains the safer baseline until runtime smoke evidence proves
  the device-specific bundles.

This is a model/runtime compatibility issue, not a chromatogram math or
`CalculationEngine` issue.

## Local Models In Scope

The two Qualcomm entries currently registered are:

| Model id | File | Target | Registry size | Remote HEAD size / hash status |
| --- | --- | --- | ---: | --- |
| `gemma4-e2b-qualcomm-sm8750` | `gemma-4-E2B-it_qualcomm_sm8750.litertlm` | Qualcomm SM8750 / Snapdragon 8 Elite | 3,016,294,400 bytes | Matches current HF `X-Linked-Size`; current `X-Linked-ETag` is `41dd675fbe735b6029012b5576a5716bac614fd8156de0128db4c9dff3cebd4e`. |
| `gemma4-e2b-qualcomm-qcs8275` | `gemma-4-E2B-it_qualcomm_qcs8275.litertlm` | Qualcomm QCS8275 / Dragonwing IQ8 | 3,294,593,024 bytes | Matches current HF `X-Linked-Size`; current `X-Linked-ETag` is `ebc103e548e76a34b8114a1dc1dd6d33a893c38add108d8bae8165b51f320f02`. |

The generic fallback remains:

| Model id | File | Target | Registry size |
| --- | --- | --- | ---: |
| `gemma4-e2b` | `gemma-4-E2B-it.litertlm` | Generic Android CPU/GPU | 2,588,147,712 bytes |

## Upstream State Checked

- Hugging Face `litert-community/gemma-4-E2B-it-litert-lm` documents `.litertlm`
  as the LiteRT-LM deployment format and lists Android CPU/GPU benchmarks for
  generic E2B plus Dragonwing IQ8 NPU benchmark data.
- The same model card says Android AI Core / Gemini Nano is the recommended
  production path on supported Android devices, which is relevant because these
  standalone `.litertlm` bundles still need app-side runtime validation.
- The `gemma-4-E2B-it_qualcomm_sm8750.litertlm` file was replaced upstream; the
  current file hash/size now matches ChromaLab's registry, but older local
  downloads may still be stale.
- `litertlm-android` latest Google Maven release is `0.12.0`, last updated
  `20260518174626`; ChromaLab already uses `0.12.0`.
- The local `litertlm-android-0.12.0.aar` exposes `Backend.NPU`, but
  ChromaLab's `LiteRTEngine` currently tries only `Backend.GPU()` then
  `Backend.CPU()`.
- Public Google AI Edge Gallery issue #557 shows the same broad failure class:
  Gemma 4 LiteRT engine creation can fail with `Failed to create engine:
  INTERNAL` during model setup.
- Public LiteRT issue #2793 shows Qualcomm SM8750 NPU compilation is a distinct
  target path; Qualcomm-specific packages should not be treated like generic
  CPU/GPU bundles.

Sources:

- https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it_qualcomm_sm8750.litertlm
- https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it_qualcomm_qcs8275.litertlm
- https://github.com/google-ai-edge/LiteRT-LM
- https://github.com/google-ai-edge/gallery/issues/557
- https://github.com/google-ai-edge/LiteRT/issues/2793
- https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/maven-metadata.xml

## Local Root-Cause Findings

### 1. Chat path does not enforce device target

`ModelDeviceSelector` rejects nonmatching device-specific bundles for
chromatogram pipeline selection, but chat activation goes through
`ModelManagerController.activateForChat(...)`.

That chat path calls `manager.canLoadForText(model.info)`, and
`canLoadForText(...)` does not check `matchesCurrentDeviceTarget(model)`.

Impact:

- `gemma4-e2b-qualcomm-qcs8275` can be selected for chat on a Snapdragon 8 Elite
  device even though it targets Dragonwing IQ8 / QCS8275.
- The app can attempt to load a device-specific NPU bundle on the wrong
  Qualcomm target, which is expected to fail or behave unpredictably.

### 2. ChromaLab does not use LiteRT-LM NPU backend

`LiteRTEngine.loadModel(...)` currently builds this backend list:

```kotlin
val backends = if (preferGpu) {
    listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU())
} else {
    listOf("CPU" to Backend.CPU())
}
```

The installed `litertlm-android:0.12.0` AAR exposes:

```text
com.google.ai.edge.litertlm.Backend$CPU
com.google.ai.edge.litertlm.Backend$GPU
com.google.ai.edge.litertlm.Backend$NPU
```

`Backend.NPU` has an optional `nativeLibraryDir` constructor argument.

Impact:

- The SM8750 bundle is a Snapdragon 8 Elite / Qualcomm NPU-targeted package, but
  the app currently tries to load it through GPU/CPU.
- A runtime failure in `Engine(engineConfig)` or `sendMessageAsync(...)` is
  consistent with this mismatch.

### 3. Download smoke checks are not runtime smoke checks

R1 verifies HEAD size, storage, and partial-download cleanup. It does not prove:

- `EngineConfig` can be created;
- `Engine.initialize()` succeeds;
- `createConversation()` succeeds;
- first text-only chat response succeeds;
- vision-enabled load succeeds;
- NPU backend works on the current Android package.

Impact:

- A model may be complete on disk and still unusable at runtime.
- R8/R9 must not assume device-specific LiteRT bundles are production-ready
  because they passed download preflight.

### 4. No load-failure fallback from device-specific E2B to generic E2B

Pipeline selection prefers a downloaded matching device-specific bundle. If that
bundle fails runtime load, `activateForPipeline(...)` catches the error and
returns `false`; it does not immediately retry generic `gemma4-e2b`.

Impact:

- On SM8750, a downloaded but broken `gemma4-e2b-qualcomm-sm8750` can block the
  generic E2B fallback that may work.
- Chat has the same practical issue: load failure clears active state but does
  not guide the user to generic E2B as the safe fallback.

## Current Diagnosis

The failing "two LiteRT versions" are best classified as:

1. `gemma4-e2b-qualcomm-sm8750`: likely correct target for Snapdragon 8 Elite,
   but currently launched through the wrong backend family because ChromaLab does
   not use `Backend.NPU`.
2. `gemma4-e2b-qualcomm-qcs8275`: not a Snapdragon 8 Elite bundle; it should not
   be loadable for chat on SM8750 unless the user explicitly bypasses a
   diagnostic warning. It targets Dragonwing IQ8 / QCS8275.

This is not caused by an outdated LiteRT-LM dependency: `0.12.0` is currently the
latest public Google Maven release. The immediate issue is missing Qualcomm NPU
runtime handling and missing runtime-load smoke gates.

## Required Before R8/R9

Do not start R8/R9 assuming the Qualcomm LiteRT bundles are ready.

Minimum precondition for using device-specific LiteRT bundles:

1. Add a debug/runtime smoke command for LiteRT-LM:
   - model id;
   - selected backend: CPU/GPU/NPU;
   - load attempted/result;
   - `Engine.initialize()` result;
   - first chat token/result;
   - timeout;
   - sanitized exception;
   - fallback attempted/result.
2. Enforce device target for chat activation:
   - QCS8275 bundle must not load on SM8750 by default.
3. Add NPU backend handling for device-specific Qualcomm bundles:
   - try `Backend.NPU(nativeLibraryDir = applicationInfo.nativeLibraryDir)` for
     NPU-targeted packages;
   - keep generic E2B on CPU/GPU.
4. Add runtime fallback:
   - if device-specific E2B load fails, retry generic E2B if downloaded and
     compatible;
   - export both failed and fallback diagnostics.
5. Detect stale local file:
   - compare local byte size and, if practical, checksum/hash against current HF
     metadata;
   - if stale, require re-download before runtime test.

## Suggested Work Slice

Title:

`Pre-R8/R9 - LiteRT Qualcomm Runtime Smoke Gate`

Scope:

- no chromatogram algorithm changes;
- no `CalculationEngine` changes;
- add runtime diagnostics only;
- add chat target guard;
- add NPU smoke path;
- keep generic E2B as safe FAST fallback.

Suggested commit message:

`Add LiteRT Qualcomm runtime smoke gate`

## ADB Evidence To Collect On Device

When a Snapdragon 8 Elite device is attached, collect:

```powershell
adb shell getprop ro.hardware
adb shell getprop ro.board.platform
adb shell getprop ro.product.model
adb logcat -c
adb logcat -s ChromaLabLiteRT ChromaLabModels AndroidRuntime DEBUG
```

For each model:

- `gemma4-e2b`;
- `gemma4-e2b-qualcomm-sm8750`;
- `gemma4-e2b-qualcomm-qcs8275`.

Record:

- backend attempted;
- exact sanitized load error;
- whether `Engine.initialize()` fails or chat generation fails later;
- whether generic E2B fallback works.
