# Phase 8D Tick Localization Audit

Run audited: `white_tiger_ion71_20260520_170118`

Local artifact directory:

`artifacts/phase8c-android-validation/white_tiger_ion71_20260520_170118/white_tiger_ion71_20260520_170118/`

## Finding

The Android fixture no longer stops before geometry. It reached:

1. `GRAPH_SELECTION`
2. `GRAPH_ROI`
3. `AXIS_DETECTION`
4. `OCR_SUGGESTION`
5. `X_CALIBRATION`
6. `Y_CALIBRATION`

The terminal failure was:

- report gate: `BLOCKED`
- validator: `FAIL`
- runtime failure class: `TICK_LOCALIZATION_FAILURE`
- terminal message: at least two Y tick labels were required before signal conversion

## Evidence Gap

The runtime package exported `graphs: []`, so the validator could only report `package.graphs_missing`. The artifact did not preserve the selected graphPanel, plotArea, axis candidate, tick candidate, OCR crop, accepted/rejected anchor, or calibration attempt table.

## Available Counts From Existing Artifact

| Item | Value |
| --- | --- |
| Graph packages | 0 |
| Model diagnostics | 1, `NOT_CONFIGURED` |
| Stage timings | Present through `Y_CALIBRATION` |
| Graph-level failure package | Missing |
| Overlay artifacts | Manifest placeholders only |
| Old axis failure reached | Yes, but exact axis/tick evidence was not exportable |

## Required Closure

Phase 8D must export graph-level failure packages for graph-stage terminal failures and must preserve deterministic tick/OCR/calibration attempt evidence even if final chromatographic signal/report generation is blocked.

## Closure Reruns

Phase 8D reruns after the graph-package and tick-rescue changes:

| Run | Result |
| --- | --- |
| `white_tiger_ion71_20260520_173305` | Terminal failure exported a graph-level package. X tick candidates were present, but Y tick localization remained insufficient. |
| `white_tiger_ion71_20260520_174154` | Deterministic label-band rescue produced 10 X and 10 Y tick candidates, but OCR pairing still lacked enough accepted anchors. Validator returned `REVIEW`, not missing graph package. |
| `white_tiger_ion71_20260520_184550` | Full report export reached. Runtime package contains one graph, `VALID` X/Y calibration statuses, trace/peak overlays, two peak evidence rows, and validator verdict `REVIEW` with zero blocking issues. |

Final Phase 8D status for the fixture: tick localization and X/Y calibration no longer block the run. The remaining `REVIEW_ONLY` gate is `VLM_SEMANTIC_LAYER_UNAVAILABLE`, not `TICK_LOCALIZATION_FAILURE`.
