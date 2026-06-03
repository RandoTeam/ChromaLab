# DR-1 Graph Layout And Axis Scale Deep Research

Date: 2026-06-03

Status: research-only wave. No application code changed.

## Scope

DR-1 studies the first production blocker group:

```text
graph layout -> plotArea -> axes/ticks/grid/OCR labels -> pixel-to-value scale
```

This is intentionally upstream of `CalculationEngine`. The goal is to decide which
methods are worth prototyping for ChromaLab's fully automatic path:

```text
user photo/gallery/screenshot -> automatic graph analysis -> automatic report
```

## Activated Agents And Skills

Agents:

- Orchestrator
- Research Intelligence
- Geometry / Calibration Core
- OCR / VLM / Text Semantics
- VLM Evaluation
- QA / Regression
- Product Acceptance
- Scientific Reporting / Validation
- Chromatography SME

Skills:

- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `method-comparison-matrix`
- `geometry-calibration-robust-fit`
- `ocr-local-crops`
- `evidence-package-validator`
- `regression-benchmark-golden`

## Current ChromaLab Fixture Reality

Source: `build\phase-pc-axis-llm-audit\pc_chromatogram_bench_summary.csv`.

| Fixture | Expected graphs | Detected graphs | Ready | Blocked stage | Calibrated graphs | Interpretation |
| --- | ---: | ---: | --- | --- | ---: | --- |
| `bench_01_mz71_screenshot_page` | 2 | 2 | false | `axis_calibration` | 0 | Graph count works; scale evidence fails. |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | false | `crop_quality` | 0 | Graph found; crop/boundary confidence blocks before scale. |
| `bench_03_small_tic_export` | 1 | 1 | false | `axis_calibration` | 0 | Low-res export likely needs label/grid/sequence evidence. |
| `bench_04_stacked_xic_resolution` | 4 | 4 | false | `axis_calibration` | 0 | Multi-panel graph count works on PC; per-panel scale missing. |
| `bench_05_tic_plus_ions` | 4 | 4 | false | `axis_calibration` | 0 | TIC/ion panels need layout semantics plus per-panel scale. |
| `bench_06_photo_two_graphs_page` | 2 | 2 | false | `axis_detect` | 0 | Photo page needs axis/frame detection after graph split. |
| `bench_07_rotated_page_photo` | 1 | 1 | true | `not_blocked` | 1 | Replay axis evidence proves downstream path works. |
| `bench_08_mz71_duplicate_candidate` | 1 | 1 | false | `axis_calibration` | 0 | Duplicate-candidate handling works; scale evidence missing. |

Conclusion: current PC graph counts are mostly correct, but automatic axis scale is
not robust enough. This supports pausing model-only fixes and researching geometry,
layout, tick/grid/label association, and robust calibration.

## Source Matrix

