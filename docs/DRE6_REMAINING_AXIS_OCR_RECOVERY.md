# DR-E6 Remaining Axis OCR Recovery Benchmark

Status: `DR_E6_COMPLETE_ALL_TARGET_AXES_USABLE_NOT_RUNTIME_READY`

Date: 2026-06-04

## Purpose

DR-E6 targets the two partial calibration blockers that remained after DR-E5:

- `bench_05_tic_plus_ions` / `bench_05_graph_3` Y axis;
- `bench_06_photo_two_graphs_page` / `bench_06_graph_2` X axis.

This is a PC-side benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Inputs

- `benchmark/reports/dre1_axis_scale_candidate_builder/summary.json`
- `benchmark/reports/dre5_repaired_crop_calibration_pipeline/summary.json`
- `benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json`
- `benchmark/reports/dre4_label_band_crop_coverage_repair/summary.json`
- `benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json`
- `benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json`

## Command

```powershell
python tools/benchmark/run_dre6_remaining_axis_ocr_recovery.py
```

Output:

`benchmark/reports/dre6_remaining_axis_ocr_recovery/`

Generated files:

- `summary.json`
- `summary.md`
- `overlays/bench_06_photo_two_graphs_page_dre6_aggressive_x_ocr_crops.png`

## Verdict

`REMAINING_AXIS_OCR_RECOVERY_ALL_TARGET_AXES_USABLE_NOT_RUNTIME_READY`

## Results

| Metric | DR-E5 | DR-E6 |
| --- | ---: | ---: |
| Usable axes | 22 | 24 |
| Usable graphs | 10 | 12 |
| Valid graphs | 8 | 9 |
| Review graphs | 2 | 3 |
| Partial graphs | 2 | 0 |
| Invalid graphs | 0 | 0 |
| Graph regressions | 0 | 0 |

Additional target fallback anchors: `12`.

Aggressive sweep:

| Metric | Value |
| --- | ---: |
| Crops | 22 |
| OCR rows | 431 |
| Safe target rows | 131 |

## Target Axis Recovery

| Fixture | Graph | Axis | Truth labels | Recovered fallback anchors | Remaining missing | Recovered labels |
| --- | --- | --- | ---: | ---: | ---: | --- |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | Y | 2 | 2 | 0 | `0`, `10 000` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | X | 11 | 11 | 0 | `5.00`, `10.00`, `15.00`, `20.00`, `25.00`, `30.00`, `35.00`, `40.00`, `45.00`, `50.00`, `55.00` |

## Graph Calibration Changes

| Fixture | Graph | DR-E5 | DR-E6 | Change |
| --- | --- | --- | --- | --- |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `PARTIAL` | `REVIEW` | Y axis recovered to review-grade two-anchor calibration |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `PARTIAL` | `VALID` | X axis recovered to valid 11-anchor calibration |

All other graph decisions stayed unchanged.

## What This Proves

- A single global OCR preprocessing choice is too weak for this corpus.
- Per-axis OCR variant fallback recovers `bench_05_graph_3` Y-axis evidence that
  DR-E5 lost by using the global best OCR variant.
- Deep photo X-label-band crops with aggressive preprocessing recover all
  `bench_06_graph_2` X-axis labels.
- All 12 PC-side graph calibration cases are now `VALID` or `REVIEW` under the
  existing robust-fit gate.
- This is still not Android acceptance. It is benchmark evidence for the next
  runtime candidate.

## Product Rule

DR-E6 does not fabricate labels, pixel coordinates, anchors, or calibration
coefficients. Added labels are accepted only when they pass geometry-owned OCR
matching, safe text-role gating, numeric parsing, monotonicity, and robust-fit
calibration.

## Next Slice

Recommended next slice:

`DR-E7: Axis OCR Runtime Candidate Contract And Android Parity Plan`

Goal:

- convert the proven PC-side rules into a minimal runtime candidate contract;
- specify per-axis OCR variant fallback without changing scientific math;
- specify deep photo X-band crop generation;
- define Android parity checks before any production integration;
- keep this as a contract/planning slice unless explicitly approved for runtime
  implementation.
