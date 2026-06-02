# PC Chromatogram Algorithm Research Notes

Date: 2026-06-02

Scope: desktop/offline chromatogram image analysis, graph extraction, axis scale
calibration, trace extraction, peak detection, peak integration, and evidence gates.

## Activated Agents

- Orchestrator: scoped the work to a PC-first algorithm audit slice.
- Research Intelligence: checked current chart extraction and chromatographic peak literature.
- Geometry / Calibration Core: mapped graph/axis/calibration findings to current failures.
- Trace Extraction / Peak Review: reviewed peak detection and integration implications.
- Chromatography SME: checked whether graph-count and calibration claims are scientifically usable.
- Scientific Reporting / Validation: kept release/report gates tied to evidence.
- QA / Regression: ran desktop fixture tests and the PC suite runner.
- Product Acceptance: rejected unsupported "99%/100% exact" claims without locked numeric truth.

## Sources Reviewed

1. Walter et al., "Learning to See Peaks: Attention-Based Feature Extraction for
   Automated Chromatographic Peak Detection", ACS Omega, published 2026-05-26.
   URL: https://pubs.acs.org/doi/10.1021/acsomega.6c01862
2. Cliche et al., "Scatteract: Automated extraction of data from scatter plots",
   arXiv / ECML PKDD 2017.
   URL: https://arxiv.org/abs/1704.06687
3. Huang et al., "Automatic Extraction of Data Points and Text Blocks from
   2-Dimensional Plots in Digital Documents", AAAI 2008.
   URL: https://cdn.aaai.org/AAAI/2008/AAAI08-185.pdf
4. Zhang et al., "Review of peak detection algorithms in
   liquid-chromatography-mass spectrometry", PubMed record.
   URL: https://pubmed.ncbi.nlm.nih.gov/20190954/
5. "Digital Integrators: Effect of Slope Sensitivity, Filtering and Baseline
   Correction Rate on Accuracy", Journal of Chromatographic Science.
   URL: https://academic.oup.com/chromsci/article-pdf/5/12/621/1087291/5-12-621.pdf

## Current Method Implications

- Chart extraction should be treated as component detection plus robust regression,
  not OCR alone. Scatteract explicitly combines visual component detection, OCR, and
  robust pixel-to-coordinate regression.
- Axis detection should use line/profile evidence. The AAAI plot extraction paper
  describes axis-line detection via image profiles and Hough transform, then tick
  periodicity via axis-local projections. This matches ChromaLab's need for graph
  panel, plot area, axis, tick, and label evidence as separate stages.
- Peak picking must not depend on one brittle rule. The 2026 ACS Omega paper frames
  drift, overlap, and analyst variability as the practical failure modes of
  threshold/derivative approaches. It does not justify replacing ChromaLab's
  deterministic calculation path blindly; it does justify keeping peak quality,
  overlap, baseline, and region evidence explicit.
- Baseline and integration parameters directly affect quantitative accuracy.
  Slope sensitivity, filtering, and baseline correction can introduce measurable
  errors, so ChromaLab should not silently tune these inside `CalculationEngine`.
- The immediate PC blocker is upstream of peak math: current offline runs correctly
  find graph counts, but automatic calibration is unavailable or blocked for all
  fixtures without live/replay OCR/VLM axis evidence.

## Engineering Decision For This Slice

No `CalculationEngine` or chromatographic math changes are justified yet. The first
repair layer is reproducible PC regression and truth reporting:

- run all bench fixtures on the desktop path;
- preserve graph/axis/calibration/trace/peak evidence artifacts;
- classify blocked stages honestly;
- only after a locked truth matrix exists, repair one upstream stage at a time.

## Next Algorithmic Repair Order

1. Desktop OCR/VLM axis-band support and replay coverage for all eight fixtures.
2. Crop-quality gate review for `bench_02_mz92_belyi_tigr`.
3. Axis detection rescue for `bench_06_photo_two_graphs_page`.
4. Calibration evidence expansion from line/profile/label evidence.
5. Trace and peak validation after calibration is evidence-backed.
6. Numeric truth locking for selected fixtures before claiming accuracy.
