# Chromatogram 2026 Method Research Plan

Status: Phase 8.3c.5c.3b research contract.
Date: 2026-05-17.

This document defines the theoretical research plan for modernizing ChromaLab's
chromatogram analysis pipeline in 2026. It reviews methods and public solutions for
each stage from image input to final report, then maps them to current ChromaLab
weaknesses.

This is not an implementation slice. It is the research contract that decides what
must be tested before more code is added.

## Core Finding

The 2026 direction should be a deterministic, evidence-first hybrid pipeline:

```text
CV geometry and pixel evidence
  -> OCR for labels/tick values
  -> calibrated numeric signal
  -> deterministic chromatography calculations
  -> VLM only for semantic explanation and report language
```

The current risk is that ChromaLab still lets model/OCR output sit too close to pixel
geometry. That is not accurate enough for scientific numbers. VLMs can describe charts
well, but they should not own graph bounds, tick pixel positions, calibration anchors,
curve centerlines, or peak calculations.

## Sources Reviewed

Primary technical references checked for this plan:

- OpenCV 4.x: Hough line transforms, Line Segment Detector, connected components,
  contours, perspective transforms, camera calibration, Android OpenCL/T-API.
- BoofCV: JVM/Android image processing, geometric vision, calibration, and feature
  detection.
- Google ML Kit: Android Text Recognition v2 and model installation paths.
- Google AI Edge LiteRT: GPU/NPU delegates and Android on-device model execution.
- PaddleOCR 3.x / PP-OCRv5: mobile OCR, Android deployment, multilingual recognition.
- Tesseract 5 documentation as a fallback/offline OCR baseline.
- WebPlotDigitizer, PlotDigitizer, Plot Extractor, Engauge Digitizer as graph
  digitizer UX/architecture references.
- ChartOCR, LineEX, ChartLine, DePlot, Scatteract, Plot2Spectra papers as chart
  extraction references.
- SciPy signal peak detection, pybaselines, pyOpenMS/OpenMS, MZmine, MOCCA as
  calculation and chromatogram-processing references.

## Stage Review Matrix

### 0. Input Selection And Provenance

Modern expectation:

- Preserve original file, normalized file, source route, scanner/camera/gallery route,
  timestamps, device, resolution, orientation, filter, crop, and user/manual changes.
- Treat scanner output as one provider, not as proof that geometry is correct.
- Keep source provenance attached to every graph and report.

Current ChromaLab state:

- Android Smart Scan returns a final JPEG path.
- `ProcessingFlowScreen` saves photo outputs as `SourceType.PHOTO`.
- Smart Scan camera and Smart Scan gallery are not structurally separated.
- ML Kit crop/filter/deskew/editor provenance is not persisted.

What is not correct enough:

- The final report cannot prove how the image was prepared.
- Runtime can assume that Smart Scan prepared the image even when the stored metadata
  does not prove it.

What is missing:

- `SourcePreparationProvenance` contract.
- Per-provider preparation evidence: ML Kit, local CV, desktop import, future PDF.
- Explicit "scanner-provided" versus "ChromaLab-verified" geometry status.

Research tasks:

1. Define the minimum provenance schema needed for scientific audit.
2. Compare ML Kit scanner metadata availability with what ChromaLab can store.
3. Define source route states: camera scanner, scanner gallery, file image, PDF page,
   raw CSV/mzML, manual diagnostic.

Acceptance:

- Every report can answer: source route, original image, normalized image, preparation
  provider, preparation confidence, and whether ChromaLab verified geometry.

### 1. Decode, Resize, Orientation, And Memory

Modern expectation:

- Use predictable decode sizes and ROI pyramids.
- Avoid processing huge full-resolution images for every stage.
- Preserve EXIF orientation and record every rotation/crop/scale transform.
- Use per-stage preview/full-resolution policies.

Current ChromaLab state:

