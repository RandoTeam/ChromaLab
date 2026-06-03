# DR-C5 P0 Graph Layout Score

Verdict: `CURRENT_OUTPUT_FAILS_P0_GRAPH_LAYOUT_TRUTH`
Cases: `8`
Fixtures: `4`
Graph count pass/fail: `0` / `8`
Layout class pass/fail-or-missing: `0` / `8`
E2B regressions: `0`

## Case Scores

| Fixture | Mode | Truth graphs | Detected | Graph count | Truth layout | Predicted layout | Layout score | Gate | Runtime failure | Decision | Reason |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | deterministic | 2 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | UNKNOWN_REVIEW | MISSING | BLOCKED | TICK_LOCALIZATION_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 2 physical graph(s). |
| `bench_01_mz71_screenshot_page` | model_enabled | 2 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | UNKNOWN_REVIEW | MISSING | BLOCKED | TICK_LOCALIZATION_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 2 physical graph(s). |
| `bench_04_stacked_xic_resolution` | deterministic | 4 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | DENSE_PEAK_SINGLE_AXIS | FAIL | REVIEW_ONLY | VLM_SEMANTIC_LAYER_UNAVAILABLE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 4 physical graph(s). |
| `bench_04_stacked_xic_resolution` | model_enabled | 4 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | DENSE_PEAK_SINGLE_AXIS | FAIL | REVIEW_ONLY | UNKNOWN_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 4 physical graph(s). |
| `bench_05_tic_plus_ions` | deterministic | 4 | 1 | FAIL | TIC_PLUS_ION_PANELS | UNKNOWN_REVIEW | MISSING | BLOCKED | CALIBRATION_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 4 physical graph(s). |
| `bench_05_tic_plus_ions` | model_enabled | 4 | 1 | FAIL | TIC_PLUS_ION_PANELS | UNKNOWN_REVIEW | MISSING | BLOCKED | CALIBRATION_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 4 physical graph(s). |
| `bench_06_photo_two_graphs_page` | deterministic | 2 | 1 | FAIL | TWO_GRAPH_PAGE | DENSE_PEAK_SINGLE_AXIS | FAIL | REVIEW_ONLY | VLM_SEMANTIC_LAYER_UNAVAILABLE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 2 physical graph(s). |
| `bench_06_photo_two_graphs_page` | model_enabled | 2 | 1 | FAIL | TWO_GRAPH_PAGE | DENSE_PEAK_SINGLE_AXIS | FAIL | REVIEW_ONLY | UNKNOWN_FAILURE | FAIL_GRAPH_COUNT | Detected 1 graph(s), but P0 annotation truth has 2 physical graph(s). |

## E2B Comparison

| Fixture | Deterministic graphs | E2B graphs | Gate delta | Layout changed | Comparison |
| --- | ---: | ---: | ---: | --- | --- |
| `bench_01_mz71_screenshot_page` | 1 | 1 | 0 | False | E2B_NEUTRAL |
| `bench_04_stacked_xic_resolution` | 1 | 1 | 0 | False | E2B_NEUTRAL |
| `bench_05_tic_plus_ions` | 1 | 1 | 0 | False | E2B_NEUTRAL |
| `bench_06_photo_two_graphs_page` | 1 | 1 | 0 | False | E2B_NEUTRAL |
