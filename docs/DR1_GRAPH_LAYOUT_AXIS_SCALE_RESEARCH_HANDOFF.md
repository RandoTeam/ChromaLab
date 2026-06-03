# DR-1 Graph Layout And Axis Scale Research Handoff

Date: 2026-06-03

Status: research-only closeout. No application code changed.

## Scope

Studied methods for:

- graph layout;
- plotArea/frame detection;
- axis/tick/grid detection;
- OCR label role classification;
- pixel-to-value axis scale calibration.

Out of scope:

- `CalculationEngine`;
- peak math;
- baseline correction;
- model packaging;
- Android release work;
- report gate weakening.

## Sources Searched

Source classes covered:

- peer-reviewed / preprint chart extraction papers;
- maintained or historically important GitHub repositories;
- production digitizer tool docs;
- chromatography/spectra software references;
- Reddit / practitioner discussions as weak signals.

Main research note:

```text
docs/research/2026-06-03_dr1_graph_layout_axis_scale_deep_research.md
```

## Strongest Methods Found

1. Chart element detection plus coordinate transformation.
2. Text role classification before OCR labels can be used as axis evidence.
3. Axis/tick/grid/label association graph instead of independent heuristics.
4. Robust pixel-to-value regression with residuals and rejection reasons.
5. Multi-evidence scale resolver: ticks, grid, frame, OCR label boxes, label bands,
   numeric sequences.
6. VLM advisory layer for OCR/semantic review only.

## Adopt Now

Adopt as architecture rules:

- stage decomposition;
- evidence-first axis candidate graph;
- residual-backed calibration candidate scoring;
- OCR text role classification;
- VLM advisory-only boundary;
- no release-ready report without graph/scale/trace evidence.

## Prototype Only

Prototype in a future code phase:

- `AxisElementGraph`;
- plot frame / grid / tick / OCR label node model;
- association edges between labels and geometry;
- residual fit candidate matrix;
- evidence overlays for all candidates and rejected candidates.

## Reject

Reject for now:

- end-to-end VLM numeric extraction;
- direct WebPlotDigitizer frontend import;
- commercial tool claims as acceptance proof;
- peak/baseline changes before calibrated trace evidence is stable.

## Fixtures Affected

All eight PC bench fixtures:

- `bench_01_mz71_screenshot_page`
- `bench_02_mz92_belyi_tigr`
- `bench_03_small_tic_export`
- `bench_04_stacked_xic_resolution`
- `bench_05_tic_plus_ions`
- `bench_06_photo_two_graphs_page`
- `bench_07_rotated_page_photo`
- `bench_08_mz71_duplicate_candidate`

Primary blockers remain `axis_calibration`, `axis_detect`, and `crop_quality`.

## Required Tests For Next Implementation Phase

Before changing report gates or calibration behavior:

1. Axis element graph exports JSON for every fixture.
2. Axis element graph exports overlays for frame/grid/tick/label candidates.
3. Label text without geometry is rejected.
4. Geometry without label value stays candidate, not anchor.
5. Title/ion/m/z labels are rejected as scale labels.
6. Robust regression residuals are exported.
7. `bench_07_rotated_page_photo` remains calculation-ready when replay evidence is
   supplied.
8. Missing evidence remains BLOCKED/DIAGNOSTIC, not REVIEW by default.

## Risks

- The project can still overfit to eight fixtures if synthetic or external
  benchmark coverage is not added later.
- Object-detection-based methods may require training data and may be too heavy for
  mobile if implemented directly.
- VLM results are improving quickly, but recent research still shows value recovery
  bias; VLM must remain advisory until ChromaLab has its own measured reliability.
- "Millimeter-level" precision cannot be claimed without physical scale and
  ground-truth measurement.

## Recommended Next Implementation Phase

```text
DR-1P: Axis Element Graph Prototype
```

Phase rules:

- evidence-only first;
- no `CalculationEngine` changes;
- no report gate changes;
- no fixture-specific coordinates;
- no VLM numeric geometry;
- run PC suite before and after;
- commit only after all eight fixtures export evidence.

## Orchestrator Decision

Decision: `APPROVED_WITH_REVIEW`

Reason:

DR-1 produced a usable source matrix and a constrained next-step prototype plan.
It does not prove product acceptance and does not justify Phase 10 or release-ready
claims.