- `ImageNormalizer` and `OrientationCorrector` exist.
- Runtime records normalized dimensions, but not enough correction metadata for the
  final report.
- Desktop and Android correction parity is not fully proven.

What is not fast or modern enough:

- Any full-image repeated Kotlin pixel loop will become slow and memory-heavy on old
  devices.
- Orientation correction is useful, but it is not yet part of a full transform chain.

What is missing:

- Shared image pyramid/ROI decode policy.
- Transform chain from original pixels to normalized pixels to graph pixels.
- Memory budget per stage for weak devices.

Research tasks:

1. Benchmark full-image versus ROI/pyramid processing on the eight fixtures.
2. Define max working image dimensions for geometry, OCR, trace extraction, and final
   evidence crops.
3. Store all transforms as matrix or structured operations.

Acceptance:

- Every stage can map a pixel back to original image coordinates.
- Desktop and Android produce comparable dimensions and orientation results.

### 2. Document, Page, And Perspective Preparation

Modern expectation:

- Use deterministic document/page and plot-frame geometry before analysis.
- Use contours, connected components, Hough/LSD line segments, quadrilateral fitting,
  homography, and residual straightness metrics.
- Use camera calibration/undistortion when device profiles are available.

Current ChromaLab state:

- Android runtime skips internal document crop and perspective with fallback results.
- Desktop perspective correction currently behaves as identity/copy.
- ML Kit scanner can produce visually useful crops, but its internal crop/filter data
  is not persisted.

What is not correct enough:

- A no-op perspective stage can enter the report path.
- Desktop cannot fully validate the same geometry that Android needs.
- A photographed sheet with perspective skew can corrupt all downstream calibration.

What is missing:

- Shared `PageRectificationProvider` with measurable output.
- Homography matrix, source corners, corrected corners, straightness residuals, and
  confidence.
- Explicit stage block when perspective quality is required but not available.

Research tasks:

1. Compare OpenCV and BoofCV for page/plot quadrilateral detection.
2. Test HoughLinesP, LineSegmentDetector, contours, and connected components on all
   fixtures.
3. Define rectification quality metrics: frame straightness, opposite-line parallelism,
   corner residual, crop completeness.
4. Decide whether Android should use native OpenCV/BoofCV or a Kotlin adapter around a
   smaller subset.

Acceptance:

- The pipeline can prove whether the page/plot was rectified or blocked.
- Perspective correction is not silently replaced by identity when needed.

### 3. Graph Panel Detection And Multi-Graph Splitting

Modern expectation:

- Separate full graph panel bounds from plot area bounds.
- Detect all panels and preserve reading order.
- Rank candidates by line geometry, text layout, contour evidence, trace density,
  aspect ratio, and expected chromatogram structure.
- Keep accepted and rejected candidates as visual evidence.

Current ChromaLab state:

- `AutoSweepEngine` combines VLM region detection, CV graph detection/refinement, OCR,
  axis detection, and variant scoring.
- Bench work already distinguishes graph panel and plot area.
- Runtime still lets VLM participate too early in region ownership.

What is not correct enough:

- VLM graph-region output is not a scientific geometry primitive.
- Current custom detectors rely on projections and heuristics instead of a mature line
  and component model.
- Some bad photos can be detected partially, dropping first peaks or axis labels.

What is missing:

- Candidate overlay evidence for every detected/rejected graph panel.
- Connected-component statistics for graph/text/frame separation.
- Robust reading-order sort for rotated and multi-panel pages.

Research tasks:

1. Build an evaluation table for graph-panel recall/precision on the eight fixtures.
2. Compare current detector output with OpenCV line/contour/component candidates.
3. Study ChartOCR, LineEX, ChartLine, Plot Extractor, and WebPlotDigitizer style
   decomposition: plot area, axes, ticks, curves, text, legends.
4. Define a "do not reject user image too early" policy: block only at the stage that
   cannot be proven.

Acceptance:

