# Phase 8B Real Android Validation

Verdict: implementation ready; device install blocked by package signature mismatch.

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

Use this command after installing the debug APK:

```powershell
adb shell am start -S -n com.chromalab.app/.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

The command uses an explicit component and action so no external camera/gallery UI is involved.

Current device note: device `a36d1946` was attached, but `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Prepare the device with matching signing or explicitly approve uninstall/reinstall before running the command above.

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

## Axis / Calibration Failure Interpretation

If the run still fails at axis/tick/calibration, that is a valid validation result. The report must remain `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, or `BLOCKED`; the runtime evidence package and final report contract must carry a structured `runtimeFailureClass`.
