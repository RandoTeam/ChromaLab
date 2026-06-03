# ChromaLab Autonomous Chromatogram Analysis Stage Map

Status: `PIPELINE_MAP_BASELINE`

Purpose: define every stage, substage, and evidence gate from user image input to final scientific report. This is the checklist we will use to improve the product without shallow fixes.

## Product Target

User action:

1. take photo;
2. or select photo/screenshot.

Application result:

1. automatically detects graph(s);
2. prepares image;
3. calibrates axes;
4. extracts trace;
5. detects/integrates peaks;
6. validates evidence;
7. generates a professional report with calculations and explanations.

## Stage 0: Input Acquisition And Provenance

### 0.1 Source selection

- camera capture;
- gallery image;
- screenshot;
- validation fixture;
- imported scientific/reference image.

### 0.2 Provenance recording

- source type;
- timestamp;
- app/build version;
- device profile;
- image path/hash;
- privacy class;
- model mode;
- validation mode if applicable.

### 0.3 Acquisition quality checks

- image exists;
- dimensions valid;
- EXIF orientation known;
- file readable;
- no corrupt bitmap;
- privacy/export policy assigned.

Evidence:

- source provenance JSON;
- original image hash;
- acquisition warnings.

## Stage 1: Image Preparation

### 1.1 Decode

- decode bitmap;
- preserve original dimensions;
- normalize color format.

### 1.2 Orientation

- EXIF rotation;
- visual rotation candidates;
- document/page orientation;
- rotated chromatogram detection.

### 1.3 Document/page preparation

- page boundary detection;
- perspective/skew estimate;
- crop/deskew decision;
- scanner/ML Kit crop provenance when available.

### 1.4 Normalization variants

- grayscale;
- contrast;
- denoise;
- threshold;
- edge image;
- line-enhanced image;
- OCR-enhanced crop variants.

Evidence:

- normalized image;
- preprocessing variants;
- selected preprocessing reason;
- rejected preprocessing variants.

## Stage 2: Graph Discovery

### 2.1 Candidate generation

- bright panel candidates;
- frame/axis candidates;
- embedded screenshot panel candidates;
- page layout candidates;
- full-image graph candidates.

### 2.2 Candidate scoring

- frame completeness;
- plot-like aspect ratio;
- axis evidence;
- trace density;
- text/label context;
- whitespace/separator evidence;
- crop risk.

### 2.3 Multiplicity resolution

- one physical graph;
- multiple panels;
- stacked traces with shared axes;
- TIC plus ion panels;
- two-graph page;
- duplicate candidate removal;
- nested candidate rejection.

### 2.4 Graph count decision

- expected graph count if fixture;
- detected physical graph count;
- report graph count;
- graph grouping rationale.

Evidence:

- graph candidates overlay;
- selected/rejected graphPanel table;
- graph layout class;
- graph count decision.

## Stage 3: Plot Area And Layout Semantics

### 3.1 Plot area detection

- frame interior;
- axis-aligned plot area;
- grid/frame boundary;
- trace containment;
- margin/label exclusion.

### 3.2 Layout classification

- single trace single axis;
- dense peak single axis;
- stacked traces shared axis;
- multi-panel separate axes;
- TIC plus ion panels;
- two-graph page;
- embedded screenshot graph;
- rotated page graph;
- low-resolution export graph;
- unknown review.

### 3.3 Per-graph package creation

- graph id;
- graphPanel bounds;
- plotArea bounds;
- parent page relation;
- panel group relation.

Evidence:

- plotArea overlay;
- layout taxonomy result;
- per-graph package.

## Stage 4: Axis, Grid, Tick, And Label Evidence

### 4.1 Axis line evidence

- X axis line candidates;
- Y axis line candidates;
- frame edge candidates;
- origin estimate;
- axis direction.

### 4.2 Tick/grid evidence

- explicit tick marks;
- grid lines;
- repeated spacing;
- frame endpoints;
- axis projections.

### 4.3 Label band planning

- X numeric label band;
- Y numeric label band;
- title/metadata rejection band;
- legend/channel bands;
- expanded crop candidates.

### 4.4 OCR crop rendering

- original;
- grayscale;
- scaled;
- contrast;
- threshold;
- inverted threshold.

### 4.5 OCR/VLM text reading

- ML Kit OCR;
- RapidOCR/PaddleOCR PC benchmark;
- E2B/Gemma advisory crop OCR;
- text confidence;
- raw text provenance.

### 4.6 Text classification

- X tick label;
- Y tick label;
- axis title;
- ion/m/z/channel;
- sample/file metadata;
- legend text;
- noise/unreadable.

Rule:

OCR/VLM may read and classify text. OCR/VLM may not create pixel geometry, calibration coefficients, or chromatographic metrics.

Evidence:

- axis overlay;
- tick/grid overlay;
- OCR crop grid;
- OCR text table;
- forbidden-number rejection table.

## Stage 5: Calibration Evidence

### 5.1 Anchor candidate generation

- tick pixel + OCR value;
- gridline + OCR value;
- OCR label box projection;
- regular numeric sequence;
- frame endpoint review fallback.

### 5.2 Anchor validation

- axis ownership;
- monotonicity;
- spacing consistency;
- residuals;
- forbidden text rejection;
- no-pixel-geometry rejection.