- Every fixture gets panel candidates with accepted/rejected overlays.
- Full graph panel must include axis labels, tick values, titles/ION text, and first
  visible peaks.

### 4. Plot Area, Axes, Tick Geometry

Modern expectation:

- CV owns axis lines and tick pixel positions.
- OCR reads tick values only after CV has found tick positions.
- Axis transforms are fitted from multiple anchors with residuals, not from two
  guessed points.

Current ChromaLab state:

- Runtime uses OCR/VLM element centers as calibration anchors.
- X calibration uses first/last distinct tick labels.
- Y calibration uses highest tick plus origin/bottom fallback.
- Bench already blocks when axis calibration is not ready, but runtime and bench are
  not fully unified.

What is not correct enough:

- Tick-label center is not necessarily tick-mark position.
- Two-point calibration cannot detect OCR/tick mismatch or nonlinearity.
- Highest visible Y label is not always axis max.

What is missing:

- Deterministic tick mark detector.
- Tick-label-to-tick matching.
- Robust linear fit, residuals, outlier rejection, skew/nonlinearity warning.
- Calibration evidence in stored metadata and final report.

Research tasks:

1. Implement an experiment, not production code first, for:
   - horizontal and vertical axis fitting;
   - tick segment detection near axes;
   - label-band extraction around candidate ticks;
   - residual-based calibration.
2. Compare ordinary least squares, robust regression, and RANSAC-style anchor fitting.
3. Define failure thresholds for X and Y residuals separately.

Acceptance:

- The report never uses OCR text boxes directly as tick pixel anchors.
- At least three anchors per axis are preferred; two anchors are accepted only with an
  explicit reduced-confidence reason.

### 5. OCR For Labels, Tick Values, Title, ION

Modern expectation:

- Use narrow, rectified crops.
- Use OCR for text only: title, ION/channel, axis labels, tick values.
- Support bundled/offline OCR for devices without reliable Play Services.
- Keep OCR confidence, bounding boxes, language/model, and normalization rules.

Current ChromaLab state:

- ML Kit text dependencies exist.
- Model-assisted OCR can be used, but current calibration path is still too connected
  to OCR/VLM positions.
- GMS-poor devices are a known product target.

What is not modern enough:

- Relying on unbundled or Play-Services-delivered OCR/model behavior is risky for the
  Xiaomi Mi 8 class of devices.
- General VLM OCR is slower and less deterministic than specialized OCR for small
  tick-label crops.

What is missing:

- OCR provider abstraction with ML Kit bundled, PaddleOCR mobile experiment, and
  optional Tesseract fallback.
- Numeric tick normalization for Russian/English decimal separators, exponent notation,
  `x10^`, commas, spaces, and ion ranges.
- OCR result validator that checks axis ordering and tick spacing.

Research tasks:

1. Compare ML Kit bundled text recognition versus PaddleOCR PP-OCRv5 mobile on the
   eight fixture label crops.
2. Keep Tesseract 5 as a desktop/offline baseline, not the primary Android path.
3. Build a tick-value parser with confidence and correction rules.

Acceptance:

- OCR can fail without fabricating calibration.
- OCR output is attached to deterministic tick candidates and validated by spacing.

### 6. Distortion Compensation

Modern expectation:

- Correct planar perspective with homography.
- Correct lens distortion when device profile or calibration is available.
- Estimate residual distortion from frame straightness and tick spacing.
- Block if residual distortion makes numeric extraction unreliable.

Current ChromaLab state:

- Orientation handling exists.
- Perspective handling is incomplete and inconsistent between runtime and desktop.
- No stored residual distortion metrics are available in the report path.

What is missing:

- Device profile model.
- Homography plus optional undistort transform chain.
- Residual straightness and tick-spacing diagnostics.

Research tasks:

1. Define minimum distortion metrics from graph frame and axes.
2. Test whether homography alone is enough for current fixtures.
3. Decide if device-specific calibration should be optional advanced mode only.

Acceptance:

