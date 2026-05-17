# Chromatogram Deterministic CV Core Review

Status: Phase 8.3c.5c.2 technical review.

## Decision

The current direction is scientifically correct only if VLM is kept out of the
numeric pixel geometry loop. VLM can help with semantic recognition, titles, ION
labels, axis captions, and uncertainty explanations, but it must not be the primary
engine for:

- graph boundaries;
- axis line positions;
- tick pixel positions;
- pixel-to-unit calibration;
- curve centerline extraction;
- peak height, width, area, and integration.

The next chromatography slice should pivot from "make VLM axis crops smaller" to a
deterministic CV geometry core. Narrow VLM crops are still useful later, but only after
the app has already produced exact graph/axis/tick/trace artifacts.

## Current Repository Findings

### What Is Correct

- `docs/CHROMATOGRAM_CALIBRATION_BENCH_PLAN.md` already defines separate `graph panel
  bounds` and `plot area bounds`. This is the right contract: the full panel preserves
  labels and tick values, while the plot area drives pixel-to-unit calculation.
- `docs/MODEL_ASSISTED_STAGE_CONTRACT.md` already says model output cannot create
  peak heights, areas, FWHM, baseline, signal-to-noise, Kovats values, or compound IDs.
- The bench fixture set is strong enough to expose the real failure modes: screenshots,
  printed photos, rotated pages, multiple graphs, stacked traces, and weak ion channels.
- The report pipeline blocks honestly when axis calibration is not trustworthy instead
  of fabricating values.

### What Is Missing Or Too Weak

