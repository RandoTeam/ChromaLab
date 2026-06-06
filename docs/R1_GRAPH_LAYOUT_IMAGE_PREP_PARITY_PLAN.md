# R1 Graph/Layout And Image Preparation Parity Plan

Date: 2026-06-06

Status: `R1_PARITY_PLAN_READY`

Scope: parity and promotion plan for Stage 1-3 only. No runtime behavior,
validators, graph counts, chromatographic math, or model behavior changed.

## Purpose

This plan defines how ChromaLab will prove that a replacement image
preparation, graph discovery, and plotArea/layout layer is better than the
current implementation before it becomes active.

The goal is replacement, not accumulation. New code must run in shadow/parity
mode first, prove its output, then demote or retire the old path.

## Corpus

### Required Android Fixtures

| Fixture | Expected graph count | R1 role |
|---|---:|---|
| `white_tiger_ion71` | 1 | Regression shield: must stay one graph and preserve downstream REVIEW behavior. |
| `bench_01_mz71_screenshot_page` | 2 | Printed-page two-graph blocker; verifies graph split and page/text rejection. |
| `bench_02_mz92_belyi_tigr` | 1 | One-graph duplicate/split blocker. |
| `bench_03_small_tic_export` | 1 | Low-resolution REVIEW witness. |
| `bench_04_stacked_xic_resolution` | 4 | Stacked XIC/panel grouping witness. |
| `bench_05_tic_plus_ions` | 4 | TIC plus ion panel blocker; verifies channel separation. |
| `bench_06_photo_two_graphs_page` | 2 | Photo/perspective/two-graph page witness. |
| `bench_07_rotated_page_photo` | 1 | Rotated page REVIEW witness. |

### Optional Corpus

| Source | Role |
|---|---|
| `bench_08_mz71_duplicate_candidate` | Optional one-graph duplicate candidate regression. |
| `benchmark/annotations/` | Human annotation source for graphPanel/plotArea/layout truth where available. |
| `benchmark/examples/phase9j_truth_audit/` | Source-controlled Phase 9J-derived prediction/evidence records. |
| `composeApp/src/androidMain/assets/validation/rust_axis_element/` | Rust parity crop corpus. |

### Current Truth Gaps

| Gap | R1/R2 handling |
|---|---|
| No locked Stage 1 normalized-image truth for all fixtures. | R2 must record normalized dimensions/hash or perceptual hash before comparing downstream geometry. |
| GraphPanel/plotArea annotations are incomplete for `bench_02`, `bench_03`, `bench_07`, and `white_tiger_ion71`. | Use graph count/layout/status as first gate; add box metrics only where annotations exist. |
| DR-C3 boxes are in original image coordinates. | R2 must preserve coordinate-space metadata before using IoU thresholds. |
| Phase 9J examples include prediction/evidence records, but not full per-case `truth.json`. | Do not treat Phase 9J records as complete golden truth for every metric. |

## Parity Outputs

Every Stage 1-3 parity run must write a small comparable record:

```text
Stage123ParityRecord
- fixtureId
- mode
- sourceImagePath
- sourceDimensions
- normalizedImagePath
- selectedPreprocessingVariant
- orientationDecision
- pageRectificationDecision
- graphPanelCandidates
- selectedGraphPanels
- rejectedGraphPanels
- expectedGraphCount
- detectedPhysicalGraphCount
- detectedReportGraphCount
- layoutClass
- selectedPlotAreas
- rejectedPlotAreas
- candidateRejectionReasons
- evidenceArtifacts
- timing
- status
- failureClass
- failureSubreason
```

## Metrics

### Image Preparation Metrics

| Metric | Purpose |
|---|---|
| source decode success | Detect unreadable source inputs early. |
| normalized dimension stability | Prevent accidental resizing/orientation regressions. |
| orientation decision | Verify rotated/photo fixtures. |
| page rectification status | Separate applied, skipped, and review-grade rectification. |
| selected variant id | Make preprocessing selection auditable. |
| variant score table | Prevent silent variant changes. |
| quality warnings | Preserve low-resolution and blur caveats. |
| runtime | Bound image preparation cost. |

### Graph Discovery Metrics

| Metric | Purpose |
|---|---|
| candidate count | Detect no-graph and candidate explosion cases. |
| selected graph count | Compare to expected graph count. |
| physical graph count | Product-level graph count. |
| report graph count | Report propagation graph count. |
| duplicate/nesting ratio | Prevent one graph becoming many reports. |
| candidate completeness score | Prevent partial graphPanel selection. |
| rejection reason coverage | Prevent opaque graph failure. |
| graphPanel IoU | Compare to annotations where available. |

### PlotArea/Layout Metrics

