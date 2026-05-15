# Chromatogram Pixel Detection Research

Status: Phase 5.8b.1 research note.

## Problem

`bench_06` now exposes the core tradeoff clearly:

- Graph 1: many visible candidate peaks are present, but most are rejected by the
  prominence/noise gate.
- Graph 2: lowering the noise/prominence behavior too early accepts bleed-through and
  page/text artifacts as false chromatogram peaks.

Therefore the next implementation must not simply weaken peak thresholds. The safer
order is:

1. classify and suppress non-trace artifacts inside the plot area;
2. extract a cleaner trace/evidence map;
3. only then tune peak detection for missed visible peaks.

## External References Reviewed

| Tool / library | Useful idea | ChromaLab decision |
| --- | --- | --- |
| WebPlotDigitizer | Computer-vision-assisted extraction from plot images; mature digitizer workflow. | Use as architecture reference for plot calibration, masking, and auditable extraction, not as an Android dependency. |
| StarryDigitizer | Browser-local graph value extraction with multiple automatic extraction logics and color/line extraction modes. | Useful reference for multi-pass extraction and local/offline processing. Not directly reusable for grayscale printed chromatograms. |
| Engauge Digitizer | Offline graph digitizer workflow with grid/background filtering and curve extraction tools. | Good UX/algorithm reference; avoid importing desktop UI/runtime. |
| OpenCV HoughLinesP / Canny | Deterministic line detection for axes, grid, frame, and bleed-through straight-line artifacts. | Strong candidate for our existing Kotlin/OpenCV-style geometry logic, especially for artifact masks before curve extraction. |
| scikit-image morphology | Skeletonization, medial axis, small-object removal, holes/object morphology. | Algorithm reference for future in-house Kotlin implementation: skeleton/evidence trace should be separate from artifact components. |
| SciPy `find_peaks` | Peak properties: height, distance, prominence, width, plateau, position-dependent constraints. | Reference for improving `PeakDetector` audit and dynamic constraints, but not a direct runtime dependency. |
| pybaselines | Broad baseline-correction families: polynomial, Whittaker, morphological, smooth, spline, classification, optimizers. | Reference for future baseline strategy; current issue must first be solved at image artifact/trace evidence level. |
| OpenMS | Mass traces are detected first, then split into chromatographic peaks and assembled with RT/m/z compatibility. | Important architecture lesson: trace detection and peak detection should be separate auditable stages. |
| ChartOCR / Chart-RCNN research | Deep chart extraction can combine plot-area detection, OCR, and object detection. | Future VLM/model-assisted path only; not acceptable as the deterministic core for offline Android MVP. |

## Technical Direction

### 1. Add A Trace Evidence Layer Before Peak Detection

Current extraction produces a single signal vector and then peak detection must decide
what is real. Phase 5.8b should insert an auditable trace-evidence layer:

- candidate columns from the cleaned mask;
- component membership per column;
- vertical continuity score from baseline to apex;
- local stroke width / skeleton score;
- artifact score from straight-line, text, grid, and bleed-through detectors;
- final `traceConfidence` per signal point.

Peak detection should later use these evidence scores as dynamic constraints instead of
one global noise threshold.

### 2. Artifact Masks Before Threshold Loosening

The failed local experiment showed why threshold-only tuning is unsafe. Lowering the
effective MAD/noise threshold recovered many graph 1 peaks but accepted graph 2
bleed-through. The next safe artifact masks are:

- straight horizontal/vertical line masks for grid, frame, and axis residue;
- top-title/text residue mask inside plot area;
- bleed-through mask: low-contrast repeated vertical strokes disconnected from the
  baseline trace or matching a second panel's geometry;
- edge/frame exclusion mask already present, extended to internal straight artifacts.

### 3. Multi-Hypothesis Trace Extraction

For hard photos, keep multiple extraction hypotheses and audit them:

- current binary trace;
- scan-style / contrast trace;
- skeletonized centerline trace;
- baseline-connected vertical-stroke trace;
- artifact-suppressed trace.

The runner should compare candidates by coverage, continuity, artifact score, and
expected chromatogram behavior, then expose the selected hypothesis in `audit.json`.

### 4. Peak Detector Upgrade After Artifact Review

Only after artifact-heavy graphs are protected:

- add dynamic prominence/noise constraints by time/region;
- expose peak properties similar to SciPy: width, prominence, plateau, local threshold;
- keep rejected candidate tables for diagnostics;
- avoid saving final reports from artifact-heavy or low-confidence traces.

## Next Implementation Slice

Phase 5.8b.2 is implemented:

1. `traceArtifactAudit` records internal straight-line/text/bleed-through risk.
2. `trace_artifacts.png` renders gray clean-mask pixels and red artifact-risk pixels.
3. Accepted peak behavior remains unchanged.

Phase 5.8b.3 is implemented:

1. `trace_artifact_suppressed_mask.png` stores the cleanup hypothesis beside the current mask.
2. `traceArtifactAudit` records retained ratio, retained column coverage, and the
   threshold-relaxation guard.
3. `bench_06` graph 2 blocks threshold loosening through the artifact guard while graph
   1 remains eligible for later controlled completeness review.
4. Accepted peak behavior remains unchanged.

Phase 5.8b.4 is implemented:

1. Peak detection now compares a default run against a guarded completeness run only
   when `thresholdRelaxationAllowed=true`.
2. The guarded run is selected only for under-detected graphs with many
   prominence-threshold rejections and bounded added peak count.
3. Calibrated `bench_06` graph 1 moves from 2 to 14 accepted peaks; graph 2 remains
   blocked by the artifact guard.
4. Already-complete rotated pages stay on the default profile, preventing late
   right-frame artifacts from re-entering.

Phase 5.8b.5 should review tuned peak quality and add per-peak false-positive controls
before this path is broadened beyond the current protected case.

## Sources

- WebPlotDigitizer: https://github.com/automeris-io/WebPlotDigitizer
- StarryDigitizer: https://digitizer.starrydata.org/
- OpenCV Hough Line Transform: https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
- scikit-image morphology: https://scikit-image.org/docs/stable/api/skimage.morphology.html
- SciPy `find_peaks`: https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html
- pybaselines algorithms: https://pybaselines.readthedocs.io/en/latest/algorithms/
- OpenMS FeatureFindingMetabo: https://www.openms.org/documentation/html/classOpenMS_1_1FeatureFindingMetabo.html
- ChartOCR paper: https://www.microsoft.com/en-us/research/uploads/prod/2020/12/WACV_2021_ChartOCR.pdf
- Chart-RCNN paper: https://arxiv.org/abs/2211.14362
