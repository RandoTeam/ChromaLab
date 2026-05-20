# Assisted Trace Overlay UX

Phase 4 adds an Assisted Review screen for extracted trace evidence. The user checks whether the trace line follows the chromatogram signal inside the plotArea only when autonomous trace evidence is review-grade, invalid, or explicitly opened for inspection.

This screen is not the default production path. `AUTONOMOUS_PRODUCTION` should first attempt to pass trace gates automatically. The UI is a fallback and evidence-review tool.

## User Flow

1. Autonomous analysis attempts trace extraction and quality scoring first.
2. If trace quality is `REVIEW`/`INVALID`, or the user opens evidence inspection, the trace overlay screen is shown.
3. The normalized image is shown with graphPanel and plotArea context.
4. The auto-extracted trace is drawn as a distinct pink polyline with point samples.
5. The screen displays trace status, point count, and coverage.
6. Warnings explain sparse coverage, large gaps, branch structure, missing metrics, or invalid points.
7. User chooses:
   - accept valid trace;
   - accept review-grade trace;
   - reject trace;
   - reset to original auto suggestion.
8. The screen shows Phase 5 peak review as disabled handoff.

## Interaction Rules

- Zoom/pan is supported for visual inspection.
- Trace editing/drawing is not supported in Phase 4.
- Rejecting trace records a rejection reason and blocks release.
- Accepting review-grade trace preserves warnings and keeps report gating review-grade.
- Accepting valid trace as user-confirmed is enabled only when deterministic quality evaluation is `VALID`.
- Automatic valid trace evidence can satisfy `AUTONOMOUS_PRODUCTION` trace gate without this screen.

## Visual Design

- graphPanel context: cyan outline;
- plotArea: green translucent fill and outline;
- trace overlay: pink polyline with sampled points;
- status chips: `VALID`, `REVIEW`, `INVALID`, or missing;
- warnings are textual and icon-backed, not color-only.

The UI stays focused on verification, not editing. This avoids implying the trace can be manually repaired in Phase 4.

## Accessibility and Localization

- Primary actions are standard Material buttons with 48 dp height.
- State is communicated with text labels plus color.
- The image overlay has a content description.
- Russian and English strings are provided through the component string helper.

## Handoff

After confirmation, guided state moves to `TRACE_CONFIRMED`. Phase 5 may consume the confirmed/review/rejected trace and implement peak review. Phase 4 does not mark peaks as reviewed.