| Metric | Purpose |
|---|---|
| layout class | Verify single, stacked, TIC+ions, two-graph page semantics. |
| selected plotArea count | Verify each physical/report graph has a plotArea attempt. |
| plotArea containment | PlotArea must be inside graphPanel. |
| plotArea IoU | Compare to annotations where available. |
| axis/frame evidence count | Check downstream axis compatibility. |
| label-band evidence | Check downstream OCR/calibration compatibility. |
| status/failure subreason | Prevent generic BLOCKED. |

## Acceptance Thresholds

R2/R3 may define stricter numeric thresholds after the first parity run. R1 sets
minimum qualitative thresholds:

| Fixture | Minimum Stage 1-3 threshold |
|---|---|
| `white_tiger_ion71` | 1 graph, valid/review graphPanel and plotArea, no lower downstream status caused by Stage 1-3. |
| `bench_01_mz71_screenshot_page` | 2 candidate graph units or explicit graph-count failure evidence before calibration. |
| `bench_02_mz92_belyi_tigr` | 1 physical graph; duplicate candidates rejected with reasons. |
| `bench_03_small_tic_export` | 1 graph, low-res warnings retained, no graphPanel regression. |
| `bench_04_stacked_xic_resolution` | 4 panel groups or documented mismatch with visual evidence. |
| `bench_05_tic_plus_ions` | 4 panel groups or documented mismatch with visual evidence; channel text rejected as graph geometry. |
| `bench_06_photo_two_graphs_page` | 2 graph panels or page/prep failure with visual evidence. |
| `bench_07_rotated_page_photo` | 1 graph, rotated orientation handled or review-grade with evidence. |

## Current Benchmark Signals

These benchmark signals are not production acceptance. They guide R2 priority:

| Signal | Meaning |
|---|---|
| DR-C5 current Android P0 graph/layout baseline fails all eight P0 mode cases. | Existing runtime path is not enough as a graph/layout truth source. |
| DR-C6 label-band-assisted axis projection reaches four of four graph-count cases but only two of four layout cases. | Label bands help graph count but do not close layout semantics alone. |
| DR-C7 annotation/page-context upper bound reaches four of four layout cases. | Automatic text-role and page-context extraction are likely needed before promotion. |
| Rust axis-element corpus has 18 graph-level items. | Useful downstream crop witness, but not Stage 1-3 graph/layout truth. |

## Shadow Mode Rules

1. Run the current Kotlin Stage 1-3 path first and record its output.
2. Run the replacement Rust/Kotlin candidate path on the same input.
3. Do not let shadow output affect report gates or runtime behavior.
4. Compare outputs in benchmark records and summaries.
5. If the replacement fails where current code passes, keep current code active.
6. If the replacement passes where current code fails, inspect evidence before
   promotion.
7. If both fail, require precise subreason and graph-level artifact completeness.

## E2B Baseline Rules

E2B is a supported production FAST/weaker-device model mode, but it remains
advisory for Stage 1-3:

- E2B may add OCR/semantic warnings and graph-candidate review comments.
- E2B may not create graphPanel, plotArea, graph count, pixel coordinates,
  calibration coefficients, or chromatographic metrics.
- E2B disagreement keeps deterministic output and marks REVIEW.
- E2B failure cannot suppress deterministic candidates.

## Validation Plan

### Documentation-Only R1

Required:

```powershell
git diff --check
```

Gradle is not required for R1 because no source code, runtime contracts, tests,
or build files are changed.

### Future R2/R3 Implementation

Targeted checks should include:

```powershell
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.geometry.*"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"
.\tools\rust\Run-RustCoreChecks.ps1
```

If runtime Stage 1-3 changes are integrated, rerun Android deterministic and E2B
fixtures before any acceptance claim.

## Promotion Checklist

- [ ] Shadow records exist for all eight fixtures.
- [ ] Deterministic and E2B records are compared.
- [ ] Graph count mismatches are either fixed or product/scientific/QA signed
      off.
- [ ] Candidate overlays exist.
- [ ] Rejected candidates have reasons.
- [ ] PlotArea containment is checked.
- [ ] No regression on `white_tiger_ion71`, `bench_03_small_tic_export`, or
      `bench_07_rotated_page_photo`.
- [ ] Runtime timeouts/export failures are not introduced.
- [ ] Validators stay strict.
- [ ] Old active owner is demoted, retired, or explicitly kept as shadow only.

## Next Work Slice

The next work slice should not change production behavior immediately.

Recommended:

```text
R2 - Stage 1-3 Shadow Parity Harness
```

R2 should create a small benchmark/desktop harness that emits
`Stage123ParityRecord` from current active code and from the replacement
candidate path. Only after the first side-by-side truth table should production
integration begin.
