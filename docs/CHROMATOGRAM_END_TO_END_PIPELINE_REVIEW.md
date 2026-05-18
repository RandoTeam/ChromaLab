# Chromatogram End-To-End Pipeline Review

Status: Phase 8.3c.5c.3a technical review.
Date: 2026-05-17.

This document audits the real ChromaLab path from image selection to the final
calculation report. It is a code-grounded revision, not a target-only diagram.

## Implementation Update - 2026-05-18

The first shared geometry-pipeline slice is now implemented in runtime code.

Completed:

- Added shared geometry contracts:
  `SourceProvenance`, `PageRectificationResult`, `GraphPanelBounds`,
  `PlotAreaBounds`, `AxisGeometry`, `TickGeometry`, `TickOcrResult`,
  `AxisCalibrationFit`, `GeometryTrace`, and `GeometryPipelineResult`.
- Added explicit stage statuses:
  `SKIPPED_NOT_NEEDED`, `SKIPPED_NOT_CONFIDENT`, `APPLIED`, and `FAILED`.
- Added `GeometryPipelineRunner` as the common upstream geometry runner used by
  `AutoSweepEngine`.
- Added multi-candidate ROI scoring from CV regions, optional VLM hints, full-image
  fallback, and expanded candidates. VLM candidates are marked as hints and are not
  treated as pixel truth.
- Split selected graph evidence into graph-panel bounds and plot-area bounds.
- Added deterministic tick geometry contract and enabled the Android projection-based
  tick backend instead of the previous unavailable stub.
- Added local OCR-to-tick matching: numeric text without a deterministic tick position
  is marked `SEMANTIC_ONLY` and is not allowed into calibration anchors.
- Added robust multi-anchor `AxisCalibrationFitter` with outlier rejection,
  residual metrics, `VALID` / `REVIEW` / `INVALID` statuses, and an adapter back to
  the existing `PixelCalibration` path.
- Routed geometry status and trace into saved report metadata, structured report
  rendering, and report-contract validation.
- Updated legacy runtime crop and perspective result models so identity/no-op stages
  can be recorded as `SKIPPED_NOT_CONFIDENT` or `SKIPPED_NOT_NEEDED` instead of
  being described as successful processing.

Current limits:

- Real page homography selection is still not enabled as a trusted production step.
  If no reliable quadrilateral exists, the shared runner preserves identity geometry
  and reports `SKIPPED_NOT_CONFIDENT`.
- The runtime sweep still uses the existing selected graph region for curve extraction
  to avoid breaking saved-signal behavior in this slice. The geometry runner now
  computes `PlotAreaBounds`, but the next slice must make curve extraction consume
  validated/review plot-area geometry directly.
- Overlay files for every trace artifact are represented by the contract, but not all
  runtime artifacts are rendered yet on Android.
- Manual calibration UI is still the diagnostic fallback for invalid geometry; this
  slice prevents fake scientific readiness but does not yet finish the full evidence
  viewer UI.

The important product change is that missing crop/perspective/calibration evidence is
now visible to the report layer. The app should no longer silently promote identity
geometry or weak two-point assumptions into a release-quality scientific report.

## Operational Geometry Update - 2026-05-18

The next slice made the geometry contract affect actual processing, not only report
metadata.

Implemented:

- Runtime auto-sweep now sends `PlotAreaBounds` into `CurveMaskPreparer` and
  `CurveExtractor`; `GraphPanelBounds` remains the source for panel-level OCR and
  report/source context.
- Curve extraction now records a `plot_area_crop.png` artifact before mask creation.
  This makes it auditable whether tick labels, captions, title text, or page margins
  entered the curve mask.
- `GeometryPipelineRunner` now evaluates a retry ladder of ROI candidates instead of
  selecting the first/best preliminary candidate and stopping. Candidate selection is
  based on final status priority:
  `SCIENTIFIC_READY > REVIEW_READY > DIAGNOSTIC_ONLY`, then deterministic score.
- Candidate scoring now carries an explicit curve-coverage score field in addition to
  axis visibility, tick visibility, plot frame confidence, trace density, margin
  safety, aspect ratio, and calibration viability.
- `GeometryTrace` now has a plot-area crop path and receives curve mask / centerline
  artifact paths from the selected sweep variant.
- Deterministic tick positions now produce local `geometry_tick_crops/candidate_N/*.png`
  evidence crops on Android and desktop. Accepted tick OCR items can link back to the
  exact local crop used as evidence.
