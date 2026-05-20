# Autonomous Trace Quality Model

Phase 4 trace quality is first an autonomous evidence gate. It does not change curve extraction. It evaluates trace evidence already produced by upstream extraction and decides whether the trace is automatic `VALID`, `REVIEW`, or `INVALID`. The same model also powers Assisted Review when automatic evidence is weak.

## Inputs

`TraceOverlayEditorSnapshot` carries:

- normalized image dimensions;
- graphPanel bounds;
- plotArea bounds;
- source trace id;
- trace overlay points;
- original auto trace points;
- quality metrics;
- source and artifact paths;
- calibration set id;
- warnings and rejection reason.

## Metrics

Supported metrics:

- `pointCount`;
- `columnCoverageRatio`;
- `maxGapColumns`;
- `componentCount`;
- `branchPointCount`;
- `selectedComponentCoverage`;
- `textContaminationScore`;
- `baselineTouchRatio`;
- `frameTouchRatio`;
- `traceConfidence`.

When metrics are absent, the reducer infers only safe basics from points:

- point count;
- column coverage when plotArea width is known;
- max X gap;
- mean point confidence.

No unavailable metric is fabricated. Unknown evidence creates review warnings.

## Status Thresholds

`VALID` requires:

- at least 24 points;
- column coverage at least 0.30;
- confidence at least 0.70;
- max gap not too large relative to plotArea;
- low text contamination;
- low frame touch;
- points inside plotArea;
- overlay and centerline artifacts for autonomous release;
- no blocking warnings.

`REVIEW` is used for:

- 8 to 23 points;
- column coverage from 0.05 to 0.30;
- confidence from 0.35 to 0.70;
- moderate frame/text contamination;
- multiple components or branch-like structure;
- unknown quality metrics.

`INVALID` is used for:

- no points;
- fewer than 8 points;
- points outside plotArea;
- missing plotArea;
- missing calibration when calibrated trace is required;
- column coverage below 0.05;
- max gap above the blocking threshold;
- severe text/frame contamination;
- confidence below 0.35;
- user rejection.

## Gate Mapping

| Trace decision | Gate |
| --- | --- |
| automatic valid evidence | `TraceGateStatus.AUTO_VALID` |
| user accepts valid in Assisted Review | `TraceGateStatus.USER_CONFIRMED` |
| automatic or user review-grade evidence | `TraceGateStatus.REVIEW_REQUIRED` |
| reject | `TraceGateStatus.INVALID` |
| missing | `TraceGateStatus.MISSING` |

`GuidedReportGateMapper` maps `AUTO_VALID` to `EvidenceGateStatus.VALID` only in `AUTONOMOUS_PRODUCTION`. `AUTO_DIAGNOSTIC` remains isolated and cannot use assisted/manual trace confirmation as user evidence. `ASSISTED_REVIEW` and `MANUAL_ADVANCED` map explicit user acceptance to `USER_CONFIRMED`.

## Provenance

Trace confirmation records:

- source trace id;
- user/session provenance;
- timestamp;
- decision;
- trace source;
- quality metrics and warnings;
- artifact paths when available;
- plotArea bounds;
- calibration set id;
- rejection reason when rejected.
