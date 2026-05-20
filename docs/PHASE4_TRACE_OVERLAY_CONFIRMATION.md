# Phase 4: Autonomous Trace Extraction + Evidence Review

Phase 4 was first implemented as guided trace overlay confirmation. It is now realigned as the autonomous trace evidence layer with Assisted Review fallback. The existing overlay UI remains useful, but it is not the default production path.

This phase does not implement manual trace drawing, peak editing, peak integration changes, VLM changes, or `CalculationEngine` changes.

## Scope

Implemented:

- `TraceOverlayPoint` contract for overlay polyline evidence;
- `TraceOverlaySource` and `TraceConfirmationStatus` provenance/status enums;
- expanded `TraceQualitySummary` metrics;
- `TraceOverlayEditorSnapshot` and `TraceOverlayEvaluation`;
- reducer logic for reset, accept valid, accept review, and reject;
- reusable Compose/KMP trace overlay screen;
- autonomous `AUTO_VALID` trace gate plus assisted report-gate mapping through existing `UserConfirmedTrace`;
- tests for missing plotArea, missing points, out-of-plot points, sparse/review traces, rejection, reset, AUTO isolation, guided gate mapping, and serialization.

Out of scope:

- manual trace drawing/editing;
- peak editor and peak review;
- auto curve extraction algorithm changes;
- report rendering redesign;
- runtime evidence exporter changes.

## Data Flow

```mermaid
flowchart LR
    A["Confirmed plotArea"] --> C["Trace overlay editor"]
    B["Confirmed calibration"] --> C
    D["Auto trace candidate"] --> C
    C --> E["TraceOverlayEvaluation"]
    E --> F["Trace evidence gate"]
    F --> G["AUTO_VALID or Assisted Review"]
    G --> H["GuidedReportGateMapper"]
```

## Trace Quality Inputs

The model accepts or infers:

- `pointCount`;
- `columnCoverageRatio`;
- `maxGapColumns`;
- `componentCount`;
- `branchPointCount`;
- `selectedComponentCoverage`;
- `textContaminationScore`;
- `baselineTouchRatio`;
- `frameTouchRatio`;
- `traceConfidence`;
- trace points.

If available metrics are incomplete, the trace is review-grade. Missing metrics are never fabricated.

## Validation Rules

`INVALID`:

- no confirmed plotArea;
- missing calibration when calibrated trace is required;
- no trace points;
- any trace point outside plotArea;
- severe sparse coverage;
- severe text contamination;
- severe frame/border contact;
- very low confidence;
- explicit user rejection.

`REVIEW`:

- low point count;
- sparse column coverage;
- large gaps;
- multiple components;
- branch-like structure;
- unavailable metrics;
- user accepts as review-grade.

`VALID`:

- plotArea exists;
- calibration exists when calibrated trace is required;
- trace points exist and stay inside plotArea;
- quality metrics pass thresholds;
- overlay/centerline artifacts exist for autonomous release;
- automatic trace can satisfy the trace gate without user confirmation in `AUTONOMOUS_PRODUCTION`.

## Evidence

`TraceConfirmationEvidence` stores:

- source trace id;
- overlay, mask, and centerline artifact paths when available;
- trace points;
- quality status and metrics;
- warning codes;
- source (`AUTO_EXTRACTED`, `USER_CONFIRMED`, `USER_REVIEW_CONFIRMED`, `USER_REJECTED`);
- plotArea bounds;
- calibration set id;
- rejection reason.

`UserConfirmedTrace` stores the evidence plus timestamp, user/session provenance, edit decision, gate status, and confirmation status.

## Release Gate Behavior

- automatic valid trace maps to `EvidenceGateStatus.VALID` in `AUTONOMOUS_PRODUCTION`.
- `USER_CONFIRMED_VALID` trace maps to `EvidenceGateStatus.USER_CONFIRMED` in `ASSISTED_REVIEW`, deprecated `GUIDED_PRODUCTION`, or `MANUAL_ADVANCED`.
- `USER_CONFIRMED_REVIEW` trace maps to `EvidenceGateStatus.REVIEW`.
- rejected/invalid trace maps to `EvidenceGateStatus.INVALID`.
- `AUTO_DIAGNOSTIC` ignores guided trace confirmation objects.

Phase 4 cannot complete peak review. Phase 5 must add peak review/edit evidence before peak-specific claims are treated as user-reviewed.

## Realignment Note

`GUIDED_PRODUCTION` is now a compatibility alias for earlier contracts. New product planning should use `AUTONOMOUS_PRODUCTION` for the primary path and `ASSISTED_REVIEW` for the trace overlay UI.
