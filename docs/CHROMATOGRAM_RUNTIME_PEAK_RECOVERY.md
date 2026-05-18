# Runtime Peak-Label Recovery Contract

Status: implementation slice after commit `66e5bb6`.

## Runtime Data Path

The production photo pipeline routes recovery evidence without changing
`CalculationEngine`:

1. Image input is normalized and passed to `GeometryPipelineRunner`.
2. `GeometryPipelineRunner` selects `graphPanelBounds` and `plotAreaBounds`.
3. Axis/tick OCR and calibration are built from deterministic tick positions.
4. Android `PeakLabelEvidenceReader` runs ML Kit on local crops around graph-panel
   text bands and plot-area annotation bands.
5. If ML Kit returns no usable text, low-confidence text, or ambiguous
   non-annotation text, Android can ask the active local vision model to read the
   same crop through `VisionModelBackend.readLocalTextCrop`.
6. Runtime peak-label OCR results are stored in `GeometryTrace.peakLabelEvidence`
   with source, crop path, bounds, parsed RT, text classification, and
   `isRuntimeEvidence`.
7. `CurveMaskPreparer` receives OCR text boxes and suppresses them before clean
   mask generation so peak labels inside the plot area do not become signal
   pixels.
8. Curve extraction and signal conversion produce the normal `DigitalSignal`.
9. `CalculationEngine` runs unchanged and produces the deterministic peak table.
10. `CalculationRunReportMapper` reads saved `GeometryTrace` from
   `algorithmConfig`, verifies every runtime RT label against the calculated local
   signal, and creates `RecoveredPeakCandidate` rows only for labels with signal
   evidence.
11. The final report exposes ordinary `CalculationEngine` peaks separately from
    runtime-recovered review-grade peaks and rejected recovery candidates.

## Data Classes And Mappers

- `PeakLabelEvidence`: runtime/local OCR or VLM text evidence. `FIXTURE_HINT`
  remains test-only and cannot become production evidence.
- `VisionModelBackend`: optional local-crop VLM interface. It can read crop text,
  classify region type, judge overlays, and summarize warnings. It is not allowed
  to output peak height, area, FWHM, S/N, or calibration coordinates.
- `PeakLabelTextClassification`: separates `PEAK_ANNOTATION`, tick labels, axis
  labels, title/channel text, page text, and unknown text.
- `RecoveredPeakCandidate`: local-signal verification result for one parsed label
  RT.
- `RuntimePeakRecoveryEvaluator`: pure report-layer evaluator. It uses
  `CalculationRun.signals` and existing peaks, but does not mutate
  `CalculationRun` or `CalculationEngine`.
- `PeakEvidenceAndRecoveryReport`: report contract section with raw detected,
  validated, runtime recovered, test-only recovered, rejected, production
  reportable, review-grade, dense-series, and rejected-artifact counts.
- `CalculationRunReportMapper`: attaches recovery evidence to `GraphReport`.
- `ReportMarkdownRenderer`, `ReportHtmlRenderer`, and `StructuredReportPreview`:
  render "Peak label evidence and recovery".

## Production Rules

- `FIXTURE_HINT` never sets production reportability.
- Runtime recovery requires `ML_KIT`, `VLM`, or `BOTH`.
- A label such as `5.610` is only an RT hint.
- A recovered candidate must have calibration status `VALID` or `REVIEW`, be
  inside the signal time range, avoid duplicate detected peaks, and show a local
  maximum or shoulder with height/prominence/SNR or curvature evidence.
- Accepted runtime recoveries are reportable only as review-grade peaks.
- Invalid calibration blocks production recovery.
- Labels inside `plotAreaBounds` are allowed as `PEAK_ANNOTATION`, but their text
  boxes are suppressed before curve-mask cleanup.
- ML Kit/VLM disagreement does not automatically create a peak. Duplicate
  same-RT evidence is collapsed through local signal verification and the
  duplicate evidence is reported as rejected.
- VLM crop evidence must keep the local crop path and source provenance. It is
  signal-verified through the same `RuntimePeakRecoveryEvaluator` path as ML Kit
  evidence.

## Runtime Evidence Package

The report export screen now exposes `Runtime evidence package (JSON)`. The
package is generated from the saved production report contract and includes:

- input artifacts: original/normalized images, graph-panel and plot-area overlays,
  axis/tick overlay, and calibration anchors overlay when available;
- OCR artifacts: local crop paths, crop-bounds overlay, text-classification
  overlay, raw evidence, parsed labels, rejected text candidates, and source
  provenance;
- recovery artifacts: accepted/rejected `RecoveredPeakCandidate` rows, local
  signal metrics, warnings, and review/test-only flags;
- curve artifacts: raw/clean masks, text-suppression overlay, rejected/selected
  components, skeleton, and final centerline overlay;
- summary counts: raw detected, validated, runtime recovered, test-only
  recovered, rejected recovery candidates, production reportable, and
  review-grade peaks.

The package is intentionally report-contract based: it cannot promote
`FIXTURE_HINT` rows into runtime reportable peaks, and every runtime recovery
must retain a crop path with source `ML_KIT`, `VLM`, or `BOTH`.

## bench_03 Meaning

The fixture can still have test-only label hints for `5.610` and `8.560`, but
those hints do not prove production readiness. Production recovery now depends
on actual runtime ML Kit/VLM crop evidence stored in `GeometryTrace`, followed by
local signal verification in the report mapper. If runtime OCR cannot read a
label, the production report keeps the `CalculationEngine` count and records the
missing/rejected recovery evidence instead of faking a peak.

## bench_08 Guard

Dense-series behavior remains evidence-backed. The recovery layer does not
change raw `CalculationEngine` peak counts and does not count vertical candidate
lines as peaks. `PeakEvidenceAndRecoveryReport.rawDetectedPeaks`,
`validatedPeaks`, and `productionReportablePeaks` make the count source explicit.
