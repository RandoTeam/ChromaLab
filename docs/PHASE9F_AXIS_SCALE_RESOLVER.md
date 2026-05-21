# Phase 9F Axis Scale Resolver

## Purpose

`AxisScaleResolver` replaces tick-mark-only calibration assembly with a deterministic multi-evidence resolver. It does not change chromatographic math or `CalculationEngine`; it only decides which calibration anchors are sufficiently evidenced before the existing calibration fitter is used.

## Evidence Model

Supported evidence types:

- `EXPLICIT_TICK_MARK`
- `GRID_LINE`
- `OCR_LABEL_BOX`
- `LABEL_PROJECTION`
- `FRAME_ENDPOINT`
- `REGULAR_SEQUENCE`
- `AXIS_ENDPOINT`
- `PLOT_FRAME_EDGE`
- `OCR_VALUE_ONLY_REJECTED`
- `SEMANTIC_TEXT_REJECTED`

Each accepted or rejected anchor records axis, pixel coordinate, numeric value when known, evidence source, confidence, OCR crop/projection context when available, and rejection reason when rejected.

## Resolver Algorithm

1. Gather deterministic tick candidates from the existing tick OCR/calibration path.
2. Gather OCR numeric label boxes from local axis-label crops.
3. Reject semantic-only model text, title text, ion/m/z/SIM/channel text, and mass-range strings such as `70.70 to 71.70`.
4. Build anchor candidates from explicit deterministic tick+OCR anchors.
5. Build OCR-label-box anchors when no explicit tick can be linked.
6. Build label-projection anchors when a numeric OCR box is close to a deterministic tick position.
7. Fit candidate X/Y scales with the existing `AxisCalibrationFitter`.
8. Downgrade label-box-only fits to `REVIEW` unless stronger deterministic geometry supports them.
9. Export subreasons for invalid fits instead of generic tick failure.

## Safety Rules

- VLM text may supplement OCR text, but VLM-derived positions are rejected as geometry.
- OCR numeric text without acceptable geometry remains rejected evidence.
- The resolver cannot fabricate labels, ticks, or calibration values.
- Release-ready reports still require valid calibration evidence and validator approval.

## Files

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/AxisScaleResolver.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/GeometryContracts.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/GeometryPipelineRunner.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/debug/RuntimeEvidencePackageValidator.kt`
- `composeApp/src/commonTest/kotlin/com/chromalab/feature/processing/geometry/AxisScaleResolverTest.kt`

## Current Result

The resolver improved `bench_04`, `bench_06`, and preserved `bench_07` as reviewable Android reports. It did not close Phase 9 because `white_tiger_ion71`, `bench_01`, `bench_02`, and `bench_05` still lack sufficient scale evidence.