- `composeApp/src/desktopMain/kotlin/com/chromalab/feature/processing/perspective/PerspectiveWarper.desktop.kt`
  currently copies the input image and returns an identity homography. Desktop tests
  therefore do not validate the same perspective correction that Android is expected
  to perform.
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/perspective/PerspectiveWarper.android.kt`
  implements a custom warp in Kotlin. It is auditable, but it is not a proven shared
  geometry backend and is hard to compare against mature CV implementations.
- `GraphRegionDetector.desktop.kt`, `AxisDetector.desktop.kt`, and
  `GraphPlotAreaDetector.kt` use hand-written projections, dark-run scans, thresholds,
  and padding rules. This is useful as a prototype, but it lacks a formal line model,
  RANSAC/homography residuals, connected-component statistics, subpixel refinement, and
  distortion metrics.
- `AxisOcrReader.desktop.kt` asks a VLM to return both tick values and normalized tick
  positions. This is the wrong long-term contract. CV should find tick pixel positions;
  OCR/VLM should only read the printed values attached to those positions.
- The build currently has ML Kit text recognition and Google Play Services document
  scanner dependencies, but no OpenCV or BoofCV dependency. That means the app is still
  doing core geometry through custom Kotlin loops or Google-service-adjacent scanner
  behavior.

## External Technical Review

### OpenCV

OpenCV is the strongest candidate for the deterministic geometry core because it
covers the exact operations ChromaLab needs:

- per-pixel image processing, contours, drawing, and geometry transforms in `imgproc`;
- Canny plus Hough line transforms for straight axes, plot frames, and grid artifacts;
- Line Segment Detector for explicit line endpoints in grayscale ROIs;
- connected components with statistics for text/artifact/trace component filtering;
- homography, camera calibration, undistortion, and remapping in `calib3d`.

This does not mean ChromaLab should blindly rewrite everything. It means the next
implementation should add a platform CV adapter and benchmark it against the current
Kotlin detectors on the eight fixtures before replacing stable pieces.

### BoofCV

BoofCV is a credible Java/Android alternative. It is open source, Java-first, and
includes low-level image processing, geometric vision, calibration, detection, and
Android support. It may be easier to keep in a JVM/Kotlin codebase than native OpenCV,
but OpenCV has broader documentation, Java bindings, and more standard primitives for
Hough/LSD/homography workflows.

Decision: keep BoofCV as a fallback candidate, but test OpenCV first.

### Plot Digitizer Tools

WebPlotDigitizer, PlotDigitizer, Engauge Digitizer, and similar tools confirm the
right architecture:

1. determine plot type and axes;
2. calibrate pixel positions to data values;
3. apply automatic extraction or autotrace;
4. export numeric data.

They are useful as algorithm and UX references. They are not a direct dependency target
for the Android app because licensing, runtime stack, and manual-first workflows do not
match ChromaLab's offline mobile product.

### OCR

OCR should be scoped narrowly:

- detect tick marks and tick-label zones with CV;
- run OCR only on small, rectified label crops;
- match OCR values back to deterministic tick positions;
- robustly fit a linear axis transform and reject poor residuals.

ML Kit bundled text recognition is acceptable for devices without reliable Google Play
Services because the bundled model is statically linked at build time. The unbundled
Google Play Services path is not acceptable as the only path on GMS-poor devices.
PaddleOCR mobile deployment is worth a later experiment for offline OCR, but it is a
larger integration than CV geometry and should not block the geometry core.

## Proposed Deterministic Pipeline

### 1. Decode, Orientation, And Lens Context

- Decode once into a shared pixel container.
- Apply EXIF/right-angle orientation.
- Record camera/device metadata when available.
- Later: optionally apply saved camera lens calibration if the device has a known
  profile. Do not require the user to calibrate a camera for normal analysis.

### 2. Page And Plot Rectification

- Detect document/page quadrilateral when a sheet is visible.
- Detect plot-frame quadrilateral independently when the graph frame is visible.
- Compute homography with a mature CV backend.
- Persist homography matrix, source corners, corrected corners, max warp distance, and
  residual line-straightness error.
- Desktop and Android must use the same algorithmic contract.

### 3. Graph Panel Detection

- Use connected components, contours, horizontal/vertical line evidence, and projection
  profiles to find all graph panels.
- Preserve the full panel: title/ION, axis labels, tick values, and plot frame.
- Rank candidates by line geometry, text layout, area, aspect ratio, and expected
  chromatogram density.
- Reject only when the graph is genuinely not analyzable; do not make the user manually
  move the phone to satisfy internal heuristics.

### 4. Plot Area, Axes, And Tick Geometry

- Detect axis/frame line segments with HoughLinesP or Line Segment Detector.
- Fit horizontal and vertical axes with robust line fitting/RANSAC.
- Detect tick marks using small line segments and projection peaks near each axis.
- Refine axis/tick positions to subpixel or near-subpixel coordinates when possible.
- Produce deterministic tick pixel coordinates before OCR is called.

### 5. Tick Value OCR And Calibration

- Crop only tick-label bands around already-detected tick positions.
- OCR reads text values; CV owns positions.
- Match OCR values to ticks using order, spacing, and unit rules.
- Fit axis transforms using robust linear regression.
- Record residuals:
  - X tick spacing residual in pixels and units;
  - Y tick spacing residual in pixels and units;
  - skew angle;
  - nonlinearity score;
  - confidence per anchor.
- Block the report if residuals exceed thresholds.

### 6. Distortion Compensation

- First-order correction: homography for planar perspective.
- Lens correction: optional undistort/remap from device profile or estimated straight
  frame lines.
- Residual correction: estimate remaining non-linear deformation from tick spacing and
  frame straightness. If residual distortion is small, compensate in the pixel-to-unit
  transform. If it is large, block with a clear technical warning instead of producing
  false numbers.

### 7. Trace Extraction

- Build multiple masks: source, contrast, scan-style, adaptive threshold, edge, and
  artifact-suppressed.
- Remove axes, tick marks, labels, grid/frame residue, bleed-through, and page text as
  separate auditable masks.
- Skeletonize or thin the trace so the signal centerline is not dependent on stroke
  thickness.
- Extract a per-column centerline/envelope with confidence, not just one topmost pixel.
- Preserve vertical spike geometry for chromatographic peaks.

### 8. Peak Calculation And Report

- Convert pixels to calibrated units only after calibration is ready.
- Run deterministic peak detection, boundary detection, integration, and metrics.
- Keep VLM out of numeric calculation.
- Use VLM/local knowledge only for title/ION cleanup, interpretation hypotheses, and
  human-readable report wording.

## Accuracy Metrics To Add

Every fixture run should emit these metrics before we call the pipeline stable:

| Stage | Required metric |
| --- | --- |
| Graph panel | IoU or boundary error against fixture review boxes. |
| Plot area | Axis endpoint error and frame-line residual. |
| Perspective | Homography residual, skew angle, straight-line residual, warp distance. |
| Tick geometry | Tick count, tick pixel error, missing tick count, false tick count. |
| OCR | Text confidence, numeric parse rate, value/order consistency. |
| Calibration | Linear-fit residual, unit detection, nonlinearity score. |
| Curve | Column coverage, skeleton continuity, artifact ratio, trace confidence. |
| Peaks | Retention-time error, height error, width/FWHM error, false/missed peaks. |

## Dependency Recommendation

### Primary Candidate

Add a narrow OpenCV adapter behind common interfaces, then benchmark it before replacing
the current detectors:

- document/page contour detection;
- homography/warpPerspective;
- HoughLinesP or LineSegmentDetector for axes and frames;
- morphology/adaptive threshold;
- connectedComponentsWithStats;
- optional undistort/remap later.

### Secondary Candidate

Evaluate BoofCV only if OpenCV packaging, APK size, or native loading becomes a major
problem. BoofCV is still a serious option because it is Java-first and supports Android,
but it should not split the architecture before OpenCV is measured.

### Not Recommended As Core

- VLM for pixel positions.
- Google Play Services document scanner as the only preprocessing path.
- Pure manual calibration as the normal user flow.
- Threshold loosening without artifact and distortion evidence.

## Next Work Slices

### Phase 8.3c.5c.3 - CV Geometry Adapter Spike

- Add a small platform CV adapter contract without changing report behavior.
- Prototype OpenCV-backed desktop operations for one fixture:
  - read image;
  - detect long line segments;
  - detect connected components;
  - write axis/frame candidate overlays.
- Compare runtime and artifact quality against the current Kotlin detector.

### Phase 8.3c.5c.4 - Real Shared Perspective Contract

- Done: replace desktop identity perspective correction with measured homography
  output.
- Done: add platform-neutral `perspectiveGeometry` audit fields for document trust,
  graph-panel count, plot-area count, corner displacement, skew, and residual-metric
  gating.
- Done: verify `bench_03`, `bench_06`, and `bench_07` through executable fixture
  tests and persistent desktop artifacts.

### Phase 8.3c.5c.5 - Production CV Document/Plot Quadrilateral Candidates

- Done: fill the `perspectiveGeometry` contract with document, graph-panel, and
  plot-area quadrilateral candidates.
- Done: add aggregate residual metrics for candidate counts, accepted plot candidates,
  corner displacement, skew, orthogonality, straightness, and plot-area ratio.
- Done: verify the contract on the full fixture gate plus persistent clean,
  two-graph, and rotated artifacts.

### Phase 8.3c.5c.6 - OpenCV-backed Document/Plot Quadrilateral Detector Benchmark

- Implement an OpenCV-backed desktop candidate source for document/page contours and
  plot-frame line evidence.
- Compare it against the current platform-neutral quadrilateral contract.
- Keep BoofCV as the fallback candidate if OpenCV packaging blocks Android parity.

### Phase 8.3c.5c.7 - Deterministic Axis And Tick Geometry

- Detect axes and ticks through CV geometry after plot geometry is trusted.
- Persist tick-position overlays.
- Stop asking VLM for tick positions.

### Phase 8.3c.5c.8 - OCR Values Only

- OCR only small tick-label crops.
- Match values to CV tick positions.
- Fit robust calibration and block on residual failures.

### Phase 8.3c.5c.9 - Skeleton Trace Extraction

- Add skeleton/thinning or equivalent trace-centerline extraction.
- Add distortion-aware signal conversion.
- Validate on best and worst fixtures before Android parity work.

## Bottom Line

We are not wrong to keep VLM strict and non-skippable for the model-assisted stages,
but we are currently asking it to do too much of the geometry. The fastest and most
accurate path is:

```text
OpenCV-style deterministic geometry -> OCR small numeric labels -> robust calibration
-> deterministic trace/peaks -> VLM only for semantic assistance and wording
```

This is both faster and more scientific than trying to make a VLM "look harder" at a
full or semi-full chromatogram image.

## Sources

- OpenCV imgproc module: https://docs.opencv.org/4.x/d7/da8/tutorial_table_of_content_imgproc.html
- OpenCV Hough Line Transform: https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
- OpenCV LineSegmentDetector Java API: https://docs.opencv.org/4.x/javadoc/org/opencv/imgproc/LineSegmentDetector.html
- OpenCV calib3d homography and undistortion APIs: https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html
- OpenCV Java `Imgproc` connected components, morphology, adaptive threshold, contours: https://docs.opencv.org/master/javadoc/org/opencv/imgproc/Imgproc.html
- ML Kit Text Recognition on Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- PaddleOCR on-device deployment: https://www.paddleocr.ai/main/en/version3.x/deployment/on_device_deployment.html
- WebPlotDigitizer GitHub: https://github.com/automeris-io/WebPlotDigitizer
- WebPlotDigitizer digitizing and automatic extraction docs: https://automeris.io/docs/digitize/
- BoofCV main page: https://boofcv.org/
- BoofCV Android support: https://boofcv.org/index.php?title=Android_support
