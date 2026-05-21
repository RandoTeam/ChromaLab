# Phase 9D Blocker Board

| Fixture | Mode | Expected Graph Count | Actual Graph Count | First Failing Stage | Failure Class | Blocker Group | Owner Squad | Final Decision |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | 1 | 1 | semantic layer unavailable | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | VLM | Squad C/D | REVIEW |
| `white_tiger_ion71` | E2B | 1 | 1 | none blocking | `UNKNOWN_FAILURE` with PASS validator | none | Squad C/D | REVIEW |
| `bench_01_mz71_screenshot_page` | deterministic | 2 | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | TICK_LOCALIZATION | Squad B | BLOCKED |
| `bench_01_mz71_screenshot_page` | E2B | 2 | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | TICK_LOCALIZATION | Squad B/C | BLOCKED |
| `bench_02_mz92_belyi_tigr` | deterministic | 1 | 1 report / 2 metadata | graph panel review | `GRAPH_PANEL_FAILURE` | GRAPH_COUNT | Squad A | DIAGNOSTIC |
| `bench_02_mz92_belyi_tigr` | E2B | 1 | 1 report / 2 metadata | graph panel review | `GRAPH_PANEL_FAILURE` | GRAPH_COUNT | Squad A/C | DIAGNOSTIC |
| `bench_03_small_tic_export` | deterministic | 1 | 1 | low-confidence graph evidence | `GRAPH_PANEL_FAILURE` | GRAPH_PANEL | Squad A/D | REVIEW |
| `bench_03_small_tic_export` | E2B | 1 | 1 | low-confidence graph evidence | `GRAPH_PANEL_FAILURE` | GRAPH_PANEL | Squad A/C | REVIEW |
| `bench_04_stacked_xic_resolution` | deterministic | 4 expected panels | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B | BLOCKED |
| `bench_04_stacked_xic_resolution` | E2B | 4 expected panels | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B/C | BLOCKED |
| `bench_05_tic_plus_ions` | deterministic | 4 expected panels | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B | BLOCKED |
| `bench_05_tic_plus_ions` | E2B | 4 expected panels | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B/C | BLOCKED |
| `bench_06_photo_two_graphs_page` | deterministic | 2 | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B | BLOCKED |
| `bench_06_photo_two_graphs_page` | E2B | 2 | 1 | calibration/tick | `TICK_LOCALIZATION_FAILURE` | GRAPH_COUNT/TICK | Squad A/B/C | BLOCKED |
| `bench_07_rotated_page_photo` | deterministic | 1 | 1 | semantic layer unavailable | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | VLM | Squad C/D | REVIEW |
| `bench_07_rotated_page_photo` | E2B | 1 | 1 | none blocking | `UNKNOWN_FAILURE` with PASS validator | none | Squad C/D | REVIEW |

## Closed in Phase 9D

- E2B no longer reduces `bench_02_mz92_belyi_tigr` to zero graphs.
- Validation fixture model geometry hints are disabled; model loading is deferred until deterministic validation geometry/calibration is complete for the current fixture flow.
- Multi-graph sweep startup now prefers reading-order regions when deterministic multiplicity is `MULTI_GRAPH_VALID`.

## Still Blocking

- Tick localization remains blocking for `bench_01`, `bench_04`, `bench_05`, and `bench_06`.
- Multi-panel graph splitting remains incomplete for `bench_04`, `bench_05`, and `bench_06`.
- `bench_02` still records `metadataDetectedGraphCount=2` for an expected one-graph fixture.
