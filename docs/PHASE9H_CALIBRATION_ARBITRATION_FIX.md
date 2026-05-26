# Phase 9H Calibration Arbitration Fix

## Fix Summary

Phase 9H fixed the White Tiger regression by restoring candidate retry evidence, not by weakening calibration gates.

Changes:

- Single-physical-graph multiplicity now keeps rejected overlapping graph panel candidates as retry alternatives for geometry evaluation.
- Primary resolved candidates remain first in retry order so established review-ready fixtures do not get pushed out by alternates.
- Calibration strategy selection/rejection enums now include explicit Phase 9H reason names.
- Android `AxisDetector` now avoids recycling the source bitmap before `getPixels()` when the crop returns the original bitmap.

## Arbitration Behavior

The calibration ensemble still enforces:

- `VALID` beats `REVIEW`.
- `REVIEW` beats `INVALID`.
- Legacy tick localization remains available as a regression shield.
- AxisScaleResolver remains one candidate strategy, not the exclusive calibration path.
- Title/ion/m/z/method strings remain rejected as calibration labels.
- No-pixel-geometry candidates remain invalid.

## Candidate Retry Behavior

For a layout classified as one physical graph, overlapping rejected candidates are retained as retry alternatives. This preserves graph count semantics while allowing the pipeline to select the candidate that actually provides usable axis/tick/calibration evidence.

This is the key White Tiger fix: the lower chromatogram panel can be evaluated again without converting one physical graph into multiple report graphs.

## Android Runtime Fix

`AxisDetector.android.kt` previously recycled `fullBitmap` immediately after creating `cropped`. Android may return the original bitmap when the requested crop is the whole decoded bitmap. In that case, `cropped.getPixels()` throws `Can't call getPixels() on a recycled bitmap`.

The detector now recycles `fullBitmap` immediately only when `cropped != fullBitmap`.