- Every report can say whether distortion was corrected, negligible, or too large.

### 7. Curve And Trace Extraction

Modern expectation:

- Extract trace from plot area after axes/ticks/frame/text are masked.
- Use multiple masks: grayscale, adaptive threshold, contrast, edge, artifact
  suppression, color/channel if available.
- Preserve skeleton/centerline and confidence per point.
- Keep raw and smoothed traces separately.

Current ChromaLab state:

- `CurveMaskPreparer`, `CurveExtractor`, and variant sweep exist.
- Scoring considers points, coverage, continuity, confidence, Y span, interpolation,
  outliers, and warnings.
- Artifact masks are not yet complete for all hard photos.

What is not accurate enough:

- If axes, labels, frame, text bleed-through, or page shadows survive masking, false
  peaks and missed peaks appear.
- Current trace scoring can be correct locally while globally missing important peaks.

What is missing:

- Artifact class masks: axes, ticks, labels, frame, page text, bleed-through,
  handwriting, and shadows.
- Trace completeness metrics against visible peak count.
- Accepted/rejected trace overlays stored per graph.

Research tasks:

1. Compare classical CV trace extraction with chart-specific research such as LineEX,
   ChartLine, ChartOCR, and Plot2Spectra.
2. Test skeletonization/centerline approaches against current extraction.
3. Add mask audit images before any threshold tuning.

Acceptance:

- Good and bad fixtures both produce trace overlays that explain missed and false
  peaks.
- First peaks and edge peaks are explicitly checked.

### 8. Signal Conversion And Data Persistence

Modern expectation:

- Convert curve pixels through confirmed calibration only.
- Store raw pixel curve, calibrated raw signal, smoothed signal, final signal,
  calibration anchors, residuals, and transform method.
- If raw digital data exists, prefer CSV/mzML/raw import over image digitization.

Current ChromaLab state:

- `SignalConverter` maps curve points through `PixelCalibration`.
- `ChromatogramEntity.dataPoints` stores final points.
- `ChromatogramEntity` stores intensity unit but not first-class time unit.

What is not correct enough:

- Later calculations can lose time-unit semantics.
- The final signal is stored, but not enough upstream evidence is stored for
  reproducible recalculation.

What is missing:

- Versioned `SignalEvidence` or equivalent metadata schema.
- Separate raw/smoothed/final signal channels.
- Time unit persistence.

Research tasks:

1. Define a backward-compatible signal evidence envelope.
2. Decide which artifacts stay in Room and which are file-backed.
3. Define report display rules for raw versus smoothed data.

Acceptance:

- Calculations can be rerun without repeating image analysis.
- Report can trace every peak to calibrated signal and source pixels.

### 9. Baseline, Peak Detection, Integration, And Metrics

Modern expectation:

- Baseline correction supports multiple algorithms and records selected method.
- Peak detection uses prominence, width, distance, S/N, and domain constraints.
- Overlap/deconvolution is handled explicitly for unresolved peaks.
- Integration and boundaries are auditable.

Current ChromaLab state:

- `CalculationEngine` is strong and deterministic.
- Boundary method, integration mode, clamp negative, and max peak width are applied.
- Metrics include retention/apex data, height, area, width, prominence, S/N, area
  percent, boundaries, and confidence.

What is still not complete:

- No benchmark matrix against established tools/methods yet.
- Overlap/deconvolution and peak purity are not at the level of dedicated packages
  such as MOCCA, MZmine/OpenMS, or pyOpenMS workflows.
- For GC-MS/LC-MS raw data, image-based digitization should not replace raw-data
  feature detection when raw data is available.

What is missing:

- Algorithm benchmark set against SciPy `find_peaks`, pybaselines, MZmine/OpenMS,
  MOCCA-style workflows, and synthetic fixtures.
- Deconvolution plan for overlapping peaks.
- Domain-aware homologous series checks for n-alkanes, alkylbenzenes, and target ions.

