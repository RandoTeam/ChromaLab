# Phase 4 Research: Trace Overlay Confirmation

Date: 2026-05-20
Phase: 4 - Trace Overlay Confirmation
Scope: trace overlay review UX, curve/skeleton evidence quality, Compose overlay rendering, provenance, and accessibility.

## Source Quality Rule

Model knowledge may be outdated. This phase used current official documentation, maintained image-processing references, and standards-oriented provenance/accessibility sources. Weak blogs, unsourced examples, marketing claims, and outdated API snippets were rejected as implementation drivers.

## Sources Reviewed

| Source | Type | Relevance | Decision Impact | Not Adopted |
| --- | --- | --- | --- | --- |
| [Android Developers: Pointer input in Compose](https://developer.android.com/jetpack/compose/gestures) | Official docs | Documents gesture APIs including custom pointer input. | Trace overlay viewer uses custom zoom/pan handling for an image annotation surface. | Did not add manual trace drawing gestures in Phase 4. |
| [Android Developers: Graphics in Compose](https://developer.android.com/develop/ui/compose/graphics) | Official docs | Covers drawing custom graphics in Compose. | Trace polyline and points are drawn on a `Canvas` over the source image. | Did not add a new rendering dependency. |
| [Android Accessibility Help: Touch target size](https://support.google.com/accessibility/android/answer/7101858?hl=en) | Official guidance | Recommends 48 dp touch targets for interactive controls. | Bottom actions use Material controls with 48 dp height and text labels; state is not color-only. | No manual point editing handles were added in this phase. |
| [scikit-image skeletonize documentation](https://scikit-image.org/docs/0.25.x/auto_examples/edges/plot_skeleton.html) | Maintained image-processing docs | Describes skeletonization, medial-axis output, and line evidence sensitivity. | Trace quality model treats skeleton/centerline evidence as reviewable and requires coverage/gap/branch metrics instead of blind acceptance. | Did not implement new skeletonization algorithms. |
| [OpenCV ximgproc documentation](https://docs.opencv.org/4.x/javadoc/org/opencv/ximgproc/Ximgproc.html) | Maintained library docs | Documents thinning and local binarization tools relevant to trace centerline extraction. | Reinforced that Phase 4 should consume existing extraction evidence rather than rewrite extraction. | Did not add OpenCV ximgproc dependency or algorithm changes. |
| [ImageJ/Fiji AnalyzeSkeleton](https://imagej.net/imagej-wiki-static/AnalyzeSkeleton) | Maintained scientific image-analysis tool docs | Lists branch, endpoint, junction, and path metrics for skeleton review. | Phase 4 trace quality stores branch/component/gap metrics and marks fragmented/branchy traces review-grade. | Did not copy plugin logic or change curve extraction. |
| [W3C PROV Overview](https://www.w3.org/TR/prov-overview/) | Standards | Defines provenance concepts for entities, activities, and agents. | User trace confirmation records source trace id, timestamp, user/session provenance, artifacts, decision, warnings, plotArea, and calibration id. | Did not implement RDF/PROV serialization. |

## Implementation Decisions

- Do not change curve extraction. Phase 4 consumes auto-extracted trace points and quality metrics.
- If metrics are unavailable, store unknowns and downgrade to review rather than invent evidence.
- User can accept valid trace, accept review-grade trace, or reject trace.
- Trace confirmation requires confirmed plotArea and confirmed calibration because the trace will later feed calibrated signal review.
- Trace points outside plotArea are invalid.
- `AUTO_DIAGNOSTIC` cannot use guided trace confirmations as user-confirmed release evidence.

## Risks Carried Forward

- Manual trace drawing/editing is not implemented.
- Overlay artifact path remains nullable until evidence export integration saves rendered user-confirmation overlays.
- Real-device gesture/UI validation remains required before product release.
