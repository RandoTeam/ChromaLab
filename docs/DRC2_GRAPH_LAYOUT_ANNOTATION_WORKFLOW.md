# DR-C2 Graph Layout Annotation Workflow

Status: `DR_C2_COMPLETE`

Date: 2026-06-03

## Purpose

DR-C2 turns the DR-C1 graph/layout/axis annotation plan into concrete workflow
records. It does not annotate graph coordinates automatically and does not
change Android runtime, graph detection, calibration, trace extraction, peak
integration, `CalculationEngine`, chromatographic math, validators, model
routing, or report rendering.

The purpose is to make manual or assisted annotation auditable before DR-C
method comparison begins.

## Generated Workflow

Command:

```powershell
python tools/benchmark/build_drc2_annotation_workflow.py --clean
```

Output:

`benchmark/annotations/drc2_graph_layout_annotation_workflow/`

Generated files:

- `workflow-summary.json`
- `workflow-summary.md`
- `dashboard-data.json`
- `records/<fixture_id>.json` for all 8 fixtures

## Data Analytics Visualization

The `data-analytics` plugin was used to render:

- DR-C2 annotation priority chart;
- DR-C2 fixture annotation workflow table.

Dashboard source data:

`benchmark/annotations/drc2_graph_layout_annotation_workflow/dashboard-data.json`

Visualization result:

- 4 fixtures are `P0_GRAPH_LAYOUT_BLOCKER`;
- 4 fixtures are `P1_REVIEW_TRUTH_GAP`;
- all 8 source images exist;
- every fixture currently has 11 required annotation fields;
- annotation status is intentionally `NOT_STARTED`.

## Workflow Summary

| Metric | Value |
| --- | ---: |
| Fixture records | 8 |
| P0 graph-layout blockers | 4 |
| P1 review truth gaps | 4 |
| Source images missing | 0 |
| Required fields per fixture | 11 |

P0 fixtures:

- `bench_01_mz71_screenshot_page`
- `bench_04_stacked_xic_resolution`
- `bench_05_tic_plus_ions`
- `bench_06_photo_two_graphs_page`

P1 fixtures:

- `bench_02_mz92_belyi_tigr`
- `bench_03_small_tic_export`
- `bench_07_rotated_page_photo`
- `white_tiger_ion71`

## Provenance

Each workflow record includes:

- fixture id;
- priority;
- source image path;
- source image existence;
- source image SHA-256;
- source image byte size;
- Phase 9J deterministic/E2B mode decisions;
- runtime failure classes;
- required annotation fields;
- owner/instruction per field.

Only source image provenance is filled automatically. GraphPanel, plotArea, axis,
tick/grid, numeric labels, and text roles remain `NEEDS_ANNOTATION`.

## Required Annotation Fields

Every fixture currently needs:

- `expected_physical_graph_count`
- `layout_class`
- `panel_groups`
- `graph_panel_bounds`
- `plot_area_bounds`
- `axis_endpoints`
- `plot_frame_edges`
- `tick_or_grid_positions`
- `numeric_label_boxes`
- `text_role_labels`
- `rejected_non_graph_regions`

## Guardrails

- Annotation records are not fixture-specific algorithm coordinates.
- Expected graph count is truth for scoring, not an input to candidate selection.
- PlotArea must be inside graphPanel.
- Numeric labels must be role-labeled before calibration use.
- Ion, m/z, title, legend, and peak annotation numbers must be rejected as tick labels.
- E2B/VLM cannot provide pixel geometry or numeric calibration truth.

## Next Slice

Recommended next slice:

`DR-C3: Create Initial Graph Layout Annotation Records`

Goal:

- annotate P0 fixtures first;
- produce explicit graphPanel, plotArea, graph count, axis, tick/grid, and text
  role truth records;
- keep incomplete annotations as `DIAGNOSTIC_ONLY`, not release evidence.
