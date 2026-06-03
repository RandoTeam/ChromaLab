# DR-C6 P0 Graph Layout Method Comparison

Verdict: `PC_PROTOTYPE_IMPROVES_GRAPH_COUNT_NOT_READY_FOR_RUNTIME`
Fixtures: `4`

## Method Summary

| Method | Cases | Graph-count pass | Layout pass | Notes |
| --- | ---: | ---: | ---: | --- |
| `android_phase9j_current` | 8 | 0 | 0 | Current Android output still collapses P0 multi-panel fixtures to one graph. |
| `full_width_axis_projection_v1` | 4 | 3 | 1 | Strong on PNG/export panels; weak on photographed pages with footer/page-frame lines. |
| `label_band_assisted_axis_projection_v1` | 4 | 4 | 2 | Best PC prototype for P0 graph count; still lacks exact semantic layout classification. |

## Prototype Scores

| Method | Fixture | Expected graphs | Detected | Graph count | Expected layout | Predicted layout | Layout score | Selected rows |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- |
| `full_width_axis_projection_v1` | `bench_01_mz71_screenshot_page` | 2 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | SINGLE_TRACE_SINGLE_AXIS | FAIL | 1224 |
| `label_band_assisted_axis_projection_v1` | `bench_01_mz71_screenshot_page` | 2 | 2 | PASS | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 715, 1112 |
| `full_width_axis_projection_v1` | `bench_04_stacked_xic_resolution` | 4 | 4 | PASS | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 226, 496, 779, 1058 |
| `label_band_assisted_axis_projection_v1` | `bench_04_stacked_xic_resolution` | 4 | 4 | PASS | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 226, 496, 779, 1058 |
| `full_width_axis_projection_v1` | `bench_05_tic_plus_ions` | 4 | 4 | PASS | TIC_PLUS_ION_PANELS | MULTI_PANEL_SEPARATE_AXES | FAIL | 362, 506, 644, 783 |
| `label_band_assisted_axis_projection_v1` | `bench_05_tic_plus_ions` | 4 | 4 | PASS | TIC_PLUS_ION_PANELS | MULTI_PANEL_SEPARATE_AXES | FAIL | 362, 506, 644, 783 |
| `full_width_axis_projection_v1` | `bench_06_photo_two_graphs_page` | 2 | 2 | PASS | TWO_GRAPH_PAGE | MULTI_PANEL_SEPARATE_AXES | FAIL | 622, 1237 |
| `label_band_assisted_axis_projection_v1` | `bench_06_photo_two_graphs_page` | 2 | 2 | PASS | TWO_GRAPH_PAGE | MULTI_PANEL_SEPARATE_AXES | FAIL | 622, 1237 |

## Current Android Baseline Scores

| Fixture | Mode | Expected graphs | Detected | Graph count |
| --- | --- | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | deterministic | 2 | 1 | FAIL |
| `bench_01_mz71_screenshot_page` | model_enabled | 2 | 1 | FAIL |
| `bench_04_stacked_xic_resolution` | deterministic | 4 | 1 | FAIL |
| `bench_04_stacked_xic_resolution` | model_enabled | 4 | 1 | FAIL |
| `bench_05_tic_plus_ions` | deterministic | 4 | 1 | FAIL |
| `bench_05_tic_plus_ions` | model_enabled | 4 | 1 | FAIL |
| `bench_06_photo_two_graphs_page` | deterministic | 2 | 1 | FAIL |
| `bench_06_photo_two_graphs_page` | model_enabled | 2 | 1 | FAIL |

## Interpretation

- `label_band_assisted_axis_projection_v1` is not production-ready, but it proves the P0 graph-count problem is not inherently blocked by the images.
- Exact layout class remains unresolved without text-role or panel semantics: `TIC_PLUS_ION_PANELS` and `TWO_GRAPH_PAGE` cannot be safely separated from geometry-only row counts.
- Next step should add a second-stage panel classifier using graph bounds, title/ion text roles, and axis ownership before any Android runtime port.
