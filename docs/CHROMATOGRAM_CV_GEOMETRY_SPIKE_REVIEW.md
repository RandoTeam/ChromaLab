# Chromatogram CV Geometry Spike Review

Status: Phase 8.3c.5c.3d artifact review and production-scope decision.

## Purpose

This review closes the desktop CV geometry spike before any production OpenCV or
BoofCV adapter is introduced. The goal is to decide what the spike proves, what it
does not prove, and which geometry responsibilities must move into a real shared CV
contract.

The reviewed spike artifacts are:

- `cv_geometry_audit.json`
- `cv_geometry_overlay.png`

The review fixtures are:

- `bench_03_small_tic_export`
- `bench_06_photo_two_graphs_page`
- `bench_07_rotated_page_photo`

## External Method Check

The checked external references point to the same architecture:

- OpenCV `HoughLinesP` and Line Segment Detector are appropriate for straight plot
  frame, axis, tick, and grid-line evidence, but only after edge/mask preprocessing.
- OpenCV connected components and contours are appropriate for component statistics,
  page/plot candidate filtering, and quadrilateral candidates.
- OpenCV perspective transforms are the right production primitive for photographed
  page and plot rectification.
- BoofCV remains a credible Java-first fallback for line and geometric-vision work,
  but should not split the architecture before OpenCV is measured.
- Plot digitizer tools keep calibration explicit: pixel positions and axes are
  geometry problems first; OCR or model stages should read values, not invent
  positions.

## Artifact Review

### `bench_03_small_tic_export`

Result: accepted as a clean proof that the spike can recover obvious plot geometry.

Observed audit:

- Graph count: `1`
- Region: full image fallback, `381x132`
- Horizontal line candidates: `6`
- Vertical line candidates: `5`
- Connected components: `60`
- Contour candidates: `10`
- Frame candidates: `12`
- Warnings: none

Best frame candidate:

- `x=22`, `y=3`, `width=356`, `height=108`
- Horizontal support: `2`
- Vertical support: `2`

Decision:

- This fixture is suitable as the clean baseline for frame and axis geometry.
- The spike correctly finds the plot box, but production code still needs tick and
  curve masks separated from text and labels.

### `bench_06_photo_two_graphs_page`

Result: accepted as useful evidence, but not production-ready line geometry.

Observed audit:

- Graph count: `2`
- Graph 1 region: `x=137`, `y=242`, `width=736`, `height=414`
- Graph 2 region: `x=131`, `y=649`, `width=785`, `height=511`
- Each graph emits frame candidates with no top-level warnings.

Visual review:

- The graph-panel bounds preserve the photographed page context, axes, labels, and
  early high peaks.
- The component-frame fallback is useful because it keeps the broad graph footprint.
- The horizontal and vertical line candidate lists are polluted by chromatogram peaks,
  tick labels, and lower-axis text. They cannot be consumed directly as axes.

Decision:

- The production detector must rank page/plot frame evidence separately from trace
  strokes.
- It must not treat every long vertical stroke as an axis or frame line because early
  tall peaks look like strong vertical lines.
- OpenCV/BoofCV integration should start with artifact-separated masks, not with
  threshold loosening.

### `bench_07_rotated_page_photo`

Result: accepted as evidence that orientation correction is feeding the geometry
stage, but production perspective/plot-frame logic is still required.

Observed audit:

- Graph count: `1`
- Region: `x=24`, `y=239`, `width=849`, `height=508`
- Frame candidate: `x=79`, `y=239`, `width=789`, `height=508`
- Horizontal line candidates: `80`
- Vertical line candidates: `80`
- Warnings: none

Visual review:

- The rotated source is corrected before the CV geometry spike runs.
- The graph footprint is usable and preserves the left axis and lower time labels.
- Line candidates are again dominated by peaks and bottom label/tick clutter.

Decision:

- Rotation handling is good enough for the next phase to focus on perspective and
  frame geometry.
- The real production adapter needs homography and residual metrics, not only
  axis-aligned bounding boxes.

## Production Scope Decision

The spike should remain a diagnostic tool. It is not accurate enough to replace the
existing detectors directly.

The production adapter should cover these responsibilities in order:

1. Page/document quadrilateral detection and perspective correction.
2. Graph-panel candidate detection using contours, connected components, line
   geometry, and page-layout evidence.
3. Plot-frame and axis detection from artifact-separated line masks.
4. Tick mark geometry from deterministic CV positions.
5. OCR value matching against already-known tick positions.
6. Residual metrics for skew, perspective, tick spacing, calibration fit, and
   uncorrected distortion.

The production adapter must not do these things:

- Use VLM output for pixel positions.
- Use manual calibration as the normal happy path.
- Accept weak threshold tuning as a substitute for artifact separation.
- Treat chromatogram peaks as frame/axis evidence.
- Save a final scientific report when geometry/calibration residuals are outside
  tolerance.

## Next Work Slice

