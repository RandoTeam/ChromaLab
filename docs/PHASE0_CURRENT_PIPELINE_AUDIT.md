# Phase 0 Current Pipeline Audit

## Current Runtime Path

1. Image input enters processing from camera/gallery/import UI.
2. `ProcessingFlowScreen` prepares model/runtime state and dispatches processing.
3. Image normalization/preprocessing creates processed image variants and provenance fragments.
4. `GeometryPipelineRunner` attempts graphPanel/plotArea/axis/tick/calibration evidence.
5. `AutoSweepEngine` evaluates candidate graph outputs and can auto-advance stages.
6. OCR/VLM stages supply title/ion/axis text, local crops, and optional VLM hints.
7. Curve mask/extraction produces signal points and overlays when available.
8. Calculation data reaches the existing deterministic `CalculationEngine`.
9. `ProcessingReportMetadataBuilder` stores metadata in report config.
10. `CalculationRunReportMapper` maps calculation output into `ChromatogramReport`.
11. `ReportContractValidator`, UI contract, export, RuntimeEvidencePackage, and validator operate after the report exists.

## Where AUTO Claimed Too Much

- Processing metadata previously used `FULL_ANALYSIS` for automatic photo processing.
- Auto reports could carry peak tables even when geometry was only review/diagnostic.
- `REVIEW_READY` was a warning, not a product-mode distinction.
- A runtime evidence package could validate as structurally present without a product mode or release gate status.

## Evidence Gaps

- Runtime evidence package did not expose terminal state.
- Runtime evidence package did not expose report gate status or gate evidence.
- ROI failure had a separate schema, but non-ROI terminal failures were not first-class.
- Some diagnostic runs may still miss overlays; validator can detect missing paths, but terminal-state reason needs to remain explicit.

## Unsafe Release-Quality Risks

- Missing graphPanel/plotArea evidence can be hidden by a polished report surface.
- Invalid or missing X/Y calibration can still leave downstream numeric-looking data in the report.
- Sparse trace extraction can produce a signal that looks calculable but is not release-quality.
- Model/VLM output can still appear early in graph or text paths; Phase 0 gates prevent numeric trust but do not rewrite those stages.

## VLM Risks

- VLM graph-region hints can be broad or partial and must remain advisory.
- VLM title/ion numbers can resemble peak labels, especially ranges such as `70.70 to 71.70`.
- VLM timeout or model failure must not block evidence export.
- VLM must not populate numeric chromatographic metrics.

## Known Real Android Failure Classes

| Failure | Previous symptom | Phase 0 handling |
| --- | --- | --- |
| ROI failure | `AI vision analysis did not produce a usable graph and axis result.` | Terminal state must export evidence and cannot be release-ready. |
| Six pseudo-graphs | One physical graph emitted as six reports. | Regression matrix requires multiplicity evidence. |
| Right-side crop | GraphPanel selected only right subregion. | Release gate requires graphPanel/plotArea/calibration/trace evidence. |
| Invalid calibration | X/Y fits missing or invalid. | Release gate blocks release-quality. |
| Sparse trace | Low column coverage accepted as review. | Release gate remains review/diagnostic unless trace valid/confirmed. |
| Missing overlays | Validator FAIL due missing artifact paths. | Missing evidence is a blocking issue for release readiness. |

## Phase 0 Code Contract Added

- `ProcessingMode.AUTO_DIAGNOSTIC`, `GUIDED_PRODUCTION`, `MANUAL_ADVANCED`.
- `ReportGateStatus`, `EvidenceGateStatus`, `RuntimeTerminalState`.
- `ReportReleaseGateEvaluator`.
- `VlmBoundaryPolicy`.

## Not Changed

- `CalculationEngine`.
- Geometry algorithms.
- OCR algorithms.
- Curve/peak math.
- Guided/manual UI.
