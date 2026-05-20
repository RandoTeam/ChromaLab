# Phase 9C Root Cause Board

Verdict before repair: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`.

Phase 9C scope is limited to Android validation, deterministic graph/tick/calibration reliability, model gating, evidence export, and regression acceptance. `CalculationEngine` and chromatographic math are out of scope.

## Board

| Blocker | Fixture / mode | Expected | Actual before repair | First failing stage | Suspected root cause | Owner | Fix plan | Tests | Status |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- |
| P9C-01 | `bench_01_mz71_screenshot_page` deterministic/E2B | 2 graphs | 0 report graphs; graph package exists | `X_CALIBRATION` | OCR crop noise accepted as axis anchors; broad axis matching permits wrong values. | Geometry + OCR | Preserve local crop axis/tick provenance and reject OCR values without deterministic tick pixels. | `TickOcrMatcherTest`; Android rerun | IN_PROGRESS |
| P9C-02 | `bench_02_mz92_belyi_tigr` deterministic | 1 graph | suite reported 2 graphs | `GRAPH_PANEL` / summary | Report metadata and summary counted raw/failed graph evidence instead of completed physical graph reports. | QA + Geometry | Distinguish report graphs, metadata count, and graph failure packages in suite summary. | debug suite tests; Android rerun | IN_PROGRESS |
| P9C-03 | `bench_02_mz92_belyi_tigr` E2B | 1 graph | 0 report graphs | `Y_CALIBRATION` | Model-enabled terminal failure discarded completed graph count in summary; tick OCR still fragile. | Android + VLM + Geometry | Keep deterministic graph/tick evidence primary; summarize graph failure packages; ensure E2B cannot erase deterministic candidates. | model comparison contract; Android rerun | IN_PROGRESS |
| P9C-04 | `bench_04_stacked_xic_resolution` deterministic/E2B | 4 graphs | 0 report graphs | calibration/tick | Stacked panels reach graph evidence but terminal failure report shows zero; tick OCR pairing weak. | Geometry + SME | Improve tick OCR provenance and rerun; classify remaining stacked-panel failures with graph packages. | Android rerun | IN_PROGRESS |
| P9C-05 | `bench_05_tic_plus_ions` deterministic/E2B | 4 graphs | 0 report graphs | calibration/tick | Multi-panel TIC+ion cases lack enough accepted anchors after OCR pairing. | Geometry + Trace/Peak | Same tick provenance fix; preserve per-panel evidence. | Android rerun | IN_PROGRESS |
| P9C-06 | `bench_06_photo_two_graphs_page` deterministic/E2B | 2 graphs | 0 report graphs | `Y_CALIBRATION` | Y labels were accepted as X anchors from an X crop; duplicate values at one tick pixel polluted calibration. | Geometry + OCR | Select one numeric value per crop and bind it to the crop axis/tick only. | `TickOcrMatcherTest`; Android rerun | IN_PROGRESS |
| P9C-07 | `bench_03_small_tic_export` deterministic/E2B | 1 graph | `DIAGNOSTIC_ONLY` | `GRAPH_PANEL` | Low-resolution image remains review/diagnostic; no critical export failure. | Product + QA | Rerun after shared fixes and keep evidence honest. | Android rerun | PENDING |
| P9C-08 | `bench_07_rotated_page_photo` deterministic/E2B | 1 graph | `REVIEW_ONLY` | none critical | Stable review baseline must not regress. | QA | Rerun as non-regression sentinel. | Android rerun | PENDING |
| P9C-09 | `white_tiger_ion71` deterministic/E2B | 1 graph | `REVIEW_ONLY` | none critical | Stable baseline must not regress. | QA | Rerun as non-regression sentinel. | Android rerun | PENDING |

## Repair Order

1. Evidence/summary export gaps.
2. Tick OCR provenance and deterministic anchor pairing.
3. Graph count and E2B regression comparison.
4. Android rerun for all eight fixtures in deterministic and E2B modes.
5. Product/QA/scientific closeout decision.
