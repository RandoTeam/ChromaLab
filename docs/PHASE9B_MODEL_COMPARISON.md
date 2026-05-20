# Phase 9B Deterministic vs E2B Comparison

Date: 2026-05-20

The Phase 9B suite ran each selected fixture in deterministic mode and E2B model-enabled mode. VLM remains a semantic/OCR/judge assistant only and must not create or alter chromatographic numeric metrics.

| Fixture | Deterministic graph count | E2B graph count | Deterministic gate / validator | E2B gate / validator | Comparison result |
| --- | ---: | ---: | --- | --- | --- |
| `white_tiger_ion71` | 1 | 1 | REVIEW_ONLY / REVIEW | REVIEW_ONLY / PASS | No geometry regression. E2B improves validator verdict by removing semantic-layer unavailable warning. |
| `bench_01_mz71_screenshot_page` | 0 | 0 | BLOCKED / REVIEW | BLOCKED / REVIEW | Both modes fail tick localization. No model-specific improvement. |
| `bench_02_mz92_belyi_tigr` | 2 | 0 | DIAGNOSTIC_ONLY / REVIEW | BLOCKED / REVIEW | E2B mode worsens the run from diagnostic graph evidence to blocked zero-graph output. This blocks acceptance. |
| `bench_03_small_tic_export` | 1 | 1 | DIAGNOSTIC_ONLY / REVIEW | DIAGNOSTIC_ONLY / REVIEW | No graph-count regression; both modes remain diagnostic due graph/evidence weakness. |
| `bench_04_stacked_xic_resolution` | 0 | 0 | BLOCKED / REVIEW | BLOCKED / REVIEW | Both modes fail tick localization before reportable graphs. |
| `bench_05_tic_plus_ions` | 0 | 0 | BLOCKED / REVIEW | BLOCKED / REVIEW | Both modes fail tick localization before reportable graphs. |
| `bench_06_photo_two_graphs_page` | 0 | 0 | BLOCKED / REVIEW | BLOCKED / REVIEW | Both modes fail tick localization before reportable graphs. |
| `bench_07_rotated_page_photo` | 1 | 1 | REVIEW_ONLY / REVIEW | REVIEW_ONLY / PASS | No geometry regression. E2B improves validator verdict by removing semantic-layer unavailable warning. |

## Safety Findings

- No run showed VLM-created RT, height, area, FWHM, S/N, baseline, Kovats, or calibration coefficients.
- No report claimed `RELEASE_READY`.
- E2B did not corrupt the successful deterministic fixtures, but it did regress `bench_02_mz92_belyi_tigr` to a blocked zero-graph run.
- The model-enabled path cannot be accepted until the `bench_02` regression is fixed or classified with Product, Scientific, and QA approval.
