# Guided Digitization State Machine

This document defines the state machine created in Phase 1. It is a contract for future screens, not a UI implementation.

## Steps

`GuidedWorkflowStep` is ordered:

1. `IMAGE_LOADED`
2. `GRAPH_PANEL_SUGGESTED`
3. `GRAPH_PANEL_CONFIRMED`
4. `PLOT_AREA_SUGGESTED`
5. `PLOT_AREA_CONFIRMED`
6. `AXIS_TICKS_SUGGESTED`
7. `CALIBRATION_POINTS_CONFIRMED`
8. `CALIBRATION_VALIDATED`
9. `TRACE_EXTRACTED`
10. `TRACE_CONFIRMED`
11. `PEAKS_DETECTED`
12. `PEAKS_CONFIRMED`
13. `REPORT_READY`
14. `DIAGNOSTIC_ONLY`

## Step Statuses

`GuidedStepStatus` values:

- `NOT_STARTED`
- `SUGGESTED`
- `IN_PROGRESS`
- `CONFIRMED`
- `VALIDATED`
- `REVIEW_REQUIRED`
- `REJECTED`
- `BLOCKED`
- `SKIPPED`

## Transition Rules

`GuidedDigitizationStateMachine` enforces a conservative Phase 1 rule:

- transition to the same step is allowed;
- transition to the next ordered step is allowed;
- transition to `DIAGNOSTIC_ONLY` is allowed from any step;
- skipping ahead is rejected.

This is intentionally strict. Future UI may allow backtracking, but it must preserve audit entries and must not erase previous confirmation evidence.

## Audit Trail

Each transition records a `GuidedWorkflowAuditEntry` with:

- timestamp;
- step;
- action;
- actor;
- optional details.

Future screens should append new entries when a user changes graph bounds, calibration anchors, trace confirmation, or peak review decisions.

## Confirmation Semantics

`UserConfirmationStatus.CONFIRMED` is required for user-confirmed gates.

Confirmed objects must include:

- timestamp;
- user/session provenance;
- related image ID;
- artifact path when available;
- validation warnings;
- gate status.

`AUTO_DIAGNOSTIC` must not interpret these objects as release gates. `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` may.

## Terminal Diagnostic Path

`DIAGNOSTIC_ONLY` is a terminal diagnostic state, not a failure to produce evidence. Future UI must still export evidence package artifacts before closing the run.

## Phase 2 Handoff

Phase 2 may build UI around this model, but must not:

- bypass state transitions;
- hide missing confirmations;
- write release-ready report status before gate evaluation;
- store heavy images or masks in transient Compose saved state;
- allow VLM-provided numeric geometry or chromatographic metrics.

## Phase 2 Addendum: ROI Confirmation

Phase 2 implements the first guided UI surface for `GRAPH_PANEL_CONFIRMED` and `PLOT_AREA_CONFIRMED`.

The editor uses `GuidedRoiEditorSnapshot` as serializable UI state and writes confirmed evidence back into:

- `GraphPanelConfirmation`;
- `PlotAreaConfirmation`;
- `RoiConfirmationEvidence`.

The Phase 2 editor may set:

- `GRAPH_PANEL_CONFIRMED`;
- `PLOT_AREA_CONFIRMED`.

It must not set:

- `CALIBRATION_POINTS_CONFIRMED`;
- `CALIBRATION_VALIDATED`;
- `TRACE_CONFIRMED`;
- `PEAKS_CONFIRMED`;
- `REPORT_READY`.

After `PLOT_AREA_CONFIRMED`, the UI may show only a disabled `Next: calibration anchors` handoff until Phase 3 implements calibration anchor placement.

## Phase 3 Addendum: X/Y Calibration

Phase 3 implements guided calibration anchor placement and may set:

- `CALIBRATION_POINTS_CONFIRMED`;
- `CALIBRATION_VALIDATED`.

The calibration editor consumes confirmed graphPanel and plotArea evidence and writes:

- `ManualCalibrationAnchor` entries;
- `UserCalibrationSet`;
- `CalibrationResidualReport`;
- `UserConfirmedCalibration`.

The editor must not set:

- `TRACE_EXTRACTED`;
- `TRACE_CONFIRMED`;
- `PEAKS_DETECTED`;
- `PEAKS_CONFIRMED`;
- `REPORT_READY`.

Two anchors per axis allow a review-grade linear transform. Three or more anchors per axis may validate the transform when residual checks pass. `AUTO_DIAGNOSTIC` still cannot treat user anchors as release evidence.
