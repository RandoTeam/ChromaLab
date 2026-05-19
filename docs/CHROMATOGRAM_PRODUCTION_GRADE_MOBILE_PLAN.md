# ChromaLab Production-Grade Mobile Chromatogram Plan

Status: active product contract.
Created: 2026-05-19.

## Core Rule

Treat the agent's built-in knowledge as stale for every phase. Before closing any
phase, the implementation owner must check current 2026 documentation,
repositories, and examples for the relevant methods. The research check is not a
formality: if current sources contradict the implementation direction, the phase
contract must be updated before code is accepted.

The goal is a mobile chromatogram analysis app that is honest, reproducible, and
evidence-backed on real phone photos and screenshots. Do not hide weak geometry,
weak calibration, or missing OCR behind a polished report.

`CalculationEngine` is not the primary rewrite target. The current critical path is
upstream:

```text
image input
  -> provenance
  -> normalization
  -> graphPanel
  -> plotArea
  -> axes and ticks
  -> local OCR / VLM text assistance
  -> calibration
  -> curve extraction
  -> peak calculation
  -> evidence-backed report
```

VLM/Gemma/LiteRT/GGUF may assist with rough ROI hints, local OCR, semantic labels,
overlay judging, and warning explanations. They must not provide numeric geometry,
peak RT, height, area, FWHM, S/N, Kovats values, or final scientific metrics.

## Closure Rules

- No phase is closed until it is tested on all eight bench fixtures.
- From phase 2 onward, closing a phase requires re-running all checks from every
  previous phase.
- A real phone failure becomes a new fixture or runtime validation case.
- If a stage is not `VALID` or `REVIEW`, the app must export diagnostic evidence
  and block release-quality scientific reporting.
- Each completed work slice needs a focused commit, validation summary, and docs
  update.

Minimum fixture gate:

| Fixture | Required role |
| --- | --- |
| `bench_01_mz71_screenshot_page` | Printed page photo with two Ion 217/218 graphs. |
| `bench_02_mz92_belyi_tigr` | m/z 92 phone screenshot and reference report-depth case. |
| `bench_03_small_tic_export` | Low-resolution TIC export with labeled peaks. |
| `bench_04_stacked_xic_resolution` | Four stacked XIC panels. |
| `bench_05_tic_plus_ions` | TIC plus ion traces and Russian labels. |
| `bench_06_photo_two_graphs_page` | Photographed page with two graph panels and perspective noise. |
| `bench_07_rotated_page_photo` | Rotated photographed page. |
| `bench_08_mz71_duplicate_candidate` | m/z 71 screenshot duplicate/crop consistency case. |

## Phase 0 - Baseline Audit And Reference Lock

Research gate: check current graph digitization, chromatogram extraction, OCR tick
pairing, OpenCV/BoofCV/scikit-image/WebPlotDigitizer/MOCCA/pyOpenMS methods.

Implement:

- Record current fixture identity, graph count, graphPanel, plotArea, axis/tick,
  OCR/calibration, curve extraction, peak/report status.
- Record real Android failures separately: ROI error, six pseudo-graphs,
  right-side graphPanel crop, missing overlays, diagnostic-only report.
- Keep a table of expected vs actual vs blocking stage.

Close only when:

- all eight desktop fixture audits exist;
- Android runtime evidence exists for the active real-device cases;
- validator JSON/Markdown exists where runtime evidence is exported;
- every non-ready graph has an explicit blocking stage.

Current status: desktop baseline captured on 2026-05-19 in
`artifacts/phase0_baseline_20260519`; Android runtime baseline remains open until a
fresh device package passes or fails with validator-readable evidence.

## Phase 1 - Image Input And Normalization

Research gate: check current mobile document scan, EXIF rotation, perspective-safe
preprocessing, adaptive thresholding, contrast/sharpen, and ROI pyramid methods.

Implement:

- Route camera, photo import, screenshot, ML Kit scan, and gallery through one
  provenance-aware pipeline.
- Preserve original and normalized image paths, source route, EXIF/applied
  rotation, scan metadata, warnings, and transform chain.
- Generate preprocessing variants and select by evidence score.
- Keep the user flow simple: the app does the correction work, not the user.

Close only when all eight fixtures preserve orientation/scale and all phase 0 checks
still pass.

## Phase 2 - GraphPanel, Multi-Graph, And Deduplication

Research gate: check current chart panel detection, connected components,
projection profiles, line/frame detection, layout segmentation, and chart OCR
pipelines.

Implement:

- Treat graphPanel as the full graph block: title, ion/channel, axes, tick labels,
  captions, and frame.
- Resolve one physical graph to one report.
- Reject overlapping, nested, subregion, same-axis, and peak-cluster candidates.
- Support both embedded white panels in phone screenshots and already-cropped chart
  panels.

Close only when the multi-panel fixtures split correctly, `bench_08` remains one
graph, real screenshot cases do not create duplicates, and phases 0-1 still pass.

## Phase 3 - PlotArea, Axes, And Ticks