Completed next slice: `Phase 8.3c.5c.4 - Real Shared Perspective And Plot Geometry Contract`.

Accepted result:

- Added an audit-visible `perspectiveGeometry` contract for document confidence,
  graph-panel count, plot-area count, perspective requirement, skew/corner metrics,
  and residual-metric gating.
- Replaced the desktop perspective identity-copy with measured homography warp output.
- Preserved Android parity by keeping the contract platform-neutral.
- Verified on:
  - clean `bench_03_small_tic_export`;
  - two-graph photographed `bench_06_photo_two_graphs_page`;
  - rotated photographed `bench_07_rotated_page_photo`.

Completed next slice: `Phase 8.3c.5c.5 - Production CV Document/Plot Quadrilateral Candidates And Residual Metrics`.

Accepted result:

- `perspectiveGeometry` now exposes document, graph-panel, and plot-area
  quadrilateral candidates with source, bounds, corners, area/aspect, skew,
  orthogonality, corner displacement, score, acceptance, and warnings.
- The contract now exposes aggregate residual metrics so OpenCV and BoofCV candidate
  backends can be compared without changing the report schema.
- Persistent fixture artifacts confirm:
  - `bench_03_small_tic_export`: 3 candidates, 1 accepted plot;
  - `bench_06_photo_two_graphs_page`: 5 candidates, 2 accepted plots;
  - `bench_07_rotated_page_photo`: 3 candidates, 1 accepted plot.

Completed next slice: `Phase 8.3c.5c.6 - OpenCV-backed Document/Plot Quadrilateral Detector Benchmark`.

Accepted result:

- Added a desktop OpenCV benchmark backend behind the existing
  `perspectiveGeometry` contract.
- Document candidates use contour approximation; plot-area benchmark candidates use
  Hough line support inside the already-detected graph panels.
- Android remains an explicit unsupported backend for this benchmark slice so native
  OpenCV packaging cannot silently change mobile behavior before parity review.
- Persistent fixture artifacts confirm OpenCV candidate sources on:
  - `bench_03_small_tic_export`: `5` total candidates, `2` accepted plot candidates;
  - `bench_06_photo_two_graphs_page`: `7` total candidates, `4` accepted plot candidates;
  - `bench_07_rotated_page_photo`: `5` total candidates, `2` accepted plot candidates.

Completed next slice: `Phase 8.3c.5c.7 - Deterministic Axis And Tick Geometry`.

Accepted result:

- Added an audit-visible `axisTickGeometry` block per graph.
- Desktop now records OpenCV Hough line evidence, axis/origin candidates,
  projection-derived X/Y tick positions, and whether the geometry is ready for OCR
  value matching.
- Android remains an explicit unsupported backend for this benchmark slice so native
  OpenCV packaging cannot silently change mobile behavior before parity review.
- Persistent fixture artifacts confirm:
  - `bench_03_small_tic_export`: axis geometry available, `117` line segments,
    X/Y tick positions still insufficient, so calibration remains blocked;
  - `bench_06_photo_two_graphs_page`: both graphs have axis geometry and X tick
    positions, Y ticks remain insufficient, so calibration remains blocked;
  - `bench_07_rotated_page_photo`: axis geometry and tick positions are ready for OCR
    value matching.

Completed next slice: `Phase 8.3c.5c.8 - OCR Values Only And Residual Calibration Fit`.

Accepted result:

- Axis calibration no longer treats OCR label bounding-box centers as pixel anchors.
- OCR output is scoped to numeric values and is matched against deterministic CV tick
  positions from `axisTickGeometry`.
- The audit now records finite X/Y fit residuals, residual ratios, and residual-fit
  readiness, so JSON/Markdown artifacts can diagnose poor calibration without
  crashing serialization.
- Persistent fixture artifacts confirm:
  - `bench_03_small_tic_export`: calibration remains blocked because tick/OCR evidence
    is insufficient;
  - `bench_06_photo_two_graphs_page`: both graphs remain blocked because Y tick/OCR
    evidence is insufficient;
  - `bench_07_rotated_page_photo`: tick geometry is ready, but calibration remains
    blocked until OCR values are available.

Next slice: `Phase 8.3c.5c.9 - Skeleton Trace Extraction`.

## Source Links

- OpenCV Hough Line Transform: https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
- OpenCV connected components, contours, and shape descriptors: https://docs.opencv.org/4.x/d3/dc0/group__imgproc__shape.html
- OpenCV geometric transforms: https://docs.opencv.org/4.x/da/d54/group__imgproc__transform.html
- OpenCV Java API: https://docs.opencv.org/4.x/javadoc/
- OpenPnP OpenCV Java packaging: https://github.com/openpnp/opencv
- BoofCV line detection example: https://boofcv.org/index.php?title=Example_Detect_Lines
- BoofCV project: https://boofcv.org/
- PlotDigitizer documentation: https://plotdigitizer.com/docs
- WebPlotDigitizer digitizing documentation: https://automeris.io/docs/digitize/