### 5.3 Fit candidates

- X fit;
- Y fit;
- residual table;
- outlier rejection;
- confidence;
- status: VALID / REVIEW / INVALID.

### 5.4 Arbitration

- strategy comparison;
- selected calibration;
- rejected calibrations;
- selection reason.

Evidence:

- accepted/rejected anchors;
- calibration overlay;
- residual table;
- coefficients;
- calibration status.

## Stage 6: Trace Extraction

### 6.1 Trace mask generation

- color/contrast mask;
- line skeleton;
- branch pruning;
- grid/text suppression;
- multi-trace separation.

### 6.2 Centerline reconstruction

- trunk path;
- fragments;
- interpolation gaps;
- smoothing;
- duplicate/branch rejection.

### 6.3 Pixel-to-unit conversion

- RT/time from X calibration;
- intensity/abundance from Y calibration;
- uncertainty propagation.

Evidence:

- trace mask overlay;
- centerline overlay;
- rejected fragments;
- trace table.

## Stage 7: Peak Detection And Integration

### 7.1 Baseline/noise

- local baseline;
- global baseline;
- noise estimate;
- S/N basis.

### 7.2 Peak candidates

- local maxima;
- shoulders;
- overlapping peaks;
- sparse/faint peaks;
- dense regions.

### 7.3 Peak metrics

- retention time;
- height;
- area;
- area percent;
- FWHM;
- S/N;
- baseline;
- flags.

### 7.4 Scientific validation

- calibration valid enough;
- trace evidence present;
- integration window valid;
- unsupported Kovats/compound identification rejected unless evidence exists.

Evidence:

- peak overlay;
- peak table;
- baseline overlay;
- integration windows;
- flags/rejection reasons.

## Stage 8: Model / Knowledge Assistance

### 8.1 Model availability

- deterministic mode;
- E2B baseline mode;
- E4B full mode if available;
- model diagnostics.

### 8.2 Allowed model tasks

- OCR crop text reading;
- title/ion/channel classification;
- warning explanation;
- overlay review warning;
- knowledge-grounded explanation.

### 8.3 Forbidden model tasks

- graph count authority;
- pixel coordinates;
- calibration coefficients;
- RT/height/area/FWHM/S/N/baseline/Kovats;
- compound identification without explicit evidence.

Evidence:

- model diagnostics;
- model outputs;
- unsupported claims;
- forbidden field attempts;
- deterministic vs model comparison.

## Stage 9: Runtime Evidence Validation

### 9.1 Evidence package

- source provenance;
- stage timings;
- graph packages;
- model diagnostics;
- calibration evidence;
- trace evidence;
- peak evidence;
- export manifest.

### 9.2 Validator

- missing evidence checks;
- overclaim checks;
- report gate checks;
- privacy checks;
- failure taxonomy.

### 9.3 Failure classification

- graph panel failure;
- plot area failure;
- axis failure;
- OCR tick failure;
- calibration failure;
- trace failure;
- peak failure;
- model unavailable;
- export/evidence failure.

Evidence:

- runtime evidence package JSON;
- validator JSON;
- validator Markdown;
- failure package.

## Stage 10: Report Generation

### 10.1 Report contract

- report gate;
- graph count;
- calibration status;
- trace status;
- peak status;
- caveats;
- evidence links.

### 10.2 Scientific content

- graph summary;
- axis/calibration summary;
- trace summary;
- peak table;
- warnings;
- limitations;
- knowledge-grounded explanations where allowed.

### 10.3 Visual report

- clean layout;
- graph overlay images;
- peak table;
- calibration evidence section;
- user-readable warnings;
- export-ready HTML/Markdown/PDF target.

### 10.4 Gate

- RELEASE_READY;
- REVIEW_ONLY;
- DIAGNOSTIC_ONLY;
- BLOCKED.

Evidence:

- final report JSON;
- HTML report;
- Markdown report;
- screenshots/contact sheets.

## Stage 11: Export And Privacy

### 11.1 Export paths

- user report;
- diagnostic artifacts;
- validation artifacts;
- debug-only artifacts.

### 11.2 Privacy classes

- user image;
- model prompt/output;
- runtime logs;
- diagnostic overlays;
- public report content.

### 11.3 Manifest

- file list;
- missing artifact reasons;
- privacy class per file;
- export timestamp;
- app/build version.

Evidence:

- export manifest;
- privacy review;
- accessible file paths.

## Stage 12: Acceptance

### 12.1 Product acceptance

- autonomous result useful;
- report understandable;
- no hidden blocked state;
- no one-fixture-only acceptance.

### 12.2 Scientific acceptance

- calibration evidence sufficient;
- trace evidence sufficient;
- peak evidence sufficient;
- no unsupported claims.

### 12.3 QA acceptance

- PC corpus;
- Android suite;
- deterministic vs E2B comparison;
- no regression;
- artifacts complete.

Decision:

- PASS;
- REVIEW;
- FAIL;
- BLOCKED.

## How We Will Use This Map

Each future phase must name exactly which stage/substage it improves, which fixtures prove it, and which evidence changed. If a phase cannot improve a stage, it must produce a precise blocker instead of a vague summary.
