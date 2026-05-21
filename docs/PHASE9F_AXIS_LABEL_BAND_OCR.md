# Phase 9F Axis Label Band OCR

## Scope

Phase 9F expands OCR use from tick-only crops to axis label-band evidence. OCR remains a source of text and boxes only. It is not a numeric measurement authority and cannot create final calibration without deterministic/provenance checks.

## Changes

- Axis-label OCR is run after deterministic graph/plot selection and before scale resolution.
- X labels are collected from the label band below the plot area.
- Y labels are collected from an expanded left/right band because Android artifacts showed Y labels were missed when plotArea selection was too broad.
- VLM axis extraction elements are tagged as `VLM_AXIS_EXTRACTION` and rejected as geometry by `AxisScaleResolver`.
- Title/ion/m/z/SIM/channel and mass-range text is rejected as scale evidence.

## Acceptance Rules

| Input | Action |
| --- | --- |
| OCR label box inside axis band with numeric value and monotonic sequence | Candidate scale anchor. |
| OCR label box near deterministic tick pixel | `LABEL_PROJECTION` candidate. |
| OCR value with no acceptable axis-band geometry | Rejected as `OCR_VALUE_ONLY_REJECTED`. |
| VLM-provided text/position | Rejected for geometry; may remain semantic evidence. |
| Title/ion/m/z/SIM/date/channel text | Rejected as `SEMANTIC_TEXT_REJECTED`. |

## Remaining Limitations

The final Android run still missed Y anchors for `white_tiger_ion71` and `bench_01`. The next repair should focus on plotArea narrowing and true Y-axis label-band localization before any additional OCR parsing changes.
