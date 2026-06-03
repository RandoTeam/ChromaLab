# Automatic Chromatogram Methods Deep Research

Date: 2026-06-03

Status: `DEEP_RESEARCH_SYNTHESIS_READY`

Scope: automatic methods for full chromatogram image analysis in 2025-2026.

This is a research-only slice. No application code, `CalculationEngine`,
chromatographic math, validators, Android runtime, or model behavior was changed.

## Orchestration

The task was split into five Deep Research groups. Each group searched a separate
method family so that graph geometry, OCR/VLM, trace extraction, signal
processing, and benchmark design do not get mixed into one generic plan.

| Group | Research area | Output status |
| --- | --- | --- |
| DR-A1 | Automatic graph, plotArea, axis, tick, scale, and multi-panel extraction | Completed |
| DR-A2 | Local crop OCR, text roles, VLM safety, structured outputs | Completed |
| DR-A3 | Curve masks, artifact suppression, centerline/path reconstruction | Completed |
| DR-A4 | Baseline, denoising, peak detection, boundaries, integration, deconvolution | Completed |
| DR-A5 | Synthetic truth, real paired corpus, metrics, evidence packages, report gates | Completed |

Activated skills:

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

## Top-Level Finding

The right 2026 direction is an automatic, multi-layer, evidence-first pipeline:

```text
panel/layout detection
  -> plot area and axis element detection
  -> OCR/VLM text reading with strict role classification
  -> tick/grid/label matching and residual-backed scale fit
  -> artifact-aware trace extraction
  -> calibrated numeric signal
  -> baseline/noise/peak/integration ensemble checks
  -> evidence-gated scientific report
```

Ground-truth metrics must become automatic too. ChromaLab should not depend on
manual visual judgment to decide if graphPanel, calibration, trace, peak
boundaries, or final report are correct.

## Source Quality Triage

