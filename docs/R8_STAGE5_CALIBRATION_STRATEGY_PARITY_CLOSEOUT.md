# R8 Stage 5 Calibration Strategy Parity Candidate Closeout

Date: 2026-06-07

Status: `R8_STAGE5_CALIBRATION_STRATEGY_PARITY_REVIEW`

Scope completed: PC-side Stage 5 calibration strategy parity candidate using R7
axis/frame/scale evidence and partial DR-C4 manual-review tick/text
annotations. R8 did not change Android runtime behavior, validators,
graph-count metadata, report gates, chromatographic math, model policy, or
`CalculationEngine`.

## Why R8 Exists

R7 measured axis/frame/scale evidence and showed that manual-review anchors
exist for 12 P0 graphs. R8 measures how calibration strategy arbitration should
behave when those anchors are available, and it records which strategy is
selected or rejected.

This is still benchmark scoring. Manual-review anchors are not production
runtime calibration evidence.

## Implementation

| Area | Result |
|---|---|
| Benchmark schema | `benchmark/schemas/stage12345-parity-record.schema.json` |
| Benchmark runner | `tools/benchmark/run_r8_stage5_calibration_strategy_parity_candidate.py` |
| Records | `benchmark/examples/r8_stage5_calibration_strategy_parity_candidate/` |
| Report | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/contact_sheet_calibration_strategy.png` |
| Detail records | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/` |

The candidate compares:

- `MANUAL_REVIEW_ALL_ANCHOR_FIT`;
- `TWO_ANCHOR_ENDPOINT_FIT`;
- `R7_AXIS_FRAME_GEOMETRY_ONLY`;
- `FORBIDDEN_TEXT_REJECTION_AUDIT`.

Rules enforced:

- all-anchor REVIEW fit is selected when manual-review DRC4 anchors are present
  and monotonic;
- two-anchor endpoint fit remains a REVIEW fallback candidate;
- geometry-only candidates are rejected because they have no numeric values;
- title/ion/m/z text-role evidence remains rejected as calibration input.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 5 records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Annotated truth fixtures | 4 |
| Selected calibration graph count | 12 |
| Annotated anchor count | 194 |
| Selected X mean RMSE | 0.000287 |
| Selected Y mean RMSE | 0.231479 |
| Stage evidence status | REVIEW for all records |

The result proves that calibration strategy arbitration can select coherent
manual-review scoring fits and reject geometry-only/no-numeric candidates. It
does not prove automatic OCR label extraction, runtime anchor generation,
Android calibration success, trace extraction, peak evidence, report readiness,
or product acceptance.

## Fixture Table

| Fixture | Graphs | Layout class | Selected graphs | Anchors | X RMSE | Y RMSE | Decision |
|---|---:|---|---:|---:|---:|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 30 | 0.0 | 0.002577 | calibration strategy REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | - | - | no DRC4 scale truth |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | - | - | no DRC4 scale truth |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 56 | 0.000863 | 0.0 | calibration strategy REVIEW |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 56 | 0.000056 | 0.0 | calibration strategy REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 52 | 0.000229 | 0.923339 | calibration strategy REVIEW |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | - | - | no DRC4 scale truth |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | - | - | no DRC4 scale truth |

## Product Meaning

R8 is evidence for calibration strategy design and regression tests. It is not
Android production acceptance and does not close Phase 9.

Phase 9 remains blocked until automatic runtime evidence produces comparable
anchors without manual annotation truth.

## Next Phase

```text
R9 - Stage 6 Automatic OCR Anchor Candidate
```

R9 should consume R8 strategy tables and measure automatic OCR/label-band anchor
generation against DR-C4 truth before any runtime promotion.
