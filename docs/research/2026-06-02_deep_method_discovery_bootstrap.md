# Deep Method Discovery Bootstrap

Date: 2026-06-02

Status: bootstrap research note. No implementation decision is final until each
method is evaluated against ChromaLab fixtures and evidence gates.

## Why This Exists

Current ChromaLab blockers may come from deterministic CV/math layers, not only
from LLM/OSR model availability. The next work should pause broad fixes and study
each stage deeply before implementation:

```text
image -> normalization -> document/perspective -> graph layout -> plotArea ->
axes/ticks/grid/OCR labels -> calibration -> trace -> baseline -> peaks -> report
```

The product target remains autonomous: the user takes a photo or selects an image,
then the app completes analysis and reporting. The implementation must still keep
LLM/VLM advisory and evidence-bound.

## Source Matrix

| Source | Type | Date / freshness | Method | ChromaLab relevance | Decision |
| --- | --- | --- | --- | --- | --- |
| [ChartRecover](https://www.nature.com/articles/s44172-026-00691-8) | PEER_REVIEWED | Published 2026-05-20 | Object detection for chart elements plus coordinate transform using axis tick alignment and adaptive conversion | Strong candidate architecture for graph/axis/layout redesign; supports separating visual element detection from coordinate conversion | PROTOTYPE concept, do not import directly yet |
| [ExChart](https://exchart.github.io/) | PEER_REVIEWED / BENCHMARK | CHI 2026 | Decomposes chart extraction into coordinate-system understanding, visual mark interpretation, and precise value recovery; reports MLLMs struggle with precise values | Supports our rule that VLM should not be numeric authority; useful for VLM evaluation harness | ADOPT as research principle |
| [PlotPick](https://github.com/tommycarstensen/plotpick) | MAINTAINED_OPEN_SOURCE | 2026 GitHub repo, small early project | Batch figure extraction with structured LLM prompts and validation dataset scripts | Useful pattern for batch truth audits and structured extraction outputs; not enough authority for numeric chromatogram calculations | MONITOR / borrow validation ideas only |
| [WebPlotDigitizer docs](https://automeris.io/docs/digitize/) | PRODUCTION_TOOL_DOCS | Active docs | Calibration discipline: two points per axis, far apart for accuracy, typed numeric scale | Good baseline for calibration acceptance and user/manual fallback UX; not autonomous enough alone | ADOPT as calibration QA reference |
| [Scatteract](https://arxiv.org/abs/1704.06687) | PEER_REVIEWED / CANONICAL | Older but still relevant | Deep chart component detection + OCR + robust regression for pixel-to-coordinate mapping | Directly matches ChromaLab axis scale problem; robust regression should be part of calibration evidence | ADOPT as architecture reference |
| [Reddit / computer vision chart extraction thread](https://www.reddit.com/r/computervision/comments/1n0gj25/stuck_on_extracting_structured_data_from/) | FORUM_OR_DISCUSSION | 2025 | Practitioners recommend OCR + OpenCV structure detection rather than OCR alone | Useful weak signal: split structure detection from OCR; not implementation authority | MONITOR |
| [Reddit / OCR relationships discussion](https://www.reddit.com/r/founderledsales/comments/1rvp7i2/why_table_extraction_fails_ocr_reads_characters/) | FORUM_OR_DISCUSSION | 2026 | OCR reads characters, structured extraction requires relationships | Useful analogy for axes: OCR tick text is insufficient without geometric relationships | MONITOR |
| [deep-learning-peak-detection](https://github.com/akensert/deep-learning-peak-detection) | OPEN_SOURCE / OLDER | GitHub repo, TensorFlow 2.4 era | CNN peak detection in chromatograms | Interesting for peak candidate review, but stale and not a replacement for calibrated signal/evidence gates | REJECT for now / monitor concepts |
| [hplc-py paper](https://gchure.github.io/assets/publications/hplcpy/Chure2024a.pdf) | PEER_REVIEWED / OPEN_SOFTWARE | 2024 | SNIP baseline correction, chunking isolated/overlapping peaks, skew-normal fitting, peak properties | Relevant only after trace/calibration are valid; good future baseline/peak validation reference | PROTOTYPE later, not before axis closure |

## Initial Findings

1. The strongest chart-digitization systems do not rely on OCR alone. They combine
   visual component detection, text reading, and robust coordinate transformation.
2. Current MLLM/VLM research still warns that precise value recovery is weak without
   verification. This reinforces ChromaLab's boundary: VLM may read labels and judge
   overlays, but deterministic geometry/calibration owns numeric metrics.
3. Production digitizer tools still treat calibration as a first-class operation with
   explicit anchors, far-apart points, zoom, and validation. Fully autonomous mode
   must reproduce that discipline through evidence, not hide it.
4. Chromatogram peak/baseline research is useful, but only after graph, plotArea,
   axis calibration, and trace extraction are stable.

## Candidate Next Research Phases

| Phase candidate | Stage | Research target | Acceptance before implementation |
| --- | --- | --- | --- |
| DR-1 | Graph layout / plotArea | ChartRecover-style element detection, panel grouping, plot frame confidence | Source matrix + fixture overlay audit for all 8 bench images |
| DR-2 | Axis scale resolver | Tick/grid/label association, OCR label boxes, robust regression, residual scoring | Method comparison + PC fixture replay/live OCR plan |
| DR-3 | Trace extraction | Skeletonization, color/contrast masks, multi-trace separation, contamination rejection | Trace overlay truth table after valid calibration |
| DR-4 | Baseline / peak integration | SNIP, ALS, rolling-ball, skew-normal fitting, overlap handling | Calibrated signal dataset with known expected peak regions |
| DR-5 | VLM advisory layer | Structured crop OCR, self-consistency, hallucination rejection, disagreement handling | VLM benchmark on local crops; no numeric authority |

## Non-Adopted Claims

- Do not claim 100% or millimeter-level accuracy until we define:
  - physical image scale or pixel-to-mm relation;
  - ground-truth calibration anchors;
  - fixture-level expected numeric outputs;
  - tolerance bands for graph geometry, calibration, trace, and peaks.
- Do not add a neural peak detector before upstream calibration and trace evidence
  are reliable.
- Do not use commercial chart extraction claims as product evidence without local
  reproducible tests.

## Immediate Recommendation

Stop broad algorithm work and open the next phase as a research-only stage:

```text
DR-1: Graph Layout and Axis Scale Deep Research
```

Scope:

- no code changes;
- inspect current ChromaLab artifacts and all 8 fixtures;
- deep search GitHub, papers, docs, Reddit/issues;
- produce source matrix and method adoption plan;
- choose exactly one implementation slice after Product/QA/Scientific review.
