# DR-C4 Tick/Grid And Text Role Annotations

Status: `DR_C4_COMPLETE_PARTIAL_DETAILED_REVIEW`

Date: 2026-06-03

## Purpose

DR-C4 adds initial major tick, numeric label, calibration-anchor, and text-role
annotations for the four P0 fixtures started in DR-C3. It does not change
Android runtime, graph detection, calibration, trace extraction, peak
integration, `CalculationEngine`, chromatographic math, validators, model
routing, or report rendering.

These annotations are benchmark truth inputs. They must not be used as
fixture-specific runtime coordinates.

## Generated Files

Command:

```powershell
python tools/benchmark/build_drc4_tick_text_annotations.py --clean
```

Output:

`benchmark/annotations/drc4_tick_text_role_annotations/`

Generated files:

- `manual-p0-tick-text-annotations.json`
- `summary.json`
- `summary.md`
- `overlays/bench_01_mz71_screenshot_page_tick_text_overlay.png`
- `overlays/bench_04_stacked_xic_resolution_tick_text_overlay.png`
- `overlays/bench_05_tic_plus_ions_tick_text_overlay.png`
- `overlays/bench_06_photo_two_graphs_page_tick_text_overlay.png`

## Annotation Status

Status is `PARTIAL_DETAILED_REVIEW`.

The records include:

- major tick positions;
- numeric label boxes;
- review-grade tick/label calibration anchor candidates;
- text-role labels;
- rejected non-tick text roles for titles, ion/m/z labels, legends, and scale
  annotations.

The records do not yet include:

- minor tick positions;
- pixel-perfect OCR text boxes;
- full OCR transcription review.

## Summary

| Metric | Count |
| --- | ---: |
| Fixtures | 4 |
| Graphs | 12 |
| Major tick positions | 194 |
| Numeric label boxes | 194 |
| Review calibration anchors | 194 |
| Rejected non-tick text roles | 36 |

## Fixture Results

| Fixture | Graphs | Major ticks | Numeric labels | Rejected text roles | Status |
| --- | ---: | ---: | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | 2 | 30 | 30 | 6 | `PARTIAL_DETAILED_REVIEW` |
| `bench_04_stacked_xic_resolution` | 4 | 56 | 56 | 12 | `PARTIAL_DETAILED_REVIEW` |
| `bench_05_tic_plus_ions` | 4 | 56 | 56 | 12 | `PARTIAL_DETAILED_REVIEW` |
| `bench_06_photo_two_graphs_page` | 2 | 52 | 52 | 6 | `PARTIAL_DETAILED_REVIEW` |

## Visual Evidence

| Fixture | Overlay |
| --- | --- |
| `bench_01_mz71_screenshot_page` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_01_mz71_screenshot_page_tick_text_overlay.png` |
| `bench_04_stacked_xic_resolution` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_04_stacked_xic_resolution_tick_text_overlay.png` |
| `bench_05_tic_plus_ions` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_05_tic_plus_ions_tick_text_overlay.png` |
| `bench_06_photo_two_graphs_page` | `benchmark/annotations/drc4_tick_text_role_annotations/overlays/bench_06_photo_two_graphs_page_tick_text_overlay.png` |

## Guardrails

- E2B/VLM did not provide any pixel geometry.
- Major tick positions are review-grade annotation truth, not production
  candidate-selection input.
- Ion/m/z titles, chart titles, legends, axis titles, and scale annotations are
  explicitly marked as non-tick text.
- These records do not make any fixture release-ready.
- Minor ticks and pixel-perfect OCR boxes remain incomplete.

## Next Slice

Recommended next slice:

`DR-C5: Score Current Graph Layout Output Against P0 Annotation Truth`

Goal:

- compare Phase 9J graph count and graphPanel behavior against DR-C3/DR-C4 P0
  truth;
- report exact graph-count/layout gaps before introducing new graph detection
  methods;
- keep all results diagnostic/review-grade until complete truth exists.
