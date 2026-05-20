# Phase 8B Closeout Report

Verdict: `PHASE_8B_BLOCKED_RUNTIME_FAILURE`

Phase 9 may start: **No**. The install/signature blocker was resolved safely, and the fixture run exported artifacts, but autonomous validation is blocked by missing chromatogram VLM availability on the device.

## Agents Activated

| Agent | Output |
| --- | --- |
| Orchestrator | Kept scope to fixture-driven Phase 8B validation and prevented Phase 9 work. |
| QA / Regression Agent | Added fixture contracts, metadata tests, asset test, artifact manifest slots, and validator failure-class tests. |
| Android Performance & On-Device AI Agent | Added direct debug intent entrypoint and app-private fixture copy into the existing processing flow. |
| Geometry / Calibration Core Agent | Added `VALIDATION_FIXTURE` source provenance for geometry and report mapping. |
| OCR / VLM / Text Semantics Agent | Preserved OCR/VLM runtime path; no model behavior or numeric boundary was changed. |
| VLM Evaluation Agent | Preserved VLM numeric boundaries; fixture mode does not let model output become numeric metrics. |
| Trace Extraction / Peak Review Agent | Preserved trace/peak pipeline path and artifact slots for overlays. |
| Scientific Reporting & Validation Agent | Added final report contract export and `runtimeFailureClass` propagation into the report contract. |
| Product Acceptance Agent | Confirmed this is developer validation, not a manual-first product path. |
| Security & Privacy Agent | Kept exports in a validation-specific directory and avoided normal user report log expansion. |
| Mobile UX Architect Agent | Added a debug-labeled capture hub entry point. |
| Compose/KMP UI Agent | Wired the debug action through Compose navigation into the existing processing route. |

## Phase 8B Unblock Workstream Update

| Agent | Output |
| --- | --- |
| Orchestrator | Selected non-destructive side-by-side validation install and kept Phase 9 blocked after runtime model failure. |
| Android Performance & On-Device AI Agent | Verified device `a36d1946`, installed `com.chromalab.app.validation`, launched fixture by ADB, captured model-unavailable runtime failure. |
| QA / Regression Agent | Added validation build packaging for fixture assets, terminal failure evidence export, and a regression test for explicit model-unavailable failure classes. |
| Security & Privacy Agent | Avoided uninstalling `com.chromalab.app`; preserved app data and kept logcat outside user-facing report exports. |
| Product Acceptance Agent | Classified result as blocked because no autonomous graph analysis occurred without the required VLM. |
| Mobile UX Architect Agent | Inspected the real screen state: the app shows a clear mandatory-model error with Retry/Cancel actions. |

## Skills Used

`real-device-validation`, `android-runtime-profiling`, `evidence-package-validator`, `golden-artifact-testing`, `test-plan-authoring`, `timeout-cache-design`, `ocr-local-crops`, `vlm-safe-assistant`, `geometry-calibration-robust-fit`, `trace-extraction-masks`, `peak-review-integration`, `report-gate-provenance`, `secure-export-review`, `log-safety-audit`, `definition-of-done`.

## Fixture Location

- `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg`
- `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.metadata.json`

## Runner Summary

`AutonomousValidationFixtureRunner` reads the metadata asset, copies the fixture image into `filesDir/captures/validation/<run_id>/`, records start metadata, and returns an initial processing request with source type `VALIDATION_FIXTURE`.

## Debug Entrypoints

- UI: `Developer: Run validation fixture`
- ADB:

```powershell
.\gradlew.bat :androidApp:assembleValidation
adb install -r androidApp\build\outputs\apk\validation\androidApp-validation.apk
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

## Artifact Export

The structured report screen exports runtime evidence, validator JSON/Markdown, final report contract JSON, HTML/Markdown report, stage timings, overlay artifacts when available, and a manifest to `/sdcard/Download/ChromaLab/validation/<run_id>/`.

## Safe Install Resolution

Original install failure:

- package: `com.chromalab.app`
- error: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
- reason: existing install has a different signing certificate
- data risk: uninstalling would delete `/data/user/0/com.chromalab.app`

Resolution:

- selected Option B, side-by-side validation package;
- added validation build type with package `com.chromalab.app.validation`;
- installed `androidApp-validation.apk` successfully;
- existing production app data was not deleted.

## Android Run Result

Run id: `white_tiger_ion71_20260520_162317`.

Artifacts:

- device: `/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_162317/`
- local pulled copy: `artifacts/phase8b-android-validation/white_tiger_ion71_20260520_162317/`

Per-run verdict:

| Field | Value |
| --- | --- |
| Report gate | `BLOCKED` |
| Validator verdict | `FAIL` |
| Runtime failure class | `VLM_MODEL_UNAVAILABLE` |
| Graph count | `0` |
| graphPanel / plotArea | `MISSING` / `MISSING` |
| X/Y calibration | `MISSING` / `MISSING` |
| Trace / peaks | `MISSING` / `MISSING` |
| Axis failure reached | No |
| Evidence package exported | Yes |
| Validator JSON/Markdown exported | Yes |
| Report JSON/HTML/Markdown exported | Yes |

Blocking validator issues:

- `package.executed_runtime_missing`
- `package.model_metadata_missing`
- `package.graphs_missing`

The run stopped at `IMAGE_QUALITY` with:

`AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first.`

## Remaining Risk

Phase 8B cannot close until the required chromatogram VLM is installed/activated on the device and the fixture run proceeds past model readiness into actual graph/axis/calibration/trace/peak stages.

If the next run fails at axes/ticks/calibration, the failure must remain classified and diagnostic/review-gated.
