# Phase 8 Real Android Validation

## Status

Verdict: `ANDROID_VALIDATION_DEFERRED_NO_DEVICE`

`adb devices` was executed during Phase 8 setup. It returned no connected device or emulator:

```text
List of devices attached
```

Because no real Android target was available, Phase 8 cannot be marked fully closed for production. Desktop regression and contract validation can be strengthened, but real Android evidence remains a blocker for starting Phase 9 unless Product Acceptance explicitly accepts the deferral.

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
.\gradlew.bat :androidApp:assembleDebug
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
adb shell am start -S -n com.chromalab.app/.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
adb logcat -d -v time > artifacts\phase8b-android-validation\white_tiger_ion71\logcat.txt
adb shell ls -R /sdcard/Download/ChromaLab/validation
```

The validation run writes artifacts to `/sdcard/Download/ChromaLab/validation/<run_id>/`. Pull or inspect that directory after the app reaches the report screen. A future instrumentation runner may wrap the same debug action, but the action above is the current deterministic runner.

## Acceptance

Real Android validation passes only if:

- all required artifacts are present;
- validator JSON/Markdown agree with report gate status;
- no `RELEASE_READY` claim is made with missing calibration, trace, peak, or evidence package;
- no VLM/Knowledge numeric metric is accepted;
- user report excludes raw logs, prompts, and `NEVER_SHARED_BY_DEFAULT` artifacts;
- failures are classified using `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md`.
