# R2 - LiteRT Device Selection And Model Discovery

Date: 2026-06-02

Status: implemented as a runtime/model-management slice.

## Scope

R2 adds deterministic device-aware selection for the Gemma 4 LiteRT catalog
introduced in R1. It does not start Phase 10 and does not change chromatogram
geometry, calibration, trace extraction, peak detection, report gates, or
`CalculationEngine`.

## Selection Rules

Manual selection remains highest priority, but device-specific LiteRT bundles
are only allowed when the current Android device explicitly matches their
target.

Automatic chromatogram VLM selection now follows:

1. If a user-selected chromatogram model exists, use it only if it is downloaded,
   compatible, and allowed on the current device target.
2. If a device-specific Gemma 4 E2B FAST bundle is downloaded and the device
   target matches, select it.
3. If no matching device-specific bundle is downloaded, fall back to generic
   `gemma4-e2b`.
4. If generic E2B is not downloaded, fall back to the existing chromatogram
   priority ranking.
5. Reject nonmatching device-specific bundles for automatic pipeline use.

LiteRT backend routing now follows model metadata instead of a generic
accelerated/not-accelerated flag:

- `gemma4-e2b-qualcomm-sm8750` is GPU-first on Snapdragon 8 Elite / SM8750;
- `gemma4-e2b-qualcomm-qcs8275` remains NPU-first for QCS8275 / Dragonwing IQ8;
- generic Gemma E2B remains GPU-first with CPU fallback.

Detected device targets:

| Target | Match signals |
| --- | --- |
| `QUALCOMM_SM8750` | `sm8750`, `snapdragon 8 elite` |
| `QUALCOMM_QCS8275` | `qcs8275`, `dragonwing iq8` |
| `GOOGLE_TENSOR_G5` | `tensor g5`, `gs501` |
| `GENERIC` | fallback when no exact signal is present |

## Discovery Diagnostics

R2 extends model availability diagnostics with:

- detected device target;
- selected device target;
- selection reason;
- fallback attempted/result;
- rejected model ids.

Android `ModelManager` can now build a discovery diagnostic for the selected
downloaded model:

- selected model id;
- expected backend;
- app-private expected model path;
- path exists yes/no;
- file size;
- selection reason;
- device target fields;
- fallback details.

R2 logs this diagnostic during `activateForPipeline`. Exporting these diagnostics
into runtime evidence is reserved for the later runtime diagnostics phase.

## Storage Locations

Downloaded models remain in app-private storage:

```text
<app filesDir>/models/<model-id>/<model-file>
```

Validation package builds use the same app-private model layout under the
validation package id. R2 does not share files between packages and does not
move models to public storage.

LiteRT cache remains:

```text
<app cacheDir>/litertlm/
```

## Safety Boundaries

R2 does not:

- make device-specific E2B mandatory;
- auto-download any model;
- lower analysis quality when a device-specific bundle is absent;
- let E2B/VLM change graph count, calibration, trace, peaks, or numeric metrics;
- allow nonmatching device-specific bundles into automatic pipeline selection.

## Tests

R2 adds tests for:

- exact device-specific E2B match;
- generic E2B fallback when device-specific bundle is not downloaded;
- rejection of nonmatching device-specific bundle;
- explicit selection still requiring a device match;
- fallback to existing chromatogram priority when generic E2B is absent.

## Validation

Required validation for this slice:

```powershell
git diff --check
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.model.*"
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
.\gradlew.bat :androidApp:assembleValidation
```
