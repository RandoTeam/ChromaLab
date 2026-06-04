# DR-E4 Label-Band Crop Coverage Repair Prototype

Status: `DR_E4_COMPLETE_LABEL_BAND_COVERAGE_REPAIR_RECOVERS_SCALE_CANDIDATES`

Date: 2026-06-04

## Purpose

DR-E4 tests whether the DR-E3 missing X-axis anchors are caused by too-tight or
too-coarse label-band crops rather than by calibration math. This is a PC-side
benchmark prototype only. It does not change Android runtime, production graph
detection, calibration, trace extraction, peak integration, `CalculationEngine`,
chromatographic math, validators, model routing, or report rendering.

## Inputs

- `benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json`
- `benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json`
- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/summary.json`
- `benchmark/reports/dre1_axis_scale_candidate_builder/summary.json`
- `benchmark/reports/dre2_robust_axis_scale_fit/summary.json`
- `benchmark/reports/dre3_missing_anchor_coverage/summary.json`

OCR method:

`rapidocr_repaired_x_label_band_rgb_x3_p2_v1`

## Command

```powershell
python tools/benchmark/run_dre4_label_band_crop_coverage_repair.py
```

Output:

`benchmark/reports/dre4_label_band_crop_coverage_repair/`

Generated files:

- `summary.json`
- `summary.md`
- `overlays/*_dre4_repaired_x_label_bands.png`

## Verdict

`LABEL_BAND_COVERAGE_REPAIR_RECOVERS_SCALE_CANDIDATES`

## Results

| Metric | Value |
| --- | ---: |
| Target X axes | 5 |
| Repaired crops | 35 |
| Recovered safe X anchors | 40 |
| Axes with anchor improvement | 4 |
| Invalid-to-usable axes | 4 |

## Axis Recovery Summary

| Fixture | Graph | Truth X labels | Baseline safe | Repaired safe | Recovered | Remaining missing |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | 7 | 0 | 7 | 7 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | 11 | 0 | 11 | 11 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | 11 | 0 | 11 | 11 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | 11 | 0 | 11 | 11 | 0 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | 11 | 0 | 0 | 0 | 11 |

## Robust Fit After Repair

| Fixture | Graph | Baseline X | Repaired X | Repaired anchors | Failure reason |
| --- | --- | --- | --- | ---: | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `INVALID` | `VALID` | 7 |  |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `INVALID` | `VALID` | 9 |  |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `VALID` | 9 |  |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `INVALID` | `REVIEW` | 3 |  |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | `INVALID` | 0 | `INSUFFICIENT_SCALE_ANCHORS` |

## What This Proves

- The DR-E3 crop coverage diagnosis was correct for `bench_04_graph_3` and
  `bench_05_graph_3`.
- Dense overlapping X-label tiles also recover safe labels for
  `bench_05_graph_2` and `bench_05_graph_4`, which DR-E3 classified as OCR-empty
  crops.
- The repaired crops are not enough for `bench_06_graph_2`; labels are covered
  after repair, but OCR still returns no safe anchors. That fixture needs a
  separate OCR visibility/preprocessing or image quality recovery slice.
- The calibration fit remains evidence-gated: OCR labels must become safe
  anchors and pass robust fitting before they can be used as calibration
  evidence.

## Product Rule

This benchmark does not fabricate labels, pixel coordinates, anchors, or
calibration coefficients. It only measures whether alternate deterministic crop
coverage produces safe OCR evidence.

## Next Slice

Completed next slice:

`DR-E5: Repaired Crop Pipeline End-To-End Calibration Benchmark`

Result:

- documented in `docs/DRE5_REPAIRED_CROP_PIPELINE_END_TO_END_CALIBRATION.md`;
- usable axes improved from 18 to 22;
- usable graphs improved from 7 to 10;
- graph regressions: 0;
- remaining partial graphs are isolated to `bench_05_graph_3` Y-axis recovery
  and `bench_06_graph_2` X-axis OCR recovery.

Original goal:

- rerun the scale-candidate and robust-fit chain with repaired crop plans as a
  first-class input;
- preserve the safe text-role and forbidden-number gates;
- quantify whether repaired label-band crops convert graph-level Android
  calibration blockers into `VALID` or `REVIEW` evidence candidates;
- keep `bench_06_graph_2` as a separate unresolved OCR recovery case rather
  than hiding it behind the successful `bench_04` and `bench_05` repairs.
