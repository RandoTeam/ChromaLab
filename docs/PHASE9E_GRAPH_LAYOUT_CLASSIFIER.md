# Phase 9E Graph Layout Classifier

## Contract

`GraphLayoutClassifier` is deterministic-only. It receives resolved graphPanel candidates plus duplicate/nested/subregion rejection evidence and returns:

- `layoutClass`;
- `physicalGraphCount`;
- `panelGroups`;
- `confidence`;
- `reviewReasons`.

The result is embedded in `GraphMultiplicityResolution.layoutClassification`.

## Current Implementation

- Collapses dense same-axis pseudo-panels only when candidates overlap/near-touch and duplicate, nested, or same-axis rejection evidence exists.
- Keeps vertically separated multi-panel groups as multiple graph units.
- Classifies two vertically stacked panels as `TWO_GRAPH_PAGE`.
- Classifies three or more aligned panels as `STACKED_TRACES_SHARED_AXIS`.
- Suppresses full-image fallback candidates when deterministic graph candidates exist.
- Keeps VLM graph-count hints advisory; VLM is not passed as final graph-count authority.

## Fixture Diagnosis

| Fixture | Human layout | Phase 9E expectation |
| --- | --- | --- |
| `bench_02_mz92_belyi_tigr` | One physical graph with pseudo-panel over-splitting risk | `DENSE_PEAK_SINGLE_AXIS` or `SINGLE_TRACE_SINGLE_AXIS`, physical count 1 |
| `bench_04_stacked_xic_resolution` | Four stacked XIC panels | `STACKED_TRACES_SHARED_AXIS`, physical count 4 when panels are detected |
| `bench_05_tic_plus_ions` | TIC plus ion panels | `TIC_PLUS_ION_PANELS` or stacked multi-panel review until channel-aware grouping lands |
| `bench_06_photo_two_graphs_page` | Two graph page photo | `TWO_GRAPH_PAGE`, physical count 2 when both panels are detected |

## Remaining Gap

The classifier cannot invent missing panels. If upstream graph detection only emits one panel for `bench_04`, `bench_05`, or `bench_06`, Phase 9E still remains blocked until graphPanel generation produces the missing panel candidates.

The final Phase 9E Android rerun still under-detected stacked/TIC-plus-ion layouts and remained blocked. The classifier improved evidence semantics and prevented E2B graph-count regression, but it did not close graph generation or tick/OCR calibration failures.