- Runtime geometry OCR now calls the local tick-crop reader when deterministic crops
  exist. If no deterministic tick crop exists, the geometry runner treats OCR as
  unavailable for calibration instead of asking a model to invent axis positions.

Invariant preserved:

- VLM may still provide a rough graph-region hint and text values, but pixel geometry
  used for calibration comes from deterministic axis/tick positions. OCR/VLM values
  without a matched deterministic tick remain semantic evidence only.

Still open:

- Desktop local crop OCR currently records the crop evidence but returns diagnostic
  `NOT_AVAILABLE` unless a dedicated desktop tick-crop OCR backend is added. Android
  uses ML Kit on the local crop images.
- Sparse/fragmented trace component scoring still needs more work. The current
  failing bench cases are classified in
  `docs/CHROMATOGRAM_GEOMETRY_FAILURE_CLASSIFICATION.md`.

## Fragmented Trace Update - 2026-05-18

The sparse trace slice made the remaining peak-quality failures more explicit:

- `CurveExtractor` now builds a second candidate signal from fragmented skeleton
  reconstruction. It is selected only when the raw trace is genuinely sparse
  (`<=20%` raw column coverage), has enough deterministic fragment columns, and
  keeps a bounded retained-component count. Dense traces continue to use the legacy
  signal-preserved centerline.
- Sparse selected traces expose
  `curve_extract.fragment_reconstruction_selected_for_sparse_trace`; sparse low
  coverage keeps `curve_extract.sparse_trace_low_column_coverage_accepted` and
  localized sparse evidence keeps
  `curve_extract.sparse_trace_localized_review_required`.
- Offline peak review now filters sparse micro-artifacts, terminal tails, and minor
  shoulders before computing review peak counts. This does not change
  `CalculationEngine`; it changes how sparse evidence is reported and gated.
- Sparse traces explicitly bypass guarded threshold relaxation. A weak sparse XIC
  panel can be review-grade, but it must not be silently promoted by the completeness
  tuner.
- Compact low-resolution plots now suppress right-edge border blocks before trace
  extraction. On `bench_03_small_tic_export` this removes the false late right-edge
  peak and leaves the remaining failure as missing low-pixel labeled peaks, not a
  false artifact peak.

Current bench status:

- `bench_04_stacked_xic_resolution` passes the sparse review contract after
  fragmented trace selection and sparse artifact filtering.
- `bench_03_small_tic_export` now passes fixture sanity without lowering global peak
  thresholds, but its extra two recovered peaks are explicitly test-only. The raw
  detector still returns the three strong production-reportable peaks, while the two
  smallest labeled peaks around 5.610 and 8.560 min are represented as fixture-hint
  review candidates after label evidence is linked to deterministic local signal
  evidence.
- `bench_08_mz71_duplicate_candidate` now treats the 19 default peaks as a dense
  chromatographic series unless deterministic artifact scoring proves otherwise. The
  older guarded 5 -> 9 expectation is obsolete for this fixture because the default
  detector is no longer under-detecting the visible series.

## Labeled Peak And Dense-Series Update - 2026-05-18

The latest peak-quality slice closed the remaining `bench_03` and `bench_08`
contracts without changing `CalculationEngine`.

Implemented:

- Added offline audit contracts for `PeakLabelEvidence`,
  `RecoveredPeakCandidate`, and `DenseSeriesResult`.
- Added explicit raw/reportable peak separation:
  `rawDetectedPeaks`, `reportablePeaks`, recovered review peaks, significant peaks,
  and rejected artifact peaks are now separate audit/report concepts.
- Added label evidence crops and overlays for low-resolution labeled peaks. A label
  such as `5.610` or `8.560` is only an RT hint; it must be linked through accepted
  calibration and verified against a bounded local signal window before it affects
  reportable peak sanity.
- Split production and test-only counts. `FIXTURE_HINT` evidence can satisfy fixture
  diagnostics only; it cannot increase production reportable peak count.
- Added local recovered-peak evidence: nearest local maximum, RT delta, local height,
  local S/N, curvature score, integration window, confidence, status, and rejection
  reason.
- Added dense-series classification for chromatograms such as the Ion 71 bench:
  RT ordering, spacing statistics, area trend, per-peak S/N/FWHM/overlap, artifact
  suspicion, and per-peak class are recorded before fixture expectations are updated.

Bench decisions:

