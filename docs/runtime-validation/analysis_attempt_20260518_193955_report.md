# Android graph analysis attempt - ROI failure

## Summary

- Device: `a36d1946`
- Device model: `OnePlus PJZ110`
- Android: `16` / SDK `36`
- App package: `com.chromalab.app`
- App version: `0.0.5-beta` (`versionCode=5`)
- App PID during capture: `3266`
- Capture date: `2026-05-18`
- Final observed state: controlled pipeline error, not an app crash.

## Observed final error

The UI dump and screenshot both show the same blocking error:

```text
Ошибка: ROI графика
ROI графика: AI vision analysis did not produce a usable graph and axis result.
Этот этап обязателен для полного анализа и не может быть пропущен.
```

The app offered two actions:

- `Отмена`
- `Повторить`

## Timing

The exact pipeline start/end timestamps were not emitted by the app logs for this run. The available evidence gives a bounded duration:

- `20:24:40.738` - ML Kit document scanner flow starts from ChromaLab.
- `20:24:46.968` - `com.chromalab.app/.MainActivity` is active again after scanner/photo selection flow.
- `20:39:55` - ChromaLab app PID log, screenshot, and UI dump were captured with the ROI error visible.

Measured from return to `MainActivity` after scanner/photo selection to the captured error screen:

```text
20:24:46.968 -> 20:39:55 ~= 15 min 08 sec upper bound
```

This is not a precise processing duration. It is the maximum elapsed time visible from the captured evidence. The error may have appeared earlier.

## Log findings

- No `FATAL EXCEPTION`, `AndroidRuntime`, or ChromaLab process crash was found in the app PID log.
- The app PID log does not contain `PIPELINE[...]`, `RuntimeEvidence`, or detailed geometry-stage timestamps for this run.
- The broad filtered log shows the external ML Kit document scanner and Android photo picker activity around `20:24:40` - `20:24:47`.
- The captured UI state confirms the failure happened at the mandatory graph ROI / graph-axis result stage.

## Artifacts

- `analysis_attempt_20260518_193915_filtered_logcat.txt` - broad filtered logcat around the run.
- `analysis_attempt_20260518_193955_app_pid_3266_logcat.txt` - PID-filtered ChromaLab process logcat.
- `analysis_attempt_20260518_193955_window.xml` - UIAutomator dump with the ROI error text.
- `analysis_attempt_20260518_193955_screen_pull.png` - device screenshot showing the ROI error dialog.

## Technical classification

- Failure class: upstream geometry / AI vision ROI failure.
- Failed stage: graph ROI and axis detection result from AI vision analysis.
- Downstream status: calculation/report pipeline did not complete.
- Runtime evidence package: not produced for this run because the pipeline stopped at the mandatory ROI stage.

## Trace gap

This run cannot provide exact analysis duration or per-stage timing because the Android runtime did not emit structured pipeline timing logs for the failed ROI path. The next diagnostic run should clear logcat before analysis and should record explicit timestamps for:

1. image received,
2. normalization started/finished,
3. graph panel ROI started/finished,
4. plot area detection started/finished,
5. AI vision ROI result received,
6. blocking error emitted.
