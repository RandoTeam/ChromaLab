# ChromaLab Scientific Product Overview

Status: RP_3_SCIENTIFIC_PRODUCT_OVERVIEW_READY

ChromaLab is a public Android/Kotlin Multiplatform research project for offline-first chromatogram image analysis. It is built for a practical scientific problem: many students, educators, and researchers have chromatogram screenshots, printouts, exports, or phone photos, but do not always have access to commercial software that can reprocess the original instrument data.

ChromaLab's target workflow is simple for the user and strict for the system:

```text
Photo or screenshot
  -> graph evidence
  -> axis and scale calibration
  -> trace extraction
  -> deterministic peak calculation
  -> evidence validation
  -> scientific report
```

The project is not a black-box AI chemistry assistant. The numeric chromatographic measurements must come from deterministic, auditable code. Local AI can assist OCR, semantic classification, warning explanations, and report language, but it must not invent chromatographic measurements.

## Product Thesis

The product thesis is:

> A useful chromatogram assistant should not only output a peak table. It should show how the result was obtained, what evidence supports it, what remains uncertain, and whether the output is release-ready, review-only, diagnostic-only, or blocked.

This matters because chromatogram interpretation is not only a visual task. It is a chain of scientific assumptions and measured transformations:

- a graph must be detected correctly;
- the plot area must be separated from titles, legends, UI chrome, and page background;
- axes must be understood;
- pixel coordinates must be calibrated into scientific units;
- the trace must be extracted without confusing grid lines, text, frames, and noise for signal;
- peaks must be detected and integrated consistently;
- the final report must expose uncertainty rather than hiding it.

## Intended Users

### Students

ChromaLab is designed to help students learn how a chromatogram becomes a measurable signal.

Student-facing value:

- connect a visual chromatogram to axes, calibration, trace, and peaks;
- inspect why a peak was accepted, rejected, or marked review-grade;
- understand metrics such as retention time, height, area, area percent, FWHM, S/N, baseline, and resolution;
- compare deterministic calculations with evidence overlays;
- learn why low-quality images can block release-grade reporting.

The student story should not be "AI gives the answer." The student story should be "the app shows the evidence chain and teaches what must be checked."

### Educators

Educators can use ChromaLab as a teaching and demonstration tool for chromatogram interpretation.

Education-facing value:

- build exercises around screenshots, exported graphs, and validation fixtures;
- show why calibration matters before peak metrics are trusted;
- demonstrate how bad image quality affects scientific confidence;
- use review-only and blocked reports as teaching material;
- compare deterministic evidence, local OCR, and AI-assisted explanations.

### Researchers And Laboratory Learners

ChromaLab can support early analysis, review, and documentation workflows for researchers who need to inspect visual chromatogram data.

Research-facing value:

- preserve an audit trail for visual chromatogram analysis;
- produce a structured report contract instead of an informal screenshot annotation;
- document graph, calibration, trace, peak, and model evidence;
- keep private chromatogram images local by default;
- use local model assistance without letting the model own numeric metrics.

Domain relevance includes analytical chemistry, GC/MS learning workflows, petroleum geochemistry contexts, and chromatogram interpretation from visual data. These are relevance areas, not claims of validated coverage for every instrument, method, matrix, or regulatory workflow.

## Scientific Problem

Digitizing chromatogram images is difficult because the input is often not a clean digital signal.

Common real-world problems:

- graph panels may be embedded inside application screenshots;
- phone photos can be rotated, skewed, blurred, or cropped;
- axes can be faint, missing, or partially hidden by grid lines;
- tick marks can be too small to detect directly;
- numeric tick labels can be low resolution or compressed;
- multiple traces can share one axis system;
- multiple graph panels can appear on one page;
- legends, ion labels, m/z values, and titles can look like numeric axis labels;
- text OCR can produce plausible but scientifically wrong values;
- peak shoulders, overlap, and baseline drift can make integration uncertain.

Because of this, ChromaLab treats the analysis as an evidence pipeline rather than a single detection step.

## Pipeline Overview

### 1. Input Acquisition

Supported and planned input paths include:

