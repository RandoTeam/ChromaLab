# Guided Production Contracts

Phase 1 defines the data contracts that future guided and manual workflows will write. These contracts are designed to preserve scientific provenance and prevent `AUTO_DIAGNOSTIC` output from masquerading as confirmed production analysis.

## Phase 4 Autonomous-First Realignment

`GUIDED_PRODUCTION` is now a deprecated compatibility name. New architecture uses:

- `AUTONOMOUS_PRODUCTION` as the primary target path;
- `AUTO_DIAGNOSTIC` for incomplete automatic runs;
- `ASSISTED_REVIEW` for the Phase 2 ROI, Phase 3 calibration, and Phase 4 trace review tools;
- `MANUAL_ADVANCED` for expert fallback.

The contracts in this document remain valid, but they should be interpreted as assisted/manual evidence contracts. They are not the default happy path for normal images.

## Contract Groups

| Group | Main types | Purpose |
| --- | --- | --- |
| Mode/state | `GuidedDigitizationState`, `GuidedDigitizationMode`, `GuidedWorkflowStep`, `GuidedStepStatus` | Persist workflow state and mode. |
| User provenance | `GuidedUserProvenance`, `GuidedWorkflowAuditEntry` | Record who/what/session changed evidence without storing raw personal identifiers. |
| Geometry | `UserConfirmedGraphPanel`, `UserConfirmedPlotArea`, `GraphPanelConfirmation`, `PlotAreaConfirmation` | Capture graphPanel and plotArea confirmation. |
| Calibration | `ManualCalibrationAnchor`, `UserCalibrationSet`, `CalibrationResidualReport`, `UserConfirmedCalibration` | Capture X/Y anchors, residual evidence, monotonicity, and gate status. |
| Trace | `UserConfirmedTrace`, `TraceConfirmationEvidence`, `TraceQualitySummary` | Capture trace confirmation and quality evidence. |
| Peaks | `UserPeakEditDecision`, `UserConfirmedPeakSet` | Capture future peak review/edit decisions. |
| Gate mapping | `GuidedReportGateMapper`, `GuidedWorkflowGateEvaluation` | Convert guided state into Phase 0 report gates. |

## Geometry Rules

- `graphPanel` includes title, ion/channel, axis labels, tick labels, and plot frame.
- `plotArea` includes only the coordinate rectangle with trace/grid/frame.
- A plotArea confirmation must be linked to the parent graphPanel bounds.
- A confirmation without user provenance or image linkage is not production evidence.

## Calibration Rules

- X and Y calibration each require at least two accepted anchors.
- Three or more accepted anchors per axis are required for robust-fit readiness.
- Accepted and rejected anchors are preserved.
- Residual evidence, RMSE, R2, and monotonicity are stored when available.
- Two-anchor calibration maps to review-grade by default in Phase 1 gate mapping.

## Trace Rules

- Trace confirmation can accept, reject, or mark auto trace evidence for review.
- Overlay and centerline artifact paths are part of the confirmation evidence.
- Sparse/fragmented trace evidence must not silently become release-ready without user confirmation and gate mapping.

## Peak Review Rules

Future peak review supports:

- adding peaks;
- removing peaks;
- merging/splitting peaks;
- adjusting boundaries;
- marking shoulders;
- marking review-grade peaks;
- accepting auto peaks.

Phase 1 stores decisions only. It does not alter peak detection or integration.

## Report Provenance Rules

Manual and guided decisions must be visible in the report provenance:

- mode;
- user confirmation status;
- artifact paths;
- validation warnings;
- audit entries;
- gate mapping decision.

The report must distinguish:

- deterministic auto evidence;
- user-confirmed evidence;
- imported/manual evidence;
- VLM/OCR semantic evidence.

## VLM Boundary

VLM may help read or classify local text crops and judge overlays. It must not populate:

- pixel geometry used for calculation;
- peak RT as final measurement;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index.

## Phase 2 Handoff

The next phase may create graphPanel/plotArea confirmation UI using these contracts. It must use the state machine, persist audit entries, and keep evidence package exports intact.

