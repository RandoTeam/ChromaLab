# End-To-End Chromatogram Analysis Research

Date: 2026-06-03

Status: `RESEARCH_BASELINE_BEFORE_NEXT_REPAIR`

This is a research slice only. No algorithmic code, `CalculationEngine`,
chromatographic math, validators, model policy, or Android runtime behavior was
changed.

## Core Conclusion

ChromaLab should treat full autonomous chromatogram analysis as three linked but
separate systems:

1. **Image digitizer**: turn a photo/screenshot into calibrated numeric signal
   with traceable pixel evidence.
2. **Chromatography processor**: run baseline, noise, peak detection,
   integration, deconvolution/review, and metric calculation on the numeric
   signal.
3. **Evidence-gated scientific report**: publish only what the evidence proves,
   with artifacts for every accepted or rejected stage.

The current product problem is not only OCR/VLM. It is that several stages still
do not have independent ground truth, objective per-stage metrics, and enough
real fixture coverage to prove whether an output is genuinely robust or only
works for a narrow class of screenshots.

## Sources Reviewed

Primary sources and implementation references:

- WebPlotDigitizer 5.3 app/manual: XY calibration requires explicit axis
  calibration points, supports image editing, grid detection, masking, and
  automatic extraction. Source: https://webplotdigitizer.bose.dev/
- PlotDigitizer documentation: graph digitization workflow is upload -> graph
  type -> scale calibration -> extraction -> export; calibration markers can be
  placed at known coordinates and should be far apart for accuracy. Source:
  https://plotdigitizer.com/docs
- Plot2Spectra paper: separates axis alignment from plot data extraction; uses
  plot-region detection, edge-based axis refinement, text detection/recognition,
  and line extraction. Source:
  https://pubs.rsc.org/en/content/articlehtml/2022/dd/d1dd00036e
- ChartOCR paper page: argues that chart extraction works best as a hybrid of
  deep component extraction and rule-based chart priors, not pure rules or pure
  end-to-end models. Source:
  https://www.microsoft.com/en-us/research/publication/chartocr-data-extraction-from-charts-images-via-a-deep-hybrid-framework/
- PlotPick preprint, May 2026: VLMs can batch-extract structured data from
  scientific figures, but this is chart-to-table assistance and still needs
  validation for scientific numeric use. Source:
  https://arxiv.org/abs/2605.06021
- MZmine chromatogram builder: mature LC-MS workflows build chromatograms from
  raw scan/mass lists, then resolve features later. Source:
  https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_adap_chromatogram_builder/adap-chromatogram-builder.html
- MZmine local minimum resolver: resolving overlaps/shoulders uses local minima,
  minimum height/duration, minimum data points, and noise/threshold parameters.
  Source:
  https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_local_minimum/local-minimum-resolver.html
- OpenChrom BMC Bioinformatics paper: peak detection, baseline detection,
  filters, and peak integration are separated extension points so different
  detectors/integrators can be compared. Source:
  https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-405
- pyOpenMS PeakIntegrator: area, background, and peak-shape metrics are explicit
  operations; integration supports trapezoid, Simpson, and intensity-sum styles.
  Source:
  https://pyopenms.readthedocs.io/en/latest/apidocs/_autosummary/pyopenms/pyopenms.PeakIntegrator.html
- SciPy `find_peaks`: peak detection is property-gated by height, threshold,
  distance, prominence, width, and plateau size. Source:
  https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html
- IUPAC Gold Book, peak widths in chromatography: peak width definitions depend
  on baseline and retention dimension; FWHM/base width claims need a clear
  baseline and peak geometry. Source:
  https://old.goldbook.iupac.org/html/P/P04466.html
- FDA analytical procedure validation guidance: procedures and representative
  calculation formulas should be documented for analytical data. Source:
  https://www.fda.gov/regulatory-information/search-fda-guidance-documents/analytical-procedures-and-methods-validation-drugs-and-biologics

## What Correct Work Looks Like

### 1. Digitize The Image Before Calculating Chemistry

The image path must not jump directly from screenshot to peak table. The correct
order is:

1. preserve original image and transforms;
2. rectify page/plot if needed;
3. detect graph panel and plot area;
4. detect axes, grid, labels, and trace pixels;
5. fit X/Y pixel-to-unit calibration with residuals;
6. extract a numeric `(time, intensity)` signal;
7. only then run chromatographic calculations.

