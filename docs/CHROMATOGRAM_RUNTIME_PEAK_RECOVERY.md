# Runtime Peak-Label Recovery Contract

Status: implementation slice after commit `4acef3c`.

## Runtime Data Path

The production photo pipeline routes recovery evidence without changing
`CalculationEngine`:

1. Image input is normalized and passed to `GeometryPipelineRunner`.
2. `GeometryPipelineRunner` selects `graphPanelBounds` and `plotAreaBounds`.
3. Axis/tick OCR and calibration are built from deterministic tick positions.
4. Android `PeakLabelEvidenceReader` runs ML Kit on local crops around graph-panel
   text bands and plot-area annotation bands.
5. Runtime peak-label OCR results are stored in `GeometryTrace.peakLabelEvidence`
   with source, crop path, bounds, parsed RT, text classification, and
   `isRuntimeEvidence`.
6. `CurveMaskPreparer` receives OCR text boxes and suppresses them before clean
   mask generation so peak labels inside the plot area do not become signal
   pixels.
7. Curve extraction and signal conversion produce the normal `DigitalSignal`.
8. `CalculationEngine` runs unchanged and produces the deterministic peak table.
9. `CalculationRunReportMapper` reads saved `GeometryTrace` from
   `algorithmConfig`, verifies every runtime RT label against the calculated local
   signal, and creates `RecoveredPeakCandidate` rows only for labels with signal
   evidence.
10. The final report exposes ordinary `CalculationEngine` peaks separately from
    runtime-recovered review-grade peaks and rejected recovery candidates.

## Data Classes And Mappers

- `PeakLabelEvidence`: runtime/local OCR or VLM text evidence. `FIXTURE_HINT`
  remains test-only and cannot become production evidence.
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
