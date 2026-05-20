# Phase 8 Real Android Validation

## Status

Verdict: `SUPERSEDED_BY_PHASE_8B_FIXTURE_VALIDATION`

`adb devices` was executed during Phase 8 setup. It returned no connected device or emulator:

```text
List of devices attached
```

Because no real Android target was available during the original Phase 8 slice, Phase 8 could not be marked fully closed for production at that time. Phase 8B later connected device `a36d1946`, resolved the install/signature conflict with a side-by-side validation package, and ran the fixture path. The current blocker is `VLM_MODEL_UNAVAILABLE`, not camera/gallery navigation.

## Required Android Dataset

Run at minimum:

1. original user graph / Ion 71 / White Tiger class: `bench_08_mz71_duplicate_candidate`;
2. clean screenshot: `bench_02_mz92_belyi_tigr`;
3. known ROI/multi-graph failure class: `bench_01_mz71_screenshot_page` or `bench_06_photo_two_graphs_page`.

## Required Export Per Run

Each Android run must export:

- RuntimeEvidencePackage JSON;
- validator JSON;
- validator Markdown;
- final report JSON;
- final screen screenshot;
- relevant logcat excerpt;
- generated HTML/Markdown report export if available;
- graphPanel/plotArea overlays;
- axis/tick/calibration evidence;
- trace overlay;
- peak overlay/evidence table;
- stage timings and model/runtime profile.

## Command Checklist

Phase 8B adds a fixture-driven debug entrypoint for the White Tiger Ion 71 case. This is the preferred command because it bypasses camera/gallery/photo picker ambiguity while preserving the real processing path after acquisition:

```powershell
adb devices
.\gradlew.bat :androidApp:assembleValidation
adb install -r androidApp\build\outputs\apk\validation\androidApp-validation.apk
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
adb logcat -d -v time > artifacts\phase8b-android-validation\white_tiger_ion71\logcat.txt
adb shell ls -R /sdcard/Download/ChromaLab/validation
```

The validation run writes artifacts to `/sdcard/Download/ChromaLab/validation/<run_id>/`. Pull or inspect that directory after the app reaches the report screen. Use the `validation` package so existing `com.chromalab.app` installs and data are not replaced during signature-mismatch recovery.

## Acceptance

Real Android validation passes only if:

- all required artifacts are present;
- validator JSON/Markdown agree with report gate status;
- no `RELEASE_READY` claim is made with missing calibration, trace, peak, or evidence package;
- no VLM/Knowledge numeric metric is accepted;
- user report excludes raw logs, prompts, and `NEVER_SHARED_BY_DEFAULT` artifacts;
- failures are classified using `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md`.
