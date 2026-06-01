# R4 - Structured Runtime Diagnostics Export

Date: 2026-06-02

Status: implemented as a runtime evidence export slice.

## Scope

R4 makes runtime/model behavior auditable in exported technical evidence JSON.
It does not start Phase 10 and does not change chromatogram geometry,
calibration, trace extraction, peak detection, report gates, or
`CalculationEngine`.

## Export Contract

R4 adds `structuredRuntimeDiagnostics` to `RuntimeEvidencePackage` schema
`runtime-evidence-1.3`.

Each diagnostic records:

- diagnostic id;
- runtime source:
  - `MODEL_DISCOVERY`;
  - `LITERT_LM`;
  - `LLAMA_CPP`;
  - `MTMD_MMPROJ`;
  - `GGUF_MTP_TEXT_ONLY`;
  - `VULKAN_PREFLIGHT`;
  - `UNKNOWN`;
- model id;
- model path class, not the absolute model path;
- backend;
- load attempted/result;
- load time;
- first response latency;
- total response duration;
- timeout budget/status;
- fallback reason;
- MTP/speculative fields when present;
- export privacy class.

## Privacy Rule

Structured runtime diagnostics are technical evidence, not normal user report
content. The exported diagnostic stores only a path class:

- `APP_PRIVATE_MODEL`;
- `VALIDATION_PACKAGE_PRIVATE_MODEL`;
- `PUBLIC_DOWNLOAD_EXPORT`;
- `USER_SELECTED_EXTERNAL`;
- `NOT_AVAILABLE`;
- `UNKNOWN`.

It does not store private absolute paths such as Android `/data/user/0/...` or
desktop `C:/Users/...` model locations in user-facing report Markdown/HTML.

The validator blocks structured runtime diagnostics classified as `USER_REPORT`
and blocks private path leaks in exported string fields.

## Integration

R4 integrates structured diagnostics into:

- `RuntimeEvidencePackage`;
- `RuntimeEvidencePackageBuilder`;
- `DebugPackageExporter`;
- `CalculationRunReportOptions`;
- `StoredReportMetadata`;
- `CalculationRunReportOptionsBuilder`;
- `CalculationRunReportExporter`;
- `RuntimeEvidencePackageValidator` JSON/Markdown validation summaries.

Existing `ModelAvailabilityDiagnostic` entries are mapped into
`MODEL_DISCOVERY` structured diagnostics automatically, so current terminal
failure/export paths produce a structured runtime row without extra Android UI
changes.

R3 `LiteRtRuntimeDiagnostics` can be mapped into `LITERT_LM` structured
diagnostics through `StructuredRuntimeDiagnosticMapper.fromLiteRt`.

## Safety Boundaries

R4 does not:

- expose raw model paths in user reports;
- include runtime diagnostics in normal report Markdown/HTML;
- enable LiteRT speculative decoding;
- enable GGUF MTP;
- modify numeric chromatographic metrics;
- alter deterministic geometry/calibration/trace/peak behavior.

## Validation

Required validation for this slice:

```powershell
git diff --check
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RuntimeEvidencePackageValidatorTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.inference.LiteRtMtpCapabilityPolicyTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.*"
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
.\gradlew.bat :androidApp:assembleValidation
```

Android runtime rerun is optional for R4. R9 is the planned phase for runtime
evidence integration reruns across Android validation fixtures.
