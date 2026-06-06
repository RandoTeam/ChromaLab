# R4 Rust Stage 1 Image Preparation Parity

Verdict: `R4_RUST_STAGE1_PARITY_REVIEW_READY_WITH_DECODER_LIMITS`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`

R4 compares the Rust Stage 1 bridge against the R3 PC image-preparation candidate.
It does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.

R3 visual reference contact sheet: `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png`

## Fixture Parity

| Fixture | Status | Rust variant | R3 variant | Variant parity | Status parity | SHA parity | Score delta | Issues |
|---|---|---|---|---|---|---|---:|---|
| `bench_01_mz71_screenshot_page` | REVIEW | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | PASS | source-only | 0.001837 | NORMALIZED_SHA_MISMATCH |
| `bench_02_mz92_belyi_tigr` | REVIEW | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | PASS | source-only | 0.000554 | NORMALIZED_SHA_MISMATCH |
| `bench_03_small_tic_export` | REVIEW | `autocontrast` | `autocontrast` | PASS | PASS | source-only | 0.0 | NORMALIZED_SHA_MISMATCH |
| `bench_04_stacked_xic_resolution` | PASS | `autocontrast` | `autocontrast` | PASS | PASS | source+normalized | 0.000168 | NONE |
| `bench_05_tic_plus_ions` | PASS | `autocontrast` | `autocontrast` | PASS | PASS | source+normalized | 0.00022 | NONE |
| `bench_06_photo_two_graphs_page` | REVIEW | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | PASS | source-only | 0.004277 | NORMALIZED_SHA_MISMATCH |
| `bench_07_rotated_page_photo` | REVIEW | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | PASS | source-only | 0.001024 | NORMALIZED_SHA_MISMATCH |
| `white_tiger_ion71` | REVIEW | `sharpened_autocontrast` | `sharpened_autocontrast` | PASS | PASS | source-only | 0.001178 | NORMALIZED_SHA_MISMATCH |

## Next Required Work

- Keep Rust Stage 1 shadow-only until Stage 2 graph discovery consumes the selected variant.
- Treat Rust/Pillow JPEG normalized-byte hash differences as decoder evidence, not runtime failure.
- Add Stage 2 graph discovery candidate only after this Rust Stage 1 bridge remains stable.
- Keep Android runtime unchanged until Stage 1-3 promotion gates pass.
