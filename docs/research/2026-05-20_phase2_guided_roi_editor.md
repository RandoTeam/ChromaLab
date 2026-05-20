# Phase 2 Research Note: Guided ROI Editor

Date: 2026-05-20
Phase: Phase 2 - Guided GraphPanel / PlotArea Editor
Scope: Compose/KMP ROI editing, zoom/pan gestures, state restoration, accessibility, and visual design constraints.

## Source Quality Triage

Priority was given to current official documentation and standards. Weak blogs, uncited examples, and outdated snippets were not used for implementation decisions. Model knowledge may be outdated, so this research note records the sources that drove Phase 2 choices.

## Sources Reviewed

| Source | Why Relevant | Decision Impact | Not Adopted |
| --- | --- | --- | --- |
| [Android Developers: Pointer input in Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input) | Official Compose guidance for low-level pointer handling. | ROI handles use Compose pointer input and drag gestures rather than ad hoc platform views. | Did not copy sample code directly; kept ChromaLab-specific state model. |
| [Android Developers: Understand gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures) | Official guidance on choosing gesture abstraction level. | `detectTransformGestures` is used for image zoom/pan; resize handles use drag gestures. | Did not add complex custom gesture recognizer in Phase 2. |
| [Android Developers: Graphics in Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview) | Official Canvas/drawing guidance. | ROI rectangles, scrim, guide grid, and handles are drawn as Compose overlays. | Did not introduce a separate vector/bitmap overlay renderer. |
| [Android Developers: Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving) | Official state restoration guidance. | Phase 2 keeps a serializable `GuidedRoiEditorSnapshot` and stateless content API for hoisting/restoration. | Did not store heavy images or masks in saved Compose state. |
| [Android Accessibility Help: Touch target size](https://support.google.com/accessibility/android/answer/7101858) | Current Android accessibility guidance for tappable controls. | Resize/move handles use 48 dp touch targets. | Did not rely on tiny visual handles as the only touch area. |
| [Material Design 3](https://m3.material.io/) | Current Material guidance for mobile components, hierarchy, and states. | Status chips, top/bottom bars, and controls use Material 3 components and existing ChromaLab theme. | Did not copy external Gallery assets or visual identity. |
| [WCAG 2.2 Target Size](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) | Standard target-size reference. | Reinforced 48 dp touch targets and non-tiny controls. | Did not treat WCAG target minimum as a reason to shrink handles. |
| [WCAG 2.2 Use of Color](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html) | Standard rule that color cannot be the only state indicator. | ROI states include text labels and status chips, not color alone. | Did not encode confirmation/review solely by blue/yellow/green. |
| [WCAG 2.2 Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html) | Standard contrast guidance for graphical controls. | Handles and overlays use high-contrast outlines against chromatogram images. | Did not use low-alpha decorative-only borders for handles. |

## Synthesis

Phase 2 should implement a reusable editor component, not an image-specific fix. The editor must maintain all geometry in normalized image coordinates so it can feed future calibration, trace, and peak editors without coordinate ambiguity. Compose pointer input is appropriate for this phase because the editor needs custom image-coordinate drag/resize behavior and cannot be expressed as a standard form component.

The editor should use a hoisted, serializable state object. Compose state saving guidance discourages storing heavy artifacts in saved state, so Phase 2 stores only bounds, statuses, suggestions, and edit flags. Image paths and evidence artifacts remain external references.

Accessibility and scientific provenance constraints are linked: every user-confirmed ROI must preserve not only coordinates but also source, timestamp, related image id, warnings, and gate status. A visually plausible rectangle is not enough production evidence.

## Decisions

- Add `GuidedRoiEditorSnapshot` as serializable transient UI state.
- Keep `graphPanel` and `plotArea` in normalized image coordinates.
- Add pure reducer/validator helpers for testable bounds updates and confirmations.
- Implement zoom/pan with `detectTransformGestures`.
- Implement move/resize with 48 dp draggable handles.
- Use status chips and text warnings so state does not rely only on color.
- Keep Phase 2 out of calibration, trace, peak, VLM, and CalculationEngine logic.

## Risks

- Real Android process-death restoration still needs integration with the eventual guided workflow store.
- Visual rendering needs real-device review after the screen is wired into navigation.
- Pointer-input behavior should be tested manually on phones with different densities because common unit tests cover only reducer logic.
