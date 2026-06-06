# R3 Image Preparation Candidate

Verdict: `R3_STAGE1_IMAGE_PREP_CANDIDATE_READY_FOR_RUST_PARITY`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`

R3 does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png`

## Fixture Results

| Fixture | Status | Selected variant | Score | Size | Contrast | Edge density | Warnings |
|---|---|---|---:|---|---:|---:|---|
| `bench_01_mz71_screenshot_page` | PASS | `sharpened_autocontrast` | 0.538457 | 964x1280 | 91.0 | 0.025611 | NONE |
| `bench_02_mz92_belyi_tigr` | PASS | `sharpened_autocontrast` | 0.513927 | 576x1280 | 252.0 | 0.02154 | NONE |
| `bench_03_small_tic_export` | REVIEW | `autocontrast` | 0.362928 | 381x132 | 32.0 | 0.092442 | LOW_RESOLUTION_INPUT |
| `bench_04_stacked_xic_resolution` | REVIEW | `autocontrast` | 0.291481 | 534x1110 | 12.0 | 0.067761 | LOW_CONTRAST_INPUT, WEAK_PREPROCESSING_VARIANT_SCORE |
| `bench_05_tic_plus_ions` | REVIEW | `autocontrast` | 0.278398 | 683x807 | 1.0 | 0.062376 | LOW_CONTRAST_INPUT, WEAK_PREPROCESSING_VARIANT_SCORE |
| `bench_06_photo_two_graphs_page` | PASS | `sharpened_autocontrast` | 0.725589 | 964x1280 | 127.0 | 0.022314 | NONE |
| `bench_07_rotated_page_photo` | PASS | `sharpened_autocontrast` | 0.539073 | 1280x964 | 72.0 | 0.02578 | NONE |
| `white_tiger_ion71` | PASS | `sharpened_autocontrast` | 0.546418 | 576x1280 | 252.0 | 0.028533 | NONE |

## Next Required Work

- Port the Stage 1 image-preparation candidate into a Rust parity bridge or equivalent Rust-owned primitive layer.
- Compare Rust Stage 1 output against the R3 PC candidate before production promotion.
- Only after Stage 1 parity is stable, add Stage 2 graph discovery candidate that consumes selected Stage 1 variants.
- Compare graph count and layout on the same fixtures before production promotion.
- Keep Android runtime unchanged until parity gates pass.
