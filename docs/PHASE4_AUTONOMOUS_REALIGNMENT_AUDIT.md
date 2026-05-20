# Phase 4 Autonomous Realignment Audit

## Verdict

The Phase 4 implementation is useful, but its original framing was too guided-first. The trace overlay model, metrics, provenance fields, UI, and tests should be preserved and reclassified as the trace evidence review layer for `AUTONOMOUS_PRODUCTION`, `AUTO_DIAGNOSTIC`, `ASSISTED_REVIEW`, and `MANUAL_ADVANCED`.

## What Was Implemented

- `TraceOverlayPoint`, `TraceOverlaySource`, `TraceConfirmationStatus`, and expanded `TraceQualitySummary`.
- `TraceOverlayEditorSnapshot` and `TraceOverlayEvaluation`.
- `GuidedTraceOverlayReducer` with trace loading, quality evaluation, reset, accept valid, accept review, and reject.
- `GuidedTraceOverlayScreen` for image + plotArea + trace overlay inspection.
- Tests for missing plotArea, missing points, out-of-plot trace, sparse/review traces, user review, rejection, reset, auto diagnostic isolation, guided gate mapping, and serialization.
- Phase 4 docs describing trace overlay confirmation.

## Useful Components To Preserve

| Component | Why it remains useful |
| --- | --- |
| Trace quality model | It can score autonomous trace evidence before user review. |
| Trace overlay screen | It provides evidence visualization and Assisted Review fallback. |
| Trace confirmation evidence | It records artifacts, warnings, points, source, and provenance. |
| AUTO_DIAGNOSTIC isolation tests | They prevent user/manual evidence from silently upgrading diagnostic runs. |
| Serialization tests | They protect persisted review/trace decisions. |

## Misclassified Components

| Component or wording | Problem | Realignment |
| --- | --- | --- |
| “Guided Trace Overlay Confirmation” phase title | Implies user confirmation is the main path. | Phase 4 is now “Autonomous Trace Extraction + Evidence Review.” |
| `GUIDED_PRODUCTION` as reliable target | Makes manual confirmation the product center. | Deprecated alias; use `ASSISTED_REVIEW` for repair/review. |
| Valid trace requires “user confirms valid trace” | Overstates human confirmation as the source of trace validity. | Autonomous valid trace can satisfy trace gate as `VALID`; user-confirmed trace is repair provenance. |
| Trace overlay UI in main path | Would slow normal images and bias product to manual review. | UI appears only for review/fallback or explicit evidence inspection. |

## What Changed

- Added `AUTONOMOUS_PRODUCTION` and `ASSISTED_REVIEW` modes.
- Kept `GUIDED_PRODUCTION` as a deprecated compatibility alias.
- Added `TraceGateStatus.AUTO_VALID`.
- Added autonomous trace acceptance path that requires valid trace quality and overlay/centerline artifacts.
- Kept existing user confirmation/rejection as Assisted Review behavior.
- Updated report-gate docs to separate automatic `VALID` from `USER_CONFIRMED`.

## What Must Not Change In This Slice

- No `CalculationEngine` rewrite.
- No chromatographic math changes.
- No peak detection/integration tuning.
- No broad full-auto geometry/OCR/VLM/runtime rewrite.
- No deletion of Phase 2/3/4 editors.

## Regression Risks

- Old docs may continue to say `GUIDED_PRODUCTION` is the target path.
- Persisted enum values need compatibility handling if old reports use `GUIDED_PRODUCTION`.
- User-confirmed review-grade traces must not become release-ready silently.
- Autonomous trace acceptance must not pass without artifacts or quality metrics.
- Existing guided tests must continue to pass because the UI remains Assisted Review fallback.

## Required Regression Coverage

- `AUTONOMOUS_PRODUCTION` can satisfy trace gate through automatic valid evidence.
- `AUTO_DIAGNOSTIC` cannot consume autonomous or user trace confirmation as release evidence unless its own automatic gate evidence is complete.
- `ASSISTED_REVIEW` can satisfy gates through explicit user-confirmed evidence.
- Deprecated `GUIDED_PRODUCTION` remains safe as an alias.
- Trace overlay artifact requirements block autonomous release when missing.
