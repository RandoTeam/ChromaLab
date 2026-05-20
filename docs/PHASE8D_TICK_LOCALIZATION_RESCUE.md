# Phase 8D Tick Localization Rescue

## Purpose

The White Tiger Ion 71 fixture can fail at Y tick localization/calibration. Phase 8D adds a deterministic Android rescue path without hardcoded coordinates and without using VLM/OCR as numeric geometry authority.

## Added Android CV Rescue

`AxisTickGeometryDetector.android.kt` now merges existing short tick-mark projection candidates with label-band projection candidates:

- X rescue scans the deterministic band below the plot area and finds repeated dark text-column clusters.
- Y rescue scans the deterministic band left of the plot area and finds repeated dark text-row clusters.
- Candidate rows/columns are clustered and merged with tick-mark candidates.
- Warnings record `axis_tick_geometry.x_label_projection_rescue` or `axis_tick_geometry.y_label_projection_rescue` when rescue contributes candidates.

## Boundary

The rescue only creates candidate pixel positions from image raster geometry. OCR/ML Kit/VLM may read crop text later, but text-only values are rejected unless linked to deterministic tick pixels.

## Calibration

Existing `AxisCalibrationFitter` remains the fitter. Phase 8D does not change chromatographic math or `CalculationEngine`.

## Android Verification

The final fixture run `white_tiger_ion71_20260520_184550` produced `VALID` X and Y calibration statuses from deterministic tick geometry plus OCR-paired values. The report gate remains `REVIEW_ONLY` only because the validation build has no active VLM semantic layer; tick localization and calibration are no longer the blocking stage.