- `bench_03`: root cause was low-resolution/fragmented trace evidence around labeled
  weak peaks, not a final calculation defect. The false right-edge artifact remains
  suppressed. Peaks at 5.610 and 8.560 are review-grade
  `LOW_RESOLUTION_RECOVERED` / `LABEL_EVIDENCE_VERIFIED` candidates in fixture
  testing, but they also carry `FIXTURE_HINT_ONLY` and are not production reportable.
- `bench_08`: the current 19-peak signal is treated as valid dense-series raw
  detection because every counted peak is linked to a signal apex
  (`isValidatedApex=true`, `isCandidateLineOnly=false`). No evidence currently
  justifies collapsing it to the old 9-peak guarded contract. Any future reduction
  must come from deterministic artifact classification, not from a hardcoded fixture
  count.

Runtime note:

- The desktop bench uses explicit fixture label hints as deterministic test-only
  evidence input and records that provenance. Android/runtime now has a local-crop
  `PeakLabelEvidenceReader` wired into the geometry trace. Production recovered
  peak promotion now runs through `RuntimePeakRecoveryEvaluator` in the report
  mapper: only runtime ML Kit/VLM labels with local signal evidence become
  review-grade `RecoveredPeakCandidate` rows, while `FIXTURE_HINT` remains
  test-only and cannot increase production reportable peak counts. See
  `docs/CHROMATOGRAM_RUNTIME_PEAK_RECOVERY.md`.

## Runtime Evidence Package And Crop-VLM Update - 2026-05-18

The current runtime slice makes production peak-label recovery auditable on
Android without changing `CalculationEngine`.

Implemented:

- Android local crop OCR now has an optional crop-only VLM fallback through
  `VisionModelBackend`. It is used only when ML Kit is missing, low-confidence,
  ambiguous, or when a full-analysis mode requests stronger crop reading.
- The VLM fallback receives the saved local crop and context only. It may return
  text, parsed RT-like text, confidence, region classification, and warnings; it
  may not return peak height, area, FWHM, S/N, calibration coordinates, or final
  numeric geometry.
- ML Kit and VLM evidence are merged as `BOTH` only when they agree on a plausible
  parsed RT. Disagreements remain separate evidence rows and must pass local
  signal verification before they can become review-grade recovered peaks.
- Runtime duplicate recovery is guarded twice: against already detected
  `CalculationEngine` peaks and against other recovered candidates from the same
  RT label.
- `GeometryTrace` now records crop-bounds and text-classification overlays for
  peak label evidence, plus the curve text-suppression overlay.
- The report export screen adds `Runtime evidence package (JSON)`. The package
  includes original/normalized images, graph/plot overlays, OCR crop paths,
  evidence rows, recovery decisions, curve artifacts, report contract JSON,
  summary counts, warnings, and runtime/test-only provenance.

Current evidence status:

- `bench_03` remains production-reportable as the three deterministic raw peaks
  unless real Android ML Kit/VLM crop evidence reads `5.610` and `8.560` and local
  signal verification accepts them as review-grade recovered peaks. Fixture hints
  still produce a separate test-only total of five and cannot become production
  evidence.
- `bench_08` remains unchanged: raw, validated, and production reportable counts
  are 19. Recovery cannot add duplicate label-derived peaks or turn candidate
  lines into peaks without apex evidence.

This slice proves the runtime data path in code and tests, but it does not claim
that a specific phone image has already been recovered unless the exported runtime
evidence package contains crop paths with source `ML_KIT`, `VLM`, or `BOTH` and
accepted/review `RecoveredPeakCandidate` rows.

## Scope

Reviewed areas:

- Android Smart Scan/photo entry.
- Runtime `ProcessingFlowScreen` image-to-signal path.
- Desktop/offline bench image-to-report path.
- `CalculationEngine` deterministic peak calculation.
- Stored report metadata and structured report rendering.

Primary files:

