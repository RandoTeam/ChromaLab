# R5 Stage 2 Graph Discovery Candidate Closeout

Date: 2026-06-06

Status: `R5_STAGE2_GRAPH_DISCOVERY_COUNT_READY_FOR_STAGE3_SHADOW`

Scope completed: PC-side Stage 2 graph discovery candidate using R4 Rust Stage
1 evidence as upstream image-preparation input. R5 did not change Android
runtime behavior, validators, graph-count metadata, report gates,
chromatographic math, model policy, or `CalculationEngine`.

## Why R5 Exists

Phase 9 remains blocked largely because graph count, graph panel propagation,
and downstream calibration are unreliable on real screenshots/photos. R5 starts
the replacement path for Stage 2 by measuring graph-count discovery on all
eight validation fixtures before any runtime promotion.

## Implementation

| Area | Result |
|---|---|
| Benchmark runner | `tools/benchmark/run_r5_stage2_graph_discovery_candidate.py` |
| Records | `benchmark/examples/r5_stage2_graph_discovery_candidate/` |
| Report | `benchmark/reports/r5_stage2_graph_discovery_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r5_stage2_graph_discovery_candidate/contact_sheet_graph_discovery.png` |

The candidate uses deterministic row-axis projection evidence:

- long horizontal row candidates;
- rejection of top page/title bands;
- rejection of giant dense interior bands;
- close-row collapse for duplicated axis/label bands;
- selected row evidence converted into rough graphPanel candidates;
- accepted and rejected row candidates preserved in JSON records and overlays.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 2 records generated | 8 |
| Graph-count pass | 8/8 |
| Stage evidence status | REVIEW for all records |

The graph-count result is useful, but the stage remains REVIEW because graphPanel
localization is candidate-only and has not yet been scored with IoU, plotArea
truth, or semantic layout rules.

## Fixture Table

| Fixture | Expected graphs | Detected graphs | Selected rows | Decision |
|---|---:|---:|---|---|
| `bench_01_mz71_screenshot_page` | 2 | 2 | 715, 1112 | graph count PASS; panel localization REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | 903 | graph count PASS; panel localization REVIEW |
| `bench_03_small_tic_export` | 1 | 1 | 99 | graph count PASS; panel localization REVIEW |
| `bench_04_stacked_xic_resolution` | 4 | 4 | 226, 496, 779, 1058 | graph count PASS; panel localization REVIEW |
| `bench_05_tic_plus_ions` | 4 | 4 | 362, 506, 644, 783 | graph count PASS; panel localization REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | 2 | 622, 1237 | graph count PASS; panel localization REVIEW |
| `bench_07_rotated_page_photo` | 1 | 1 | 963 | graph count PASS; panel localization REVIEW |
| `white_tiger_ion71` | 1 | 1 | 838 | graph count PASS; panel localization REVIEW |

## Product Meaning

R5 proves that a deterministic Stage 2 graph-count candidate can recover the
expected physical graph count across the current eight Android validation
fixtures in shadow mode. It does not prove final graphPanel bounds, plotArea,
axis detection, calibration, trace extraction, peak evidence, report readiness,
or product acceptance.

Phase 9 remains blocked. R5 is evidence for the next analyzer replacement layer,
not Android production acceptance.

## Next Phase

```text
R6 - Stage 3 PlotArea And Layout Semantics Candidate
```

R6 should consume R5 graphPanel candidates and add plotArea candidate scoring,
layout class evidence, and graphPanel/plotArea visual quality checks before any
runtime promotion.
