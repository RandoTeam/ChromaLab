# DR-B Ground Truth Corpus And Automatic Metrics

Status: `DR_B3_COMPLETE`

Date: 2026-06-03

## Purpose

DR-B creates the measurement layer for ChromaLab. It does not repair graph
detection, tick localization, trace extraction, or peak integration. It defines
how future automatic methods will be scored.

The product problem is that an output can look plausible while still being
scientifically review-grade. DR-B makes every stage measurable.

## Activated Agents

| Agent | Responsibility in this slice |
| --- | --- |
| Orchestrator | Kept scope to one phase and prevented algorithmic repair. |
| Research Intelligence Agent | Applied current research and source triage. |
| QA / Regression Agent | Defined stage metrics and benchmark contracts. |
| Product Acceptance Agent | Kept product decisions tied to report gates and evidence. |
| Geometry / Calibration Core Agent | Defined graph, plotArea, axis, tick, and calibration metrics. |
| OCR / VLM / Text Semantics Agent | Defined OCR/text role safety metrics and VLM boundaries. |
| Trace Extraction / Peak Review Agent | Defined trace, mask, centerline, and peak evidence metric families. |
| Scientific Reporting / Chromatography SME | Preserved chromatographic claim gates and calculation boundaries. |
| Security / Privacy Agent | Required evidence package privacy class and artifact manifest. |

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

## New Benchmark Contracts

| Contract | File | Why it exists |
| --- | --- | --- |
| Truth | `benchmark/schemas/truth.schema.json` | Stores graphPanel, plotArea, axis, label, calibration, trace, and peak truth. |
| Prediction | `benchmark/schemas/prediction.schema.json` | Normalizes ChromaLab output into a scoreable form. |
| Metrics | `benchmark/schemas/metrics.schema.json` | Records per-stage pass/review/fail metrics. |
| Evidence package | `benchmark/schemas/evidence-package.schema.json` | Requires terminal artifacts and missing-artifact reasons. |
| Report claims | `benchmark/schemas/report-claims.schema.json` | Blocks unsupported release-quality scientific claims. |

## Stage Metrics

| Stage | Automatic metrics |
| --- | --- |
| Input/provenance | source image exists, hash present, transform chain present, privacy class present |
| GraphPanel | detected count, IoU, crop completeness, duplicate/nested rejection |
| PlotArea | IoU, inside graphPanel, label/title exclusion, trace containment |
| Axis/tick/grid | axis endpoint error, tick/grid precision/recall, label-to-tick assignment |
| OCR/text | CER/WER, numeric parse accuracy, role F1, forbidden tick-label false positives |
| Calibration | X/Y RMSE, max residual, monotonicity, anchor count, rejected anchor reasons |
| Trace | mask IoU, centerline distance, x-coverage, max gap, artifact contamination |
| Peaks | apex RT error, boundary IoU, area/height/FWHM/S/N error, false discovery rate |
| Report | supported/review/rejected claim count, unsupported claim count, gate correctness |
| Evidence package | required artifact presence, privacy class, terminal failure class, validator output |

## Corpus Policy

| Corpus type | Can prove release numeric accuracy? | Purpose |
| --- | --- | --- |
| Synthetic | Yes for generated cases | Complete controlled truth and stress testing. |
| Real paired | Yes | Real-world validation with reference signal or vendor/raw export. |
| Real unpaired diagnostic | No | Robustness, failure classification, and evidence completeness. |

## Zero-Tolerance Rules

`RELEASE_READY` must fail if any of these are missing or invalid:

- graphPanel;
- plotArea;
- X calibration;
- Y calibration;
- trace evidence;
- peak evidence;
- runtime/evidence package;
- validator JSON/Markdown;
- source provenance;
- report claim evidence.

The benchmark also fails if VLM/LLM output is accepted as numeric geometry,
calibration, RT, height, area, FWHM, S/N, baseline, Kovats/RI, peak count, or
compound identity.

## Current Slice Result

DR-B has baseline schemas, a research handoff, schema example records, a
validation runner, DR-B2 Phase 9J benchmark records, and DR-B3 score reports
that expose fixture truth gaps.

DR-B1 detail: `docs/DRB1_BENCHMARK_SCHEMA_EXAMPLES_AND_VALIDATOR.md`.

DR-B2 detail: `docs/DRB2_PHASE9J_BENCHMARK_RECORDS.md`.

DR-B3 detail: `docs/DRB3_BENCHMARK_SCORING_AND_TRUTH_GAPS.md`.

Current generated benchmark coverage:

- 16 Phase 9J benchmark cases;
- 8 fixtures;
- deterministic and E2B baseline modes;
- 64 generated schema-backed JSON records;
- 12 REVIEW decisions;
- 4 BLOCKED decisions;
- 0 RELEASE_READY decisions.

Current DR-B3 score result:

- 16 scored benchmark cases;
- 8 fixtures;
- 8 P0 fixture/mode records;
- 7 P1 fixture/mode records;
- 1 P2 fixture/mode record;
- evidence package PASS for all 16;
- calibration FAIL for 2;
- trace FAIL for 4;
- peaks FAIL for 4.

## Next Slice

Recommended next slice:

`DR-C1: Graph Layout / PlotArea / Axis Element Method Research Inputs`

Deliverables:

- graph/layout annotation requirements for P0/P1 fixtures;
- method comparison inputs for graphPanel, plotArea, axis endpoints, ticks/grid,
  and numeric label boxes;
- no algorithmic graph repair until truth fields or diagnostic-only limits are
  explicit.
