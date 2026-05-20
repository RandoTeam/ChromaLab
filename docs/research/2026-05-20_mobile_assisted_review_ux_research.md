# Mobile Assisted Review UX Research - 2026-05-20

## Scope

This note supports reclassifying existing guided editors as Assisted Review and Manual Advanced tools.

## Source Matrix

| Source | Quality | Relevance | Decision affected | Not adopted |
| --- | --- | --- | --- | --- |
| [Compose touch input](https://developer.android.com/develop/ui/compose/touch-input) | Official Android docs | Custom pointer input is appropriate for image annotation but needs state and accessibility care. | Keep Phase 2-4 zoom/pan overlay components reusable for review. | Do not make canvas-only controls without semantics. |
| [Compose graphics draw overview](https://developer.android.com/develop/ui/compose/graphics/draw/overview) | Official Android docs | Canvas overlay rendering is suitable for graph/trace evidence display. | Trace overlay screen can remain reusable evidence UI. | Do not rely only on visual color to communicate validity. |
| [Compose state saving](https://developer.android.com/develop/ui/compose/state-saving) | Official Android docs | Review decisions must survive recomposition and process recreation through state contracts. | Guided/assisted reducers remain contract-first and serializable. | Do not store large image blobs in UI state. |
| [Android touch target guidance](https://support.google.com/accessibility/android/answer/7101858) | Official accessibility guidance | Touch targets should be at least 48 dp for interactive controls. | Review controls must remain finger-usable and labeled. | Do not use tiny unlabeled handles for production UX. |
| [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility) | Official Android docs | Semantics and content descriptions are required for screen-reader support. | Assisted review screens need explicit labels for accept/reject/review decisions. | Do not ship visual-only status indicators. |

## Decisions

- The app should not open manual/guided screens as the normal happy path.
- Assisted Review should be staged and contextual: only failed/review gates ask for correction.
- Existing Phase 2/3/4 UI remains valuable as repair tooling.
- User intervention must be concise, localized, and provenance-backed.

## Rejected Sources / Claims

- Generic manual digitization UX is too slow as the primary product workflow.
- Requiring every normal user to place axes/calibration anchors is not the target product.
