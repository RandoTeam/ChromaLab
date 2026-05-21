# Phase 9F Graph Layout Signoff

## Decision

No expected graph-count metadata was changed in Phase 9F. The changes were limited to axis-scale evidence and model/geometry safety. Expected graph-count changes still require Chromatography SME, Product, and QA signoff with visual evidence.

## Fixture Signoff Table

| Fixture | Expected count | Phase 9F observed behavior | Signoff |
| --- | ---: | --- | --- |
| `white_tiger_ion71` | 1 | 1 report graph in both modes; calibration blocked by missing Y scale anchors. | Count accepted; calibration blocked. |
| `bench_01_mz71_screenshot_page` | 2 | Final report emits 1 graph; axis scale blocked. | Count not accepted; remains layout/calibration blocker. |
| `bench_02_mz92_belyi_tigr` | 1 | 1 graph in both modes; E2B no longer regresses to zero. | Count accepted; calibration blocked. |
| `bench_03_small_tic_export` | 1 | 1 graph; review-only due peak/evidence limits. | Count accepted. |
| `bench_04_stacked_xic_resolution` | 4 | Metadata/layout evidence sees 4; final report summary still presents 1 graph unit. | Needs report graph propagation follow-up. |
| `bench_05_tic_plus_ions` | 4 | 1 graph; plot frame/scale blocked. | Count not accepted; remains blocker. |
| `bench_06_photo_two_graphs_page` | 2 | Metadata/layout evidence sees 2; final report summary still presents 1 graph unit. | Needs report graph propagation follow-up. |
| `bench_07_rotated_page_photo` | 1 | 1 graph; review-only in both modes. | Count accepted. |

## Product Position

Phase 10 must not start because supported fixtures still produce `BLOCKED` outcomes and graph propagation is incomplete for multi-panel inputs.
