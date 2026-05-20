# Phase 8B Real Android Validation

Verdict: `PHASE_8B_BLOCKED_RUNTIME_FAILURE`.

Phase 9 may start: **No**. The fixture-driven Android validation path now installs and exports artifacts, but the real device run is blocked before image analysis because no chromatogram vision model is active on the device.

## Device State And Install Strategy

Device: `a36d1946`.

Installed production package before validation:

- package: `com.chromalab.app`
- versionName/versionCode: `0.0.5-beta.4` / `6`
- dataDir: `/data/user/0/com.chromalab.app`
- firstInstallTime/lastUpdateTime: `2026-05-19 00:07:56` / `2026-05-19 13:49:02`
- installer: shell / no installer package recorded
- signature digest in `dumpsys package`: `fc204f8f`

`adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, so the existing app was not uninstalled and no device data was deleted.

Selected strategy: **Option B - side-by-side validation package**.

- build type: `validation`
- package id: `com.chromalab.app.validation`
- APK: `androidApp/build/outputs/apk/validation/androidApp-validation.apk`
- install result: `adb install -r ...` succeeded.
- production `applicationId` remains `com.chromalab.app`.

## Fixture-Driven Android Validation Mode

Phase 8B adds a developer-only validation path that avoids camera, gallery, and Android photo picker ambiguity. Only acquisition is bypassed: the fixture image is copied from packaged assets into the app-private capture workspace and then submitted to the same processing route used by normal acquired images.

Fixture:

- id: `white_tiger_ion71`
- image asset: `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg`
- metadata asset: `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.metadata.json`
- expected graph count: `1`
- known historical failures: `AXIS_DETECTION_FAILURE`, `TICK_LOCALIZATION_FAILURE`, `OCR_TICK_FAILURE`, `CALIBRATION_FAILURE`
- expected current status: `DIAGNOSTIC_ONLY` or `REVIEW_ONLY` until autonomous axis/tick/calibration evidence passes

## UI Entry Point

Debug builds show:

`Developer: Run validation fixture`

The entry point is on the capture hub. It prepares the bundled fixture and navigates into the existing processing flow with source type `VALIDATION_FIXTURE`.

## ADB Entry Point

Use this command after installing the validation APK:

```powershell
.\gradlew.bat :androidApp:assembleValidation
adb install -r androidApp\build\outputs\apk\validation\androidApp-validation.apk
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

The command uses an explicit component and action so no external camera/gallery UI is involved.

The action string remains `com.chromalab.app.RUN_VALIDATION_FIXTURE`; the side-by-side package is selected by the explicit component.

## Phase 8D Fixture Evidence Closure

After Phase 8C, run `white_tiger_ion71_20260520_170118` reached graph selection, graph ROI, axis detection, OCR suggestion, X calibration, and Y calibration. It failed with `TICK_LOCALIZATION_FAILURE` because fewer than two Y tick labels were available for automatic calibration.

Phase 8D requires the next Android fixture run to export graph-stage failure evidence even when no final calibrated graph report can be produced:

- `graph_failure_package_<run_id>.json`;
- graph failure summary in runtime validator JSON/Markdown;
- graphPanel/plotArea bounds or explicit missing reasons;
- axis/tick candidate summaries;
- OCR crop and accepted/rejected tick anchor summaries;
- calibration fit status and warnings.

Android tick candidate rescue is deterministic only. OCR/VLM text may be paired to candidates, but cannot create pixel coordinates or calibration.

## Android Fixture Run - 2026-05-20

Run id: `white_tiger_ion71_20260520_162317`.

Public artifact directory:

`/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_162317/`

Local pulled artifact directory:

`artifacts/phase8b-android-validation/white_tiger_ion71_20260520_162317/`

Result:

- global report gate: `BLOCKED`
- validator verdict: `FAIL`
- runtime failure class: `VLM_MODEL_UNAVAILABLE`
- graph count: `0` detected; analysis stopped before graph detection
- graphPanel status: `MISSING`
- plotArea status: `MISSING`
- X calibration status: `MISSING`
- Y calibration status: `MISSING`
- trace status: `MISSING`
- peak evidence status: `MISSING`
- old axis-detection failure reached: **No**. The run stops earlier at the mandatory VLM readiness guard.
- Assisted Review needed: not applicable until a chromatogram vision model is active; no manual fallback should bypass this required full-analysis model gate.

The user-visible screen shows:

`AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first.`

## Expected Artifacts

Validation artifacts are written to:

`/sdcard/Download/ChromaLab/validation/<run_id>/`

Required slots:

- `runtime_evidence_package_<id>.json`
- `runtime_evidence_validation_<id>.json`
- `runtime_evidence_validation_<id>.md`
- `final_report_contract_<id>.json`
- `report_<id>.html`
- `report_<id>.md`
- `stage_timings_<id>.json`
- `artifact_manifest_<id>.json`
- graphPanel, plotArea, axis/tick, calibration, trace, and peak overlays when available

Missing overlays are recorded in the manifest with explicit reasons instead of being silently omitted.

For the current run, the required text artifacts were exported. Overlay artifacts were correctly marked unavailable because the terminal failure happened before geometry/trace/peak overlay generation.

## Axis / Calibration Failure Interpretation

If the run still fails at axis/tick/calibration, that is a valid validation result. The report must remain `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, or `BLOCKED`; the runtime evidence package and final report contract must carry a structured `runtimeFailureClass`.

Phase 8C removed the pre-geometry VLM blocker. Phase 8D then closed the tick/calibration blocker with final run `white_tiger_ion71_20260520_184550`.

Final Phase 8D result:

- global report gate: `REVIEW_ONLY`;
- validator verdict: `REVIEW`;
- blocking validator issues: `0`;
- runtime failure class: `VLM_SEMANTIC_LAYER_UNAVAILABLE`;
- graph count: `1`;
- X/Y calibration: `VALID` / `VALID`;
- trace and peak evidence: present;
- final report JSON/HTML/Markdown: exported;
- graphPanel, plotArea, axis/tick, trace, and peak overlays: exported.

The current run tests axis/tick/calibration successfully. The remaining non-release status is limited to the unavailable VLM semantic layer and is explicitly visible in runtime evidence.