Research gate: check current Hough/projection/subpixel line fitting, tick
localization, grid/frame separation, and chart axis extraction methods.

Implement:

- Derive plotArea inside graphPanel and exclude title, ion text, tick labels, axis
  captions, and page margins.
- Detect axes and tick pixel positions by deterministic CV only.
- Reject or expand candidates that lose axes, tick labels, first peaks, or left/bottom
  context.

Close only when graphPanel/plotArea overlays are visually auditable for every graph
and phases 0-2 still pass.

## Phase 4 - OCR, Text Classification, And Calibration

Research gate: check current local OCR crop strategies, numeric tick parsing,
robust regression/RANSAC, and OCR confidence handling.

Implement:

- Read OCR from local crops around deterministic tick positions and text regions.
- Classify text as `PEAK_ANNOTATION`, `TICK_LABEL`, `AXIS_LABEL`,
  `TITLE_OR_CHANNEL`, `PAGE_TEXT`, or `UNKNOWN_TEXT`.
- Ensure title/channel ranges such as `Ion 71.00 (70.70 to 71.70)` never become peak
  labels.
- Fit calibration from accepted anchors with residuals, RMSE, R2, and
  `VALID`/`REVIEW`/`INVALID` status.

Close only when OCR values without deterministic tick pixels cannot enter
calibration, invalid calibration blocks scientific reporting, and phases 0-3 still
pass.

## Phase 5 - Curve Extraction And Signal Reconstruction

Research gate: check current graph digitizer algorithms, skeletonization/thinning,
fragmented trace reconstruction, text/grid suppression, and line tracing methods.

Implement:

- Run curve extraction only on validated/review plotArea.
- Suppress text, grid, frame, and tick artifacts before selecting the signal.
- Score trace candidates by coverage, gaps, component count, branch points, frame
  touch, text contamination, and confidence.
- Use REVIEW/DIAGNOSTIC for weak or fragmented traces instead of silent success.

Close only when selected centerline overlays match the real trace on every fixture
and phases 0-4 still pass.

## Phase 6 - Peak Detection, Integration, And Recovered Labels

Research gate: check current chromatographic baseline correction, S/N, prominence,
FWHM, overlap/shoulder handling, pyOpenMS/MOCCA/SciPy peak methods.

Implement:

- Keep `CalculationEngine` stable unless upstream evidence proves a math defect.
- Verify that boundary method, clamp negative, max width, and integration mode affect
  calculation inputs/results as configured.
- Treat PeakLabelEvidence as RT hints only.
- Recover a labeled peak only after local signal maximum/shoulder verification.
- Keep `FIXTURE_HINT` test-only and never production-reportable.

Close only when peak overlays show apex alignment, integration windows, rejected
artifacts, and phases 0-5 still pass.

## Phase 7 - VLM/Gemma/LiteRT Role

Research gate: check current official LiteRT/Gemma/Gallery docs, mobile VLM examples,
vision OCR limitations, and 2026 on-device model practices.

Implement:

- Restrict model output to hints, local OCR, semantic metadata, overlay judging, and
  warnings.
- Let deterministic geometry win when it validates and VLM disagrees.
- Continue deterministic valid paths when VLM fails.

Close only when VLM failures do not block obvious deterministic ROI, VLM numeric
geometry is impossible, and phases 0-6 still pass.

## Phase 8 - Runtime Evidence, Exports, And Report UI

Research gate: check Android Storage Access Framework, share/export workflows,
PDF/HTML/CSV generation, and scientific report UI examples.

Implement:

- Export evidence for all terminal states: PASS, REVIEW, DIAGNOSTIC_ONLY,
  ROI_FAILURE, CALIBRATION_FAILURE, CURVE_FAILURE, FATAL.
- Make peak table, full report, HTML, PDF, JSON, Markdown, share, and save buttons
  work end to end.
- Render professional report sections: graph, preparation, calibration, peaks,
  quality, model/runtime metadata, timings, warnings, and appendix.

Close only when every export opens with correct data and phases 0-7 still pass.

## Phase 9 - Android Performance, Stability, And Device Validation

Research gate: check Android image-processing optimization, OpenCV NEON, LiteRT
acceleration, llama.cpp/Vulkan mobile behavior, memory and thermal best practices.

Implement:

- Validate weak and strong Android devices.
- Preserve scientific depth; do not degrade analysis silently for weak devices.
- Log stage timings, memory, model runtime, timeouts, and thermal/retry warnings.
- Lock portrait orientation and prevent the app from enabling system auto-rotate.

Close only when real phones produce validator-readable evidence and phases 0-8 still
pass.

## Phase 10 - Release Gate

Research gate: check current dependency versions, Android signing, GitHub release
practices, and release artifact expectations.

Implement:

- Run full desktop fixture gate, targeted geometry/evidence/export tests, Android
  build, and manual Android validation.
- Update README, roadmap, pipeline docs, and release notes.
- Clearly mark production-ready, review-grade, experimental, and known limitations.

Close only when all fixture gates and real-device evidence gates are satisfied for
the release scope.

