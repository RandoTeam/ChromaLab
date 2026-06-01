# R3 - LiteRT-LM MTP Capability And Performance Probe

Date: 2026-06-02

Status: implemented as diagnostics only.

## Scope

R3 adds LiteRT-LM runtime diagnostics for MTP/speculative-decoding capability and
basic performance timing. It does not start Phase 10 and does not change
chromatogram geometry, tick localization, calibration, trace extraction, peak
detection, report gates, or `CalculationEngine`.

## Local LiteRT-LM API Audit

Current dependency:

```text
com.google.ai.edge.litertlm:litertlm-android:0.12.0
```

The local AAR exposes:

- `com.google.ai.edge.litertlm.ExperimentalFlags.enableSpeculativeDecoding`;
- `com.google.ai.edge.litertlm.Capabilities.hasSpeculativeDecodingSupport()`;
- `com.google.ai.edge.litertlm.BenchmarkInfo` with init and time-to-first-token
  timing fields.

R3 treats these as LiteRT-LM MTP/speculative-decoding diagnostics. It does not
enable speculative decoding and does not route chromatogram analysis through a
new generation mode.

## Diagnostics Added

R3 adds a common diagnostics contract:

- `LiteRtMtpCapabilityDiagnostic`;
- `LiteRtPerformanceDiagnostic`;
- `LiteRtRuntimeDiagnostics`;
- `LiteRtMtpCapabilityPolicy`.

Runtime fields:

- model supports MTP/speculative decoding: `YES`, `NO`, `UNKNOWN`;
- runtime exposes MTP/speculative control: `EXPOSED`, `NOT_EXPOSED`, `UNKNOWN`;
- MTP enabled state: `ENABLED`, `DISABLED`, `UNAVAILABLE`;
- reason string;
- model load time;
- first response latency;
- total response duration;
- timeout status;
- response character count.

## Runtime Behavior

Android `LiteRTEngine` now:

1. probes `Capabilities(modelPath).hasSpeculativeDecodingSupport()`;
2. reads `ExperimentalFlags.enableSpeculativeDecoding`;
3. records whether the runtime exposes speculative decoding controls;
4. records load duration;
5. records first response latency and total response duration for normal and
   streaming inference;
6. records timeout status.

The probe is diagnostic-only. R3 does not call `setEnableSpeculativeDecoding`
and does not change sampling, prompts, model choice, or analysis gates.

## Safety Boundaries

R3 preserves:

- E2B as the supported FAST/weaker-device production baseline;
- deterministic geometry/calibration/trace/peak authority;
- VLM/E2B advisory-only boundaries;
- no VLM numeric chromatographic metrics;
- no MTP for GGUF `mmproj` or vision analysis changes.

## Tests

R3 adds policy tests for:

- runtime does not expose MTP/speculative controls;
- model support is unknown;
- model explicitly does not support speculative decoding;
- supported model with flag disabled;
- supported model with flag enabled.

## Validation

Required validation for this slice:

```powershell
git diff --check
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.inference.LiteRtMtpCapabilityPolicyTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.model.*"
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
.\gradlew.bat :androidApp:assembleValidation
```

Android device execution is optional for R3. If a Gemma LiteRT model is installed,
the `ChromaLabLiteRT` / `ChromaLabModels` logs will include the runtime
diagnostics after model load and inference.

## Closeout Validation

R3 validation completed on 2026-06-02:

- `git diff --check` passed;
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.inference.LiteRtMtpCapabilityPolicyTest"` passed;
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.model.*"` passed;
- `.\gradlew.bat :composeApp:compileKotlinDesktop` passed;
- `.\gradlew.bat :composeApp:assembleAndroidMain` passed;
- `.\gradlew.bat :androidApp:assembleValidation` passed.

`adb devices` reported no attached device, so no Android LiteRT model-run was
performed in R3.
