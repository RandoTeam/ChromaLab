# DR-B3 Benchmark Scoring And Fixture Truth Gaps

Status: `DR_B3_COMPLETE`

Date: 2026-06-03

## Purpose

DR-B3 scores the Phase 9J benchmark records created in DR-B2 and turns them
into a compact product/engineering truth table. It does not change Android
runtime, graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

This slice answers one question:

What does the current pipeline prove, and what truth data is still missing
before the next method-comparison wave?

## Scoring Command

```powershell
python tools/benchmark/score_phase9j_benchmarks.py
```

Output:

`benchmark/reports/phase9j_truth_audit_score/`

Generated files:

- `summary.json`
- `summary.md`
- `truth-gaps.json`
- `truth-gaps.md`

## Score Summary

From `benchmark/reports/phase9j_truth_audit_score/summary.json`:

| Metric | Count |
| --- | ---: |
| Records | 16 |
| Fixtures | 8 |
| REVIEW decisions | 12 |
| BLOCKED decisions | 4 |
| RELEASE_READY decisions | 0 |
| Evidence package PASS | 16 |

Stage status counts:

| Stage | PASS | REVIEW | FAIL |
| --- | ---: | ---: | ---: |
| Graph panel | 8 | 8 | 0 |
| Calibration | 0 | 14 | 2 |
| Trace | 0 | 12 | 4 |
| Peaks | 0 | 12 | 4 |
| Report claims | 0 | 12 | 4 |
| Evidence package | 16 | 0 | 0 |

## Fixture Priority

| Priority | Meaning | Fixture/mode count |
| --- | --- | ---: |
| P0 | Blocks method comparison or has critical graph-count/runtime issue | 8 |
| P1 | Review-grade case needing stronger truth before release metrics | 7 |
| P2 | Lower-priority review case | 1 |

P0 fixtures:

- `bench_01_mz71_screenshot_page`
- `bench_04_stacked_xic_resolution`
- `bench_05_tic_plus_ions`
- `bench_06_photo_two_graphs_page`

P1 fixtures:

- `bench_02_mz92_belyi_tigr`
- `bench_03_small_tic_export`
- `bench_07_rotated_page_photo` E2B mode
- `white_tiger_ion71`

## Truth Gaps

The current Phase 9J records are useful as real diagnostic cases, but they do
not yet contain enough ground truth to prove release-quality numeric accuracy.

Missing truth fields counted across cases:

| Truth field | Cases needing it |
| --- | ---: |
| `input_image_hash` | 16 |
| `tick_or_grid_positions` | 16 |
| `numeric_label_boxes` | 16 |
| `calibration_anchors` | 16 |
| `trace_reference` | 16 |
| `peak_reference_metrics` | 16 |
| `report_claim_expectations` | 16 |
| `axis_endpoints` | 2 |

## Product Interpretation

DR-B3 confirms the current state without reclassifying it:

- all 16 runs have evidence packages;
- no run is release-ready;
- four runs remain blocked;
- twelve runs are review-only;
- graph-count/layout truth is the highest-risk gap for P0 fixtures;
- calibration/trace/peak truth is missing across all current real fixtures.

## Next Slice

Recommended next slice:

`DR-C1: Graph Layout / PlotArea / Axis Element Method Research Inputs`

Goal:

- use the P0/P1 fixture priorities from DR-B3;
- define annotation requirements for graphPanel, plotArea, axis endpoints, tick
  or grid positions, and numeric label boxes;
- compare graph/layout methods only after those truth fields are available or
  explicitly marked diagnostic-only.
