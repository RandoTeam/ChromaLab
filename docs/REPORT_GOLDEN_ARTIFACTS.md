# Report Golden Artifacts

Status: Phase 7B acceptance coverage

## Purpose

Phase 7B adds structured golden assertions for report evidence and overclaim prevention. Screenshot goldens remain a future visual-regression task; current coverage validates JSON contract content, HTML output, Markdown output, gate status visibility, evidence status visibility, and privacy/export separation.

## Covered Report Shapes

1. `RELEASE_READY` single graph.
2. `REVIEW_ONLY` single graph with review peak evidence.
3. `DIAGNOSTIC_ONLY` missing calibration.
4. `BLOCKED` report with missing graph evidence.
5. Multi-graph report with per-graph status and warning visibility.
6. Knowledge/VLM grounded explanation report.
7. Compound hypothesis without identity evidence.
8. Kovats caveat without a valid reference series.

## Assertions

Report golden tests validate:

- JSON includes `knowledgeCitations`, `usedEntryIds`, and `usedEntryRecords`.
- HTML and Markdown render Knowledge Pack citations in the technical/evidence appendix.
- Release-ready is not claimed when calibration, trace, or peak evidence is missing.
- Empty graph reports produce a visible `BLOCKED` gate.
- Multi-graph reports preserve each graph index and graph-level warning.
- Compound hypotheses are rendered as candidate hypotheses, not identified compounds.
- Kovats/RI values cannot be reported as calculated without reference retention times.
- User report exports do not list `NEVER_SHARED_BY_DEFAULT` artifacts.

## Test Location

Primary Phase 7B coverage lives in:

- `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase7BReportEvidenceHardeningTest.kt`
- `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase7BReportFixtures.kt`
