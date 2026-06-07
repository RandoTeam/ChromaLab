# R14 Runtime Calibration Promotion Candidate Closeout

Date: 2026-06-07

Status: `R14_RUNTIME_CALIBRATION_PROMOTION_CANDIDATE_IMPLEMENTED_NOT_ACCEPTED`

Scope: runtime calibration strategy evidence only. R14 does not change
`CalculationEngine`, chromatographic math, graph detection, trace extraction,
peak metrics, report gates, or E2B authority.

## Why R14 Exists

R13 made Android/runtime OCR-anchor rows visible in RuntimeEvidencePackage.
Those rows still needed a controlled path into calibration arbitration so
runtime evidence can show whether safe Android anchors are usable, rejected, or
insufficient. R14 adds that path as a named calibration strategy without making
it the only strategy.

## Implementation

R14 adds:

- `CalibrationStrategyId.ANDROID_RUNTIME_OCR_ANCHOR`;
- `RuntimeOcrAnchorCoordinateFrame`;
- `RuntimeOcrAnchorBridgeRow.coordinateFrame`;
- Android-runtime-anchor strategy support inside `CalibrationStrategyEnsemble`;
- graph-package calibration summary export;
- validator summary rendering for selected X/Y strategy and strategy count.

The new strategy consumes only accepted runtime OCR-anchor rows. It rejects:

- missing numeric values;
- missing deterministic pixel coordinates;
- forbidden title, ion, m/z, SIM/channel, scan, and method text;
- VLM/E2B numeric source rows;
- rejected or semantic-only geometry sources;
- rows without coordinate frame;
- rows outside the selected plot frame after coordinate conversion.

Rows marked `PLOT_RELATIVE` are consumed directly. Rows marked
`IMAGE_ABSOLUTE` are converted to plot-relative coordinates using the selected
plotArea.

## Arbitration Behavior

The strategy is added to the existing calibration ensemble. It does not replace
legacy tick localization, AxisScaleResolver, OCR label-box direct fit, regular
sequence fit, grid/frame projection, or frame-endpoint fallback.

Selection remains deterministic:

- `VALID` beats `REVIEW`;
- `REVIEW` beats `INVALID`;
- legacy White Tiger fallback remains available;
- an invalid runtime-anchor strategy cannot override a usable legacy strategy;
- runtime-anchor fits are downgraded to `REVIEW` unless anchor count,
  monotonicity, residuals, and geometry support are strong.

## Validation Coverage

R14 adds or updates tests proving:

- safe runtime OCR-anchor rows can produce a named calibration strategy result;
- image-absolute rows convert to plot-relative calibration anchors;
- forbidden text, VLM numeric source, no-pixel rows, rejected geometry sources,
  and missing coordinate frames are rejected before fitting;
- legacy fallback wins when runtime-anchor evidence is invalid;
- RuntimeEvidencePackage exposes selected/rejected calibration strategy
  evidence.

## Product Meaning

R14 is a runtime calibration promotion candidate, not a Phase 9 acceptance. It
allows Android runtime anchor rows to participate in calibration arbitration
under strict safety checks, but Android fixture reruns still need to prove that
`bench_01` and `bench_05` improve without regressing White Tiger, `bench_03`,
`bench_07`, E2B behavior, graph counts, metrics, or report gates.

## Next Phase

```text
R15 - Graph Layout And Multi-Panel Runtime Closure
```

R15 should re-audit `bench_04`, `bench_05`, and `bench_06`, confirm graph-count
semantics with Product/QA/Scientific roles, and ensure layout classification
propagates into report graph sections without fixture-specific coordinates.