Research tasks:

1. Build a calculation benchmark with synthetic known-truth chromatograms and our
   eight image-derived fixtures.
2. Compare baseline algorithms: none, manual linear, ALS, SNIP, rolling/minimum,
   polynomial where appropriate.
3. Compare peak picking and boundary methods against SciPy-style prominence/width
   definitions.
4. Add explicit overlap/deconvolution research before changing default integration.

Acceptance:

- The engine is not tuned only by visual impression.
- Every algorithm setting has a fixture-backed reason.

### 10. Local Domain Knowledge And Chemical Interpretation

Modern expectation:

- Calculations produce numeric facts.
- Local curated knowledge maps ions, retention-index families, homologous series,
  expected patterns, and cautions.
- VLM may explain but cannot invent compound assignments.

Current ChromaLab state:

- Local knowledge pack exists for some ions and Kovats support.
- The report mapper distinguishes deterministic, imported, inferred, and unknown
  sources.

What is missing:

- Larger curated ion/channel knowledge base.
- Reference rules for gases, petroleum fractions, n-alkanes, alkylbenzenes, and other
  expected study categories.
- Data-source citations/versioning for knowledge entries.

Research tasks:

1. Define required knowledge schema for each chemical family.
2. Add confidence tiers: exact reference, family inference, pattern-only warning,
   not assignable.
3. Decide which assignments require raw MS spectrum rather than chromatogram image.

Acceptance:

- The app never claims a compound identity from image-only evidence unless the
  required reference data exists.

### 11. Final Report And Evidence UI

Modern expectation:

- Final report is readable like a professional lab/audit report, not terminal text.
- It contains source preparation, graph evidence, calibration, peak table, integration
  details, quality warnings, domain interpretation, and appendix.
- The UI is minimal for users, but the report remains scientifically traceable.

Current ChromaLab state:

- Structured report contract and preview/export surfaces exist.
- Runtime photo path still needs parity with bench evidence before the report can be
  trusted as final professional output.

What is missing:

- Full reference-style report layout for the mobile result screen.
- Report gating from required upstream evidence.
- Better graph/report ordering for multi-graph pages.

Research tasks:

1. Convert the reference analysis format into a report UI acceptance contract.
2. Define required sections and visual evidence per graph.
3. Make missing evidence visible and block final scientific language when needed.

Acceptance:

- A user sees a clean report; a reviewer can audit every number.

### 12. Runtime Performance And Device Strategy

Modern expectation:

- Geometry uses efficient native/image-processing backends where they outperform
  Kotlin loops.
- ML inference uses LiteRT delegates where supported.
- OCR and VLM are invoked on cropped inputs, not full pages.
- Weak devices are not given weaker scientific analysis; they may take longer or
  block with an honest reason.

Current ChromaLab state:

- LiteRT works well for the stable Google model path.
- GGUF/VLM path has had runtime and performance issues.
- Deterministic CV is still mostly custom Kotlin and not benchmarked against OpenCV or
  BoofCV.

What is not fast or modern enough:

- Full-image VLM calls for geometry are expensive and fragile.
- Custom CPU loops may be acceptable for prototypes but need native/OpenCL/OpenCV
  comparison before hardening.
- NPU support on Android is fragmented; GPU/LiteRT delegate is a practical default for
  supported LiteRT models, not a universal solution.

What is missing:

- Per-stage timing budget.
- Backend matrix: Kotlin, OpenCV Java/native, BoofCV, ML Kit OCR, PaddleOCR, LiteRT,
  llama.cpp/GGUF.
- Device policy that separates "can run chat" from "can run scientific photo
  analysis".

Research tasks:

1. Build a benchmark harness for the eight fixtures: wall time, memory, accepted stage,
   failure reason, graph count, calibration residual, peak sanity.
2. Test OpenCV native/OpenCL or BoofCV for geometry before production integration.
3. Test ML Kit bundled OCR and PaddleOCR mobile on tick crops.
4. Keep LiteRT as reference for supported on-device VLM; treat GGUF vision as
   experimental until model+mmproj+backend is validated.