| Source | Tier | Use status | ChromaLab impact |
| --- | --- | --- | --- |
| [ChartRecover, 2026](https://www.nature.com/articles/s44172-026-00691-8) | Peer-reviewed / current | Accepted | Strongest current reference for chart element detection, tick alignment, and pixel-to-value reconstruction. |
| [ChartZero, 2026](https://arxiv.org/abs/2605.05820) | Current preprint | Background / watch | Valuable idea: synthetic priors and end-to-end reconstruction metric; code not yet available at search time. |
| [ExChart, CHI 2026](https://exchart.github.io/) | Current benchmark | Accepted for evaluation, not runtime authority | Useful for chart extraction metrics and evidence that MLLM precision is still limited. |
| [PlotPick, 2026](https://arxiv.org/abs/2605.06021) | Current preprint / repo | Background | Useful for batch extraction and VLM workflow ideas; not accepted as numeric authority. |
| [Plot2Spectra](https://pubs.rsc.org/en/content/articlehtml/2022/dd/d1dd00036e) / [Plot2Spec](https://github.com/MaterialEyes/Plot2Spec) | Peer-reviewed + repo | Accepted with license caution | Closest to chromatogram-like scientific plots: plot region, axis refinement, OCR, segmentation, curve grouping. |
| [LineEX](https://openaccess.thecvf.com/content/WACV2023/papers/P._LineEX_Data_Extraction_From_Scientific_Line_Charts_WACV_2023_paper.pdf) | Peer-reviewed + repo | Accepted for architecture | Modular keypoints, chart element/text extraction, grouping, scaling. |
| [ChartLine](https://www.mdpi.com/1424-8220/24/21/7015) | Peer-reviewed | Accepted for trace masks | Useful for complex-background curve tracing and curve-mask metrics. |
| [C3E](https://www.sciencedirect.com/science/article/pii/S0045790624007882) | Peer-reviewed | Accepted for OCR roles | Supports chart classification, text detection/recognition, and text role classification as separate tasks. |
| [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) | Official docs | Accepted | Android OCR source for text boxes and line metadata. |
| [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR) / [PP-OCRv5](https://arxiv.org/abs/2603.24373) | Maintained repo / current paper | Accepted for PC benchmark and possible fallback | Good candidate for OCR crop benchmark, not a direct Android production decision yet. |
| [MZmine](https://github.com/mzmine/mzmine) docs | Official / maintained | Accepted for signal workflow | Chromatogram builder and resolver patterns, especially ADAP/local-minimum resolvers. |
| [OpenMS](https://github.com/OpenMS/OpenMS) / [pyOpenMS](https://www.openms.org/documentation/html/pyOpenMS.html) | Maintained / official docs | Accepted for signal processing comparison | Baseline, feature detection, and integration references. |
| [MOCCA2](https://bayer-group.github.io/MOCCA/) | Maintained docs/repo | Accepted with domain limitation | Useful for peak picking/deconvolution ideas; strongest purity features require richer data than one image trace. |
| [SciPy signal](https://docs.scipy.org/doc/scipy/reference/signal.html) | Official docs | Accepted | Stable reference for `find_peaks`, prominence, widths, CWT, Savitzky-Golay. |
| [pybaselines](https://pybaselines.readthedocs.io/en/stable/introduction.html) | Maintained docs/repo | Accepted | Baseline algorithm challenger set: ALS, arPLS, airPLS, asPLS, SNIP-style comparisons. |
| [ICH Q2(R2)](https://database.ich.org/sites/default/files/ICH_Q2%28R2%29_Guideline_2023_1130_ErrorCorrection_2025.pdf) / [FDA validation](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/analytical-procedures-and-methods-validation-drugs-and-biologics) | Regulatory guidance | Accepted for report validation language | Supports documenting procedures, calculations, validation, and scientific claims. |
| Reddit/forum discussions | Forum / anecdotal | Background only | Useful for symptoms and user pain, not implementation authority. |
| Commercial chart extraction claims | Marketing | Rejected as authority | Can inspire product expectations but cannot drive algorithms without reproducible evidence. |

## DR-A1: Automatic Graph, Axis, And Calibration Methods

Use chart-element detection and matching instead of isolated heuristics:

1. detect panel and plot area;
2. detect axes, ticks/grid lines, labels, legends, and curves as separate element
   classes;
3. match tick labels to tick marks/grid/frame positions;
4. fit linear/log scale candidates for X and Y;
5. score by residuals, monotonicity, axis direction, label matching, and curve
   consistency;
6. preserve all rejected candidates as evidence.

Methods to prototype:

- ChartRecover-style object detection and tick-label matching;
- Plot2Spectra-style plot-region detection plus edge-based axis refinement;
- Hungarian matching between OCR numeric labels and deterministic tick/grid
  candidates;
- multi-panel detection before graph count finalization;
- synthetic chart priors from ChartZero/ChartGen-style generation.

Metrics:

- graphPanel/plotArea IoU;
- axis endpoint pixel error;
- tick/label detection AP, with AP-small for tiny ticks;
- tick-label assignment accuracy;
- calibration RMSE/max residual;
- percent of extracted points inside 2/5/10 percent relative error;
- fail/abstain accuracy when evidence is insufficient.

Guardrails:

- metadata expected graph count may validate output but must not select output;
- VLM may flag disagreement but cannot erase deterministic candidates;
- `RELEASE_READY` requires both X and Y scale evidence, not just a plausible
  plot overlay.

## DR-A2: OCR / VLM / Text Semantics

Use crop-first OCR and strict text-role classification:

1. crop X tick band, Y tick band, axis titles, title/ion/m/z, legend, and peak
   annotation zones;
2. run multiple preprocessing variants;
3. collect OCR boxes, text, angle, confidence, and crop provenance;
4. classify text role;
5. pass only numeric axis-label candidates into calibration after deterministic
   geometry pairing.

Methods to prototype:

- ML Kit OCR on Android for box-level crop reads;
- PaddleOCR/PP-OCRv5/RapidOCR PC benchmark for hard crops;
- E2B/VLM crop OCR only as semantic assistant;
- strict JSON output with `crop_id`, `role`, `raw_text`, `normalized_text`,
  `confidence`, and `inconclusive_reason`;
- hard-negative crop set for ION/m/z ranges, dates, legends, peak labels, and
  sample metadata.

Metrics:

- OCR character error rate and exact numeric parse accuracy;
- text box IoU/F1;
- role macro-F1;
- false positive rate for `tick_label`;
- VLM hallucinated-number rate;
- forbidden-field rejection count;
- per-crop latency and timeout rate.

Guardrails:

- OCR/VLM text is only observed text until paired with deterministic geometry;
- title, ion, m/z, SIM, dates, sample IDs, and legends must be rejected as scale
  labels unless independent graph evidence proves otherwise;
- structured JSON validates shape, not truth.

## DR-A3: Trace Extraction

Trace extraction should be a candidate ensemble with artifact masks:

1. create grid/text/frame masks;
2. generate trace candidates through color/Lab segmentation, adaptive threshold,
   ridge/Steger-like centerlines, skeletonization, and path reconstruction;
3. reject grid/text contamination explicitly;
4. reconstruct per-column signal candidates with continuity constraints;
5. output pixel evidence until calibration allows numeric signal.

Methods to prototype:

- Plot2Spec segmentation and optical-flow grouping for chromatogram-like curves;
- LineEX/LineFormer/ChartLine-style curve masks and keypoint grouping;
- graph/min-cost-flow reconstruction for occluded/dashed traces;
- ridge/centerline extraction compared against skeletonize-only output;
- multi-trace separation only after panel identity and axis ownership are locked.

Metrics:

- trace mask IoU/F1;
- artifact-mask overlap;
- centerline Chamfer/Hausdorff distance;
- x-column coverage, max gap, interpolation fraction;
- spur/branch/junction count;
- signal MAE/RMSE/P95 error after calibration;
- trace stability under blur, JPEG, downsample, and perspective variants.

Guardrails:

- a trace without valid calibration is pixel evidence, not reportable numeric
  signal;
- labeled peak text must not become trace evidence;
- multi-panel fixtures must not leak trace or scale across panels.

## DR-A4: Signal, Baseline, Peaks, Integration

After digitization, compare calculation methods on the same frozen signal rather
than tuning per image:

1. baseline challenger set;
2. denoising tied to points-per-FWHM;
3. local maxima/prominence plus CWT/ridgeline detection;
4. multiple boundary candidates;
5. integration uncertainty;
6. overlap/deconvolution gates only when overlap evidence exists.

Methods to prototype:

- keep current local maxima/prominence as baseline comparator;
- add CWT/ridgeline detection inspired by MZmine ADAP/centWave-style logic;
- compare ALS/SNIP/arPLS/airPLS/asPLS baselines;
- compare local minima, prominence bases, percent-height, and CWT boundary
  estimates;
- use MOCCA/PeakClimber-style peak-shape fitting only as review-grade until
  residual, AIC/BIC, nonnegative/unimodal, bootstrap stability, and plausible
  width checks pass.

Metrics:

- peak precision/recall/F1 and false discovery rate;
- apex RT error;
- start/end boundary MAE and boundary IoU;
- area/height/FWHM/S/N error;
- points-over-peak;
- baseline-above-signal fraction;
- area sensitivity across baseline methods;
- deconvolution residuals, AIC/BIC, component stability.

Guardrails:

- do not change `CalculationEngine` until upstream digitized signal and
  references are validated;
- integrate raw-minus-baseline signal unless smoothing is explicitly flagged;
- unresolved disagreement remains `REVIEW`, not automatic success.

## DR-A5: Automatic Metrics, Ground Truth, And Evidence

Use synthetic truth plus paired real data:

1. generate synthetic numeric signal;
2. render charts with known graphPanel, plotArea, axes, ticks, labels, trace,
   baseline, peaks, and reportable metrics;
3. add distortions: blur, JPEG, perspective, rotation, low resolution, grid,
   text collisions, multi-panel layouts;
4. score stage-by-stage;
5. keep real paired fixtures for release-quality validation;
6. keep real unpaired fixtures for diagnostic robustness only.

Benchmark shape:

```text
benchmark/
  schemas/
    truth.schema.json
    prediction.schema.json
    evidence-package.schema.json
    report-claims.schema.json
  corpora/
    synthetic_v1/<case_id>/
    real_paired_v1/<case_id>/
    real_unpaired_diagnostic_v1/<case_id>/
  runs/<run_id>/<case_id>/
  summaries/
    metrics.json
    failure_matrix.csv
    regression_report.md
```

Metrics:

- stage metrics from DR-A1 through DR-A4;
- schema validity and evidence file existence;
- visual regression for overlay/report surfaces only;
- report claim gate correctness;
- zero-tolerance checks for missing evidence and unsupported scientific claims.

Guardrails:

- synthetic charts can overfit renderer style; real paired data is required for
  release-quality numeric claims;
- visual regression is not scientific validation;
- VLM chart-to-table scores do not validate chromatogram science.

## Method Comparison Matrix

| Method family | Automation value | Maturity | Device cost | ChromaLab fit | Decision |
| --- | --- | --- | --- | --- | --- |
| ChartRecover-style element detection + coordinate transform | High | Current peer-reviewed | Medium/high; likely PC first | Strong fit for graph/axis scale | Prototype on PC corpus |
| Plot2Spectra-style plot/axis/curve pipeline | High | Older but domain-close | Medium | Strong fit for chromatogram-like plots | Prototype selected pieces; review license |
| ChartZero synthetic priors | High | Preprint, no code at search time | Unknown | Good research direction | Watch; use concept for synthetic corpus |
| VLM chart-to-table extraction | Medium | Fast-moving, unreliable for precision | High if local model | Useful for triage/OCR semantics only | Do not use as numeric authority |
| ML Kit crop OCR | High | Official Android API | Low/medium | Good Android OCR baseline | Use in crop benchmark |
| PaddleOCR/PP-OCRv5 | High | Maintained / current | Medium/high | Good PC benchmark and possible fallback | Benchmark before mobile adoption |
| Skeletonization-only trace extraction | Medium | Mature | Low | Too weak alone | Use only as one candidate |
| Artifact-masked trace ensemble | High | Needs project work | Medium | Strong fit | Prototype evidence-only first |
| SciPy/MZmine/OpenMS-style peak processing | High | Mature | Low/medium | Strong fit after numeric signal exists | Compare on frozen signal |
| Deep peak models | Medium/high | Emerging | Medium/high | Needs labeled data | Research/prototype only after corpus |
| Synthetic truth benchmark | Very high | Strong direction | PC-first | Mandatory before more algorithm work | Next phase |

## Recommended Research Waves

1. **DR-B: Ground Truth Corpus And Automatic Metrics.**
   Build schemas and metrics first. This is the required foundation.
2. **DR-C: Graph Layout / PlotArea / Axis Element Benchmark.**
   Compare current detector, OpenCV/Rust primitives, and chart-element methods.
3. **DR-D: OCR Crop Benchmark And Text Role Safety.**
   Build crop corpus, hard negatives, ML Kit/Paddle/E2B comparison.
4. **DR-E: Axis Scale And Calibration Benchmark.**
   Evaluate tick/grid/label matching and residual-backed calibration.
5. **DR-F: Trace Extraction Candidate Ensemble.**
   Evidence-only masks, centerlines, artifact suppression, and signal metrics.
6. **DR-G: Peak/Baseline/Integration Benchmark.**
   Compare calculation methods on frozen digitized signals, without changing
   `CalculationEngine`.
7. **DR-H: Report Gate Truth Automation.**
   Turn stage metrics into release/review/diagnostic product decisions.

## Immediate Recommendation

Start with **DR-B: Ground Truth Corpus And Automatic Metrics**.

Reason: without automatic ground truth and metrics, every later method can look
better visually while still being numerically wrong or overfitted to the current
fixtures.