- `composeApp/src/androidMain/kotlin/com/chromalab/feature/capture/CameraScreen.android.kt`
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/document/MlKitDocumentScanner.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/flow/ProcessingFlowScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/sweep/AutoSweepEngine.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/bench/OfflineAnalysisRunner.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/signal/SignalConverter.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/calculation/core/CalculationEngine.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/calculation/flow/AnalysisFlowScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/CalculationRunReportMapper.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/StructuredReportPreview.kt`

Related contracts:

- `docs/CAMERA_SMART_SCAN_AUDIT.md`
- `docs/MODEL_ASSISTED_STAGE_CONTRACT.md`
- `docs/CHROMATOGRAM_DETERMINISTIC_CV_CORE_REVIEW.md`
- `docs/CHROMATOGRAM_CALIBRATION_BENCH_PLAN.md`

## Required Product Order

The intended scientific pipeline is:

```text
image selection
  -> source provenance
  -> decode / EXIF normalization / orientation
  -> document or page preparation
  -> graph panel detection and multi-graph splitting
  -> plot area / axes / tick geometry
  -> OCR or VLM text reading for labels and values
  -> pixel-to-unit calibration
  -> preprocessing variant sweep
  -> curve extraction
  -> calibrated signal conversion
  -> deterministic peak calculation
  -> local domain interpretation
  -> structured report with evidence and warnings