This is consistent with WebPlotDigitizer/PlotDigitizer and with Plot2Spectra:
axis/plot alignment and data-line extraction are separate stages. The important
lesson for ChromaLab is that axis scale is not a side detail. It is the bridge
between pixels and all reported RT, height, area, FWHM, and S/N values.

### 2. Separate Detector, Integrator, Baseline, And Report Gates

Mature chromatogram tools do not treat "peak found" as "peak calculated and
reportable". OpenChrom explicitly separates peak detection from integration.
OpenMS/pyOpenMS exposes peak area, background, and shape metrics as distinct
operations. MZmine separates chromatogram building, resolving/deconvolution, and
filtering by height/duration/data-point count.

For ChromaLab this means:

- trace extraction must produce a numeric signal before peak detection;
- peak detector output must stay separate from peak integration output;
- baseline/noise evidence must be reportable;
- peak boundaries and integration windows must be visible artifacts;
- report gates must fail if the peak table exists but lacks evidence.

### 3. Use Models As Assistants, Not Numeric Authorities

The 2026 chart-extraction direction is useful but not enough by itself. PlotPick
and DePlot-style work shows that VLMs can help extract structured chart data, but
scientific chromatogram output needs stronger evidence gates. ChartOCR's hybrid
approach is a better pattern for ChromaLab: learned or model-driven components
can classify/read, while deterministic rules and residual checks enforce geometry
and numeric validity.

For ChromaLab:

- E2B/Gemma may help local OCR, semantic classification, warnings, and report
  language.
- E2B/Gemma must not create graph counts, pixel geometry, calibration
  coefficients, RT, height, area, FWHM, S/N, baseline, Kovats, or compound
  identity without explicit evidence.
- If E2B disagrees with deterministic geometry, keep deterministic evidence and
  mark review.

## Current ChromaLab State

ChromaLab already has substantial structure:

- `CHROMATOGRAM_AUTONOMOUS_ANALYSIS_STAGE_MAP.md` defines stages from input to
  report.
- Runtime evidence packages, validator JSON/Markdown, report contracts, and
  Android validation fixtures exist.
- Graph layout, tick localization, axis scale resolution, and calibration
  strategy ensemble code exists.
- Phase 9J truth audit gives a real product truth table for eight Android
  fixtures in deterministic and E2B modes.
- Calculation/report code separates some peak evidence from final report gates.

That is the correct direction. The missing part is not "one more heuristic". The
missing part is a stage-by-stage scientific measurement system that proves each
stage works before it feeds the next stage.

## What Is Still Missing

### Dataset And Ground Truth

Missing:

- pixel-level graphPanel and plotArea ground truth for every fixture;
- axis line and tick/grid/label ground truth;
- calibration anchor ground truth;
- trace centerline/mask ground truth;
- peak boundary/apex/area ground truth;
- raw reference data where available, such as CSV/mzML/vendor exports.

Without this, tests can prove that code runs, but cannot prove that a reported
chromatogram is numerically correct.

### Stage Metrics

Each stage needs objective pass/fail metrics:

- graphPanel/plotArea IoU and crop-completeness score;
- axis line pixel residual;
- OCR numeric label precision/recall by text class;
- calibration RMSE/max residual and monotonicity;
- trace mask IoU or sampled signal error;
- peak apex/boundary error;
- area/height/FWHM/S/N deviation against reference;
- report-gate correctness and overclaim checks.

Current Phase 9J shows "REVIEW" or "BLOCKED", but not enough quantitative stage
accuracy to decide why the next algorithm is better.

### Image Digitizer Robustness

Open problems:

- multi-panel layout propagation is not reliable enough;
- TIC + ions and stacked XIC cases need graph-count and shared-axis proof;
- blocked fixtures still fail around Y calibration;
- overlay coverage is incomplete for some blocked runs;
- graph-count expectations need visual signoff, not just metadata.

### Calibration Robustness

Open problems:

- axis/tick/label evidence exists, but calibration still lacks durable
  ground-truth evaluation;
- "inferred" calibration can produce review reports, but release readiness needs
  explicit residual-backed anchors or approved review logic;
- label text rejection is safety-correct, but the system still needs a better
  way to form legitimate Y-scale evidence on real screenshots.

### Trace And Peak Evidence

