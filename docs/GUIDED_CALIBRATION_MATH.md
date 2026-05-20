# Guided Calibration Math

Phase 3 uses deterministic linear calibration from user-provided anchors. It reuses the existing `AxisCalibrationFitter`; no new chromatographic math or `CalculationEngine` behavior is introduced.

## Anchor Model

Each `ManualCalibrationAnchor` stores:

- axis: `X` or `Y`;
- pixel position in normalized image coordinates;
- known numeric value;
- unit label;
- source;
- status;
- timestamp;
- user/session provenance;
- related image id;
- warning and overlay artifact fields.

For fitting:

- X anchors use `pixel.x` as the axis pixel position.
- Y anchors use `pixel.y` as the axis pixel position.
- values are the user-entered calibration values.

## Fit Model

The fitter estimates a linear transform:

```text
unit = slope * pixel + intercept
```

The fit returns:

- slope;
- intercept;
- accepted anchors;
- rejected anchors;
- residuals in pixels;
- residuals in units;
- max residual px;
- RMSE px;
- R2;
- confidence;
- `VALID`, `REVIEW`, or `INVALID`.

## Two Anchors

Two anchors are enough to define a linear transform, but they cannot prove residual quality. Phase 3 therefore treats two-anchor calibration as review-grade even if the line is geometrically clean.

This behavior is enforced by:

- fitter warnings (`calibration.x.two_anchor_review`, `calibration.y.two_anchor_review`);
- guided gate mapping that requires three accepted anchors per axis for robust-fit readiness;
- `GuidedReportGateMapper`, which maps non-robust calibration to `REVIEW`.

## Three or More Anchors

Three or more anchors can become `VALID` when:

- all anchor values are finite;
- pixel/value order is monotonic;
- residuals are within `AxisCalibrationFitter` thresholds;
- the graphPanel and plotArea confirmations exist.

High residuals or outliers keep the gate review-grade or invalid depending on fitter output and reducer validation.

## Invalid Conditions

Calibration is invalid when:

- plotArea is missing;
- graphPanel is missing;
- fewer than two X anchors are available;
- fewer than two Y anchors are available;
- any value is non-finite;
- duplicate pixel positions conflict with different values;
- values are non-monotonic;
- the fit is degenerate or invalid.

## Direction Review

Chromatograms normally increase left-to-right on X and usually map higher signal upward even though pixel Y increases downward. Phase 3 does not silently reject every reversed direction, because scanned/cropped images and domain conventions can vary. Suspicious directions become review-grade warnings.

## Release Implication

Guided calibration can satisfy X/Y release gates only in guided/manual modes. `AUTO_DIAGNOSTIC` does not use user confirmations as release evidence.
