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

## Phase 8C Hardening Update

## Phase 8D Tick Localization / Calibration Update

Phase 8D adds graph-level terminal failure evidence for Android fixture runs that reach graph processing but fail before calibrated signal/report generation.

Changes:

- `RuntimeEvidencePackage` schema advanced to `runtime-evidence-1.2`.
- Terminal validation exports now include `graph_failure_package_<run_id>.json`.
- Validator emits `graph_failure.package_missing` when a graph-stage terminal failure has no graph-level failure package.
- Android tick localization now has deterministic label-band projection rescue for X/Y tick candidate pixels.
- OCR/VLM remain text-only helpers; they cannot create tick pixel positions, calibration, or chromatographic metrics.

Expected next Android rerun:

- The White Tiger fixture may still report `BLOCKED` if calibration remains invalid.
- The artifact package must show selected graphPanel/plotArea, tick candidate counts, accepted/rejected anchors, calibration status, and exact missing artifact reasons.

Phase 8C changes the acceptance target: missing VLM is no longer allowed to stop deterministic geometry before it starts. The validation package must now:

- export `modelAvailabilityDiagnostics`;
- continue deterministic graphPanel/plotArea/axis attempts when VLM is unavailable;
- fail validator check `package.deterministic_fallback_not_attempted` if model unavailability prevents those attempts;
- classify later failures by deterministic stage (`GRAPH_PANEL_FAILURE`, `AXIS_DETECTION_FAILURE`, `TICK_LOCALIZATION_FAILURE`, `CALIBRATION_FAILURE`) rather than primary `VLM_MODEL_UNAVAILABLE`.

Phase 8 remains blocked until an Android fixture rerun confirms this behavior and exports updated artifacts.

Phase 8C rerun `white_tiger_ion71_20260520_170118` confirms the pre-geometry VLM blocker is fixed: deterministic stages reached `GRAPH_SELECTION`, `GRAPH_ROI`, `AXIS_DETECTION`, `OCR_SUGGESTION`, `X_CALIBRATION`, and `Y_CALIBRATION`. The remaining terminal status is `BLOCKED` with `runtimeFailureClass = TICK_LOCALIZATION_FAILURE`, caused by insufficient Y tick labels for automatic calibration. Phase 9 remains blocked until Product/QA accept the remaining Phase 8 limitations or the tick/calibration failure is fixed.

## Phase 8D Closure Update

Verdict: `PHASE_8_CLOSED`

Phase 9 may start: **Yes**, after this Phase 8D commit.

Final Android fixture run: `white_tiger_ion71_20260520_184550`.

Phase 8D fixed the remaining Android tick/calibration evidence blocker:

- deterministic graph processing no longer depends on startup VLM loading for validation fixtures;
- graph-stage terminal failures now export `RuntimeGraphFailurePackage` evidence;
- the validator fails graph-stage terminal runs that omit graph-level failure packages;
- deterministic tick localization now includes label-band projection rescue;
- OCR tick values are accepted only when paired to deterministic tick pixels;
- the final fixture run exported one graph package, valid X/Y calibration evidence, trace/peak overlays, report HTML/Markdown/JSON, runtime evidence package, and validator JSON/Markdown.

Final run summary:

| Field | Value |
| --- | --- |
| Report gate | `REVIEW_ONLY` |
| Validator verdict | `REVIEW` |
| Blocking validator issues | 0 |
| Runtime failure class | `VLM_SEMANTIC_LAYER_UNAVAILABLE` |
| Graph count | 1 |
| X/Y calibration | `VALID` / `VALID` |
| Trace/peak evidence | Present |
| Release-ready claim | No |

The remaining review reason is the intentionally disabled/unavailable VLM semantic layer in the validation build. Deterministic graphPanel, plotArea, tick localization, calibration, trace, peak, and report export are no longer blocked for the White Tiger fixture.