```

The user must not need to manually position the phone or tune internal thresholds.
If the app cannot produce trusted geometry or calibration, it must block the final
scientific report and show a clear technical reason.

## Current Android Runtime Path

### 1. Image Selection

Current behavior:

- The normal camera route opens ML Kit Document Scanner immediately.
- The scanner can capture from camera or import from gallery inside the scanner UI.
- On success, the first scanner JPEG is copied into app storage and passed to
  `ProcessingFlowScreen`.

Status: acceptable entry point, but provenance is incomplete.

Gaps:

- The stored path does not distinguish Smart Scan camera from Smart Scan gallery.
- ML Kit crop rectangle, deskew parameters, selected filter, and editor state are not
  persisted.
- `ProcessingFlowScreen` later saves the record as `SourceType.PHOTO`, so the report
  cannot reconstruct the true source route.

Required correction:

- Persist source route and scanner provenance before processing starts.
- Keep the final scanner JPEG, but do not treat it as enough audit evidence.

### 2. Normalize And Orientation

Current behavior:

- `ImageNormalizer.normalize` loads the image, applies size normalization and EXIF
  orientation handling, and records dimensions.
- `OrientationCorrector.correct` may rotate right-angle pages before graph detection.

Status: partially correct.

Gaps:

- The runtime path records normalized image dimensions, but later report metadata does
  not expose enough correction detail for scientific review.
- Desktop and Android correction contracts are not fully equivalent yet.

Required correction:

- Store original dimensions, normalized dimensions, rotation applied, and final image
  path as first-class source-preparation metadata.

### 3. Document Crop And Perspective

Current behavior:

- Runtime `ProcessingFlowScreen` skips internal document crop with `fallbackCropResult`.
- Runtime `ProcessingFlowScreen` skips internal perspective correction with
  `fallbackPerspectiveResult`.
- The comment assumes ML Kit images are pre-cropped and gallery images are
  user-selected.

Status: critical parity gap.

Why this matters:

- On Android Smart Scan this can work only because ML Kit already prepared the image.
- On desktop/offline, direct file import, GMS-poor devices, or any future
  cross-platform path, this becomes a hidden no-op.
- The final report can imply a prepared image even when ChromaLab itself did not
  prove crop or perspective quality.

Required correction:

- Treat ML Kit preparation as one possible source-preparation provider.
- Add the deterministic ChromaLab document/page/plot rectification provider used by
  desktop and Android.
- Never hide a no-op perspective stage as if geometry was corrected.

### 4. Graph Panel Detection And Splitting

Current behavior:

- `AutoSweepEngine` first asks the model-assisted reader for a graph region when
  model analysis is required.
- It also runs CV graph detection/refinement and chooses a selected region.
- Multi-graph snapshots are saved and later exposed through graph switching.

Status: useful but not scientifically complete.

Gaps:

- VLM is still allowed too early in the pixel geometry chain.
- Graph region scoring is based on bounds, area, aspect, warnings, and confidence,
  but it is not yet a mature line/contour/component geometry model.
- The graph panel versus plot area contract exists in docs and bench work, but the
  runtime path still relies heavily on selected region output from the sweep.

Required correction:

- Use deterministic CV to own graph panel bounds.
- Preserve full panel bounds for labels and tick values.
- Use separate plot area bounds for numeric extraction.
- Save accepted and rejected graph candidates with reasons.

### 5. Plot Area, Axes, OCR, And Calibration

Current runtime behavior:

- `AutoSweepEngine` runs OCR and axis detection once before preprocessing variants.
- `ProcessingFlowScreen` builds X calibration from OCR tick values by taking the
  first and last valid distinct X tick label positions.
- It builds Y calibration from the highest Y tick value and origin/bottom fallback.
- If enough values cannot be found, `failAutomaticAxisCalibration` blocks signal
  conversion and report save.

Status: honest blocking is good; calibration method is too weak.

Critical gaps:

- OCR/VLM text element centers are treated as calibration anchors instead of matching
  text values to deterministic tick positions.
- X calibration uses only two anchors even when more ticks are available.
- Y calibration uses the highest tick and zero origin/bottom assumption, which can be
  wrong for cropped, tilted, offset, or non-zero axes.
- There is no robust residual fit across all tick anchors in the Android runtime path.
- Axis units can be read into `SignalConverter`, but `ChromatogramEntity` does not
  store `timeUnit`; `AnalysisFlowScreen` reconstructs signals with a hardcoded
  minute unit.

Required correction:

- CV must detect axis lines and tick pixel positions before OCR.
- OCR/VLM may read tick values, labels, and ION/title text only.
- Axis calibration must fit all accepted anchors and store residuals.
- Report save must persist time unit, intensity unit, transform method, anchor points,
  residuals, and warnings.
- No final report should be produced from inferred or missing axis calibration.

### 6. Preprocessing Variant Sweep

Current behavior:

- `AutoSweepEngine` evaluates multiple preprocessing configurations.
- Curve mask and curve extraction are scored by point count, coverage, continuity,
  confidence, Y span, interpolation, outliers, and warnings.
- Selected and rejected preparation variants can be stored in report metadata.

Status: good direction.

Gaps:

- Variant scoring happens after a potentially weak graph/calibration base.
- The score does not yet have enough deterministic artifact separation for all hard
  photographed cases: axes, frame, page text, bleed-through, and labels.

Required correction:

- Keep variant sweep, but feed it from stable panel/plot geometry.
- Add artifact masks and candidate overlays before tuning thresholds further.

### 7. Curve Extraction And Signal Conversion

Current behavior:

- If the sweep produced curve points, runtime reuses them.
- Otherwise it runs `CurveMaskPreparer` and `CurveExtractor`.
- `SignalConverter` maps pixel points through `PixelCalibration`, sorts by time,
  deduplicates equal time points, counts gaps, and returns `DigitalSignal`.

Status: structurally correct after calibration, but audit persistence is incomplete.

Gaps:

- Stored `ChromatogramEntity.dataPoints` contains the final saved signal points.
- Raw pixel trace, masks, accepted/rejected curve candidates, and transform anchors
  are not persisted as first-class reproducibility data in the same way the bench
  artifacts are.

Required correction:

- Store raw pixel curve, calibrated raw signal, smoothed signal, and selected final
  signal separately or in report metadata.
- Preserve enough data to rerun calculations without repeating image analysis.

### 8. Calculation Engine

Current behavior:

`CalculationEngine.execute` performs:

1. signal validation;
2. optional smoothing;
3. baseline estimation and correction;
4. noise estimation;
5. peak detection;
6. boundary detection using configured boundary method;
7. overlap analysis;
8. integration using configured integration method and clamp-negative setting;
9. peak metric calculation;
10. run confidence and warnings.

Status: calculation core is now the strongest part of the pipeline.

What is correct:

- Boundary method, integration mode, clamp negative, and max peak width are used by
  the engine, not only displayed in UI.
- Metrics include retention/apex data, area, height, width, prominence, S/N, area
  percent, boundary method, baseline method, integration method, and confidence.

Remaining risk:

- Calculation quality is only as good as the upstream calibrated signal.
- If upstream graph bounds, plot area, or axis calibration are wrong, the calculation
  engine can produce internally consistent but scientifically wrong numbers.

Required correction:

- Calculation output must always carry upstream geometry/calibration confidence into
  report warnings and quality status.

### 9. Analysis Flow And Report Generation

Current behavior:

- `AnalysisFlowScreen` loads saved `ChromatogramEntity.dataPoints`.
- It reconstructs `DigitalSignal`, runs `CalculationEngine`, then runs distribution,
  pattern, method-quality, and optional geochemical calculations.
- `buildCalculationReportOptions` reads stored report metadata from
  `algorithmConfig`.
- `CalculationRunReportMapper`, `ReportContractValidator`, HTML export, and
  `StructuredReportPreview` render a structured report contract.

Status: structured report infrastructure exists.

Gaps:

- `AnalysisFlowScreen` rebuilds signal metadata from stored points and currently does
  not have a first-class stored time unit in `ChromatogramEntity`.
- If `algorithmConfig` is missing or incomplete, the mapper marks source, OCR, model,
  and upstream axis metadata as missing/inferred.
- The report UI can render structured sections, but the runtime photo path still must
  be brought to parity with bench artifacts before the output can match the reference
  report style reliably.

Required correction:

- Persist complete source/calibration/curve/report metadata at processing time.
- Make report generation fail or clearly mark non-scientific output when required
  upstream sections are missing.
- Keep the final user report visual and professional, but make every displayed number
  traceable to stored evidence.

## Current Desktop/Offline Bench Path

The desktop/offline bench already represents the stricter target order better than the
Android runtime path.

Current staged order in `OfflineAnalysisRunner`:

```text
normalize
  -> document_detect
  -> orientation
  -> graph_region
  -> graph_boundary
  -> graph_refine
  -> preprocessing
  -> crop_boundary
  -> plot_area
  -> axis_ocr
  -> axis_detect
  -> axis_calibration
  -> curve_mask
  -> curve_extract
  -> signal_convert
  -> peak_detection
  -> peak_metrics
  -> peak_sanity
  -> report_validation
