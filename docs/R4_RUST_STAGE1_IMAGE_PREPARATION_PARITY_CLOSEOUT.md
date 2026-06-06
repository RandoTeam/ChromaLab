# R4 Rust Stage 1 Image Preparation Parity Closeout

Date: 2026-06-06

Status: `R4_RUST_STAGE1_PARITY_REVIEW_READY_WITH_DECODER_LIMITS`

Scope completed: Rust Stage 1 image-preparation parity bridge. R4 did not
change Android runtime behavior, validators, graph-count metadata, report
gates, chromatographic math, model policy, or `CalculationEngine`.

## Why R4 Exists

R3 proved that Stage 1 image preparation can be measured as a standalone
shadow candidate. R4 ports that candidate into Rust-owned primitives so future
graph/layout work can consume a fast deterministic Stage 1 layer without
blindly switching production behavior.

## Implementation

| Area | Result |
|---|---|
| Rust module | `rust/chromalab-cv-core/src/stage1_image_prep.rs` |
| Rust CLI | `rust/chromalab-cv-core/src/bin/chromalab_cv_stage1_prep.rs` |
| Benchmark runner | `tools/benchmark/run_r4_rust_stage1_image_preparation_parity.py` |
| Records | `benchmark/examples/r4_rust_stage1_image_preparation_parity/` |
| Report | `benchmark/reports/r4_rust_stage1_image_preparation_parity/summary.md` |

The Rust bridge decodes JPG/PNG inputs, computes source and normalized hashes,
builds the same Stage 1 variant family used in R3, computes image metrics,
scores variants, selects the best variant, emits warnings, and writes a
JSON report for each fixture.

Stage 1 variants:

- `source_rgb`;
- `grayscale`;
- `autocontrast`;
- `sharpened_autocontrast`;
- `otsu_binary`.

## Parity Result

| Metric | Result |
|---|---:|
| Fixtures processed | 8 |
| Stage 1 records generated | 8 |
| Selected variant parity | 8/8 |
| PASS/REVIEW status parity | 8/8 |
| Source file SHA parity | 8/8 |
| Normalized byte SHA parity | 2/8 |
| PASS records | 2 |
| REVIEW records | 6 |

The six `NORMALIZED_SHA_MISMATCH` cases are JPEG inputs. Their source file SHA
matches, dimensions match, selected variant matches, and PASS/REVIEW status
matches, but normalized RGB bytes differ between Pillow and Rust `image`
decoding. This is recorded as decoder evidence, not a production failure.

PNG fixtures matched both source and normalized SHA:

- `bench_04_stacked_xic_resolution`;
- `bench_05_tic_plus_ions`.

## Fixture Table

| Fixture | Rust variant | R3 variant | Status parity | Issue |
|---|---|---|---|---|
| `bench_01_mz71_screenshot_page` | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |
| `bench_02_mz92_belyi_tigr` | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |
| `bench_03_small_tic_export` | `autocontrast` | `autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |
| `bench_04_stacked_xic_resolution` | `autocontrast` | `autocontrast` | PASS | none |
| `bench_05_tic_plus_ions` | `autocontrast` | `autocontrast` | PASS | none |
| `bench_06_photo_two_graphs_page` | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |
| `bench_07_rotated_page_photo` | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |
| `white_tiger_ion71` | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | `NORMALIZED_SHA_MISMATCH` |

## Validation

Commands run:

```powershell
.\tools\rust\Install-LocalRustToolchain.ps1
.\tools\rust\Run-RustCoreChecks.ps1
python tools\benchmark\run_r4_rust_stage1_image_preparation_parity.py --clean
```

Further validation is recorded in the final R4 work-slice status.

## Product Meaning

R4 proves that Rust can own the Stage 1 image-preparation candidate in shadow
mode with stable variant/status parity against R3. It does not prove graph
detection, plotArea detection, calibration, trace extraction, peak calculation,
or report readiness.

Phase 9 remains blocked. R4 is infrastructure for the next analyzer repair
layer, not product acceptance.

## Next Phase

```text
R5 - Stage 2 Graph Discovery Candidate
```

R5 should consume the Rust Stage 1 selected variant and build graphPanel
candidate evidence for the same eight fixtures. It must compare graph count and
layout against the current benchmark records before any runtime promotion.

Update 2026-06-06: R5 completed the Stage 2 graph discovery candidate in
`docs/R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_CLOSEOUT.md`.
