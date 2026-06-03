# DR-C7 Panel Semantic Layout Classifier Prototype

Status: `DR_C7_COMPLETE_ANNOTATION_SEMANTIC_UPPER_BOUND_SOLVES_P0_LAYOUT_CLASS_NOT_RUNTIME_READY`

Date: 2026-06-03

## Purpose

DR-C7 tests whether semantic panel features can close the layout-class gaps left
by DR-C6. It uses the best DR-C6 graph-row candidate counts as input and compares
three classifier levels:

1. geometry-only panel count;
2. annotation-assisted text-role panel family;
3. annotation-assisted text-role plus page-context upper bound.

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Command

```powershell
python tools/benchmark/prototype_drc7_panel_semantic_classifier.py
```

Output:

`benchmark/reports/drc7_panel_semantic_classifier/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`ANNOTATION_SEMANTIC_UPPER_BOUND_SOLVES_P0_LAYOUT_CLASS_NOT_RUNTIME_READY`

| Method | Cases | Layout pass | Runtime readiness |
| --- | ---: | ---: | --- |
| `geometry_only_panel_count_v1` | 4 | 2 | `PROTOTYPE_GEOMETRY_ONLY` |
| `annotation_text_role_panel_family_v1` | 4 | 3 | `UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT_ROLE_EXTRACTION` |
| `annotation_text_role_page_context_upper_bound_v1` | 4 | 4 | `UPPER_BOUND_REQUIRES_AUTOMATIC_PAGE_CONTEXT_AND_TEXT_ROLE_EXTRACTION` |

## Fixture Results

| Fixture | Truth layout | Geometry-only | Text-role family | Text-role + page context |
| --- | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | `MULTI_PANEL_SEPARATE_AXES` | PASS | PASS | PASS |
| `bench_04_stacked_xic_resolution` | `MULTI_PANEL_SEPARATE_AXES` | PASS | PASS | PASS |
| `bench_05_tic_plus_ions` | `TIC_PLUS_ION_PANELS` | FAIL | PASS | PASS |
| `bench_06_photo_two_graphs_page` | `TWO_GRAPH_PAGE` | FAIL | FAIL | PASS |

## What This Proves

- DR-C6 graph-row candidates solve P0 graph count but do not solve semantic
  layout class.
- `bench_05_tic_plus_ions` requires automatic TIC/ion text-role family
  extraction.
- `bench_06_photo_two_graphs_page` requires automatic page-context and
  background/hand/document-region rejection in addition to text roles.
- A geometry-only detector should not be ported to Android as the final graph
  layout classifier.

## Guardrails

- Annotation-assisted methods are upper-bound research methods, not runtime
  methods.
- P0 annotation truth is used for scoring and feature-gap diagnosis only.
- No fixture-specific coordinates were added to production code.
- E2B/VLM did not provide graph geometry.
- This does not make Phase 9 accepted.

## Runtime Capability Gap

Before Android runtime work, the PC benchmark needs automatic versions of:

- OCR text-role extraction for panel titles, ion/m/z metadata, TIC labels,
  legends, axis titles, and tick labels;
- panel-title family classification;
- page/header/hand/background region rejection;
- axis ownership per graph panel.

## Next Slice

Recommended next slice:

`DR-D1: OCR Text-Role Feature Extraction Benchmark`

Goal:

- evaluate OCR/text-role extraction candidates on the DR-C4 annotated P0 set;
- score whether automatic text roles can reproduce the upper-bound features used
  in DR-C7;
- keep VLM/E2B advisory and reject numeric metric authority;
- do not port the semantic classifier to Android until automatic text roles are
  measurable.