Open problems:

- trace extraction has overlays, but it needs numeric comparison to a reference
  signal;
- peak tables exist for review fixtures, but `PEAK_EVIDENCE_FAILURE` remains
  common;
- baseline/noise/integration-window evidence should become first-class in the
  truth table;
- dense/overlapped peaks need separate detector, resolver/deconvolution, and
  integrator evidence.

### Report Truthfulness

Current report gates are mostly honest because Phase 9J still blocks Phase 10.
The gap is that product-level output is not yet release-ready:

- zero `RELEASE_READY` outputs in Phase 9J;
- `bench_01` and `bench_05` remain blocked;
- several review fixtures produce peak tables, but not release-grade evidence;
- final report screenshots and visual truth package are useful, but not enough
  without stage metrics.

## Where Outputs May Be Overfitted

These are risk areas, not accusations of intentional hardcoding:

1. **Fixture metadata can become a hidden crutch.** Expected graph counts are
   useful for validation, but they must not guide production candidate selection.
2. **A small eight-fixture set can reward fragile heuristics.** Passing one Ion
   71 screenshot does not prove general graph, axis, or trace logic.
3. **Status-based tests can hide numeric weakness.** A `REVIEW_ONLY` report with
   peaks is not evidence that RT/area/FWHM are correct.
4. **OCR success can look like calibration success.** Numeric text must be paired
   to deterministic geometry and residual checks.
5. **Model pass can hide product weakness.** E2B validator `PASS` does not make a
   report release-ready if the deterministic evidence remains review-grade.

## Required Work Method Going Forward

Each future stage should follow this loop:

1. **Research**: collect best external methods and failure modes for one stage.
2. **Contract**: define inputs, outputs, evidence, metrics, and release gate for
   that stage.
3. **PC prototype**: build fast local benchmark/prototype first, preferably Rust
   where performance matters.
4. **Corpus run**: run all known fixtures plus new hard cases, with contact
   sheets and metrics.
5. **Android parity**: only after PC proof, run the same stage on device.
6. **Integration**: wire into app without weakening gates.
7. **Truth audit**: update product truth table with before/after evidence.
8. **Commit**: commit the focused slice.

This prevents moving broken Kotlin heuristics into Rust unchanged. Rust should be
used to implement proven, measured stage logic, not to accelerate an unproven
algorithm.

## Proposed Next Phases

### Research Phase A: Ground Truth Corpus And Metrics

Build the annotation/metric framework first. No algorithm rewrite is useful
without it.

Deliverables:

- fixture annotation schema;
- graph/axis/calibration/trace/peak metric definitions;
- contact sheets;
- first metric report for all current fixtures.

### Research Phase B: Graph Layout And Plot-Area Benchmark

Compare current detector with line/contour/component methods inspired by
Plot2Spectra/ChartOCR.

Deliverables:

- graphPanel and plotArea IoU table;
- multi-panel and stacked-trace decision table;
- failures grouped by layout class.

### Research Phase C: Axis Scale And Calibration Benchmark

Benchmark tick marks, grid lines, OCR label boxes, regular sequences, and
calibration ensemble strategies against ground truth.

Deliverables:

- X/Y calibration residual tables;
- rejected text taxonomy;
- per-strategy accuracy and failure modes.

### Research Phase D: Trace Extraction Benchmark

Convert calibrated graph images into numeric traces and compare against
reference/raw data where available.

Deliverables:

- trace mask/centerline metrics;
- signal error table;
- grid/text contamination report.

### Research Phase E: Peak Detection, Baseline, And Integration Benchmark

Compare ChromaLab peak output with mature signal-processing patterns from
SciPy, OpenMS, MZmine, MOCCA, and OpenChrom.

Deliverables:

- baseline/noise model contract;
- detector vs integrator separation;
- peak boundary/apex/area accuracy table.

### Research Phase F: Report Truth And Scientific Gate

Only after the upstream metrics are measurable, tune report gates toward
release readiness.

Deliverables:

- release/review/diagnostic gate matrix;
- no-overclaim validator cases;
- user-facing report truth table.

## Immediate Decision

The next useful step is **Research Phase A: Ground Truth Corpus And Metrics**.
It should come before more algorithmic changes, before broad Rust rewrites, and
before trying to make Phase 9 pass by patching individual fixtures.