Acceptance:

- Performance optimization never removes required scientific stages.
- Device compatibility is reported by model/pipeline role, not just by file format.

## Ranked Research Backlog

### R1. Evidence And Fixture Metrics

Goal:

- Turn the eight fixtures into a measurable benchmark before changing detectors.

Deliverables:

- Per-fixture expected graph count, orientation, panel count, plot-area quality,
  known visual hazards, and acceptance notes.
- JSON metric schema for graph panel, plot area, axis, tick, calibration, trace, peaks,
  and report status.

Why first:

- Without metrics, every detector change can look good on one screenshot and regress
  another.

### R2. CV Backend Selection

Goal:

- Decide whether OpenCV, BoofCV, or a smaller native adapter should own geometry.

Deliverables:

- Same fixture overlays from current Kotlin, OpenCV, and BoofCV candidate pipelines.
- Speed/memory comparison on desktop and Android.
- Dependency and APK-size review.

Likely decision:

- Test OpenCV first because it has the most complete line, contour, connected
  component, perspective, and calibration primitives. Keep BoofCV as Java/JVM fallback.

### R3. Page And Plot Rectification

Goal:

- Replace hidden no-op crop/perspective with verified rectification or honest block.

Deliverables:

- Page quadrilateral candidates.
- Plot-frame quadrilateral candidates.
- Homography matrix and residual straightness metrics.
- Desktop perspective implementation or explicit blocking.

### R4. Graph Panel And Plot Area Geometry

Goal:

- Guarantee full graph panel capture and separate numeric plot area.

Deliverables:

- Candidate overlays.
- Accepted/rejected reasons.
- Multi-panel reading order.
- Edge-peak safety checks.

### R5. Axis Tick Geometry And OCR Matching

Goal:

- Remove VLM/OCR ownership of pixel positions.

Deliverables:

- Tick segment detector.
- Label-band crops.
- OCR provider comparison.
- Robust pixel-value anchor matching and residuals.

### R6. Trace Extraction And Artifact Suppression

Goal:

- Reduce false peaks and missed peaks from axes/text/bleed-through.

Deliverables:

- Artifact masks.
- Skeleton/centerline trace candidates.
- Per-point confidence.
- Trace completeness warnings.

### R7. Calculation Benchmark

Goal:

- Validate `CalculationEngine` against known methods and fixtures.

Deliverables:

- Synthetic known-truth signals.
- SciPy/pybaselines comparison notebook or CLI.
- Baseline/peak/integration setting matrix.
- Overlap/deconvolution research note.

### R8. Report Contract Parity

Goal:

- Make Android runtime reports as evidence-rich as desktop bench reports.

Deliverables:

- Stored metadata parity checklist.
- Time-unit persistence migration plan.
- Final report gating rules.
- Reference-style mobile report design contract.

### R9. Device And Backend Matrix

Goal:

- Tell users honestly which models and devices can run which roles.

Deliverables:

- Model role matrix: chat, OCR, VLM semantic, chromatography vision, deterministic CV.
- Backend matrix: CPU, GPU, NPU, LiteRT, GGUF/mmproj, ML Kit, PaddleOCR.
- Weak-device timing and memory limits without weakening analysis quality.

## Immediate Next Slice

