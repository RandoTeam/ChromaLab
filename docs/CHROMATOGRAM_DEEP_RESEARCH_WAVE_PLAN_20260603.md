# ChromaLab Deep Research Wave Plan

Status: `WAVE_PLAN_READY`

Date: 2026-06-03

Purpose: define how ChromaLab will research and validate automatic chromatogram
analysis methods one wave at a time.

This plan extends:

- `docs/CHROMATOGRAM_END_TO_END_GAP_AUDIT_20260603.md`
- `docs/research/2026-06-03_automatic_chromatogram_methods_deep_research.md`

## Operating Rule

One wave is one phase. Do not mix graph layout, OCR, trace extraction, peak
integration, Android runtime, and report gate changes in one implementation
slice.

Every wave follows:

1. research;
2. method comparison;
3. metric contract;
4. PC prototype or evidence-only runner;
5. corpus run;
6. truth table;
7. Android parity only when PC evidence is strong;
8. docs and commit.

## Activated Research Groups

| Group | Owner role | Scope |
| --- | --- | --- |
| DR-A1 | Geometry / Calibration Core | graphPanel, plotArea, axes, ticks, scale, graph count |
| DR-A2 | OCR / VLM / Text Semantics | crop OCR, text roles, VLM safety, forbidden numeric output |
| DR-A3 | Trace Extraction / Peak Review | trace masks, centerlines, artifact suppression, multi-trace handling |
| DR-A4 | Chromatography SME / Scientific Reporting | baseline, denoising, peak detection, integration, deconvolution |
| DR-A5 | QA / Product Acceptance | ground truth, metrics, benchmark harness, evidence packages, report gates |

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `method-comparison-matrix`
- `geometry-calibration-robust-fit`
- `ocr-local-crops`
- `vlm-safe-assistant`
- `trace-extraction-masks`
- `peak-review-integration`
- `chromatography-domain-review`
- `evidence-gated-reporting`
- `regression-benchmark-golden`
- `test-plan-authoring`
- `definition-of-done`

## Wave DR-B: Ground Truth Corpus And Automatic Metrics

Goal: create the automatic scoring system before more algorithmic repair.

Current status: `DR_B3_COMPLETE` in
`docs/DRB_GROUND_TRUTH_CORPUS_AND_METRICS.md`; `DR_B1_COMPLETE` in
`docs/DRB1_BENCHMARK_SCHEMA_EXAMPLES_AND_VALIDATOR.md`; `DR_B2_COMPLETE` in
`docs/DRB2_PHASE9J_BENCHMARK_RECORDS.md`; `DR_B3_COMPLETE` in
`docs/DRB3_BENCHMARK_SCORING_AND_TRUTH_GAPS.md`.

Why first:

- current output is mostly `REVIEW_ONLY` or `BLOCKED`;
- visual success is not enough;
- we need automatic proof for graph bounds, calibration, trace, peaks, and report
  claims.

Deliverables:

- `truth.schema.json` contract;
- `prediction.schema.json` contract;
- per-stage metric definitions;
- synthetic corpus generator plan;
- real paired/unpaired corpus classification;
- first metric report for current fixtures.

Acceptance:

- every current fixture has either annotation truth or explicit missing-truth
  reason;
- metrics exist for graphPanel, plotArea, axes, OCR, calibration, trace, peaks,
  and report gates;
- no algorithm is promoted based only on visual inspection.

## Wave DR-C: Graph Layout / PlotArea / Axis Elements

Goal: prove automatic graph and axis element detection.

Current status: `DR_C4_COMPLETE_PARTIAL_DETAILED_REVIEW` in
`docs/DRC1_GRAPH_LAYOUT_AXIS_RESEARCH_INPUTS.md` and
`docs/DRC2_GRAPH_LAYOUT_ANNOTATION_WORKFLOW.md`; initial P0 graph layout
annotations are documented in `docs/DRC3_INITIAL_GRAPH_LAYOUT_ANNOTATIONS.md`;
initial P0 tick/text-role annotations are documented in
`docs/DRC4_TICK_GRID_TEXT_ROLE_ANNOTATIONS.md`.

Methods to compare:

- current ChromaLab graph/layout stack;
- OpenCV/Rust line, contour, component, and Hough/LSD primitives;
- ChartRecover-style chart element detection;
- Plot2Spectra-style plot/axis refinement;
- multi-panel and stacked-trace grouping rules.

Metrics:

- graphPanel/plotArea IoU;
- graph count accuracy;
- axis endpoint error;
- tick/grid detection AP;
- candidate rejection precision;
- fail/abstain correctness.

Acceptance:

- no fixture-specific coordinates;
- expected graph count is validation truth, not candidate selection input;
- graph-stage failures include overlays and rejected candidate reasons.

## Wave DR-D: OCR Crop Benchmark And Text Role Safety

