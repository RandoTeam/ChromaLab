# R7 Stage 4 Axis, Frame, And Scale Evidence Candidate Closeout

Date: 2026-06-07

Status: `R7_STAGE4_AXIS_FRAME_SCALE_EVIDENCE_REVIEW`

Scope completed: PC-side Stage 4 axis/frame/scale evidence candidate using R6
plotArea/layout candidates and partial DR-C4 manual-review tick/text
annotations. R7 did not change Android runtime behavior, validators,
graph-count metadata, report gates, chromatographic math, model policy, or
`CalculationEngine`.

## Why R7 Exists

R6 showed that the replacement path can classify graph layout across all eight
validation fixtures, but plotArea and downstream calibration still need measured
axis/frame/scale evidence. R7 creates that measurement layer before any runtime
promotion.

## Implementation

| Area | Result |
|---|---|
| Benchmark schema | `benchmark/schemas/stage1234-parity-record.schema.json` |
| Benchmark runner | `tools/benchmark/run_r7_stage4_axis_frame_scale_candidate.py` |
| Records | `benchmark/examples/r7_stage4_axis_frame_scale_candidate/` |
| Report | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/contact_sheet_axis_frame_scale.png` |
| Detail records | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/` |

The candidate records:

- x/y axis support along R6 plotArea edges;
- plot frame support;
- grid-line candidates;
- x/y label-band candidates;
- rejected text-role counts from DR-C4 annotations where available;
- manual-review scale fits where DR-C4 anchors exist;
- axis alignment error against manual-review anchor positions.

DR-C4 anchors are used only for scoring candidate evidence. They are not
runtime calibration evidence.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 4 records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| P0 annotated truth fixtures | 4 |
| Manual-review scale graph count | 12 |
| Mean x-axis support | 0.519346 |
| Mean y-axis support | 0.45788 |
| Annotated x-axis mean pixel error | 21.482143 |
| Annotated y-axis mean pixel error | 110.5 |
| Stage evidence status | REVIEW for all records |

The result is useful but not production-ready. Manual-review scale fits are
available for 12 annotated P0 graphs, but axis alignment errors show that
bench_01 and bench_06 still need stronger plotArea/axis geometry before
automatic calibration can be trusted.

## Fixture Table

| Fixture | Graphs | Layout class | Manual-review scale graphs | X support | Y support | X error | Y error | Decision |
|---|---:|---|---:|---:|---:|---:|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 0.304077 | 0.089208 | 16.928572 | 158.0 | axis/frame/scale REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0.615707 | 1.0 | - | - | unannotated scale REVIEW |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0.449367 | 0.147917 | - | - | unannotated scale REVIEW |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 0.692673 | 0.188661 | 1.75 | 15.75 | axis/frame/scale REVIEW |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 0.389771 | 0.10458 | 0.75 | 25.75 | axis/frame/scale REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 0.697805 | 0.64773 | 66.5 | 242.5 | axis/frame/scale REVIEW |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0.390451 | 0.484943 | - | - | unannotated scale REVIEW |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0.61492 | 1.0 | - | - | unannotated scale REVIEW |

## Product Meaning

R7 is evidence for the next replacement layer. It does not prove runtime OCR,
calibration, trace extraction, peak evidence, report readiness, or product
acceptance.

Phase 9 remains blocked. R7 should guide calibration parity work, not bypass
it.

## Next Phase

```text
R8 - Stage 5 Calibration Strategy Parity Candidate
```

R8 should consume R7 axis/frame/scale evidence and compare calibration strategy
candidates without changing Android runtime or `CalculationEngine`.

Update: R8 is complete in
`docs/R8_STAGE5_CALIBRATION_STRATEGY_PARITY_CLOSEOUT.md`. The next replacement
step is R9 automatic OCR anchor evidence, still shadow-only.