Phase 8.3c.5c.3c completed the R2 diagnostic spike, Phase 8.3c.5c.3d reviewed
the generated artifacts on clean, photographed two-graph, and rotated examples,
Phase 8.3c.5c.4 added the first audit-visible perspective/plot geometry contract,
Phase 8.3c.5c.5 added document, graph-panel, and plot-area quadrilateral
candidates plus aggregate residual metrics, Phase 8.3c.5c.6 added an
OpenCV-backed desktop benchmark source behind that contract, Phase 8.3c.5c.7
added deterministic axis/tick geometry audit fields, Phase 8.3c.5c.8 moved
calibration toward values-only OCR matched to deterministic tick positions with
residual-fit gates, and Phase 8.3c.5c.9 added audited skeleton/centerline trace
candidates without yet changing the calculation signal. Phase 8.3c.5c.10 added
centerline-vs-preserved-signal parity metrics and confirmed on real fixtures that
centerline must remain audit-only until visual acceptance explains the large pixel
deltas. Phase 8.3c.5c.11 added per-graph centerline parity overlays and exposed
large-delta counts so the next correction step can be driven by visual evidence.
Phase 8.3c.5c.12 classified those large deltas into branch-near, signal-above-
centerline, and signal-below-centerline groups, confirming that branch pruning and
peak-edge/top-edge handling must be audit-first before centerline can drive signal
conversion. Phase 8.3c.5c.13 added an audit-only branch-pruned centerline
hypothesis and overlay evidence; it reduces many large-delta failures but currently
loses too much overlap on hard photographed/rotated fixtures to drive calculations.
Phase 8.3c.5c.14 added continuity-interpolated pruning behind metric-safe
selection; it is accepted only where it preserves P95/large-delta quality, so hard
photo cases still require graph-path trunk extraction rather than broader
interpolation. Phase 8.3c.5c.15 added an audit-only skeleton graph trunk-path
candidate with node/edge/endpoint/junction/component metrics and per-graph
`centerline_trunk_path_overlay.png` evidence. The real fixture result is important:
hard photographed and rotated traces are not merely branched, they are fragmented
into many skeleton components, so the next step must review fragmentation-aware
trace reconstruction before any centerline candidate can drive calculations. Phase
8.3c.5c.16 adds that reconstruction as audit-only evidence: retained skeleton
components are converted to a top-envelope candidate, short column gaps are
interpolated under a fixed cap, and `centerline_fragment_reconstruction_overlay.png`
plus parity metrics are emitted for review while the calculation signal stays
unchanged. First CLI review shows the reconstructed candidate improves apparent
coverage on hard photographed fixtures but still fails P95/large-delta acceptance,
so the next slice is visual guard tuning rather than signal switching.

The next code slice should not tune prompts or ask VLM for pixel positions. It should
start trace centerline extraction only after axis calibration has an auditable path:

```text
input fixture
  -> grayscale/threshold variants
  -> existing graph-panel and plot-area candidates
  -> OpenCV contour/Hough benchmark candidates
  -> artifact-separated axis and frame masks
  -> deterministic axis-line candidates
  -> tick geometry positions
  -> OCR value matching and calibration residual metrics
  -> skeleton/centerline trace extraction candidates
  -> centerline-vs-signal parity metrics
  -> visual parity overlays before centerline drives signal conversion
  -> branch/peak-edge correction review
  -> branch-pruned centerline hypothesis
  -> branch-pruned continuity and visual acceptance tuning
  -> skeleton graph trunk-path centerline candidate
  -> fragmentation-aware trace reconstruction and acceptance review
  -> fragment reconstruction visual review and guard tuning
  -> reconstructed trace residual taxonomy and acceptance gate
```

The CV geometry spike and OpenCV benchmark remain diagnostic evidence until Android
native parity is reviewed. Production work now has a platform-neutral contract to
fill with branch-aware centerline correction, continuity-safe pruning, skeleton
graph trunk-path extraction, fragmentation-aware trace reconstruction, and
signal-guided residual classification before distortion-aware signal conversion.
Keep BoofCV as a fallback candidate if native OpenCV packaging, APK size, or Android
loading blocks the OpenCV path.

## Source Links

- OpenCV Hough line transform:
  https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
- OpenCV Line Segment Detector:
  https://docs.opencv.org/4.x/db/d73/classcv_1_1LineSegmentDetector.html
- OpenCV connected components, contours, and shape descriptors:
  https://docs.opencv.org/4.x/d3/dc0/group__imgproc__shape.html
