# DR-C5 P0 Graph Layout Score

Status: `DR_C5_COMPLETE_CURRENT_OUTPUT_FAILS_P0_GRAPH_LAYOUT_TRUTH`

Date: 2026-06-03

## Purpose

DR-C5 scores the current Phase 9J graph/layout output against the P0 annotation
truth created in DR-C3 and DR-C4. It does not change Android runtime, graph
detection, calibration, trace extraction, peak integration, `CalculationEngine`,
chromatographic math, validators, model routing, or report rendering.

The goal is to establish the current failure baseline before introducing or
testing any new graph/layout method.

## Score Command

```powershell
python tools/benchmark/score_drc5_p0_graph_layout.py
```

Output:

`benchmark/reports/drc5_p0_graph_layout_score/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`CURRENT_OUTPUT_FAILS_P0_GRAPH_LAYOUT_TRUTH`

The current Phase 9J output fails graph-count truth for every P0 case:

| Metric | Count |
| --- | ---: |
| P0 fixtures | 4 |
| P0 deterministic/E2B cases | 8 |
| Graph-count pass | 0 |
| Graph-count fail | 8 |
| Layout-class pass | 0 |
| Layout-class fail or missing | 8 |
| E2B graph-count regressions | 0 |

## Case Results

| Fixture | Mode | Truth graphs | Detected graphs | Truth layout | Predicted layout | Gate | Decision |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | deterministic | 2 | 1 | `MULTI_PANEL_SEPARATE_AXES` | `UNKNOWN_REVIEW` | `BLOCKED` | `FAIL_GRAPH_COUNT` |
| `bench_01_mz71_screenshot_page` | model_enabled | 2 | 1 | `MULTI_PANEL_SEPARATE_AXES` | `UNKNOWN_REVIEW` | `BLOCKED` | `FAIL_GRAPH_COUNT` |
| `bench_04_stacked_xic_resolution` | deterministic | 4 | 1 | `MULTI_PANEL_SEPARATE_AXES` | `DENSE_PEAK_SINGLE_AXIS` | `REVIEW_ONLY` | `FAIL_GRAPH_COUNT` |
| `bench_04_stacked_xic_resolution` | model_enabled | 4 | 1 | `MULTI_PANEL_SEPARATE_AXES` | `DENSE_PEAK_SINGLE_AXIS` | `REVIEW_ONLY` | `FAIL_GRAPH_COUNT` |
| `bench_05_tic_plus_ions` | deterministic | 4 | 1 | `TIC_PLUS_ION_PANELS` | `UNKNOWN_REVIEW` | `BLOCKED` | `FAIL_GRAPH_COUNT` |
| `bench_05_tic_plus_ions` | model_enabled | 4 | 1 | `TIC_PLUS_ION_PANELS` | `UNKNOWN_REVIEW` | `BLOCKED` | `FAIL_GRAPH_COUNT` |
| `bench_06_photo_two_graphs_page` | deterministic | 2 | 1 | `TWO_GRAPH_PAGE` | `DENSE_PEAK_SINGLE_AXIS` | `REVIEW_ONLY` | `FAIL_GRAPH_COUNT` |
| `bench_06_photo_two_graphs_page` | model_enabled | 2 | 1 | `TWO_GRAPH_PAGE` | `DENSE_PEAK_SINGLE_AXIS` | `REVIEW_ONLY` | `FAIL_GRAPH_COUNT` |

## E2B Comparison

E2B did not regress graph count in this P0 score:

| Fixture | Deterministic graphs | E2B graphs | Comparison |
| --- | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | 1 | 1 | `E2B_NEUTRAL` |
| `bench_04_stacked_xic_resolution` | 1 | 1 | `E2B_NEUTRAL` |
| `bench_05_tic_plus_ions` | 1 | 1 | `E2B_NEUTRAL` |
| `bench_06_photo_two_graphs_page` | 1 | 1 | `E2B_NEUTRAL` |

This is not acceptance. It only means E2B did not make graph count worse than
the deterministic result in these P0 cases.

## What This Proves

- The current runtime collapses multi-panel layouts into one graph.
- `bench_04` and `bench_06` are classified as `DENSE_PEAK_SINGLE_AXIS`, but P0
  truth says they are multi-panel/two-graph layouts.
- `bench_01` and `bench_05` do not expose a scoreable layout class.
- Predicted graph bounds are not scoreable for IoU from the normalized
  Phase 9J prediction contracts, so DR-C5 reports
  `NOT_SCOREABLE_MISSING_PREDICTED_BOUNDS`.
- Current output is not ready for graph/layout method acceptance.

## Visual Truth Evidence

| Fixture | Graph layout overlay | Tick/text overlay |
| --- | --- | --- |
| `bench_01_mz71_screenshot_page` | `benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_01_mz71_screenshot_page_overlay.png` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_01_mz71_screenshot_page_tick_text_overlay.png` |
| `bench_04_stacked_xic_resolution` | `benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_04_stacked_xic_resolution_overlay.png` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_04_stacked_xic_resolution_tick_text_overlay.png` |
| `bench_05_tic_plus_ions` | `benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_05_tic_plus_ions_overlay.png` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_05_tic_plus_ions_tick_text_overlay.png` |
| `bench_06_photo_two_graphs_page` | `benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_06_photo_two_graphs_page_overlay.png` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_06_photo_two_graphs_page_tick_text_overlay.png` |

## Guardrails

- No production status was upgraded.
- No validator was weakened.
- No runtime algorithm was modified.
- E2B remains advisory and did not provide graph count truth.
- DR-C3/DR-C4 coordinates remain benchmark truth only, not runtime coordinates.

## Next Slice

Next slice completed:

`DR-C6: P0 Graph Layout Method Comparison Prototype`

Result:

- documented in `docs/DRC6_P0_GRAPH_LAYOUT_METHOD_COMPARISON.md`;
- current Android baseline remains 0/8 graph-count pass on P0 cases;
- best PC prototype reaches 4/4 graph-count pass on P0 fixture images;
- semantic layout class still needs a dedicated classifier before runtime work.
