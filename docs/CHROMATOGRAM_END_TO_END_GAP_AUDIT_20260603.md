# ChromaLab End-To-End Gap Audit

Status: `GAP_AUDIT_READY`

Date: 2026-06-03

Scope: full autonomous chromatogram analysis from user photo/screenshot to final
calculated report.

This document summarizes what the external research implies for ChromaLab. The
source notes are in `docs/research/2026-06-03_end_to_end_chromatogram_analysis_research.md`.

## Short Answer

The blocker is not only the LLM/OCR layer. The larger issue is that ChromaLab
does not yet have a fully measured image-to-signal digitization layer. Until we
can prove graph bounds, axis scale, trace extraction, and peak boundaries against
ground truth, peak tables can look plausible while still being review-grade.

The correct next move is not a blind algorithm rewrite. The next move is a
ground-truth and metric phase, then one stage at a time:

```text
image -> graph layout -> plot area -> axes/scale -> numeric trace
      -> baseline/noise -> peaks/integration -> evidence-gated report
```

## Current Product Truth

Based on Phase 9J:

- 8 Android fixtures were audited in deterministic and E2B modes.
- 16/16 evidence exports existed.
- 0 fixtures were `RELEASE_READY`.
- Most successful outputs were `REVIEW_ONLY`.
- `bench_01_mz71_screenshot_page` remains blocked around Y calibration.
- `bench_05_tic_plus_ions` remains blocked around TIC+ions layout and Y
  calibration.
- E2B did not become a numeric authority and did not solve the deterministic
  evidence gaps.

## Stage Gap Matrix

| Stage | What good systems require | What ChromaLab has | Gap |
| --- | --- | --- | --- |
| Input/provenance | Original image, transforms, device, route, model mode, privacy class. | Runtime packages and source provenance exist. | Transform chain still needs stronger pixel-coordinate audit from original to every artifact. |
| Page/plot preparation | Rectification, orientation, crop completeness, straightness residuals. | Normalization/orientation and some scanner/runtime evidence. | Page/plot rectification is not yet a measured benchmark stage. |
| Graph layout | All panels, shared axes vs separate axes, rejected candidates, reading order. | `GraphLayoutClassifier`, multiplicity resolver, overlays. | TIC+ions, stacked XIC, and multi-graph pages are not product-proven. |
| Plot area | Plot interior separated from labels/title/legend. | PlotArea overlays and validator checks. | Needs ground-truth IoU and crop-completeness scoring. |
| Axis/scale | Axis lines, grid/tick/label evidence, residual-backed X/Y calibration. | Tick pipeline, AxisScaleResolver, calibration ensemble. | Still blocked on real Y calibration for supported fixtures; no ground-truth residual benchmark. |
| OCR/text | Numeric labels classified separately from title/ion/m/z/legend. | ML Kit/E2B contracts and forbidden-text rejection. | Need OCR precision/recall per text class and label-band metrics. |
| Trace extraction | Trace mask, centerline, grid/text suppression, numeric signal error. | Trace overlays and recovered report traces exist. | Need reference signal comparison and contamination metrics. |
| Peak detection | Separate detector, baseline/noise, boundaries, integration windows. | Peak detector/integrator code and report evidence status. | Peak evidence remains review-grade; baseline/noise/integration windows need first-class audit metrics. |
| Report | Release only when every upstream evidence gate is proven. | Report gate and validators are mostly honest. | Product still has 0 release-ready fixtures; report can be visually useful but not scientifically release-ready. |
| Model assistance | OCR/semantic/judge assistant only; no numeric authority. | E2B policy and tests exist. | E2B is safe only if deterministic evidence is strong; it cannot replace missing geometry/calibration. |

## What We Should Not Do

- Do not keep patching one fixture at a time without stage metrics.
- Do not port weak heuristics to Rust exactly as-is.
- Do not accept a peak table as proof of correct analysis.
- Do not use E2B/VLM to invent geometry, calibration, or numeric metrics.
- Do not downgrade blocked fixtures to review without graph-level evidence.

## What We Should Do

### Step 1: Build Ground Truth Corpus And Metrics

This should be the next phase.

Required annotations:

- graphPanel bounds;
- plotArea bounds;
- physical graph count and layout class;
- axis line endpoints;
- tick/grid/label boxes;
- calibration anchors;
- trace mask/centerline;
- peak apex/boundaries/integration windows;
- reportable peak table when reference data exists.

Required metrics:

- graphPanel/plotArea IoU;
- axis endpoint pixel error;
- OCR label classification precision/recall;
- calibration RMSE/max residual;
- trace mask IoU or numeric signal error;
- peak RT/height/area/FWHM/S/N error;
- report gate correctness.

### Step 2: Rebuild Stages In The Right Order

Each stage gets a focused research/prototype/benchmark cycle:

1. graph layout and plot area;
2. axis scale and calibration;
3. trace extraction;
4. baseline/noise and peak integration;
5. report gate truthfulness.

Rust should be used where it gives speed and deterministic reproducibility, but
only after the stage logic is proven on PC.

### Step 3: Keep Android As The Final Parity Gate

PC benchmarks should find most logic errors first. Android should verify:

- same selected graph layout;
- same calibration status;
- same trace/peak metrics within tolerance;
- same report gate;
- no E2B regression;
- complete export artifacts.

## Product-Level Missing Pieces

1. A visual and numeric benchmark corpus larger than the current 8 fixtures.
2. Ground truth for graph layout, axes, scale, trace, and peaks.
3. Per-stage metrics that can prove an algorithm improved.
4. Raw/reference chromatogram data for calibration of image-derived peak metrics.
5. A report gate that can explain not only "blocked/review", but exact scientific
   uncertainty per metric.
6. A clear separation between production autonomous output and assisted/manual
   review output.

## Next Recommended Phase

Name: `DR-A Ground Truth Corpus And Metrics`

Goal: create the measurement system before the next algorithm rewrite.

Acceptance:

- every current fixture has visual annotations or an explicit missing annotation
  reason;
- every pipeline stage has a metric;
- current ChromaLab output is scored against those metrics;
- the next algorithmic phase is chosen from measured failures, not from intuition.
