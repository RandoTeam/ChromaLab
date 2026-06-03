# DR-C7 Panel Semantic Layout Classifier Prototype

Verdict: `ANNOTATION_SEMANTIC_UPPER_BOUND_SOLVES_P0_LAYOUT_CLASS_NOT_RUNTIME_READY`
Fixtures: `4`

## Method Summary

| Method | Cases | Layout pass | Fail/missing | Runtime readiness | Notes |
| --- | ---: | ---: | ---: | --- | --- |
| `geometry_only_panel_count_v1` | 4 | 2 | 2 | `PROTOTYPE_GEOMETRY_ONLY` | Control method; cannot distinguish TIC+ion or two-graph page semantics. |
| `annotation_text_role_panel_family_v1` | 4 | 3 | 1 | `UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT_ROLE_EXTRACTION` | Shows that OCR/text-role families can resolve TIC+ion panels. |
| `annotation_text_role_page_context_upper_bound_v1` | 4 | 4 | 0 | `UPPER_BOUND_REQUIRES_AUTOMATIC_PAGE_CONTEXT_AND_TEXT_ROLE_EXTRACTION` | Shows that page/background context is needed to separate two-graph pages from generic ion panels. |

## Case Scores

| Method | Fixture | Truth layout | Predicted layout | Score | Graphs | Reason |
| --- | --- | --- | --- | --- | ---: | --- |
| `geometry_only_panel_count_v1` | `bench_01_mz71_screenshot_page` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 2 | Geometry detected multiple graph rows but no semantic panel type. |
| `annotation_text_role_panel_family_v1` | `bench_01_mz71_screenshot_page` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 2 | Text roles show multiple panels but no TIC+ion mix. |
| `annotation_text_role_page_context_upper_bound_v1` | `bench_01_mz71_screenshot_page` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 2 | Text roles show multiple panels but no TIC+ion mix. |
| `geometry_only_panel_count_v1` | `bench_04_stacked_xic_resolution` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 4 | Geometry detected multiple graph rows but no semantic panel type. |
| `annotation_text_role_panel_family_v1` | `bench_04_stacked_xic_resolution` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 4 | Text roles show multiple panels but no TIC+ion mix. |
| `annotation_text_role_page_context_upper_bound_v1` | `bench_04_stacked_xic_resolution` | MULTI_PANEL_SEPARATE_AXES | MULTI_PANEL_SEPARATE_AXES | PASS | 4 | Text roles show multiple panels but no TIC+ion mix. |
| `geometry_only_panel_count_v1` | `bench_05_tic_plus_ions` | TIC_PLUS_ION_PANELS | MULTI_PANEL_SEPARATE_AXES | FAIL | 4 | Geometry detected multiple graph rows but no semantic panel type. |
| `annotation_text_role_panel_family_v1` | `bench_05_tic_plus_ions` | TIC_PLUS_ION_PANELS | TIC_PLUS_ION_PANELS | PASS | 4 | Text roles expose a TIC panel plus ion/mz panels. |
| `annotation_text_role_page_context_upper_bound_v1` | `bench_05_tic_plus_ions` | TIC_PLUS_ION_PANELS | TIC_PLUS_ION_PANELS | PASS | 4 | Text roles expose a TIC panel plus ion/mz panels. |
| `geometry_only_panel_count_v1` | `bench_06_photo_two_graphs_page` | TWO_GRAPH_PAGE | MULTI_PANEL_SEPARATE_AXES | FAIL | 2 | Geometry detected multiple graph rows but no semantic panel type. |
| `annotation_text_role_panel_family_v1` | `bench_06_photo_two_graphs_page` | TWO_GRAPH_PAGE | MULTI_PANEL_SEPARATE_AXES | FAIL | 2 | Text roles show multiple panels but no TIC+ion mix. |
| `annotation_text_role_page_context_upper_bound_v1` | `bench_06_photo_two_graphs_page` | TWO_GRAPH_PAGE | TWO_GRAPH_PAGE | PASS | 2 | Page-context features show a photographed two-graph page with hand/background rejection. |

## Semantic Feature Audit

| Fixture | Panel groups | Panel families | Text families | Page context |
| --- | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | ion_217_panel, ion_218_panel | {"ion": 2} | {"ion": 2, "ion_text": 2} | {"page_header": 1} |
| `bench_04_stacked_xic_resolution` | xic_198_0315_pm_0_2, xic_198_0315_pm_0_02, xic_198_0315_pm_0_002, xic_198_0315_pm_0_0002 | {"xic": 4} | {"xic": 4, "legend_text": 4} | {} |
| `bench_05_tic_plus_ions` | tic, ion_326, ion_360, ion_394 | {"tic": 1, "ion": 3} | {"tic": 1, "tic_text": 1, "ion": 3, "ion_text": 3} | {} |
| `bench_06_photo_two_graphs_page` | ion_83_panel, ion_92_panel | {"ion": 2} | {"ion": 2, "ion_text": 2} | {"page_header": 1, "hand_or_background": 1} |

## Interpretation

- Geometry-only row counting solves P0 physical graph count but not semantic layout class.
- OCR/text-role families are sufficient to identify `TIC_PLUS_ION_PANELS` in this P0 set.
- Page/background context is needed to separate `TWO_GRAPH_PAGE` from generic two ion-panel layouts.
- The upper-bound method uses annotation features; it is not runtime-ready until those features are produced automatically.