- OpenCV ximgproc thinning:
  https://docs.opencv.org/4.x/df/d2d/group__ximgproc.html
- OpenCV drawing functions:
  https://docs.opencv.org/4.x/d6/d6e/group__imgproc__draw.html
- PoreSpy skeleton branch pruning:
  https://porespy.org/autoapi/porespy/filters/prune_branches.html
- PlantCV morphology pruning and branch-point diagnostics:
  https://docs.plantcv.org/en/v3.10.1/morphology_tutorial/
- Skan skeleton graph representation and branch statistics:
  https://skeleton-analysis.org/stable/api/skan.csr.html
- Plot2Spectra automatic spectra extraction:
  https://arxiv.org/abs/2107.02827
- MatGD graph digitizer architecture:
  https://arxiv.org/abs/2311.12806
- PlotPick VLM chart extraction benchmark:
  https://arxiv.org/abs/2605.06021
- OpenCV geometric transforms:
  https://docs.opencv.org/4.x/da/d54/group__imgproc__transform.html
- OpenCV camera calibration:
  https://docs.opencv.org/4.x/dc/dbb/tutorial_py_calibration.html
- OpenCV Android OpenCL/T-API:
  https://docs.opencv.org/4.x/d7/dbd/tutorial_android_ocl_intro.html
- BoofCV:
  https://boofcv.org/
- OpenPnP OpenCV Java packaging:
  https://github.com/openpnp/opencv
- ML Kit Text Recognition v2:
  https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- ML Kit model installation paths:
  https://developers.google.cn/ml-kit/tips/installation-paths?hl=en
- PaddleOCR Android on-device deployment:
  https://www.paddleocr.ai/main/en/version3.x/deployment/on_device_deployment.html
- PaddleOCR OCR pipeline and PP-OCRv5 mobile models:
  https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html
- Tesseract documentation:
  https://tesseract-ocr.github.io/
- Google AI Edge LiteRT GPU delegates:
  https://ai.google.dev/edge/litert/performance/gpu
- Google AI Edge LiteRT delegates:
  https://ai.google.dev/edge/litert/performance/delegates
- Google AI Edge Gallery:
  https://github.com/google-ai-edge/gallery
- LiteRT-LM:
  https://github.com/google-ai-edge/LiteRT-LM
- WebPlotDigitizer docs:
  https://automeris.io/docs/digitize/
- PlotDigitizer:
  https://plotdigitizer.com/
- PlotDigitizer documentation:
  https://plotdigitizer.com/docs
- Plot Extractor:
  https://plotextractor.com/
- Engauge Digitizer:
  https://engaugedigitizer.com/
- ChartOCR:
  https://www.microsoft.com/en-us/research/publication/chartocr-data-extraction-from-charts-images-via-a-deep-hybrid-framework/
- LineEX paper:
  https://openaccess.thecvf.com/content/WACV2023/papers/P._LineEX_Data_Extraction_From_Scientific_Line_Charts_WACV_2023_paper.pdf
- ChartLine:
  https://www.mdpi.com/1424-8220/24/21/7015
- Plot2Spectra:
  https://arxiv.org/abs/2107.02827
- DePlot:
  https://arxiv.org/abs/2212.10505
- Scatteract:
  https://arxiv.org/abs/1704.06687
- SciPy `find_peaks`:
  https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html
- pybaselines:
  https://pybaselines.readthedocs.io/
- MZmine:
  https://mzmine.github.io/mzmine_documentation/index.html
- pyOpenMS feature detection:
  https://pyopenms.readthedocs.io/en/latest/user_guide/feature_detection.html
- OpenMS FeatureFinderMetabo:
  https://www.openms.org/doxygen/nightly/html/TOPP_FeatureFinderMetabo.html
- MOCCA chromatogram processing package:
  https://www.sciencedirect.com/org/science/article/pii/S2635098X24001608