```

Status: this is the correct audit backbone.

Gaps:

- Desktop `PerspectiveWarper` currently returns identity/copy behavior, so desktop
  tests do not yet validate real perspective correction.
- Axis OCR/VLM work still needs deterministic CV tick positions before text reading.
- Bench artifacts and Android runtime metadata are not yet one unified artifact model.

Required correction:

- Treat the bench path as the source of truth for stage order.
- Port the missing deterministic geometry core into shared code.
- Make Android runtime produce the same stage evidence, even if the UI stays minimal.

## Critical Mismatches To Fix In Order

| Priority | Mismatch | Why it blocks scientific output | Required fix |
| --- | --- | --- | --- |
| P0 | Runtime skips document crop and perspective with fallback results. | Direct photo/file paths can pass unrectified images into analysis. | Shared deterministic document/page/plot rectification or explicit scanner provenance. |
| P0 | Pixel geometry still depends too much on VLM/OCR output. | Models can guess positions; calculations need pixel evidence. | CV owns graph, axes, ticks, trace; OCR/VLM reads text only. |
| P0 | Axis calibration uses too few anchors and weak assumptions. | Retention time and intensity can be scaled incorrectly. | Robust multi-anchor calibration with residuals and blocking thresholds. |
| P1 | Runtime and bench stage contracts are not fully unified. | Desktop tests can pass while Android still saves weaker metadata. | Use one stage/evidence contract for both. |
| P1 | Stored chromatogram lacks explicit time unit and full calibration artifacts. | Later calculations/reports can lose axis semantics. | Persist units, anchors, transforms, raw/smoothed signals. |
| P1 | Final report can render from incomplete upstream metadata. | A polished report can hide missing evidence. | Report validator must gate scientific sections on required evidence. |
| P2 | Variant sweep scoring is useful but not artifact-complete. | Hard photos can create missed peaks or false peaks. | Add masks/overlays for axes, labels, frame, bleed-through, and text. |

## Correct Next Work Order

The next implementation slices should stay narrow:

1. Add a shared CV geometry adapter spike for line segments, connected components,
   contours, and axis/frame candidate overlays on the eight fixtures.
2. Make desktop perspective correction non-identity or explicitly block stages that
   depend on perspective quality.
3. Replace VLM-owned normalized tick positions with deterministic tick pixel
   candidates plus OCR value matching.
4. Add robust axis calibration using all accepted anchors and residual thresholds.
5. Persist full calibration and signal provenance in runtime report metadata.
6. Bring Android `ProcessingFlowScreen` stage evidence to parity with
   `OfflineAnalysisRunner`.
7. Only then tune VLM prompts/model stages for semantics and final report language.

## Decision

The current project has a strong calculation engine and a strong offline audit
contract, but the image-to-signal runtime path is not yet scientifically stable
enough to be treated as finished. The main correction is architectural:

- deterministic CV must own geometry;
- OCR/VLM must only read semantic/text values;
- calculation must consume calibrated numeric signal only;
- report rendering must expose or block based on evidence completeness.

This preserves the product intent: the app stays simple for the user, but internally
it performs a full auditable scientific pipeline before showing final calculated
results.
