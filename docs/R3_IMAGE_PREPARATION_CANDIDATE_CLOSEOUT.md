# R3 Image Preparation Candidate Closeout

Date: 2026-06-06

Verdict: `R3_STAGE1_IMAGE_PREP_CANDIDATE_READY_FOR_RUST_PARITY`

Scope completed: PC-side shadow Stage 1 image-preparation candidate. R3 did not
change Android runtime behavior, validators, graph selection, plotArea
selection, report gates, graph-count expectations, chromatographic math, model
policy, or `CalculationEngine`.

## Purpose

R3 creates the first measurable Stage 1 candidate before Rust promotion.

The candidate:

- decodes all eight Android validation fixture images;
- applies EXIF transpose and RGB normalization;
- computes original and normalized SHA-256 hashes;
- builds preprocessing variants;
- ranks variants by contrast, edge density, and dark-pixel balance;
- records quality metrics and warnings;
- writes preview artifacts and a contact sheet;
- emits schema-backed `stage123-parity-record.json` records.

## Files Created

| Path | Role |
|---|---|
| `tools/benchmark/run_r3_image_preparation_candidate.py` | PC-side Stage 1 candidate harness. |
| `benchmark/examples/r3_image_preparation_candidate/` | Eight Stage 1 `stage123-parity-record.json` examples. |
| `benchmark/reports/r3_image_preparation_candidate/summary.json` | Machine-readable R3 summary. |
| `benchmark/reports/r3_image_preparation_candidate/summary.md` | Human-readable R3 summary. |
| `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png` | Visual preview of source and selected variants. |
| `docs/R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md` | This closeout. |

## Files Updated

| Path | Change |
|---|---|
| `benchmark/schemas/stage123-parity-record.schema.json` | Added R3 source kind and Stage 1 promotion decision. |
| `benchmark/README.md` | Added R3 generation and validation commands. |
| `docs/AUTONOMOUS_ANALYZER_SOURCE_OF_TRUTH_INDEX.md` | Added R3 docs, examples, and report paths. |
| `docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md` | Marked R3 complete and set R4 as Rust Stage 1 parity bridge. |
| `docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md` | Updated replacement progress and next phase. |
| `docs/README.md` | Added R3 closeout link. |
| `docs/CHROMATOGRAM_REGRESSION_MATRIX.md` | Added R3 regression-matrix update. |
| `docs/CHROMALAB_VALIDATION_SUMMARY.md` | Added R3 benchmark summary. |
| `ROADMAP.md` | Added R3 status under Rust CV production integration. |
| `PIPELINE.md` | Clarified R3 as shadow-only benchmark tooling. |
| `docs/R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md` | Marked R3 follow-up as completed and updated next phase. |

## Result Summary

| Fixture | Status | Selected variant | Score | Warning |
|---|---|---|---:|---|
| `bench_01_mz71_screenshot_page` | `PASS` | `sharpened_autocontrast` | 0.538457 | none |
| `bench_02_mz92_belyi_tigr` | `PASS` | `sharpened_autocontrast` | 0.513927 | none |
| `bench_03_small_tic_export` | `REVIEW` | `autocontrast` | 0.362928 | `LOW_RESOLUTION_INPUT` |
| `bench_04_stacked_xic_resolution` | `REVIEW` | `autocontrast` | 0.291481 | `LOW_CONTRAST_INPUT`, `WEAK_PREPROCESSING_VARIANT_SCORE` |
| `bench_05_tic_plus_ions` | `REVIEW` | `autocontrast` | 0.278398 | `LOW_CONTRAST_INPUT`, `WEAK_PREPROCESSING_VARIANT_SCORE` |
| `bench_06_photo_two_graphs_page` | `PASS` | `sharpened_autocontrast` | 0.725589 | none |
| `bench_07_rotated_page_photo` | `PASS` | `sharpened_autocontrast` | 0.539073 | none |
| `white_tiger_ion71` | `PASS` | `sharpened_autocontrast` | 0.546418 | none |

Counts:

- `PASS`: 5;
- `REVIEW`: 3;
- `FAIL`: 0;
- records: 8;
- schemas validated: 6;
- example documents validated: 117.

## Visual Evidence

Contact sheet:

```text
benchmark/reports/r3_image_preparation_candidate/contact_sheet.png
```

Spot-check result: source/selected preview pairs render and are not blank.

## Interpretation

R3 is useful because it turns Stage 1 image preparation into a measured,
repeatable shadow output.

R3 does not prove graph detection, calibration, trace extraction, peak evidence,
or release readiness.

The low-contrast warnings on `bench_04_stacked_xic_resolution` and
`bench_05_tic_plus_ions` are important: these fixtures should not be forced to
PASS by presentation. Their Stage 1 output is review-grade until the next layer
proves graph/layout robustness.

## Commands

Generate R3 records:

```powershell
python tools\benchmark\run_r3_image_preparation_candidate.py --clean
```

Validate benchmark schemas and examples:

```powershell
python tools\benchmark\validate_benchmark_schemas.py
```

Expected validation result:

```text
Benchmark schema validation passed: 6 schemas, 117 example documents.
```

## Next Phase

```text
R4 - Rust Stage 1 Image Preparation Parity Bridge
```

R4 should port or bridge the R3 Stage 1 candidate into Rust-owned primitives and
compare Rust output against the R3 PC records before any production promotion.

Update 2026-06-06: R4 completed this bridge in
`docs/R4_RUST_STAGE1_IMAGE_PREPARATION_PARITY_CLOSEOUT.md`.
