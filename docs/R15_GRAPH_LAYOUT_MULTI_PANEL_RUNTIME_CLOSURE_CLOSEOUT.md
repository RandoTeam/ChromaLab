# R15 - Graph Layout And Multi-Panel Runtime Closure Closeout

Date: 2026-06-07

Status: `R15_GRAPH_LAYOUT_MULTI_PANEL_RUNTIME_CLOSURE_IMPLEMENTED`

Verdict: `PHASE_9_REMAINS_BLOCKED_PENDING_ANDROID_RERUN`

## Scope

R15 updates runtime/evidence/report propagation for multi-panel chromatogram
layouts. It does not modify `CalculationEngine`, chromatographic math, trace
extraction, peak metrics, validators, or E2B authority.

## Implemented Changes

- Added `GeometryGraphResult` and `GeometryPipelineResult.graphResults` so the
  geometry pipeline can preserve per-physical-graph panel, plotArea, axis, OCR,
  calibration, strategy, and warning evidence.
- Changed `GeometryPipelineRunner` to evaluate
  `GraphMultiplicityResolution.resolvedGraphPanels` as the source of physical
  graph units when deterministic multiplicity reports more than one graph.
- Assigned stable 1-based graph indexes in reading order for multi-panel
  geometry evaluations.
- Kept existing single-graph fields as the primary/first selected graph for
  compatibility.
- Updated `ProcessingFlowScreen` to advance through resolved physical graph
  regions instead of raw `graphResult.filteredRegions`, preventing duplicate or
  nested pseudo-panels from driving graph iteration.
- Added deterministic TIC+ion semantic hints to `GraphLayoutClassifier` without
  allowing text to create graph count. Physical graph count still comes only
  from deterministic panel geometry.
- Added explicit stored-report metadata warning
  `multi_panel_report_aggregation_unsupported` whenever runtime detects
  multiple physical panels but a stored report section covers only one graph.
- Added test coverage for TIC+ion semantic naming, VLM advisory graph-count
  limits, four-panel preservation, per-graph stored report warning, and runtime
  evidence graph-package cardinality.

## Fixture Graph-Count Expectations

R15 keeps fixture graph-count expectations unchanged:

| Fixture | Expected graph units | R15 rule |
|---|---:|---|
| `bench_04_stacked_xic_resolution` | 4 | Preserve four resolved physical graph units. |
| `bench_05_tic_plus_ions` | 4 | Preserve four graph units; TIC+ion name requires deterministic text support. |
| `bench_06_photo_two_graphs_page` | 2 | Preserve two resolved physical graph units. |

No metadata expected-count changes were made.

## E2B Boundary

E2B remains advisory only. R15 does not allow model output to:

- change deterministic physical graph count;
- create panel grouping;
- alter calibration, trace, peak metrics, or report gates;
- erase deterministic graph candidates.

## Remaining Limitation

The app can now propagate resolved physical graph regions through geometry and
per-graph runtime processing. However, if a saved report still represents only
one graph section while multiple panels were detected, the report is explicitly
marked with `multi_panel_report_aggregation_unsupported`.

That warning is intentional and prevents silent overclaiming. Phase 9 is not
accepted until Android reruns prove multi-panel fixtures export complete
per-graph evidence and reports without critical blockers.

## Validation

Targeted validation completed:

- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.geometry.GraphLayoutClassifierTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.geometry.GraphMultiplicityResolverTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.report.ProcessingReportMetadataBuilderTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RuntimeEvidencePackageValidatorTest"`

Final build validation and Android device rerun status are recorded in the final
R15 task response.

Build validation completed:

- `git diff --check`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.ReportContractValidatorGeometryTest"`
- `.\gradlew.bat :composeApp:compileKotlinDesktop`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :androidApp:assembleValidation`

Android fixture reruns were not completed in this slice because `adb devices`
reported no connected device or emulator.

## Next Phase

If Android reruns show multi-panel reports still cannot aggregate complete
sections for `bench_04`, `bench_05`, or `bench_06`, the next slice should close
that report aggregation gap before moving to R16 trace extraction evidence.

If multi-panel report/evidence propagation is stable, the planned next phase is:

```text
R16 - Trace Extraction Evidence Candidate
```
