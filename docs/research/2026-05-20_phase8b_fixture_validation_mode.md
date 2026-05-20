# Phase 8B Fixture Validation Mode Research

Date: 2026-05-20

## Sources

| Source | Type | Notes | Decision |
| --- | --- | --- | --- |
| Android `Intent` API reference: https://developer.android.com/reference/android/content/Intent | Official docs | Describes activities started by `Intent`, actions, explicit components, and extras. | Use an explicit debug activity launch action with a `fixture` extra so adb does not depend on camera/gallery/photo picker UI. |
| Android intents guide: https://developer.android.com/guide/components/intents-filters | Official docs | Explicit intents name the app component and avoid external resolution ambiguity. | Prefer `adb shell am start -n com.chromalab.app/.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71`. |
| Android `AssetManager` API reference: https://developer.android.com/reference/android/content/res/AssetManager | Official docs | `AssetManager.open(String)` reads packaged app assets. | Store the known chromatogram fixture under `androidMain/assets/validation/` and copy it to the same app-private capture workspace before analysis. |
| Android `MediaStore` API reference: https://developer.android.com/reference/android/provider/MediaStore | Official docs | `MediaStore.Downloads` and `MediaColumns.RELATIVE_PATH` support user-visible Downloads output on Android Q+. | Export validation artifacts to `Download/ChromaLab/validation/<run_id>/` through MediaStore on modern Android. |
| Android adb docs: https://developer.android.com/tools/adb | Official docs | `adb shell am start` can start an activity with an intent action. | Document a direct adb command for deterministic fixture validation. |

## Applied Decisions

- Only image acquisition is bypassed. The runner copies the bundled asset into `filesDir/captures/validation/<run_id>/` and navigates into the existing processing flow.
- The input source is marked as `VALIDATION_FIXTURE` in acquisition, geometry provenance, stored report metadata, and report contract metadata.
- Validation artifacts are exported automatically after the structured report is built, including runtime evidence JSON, validator JSON/Markdown, final report contract JSON, report HTML/Markdown, stage timings, overlay artifacts where available, and a manifest.
- The debug entrypoint is limited to debuggable builds and remains labeled as a developer validation action.
