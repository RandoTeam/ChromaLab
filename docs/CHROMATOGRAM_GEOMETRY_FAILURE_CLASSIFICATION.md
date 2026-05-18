# Chromatogram Geometry Failure Classification

Status: runtime geometry follow-up after `b7467e0`.
Date: 2026-05-18.

This document records the current failure mode for the three bench cases that still
fail the full `ChromatogramBenchFixtureTest`. These are not `CalculationEngine`
rewrites; they identify upstream image-to-signal weaknesses that the geometry
pipeline must continue to remove.

## Summary Table

| Fixture | Expected | Actual | Blocking Origin | Classification |
| --- | --- | --- | --- | --- |
| `bench_03_small_tic_export` | at least 5 visible peaks; labeled apexes near 3.244, 3.890, 4.647, 5.610, 8.560 | 4 peaks after guarded completeness; peak sanity blocks | tick localization/OCR anchors plus centerline coverage | Low-resolution clean TIC has valid plot area and usable curve, but deterministic tick geometry finds only 1 X and 1 Y tick, OCR is unavailable, manual calibration allows signal conversion, then peak sanity reports `min_peak_count_not_met` and `expected_apex_missing`. |
| `bench_04_stacked_xic_resolution` graph 3 | sparse review should cover 4 peaks | 20 reviewed/default peaks | curve mask extraction and centerline selection | Graph/plot/signal conversion succeed, but sparse trace extraction admits too many fragmented/noisy components as peaks. This is not a calibration failure; it is over-detection after curve extraction. |
| `bench_08_mz71_duplicate_candidate` | guarded completeness path: base 5 -> tuned 9 | default path returns 19 peaks; guarded tuning not applied | curve mask extraction / peak candidate quality gate | GraphPanel and plotArea are detected and the X tick geometry is rich, but Y tick geometry is weak and the signal contains many accepted candidates. The guarded under-detection gate does not activate because the base table is already over-detected. |

## Stage Notes

### `bench_03_small_tic_export`

- GraphPanel detection: full-image fallback accepted; acceptable for this compact screenshot.
- PlotArea detection: detected `13,1 367x100`; warning `plot_area.signal_extends_above_detected_y_axis`.
- Axis detection: axes/origin detected with high confidence.
- Tick localization: weak; only one X tick and one Y tick are found.
- OCR anchor pairing: unavailable in the default desktop environment; no reliable automatic tick-value anchors.
- Calibration fit: manual fixture calibration makes signal conversion possible, but automatic calibration remains blocked.
- Curve extraction: usable, but coverage is marginal for the labeled low-resolution peaks.
- Peak result: 4 peaks instead of at least 5; failure is upstream tick/curve/centerline evidence, not integration math.

### `bench_04_stacked_xic_resolution`

- GraphPanel detection: four panels are split.
- PlotArea detection: all four plot areas are detected.
- Axis/tick localization: deterministic tick evidence remains sparse on several panels.
- Calibration fit: manual fixture calibration permits signal conversion.
- Curve extraction: graph 3 has sparse coverage but produces a dense peak table.
- Peak result: graph 3 returns 20 reviewed peaks where the sparse-quality contract expects 4. This points to fragmented trace/noise being accepted as curve/peak evidence.

### `bench_08_mz71_duplicate_candidate`

- GraphPanel detection: single Ion 71 panel detected.
- PlotArea detection: detected with top-signal warning.
- Tick localization: X ticks are abundant; Y tick localization is weak.
- Calibration fit: manual fixture calibration permits signal conversion.
- Curve extraction: usable but includes artifact risk from top-band text / frame context.
- Peak result: 19 default peaks. The expected guarded-completeness path is not reached because the base detector is over-detecting, not under-detecting.

## Immediate Engineering Direction

The current failures are all upstream of final report quality:

- `bench_03`: improve deterministic tick localization/OCR pairing and low-resolution centerline recovery.
- `bench_04`: improve sparse trace component selection so fragmented artifacts do not become a dense peak table.
- `bench_08`: improve plot-area/trace artifact suppression and guarded quality rules for over-detected near-duplicate screenshot cases.

The new runtime changes in this slice move actual curve extraction to `PlotAreaBounds`
and store plot-area crop/mask/centerline artifacts in `GeometryTrace`. That makes the
next numerical tuning auditable instead of hidden inside a polished report.
