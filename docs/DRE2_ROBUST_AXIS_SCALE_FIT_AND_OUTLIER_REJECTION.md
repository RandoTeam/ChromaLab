# DR-E2 Robust Axis Scale Fit And Outlier Rejection Benchmark

Status: `DR_E2_COMPLETE_ROBUST_AXIS_SCALE_FIT_IMPROVES_OUTLIERS_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-E2 compares robust axis-scale fitting strategies on the safe OCR anchors from
DR-E1. The goal is to separate recoverable OCR outliers from true missing-label
failures.

This is a PC-side research and benchmark slice only. It does not change Android
runtime, production graph detection, calibration, trace extraction, peak
integration, `CalculationEngine`, chromatographic math, validators, model
routing, or report rendering.

## Input

`benchmark/reports/dre1_axis_scale_candidate_builder/summary.json`

OCR variant:

`rapidocr_rgb_x3_p2_v1`

## Command

```powershell
python tools/benchmark/run_dre2_robust_axis_scale_fit_benchmark.py
```

Output:

`benchmark/reports/dre2_robust_axis_scale_fit/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`ROBUST_AXIS_SCALE_FIT_IMPROVES_OUTLIERS_NOT_ACCEPTANCE_READY`

## Strategy Set

Compared strategies:

- `ALL_ANCHOR_LINEAR_FIT`
- `RANSAC_RESIDUAL_TRIMMED_FIT`
- `MONOTONIC_SUBSEQUENCE_FIT`
- `TWO_ANCHOR_REVIEW_FALLBACK`

Selected strategy in this run:

`RANSAC_RESIDUAL_TRIMMED_FIT` for all 24 axis candidates.

## Results

| Metric | DR-E1 | DR-E2 |
| --- | ---: | ---: |
| Usable axes | 15 | 18 |
| Valid axes | 13 | 16 |
| Review axes | 2 | 2 |
| Invalid axes | 9 | 6 |
| Valid graphs | 5 | 7 |
| Partial graphs | 5 | 4 |
| Invalid graphs | 2 | 1 |

## Graph Summary

| Fixture | Graph | DR-E1 | DR-E2 | X status | Y status | Result |
| --- | --- | --- | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | `VALID` | `VALID` | `VALID` | stable |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `PARTIAL` | `VALID` | `VALID` | `VALID` | recovered high-residual X |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | `VALID` | `VALID` | `VALID` | stable |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | `VALID` | `VALID` | `VALID` | stable |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `PARTIAL` | `PARTIAL` | `INVALID` | `VALID` | missing X anchors |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | `VALID` | `VALID` | `VALID` | stable |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | `VALID` | `VALID` | `VALID` | stable |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `PARTIAL` | `PARTIAL` | `INVALID` | `REVIEW` | missing X anchors |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `INVALID` | `INVALID` | `INVALID` | missing anchors |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `PARTIAL` | `PARTIAL` | `INVALID` | `REVIEW` | missing X anchors |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `PARTIAL` | `VALID` | `VALID` | `VALID` | recovered non-monotonic Y |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | `PARTIAL` | `INVALID` | `VALID` | recovered Y only |

## What Improved

- Robust residual trimming recovered the high-residual X-axis case in
  `bench_01_graph_2`.
- Robust trimming recovered non-monotonic Y evidence for
  `bench_06_graph_1`.
- `bench_06_graph_2` improved from invalid to partial because Y could be
  recovered.
- Missing anchors remained explicit; no new tick labels were fabricated.

## What Still Fails

- `bench_04_graph_3` has no usable X anchors.
- `bench_05_graph_2`, `bench_05_graph_3`, and `bench_05_graph_4` still lack
  enough safe X anchors.
- `bench_05_graph_3` remains invalid because both axes are insufficient after
  robust fitting.

## Product Rule

RANSAC/outlier rejection may choose a smaller monotonic inlier set, but it must
export rejected anchors and residuals. Two-anchor fits remain review-only and
must never become release-ready without stronger geometry evidence.

## Next Slice

Recommended next slice:

`DR-E3: Missing Anchor Recovery And Label-Band Coverage Benchmark`

Goal:

- target the remaining X-anchor gaps in stacked/TIC+ion panels;
- determine whether the blocker is crop coverage, OCR detection, or label
  geometry;
- keep robust fit evidence from DR-E2 as the calibration selector baseline.
