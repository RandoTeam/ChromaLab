# R9 Stage 6 Automatic OCR Anchor Candidate Closeout

Date: 2026-06-07

Status: `R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_REVIEW`

Scope completed: PC-side Stage 6 automatic OCR anchor candidate using R8
calibration strategy evidence plus DRD/DRE OCR benchmark outputs. R9 did not
change Android runtime behavior, validators, graph-count metadata, report
gates, chromatographic math, model policy, or `CalculationEngine`.

## Why R9 Exists

R8 proved that calibration strategies can select coherent fits when
manual-review anchors are already present. That was not enough for production:
the next required question was whether automatic OCR and label-band evidence
can create usable scale anchors without treating manual truth as runtime input.

R9 answers that question in shadow mode. It consumes DRE6 safe OCR anchor
recovery and records which graphs already have automatic OCR anchors, residuals,
source provenance, and review/valid decisions.

## Implementation

| Area | Result |
|---|---|
| Benchmark schema | `benchmark/schemas/stage123456-parity-record.schema.json` |
| Benchmark runner | `tools/benchmark/run_r9_stage6_automatic_ocr_anchor_candidate.py` |
| Records | `benchmark/examples/r9_stage6_automatic_ocr_anchor_candidate/` |
| Report | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/summary.md` |
| Visual evidence | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/contact_sheet_automatic_ocr_anchor_candidate.png` |
| Detail records | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/` |

The candidate consumes:

- R8 Stage 5 calibration strategy parity records;
- DRD2 RapidOCR crop OCR benchmark summary;
- DRD4 text-box OCR benchmark summary;
- DRD6 axis-owned OCR preprocessing benchmark summary;
- DRE6 remaining-axis OCR recovery summary.

Rules enforced:

- accepted anchors require OCR-derived numeric values and pixel geometry;
- title, ion, m/z, legend, and other non-tick text remain rejected by the
  upstream safe OCR reports;
- no VLM pixel coordinates are used;
- fixtures without DRC4/DRE6 truth are marked missing instead of fabricated;
- all output remains shadow-only until Android or Rust runtime produces the
  same anchor rows.

## Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 6 records generated | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Annotated truth fixtures | 4 |
| Automatic OCR candidate graphs | 12 |
| Valid candidate graphs | 9 |
| Review candidate graphs | 3 |
| Accepted OCR anchors | 155 |
| Rejected OCR anchors | 20 |
| Mean fit RMSE px | 0.725662 |
| Mean truth tick RMSE px | 12.601638 |
| Stage evidence status | REVIEW for 4 records, MISSING for 4 unannotated records |

Best axis-owned OCR variant from DRD6:

```text
rapidocr_rgb_x3_p2_v1
```

It reached 0.7322 tick-label recall, 0.8397 safe role accuracy, and zero safe
false tick labels in the DRD6 axis-owned crop benchmark.

## Fixture Table

| Fixture | Graphs | Layout class | OCR candidate graphs | Valid | Review | Accepted anchors | Decision |
|---|---:|---|---:|---:|---:|---:|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 2 | 0 | 25 | automatic OCR anchor REVIEW |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | no DRC4/DRE6 OCR anchor truth |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | 0 | 0 | no DRC4/DRE6 OCR anchor truth |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 4 | 0 | 52 | automatic OCR anchor REVIEW |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 1 | 3 | 40 | automatic OCR anchor REVIEW |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 2 | 0 | 38 | automatic OCR anchor REVIEW |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | 0 | 0 | no DRC4/DRE6 OCR anchor truth |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | no DRC4/DRE6 OCR anchor truth |

## Product Meaning

R9 is evidence that the hard annotated fixtures already have useful PC-side
automatic OCR anchor candidates. It does not close Phase 9 because those anchors
are not yet produced by the Android or Rust runtime path and cannot be used as
production calibration evidence.

Phase 9 remains blocked until the runtime replacement pipeline can produce
equivalent OCR anchors with graph-level evidence, forbidden-text rejection,
residuals, and exportable failure packages.

## Next Phase

```text
R10 - Stage 6 Runtime OCR Anchor Bridge Candidate
```

R10 should bridge the safe OCR anchor generation into the replacement pipeline
without adding a permanent duplicate layer. Promotion requires Android or Rust
runtime parity against the R9 per-anchor evidence, not visual similarity.
