# Assisted Review Fallback Workflow

## Purpose

`ASSISTED_REVIEW` lets a user repair only the stages that autonomous analysis cannot validate. It is not the default path for normal images.

## Entry Conditions

Assisted Review starts when:

- graphPanel or plotArea is partial/uncertain;
- calibration is invalid or review-grade;
- trace is sparse, fragmented, contaminated, or missing artifacts;
- peak evidence is weak or ambiguous;
- validator blocks release but identifies repairable evidence.

## Existing Tools

| Phase | Tool | Assisted Review role |
| --- | --- | --- |
| Phase 2 | Guided ROI editor | Correct graphPanel/plotArea only when auto ROI is uncertain. |
| Phase 3 | Guided calibration editor | Repair invalid/review X/Y calibration. |
| Phase 4 | Trace overlay screen | Review, accept, or reject extracted trace evidence. |
| Phase 5 | Peak evidence review contracts | Review peak apexes/boundaries after autonomous peak validation; full manual editor remains future work. |

## Rules

- User intervention must be explicit and stored.
- The report must show user-reviewed/manual provenance.
- Review-grade acceptance cannot silently become release-ready.
- `AUTO_DIAGNOSTIC` cannot consume assisted evidence as automatic release evidence.
- `MANUAL_ADVANCED` remains available for expert workflows beyond assisted repair.
- user peak decisions must appear in report provenance and cannot be hidden as automatic detection.

## UX Principles

- Ask the user only for the failed stage.
- Show the automatic suggestion and reason for review.
- Preserve zoom/pan image context.
- Use non-color-only status labels.
- Keep controls touch-friendly and localizable.
