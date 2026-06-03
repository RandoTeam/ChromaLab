# DR-C2 Annotation Workflow

Records: `8`

| Fixture | Priority | Images | Required fields | Blocking reasons | Status |
| --- | --- | --- | ---: | --- | --- |
| `bench_01_mz71_screenshot_page` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_01_mz71_screenshot_page.jpg` (ok) | 11 | TICK_LOCALIZATION_FAILURE | NOT_STARTED |
| `bench_04_stacked_xic_resolution` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_04_stacked_xic_resolution.png` (ok) | 11 | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE | NOT_STARTED |
| `bench_05_tic_plus_ions` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_05_tic_plus_ions.png` (ok) | 11 | CALIBRATION_FAILURE | NOT_STARTED |
| `bench_06_photo_two_graphs_page` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_06_photo_two_graphs_page.jpg` (ok) | 11 | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE | NOT_STARTED |
| `bench_02_mz92_belyi_tigr` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_02_mz92_belyi_tigr.jpg` (ok) | 11 | GRAPH_PANEL_FAILURE | NOT_STARTED |
| `bench_03_small_tic_export` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_03_small_tic_export.jpg` (ok) | 11 | PEAK_EVIDENCE_FAILURE | NOT_STARTED |
| `bench_07_rotated_page_photo` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_07_rotated_page_photo.jpg` (ok) | 11 | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE | NOT_STARTED |
| `white_tiger_ion71` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` (ok) | 11 | PEAK_EVIDENCE_FAILURE | NOT_STARTED |

## Field Checklist

| Field | Fixtures needing it | Owner |
| --- | ---: | --- |
| `expected_physical_graph_count` | 8 | Chromatography SME + QA |
| `layout_class` | 8 | Geometry + Chromatography SME |
| `panel_groups` | 8 | Geometry |
| `graph_panel_bounds` | 8 | Geometry |
| `plot_area_bounds` | 8 | Geometry |
| `axis_endpoints` | 8 | Geometry |
| `plot_frame_edges` | 8 | Geometry |
| `tick_or_grid_positions` | 8 | Geometry + OCR |
| `numeric_label_boxes` | 8 | OCR / Text Semantics |
| `text_role_labels` | 8 | OCR / Text Semantics + Scientific Reporting |
| `rejected_non_graph_regions` | 8 | QA + Geometry |
