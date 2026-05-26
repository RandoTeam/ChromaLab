# Phase 9H White Tiger Diff Audit

## Scope

Compared the working Phase 8D White Tiger Android artifact against the regressed Phase 9G final artifact.

- Working: `artifacts/phase8d-android-validation/white_tiger_ion71_20260520_184550/`
- Regressed: `artifacts/phase9g-multi-fixture-android-final2/white_tiger_ion71/`

## Working Phase 8D Result

- Report gate: `REVIEW_ONLY`
- Validator: `REVIEW`
- Graph count: 1
- Runtime failure class: `VLM_SEMANTIC_LAYER_UNAVAILABLE`
- Gate evidence: graph panel, plot area, axis, tick, X calibration, Y calibration, trace, and peak review all `VALID`.
- Selected graph panel: `x=0, y=418, width=576, height=425`
- Selected plot area: `x=0, y=418, width=576, height=375`
- X calibration: `VALID`, 6 accepted anchors, RMSE 1.97 px, max residual 2.76 px.
- Y calibration: `VALID`, 5 accepted anchors, RMSE 0.29 px, max residual 0.40 px.

## Regressed Phase 9G Result

- Report gate: `BLOCKED`
- Validator: `REVIEW`
- Graph count in report: 0; graph failure packages: 1
- Runtime failure class: `TICK_LOCALIZATION_FAILURE`
- Failure stage: `X_CALIBRATION`
- Selected graph failure panel: `x=0, y=0, width=576, height=527`
- Selected plot area: `x=0, y=0, width=576, height=495`
- Tick candidates: 2 X, 2 Y.
- Accepted OCR anchor before downstream filtering: one X value from raw text `70 to 71.70): BE`.
- Calibration strategy count: 6.
- Selected X strategy: `AXIS_SCALE_RESOLVER`, status `INVALID`.
- Selected Y strategy: `LEGACY_TICK_LOCALIZATION`, status `INVALID`.

## Exact Regression Cause

Legacy calibration was not absent and was not discarded in favor of a valid AxisScaleResolver result. The regression was caused earlier in the geometry path:

1. Graph multiplicity collapsed overlapping single-physical-graph candidates before final geometry evaluation.
2. The candidate that survived into evaluation was a broad upper/full chart/page candidate with weak tick crop evidence.
3. The older lower chromatogram panel candidate, which produced enough OCR tick crops and valid X/Y calibration in Phase 8D, was no longer evaluated.
4. Calibration arbitration then correctly found no usable strategy because all available candidate evidence was starved.

Secondary finding:

- `bench_03_small_tic_export` exposed an Android bitmap lifecycle bug after retry candidate evaluation was restored. `AxisDetector.android.kt` recycled the decoded source bitmap before reading pixels when `Bitmap.createBitmap(...)` returned the same bitmap for a whole-image crop.

## Boundary Decisions

- No White Tiger coordinates were hardcoded.
- No fake anchors or tick labels were added.
- VLM/E2B remains advisory and did not provide pixel geometry or numeric calibration.
- CalculationEngine was not modified.
