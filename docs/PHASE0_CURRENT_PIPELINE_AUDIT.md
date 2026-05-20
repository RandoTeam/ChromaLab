# Phase 0 Current Pipeline Audit

Phase 0 audit scope: image input to final report/evidence contract. This document describes the current system risks and gate requirements; it does not implement algorithmic fixes.

## Current Runtime Path

1. Image input enters processing from camera, gallery, import, or scan UI.
2. `ProcessingFlowScreen` prepares model/runtime state, tracks stage durations, and dispatches the processing flow.
3. Image normalization and orientation correction produce the working image.
4. Crop and perspective stages currently preserve identity geometry when no reliable quad exists; this must be reported as skipped/not confident, not hidden as successful rectification.
5. `AutoSweepEngine` runs preprocessing variants and invokes `GeometryPipelineRunner`.
6. `GeometryPipelineRunner` attempts graphPanel, plotArea, axis, tick, OCR, calibration, multiplicity, and trace evidence.
7. OCR/VLM stages supply title/ion/axis text, local crops, and optional hints. VLM remains advisory for geometry and text semantics.
8. Curve mask/extraction produces signal points and overlays when available.
9. The existing deterministic `CalculationEngine` receives signal/calibration data; Phase 0 does not rewrite it.
10. `ProcessingReportMetadataBuilder` and report mappers store metadata into `ChromatogramReport`.
11. `ReportReleaseGateEvaluator`, `ReportContractValidator`, `RuntimeEvidencePackage`, and `RuntimeEvidencePackageValidator` evaluate release readiness after report construction.

## Key Files Inspected

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/flow/ProcessingFlowScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/sweep/AutoSweepEngine.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/GeometryPipelineRunner.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/ScreenshotEmbeddedChartDetector.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/geometry/GraphMultiplicityResolver.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/calibration/AxisCalibrationFitter.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/curve/CurveMaskPreparer.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/curve/CurveExtractor.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/debug/RuntimeEvidencePackage.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/debug/RuntimeEvidencePackageValidator.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/Phase0ProductContracts.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/ReportModels.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/ChromatogramReportUiContract.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/ReportContractValidator.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/ReportHtmlRenderer.kt`

## Where AUTO Can Still Overclaim

| Area | Risk | Phase 0 gate response |
| --- | --- | --- |
| Report surface | A polished report can make diagnostic output look final. | Report gate status must be visible and exported. |
| Peak table | Downstream peak rows can exist even when upstream geometry is weak. | `RELEASE_READY` requires graphPanel, plotArea, X/Y calibration, trace, evidence package, and source provenance gates. |
| Legacy metadata | Older flows may carry `FULL_ANALYSIS`. | Phase 0 treats release readiness as gate-based, not label-based. |
| VLM hints | VLM graph/text output can look authoritative. | VLM cannot populate numeric geometry or chromatographic metrics. |
| Sparse trace | A calculable signal can still be scientifically weak. | Trace gate must be valid or user-confirmed for release. |
| Missing evidence | A run can fail before all overlays are saved. | Terminal states require evidence packages; missing evidence blocks release. |

Additional current-code risks found during Phase 0 audit:

- `ProcessingFlowScreen` still auto-advances major stages and can save once calibration/signal exist; Phase 0 gates are report/evidence gates, not a full runtime rewrite.
- Legacy automatic calibration helpers may still build X/Y calibration from OCR/endpoints when robust geometry fits are missing; release gates must catch missing/invalid X/Y fit evidence.
- Runtime evidence packages allow nullable overlay paths; the validator catches missing artifacts, but future runtime work must guarantee export attempts for every terminal path.
- Evidence export is strongest after a report exists; non-ROI fatal processing failures still need future runtime enforcement.
- VLM `numGraphs`-style splitting remains a historical risk; multiplicity resolver mitigates it, but the regression matrix keeps one-physical-graph/many-candidates as a required row.

## Evidence Gaps To Preserve As Open Risks

- Real Android proof is still required for every terminal-state export path.
- Some diagnostic runs have previously missed graphPanel, plotArea, axis, tick, or trace overlays.
- Axis/tick gates are recorded, but Phase 0 release-required gates are currently graphPanel, plotArea, X/Y calibration, trace, evidence package, and source provenance. Future phases should decide whether axis/tick status should directly block release in addition to calibration.
- `AUTO_DIAGNOSTIC` still depends on upstream geometry and trace extraction quality that Phase 0 does not fix.
- ROI failure has a dedicated package path, but `CALIBRATION_FAILURE`, `CURVE_FAILURE`, `OCR_FAILURE`, `VLM_TIMEOUT`, and `FATAL_PIPELINE_ERROR` still need real Android proof that evidence is exported before user-visible termination.

## VLM Risks

- VLM graph-region hints can be broad, partial, or wrong.
- VLM title/ion numbers can resemble peak labels, especially ranges such as `70.70 to 71.70`.
- VLM timeout or model failure must not block evidence export.
- VLM must not populate RT, height, area, FWHM, S/N, baseline, Kovats, exact pixel geometry, final peak count, or chromatographic metrics.

## Known Real Android Failure Classes

| Failure class | Previous symptom | Required Phase 0 behavior |
| --- | --- | --- |
| ROI failure | `AI vision analysis did not produce a usable graph and axis result.` | Export ROI failure evidence and block release. |
| Six pseudo-graphs | One physical graph emitted as six reports. | Multiplicity evidence; duplicates/nested panels collapse or report diagnostic. |
| Right-side graphPanel crop | Selected crop omitted left axis/ticks and full panel. | Missing graphPanel/plotArea/calibration gates block release. |
| Invalid calibration | X/Y fits missing or invalid. | `CALIBRATION_FAILURE` or `DIAGNOSTIC_ONLY`; no release peak table. |
| Sparse trace | Low column coverage accepted as review. | `REVIEW_ONLY` or diagnostic unless trace is valid/confirmed. |
| Missing overlays | Validator failed due null artifact paths. | Missing evidence is a blocking issue for release readiness. |

## Phase 0 Contract Already Present In Code

Observed contracts:

- `ProcessingMode.AUTO_DIAGNOSTIC`, `GUIDED_PRODUCTION`, `MANUAL_ADVANCED`.
- `ReportGateStatus.RELEASE_READY`, `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, `BLOCKED`.
- `EvidenceGateStatus`.
- `RuntimeTerminalState`.
- `VlmEvidenceTaskType`.
- `NumericChromatographicMetric`.
- `ReportReleaseGateEvaluator`.
- `VlmBoundaryPolicy`.
- `RuntimeEvidencePackage` and ROI failure package models.

## Not Changed By Phase 0

- `CalculationEngine`.
- Geometry algorithms.
- OCR algorithms.
- VLM runtime behavior.
- Curve extraction.
- Peak detection/integration math.
- Guided/manual UI.
- Image-specific ROI fixes.

## Audit Decision

The current pipeline remains usable only under `AUTO_DIAGNOSTIC` unless release gates pass. Phase 0 can close as a product-mode and evidence-gate reset, not as a claim that automatic chromatogram analysis is production-ready.
