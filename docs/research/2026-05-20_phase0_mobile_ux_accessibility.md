# Phase 0 Research - Mobile UX / Accessibility

Date: 2026-05-20
Phase: Phase 0
Agents: Mobile UX Architect Agent, Visual Design System Agent, Accessibility & Localization Agent
Skills: `current-web-research-deep`, `source-quality-triage`, `mobile-ux-flow-design`, `plain-language-errors`
Research confidence: HIGH for future UI constraints

## Research Question

How should Phase 0 define future product-mode UX without implementing Guided UI?

## Sources Reviewed

| Source | Tier | Relevance | Limitation | Decision impact |
| --- | --- | --- | --- | --- |
| [Jetpack Compose state saving](https://developer.android.com/develop/ui/compose/state-saving) | Official docs | Future guided workflow must persist user confirmation state. | Phase 0 does not build guided UI. | Guided/manual confirmations must be explicit and restorable. |
| [Compose state and state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting) | Official docs | Clear ownership of workflow state. | No implementation in Phase 0. | Product mode state should be shared/contract-level, not hidden UI state. |
| [Compose Multiplatform and Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html) | Official docs | ChromaLab targets Android and desktop/KMP. | General architecture reference. | Future guided contracts should stay common where practical. |
| [Android touch target guidance](https://support.google.com/accessibility/android/answer/7101858) | Official guidance | Future calibration handles/buttons must be usable. | Not a ChromaLab-specific UI spec. | Guided UI phases need accessibility review. |
| [WCAG 2.2](https://www.w3.org/TR/WCAG22/) | Standard | Contrast, target size, status messages, and user-facing report readability. | Web standard; must be adapted to Compose. | Report status labels must be clear and accessible. |
| [WebPlotDigitizer docs](https://plotdigitizer.com/docs) | Product docs | Shows calibration/extraction workflows and zoom panel patterns. | Not authoritative for ChromaLab accuracy. | Background UX reference for future Guided UI only. |

## Findings

- Product modes must be visible in report/evidence status, not only internal metadata.
- Future guided workflows need persistent, explicit confirmation of graphPanel, plotArea, calibration anchors, trace, and peaks.
- Diagnostic-only or review-only states should use plain language and accessible visual treatment.

## Phase 0 Decision

Phase 0 documents modes and report gates only. It does not implement Guided UI, manual calibration, trace editing, or peak editing.

## Rejected Approaches

- Building Guided UI before Phase 0 gates are closed.
- Presenting diagnostic reports with the same visual weight as release-ready reports.
- Hiding review/invalid states behind technical logs only.

## Required Validation

- Future UI phases must include UX/design/accessibility agents.
- Report UI changes must verify mode/gate labels, touch targets, text contrast, and localization.
