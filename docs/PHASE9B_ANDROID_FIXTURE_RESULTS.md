# Phase 9B Android Fixture Results

Date: 2026-05-20

Device: `10AF5M15FY003YL`  
Package: `com.chromalab.app.validation`  
Suite runner: `tools/phase9b/run_android_validation_suite.ps1`  
Local artifact root: `artifacts/phase9b-multi-fixture-android/`

## Model Precheck

| Model | Status | Evidence |
| --- | --- | --- |
| Gemma-4-E2B LiteRT-LM | AVAILABLE | `run-as com.chromalab.app.validation ls -l files/models/gemma4-e2b` shows `gemma-4-E2B-it.litertlm`, 2,588,147,712 bytes. |
| Gemma-4-E4B LiteRT-LM | NOT_INSTALLED | `files/models/gemma4-e4b` is absent. E4B was not run. |

## Per-Run Results

| Fixture id | Mode | Run id | Graphs / expected | Report gate | Validator | Runtime failure class | Export status | Decision | Required next action |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | `white_tiger_ion71_20260520_201426` | 1 / 1 | REVIEW_ONLY | REVIEW | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | complete | REVIEW | Keep as deterministic baseline; still not release-ready. |
| `white_tiger_ion71` | E2B model-enabled | `white_tiger_ion71_20260520_201503` | 1 / 1 | REVIEW_ONLY | PASS | `UNKNOWN_FAILURE` | complete | REVIEW | E2B does not regress this fixture; still review-only. |
| `bench_01_mz71_screenshot_page` | deterministic | `bench_01_mz71_screenshot_page_20260520_201551` | 0 / 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Fix photographed-page/tick localization path with graph-level evidence. |
| `bench_01_mz71_screenshot_page` | E2B model-enabled | `bench_01_mz71_screenshot_page_20260520_201643` | 0 / 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Same as deterministic; E2B does not rescue geometry. |
| `bench_02_mz92_belyi_tigr` | deterministic | `bench_02_mz92_belyi_tigr_20260520_201736` | 2 / 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | FAIL | Fix duplicate/partial graphPanel selection for single physical graph. |
| `bench_02_mz92_belyi_tigr` | E2B model-enabled | `bench_02_mz92_belyi_tigr_20260520_201838` | 0 / 1 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Investigate model-enabled runtime regression after deterministic duplicate selection. |
| `bench_03_small_tic_export` | deterministic | `bench_03_small_tic_export_20260520_202144` | 1 / 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | REVIEW | Low-res clean export remains diagnostic; improve graphPanel/calibration confidence later. |
| `bench_03_small_tic_export` | E2B model-enabled | `bench_03_small_tic_export_20260520_202156` | 1 / 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | REVIEW | E2B does not worsen graph count; no release-ready claim. |
| `bench_04_stacked_xic_resolution` | deterministic | `bench_04_stacked_xic_resolution_20260520_202223` | 0 / 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Multi-graph stacked panel path fails before reportable graphs. |
| `bench_04_stacked_xic_resolution` | E2B model-enabled | `bench_04_stacked_xic_resolution_20260520_202250` | 0 / 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Same as deterministic; E2B does not rescue geometry. |
| `bench_05_tic_plus_ions` | deterministic | `bench_05_tic_plus_ions_20260520_202317` | 0 / 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Multi-ion stacked path fails before reportable graphs. |
| `bench_05_tic_plus_ions` | E2B model-enabled | `bench_05_tic_plus_ions_20260520_202339` | 0 / 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Same as deterministic; E2B does not rescue geometry. |
| `bench_06_photo_two_graphs_page` | deterministic | `bench_06_photo_two_graphs_page_20260520_202401` | 0 / 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Photographed two-graph page fails tick localization before reportable graphs. |
| `bench_06_photo_two_graphs_page` | E2B model-enabled | `bench_06_photo_two_graphs_page_20260520_202453` | 0 / 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED | Same as deterministic; E2B does not rescue geometry. |
| `bench_07_rotated_page_photo` | deterministic | `bench_07_rotated_page_photo_20260520_202546` | 1 / 1 | REVIEW_ONLY | REVIEW | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | complete | REVIEW | Rotated page reaches review-only report; no release-ready claim. |
| `bench_07_rotated_page_photo` | E2B model-enabled | `bench_07_rotated_page_photo_20260520_202654` | 1 / 1 | REVIEW_ONLY | PASS | `UNKNOWN_FAILURE` | complete | REVIEW | E2B does not regress this fixture; still review-only. |

## Counts

| Category | Count |
| --- | ---: |
| Fixtures selected | 8 |
| Runs executed | 16 |
| Export-complete runs | 16 |
| REVIEW decisions | 6 |
| FAIL decisions | 1 |
| BLOCKED decisions | 9 |
| RELEASE_READY runs | 0 |

## Phase 9B Acceptance

Phase 9B does not pass. The app now has enough Android evidence to reject Phase 9 acceptance:

- multiple fixtures fail before reportable graph evidence;
- one single-graph fixture is split into two graphs in deterministic mode;
- E2B model-enabled mode regresses `bench_02_mz92_belyi_tigr` from two diagnostic graphs to zero blocked graphs;
- all runs exported runtime evidence, validator JSON/Markdown, final report JSON, reports, timings, overlays or explicit missing reasons, so this is a runtime acceptance failure rather than an export failure.

## Phase 9C Update

Phase 9C reran all eight fixtures after tick OCR provenance and suite-summary repairs. The result remains blocked:

- `white_tiger_ion71` and `bench_07_rotated_page_photo` remain stable review-only baselines in both modes;
- `bench_02_mz92_belyi_tigr` deterministic mode now reports one completed graph in the suite summary, but E2B mode remains `BLOCKED`;
- `bench_01`, `bench_04`, `bench_05`, and `bench_06` still stop at `TICK_LOCALIZATION_FAILURE` with one graph failure package each, below expected multi-graph counts;
- all 16 Phase 9C runs exported complete required artifacts.

Final Phase 9C evidence is recorded in `docs/PHASE9C_ANDROID_RERUN_RESULTS.md` and local artifacts under `artifacts/phase9c-multi-fixture-android/`.
