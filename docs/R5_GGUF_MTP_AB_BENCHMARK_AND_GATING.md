# R5 GGUF Text-Only MTP A/B Benchmark And Gating

Date: 2026-06-02

Status: implemented as debug diagnostics and technical export.

## Scope

R5 adds a text-only GGUF MTP A/B benchmark path. It does not change
chromatogram image analysis, `mmproj` vision loading, peak detection,
calibration, or `CalculationEngine`.

The benchmark compares:

- `NO_MTP`: normal llama.cpp text generation.
- `DRAFT_MTP`: llama.cpp `draft-mtp` text generation with a bounded draft
  token profile.
- CPU backend.
- explicit Vulkan backend only when the native preflight reports an
  accelerated backend.

## Android Command

Use the validation/debug package and an installed text-only MTP GGUF model:

```powershell
adb shell am start -S `
  -n com.chromalab.app.validation/com.chromalab.app.MainActivity `
  -a com.chromalab.app.DEBUG_MTP_AB `
  --es modelId qwen35-mtp-4b-q4km `
  --es backend cpu `
  --ei draft 3 `
  --ei ctx 2048 `
  --ei batch 64 `
  --ei maxTokens 32 `
  --es prompt "Reply with exactly OK."
```

For Vulkan:

```powershell
adb shell am start -S `
  -n com.chromalab.app.validation/com.chromalab.app.MainActivity `
  -a com.chromalab.app.DEBUG_MTP_AB `
  --es modelId qwen35-mtp-4b-q4km `
  --es backend vulkan `
  --ei draft 3
```

The Vulkan run aborts and exports an inconclusive summary if the native
llama.cpp bridge does not expose an accelerated backend.

## Export Location

R5 writes technical artifacts under:

```text
/sdcard/Download/ChromaLab/runtime/mtp-ab/<run_id>/
```

Files:

- `gguf_mtp_ab_<run_id>.json`
- `gguf_mtp_ab_<run_id>.md`

The exported JSON uses schema:

```text
gguf-mtp-benchmark-1.0
```

The export records model id and model path class only. It does not export the
private absolute model path.

## Metrics

The summary records:

- prompt character count;
- model id;
- backend;
- context and batch tokens;
- MTP draft tokens;
- load time;
- first-token latency;
- total response duration;
- generated token count when exposed by the native streaming callback;
- total tokens/sec when generated token count is available;
- timeout/failure reason.

Native llama.cpp currently logs `drafted`, `accepted`, and acceptance rate in
logcat, but the Kotlin bridge does not yet return those counters. R5 therefore
exports these fields as missing and keeps the gate in `REVIEW_ONLY` unless
native acceptance statistics are present.

## Gate Rules

The benchmark gate requires both `NO_MTP` and `DRAFT_MTP` passes.

Decisions:

- `ENABLE_FOR_PROFILE`: MTP has measured speedup, acceptable first-token
  latency, and native drafted/accepted acceptance evidence.
- `REVIEW_ONLY`: timing was measured, but native acceptance stats are missing
  or insufficient for automatic enablement.
- `KEEP_DISABLED`: MTP is slower, first-token latency regresses, or acceptance
  rate is too low.
- `INCONCLUSIVE`: benchmark preconditions failed or one pass failed.

Default thresholds:

- minimum speedup: `1.10x`;
- maximum first-token slowdown: `1.20x`;
- minimum acceptance rate: `0.30`.

## Safety Policy

MTP remains disallowed for:

- `mmproj` vision;
- strict chromatogram numeric analysis;
- chromatogram report generation;
- any path where MTP could alter graph geometry, calibration, trace, peak, or
  report-gate evidence.

R5 is only a text-only GGUF chat benchmark. It does not make MTP a vision or
chromatogram analysis accelerator.

## Validation

Implemented tests:

- benchmark gate requires both no-MTP and MTP passes;
- benchmark gate stays `REVIEW_ONLY` when native acceptance stats are missing;
- benchmark gate enables only when speedup and acceptance evidence pass;
- first-token regression keeps MTP disabled;
- MTP policy rejects `mmproj` vision and strict chromatogram numeric analysis;
- structured runtime diagnostics map GGUF MTP benchmark rows without private
  path leakage.

Build validation:

- `git diff --check`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.inference.GgufMtpBenchmarkGateTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.StructuredRuntimeDiagnosticGgufMtpTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.chat.ChatMtpRuntimeProfileTest"`
- `.\gradlew.bat :composeApp:compileKotlinDesktop`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :androidApp:assembleValidation`

## Remaining Work

Native drafted/accepted token counters should be returned directly from the
llama.cpp bridge in a later runtime phase if automatic MTP enablement is desired.
Until then, the benchmark can prove timing but cannot automatically promote MTP
to production-on for a device/profile.