## Phase 2 Addendum: ROI Editor Contracts

Phase 2 adds a reducer-backed editor that writes graphPanel and plotArea confirmations into the Phase 1 contracts.

New ROI edit sources:

- `USER_CONFIRMED` - the user accepts the suggested ROI without changing bounds.
- `USER_EDITED_AUTO_SUGGESTION` - the user starts from an automatic suggestion and adjusts it.
- `MANUAL` - the user defines a ROI without an automatic suggestion.

Confirmation objects must preserve:

- normalized image-coordinate bounds;
- source;
- timestamp;
- user/session provenance;
- related image id/path;
- optional overlay artifact path;
- warning codes;
- gate status.

Review-grade ROI warnings remain part of report provenance. A graphPanel or plotArea confirmation alone cannot make a report release-ready; calibration, trace, evidence package, and validator gates still apply.

## Phase 3 Addendum: Calibration Editor Contracts

Phase 3 makes the calibration contracts operational through a reducer-backed editor model.

New editor-side contracts:

- `CalibrationAnchorPlacementState` - selected axis, selected anchor, unit labels, and source.
- `CalibrationAnchorEditorSnapshot` - lightweight serializable editor state.
- `CalibrationAxisFitSummary` - per-axis slope/intercept, residual report, fit status, and warnings.
- `CalibrationEditorEvaluation` - combined X/Y validation result and gate status.

Reducer operations:

- add anchor;
- move anchor;
- remove anchor;
- set anchor value;
- set anchor axis;
- reset anchors;
- evaluate fit;
- confirm calibration.

Confirmation writes `UserConfirmedCalibration` and advances the guided state to `CALIBRATION_VALIDATED`. The confirmation preserves timestamp, user/session provenance, source, anchor statuses, residual reports, warnings, and optional overlay artifact path.

Two-anchor calibration remains review-grade. Three or more accepted anchors per axis can become `USER_CONFIRMED` when residual checks pass. `AUTO_DIAGNOSTIC` cannot use these confirmation objects as release evidence.

## Phase 4 Addendum: Trace Overlay Contracts

Phase 4 makes trace confirmation operational through a reducer-backed overlay review model.

New or expanded trace contracts:

- `TraceOverlayPoint` - normalized image-coordinate point with confidence.
- `TraceOverlaySource` - auto/user/review/rejected/imported source classification.
- `TraceConfirmationStatus` - valid, review, rejected, invalid, or missing confirmation state.
- `TraceOverlayEditorSnapshot` - lightweight editor state for image, plotArea, trace points, metrics, artifacts, and warnings.
- `TraceOverlayEvaluation` - quality status, gate status, normalized metrics, and issues.

Confirmation writes `UserConfirmedTrace` and advances guided state to `TRACE_CONFIRMED`. The evidence records source trace id, trace points, plotArea bounds, calibration set id, quality metrics, warnings, artifact paths, user/session provenance, timestamp, decision, and rejection reason when applicable.

Manual trace drawing is not part of Phase 4. Trace rejection and review acceptance are first-class decisions so bad auto traces cannot silently become release-quality evidence.

## Phase 5 Addendum: Peak Evidence Contracts

Phase 5 adds autonomous peak evidence contracts while keeping user peak review as Assisted Review / Manual Advanced fallback.

New or expanded peak contracts:

- `PeakEvidence` - deterministic or user-reviewed evidence row for a peak.
- `PeakMetricEvidence` - value/status/source wrapper for RT, height, area, width, FWHM, S/N, prominence, and baseline-related metrics.
- `PeakBoundaryEvidence` - integration boundary, boundary method, integration method, and baseline method evidence.
- `PeakProvenance` - calculation run, source signal, algorithm version, trace source, and visible user-intervention metadata.
- `UserConfirmedPeakSet` - explicit user decisions only when Assisted Review or Manual Advanced is active.

`AUTONOMOUS_PRODUCTION` can satisfy peak gates through `AUTO_VALID` peak evidence. User-confirmed or user-edited peak sets must remain visibly user-sourced in report provenance.
