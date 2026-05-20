# Phase 4: Autonomous Trace Extraction + Evidence Review

## Realigned Goal

Phase 4 provides the trace evidence layer for autonomous production. The system should score automatic trace extraction first. The overlay UI is retained as Assisted Review fallback and evidence inspection.

## Preserved Work

- Trace quality contracts and metrics.
- Trace overlay reducer/evaluation.
- Trace overlay Compose screen.
- User accept-review/reject decisions.
- Provenance fields and artifact paths.
- Guided tests for review behavior.

## New Realignment

- Automatic valid trace uses `TraceGateStatus.AUTO_VALID`.
- `AUTONOMOUS_PRODUCTION` maps automatic valid trace evidence to `EvidenceGateStatus.VALID`.
- `ASSISTED_REVIEW` maps user-confirmed valid trace to `EvidenceGateStatus.USER_CONFIRMED`.
- `AUTO_DIAGNOSTIC` remains isolated from user/manual confirmation evidence.
- `GUIDED_PRODUCTION` remains a deprecated compatibility alias.

## Trace Evidence States

| Trace condition | Gate behavior |
| --- | --- |
| Auto trace passes quality and artifacts | `VALID` in `AUTONOMOUS_PRODUCTION`. |
| Auto trace has sparse/fragmented/unknown metrics | `REVIEW`. |
| Auto trace missing points/outside plotArea/severely contaminated | `INVALID` or `MISSING`. |
| User accepts valid trace in Assisted Review | `USER_CONFIRMED`. |
| User accepts review-grade trace | `REVIEW`. |
| User rejects trace | `INVALID`. |

## Out Of Scope

- Manual trace drawing.
- Peak editor.
- Peak detection math.
- `CalculationEngine` changes.
- VLM behavior changes.
- Full-auto geometry/OCR/runtime rewrite.

## Tests Required

- Automatic valid trace can satisfy trace gate in `AUTONOMOUS_PRODUCTION`.
- Missing trace artifacts block autonomous valid acceptance.
- `AUTO_DIAGNOSTIC` cannot use assisted/manual trace evidence.
- `ASSISTED_REVIEW` and `MANUAL_ADVANCED` preserve user intervention provenance.
