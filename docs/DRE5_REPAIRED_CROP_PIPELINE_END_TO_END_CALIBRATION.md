# DR-E5 Repaired Crop Pipeline End-To-End Calibration Benchmark

Status: `DR_E5_COMPLETE_REPAIRED_CROP_PIPELINE_IMPROVES_CALIBRATION_NOT_ACCEPTANCE_READY`

Date: 2026-06-04

## Purpose

DR-E5 measures whether the DR-E4 repaired X label-band crop evidence improves
end-to-end graph calibration decisions when passed through the existing safe OCR
anchor builder and DR-E2 robust-fit arbitration. This is a PC-side benchmark
slice only. It does not change Android runtime, production graph detection,
calibration, trace extraction, peak integration, `CalculationEngine`,
chromatographic math, validators, model routing, or report rendering.

## Inputs

- `benchmark/reports/dre1_axis_scale_candidate_builder/summary.json`
- `benchmark/reports/dre2_robust_axis_scale_fit/summary.json`
- `benchmark/reports/dre4_label_band_crop_coverage_repair/summary.json`
- `benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json`

## Command

```powershell
python tools/benchmark/run_dre5_repaired_crop_calibration_pipeline.py
```

Output:

`benchmark/reports/dre5_repaired_crop_calibration_pipeline/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`REPAIRED_CROP_PIPELINE_IMPROVES_CALIBRATION_NOT_ACCEPTANCE_READY`

## Results

| Metric | DR-E2 | DR-E5 |
| --- | ---: | ---: |
| Usable axes | 18 | 22 |
| Usable graphs | 7 | 10 |
| Valid graphs | 7 | 8 |
| Review graphs | 0 | 2 |
| Partial graphs | 4 | 2 |
| Invalid graphs | 1 | 0 |
| Graph regressions | 0 | 0 |

Additional repaired anchors: `40`.

## Improved Graphs

| Fixture | Graph | DR-E2 | DR-E5 | Improvement |
| --- | --- | --- | --- | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `PARTIAL` | `VALID` | repaired X axis became `VALID` with 7 anchors |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `PARTIAL` | `REVIEW` | repaired X axis became `VALID`; Y remains two-anchor `REVIEW` |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `PARTIAL` | repaired X axis became `VALID`; Y remains invalid |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `PARTIAL` | `REVIEW` | repaired X axis became `REVIEW`; Y remains `REVIEW` |

## Remaining Partial Graphs

| Fixture | Graph | Remaining blocker | Evidence status |
| --- | --- | --- | --- |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | Y axis has `INSUFFICIENT_SCALE_ANCHORS` | X scale is now valid; Y has only 1 usable safe anchor |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | X axis has `INSUFFICIENT_SCALE_ANCHORS` | repaired crop coverage exists, but OCR still recovered 0 safe X anchors |

## What This Proves

- The repaired label-band crop plan is not just visually better; it improves
  graph-level calibration decisions under the existing robust-fit gate.
- The change is regression-safe in this benchmark slice: no graph moved to a
  worse calibration decision.
- `bench_04_graph_3` is fully rescued by repaired X label-band coverage.
- `bench_05_graph_2` and `bench_05_graph_4` move to review-grade calibration
  candidates.
- `bench_05_graph_3` now has a valid X scale, so its blocker is isolated to Y
  axis safe-label recovery.
- `bench_06_graph_2` remains a separate OCR visibility/preprocessing problem:
  crop coverage alone is insufficient.

## Product Rule

DR-E5 does not fabricate labels, pixel coordinates, anchors, or calibration
coefficients. Added labels are accepted only when they pass the same safe OCR
and robust-fit contracts used by DR-E1 and DR-E2.

## Next Slice

Recommended next slice:

`DR-E6: Remaining Axis OCR Recovery For Y-Axis And Photo Graphs`

Goal:

- target `bench_05_graph_3` Y-axis safe-label recovery;
- target `bench_06_graph_2` X-axis OCR visibility/preprocessing;
- keep the repaired crop pipeline as the current best PC-side calibration
  candidate;
- do not proceed to Android runtime parity until these remaining blockers are
  either recovered or precisely classified.
