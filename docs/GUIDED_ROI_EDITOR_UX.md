# Guided ROI Editor UX

Status: Phase 2 UX contract

## Product Role

The guided ROI editor is the first step toward `GUIDED_PRODUCTION`. It turns automatic ROI suggestions into user-confirmed evidence without claiming that the whole chromatogram report is ready.

## User Flow

1. The image is loaded in guided mode.
2. The app shows the suggested graphPanel.
3. The user adjusts or accepts graphPanel.
4. The app records graphPanel confirmation and moves to plotArea.
5. The app shows the suggested plotArea inside graphPanel.
6. The user adjusts or accepts plotArea.
7. The app records plotArea confirmation.
8. The screen shows `Next: calibration anchors` as disabled Phase 3 handoff.

## Interaction Model

- Pinch or trackpad gesture zooms the image.
- Dragging the central handle moves the active rectangle.
- Dragging edge/corner handles resizes the active rectangle.
- Reset restores the current stage to its auto suggestion.
- Confirm records a user confirmation if validation has no blocking errors.

## GraphPanel User Guidance

GraphPanel must include:

- title;
- ion/channel text;
- axis labels;
- tick labels;
- plot frame;
- any labels needed to understand the graph.

GraphPanel should not be only the trace area. A right-side or partial crop is not production evidence when the full panel is visible.

## PlotArea User Guidance

PlotArea must include:

- coordinate rectangle;
- signal trace;
- grid/frame if present.

PlotArea must exclude:

- title/ion text;
- Y tick labels;
- X tick labels;
- axis captions;
- surrounding page or app UI.

If plotArea equals graphPanel, the app marks it review-grade. This is allowed for evidence capture but should not silently unlock a release-ready report.

## Error and Review States

Blocking invalid states:

- missing bounds;
- zero-area bounds;
- graphPanel outside image;
- plotArea outside graphPanel.

Review states:

- plotArea equals graphPanel;
- plotArea is too close to graphPanel edges.

## State Restoration

The reusable editor content is state-hoisted. `GuidedRoiEditorSnapshot` is serializable and can be stored by a future screen model or persistence layer. The editor does not store heavy image data in Compose saved state.

## Accessibility

- Handles expose 48 dp touch targets.
- State is shown with labels, not color alone.
- Important controls have content descriptions.
- The bottom controls are large enough for touch input.

## Phase Boundaries

This UX does not include:

- calibration anchor placement;
- trace correction;
- peak review;
- final report release;
- VLM numeric geometry.
