# Chromatogram Geometry Failure Classification

Status: runtime geometry follow-up after `6ea7d21`, `5ebe51a`, `49f60e8`.
Date: 2026-05-18.

This document records the current failure mode for the three bench cases that still
fail the full `ChromatogramBenchFixtureTest`. These are not `CalculationEngine`
rewrites; they identify upstream image-to-signal weaknesses that the geometry
pipeline must continue to remove.

## Summary Table

| Fixture | Expected | Actual | Blocking Origin | Classification |
| --- | --- | --- | --- | --- |
| `bench_03_small_tic_export` | at least 5 visible peaks; labeled apexes near 3.244, 3.890, 4.647, 5.610, 8.560 | 3 default peaks after compact right-border suppression; peak sanity blocks | plot-area text/baseline contamination plus low-resolution centerline evidence | The false late right-edge peak is now removed from the mask, but the two smallest labeled peaks remain below the current deterministic trace/peak evidence. This is still upstream image-to-signal quality, not integration math. |
| `bench_04_stacked_xic_resolution` graph 3 | sparse review should cover 4 peaks | passes sparse review: 4 reviewed peaks | fixed in curve candidate/review layer | Sparse trace extraction now selects fragment reconstruction only for genuinely sparse masks and filters micro/terminal/shoulder artifacts from sparse review counts. Graph 3 reviews 4 peaks; graph 4 reviews 1 localized peak. |
| `bench_08_mz71_duplicate_candidate` | guarded completeness path: base 5 -> tuned 9 | default path returns 19 peaks; guarded tuning not applied | curve mask extraction / peak candidate quality gate | GraphPanel and plotArea are detected and the X tick geometry is rich, but Y tick geometry is weak and the signal contains many accepted candidates. The guarded under-detection gate does not activate because the base table is already over-detected. |

## Stage Notes

### `bench_03_small_tic_export`

- GraphPanel detection: full-image fallback accepted; acceptable for this compact screenshot.
- PlotArea detection: detected approximately `13,2 367x99`; warning `plot_area.signal_extends_above_detected_y_axis`.
- Axis detection: axes/origin detected with high confidence.
- Tick localization: weak; only one X tick and one Y tick are found.
- OCR anchor pairing: unavailable in the default desktop environment; no reliable automatic tick-value anchors.
- Calibration fit: manual fixture calibration makes signal conversion possible, but automatic calibration remains blocked.
- Curve extraction: usable, with compact right-border artifact suppression now applied.
- Peak result: the previous false late peak is removed; the accepted table is cleaner but still has only 3 peaks. The labeled 5.610 and 8.560 peaks need a low-resolution trace reconstruction/OCR-evidence pass instead of lower S/N thresholds.
- Stable debug artifacts in each bench run include `plot_area_crop.png`, `mask_raw.png`, `mask_clean.png`, `trace_artifacts.png`, `centerline_*_overlay.png`, and `peak_overlay_graph_1.png`.

### `bench_04_stacked_xic_resolution`

- GraphPanel detection: four panels are split.
- PlotArea detection: all four plot areas are detected.
- Axis/tick localization: deterministic tick evidence remains sparse on several panels.
- Calibration fit: manual fixture calibration permits signal conversion.
- Curve extraction: graph 3 now routes through sparse/fragmented review rather than treating every small residual as a release-grade peak.
- Peak result: current targeted test passes. Sparse traces bypass guarded completeness tuning, and review counts are computed after deterministic artifact filtering.

### `bench_08_mz71_duplicate_candidate`

- GraphPanel detection: single Ion 71 panel detected.
- PlotArea detection: detected with top-signal warning.
- Tick localization: X ticks are abundant; Y tick localization is weak.
- Calibration fit: manual fixture calibration permits signal conversion.
- Curve extraction: usable but includes artifact risk from top-band text / frame context.
- Peak result: 19 default peaks. The expected guarded-completeness path is not reached because the base detector is over-detecting, not under-detecting.
- Next required change: trace candidate scoring must distinguish actual n-alkane series peaks from duplicate/near-duplicate screenshot context and decide whether this fixture should be a guarded-completeness case or a many-peak scientific trace. The current implementation honestly reports the 19 accepted candidates instead of forcing the older 5 -> 9 contract.

## Immediate Engineering Direction

The current failures are all upstream of final report quality:

- `bench_03`: add a low-resolution labeled-peak recovery path: deterministic peak-label OCR/local crops for semantic evidence plus trace reconstruction that can preserve 5-8 px peaks without lowering global S/N.
- `bench_04`: fixed in this slice for sparse stacked panels; keep it as a regression fixture.
- `bench_08`: decide and implement the production rule for dense n-alkane series vs guarded-completeness review. If the 19 peaks are scientifically valid, the fixture contract should be revised with evidence; if not, the trace mask must suppress the contaminating series before peak detection.

The new runtime changes in this slice keep curve extraction on `PlotAreaBounds`, select
fragment reconstruction only for sparse masks, prevent sparse traces from using guarded
threshold relaxation, and suppress compact right-border blocks before peak detection.
The remaining failures have saved deterministic artifacts and are not hidden behind a
polished report.
