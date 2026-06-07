# R10 Runtime OCR Anchor Bridge Candidate Closeout

Date: 2026-06-07

Status: `R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_REVIEW`

Scope completed: Rust/runtime-shaped OCR anchor bridge contract and benchmark
records for Stage 6 anchor rows. R10 did not change Android runtime behavior,
validators, graph-count metadata, report gates, chromatographic math, model
policy, or `CalculationEngine`.

## Why R10 Exists

R9 measured useful PC-side automatic OCR anchors, but those rows were still
benchmark evidence. The next required product question was whether those anchors
can be expressed as safe runtime rows with provenance, pixel geometry,
forbidden-text rejection, and explicit promotion blockers.

R10 answers that question in shadow mode. It converts R9 safe OCR anchors into
runtime-shaped rows and validates them through a Rust bridge contract.

## Implementation

| Area | Result |
|---|---|
| Rust bridge contract | `rust/chromalab-cv-core/src/runtime_ocr_anchor_bridge.rs` |
| Rust bridge CLI | `rust/chromalab-cv-core/src/bin/chromalab_cv_ocr_anchor_bridge.rs` |
| Benchmark schema | `benchmark/schemas/runtime-ocr-anchor-bridge-record.schema.json` |
| Benchmark runner | `tools/benchmark/run_r10_runtime_ocr_anchor_bridge_candidate.py` |
| Records | `benchmark/examples/r10_runtime_ocr_anchor_bridge_candidate/` |
| Report | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/contact_sheet_runtime_ocr_anchor_bridge.png` |
| Detail records | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/` |

Rules enforced by the bridge:

- accepted anchors require OCR numeric values and deterministic pixel geometry;
- empty source crop references are rejected;
- non-tick text roles are rejected;
- VLM pixel geometry is rejected;
- VLM numeric calibration values are rejected;
- missing crop image files are recorded as a promotion blocker instead of hidden.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Runtime OCR bridge records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Scoreable fixtures | 4 |
| Anchor-count parity pass | 4/4 |
| Bridge accepted anchors | 155 |
| Bridge rejected anchors | 20 |
| Missing source crop image files | 155 |
| Stage evidence status | REVIEW for 4 records, MISSING for 4 unannotated records |

R10 preserves the R9 accepted-anchor count exactly for every scoreable fixture.
It also preserves rejected upstream rows as rejected bridge rows.

## Fixture Table

| Fixture | Graphs | Layout class | R9 anchors | Bridge anchors | Rejected | Missing crop files | Decision |
|---|---:|---|---:|---:|---:|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 25 | 25 | 1 | 25 | runtime bridge REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | no R9 anchor truth |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | 0 | 0 | no R9 anchor truth |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 52 | 52 | 0 | 52 | runtime bridge REVIEW |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 40 | 40 | 10 | 40 | runtime bridge REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 38 | 38 | 9 | 38 | runtime bridge REVIEW |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | 0 | 0 | no R9 anchor truth |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | no R9 anchor truth |

## Product Meaning

R10 proves that safe OCR anchors can be carried through a deterministic Rust
runtime-shaped contract without making VLM numeric authority. It does not prove
Android runtime OCR generation, because the rows still originate from R9
benchmark evidence and do not include persisted crop image file paths.

Phase 9 remains blocked until runtime-generated bridge rows can feed the
calibration ensemble and the Android evidence package can show graph-level
anchor provenance.

## Next Phase

```text
R11 - Integrated Runtime Calibration Closure
```

R11 should feed R10 anchor rows into `CalibrationStrategyEnsemble` in a shadow
path, compare selected/rejected strategies against R8/R9 records, preserve White
Tiger legacy calibration fallback, and keep E2B advisory-only.