- Android camera capture;
- gallery image selection;
- screenshot or exported image workflows;
- desktop development import bridge;
- future digital file imports such as CSV/TXT/PDF/mzML when parser scope is defined and validated.

The input path should not change the calculation rules. A calibrated signal from camera, gallery, or file import should eventually pass into the same deterministic calculation engine.

### 2. Image Preparation

Image preparation normalizes the source before scientific interpretation.

Expected responsibilities:

- source metadata capture;
- orientation handling;
- document or image crop preparation;
- normalization for graph detection;
- export of normalized-image evidence when the run reaches analysis stages.

Image preparation should not silently turn a poor image into a release-ready result. If preparation loses required evidence, the report gate should reflect that.

### 3. Graph Layout Detection

Graph layout detection identifies what physical graph structure is present.

Important distinctions:

- one graph with one trace;
- one graph with dense peaks;
- one graph with multiple stacked traces sharing axes;
- multiple panels with separate axes;
- TIC plus ion/channel panels;
- two-graph pages;
- embedded screenshot graphs;
- rotated or low-resolution graph exports.

This stage matters because graph count affects the whole report. One physical graph should not become several reports just because it contains dense peaks or stacked traces. A real multi-panel page should not collapse into one graph if separate axes and plot areas are present.

### 4. Plot Area And Axis Evidence

The graph panel is not always the same as the plot area. The plot area is the calibrated region where trace points live.

Evidence should include:

- selected graph panel;
- selected plot area;
- rejected graph candidates;
- rejected plot candidates;
- axis candidates;
- frame evidence;
- graph layout class;
- stage timing and failure reason.

### 5. Axis Scale Calibration

Axis calibration is the scientific bridge from image pixels to measurement units.

ChromaLab's calibration work is moving toward a multi-evidence strategy:

- explicit tick marks;
- grid lines;
- OCR numeric label boxes;
- label-band projections;
- plot frame endpoints;
- regular numeric sequences;
- axis direction and monotonicity;
- residual-backed linear fit scoring.

The important rule is that calibration must be evidence-backed. The app may infer a scale from multiple consistent evidence sources, but it must not fake axis values or accept title, ion, m/z, or legend numbers as calibration labels.

### 6. Trace Extraction

Trace extraction converts the calibrated graph into signal points.

Evidence should include:

- trace mask or centerline;
- trace overlay;
- point coverage;
- gaps;
- contamination warnings;
- frame-touch warnings;
- graph and plot-area provenance.

If the trace is uncertain, the report should remain review-only or diagnostic-only.

### 7. Deterministic Peak Calculation

Peak calculation belongs to the deterministic `CalculationEngine`.

Current concepts include:

- signal validation;
- optional smoothing;
- baseline estimation;
- baseline correction;
- noise estimation;
- peak detection;
- boundary detection;
- overlap classification;
- integration;
- peak metrics;
- confidence and warnings.

Reportable peak metrics may include:

- apex retention time;
- centroid;
- height;
- area;
- area percent;
- width;
- FWHM;
- prominence;
- S/N;
- overlap or shoulder status;
- tailing/asymmetry where available;
- resolution where available.

AI output must not create these numeric metrics.

### 8. Evidence-Gated Reporting

The report is not just a table. It is the visible contract between the image, the algorithm, the evidence, and the user.

Expected report components:

- source summary;
- graph count;
- report gate;
- processing mode;
- graph overview;
- calibration status;
- trace status;
- peak table;
- warning summary;
- evidence matrix;
- model/runtime diagnostics where applicable;
- export manifest;
- scientific caveats.

## Report Gates

ChromaLab uses evidence gates to prevent overclaiming.

| Gate | Meaning | Public interpretation |
|---|---|---|
| `RELEASE_READY` | Required automatic evidence is complete and no critical validator blocker remains. | The report can be presented as the strongest available automated output, subject to normal scientific review. |
| `REVIEW_ONLY` | The app produced useful results, but evidence needs human review. | Useful for education, inspection, and assisted review, but not final proof. |
| `DIAGNOSTIC_ONLY` | The run contains diagnostic evidence, but scientific reporting is not supported. | Useful for debugging or method development. |
| `BLOCKED` | A critical stage failed or required evidence is missing. | Do not use as a scientific result. Inspect the failure evidence. |

