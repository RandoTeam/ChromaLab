# DR-C6 P0 Graph Layout Method Comparison

Status: `DR_C6_COMPLETE_PC_PROTOTYPE_IMPROVES_GRAPH_COUNT_NOT_READY_FOR_RUNTIME`

Date: 2026-06-03

## Purpose

DR-C6 compares the current Phase 9J Android graph-count baseline with PC-side
image-processing prototypes on the four P0 graph-layout fixtures from DR-C3 and
DR-C4.

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Command

```powershell
python tools/benchmark/prototype_drc6_p0_graph_layout_methods.py
```

Output:

`benchmark/reports/drc6_p0_graph_layout_method_comparison/`

Generated files:

- `summary.json`
- `summary.md`
- `overlays/*_full_width_axis_projection_v1.png`
- `overlays/*_label_band_assisted_axis_projection_v1.png`

## Verdict

`PC_PROTOTYPE_IMPROVES_GRAPH_COUNT_NOT_READY_FOR_RUNTIME`

The current Android baseline still collapses every P0 multi-panel fixture to one
graph. The best PC prototype detects the expected physical graph count on all
four P0 images, but it is not ready for runtime because exact layout semantics
are still unresolved.

| Method | Cases | Graph-count pass | Layout pass | Decision |
| --- | ---: | ---: | ---: | --- |
| `android_phase9j_current` | 8 | 0 | 0 | Fails P0 graph-count truth. |
| `full_width_axis_projection_v1` | 4 | 3 | 1 | Useful on PNG/export panels; weak on photographed page/footer lines. |
| `label_band_assisted_axis_projection_v1` | 4 | 4 | 2 | Best graph-count prototype; still needs semantic layout classifier. |

## Prototype Results

| Fixture | Truth graphs | Best prototype graphs | Graph count | Truth layout | Prototype layout | Layout score |
| --- | ---: | ---: | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | 2 | 2 | PASS | `MULTI_PANEL_SEPARATE_AXES` | `MULTI_PANEL_SEPARATE_AXES` | PASS |
| `bench_04_stacked_xic_resolution` | 4 | 4 | PASS | `MULTI_PANEL_SEPARATE_AXES` | `MULTI_PANEL_SEPARATE_AXES` | PASS |
| `bench_05_tic_plus_ions` | 4 | 4 | PASS | `TIC_PLUS_ION_PANELS` | `MULTI_PANEL_SEPARATE_AXES` | FAIL |
| `bench_06_photo_two_graphs_page` | 2 | 2 | PASS | `TWO_GRAPH_PAGE` | `MULTI_PANEL_SEPARATE_AXES` | FAIL |

## Visual Evidence

| Fixture | Best prototype overlay |
| --- | --- |
| `bench_01_mz71_screenshot_page` | `benchmark/reports/drc6_p0_graph_layout_method_comparison/overlays/bench_01_mz71_screenshot_page_label_band_assisted_axis_projection_v1.png` |
| `bench_04_stacked_xic_resolution` | `benchmark/reports/drc6_p0_graph_layout_method_comparison/overlays/bench_04_stacked_xic_resolution_label_band_assisted_axis_projection_v1.png` |
| `bench_05_tic_plus_ions` | `benchmark/reports/drc6_p0_graph_layout_method_comparison/overlays/bench_05_tic_plus_ions_label_band_assisted_axis_projection_v1.png` |
| `bench_06_photo_two_graphs_page` | `benchmark/reports/drc6_p0_graph_layout_method_comparison/overlays/bench_06_photo_two_graphs_page_label_band_assisted_axis_projection_v1.png` |

## What This Proves

- The P0 graph-count failure is not caused by the images being impossible.
- A simple deterministic PC pass can recover the expected physical graph count
  for the P0 set.
- Geometry-only row detection is insufficient for final layout semantics:
  `TIC_PLUS_ION_PANELS` and `TWO_GRAPH_PAGE` need text-role, title/ion, and
  axis-ownership reasoning.
- Runtime porting is premature until the semantic layout classifier is scored.

## Guardrails

- P0 truth is used only for scoring, not candidate selection.
- No fixture-specific coordinates were added to production code.
- E2B/VLM did not provide graph geometry.
- No report gate or validator was weakened.
- This does not make Phase 9 accepted.

## Next Slice

Next slice completed:

`DR-C7: Panel Semantic Layout Classifier Prototype`

Result:

- documented in `docs/DRC7_PANEL_SEMANTIC_LAYOUT_CLASSIFIER.md`;
- geometry-only classification passes 2/4 P0 layout classes;
- annotation text-role family features pass 3/4;
- annotation text-role plus page-context upper bound passes 4/4;
- runtime porting still requires automatic OCR/text-role and page-context
  extraction.
