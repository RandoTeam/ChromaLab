# R6 - GGUF llama.cpp Vulkan Runtime Matrix

Status: implemented as a debug/validation diagnostic path.

## Scope

R6 measures whether the current llama.cpp runtime can use an accelerated Android
backend safely for text-only GGUF chat models.

The matrix runs three profiles:

| Profile | Behavior |
| --- | --- |
| `CPU` | Always runs llama.cpp with `preferAccelerated = false`. |
| `EXPLICIT_VULKAN` | Runs only when native preflight reports an accelerated backend code. If not, it is skipped with a fallback reason. |
| `AUTO` | Runs accelerated when preflight allows it; otherwise falls back to CPU and records the fallback reason. |

This phase does not change graph analysis, chromatographic math, calibration,
trace extraction, peak detection, or report gates.

## Debug Entry Point

Validation/debug APK command:

```powershell
adb shell am start -S `
  -n com.chromalab.app.validation/com.chromalab.app.MainActivity `
  -a com.chromalab.app.DEBUG_VULKAN_MATRIX `
  --es modelId qwen35-mtp-4b-q4km `
  --es prompt "Reply with exactly OK." `
  --ei ctx 2048 `
  --ei batch 64 `
  --ei maxTokens 32
```

`modelId` is optional. If omitted, the runner uses the active downloaded
llama.cpp chat model or the first downloaded llama.cpp chat model.

## Export

Artifacts are written to:

```text
/sdcard/Download/ChromaLab/runtime/vulkan-matrix/<run_id>/
```

Files:

- `gguf_vulkan_matrix_<run_id>.json`
- `gguf_vulkan_matrix_<run_id>.md`

The export privacy class is `TECHNICAL_EVIDENCE`. User reports must not include
private model paths or raw logs.

## Preflight Evidence

The preflight records:

- Android device name and SDK;
- native llama.cpp backend codes;
- whether an accelerated backend is available;
- Vulkan hardware feature flags exposed by Android package manager;
- selected backend hint;
- fallback reason if accelerated runtime is unavailable.

Native backend code meaning:

- `0`: CPU;
- `1`: accelerated ggml backend, currently treated as the Vulkan/accelerated path.

## Gate Rules

The matrix gate is diagnostic, not a product release gate.

- CPU baseline is required.
- If no accelerated backend is available, decision is `CPU_DEFAULT` with
  verdict `VULKAN_UNAVAILABLE_CPU_FALLBACK`.
- Vulkan is marked beneficial only when total runtime beats CPU by at least 10%
  and first-token latency does not exceed the CPU baseline by more than 25%.
- If Vulkan or AUTO fails, the result is `REVIEW_ONLY`.

## Safety Boundaries

- No forced Vulkan on unsupported devices.
- No MTP is enabled by the Vulkan matrix itself.
- No mmproj/vision path uses this benchmark.
- No deterministic chromatogram geometry, calibration, trace, peak, or report
  calculation changes are made by R6.

## Validation

Implemented validation:

- `GgufVulkanRuntimeMatrixTest`
- `StructuredRuntimeDiagnosticVulkanMatrixTest`
- Android validation build

Device benchmark still requires an attached Android device with a downloaded
text-only GGUF chat model.