This gate system is central to the product. A visually nice report is not enough. If calibration, trace, peak evidence, or export artifacts are missing, the report must show that honestly.

## Local AI Role

Local AI is valuable, but it has strict boundaries.

Appropriate local AI uses:

- OCR support for local crops;
- title, ion, channel, and label classification;
- warning explanation;
- Knowledge Pack grounded explanations;
- overlay review comments;
- model-assisted disagreement notes;
- developer productivity and test/documentation support.

Forbidden local AI uses:

- create graph geometry as final numeric evidence;
- create calibration coefficients;
- create RT, height, area, FWHM, S/N, baseline, Kovats, or peak metrics;
- erase deterministic graph candidates;
- override deterministic peak calculations;
- identify compounds without explicit supporting evidence.

The ideal local AI behavior is advisory. If AI disagrees with deterministic evidence, the app should keep deterministic evidence, mark review where appropriate, and store the disagreement.

## Privacy And Local Processing

ChromaLab is designed around local processing because chromatogram images and reports may contain research data, educational material, project context, or laboratory metadata.

Privacy-relevant surfaces include:

- input images;
- normalized images;
- evidence packages;
- report exports;
- logs;
- model files;
- local storage;
- share/export workflows.

The public product story should not claim security audit completion before it exists. The correct claim is that the project is privacy-conscious and offline-first by design, with security and export review as explicit roadmap concerns.

## Current Validation Status

Current validation is active and intentionally transparent.

Known strengths:

- the app can run Android fixture validation;
- several fixtures reach graph detection, calibration, trace extraction, peak calculation, and report export;
- deterministic and E2B model-enabled paths are compared;
- validation artifacts include runtime evidence packages, validator JSON/Markdown, reports, overlays, and summaries;
- blocked and review-only cases are documented instead of hidden.

Known limitations:

- Phase 9 is not accepted as production autonomous validation;
- some fixtures remain blocked or review-only;
- graph layout and multi-panel propagation require more hardening;
- difficult axis scale calibration remains a major engineering focus;
- release-ready reporting requires stronger evidence completion;
- no public open-source license is declared at the repository root yet.

The project should present this status clearly. Honest blocked cases make the project more credible because they show that the validator is not being weakened to create a marketing result.

## What ChromaLab Is Not

ChromaLab is not:

- certified laboratory software;
- a medical, forensic, regulatory, or legal decision system;
- a guaranteed compound-identification system;
- a replacement for calibrated instrument software;
- a black-box AI chemistry oracle;
- production-validated for arbitrary real-world chromatogram photos.

ChromaLab is a research and education platform for building, validating, and explaining offline chromatogram analysis workflows.

## Product Quality Bar

For ChromaLab to become genuinely useful, the product must satisfy these expectations:

- one user action starts the analysis: take a photo or select an image;
- automatic stages run as far as evidence allows;
- every terminal state has evidence;
- failures have precise classes and subreasons;
- review-only output is clearly labeled;
- numeric calculations remain deterministic;
- local AI remains advisory;
- reports are readable by students and useful to scientific reviewers;
- privacy and file export behavior are explicit;
- validation fixtures cover more than one easy example.

## Near-Term Product Direction

The next public-facing product work should make the repository easier to understand and verify:

1. architecture overview with diagrams;
2. validation evidence summary;
3. local AI runtime explanation;
4. screenshot and user-flow package;
5. documentation index;
6. subsidy summary pack;
7. public QA pass;
8. license and contribution policy decision.

The next engineering work should continue improving:

1. graph layout and panel-count semantics;
2. axis scale resolution;
3. calibration evidence;
4. trace and peak evidence;
5. Android fixture stability;
6. report readability;
7. Rust CV integration;
8. secure export and privacy review.

## Reviewer Takeaway

ChromaLab is serious because it treats chromatogram analysis as evidence, not presentation. Its long-term value is not only automation. Its value is showing a student, educator, or researcher exactly how a chromatogram image became a calibrated signal, a peak table, and a report, with every weak link visible.
