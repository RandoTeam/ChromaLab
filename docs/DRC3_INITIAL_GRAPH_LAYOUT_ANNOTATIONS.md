# DR-C3 Initial Graph Layout Annotations

Status: `DR_C3_COMPLETE_PARTIAL_INITIAL_REVIEW`

Date: 2026-06-03

## Purpose

DR-C3 creates the first graph layout annotation records for the P0 fixtures
identified in DR-B3 and DR-C2. It does not change Android runtime, graph
detection, calibration, trace extraction, peak integration, `CalculationEngine`,
chromatographic math, validators, model routing, or report rendering.

These annotations are benchmark truth inputs. They must not be used as
fixture-specific algorithm coordinates.

## Generated Files

Input annotation data:

`benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json`

Render command:

```powershell
python tools/benchmark/render_drc3_annotations.py
```

Output:

`benchmark/annotations/drc3_initial_graph_layout_annotations/`

Generated files:

- `summary.json`
- `summary.md`
- `overlays/bench_01_mz71_screenshot_page_overlay.png`
- `overlays/bench_04_stacked_xic_resolution_overlay.png`
- `overlays/bench_05_tic_plus_ions_overlay.png`
- `overlays/bench_06_photo_two_graphs_page_overlay.png`

## Annotation Status

Status is `PARTIAL_INITIAL_REVIEW`.

The records include:

- expected physical graph count;
- layout class;
- graphPanel bounding boxes;
- plotArea bounding boxes;
- approximate axis endpoints;
- rejected non-graph regions where relevant;
- overlay images.

The records do not yet include release-grade:

- individual tick/grid positions;
- individual numeric label boxes;
- full text-role labels.

## P0 Fixture Results

| Fixture | Layout class | Expected graphs | Annotated graphs | Status |
| --- | --- | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | `MULTI_PANEL_SEPARATE_AXES` | 2 | 2 | `INITIAL_REVIEW` |
| `bench_04_stacked_xic_resolution` | `MULTI_PANEL_SEPARATE_AXES` | 4 | 4 | `INITIAL_REVIEW` |
| `bench_05_tic_plus_ions` | `TIC_PLUS_ION_PANELS` | 4 | 4 | `INITIAL_REVIEW` |
| `bench_06_photo_two_graphs_page` | `TWO_GRAPH_PAGE` | 2 | 2 | `INITIAL_REVIEW` |

## Fixture Interpretation

### bench_01_mz71_screenshot_page

Visual evidence shows two separate ion chromatogram panels on one printed page.
Each panel has its own y axis and x axis. This fixture must not be collapsed
into one physical graph.

Overlay:

`benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_01_mz71_screenshot_page_overlay.png`

### bench_04_stacked_xic_resolution

Visual evidence shows four vertically stacked XIC panels. They share related
time ranges but have separate y axes and separate plot areas. This is not one
single plotArea.

Overlay:

`benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_04_stacked_xic_resolution_overlay.png`

### bench_05_tic_plus_ions

Visual evidence shows one TIC panel followed by three ion panels. This is a
TIC-plus-ion-panel layout with four physical graph outputs and different y
scales.

Overlay:

`benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_05_tic_plus_ions_overlay.png`

### bench_06_photo_two_graphs_page

Visual evidence shows two separate ion chromatogram panels on a photographed
printed page. The lower panel has weak signal, but it has its own title, frame,
axes, and x/y labels, so it still counts as a physical graph.

Overlay:

`benchmark/annotations/drc3_initial_graph_layout_annotations/overlays/bench_06_photo_two_graphs_page_overlay.png`

## Guardrails

- These coordinates are annotation truth, not runtime candidate-selection input.
- All records remain `INITIAL_REVIEW`, not release-grade truth.
- Tick/grid and numeric label boxes remain incomplete.
- E2B/VLM did not provide any pixel geometry.
- No product status was upgraded.

## Next Slice

Recommended next slice:

`DR-C4: Detailed Tick/Grid And Text Role Annotation For P0 Fixtures`

Goal:

- annotate individual tick/grid positions;
- annotate numeric label boxes;
- classify text roles;
- explicitly reject title, ion, m/z, legend, and peak annotation numbers as tick
  labels;
- keep incomplete cases `DIAGNOSTIC_ONLY` until detailed evidence exists.