Goal: make OCR automatic and measurable without making OCR/VLM numeric authority.

Methods to compare:

- ML Kit crop OCR;
- PaddleOCR/PP-OCRv5/RapidOCR on PC;
- E2B crop OCR as advisory mode;
- preprocessing variants: upscale, contrast, threshold, rotation, padding.

Metrics:

- OCR CER/WER;
- numeric parse accuracy;
- text box IoU/F1;
- text role macro-F1;
- false tick-label rate for ION/m/z/title/legend numbers;
- VLM hallucinated-number rate.

Acceptance:

- text can enter calibration only through deterministic geometry pairing;
- forbidden numeric fields are rejected;
- missing/unreadable text produces `INCONCLUSIVE`, not fabricated labels.

## Wave DR-E: Axis Scale And Calibration Benchmark

Goal: prove scale resolution from ticks, grid lines, label boxes, regular
sequences, and frame evidence.

Methods to compare:

- legacy tick localization;
- AxisScaleResolver;
- label-box direct fit;
- grid/frame projection;
- regular sequence fit;
- ChartRecover-style tick-label matching;
- robust fit/RANSAC-style outlier rejection.

Metrics:

- calibration RMSE/max residual;
- monotonicity;
- X/Y anchor precision/recall;
- rejected forbidden text count;
- fit stability under OCR errors;
- release/review/blocked gate correctness.

Acceptance:

- exactly why calibration failed is machine-readable;
- old valid strategy cannot be replaced by invalid new strategy;
- no title/ion/m/z label can become a scale anchor.

## Wave DR-F: Trace Extraction Candidate Ensemble

Goal: produce automatic trace masks and numeric signal evidence.

Methods to compare:

- color/Lab segmentation;
- adaptive threshold;
- ridge/Steger centerline;
- skeletonization;
- graph/min-cost-flow path reconstruction;
- artifact masks for grid/text/frame.

Metrics:

- trace mask IoU/F1;
- centerline Chamfer/Hausdorff;
- x-coverage;
- max gap;
- interpolation fraction;
- artifact contamination;
- signal NRMSE/DTW after calibration.

Acceptance:

- trace without calibration remains pixel evidence only;
- multi-panel traces cannot leak across panels;
- every rejected trace candidate has a reason.

## Wave DR-G: Peak / Baseline / Integration Benchmark

Goal: compare peak algorithms on the same frozen digitized signal.

Methods to compare:

- current local maxima/prominence baseline;
- SciPy-style prominence/width gates;
- CWT/ridgeline detection;
- ALS/SNIP/arPLS/airPLS/asPLS baselines;
- local-minimum/prominence/percent-height boundaries;
- review-only deconvolution candidates.

Metrics:

- peak precision/recall/F1;
- apex RT error;
- boundary IoU;
- area/height/FWHM/S/N error;
- area sensitivity across baselines;
- false discovery rate;
- deconvolution residual and component stability.

Acceptance:

- `CalculationEngine` is not changed until upstream signal and comparator
  evidence prove an isolated bug or missing method;
- disagreements become review evidence, not silent release output;
- peak-shape fitting cannot invent reportable compounds or metrics.

## Wave DR-H: Report Gate Truth Automation

Goal: turn metrics into product truth.

Deliverables:

- report claim schema;
- release/review/diagnostic gate table;
- unsupported claim validator cases;
- product-level truth package update.

Acceptance:

- `RELEASE_READY` is impossible without graph, plotArea, calibration, trace,
  peak evidence, and evidence package;
- VLM/LLM numeric authority remains forbidden;
- every blocked/review result states exact missing evidence and next fix.

## PC / Rust Direction

Rust is recommended for speed and deterministic repeatability, but not as a
blind port of old logic.

Practical split:

- Rust-native image/signal kernels for crop planning, projections, masks,
  metrics, simple morphology, signal scoring, and benchmark runners;
- OpenCV/Rust bindings or PC OpenCV prototypes for heavy line/contour/edge/plot
  geometry until a Rust-native replacement is proven;
- Android integration only after PC corpus metrics prove the stage.

Do not port a weak heuristic just because Rust is faster.

## Next Phase To Start

Start: **DR-C5: Score Current Graph Layout Output Against P0 Annotation Truth**.

Reason:

- DR-C1 defines required graph/layout/axis annotation inputs;
- DR-C2 creates hash-backed workflow records for all 8 fixtures;
- DR-C3 adds initial graphPanel/plotArea/axis annotations for P0 fixtures;
- DR-C4 adds review-grade major tick, numeric label, anchor, and text-role
  annotations for P0 fixtures;
- current Phase 9J output can now be scored against P0 graph/layout truth before
  any new detection method is added;
- otherwise graph/layout methods would again be judged visually instead of by
  benchmark truth.

Phase 10 remains blocked until the research and benchmark waves prove enough
autonomous evidence for product acceptance.
