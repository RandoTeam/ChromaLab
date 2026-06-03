# DR-E1 Axis Scale Candidate Builder From Safe Owned OCR

Status: `DR_E1_COMPLETE_AXIS_SCALE_CANDIDATES_PARTIAL_SAFE_EVIDENCE_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-E1 starts the calibration benchmark wave. It consumes the best safe OCR
evidence from DR-D6 and builds deterministic X/Y axis scale candidates.

This is a PC-side research and benchmark slice only. It does not change Android
runtime, production graph detection, calibration, trace extraction, peak
integration, `CalculationEngine`, chromatographic math, validators, model
routing, or report rendering.

## Inputs

Truth:

`benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json`

OCR evidence:

`benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json`

OCR variant used:

`rapidocr_rgb_x3_p2_v1`

## Command

```powershell
python tools/benchmark/run_dre1_axis_scale_candidate_builder.py
```

Output:

`benchmark/reports/dre1_axis_scale_candidate_builder/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`AXIS_SCALE_CANDIDATES_PARTIAL_SAFE_EVIDENCE_NOT_ACCEPTANCE_READY`

## Results

| Metric | Value |
| --- | ---: |
| Graphs | 12 |
| Axis candidates | 24 |
| Safe anchors | 123 |
| Missing/rejected anchors | 71 |
| Valid axes | 13 |
| Review axes | 2 |
| Invalid axes | 9 |
| Valid graphs | 5 |
| Partial graphs | 5 |
| Invalid graphs | 2 |

## Graph Candidate Summary

| Fixture | Graph | X status | X anchors | X RMSE | Y status | Y anchors | Y RMSE | Decision |
| --- | --- | --- | ---: | ---: | --- | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | 7 | 3.5466 | `VALID` | 7 | 0.3797 | `VALID` |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `INVALID` | 6 | 103.7997 | `VALID` | 6 | 0.5994 | `PARTIAL` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | 7 | 0.4798 | `VALID` | 6 | 0.2378 | `VALID` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | 7 | 0.5613 | `VALID` | 7 | 0.2352 | `VALID` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `INVALID` | 0 | - | `VALID` | 6 | 0.2071 | `PARTIAL` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | 7 | 0.4954 | `VALID` | 5 | 0.1894 | `VALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | 6 | 0.5495 | `VALID` | 5 | 0.1997 | `VALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `INVALID` | 0 | - | `REVIEW` | 2 | 0.0 | `PARTIAL` |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | 0 | - | `INVALID` | 1 | - | `INVALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `INVALID` | 0 | - | `REVIEW` | 2 | 0.0 | `PARTIAL` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `VALID` | 10 | 0.7423 | `INVALID` | 14 | - | `PARTIAL` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | 0 | - | `INVALID` | 12 | - | `INVALID` |

## Failure Reasons

Observed blocker reasons:

- `INSUFFICIENT_SCALE_ANCHORS`
- `SCALE_FIT_HIGH_RESIDUAL`
- `LABEL_SEQUENCE_NON_MONOTONIC`

These are useful because they replace generic tick/OCR failure with explicit
calibration evidence state.

## What Improved

- Safe OCR labels now become explicit scale anchors.
- Missing OCR labels stay explicit missing anchors.
- Candidate fits export slope, intercept, residual table, RMSE, max residual,
  monotonic direction, and selected/rejected evidence.
- Several P0 graph axes are already usable without making OCR a numeric
  authority.

## What Still Fails

- `bench_05_tic_plus_ions` still lacks enough safe X anchors on stacked ion
  panels.
- `bench_06_photo_two_graphs_page` has non-monotonic Y OCR label evidence.
- One `bench_01` X candidate has high residual, likely from an OCR/value
  outlier that needs robust candidate scoring.

## Product Rule

OCR values are evidence, not authority. A scale candidate is usable only when
the numeric text is matched to geometry-owned tick-label evidence and passes
monotonicity/residual checks.

## Next Slice

Next slice completed:

`DR-E2: Robust Axis Scale Fit And Outlier Rejection Benchmark`

Result:

- documented in `docs/DRE2_ROBUST_AXIS_SCALE_FIT_AND_OUTLIER_REJECTION.md`;
- compared all-anchor, residual-trimmed, monotonic-subsequence, and two-anchor
  fallback strategies;
- usable axes improved from `15` to `18`;
- valid graphs improved from `5` to `7`, but missing X anchors still block
  stacked/TIC+ion panels.
