# Phase 1 Shared Contracts + GuidedDigitizationState

Phase 1 creates the shared state and contract layer needed for future `GUIDED_PRODUCTION` and `MANUAL_ADVANCED`. It does not implement UI screens, manual calibration tools, trace editing, peak editing, or automatic image-analysis fixes.

## Scope

Implemented in this phase:

- serializable guided workflow state model;
- geometry confirmation contracts;
- calibration confirmation contracts;
- trace confirmation contracts;
- peak review contracts;
- guided state-machine transition helper;
- guided-to-release-gate mapper;
- contract tests for transition, serialization, anchor minima, and release-gate mapping.

Explicitly not implemented:

- Guided UI screens;
- manual calibration editor;
- trace editor;
- peak editor;
- `CalculationEngine` changes;
- peak detection or integration math changes;
- geometry/OCR/VLM runtime rewrites.

## Code Contracts

Primary file:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/guided/GuidedDigitizationContracts.kt`

Primary test:

- `composeApp/src/commonTest/kotlin/com/chromalab/feature/processing/guided/GuidedDigitizationContractsTest.kt`

## Product Modes

`GuidedDigitizationMode` mirrors Phase 0 product modes:

- `AUTO_DIAGNOSTIC`
- `GUIDED_PRODUCTION`
- `MANUAL_ADVANCED`

`GuidedDigitizationMode.toProcessingMode()` maps directly to report `ProcessingMode` values.

`AUTO_DIAGNOSTIC` remains diagnostic by default. It cannot treat guided/user confirmation objects as release evidence. It must use deterministic auto evidence and evidence package validation.

`GUIDED_PRODUCTION` and `MANUAL_ADVANCED` may satisfy gates through persisted user confirmation, but only after the required objects exist and are mapped into the release gate contract.

## Guided State Model

`GuidedDigitizationState` contains:

- stable state ID and schema version;
- mode;
- current workflow step;
- per-step statuses;
- image reference;
- graphPanel confirmation;
- plotArea confirmation;
- calibration confirmation;
- trace confirmation;
- peak review confirmation;
- auto diagnostic gate evidence;
- timestamps;
- audit trail;
- warnings.

The schema version is:

- `1.0.0-phase-1`

The model is intentionally serializable and platform-neutral so it can be stored in a future repository, Room entity, JSON session file, or evidence package.

## Geometry Confirmation

New contracts:

- `UserConfirmedGraphPanel`
- `UserConfirmedPlotArea`
- `GraphPanelConfirmation`
- `PlotAreaConfirmation`
- `RoiEditSource`
- `RoiConfirmationEvidence`

Each confirmation records:

- bounds;
- source;
- confirmation status;
- timestamp;
- user/session provenance;
- related image ID/path;
- optional overlay artifact path;
- validation warnings;
- gate status.

`graphPanel` is the full graph block. `plotArea` is the coordinate rectangle inside the graphPanel. The contracts preserve this distinction for later UI and report provenance.

## Calibration Confirmation

New contracts:

- `ManualCalibrationAnchor`
- `CalibrationAxis`
- `CalibrationAnchorSource`
- `CalibrationAnchorStatus`
- `UserCalibrationSet`
- `CalibrationResidualReport`
- `UserConfirmedCalibration`

Calibration supports:

- X and Y axes;
- accepted and rejected anchors;
- unit labels;
- residuals in pixels and units;
- RMSE and R2 when available;
- monotonicity status;
- calibration gate status.

Minimum structural rule:

- at least two accepted X anchors;
- at least two accepted Y anchors.

Robust-fit readiness rule:

- three or more accepted anchors per axis.

Two-anchor user calibration is structurally valid but review-grade until later phases add confirmation UI, residual review, and explicit product policy for exceptional cases.

## Trace Confirmation

New contracts:

- `UserConfirmedTrace`
- `TraceConfirmationEvidence`
- `TraceQualityStatus`
- `TraceEditDecision`
- `TraceGateStatus`
- `TraceQualitySummary`

Trace confirmation supports:

- accepting an auto-extracted trace;
- rejecting an auto trace;
- marking a trace review-grade;
- future trimming/redrawing/import decisions;
- overlay and centerline artifact paths;
- quality metrics summary.

Phase 1 does not implement trace editing.

## Peak Review

New contracts:

- `UserPeakEditDecision`
- `PeakEditAction`
- `PeakReviewStatus`
- `UserConfirmedPeakSet`
- `PeakReviewGateStatus`

Future actions are represented:

- `ADD`
- `REMOVE`
- `MERGE`
- `SPLIT`
- `ADJUST_BOUNDARY`
- `MARK_SHOULDER`
- `MARK_REVIEW`
- `ACCEPT_AUTO`

Phase 1 does not implement the peak editor and does not change peak math.

## Gate Mapping

`GuidedReportGateMapper.evaluate(state)` maps guided state into Phase 0 gate concepts:

- graphPanel confirmation -> `EvidenceGateStatus.USER_CONFIRMED`;
- plotArea confirmation -> `EvidenceGateStatus.USER_CONFIRMED`;
- X/Y calibration -> `USER_CONFIRMED`, `REVIEW`, `INVALID`, or `MISSING`;
- trace confirmation -> `USER_CONFIRMED`, `REVIEW`, `INVALID`, or `MISSING`;
- peak review -> supporting gate;
- evidence package and source provenance remain required.

The mapper returns:

- `RELEASE_READY`
- `REVIEW_ONLY`
- `DIAGNOSTIC_ONLY`
- `BLOCKED`

Release-ready remains blocked without evidence package and source provenance.

## Acceptance

Phase 1 is accepted when:

- shared contracts compile in commonMain;
- state transitions block skipped steps;
- serialization roundtrip preserves confirmations;
- `AUTO_DIAGNOSTIC` cannot use user-confirmed objects as release evidence;
- calibration minimum anchors are enforced;
- missing required confirmations prevent `RELEASE_READY`;
- docs and tests record the contract.
