# Phase 0 Research - Guided Workflow / UI / State Machine

Scope: future `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` contracts only. No guided UI is implemented in Phase 0.

## Sources Checked

- Android UI layer and state holders:
  https://developer.android.com/topic/architecture/ui-layer
  - Relevant because future guided mode needs recoverable screen state and clear state ownership.
  - Decision affected: guided confirmation states should be explicit state-machine data, not hidden composable state.
  - Do not adopt: adding a new state-management framework.

- Jetpack Compose state:
  https://developer.android.com/develop/ui/compose/state
  - Relevant because future confirmation UI must survive recomposition and expose immutable UI state.
  - Decision affected: Phase 0 defines modes/gates before adding UI.
  - Do not adopt: UI implementation in this phase.

- Jetpack Compose state hoisting:
  https://developer.android.com/develop/ui/compose/state-hoisting
  - Relevant because graphPanel, plotArea, calibration, trace, and peak confirmation should be owned by screen-level or workflow-level state holders.
  - Decision affected: contracts are mode-first and confirmation-aware.
  - Do not adopt: passing view models deeply through every composable.

- Jetpack Compose pointer input:
  https://developer.android.com/develop/ui/compose/touch-input/pointer-input
  - Relevant for future zoomable image annotation.
  - Decision affected: guided UI must be designed for precise gestures later.
  - Do not adopt: manual annotation UI in Phase 0.

## Phase 0 Decisions

- `AUTO_DIAGNOSTIC`, `GUIDED_PRODUCTION`, and `MANUAL_ADVANCED` are first-class product modes.
- Future user confirmations map to `USER_CONFIRMED` gate status.
- A future guided report can become release-ready only when confirmations replace missing or weak automatic evidence.

## Explicit Non-Adoptions

- No graph confirmation UI.
- No manual calibration editor.
- No trace/peak editing surface.
