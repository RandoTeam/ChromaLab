# Phase 8C Android Fixture Result

## Status

Phase 8C rerun completed on Android validation package.

## Command

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

## Expected Acceptance

- Runtime evidence package is exported.
- Validator JSON/Markdown is exported.
- `modelAvailabilityDiagnostics` is present.
- If VLM is still unavailable, deterministic geometry stages are attempted before terminal failure.
- If geometry fails, failure class is geometry/calibration-specific instead of primary `VLM_MODEL_UNAVAILABLE`.

## Rerun Record

| Field | Value |
| --- | --- |
| Run id | `white_tiger_ion71_20260520_170118` |
| Device artifact directory | `/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_170118/` |
| Local pulled copy | `artifacts/phase8c-android-validation/white_tiger_ion71_20260520_170118/` |
| Report gate | `BLOCKED` |
| Validator verdict | `FAIL` |
| Runtime failure class | `TICK_LOCALIZATION_FAILURE` |
| Graph count in terminal report | `0` |
| Model availability | `NOT_CONFIGURED`, load attempted, no selected/executed model |
| Old pre-geometry VLM blocker | Fixed; deterministic stages ran after model unavailability |
| Remaining failure | Y calibration failed because fewer than two Y tick labels were available |

## Deterministic Stage Timings

The rerun recorded these stages before terminal failure:

- `IMAGE_QUALITY`
- `CROP_REVIEW`
- `PERSPECTIVE`
- `GRAPH_SELECTION`
- `GRAPH_ROI`
- `AXIS_DETECTION`
- `OCR_SUGGESTION`
- `X_CALIBRATION`
- `Y_CALIBRATION`

This proves the fixture no longer stops at `IMAGE_QUALITY` solely because the VLM is unavailable.

## Remaining Limitation

The terminal failure exporter still emits an empty `graphs` list for calibration-stage failures, so the validator correctly reports `package.graphs_missing`. The primary runtime failure is no longer `VLM_MODEL_UNAVAILABLE`; it is the deterministic tick/calibration failure now reached by the pipeline.
