# Phase 9C E2B Model Regression Review

## Boundary

E2B/Gemma is allowed to provide semantic/OCR assistance, warning explanations, and model availability diagnostics. It is not allowed to provide graphPanel pixels, plotArea pixels, tick pixels, calibration coefficients, trace values, peak metrics, or final chromatographic measurements.

## Phase 9B Regression

| Fixture | Deterministic | E2B | Regression |
| --- | --- | --- | --- |
| `bench_02_mz92_belyi_tigr` | reported 2 graphs, `DIAGNOSTIC_ONLY` | reported 0 graphs, `BLOCKED` | Critical. E2B path appeared worse than deterministic path. |

Artifact review showed the E2B terminal failure still had graph-stage evidence, but terminal report metadata and suite summary collapsed the completed report graph count to zero. Phase 9C therefore treats this as both:

- a model comparison blocker until rerun proves E2B no longer worsens deterministic evidence;
- a summary/evidence classification bug because graph failure packages must count as attempted graph evidence, not completed report graphs.

## Phase 9C Rule

Model-enabled mode may produce a worse semantic status, timeout, or unsupported-claim warning. It may not:

- reduce deterministic graph count without deterministic rejection evidence;
- reduce calibration/trace/peak evidence;
- alter deterministic numeric fingerprints;
- suppress graph failure packages;
- convert a graph-stage failure into an unexplained zero-graph report.

## Required Rerun Evidence

The final Phase 9C suite must compare deterministic and E2B for all fixtures:

- graph count;
- report gate;
- validator verdict;
- runtime failure class;
- X/Y calibration status;
- trace and peak status;
- model diagnostics;
- blocking issue count;
- export completeness.
