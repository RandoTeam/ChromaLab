# Phase 8D Graph Failure Package

Phase 8D adds a first-class graph-stage terminal evidence package to `RuntimeEvidencePackage`.

## Contract

`RuntimeGraphFailurePackage` records:

- graph id;
- failure class and stage;
- graphPanel and plotArea bounds or explicit missing reasons;
- axis availability summary;
- deterministic tick candidate counts and pixel positions;
- OCR numeric element counts;
- accepted and rejected tick anchors;
- calibration statuses, anchor counts, residual summaries when available;
- graphPanel/plotArea/axis/tick/calibration overlays or explicit missing reasons;
- OCR crop paths or explicit missing reason;
- rejection reasons, warnings, and stage timings.

## Validator Rule

For terminal graph-stage failures after deterministic geometry was attempted, the validator now fails with `graph_failure.package_missing` if no graph failure package exists.

Graph-stage failures include:

- `GRAPH_PANEL_FAILURE`
- `MULTI_GRAPH_SPLIT_FAILURE`
- `PLOT_AREA_FAILURE`
- `AXIS_DETECTION_FAILURE`
- `TICK_LOCALIZATION_FAILURE`
- `OCR_TICK_FAILURE`
- `CALIBRATION_FAILURE`
- `CV_FALLBACK_GRAPH_PANEL_FAILURE`

If the package is present, `graphs: []` is no longer treated as a missing evidence package by itself; the failure package becomes the diagnostic graph evidence.

## Android Verification

Run `white_tiger_ion71_20260520_174154` verified the terminal failure path: the validator returned `REVIEW` instead of failing for `package.graphs_missing`, and the exported graph failure package contained selected graphPanel/plotArea, tick candidate summaries, OCR crop status, and calibration status.

Run `white_tiger_ion71_20260520_184550` reached normal report export, so no graph failure package was needed. The runtime package contains a full graph package with graphPanel, plotArea, axis/tick, trace, and peak evidence.
