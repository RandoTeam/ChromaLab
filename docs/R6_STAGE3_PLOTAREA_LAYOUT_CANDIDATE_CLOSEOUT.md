# R6 Stage 3 PlotArea And Layout Candidate Closeout

Date: 2026-06-07

Status: `R6_STAGE3_LAYOUT_READY_PLOTAREA_REVIEW`

Scope completed: PC-side Stage 3 plotArea and graph-layout candidate using R5
graphPanel candidates as upstream evidence. R6 did not change Android runtime
behavior, validators, graph-count metadata, report gates, chromatographic math,
model policy, or `CalculationEngine`.

## Why R6 Exists

R5 proved that the current shadow graph-discovery candidate can recover the
expected physical graph count across the eight Android validation fixtures. That
is not enough for production: downstream axis scale, calibration, trace, and
peak stages need a usable plotArea and a clear semantic layout class.

R6 measures that next layer before any runtime promotion.

## Implementation

| Area | Result |
|---|---|
| Benchmark runner | `tools/benchmark/run_r6_stage3_plotarea_layout_candidate.py` |
| Records | `benchmark/examples/r6_stage3_plotarea_layout_candidate/` |
| Report | `benchmark/reports/r6_stage3_plotarea_layout_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r6_stage3_plotarea_layout_candidate/contact_sheet_plotarea_layout.png` |
| Detail records | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/` |

The candidate uses deterministic geometry evidence:

- R5 graphPanel candidates;
- selected row/axis projection evidence;
- horizontal axis-run projection;
- vertical axis-run search where stable;
- panel-margin fallback when axis runs are not stable enough;
- semantic layout classification from graph count, image geometry, panel row
  spacing, and panel distribution;
- manual P0 graph/layout annotations for IoU scoring where available.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 3 records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| P0 annotated truth fixtures | 4 |
| Annotated graphPanel mean IoU | 0.739372 |
| Annotated plotArea mean IoU | 0.62146 |
| Stage evidence status | REVIEW for all records |

The layout result is useful, but the stage remains REVIEW because plotArea
localization is not strong enough for autonomous runtime promotion. In
particular, photo/page cases and lower panels need stronger axis/frame/scale
evidence before the candidate can become a production owner.

## Fixture Table

| Fixture | Expected graphs | Detected graphs | Layout class | PlotArea IoU | Decision |
|---|---:|---:|---|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | 2 | `MULTI_PANEL_SEPARATE_AXES` | 0.508 | layout PASS; plotArea REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | `SINGLE_TRACE_SINGLE_AXIS` | - | layout PASS; plotArea REVIEW |
| `bench_03_small_tic_export` | 1 | 1 | `LOW_RES_EXPORT_GRAPH` | - | layout PASS; plotArea REVIEW |
| `bench_04_stacked_xic_resolution` | 4 | 4 | `MULTI_PANEL_SEPARATE_AXES` | 0.856 | layout PASS; plotArea REVIEW |
| `bench_05_tic_plus_ions` | 4 | 4 | `TIC_PLUS_ION_PANELS` | 0.771 | layout PASS; plotArea REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | 2 | `TWO_GRAPH_PAGE` | 0.351 | layout PASS; plotArea REVIEW |
| `bench_07_rotated_page_photo` | 1 | 1 | `ROTATED_PAGE_GRAPH` | - | layout PASS; plotArea REVIEW |
| `white_tiger_ion71` | 1 | 1 | `SINGLE_TRACE_SINGLE_AXIS` | - | layout PASS; plotArea REVIEW |

## Product Meaning

R6 is evidence that the current replacement path can preserve graph-count
semantics and classify the eight validation layouts in shadow mode. It does not
prove axis scale, calibration, trace extraction, peak evidence, report
readiness, or product acceptance.

Phase 9 remains blocked. R6 is a measurement layer for the next analyzer
replacement phase, not Android production acceptance.

## Next Phase

```text
R7 - Stage 4 Axis, Frame, And Scale Evidence Candidate
```

R7 should consume R6 plotArea/layout candidates and add axis/frame/grid/scale
evidence before any Rust or Android runtime promotion.
