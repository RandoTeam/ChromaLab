# DR-C1 Graph Layout And Axis Annotation Plan

Tasks: `8`
Coordinate space: `NORMALIZED_IMAGE`

| Fixture | Priority | Source images | Required fields | Blocking reasons |
| --- | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_01_mz71_screenshot_page.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | TICK_LOCALIZATION_FAILURE |
| `bench_04_stacked_xic_resolution` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_04_stacked_xic_resolution.png` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE |
| `bench_05_tic_plus_ions` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_05_tic_plus_ions.png` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | CALIBRATION_FAILURE |
| `bench_06_photo_two_graphs_page` | P0_GRAPH_LAYOUT_BLOCKER | `composeApp/src/androidMain/assets/validation/bench_06_photo_two_graphs_page.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE |
| `bench_02_mz92_belyi_tigr` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_02_mz92_belyi_tigr.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | GRAPH_PANEL_FAILURE |
| `bench_03_small_tic_export` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_03_small_tic_export.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | PEAK_EVIDENCE_FAILURE |
| `bench_07_rotated_page_photo` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/bench_07_rotated_page_photo.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | UNKNOWN_FAILURE, VLM_SEMANTIC_LAYER_UNAVAILABLE |
| `white_tiger_ion71` | P1_REVIEW_TRUTH_GAP | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` | expected_physical_graph_count, layout_class, panel_groups, graph_panel_bounds, plot_area_bounds, axis_endpoints, plot_frame_edges, tick_or_grid_positions, numeric_label_boxes, text_role_labels, rejected_non_graph_regions | PEAK_EVIDENCE_FAILURE |

## Annotation Rules

- Expected graph count is truth, not an input to candidate selection.
- Graph panels include the complete physical graph, titles, axis labels, ticks, and plot frame context.
- Plot areas contain the data region only and exclude titles, legends, and tick-label bands.
- Numeric label boxes must be role-labeled before they can be used by calibration.
- ION, m/z, title, legend, and peak annotation numbers must be explicitly rejected as tick labels.
- E2B/VLM may describe or warn, but cannot provide pixel geometry or numeric calibration truth.
