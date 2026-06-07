# R11 Integrated Runtime Calibration Closure Closeout

Date: 2026-06-07

Status: `R11_RUNTIME_CALIBRATION_CLOSURE_CANDIDATE_REVIEW`

Scope completed: shadow calibration closure records that feed R10
runtime-shaped OCR anchor bridge rows into calibration strategy fits. R11 did
not change Android runtime behavior, validators, graph-count metadata, report
gates, chromatographic math, model policy, or `CalculationEngine`.

## Why R11 Exists

R10 proved that safe OCR anchors can be represented as runtime-shaped bridge
rows with deterministic pixel geometry, numeric OCR values, source crop
references, confidence, residual/projection fields, and forbidden-source
rejection. The next question was whether those rows can feed calibration
strategy evidence without becoming production authority.

R11 answers that question in shadow mode. It maps R10 bridge rows into
calibration strategy candidates and compares the selected/rejected results with
R8 and R9 benchmark references.

## Implementation

| Area | Result |
|---|---|
| Benchmark schema | `benchmark/schemas/runtime-calibration-closure-record.schema.json` |
| Benchmark runner | `tools/benchmark/run_r11_runtime_calibration_closure_candidate.py` |
| Records | `benchmark/examples/r11_runtime_calibration_closure_candidate/` |
| Report | `benchmark/reports/r11_runtime_calibration_closure_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r11_runtime_calibration_closure_candidate/contact_sheet_runtime_calibration_closure.png` |
| Detail records | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/` |

Rules enforced by the R11 candidate:

- accepted anchors must come from R10 accepted bridge rows;
- VLM pixel geometry and VLM numeric calibration authority remain forbidden;
- title, ion, m/z, SIM, and non-tick text cannot become calibration anchors;
- selected strategies are shadow `REVIEW`, not runtime `VALID`, while crop files
  and Android runtime-generated anchor rows are missing;
- selected and rejected strategies are exported per graph for comparison.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Runtime calibration closure records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Selected calibration graphs | 12 |
| Accepted bridge anchors used | 155 |
| Rejected bridge anchors audited | 20 |
| Missing source crop image files | 155 |
| Stage evidence status | REVIEW for 4 records, MISSING for 4 unannotated records |

## Fixture Table

| Fixture | Graphs | Layout class | Status | Selected calibration graphs | Accepted anchors | Missing crop files | Decision |
|---|---:|---|---|---:|---:|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | `REVIEW` | 2 | 25 | 25 | shadow calibration closure only |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | `MISSING` | 0 | 0 | 0 | no R9/R10 anchor truth |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | `MISSING` | 0 | 0 | 0 | no R9/R10 anchor truth |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | `REVIEW` | 4 | 52 | 52 | shadow calibration closure only |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | `REVIEW` | 4 | 40 | 40 | shadow calibration closure only |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | `REVIEW` | 2 | 38 | 38 | shadow calibration closure only |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | `MISSING` | 0 | 0 | 0 | no R9/R10 anchor truth |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | `MISSING` | 0 | 0 | 0 | no R9/R10 anchor truth; legacy runtime fallback remains protected |

## Target Blocker Findings

- `bench_01_mz71_screenshot_page`: R10 bridge rows produce review-grade graph
  calibration fits; Android Y-calibration remains blocked until equivalent
  runtime OCR rows and crop files are exported.
- `bench_05_tic_plus_ions`: R10 bridge rows produce review-grade graph
  calibration fits for TIC+ions panels; Android layout propagation and
  Y-calibration evidence remain runtime blockers.

## Product Meaning

R11 shows that the safe R10 anchor contract can drive selected/rejected
calibration strategy evidence in shadow mode. It does not prove Android runtime
calibration and does not close Phase 9 acceptance.

R11 cannot upgrade Android `BLOCKED` or `REVIEW_ONLY` outcomes because the
selected fits still come from benchmark bridge rows, not equivalent Android
RuntimeEvidencePackage rows with persisted crop files.

## Next Phase

R12 has now closed as evidence/export package accountability only. It did not
promote Android runtime calibration.

```text
R13 - Android Runtime OCR Anchor Production Bridge
```

R13 should make Android runtime emit safe OCR anchor rows equivalent to the
R10/R11 benchmark rows, with persisted crop files or explicit missing-crop
reasons. Those rows must remain advisory until parity is proven.
