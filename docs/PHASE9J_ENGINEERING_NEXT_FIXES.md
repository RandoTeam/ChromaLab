# Phase 9J Engineering Next Fixes

No implementation was performed in Phase 9J. This list records the next engineering work required by the evidence audit.

| Priority | Fixture/class | Blocker | Evidence | Required fix |
| ---: | --- | --- | --- | --- |
| 1 | `bench_01_mz71_screenshot_page` / page screenshot with m/z 71 graphs | `TICK_LOCALIZATION_FAILURE` at `Y_CALIBRATION` | Graph failure package shows graphPanel/plotArea present, X valid, Y anchors insufficient | Improve deterministic Y label/tick anchor formation without hardcoding coordinates. |
| 2 | `bench_05_tic_plus_ions` / TIC + ion panels | `CALIBRATION_FAILURE` at `Y_CALIBRATION`; layout collapsed to one failure package | Graph failure package shows direction-inconsistent Y evidence | Repair TIC+ion layout propagation and Y-axis calibration evidence. |
| 3 | All review fixtures | 0 `RELEASE_READY` outputs | Reports remain `REVIEW_ONLY` due evidence caveats | Strengthen graph/calibration/trace/peak evidence until gates can honestly promote. |
| 4 | Visual evidence packaging | Blocked fixtures have graph failure packages but local overlay PNG copies are incomplete | Markdown points to package JSON and missing overlay paths | Ensure graph-stage failure overlays are pulled/exported as local image files for every blocked run. |