| Source | Type | Date / freshness | Method | Relevance | Decision | Risk |
| --- | --- | --- | --- | --- | --- | --- |
| [ChartRecover](https://www.nature.com/articles/s44172-026-00691-8) | PEER_REVIEWED | Published 2026-05-20 | Deep chart element detection plus coordinate transformation through tick-mark alignment and adaptive conversion | Strongest current architecture reference for automatic graph/axis extraction | PROTOTYPE architecture, not direct import | Requires training/data; paper-level method not immediately drop-in. |
| [ExChart](https://exchart.github.io/) | PEER_REVIEWED / BENCHMARK | CHI 2026 | Decomposes chart extraction into coordinate-system understanding, visual mark interpretation, and precise value recovery | Confirms VLMs need structured stages and struggle with precise value recovery | ADOPT principle | Do not use as permission for VLM numeric authority. |
| [PlotPick](https://arxiv.org/abs/2605.06021) | PREPRINT / OPEN_SOURCE CLAIM | Submitted 2026-05-07 | VLM batch chart-to-table extraction across benchmarks | Good signal for structured VLM extraction and batch validation | MONITOR / use evaluation ideas only | End-to-end VLM extraction is unsafe for ChromaLab metrics. |
| [Y-axis bias in MLLM chart-to-table](https://arxiv.org/abs/2604.24987) | PREPRINT | Submitted 2026-04-27 | Shows y-axis value formats, tick count, digit length, and legends bias model performance | Directly relevant to ChromaLab y-axis abundance/intensity OCR | ADOPT as VLM risk model | Reinforces that model output must remain advisory. |
| [WebPlotDigitizer repository](https://github.com/automeris-io/WebPlotDigitizer) | MAINTAINED_OPEN_SOURCE / PRODUCTION_TOOL | Latest release 2024; crawled 2026 | Computer-vision assisted digitizer with strong calibration discipline | Good reference for calibration UX, axis scale types, and export model | ADOPT concepts; do not import code | AGPL frontend license; AI Assist closed source. |
| [WebPlotDigitizer docs](https://webplotdigitizer.bose.dev/) | PRODUCTION_TOOL_DOCS | Active docs | Four-point axis calibration, scale selection, coordinate mapping | Sets practical calibration standard and manual/review fallback behavior | ADOPT as QA baseline | Mostly manual, not enough for autonomous path. |
| [PlotDigitizer docs](https://plotdigitizer.com/docs) | PRODUCTION_TOOL_DOCS | Current docs, product version 2026 | Calibration markers can be placed at any known positions; far-apart points improve accuracy; supports many axis scales | Useful for anchor spacing and scale-type policy | ADOPT concept | Commercial tool docs; not an implementation source. |
| [Scatteract paper](https://arxiv.org/abs/1704.06687) | PEER_REVIEWED / CANONICAL | 2017, still relevant | Chart components + OCR + robust regression from pixels to coordinates | Direct match for ChromaLab calibration: OCR labels are not enough without regression | ADOPT architecture | Scatter plot-specific and old. |
| [Scatteract GitHub](https://github.com/bloomberg/scatteract) | OPEN_SOURCE / OLD | Repo support for paper | Detects points, tick marks, tick values; uses Tesseract and heuristics | Good code-reading target for candidate representation and test data | PROTOTYPE ideas only | Project says it is not maintained as production open source. |
| [Plot2Spectra](https://arxiv.org/abs/2107.02827) | PEER_REVIEWED / SPECTRA-SPECIFIC | 2022 | Anchor-free plot-region detector, edge-based axis refinement, scene text for ticks | Highly relevant to chromatogram/spectra-style line plots | PROTOTYPE stage design | May assume cleaner scientific figure layouts than phone photos. |
| [Chart-RCNN](https://arxiv.org/abs/2211.14362) | PEER_REVIEWED / CAMERA-IMAGE FOCUS | 2022 | One-stage model outputs labels, mark coordinates, perspective estimation from synthetic training to real camera photos | Relevant for Android photo cases and `bench_06` | PROTOTYPE later | Model training/porting cost; not first implementation slice. |
| [ChartSense](https://www.microsoft.com/en-us/research/publication/chartsense-interactive-data-extraction-chart-images/) | PEER_REVIEWED / MIXED-INITIATIVE | 2017 | Chart type classifier plus semi-automatic extraction optimized per chart type | Supports stage-specific extraction and review fallback | ADOPT workflow principle | Product target is autonomous-first, so interaction is fallback only. |
| [C3E framework](https://www.sciencedirect.com/science/article/pii/S0045790624007882) | PEER_REVIEWED | 2025 | Chart classification, text detection/recognition, text role classification, axis analysis, legend analysis, extraction | Good taxonomy for ChromaLab graph/text roles | ADOPT taxonomy | Access may be limited; use abstract/available details until full paper reviewed. |
| [StarryDigitizer](https://digitizer.starrydata.org/) | OPEN_SOURCE TOOL | Active site | Browser-local graph value extraction; color-based line/symbol modes; spline interpolation fallback | Useful later for trace extraction review and privacy-local design | MONITOR | Still needs manual calibration/review; not axis solution. |
| [Plot Extractor](https://plotextractor.com/) | COMMERCIAL / TOOL_DOCS | Crawled 2026 | Local browser OCR, plot bounds, tick values, linear/log scales, color autotrace, JSON export | Product design signal: private local OCR + assisted calibration + trace cleanup | MONITOR | Commercial claims; not implementation authority. |
| [OpenMS](https://github.com/OpenMS/OpenMS) | MAINTAINED_OPEN_SOURCE | Active LC-MS library | Mature LC/MS algorithms, visualization, many tools | Relevant after calibrated signal exists; not image digitization | MONITOR for downstream metrics | Does not solve photo/screenshot axis extraction. |
| [OpenChrom](https://github.com/OpenChrom/openchrom) | OPEN_SOURCE / CHROMATOGRAPHY TOOL | Active but desktop/raw-data focused | Chromatographic visualization/analysis | Domain reference for downstream scientific behavior | MONITOR | Not image digitization; Java/Eclipse stack. |
| [Reddit CV chart extraction thread](https://www.reddit.com/r/computervision/comments/1n0gj25/stuck_on_extracting_structured_data_from/) | FORUM_OR_DISCUSSION | 2025 | Practitioners report OCR alone fails; suggest chart parsers/OpenCV structure + OCR | Weak but consistent signal supporting hybrid pipeline | MONITOR | Not authority; use only as background. |

## Method Findings

### 1. Best current direction: chart element graph, not OCR-only

The strongest sources converge on a staged model:

```text
detect chart elements -> classify text roles -> associate labels/ticks/grid/frame ->
fit pixel-to-value transform -> validate residuals -> extract data
```

ChromaLab should not keep adding standalone tick heuristics without a stronger
element graph/evidence model.

### 2. Axis scale must be multi-evidence

Explicit tick marks are often absent or too small in screenshots/photos. DR-1
supports a scale resolver that can combine:

- plot frame edges;
- grid lines;
- tick marks;
- OCR label boxes;
- label-band projections;
- regular numeric sequences;
- residual-backed linear fit;
- uncertainty/status labels.

This matches the direction started in Phase 9F, but the next implementation should
be stage-designed and regression-tested before replacing working paths.

### 3. VLMs help, but are not enough

2026 sources are encouraging for VLM-assisted chart understanding, but they also
show precise numeric recovery remains fragile. For ChromaLab:

- VLM can help classify titles, ions, axis labels, tick text, and warnings.
- VLM can compare overlays and flag suspicious geometry.
- VLM must not produce pixel geometry, calibration coefficients, or peak metrics.

### 4. "100% to millimeter precision" is not an honest claim yet

Precision depends on:

- image resolution;
- focus/blur;
- camera perspective;
- physical pixel-to-mm scale;
- axis label visibility;
- line thickness;
- ground-truth labels and tolerances.

The right product target is measured acceptance:

```text
fixture class -> expected stage -> tolerance -> evidence -> pass/review/fail
```

## Adopt / Prototype / Reject

### Adopt Now As Architecture Rules

- Stage decomposition from ExChart/C3E/ChartRecover.
- OCR text role classification before scale use.
- Robust regression with residuals, not two-point blind fit.
- Calibration anchors must be far apart when possible.
- VLM advisory-only boundary.
- Evidence package must include rejected candidates and residuals.

### Prototype In Future Implementation Phase

- ChartRecover-style element candidate graph for plot frame, ticks, labels, legends.
- Plot2Spectra-style plot-region detector plus edge-based axis refinement.
- Scatteract-style OCR + robust regression for pixel-to-axis conversion.
- Chart-RCNN synthetic training strategy for camera-photo perspective cases.
- Local color/trace extraction ideas from StarryDigitizer only after calibration.

### Reject For Now

- End-to-end VLM chart-to-table as numeric authority.
- Direct import of WebPlotDigitizer frontend code due AGPL/license/product mismatch.
- Commercial tool claims as acceptance evidence.
- Peak/baseline algorithm rewrites before graph/axis/trace evidence is stable.

## Fixture Mapping

| Fixture | DR-1 likely method focus |
| --- | --- |
| `bench_01_mz71_screenshot_page` | Per-panel label/tick/grid association and residual fit. |
| `bench_02_mz92_belyi_tigr` | Crop quality vs plot frame completeness; edge-boundary confidence. |
| `bench_03_small_tic_export` | Low-res label OCR, regular sequence fit, review-grade calibration. |
| `bench_04_stacked_xic_resolution` | Multi-panel grouping, shared/common axis handling, per-panel scale propagation. |
| `bench_05_tic_plus_ions` | TIC/ion panel layout semantics and scale sharing/reuse policy. |
| `bench_06_photo_two_graphs_page` | Perspective/page correction, axis/frame detection after graph split. |
| `bench_07_rotated_page_photo` | Regression shield; preserve working orientation/calibration path. |
| `bench_08_mz71_duplicate_candidate` | Duplicate candidate suppression plus scale resolver. |

## Recommended Next Phase

Open a narrow research-to-prototype phase:

```text
DR-1P: Axis Element Graph Prototype
```

Do not touch `CalculationEngine`.

Prototype scope:

1. Build an evidence-only `AxisElementGraph` model for one graph:
   - plot frame candidates;
   - grid line candidates;
   - tick mark candidates;
   - OCR label boxes;
   - text role classification;
   - association edges;
   - residual fit candidates.
2. Run it on PC fixtures without changing report gates.
3. Export overlays and JSON only.
4. Compare against current axis calibration blockers.

Acceptance:

- no production behavior change;
- evidence exported for all 8 fixtures;
- bench_07 must not regress;
- no hardcoded fixture coordinates;
- no VLM numeric authority;
- handoff to implementation only after Product/QA/Scientific review.
