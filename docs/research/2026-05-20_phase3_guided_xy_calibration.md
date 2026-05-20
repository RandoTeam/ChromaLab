# Phase 3 Research: Guided X/Y Calibration

Date: 2026-05-20
Phase: 3 - Guided X/Y Calibration
Scope: graph digitization calibration UX, Compose/KMP point placement, persistence, manual calibration provenance, and release-gate implications.

## Source Quality Rule

Model knowledge may be outdated. This phase used current official documentation, maintained project documentation/repositories, and standards-oriented sources. Weak blogs, uncited examples, marketing claims, and outdated API snippets were not used to drive implementation.

## Sources Reviewed

| Source | Type | Relevance | Decision Impact | Not Adopted |
| --- | --- | --- | --- | --- |
| [Android Developers: Pointer input in Compose](https://developer.android.com/jetpack/compose/gestures) | Official docs | Documents `pointerInput`, tap, drag, and transform gesture detectors. | Calibration editor uses explicit tap/drag/transform handling over the image because anchor placement is a custom gesture surface. | Did not rely on platform `Button`-level gestures for image points because anchors require coordinate conversion. |
| [Android Developers: Understand gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures) | Official docs | Explains gesture abstraction levels and custom pointer input. | Kept gesture handling localized in a reusable image editor component instead of changing app navigation/runtime. | Did not implement advanced multi-pointer arbitration beyond zoom/pan and anchor drag in this phase. |
| [Android Developers: Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving?hl=en) | Official docs | Covers `rememberSaveable`, state holders, and restoring UI element state. | Phase 3 keeps calibration editor state serializable and reducer-backed; future screen integration can persist it through the existing guided state plan. | Did not store heavy images or generated masks in Compose saved state. |
| [Android Developers: State lifespans in Compose](https://developer.android.com/develop/ui/compose/state-lifespans?hl=en) | Official docs | Distinguishes remembered state, retained state, and saved/restored state. | Snapshot state remains lightweight: bounds, anchors, values, selected anchor, and labels. | Did not add a new app-level persistence framework. |
| [Kotlin Serialization Documentation](https://kotlinlang.org/docs/serialization.html) | Official docs | Confirms `@Serializable` model classes and JSON roundtrip patterns. | New calibration editor snapshot/evaluation contracts are serializable; tests verify roundtrip through guided state. | Did not introduce custom serializers. |
| [W3C PROV Overview](https://www.w3.org/TR/prov-overview/) and [PROV-DM](https://www.w3.org/TR/prov-dm/) | Standards | Provides provenance concepts for entities, activities, and agents. | Manual calibration records user/session provenance, timestamps, source, related image ID, warnings, and artifact path. | Did not implement RDF/PROV serialization; the project only needs structured audit fields now. |
| [WebPlotDigitizer](https://automeris.io/WebPlotDigitizer/) and [WebPlotDigitizer GitHub](https://github.com/automeris-io/WebPlotDigitizer) | Maintained graph digitization tool/repository | Confirms manual axis definition and assisted digitization remain normal for graph images. | User-confirmed anchors are first-class evidence; auto/VLM coordinates are not trusted as final numeric geometry. | Did not copy UI/assets or implement broad auto-extraction changes. |
| [PlotDigitizer documentation](https://plotdigitizer.com/docs) | Maintained product docs | Describes manual calibration markers and known-value entry for XY plots. | Phase 3 supports at least two anchors per axis and encourages three or more for residual review. | Did not adopt "points need not lie on axes" as a release rule yet; ChromaLab still requires plotArea-confirmed coordinate provenance. |
| [NIST Engineering Statistics Handbook: Linear Least Squares Regression](https://www.itl.nist.gov/div898/handbook/pmd/section1/pmd141.htm) | Authoritative statistics reference | Supports least-squares linear fit and residual interpretation. | Phase 3 reuses the existing `AxisCalibrationFitter` for slope/intercept, residuals, RMSE, R2, and review/invalid status. | Did not add new calibration math or nonlinear calibration in this phase. |

## Implementation Decisions

- Use reducer-backed state for anchor editing so tests can validate add/move/remove/value/axis operations without UI instrumentation.
- Reuse `AxisCalibrationFitter`; do not create a second calibration implementation.
- Two anchors per axis are enough for a linear transform but remain review-grade by default because residual quality cannot be assessed robustly.
- Three or more anchors per axis can become valid when monotonicity and residual thresholds pass.
- `AUTO_DIAGNOSTIC` cannot convert manual/user-confirmed anchors into release evidence. Guided/manual modes own user confirmation.
- Store provenance on every anchor and confirmed calibration: timestamp, source, related image, user/session provenance, residual report, warnings, and overlay artifact path.

## Risks Carried Forward

- The UI component is reusable but not yet wired into the main navigation flow.
- Future Phase 4/5 work must preserve calibration provenance when trace and peak review are added.
- Nonlinear/log axes are not implemented; current scope is linear chromatogram RT/intensity calibration.
