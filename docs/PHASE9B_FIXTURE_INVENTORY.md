# Phase 9B Fixture Inventory

Date: 2026-05-20

Phase 9B expands Android real-device validation beyond the single `white_tiger_ion71` fixture. The suite uses repository-owned bench images and the existing bundled White Tiger validation fixture. No fixture contains hardcoded success coordinates.

## Selected Android Validation Fixtures

| Fixture id | Source path | Android asset | Image type | Expected graph count | Known historical failure class | Included |
| --- | --- | --- | --- | ---: | --- | --- |
| `white_tiger_ion71` | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` | `validation/white_tiger_ion71_fixture.jpg` | Original Ion 71 / White Tiger phone screenshot | 1 | `AXIS_DETECTION_FAILURE`, `TICK_LOCALIZATION_FAILURE`, `CALIBRATION_FAILURE` | Yes |
| `bench_01_mz71_screenshot_page` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_01_mz71_screenshot_page.jpg` | `validation/bench_01_mz71_screenshot_page.jpg` | Printed page with two chromatograms | 2 | `MULTI_GRAPH_SPLIT_FAILURE`, `GRAPH_PANEL_FAILURE`, `CALIBRATION_FAILURE` | Yes |
| `bench_02_mz92_belyi_tigr` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_02_mz92_belyi_tigr.jpg` | `validation/bench_02_mz92_belyi_tigr.jpg` | Belyi Tigr m/z 92 screenshot | 1 | `PLOT_AREA_FAILURE`, `TRACE_EXTRACTION_FAILURE` | Yes |
| `bench_03_small_tic_export` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_03_small_tic_export.jpg` | `validation/bench_03_small_tic_export.jpg` | Small clean TIC export | 1 | `OCR_TICK_FAILURE`, `PEAK_EVIDENCE_FAILURE` | Yes |
| `bench_04_stacked_xic_resolution` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_04_stacked_xic_resolution.png` | `validation/bench_04_stacked_xic_resolution.png` | Four stacked XIC panels | 4 | `MULTI_GRAPH_SPLIT_FAILURE`, `SPARSE_TRACE_REVIEW` | Yes |
| `bench_05_tic_plus_ions` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_05_tic_plus_ions.png` | `validation/bench_05_tic_plus_ions.png` | TIC plus ion traces | 4 | `MULTI_GRAPH_SPLIT_FAILURE`, `SPARSE_TRACE_REVIEW` | Yes |
| `bench_06_photo_two_graphs_page` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_06_photo_two_graphs_page.jpg` | `validation/bench_06_photo_two_graphs_page.jpg` | Phone photo with two graph panels | 2 | `ORIENTATION_FAILURE`, `MULTI_GRAPH_SPLIT_FAILURE`, `CALIBRATION_FAILURE` | Yes |
| `bench_07_rotated_page_photo` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_07_rotated_page_photo.jpg` | `validation/bench_07_rotated_page_photo.jpg` | Rotated photographed page | 1 | `ORIENTATION_FAILURE`, `AXIS_DETECTION_FAILURE` | Yes |

## Excluded Candidates

| Candidate | Reason excluded |
| --- | --- |
| `bench_08_mz71_duplicate_candidate` | Same image class and byte size as the existing `white_tiger_ion71` Android fixture. Kept in desktop bench resources, but not duplicated in the Android Phase 9B selected set. |
| Historical artifact overlays under `artifacts/phase0*`, `artifacts/phase8*`, `artifacts/phase9*` | Derived outputs, not source fixtures. Used only as audit evidence. |

## Coverage

The selected set covers: Ion 71 / White Tiger, clean/small export, Belyi Tigr m/z 92, stacked multi-graph pages, TIC plus ion traces, photographed page input, rotated page input, sparse/faint trace risks, dense peak risks, and multi-graph split risks.
