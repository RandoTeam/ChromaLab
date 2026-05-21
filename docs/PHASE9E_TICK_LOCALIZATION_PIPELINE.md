# Phase 9E Tick Localization Pipeline

## Contract

`TickLocalizationPipeline` records why tick localization or calibration failed without weakening calibration gates.

Pipeline authority:

- deterministic CV owns axis lines, tick pixel positions, crop regions, and calibration anchor pixels;
- ML Kit/VLM may read text from local crops only;
- OCR/VLM text without deterministic tick pixels is rejected;
- title, ion, channel, and m/z text cannot become tick labels;
- calibration fit remains residual-backed and uses accepted anchors only.

## Output

`TickLocalizationResult` reports:

- overall tick/calibration status;
- precise subreasons;
- X/Y tick candidate counts;
- accepted/rejected X/Y anchor counts;
- evidence warnings.

Subreasons include:

- `AXIS_LINE_MISSING`
- `PLOT_FRAME_MISSING`
- `TICK_MARKS_MISSING`
- `LABEL_BAND_MISSING`
- `OCR_NO_NUMERIC_TEXT`
- `OCR_NUMERIC_NO_TICK_PIXEL`
- `NON_MONOTONIC_TICK_VALUES`
- `INSUFFICIENT_X_ANCHORS`
- `INSUFFICIENT_Y_ANCHORS`
- `HIGH_RESIDUALS`
- `TITLE_OR_ION_TEXT_REJECTED`
- `GRID_ONLY_NO_TICKS`
- `LOW_RESOLUTION_LABELS_UNREADABLE`

## Runtime Evidence

Graph-stage failure packages now include:

- `tickSummary.subreasons`;
- `layoutClass`;
- `layoutPhysicalGraphCount`;
- accepted/rejected anchors;
- calibration fit statuses and residual summaries when available.

The runtime validator now fails tick/calibration graph-stage failures that omit a tick subreason.
