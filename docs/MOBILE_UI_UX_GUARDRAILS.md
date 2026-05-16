# Mobile UI/UX Guardrails

ChromaLab is a consumer-facing scientific tool. The internal pipeline can be strict and technical, but the mobile experience must stay simple, visual, and guided.

## Product Principle

The user is not responsible for satisfying the pipeline manually.

A normal user or scientist should be able to:

- open the camera or select a photo;
- roughly frame the chromatogram;
- let the app crop, normalize, deskew, filter, detect axes, calibrate, calculate, and report;
- receive a clear result or a clear reason why the analysis cannot be trusted.

The app must not require the user to move the phone "a little left", "a little right", or manually tune technical parameters just to satisfy internal OCR/calibration behavior.

## Mobile Design Direction

Before implementing major UI changes, the app design must be checked against mobile design practices, especially:

- Material 3 / Jetpack Compose mobile patterns already used in the app;
- touch-first controls with large, reachable hit targets;
- short primary actions and progressive disclosure for advanced details;
- readable typography and tables on phone screens;
- responsive layouts for small and large Android devices;
- clear loading, processing, retry, error, and success states;
- accessibility basics: labels, contrast, focus order, and text scaling.

Do not implement a generic "Codex-looking" screen full of text blocks. The UI must fit ChromaLab's existing mobile app style.

## Camera And Photo Flow

The capture flow should feel like an assisted scanner:

- show a clean camera/photo entry point;
- show minimal framing guidance, not technical instructions;
- run automatic crop, perspective correction, normalization, filter sweep, OCR, and graph detection;
- keep filter/crop/preparation details as internal audit data;
- stop with a clear retake/review state when automatic preparation cannot produce a trustworthy graph.

Normal chromatogram analysis must not open manual X/Y axis calibration as a required
user step. Axis calibration is an internal audited stage: it either succeeds
automatically from graph geometry and tick OCR, or the analysis is blocked without a
saved report. Manual axis tooling may exist only as a diagnostic/developer workflow
until a separate guided review mode is intentionally designed.

The user-facing screen should show a compact state such as:

- ready to scan;
- improving image;
- locating graph;
- reading axes;
- calculating peaks;
- report ready;
- retake needed;
- manual review needed.

## Warnings And Audit Data

Technical warnings are required for correctness, but they are not the primary UI.

User-facing presentation:

- show one compact quality state or warning summary;
- explain the next action in human language;
- avoid long raw warning lists on the main screen;
- keep full warning codes, candidate geometry, OCR confidence, transform details, and stage timings in the technical appendix.

Examples:

- Main UI: "Axis labels are unclear. Retake or review crop."
- Technical appendix: `axis.x.localized_ticks_missing`, candidate points, OCR confidence, transform status.

## Report UI

The final report must be professional and readable, not terminal-like.

The Belyi Tigr Ion 92 reference fixture is the visual and structural target for
future report work. The existing interim report/export UI is not a completed
implementation of that target.

Required direction:

- structured sections with clear hierarchy;
- readable peak tables with horizontal handling on small screens;
- graph preview/overlay near the related data;
- model/runtime/time metadata in a compact header or appendix;
- warnings grouped by severity and meaning;
- technical appendix available but visually secondary;
- no wall of raw JSON, debug logs, or unformatted Markdown as the default mobile report view.

## Theme And Orientation

ChromaLab exposes theme mode as a normal user setting:

- follow Android system;
- force light theme;
- force dark theme.

Until landscape layouts are intentionally designed and validated, the Android app runs
in portrait orientation. Landscape QA can still be used diagnostically, but it should
not imply that the production phone UI is ready to rotate freely.

## Implementation Rule

When a future phase adds or changes UI:

- inspect existing screens, Material 3 components, spacing, typography, and navigation first;
- keep the first screen focused on the primary user task;
- hide advanced/internal data behind details, accordion sections, bottom sheets, or appendix views;
- validate on phone-sized layouts before considering the task complete;
- do not weaken scientific analysis to make the UI simpler.
