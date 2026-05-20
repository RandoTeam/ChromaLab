# Guided Trace Overlay UX

Phase 4 adds a guided review screen for the extracted trace. The user checks whether the trace line follows the chromatogram signal inside the confirmed plotArea.

## User Flow

1. User reaches trace review after graphPanel, plotArea, and calibration are confirmed.
2. The normalized image is shown with graphPanel and plotArea context.
3. The auto-extracted trace is drawn as a distinct pink polyline with point samples.
4. The screen displays trace status, point count, and coverage.
5. Warnings explain sparse coverage, large gaps, branch structure, missing metrics, or invalid points.
6. User chooses:
   - accept valid trace;
   - accept review-grade trace;
   - reject trace;
   - reset to original auto suggestion.
7. The screen shows Phase 5 peak review as disabled handoff.

## Interaction Rules

- Zoom/pan is supported for visual inspection.
- Trace editing/drawing is not supported in Phase 4.
- Rejecting trace records a rejection reason and blocks release.
- Accepting review-grade trace preserves warnings and keeps report gating review-grade.
- Accepting valid trace is enabled only when deterministic quality evaluation is `VALID`.

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
