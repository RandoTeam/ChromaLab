# DR-E3 Missing Anchor Recovery And Label-Band Coverage Benchmark

Status: `DR_E3_COMPLETE_MISSING_ANCHOR_RECOVERY_BLOCKED_BY_CROP_COVERAGE`

Date: 2026-06-04

## Purpose

DR-E3 audits the missing scale anchors that remain after DR-E2 robust fitting.
It classifies each missing anchor by root cause:

- crop coverage missing;
- OCR detector empty in an owned crop;
- OCR detected text near the label but did not match;
- OCR role/safety rejection;
- anchor selection gap.

This is a PC-side research and benchmark slice only. It does not change Android
runtime, production graph detection, calibration, trace extraction, peak
integration, `CalculationEngine`, chromatographic math, validators, model
routing, or report rendering.

## Inputs

- `benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json`
- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/summary.json`
- `benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json`
- `benchmark/reports/dre1_axis_scale_candidate_builder/summary.json`
- `benchmark/reports/dre2_robust_axis_scale_fit/summary.json`

OCR variant:

`rapidocr_rgb_x3_p2_v1`

## Command

```powershell
python tools/benchmark/run_dre3_missing_anchor_coverage_benchmark.py
```

Output:

`benchmark/reports/dre3_missing_anchor_coverage/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`MISSING_ANCHOR_RECOVERY_BLOCKED_BY_CROP_COVERAGE`

## Results

| Metric | Value |
| --- | ---: |
| Target non-valid axes | 8 |
| Missing anchors inspected | 52 |
| Crop coverage missing | 18 |
| OCR empty/missing in crop | 33 |
| Role/safety rejected | 1 |

Root cause counts:

| Root cause | Count |
| --- | ---: |
| `CROP_COVERAGE_MISSING` | 18 |
| `OCR_DETECTION_EMPTY_FOR_CROP` | 33 |
| `OCR_ROLE_OR_SAFETY_REJECTED` | 1 |

## Axis Root Cause Summary

| Fixture | Graph | Axis | Truth labels | Safe anchors | Missing | Primary root cause | Recommended fix |
| --- | --- | --- | ---: | ---: | ---: | --- | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | X | 7 | 0 | 7 | `CROP_COVERAGE_MISSING` | repair axis label-band crop coverage |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | X | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | Y | 2 | 2 | 0 | `NO_MISSING_ANCHORS` | none |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | X | 11 | 0 | 11 | `CROP_COVERAGE_MISSING` | repair axis label-band crop coverage |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | Y | 2 | 1 | 1 | `OCR_ROLE_OR_SAFETY_REJECTED` | repair text role/safety classifier for owned labels |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | X | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | Y | 2 | 2 | 0 | `NO_MISSING_ANCHORS` | none |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | X | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |

## What This Proves

- Remaining invalid axes are not primarily robust-fit failures.
- Some axes have label-band coverage gaps: OCR cannot recover labels that are
  outside the owned crop.
- Some axes have owned crops but OCR returns no usable detections in dense small
  X-label bands.
- The next improvement must target crop coverage and dense label-band OCR
  subdivision before more calibration fit strategies.

## Product Rule

Missing anchors remain missing evidence. The benchmark does not fabricate
labels, coordinates, or calibration coefficients.

## Next Slice

Recommended next slice:

`DR-E4: Label-Band Crop Coverage Repair Prototype`

Goal:

- repair inferred X label-band coverage for stacked/TIC+ion panels;
- add crop subdivision candidates for dense X-label bands;
- keep the safe-anchor rules from DR-D6/DR-E1;
- rerun DR-E1/DR-E2 style scale candidate scoring after coverage repair.
