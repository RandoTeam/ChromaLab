# Guided Calibration UX

The Phase 3 calibration UX is a reusable Compose/KMP surface for placing X/Y calibration anchors after graphPanel and plotArea are confirmed.

## User Flow

1. User enters calibration after `PLOT_AREA_CONFIRMED`.
2. The normalized image is shown with confirmed graphPanel and plotArea overlays.
3. User chooses active axis: `Ставить X` or `Ставить Y`.
4. User taps inside plotArea to place an anchor.
5. User drags an anchor to refine its pixel position.
6. User selects an anchor and enters its known numeric value.
7. User adds at least two X anchors and two Y anchors.
8. UI shows per-axis fit status and residual summary.
9. User confirms calibration when blocking errors are gone.
10. UI shows disabled Phase 4 handoff: trace confirmation is next, but not implemented here.

## Interaction Rules

- Anchors can only be placed inside confirmed plotArea.
- Anchors can be dragged, but reducer logic clamps them inside plotArea.
- A selected anchor can be reassigned between X and Y.
- A selected anchor can be removed.
- Reset clears all anchors and returns calibration to invalid/missing.
- Numeric value parsing is local and deterministic. Empty or invalid text becomes non-finite and blocks confirmation.

## Visual Treatment

- graphPanel overlay: cyan outline;
- plotArea overlay: green fill/outline;
- X anchors: teal;
- Y anchors: amber;
- guide grid: low-opacity neutral lines inside plotArea;
- status chips use `VALID`, `REVIEW`, `INVALID`, and incomplete states.

The color choices are intentionally high-contrast over light and dark chromatogram images and do not rely on color alone; anchor labels and status text are also shown.

## Accessibility

- Anchor touch targets are 48 dp.
- Anchor handles expose content descriptions.
- Back, reset, remove, and confirm controls use regular Material controls.
- Important states are presented as text plus color.
- Russian strings are local to the component for now; future localization work should move them into the project resource strategy.

## State Restoration

The editor state is represented by serializable contracts:

- `CalibrationAnchorEditorSnapshot`;
- `CalibrationAnchorPlacementState`;
- `ManualCalibrationAnchor`;
- `CalibrationEditorEvaluation`.

Phase 3 keeps UI state lightweight: anchor positions/values, selected axis, selected anchor, and unit labels. Source images and overlays remain file references, not saved-state payloads.

## Handoff

After calibration confirmation, the guided state advances to `CALIBRATION_VALIDATED`. Phase 4 may consume the confirmed plotArea and calibration to display trace extraction/confirmation, but Phase 3 does not create trace evidence or release reports.
