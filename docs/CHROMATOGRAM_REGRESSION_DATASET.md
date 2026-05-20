# ChromaLab Phase 8 Regression Dataset

## Purpose

This dataset inventory names every known real-image class and required synthetic acceptance case for Phase 8. It is an acceptance contract, not a coordinate lock. Paths are repo-relative and must not be copied into user-facing public reports.

## Required Artifact Set

Every dataset item requires:

- RuntimeEvidencePackage JSON;
- validator JSON;
- validator Markdown;
- final report contract JSON;
- HTML export when a report is generated;
- Markdown export when a report is generated;
- graphPanel overlay;
- plotArea overlay;
- axis/tick/calibration evidence;
- trace overlay;
- peak overlay and peak evidence table;
- report gate status;
- failure classification if not `RELEASE_READY`;
- stage timings and model/runtime info;
- privacy manifest.

## Dataset Inventory

| ID | Source image path | Image type | Expected graphs | Expected autonomous status | Expected failure if not release-ready | Known previous bugs | Current status | Owner agent |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| `bench_02_mz92_belyi_tigr` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_02_mz92_belyi_tigr.jpg` | clean phone screenshot with title/ion text | 1 | `REVIEW_ONLY` | `CALIBRATION_FAILURE` until Android OCR/tick evidence proves valid calibration | Android screenshot calibration incomplete without tick confidence | Inventory ready; covered by bench fixture resources and Phase 8 report goldens | QA / Regression Agent |
| `bench_08_mz71_duplicate_candidate` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg` | Android screenshot / embedded graph, original Ion 71 / White Tiger class | 1 | `REVIEW_ONLY` | `GRAPH_PANEL_FAILURE` if full graphPanel is not selected | right-side graphPanel crop; six pseudo-graph reports; dense peaks must not collapse | Inventory ready; historical failure row | Geometry / Calibration Core Agent |
| `bench_03_small_tic_export` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_03_small_tic_export.jpg` | already-cropped white chart panel with low-resolution labels | 1 | `DIAGNOSTIC_ONLY` | `TRACE_EXTRACTION_FAILURE` or `SPARSE_TRACE_REVIEW` | fixture label recovery must remain test-only until runtime OCR evidence exists | Inventory ready | Trace Extraction / Peak Review Agent |
| `bench_06_photo_two_graphs_page` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_06_photo_two_graphs_page.jpg` | phone photo with mild perspective, real two-graph page | 2 | `REVIEW_ONLY` | `CALIBRATION_FAILURE` until valid autonomous calibration exists | perspective uncertainty and multi-graph split risk | Inventory ready | Product Acceptance Agent |
| `bench_07_rotated_page_photo` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_07_rotated_page_photo.jpg` | phone photo with strong orientation correction requirement | 1 | `REVIEW_ONLY` | `ORIENTATION_FAILURE` if full graphPanel is cropped after rotation | orientation correction must preserve full panel | Inventory ready | Android Performance & On-Device AI Agent |
| `bench_04_stacked_xic_resolution` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_04_stacked_xic_resolution.png` | stacked XIC real multi-graph page | 4 | `REVIEW_ONLY` | `MULTI_GRAPH_SPLIT_FAILURE` if graph order/count changes | stacked graph ordering must stay stable | Inventory ready | QA / Regression Agent |
| `bench_05_tic_plus_ions` | `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_05_tic_plus_ions.png` | TIC plus ion traces, Russian labels | 4 | `REVIEW_ONLY` | `OCR_TICK_FAILURE` or text-classification review | labels and ion metadata must not become peak labels | Inventory ready | OCR / VLM / Text Semantics Agent |
| `phase8_missing_tick_labels_synthetic` | `docs/regression/synthetic/missing_tick_labels.md` | synthetic missing tick label case | 1 | `DIAGNOSTIC_ONLY` | `TICK_LOCALIZATION_FAILURE` | release-ready claim with missing tick labels | Test-only acceptance case | Geometry / Calibration Core Agent |
| `phase8_invalid_calibration_synthetic` | `docs/regression/synthetic/invalid_calibration.md` | synthetic invalid calibration case | 1 | `DIAGNOSTIC_ONLY` | `CALIBRATION_FAILURE` | release-ready claim with invalid calibration | Test-only acceptance case | Scientific Reporting & Validation Agent |
| `phase8_six_pseudo_graph_android_failure` | `docs/regression/synthetic/six_pseudo_graph_android_failure.md` | historical Android failure class | 1 | `BLOCKED` | `MULTI_GRAPH_SPLIT_FAILURE` | six pseudo-graph reports | Test-only acceptance case | Product Acceptance Agent |
| `phase8b_white_tiger_ion71_android_fixture` | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` | bundled Android validation fixture, original Ion 71 / White Tiger style screenshot | 1 | `BLOCKED` until chromatogram VLM is installed/activated, then `DIAGNOSTIC_ONLY` or better based on evidence gates | `VLM_MODEL_UNAVAILABLE` on device `a36d1946`; after model activation expected failures may be `AXIS_DETECTION_FAILURE`, `TICK_LOCALIZATION_FAILURE`, `OCR_TICK_FAILURE`, or `CALIBRATION_FAILURE` until autonomous evidence passes | camera/gallery validation ambiguity; axis detection failure in real usage | Fixture run completed via side-by-side validation package; terminal evidence artifacts exported under `/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_162317/` | QA / Regression Agent |
| `phase9_white_tiger_model_comparison` | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` | bundled Android validation fixture, deterministic versus Gemma comparison | 1 | `REVIEW_ONLY` | `VLM_SEMANTIC_LAYER_UNAVAILABLE` is allowed for no-model baseline; E4B missing is documented until model is installed | model-enabled run originally regressed to `TICK_LOCALIZATION_FAILURE`; fixed by post-calibration Gemma activation | No-model run `white_tiger_ion71_20260520_192547` and E2B run `white_tiger_ion71_20260520_192400` completed with graph count 1, valid calibration, zero blocking issues | Android Performance & On-Device AI Agent |
| `phase9b_multi_fixture_android_suite` | `composeApp/src/androidMain/assets/validation/` | bundled Android validation suite with eight fixtures | mixed: 1, 2, and 4 graph cases | `BLOCKED` | `TICK_LOCALIZATION_FAILURE`, `GRAPH_PANEL_FAILURE`, or E2B graph-count regression depending fixture | Phase 9 could not be accepted from one fixture; multi-fixture real Android validation required | Suite run completed on `10AF5M15FY003YL`; 16/16 exports complete, but 9 runs blocked and 1 failed. Phase 9B verdict is `PHASE_9B_BLOCKED_RUNTIME_FAILURE` | QA / Regression Agent |

## Coverage Mapping

| Required class | Covered by |
| --- | --- |
| clean screenshot | `bench_02_mz92_belyi_tigr` |
| Android screenshot / embedded graph | `bench_08_mz71_duplicate_candidate` |
| already-cropped white chart panel | `bench_03_small_tic_export` |
| phone photo with mild perspective | `bench_06_photo_two_graphs_page` |
| phone photo with strong perspective | `bench_07_rotated_page_photo` |
| graph with title/ion text | `bench_02_mz92_belyi_tigr`, `bench_08_mz71_duplicate_candidate` |
| graph with labels inside plotArea | `bench_03_small_tic_export` |
| weak/faint peaks | `bench_03_small_tic_export` |
| dense peaks | `bench_08_mz71_duplicate_candidate` |
| real multi-graph page | `bench_04_stacked_xic_resolution`, `bench_05_tic_plus_ions`, `bench_06_photo_two_graphs_page` |
| one graph producing many ROI candidates | `bench_08_mz71_duplicate_candidate` |
| missing tick labels | `phase8_missing_tick_labels_synthetic` |
| invalid calibration case | `phase8_invalid_calibration_synthetic` |
| sparse/fragmented trace case | `bench_03_small_tic_export` |
| original user graph / Ion 71 / White Tiger case | `bench_08_mz71_duplicate_candidate`, `bench_07_rotated_page_photo` |
| bench_03 / bench_04 / bench_08 classes | `bench_03_small_tic_export`, `bench_04_stacked_xic_resolution`, `bench_08_mz71_duplicate_candidate` |
| known failed Android runs | `bench_08_mz71_duplicate_candidate`, `bench_05_tic_plus_ions` |
| fixture-driven Android validation | `phase8b_white_tiger_ion71_android_fixture` |
| model-enabled Android validation | `phase9_white_tiger_model_comparison` |
| multi-fixture Android validation | `phase9b_multi_fixture_android_suite` |

## Acceptance Notes

- Current desktop bench fixtures intentionally keep calibration blocked until valid calibration evidence exists. That is not a release-ready failure; it is the correct diagnostic gate until Android/OCR calibration can prove the full chain.
- A dataset item may only move from `REVIEW_ONLY`/`DIAGNOSTIC_ONLY` to `RELEASE_READY` after its evidence package, validator JSON/Markdown, report JSON, overlays, and privacy checks are present.
- Product Acceptance and QA must approve any change from expected failure to expected pass.
- The Phase 8B fixture row bypasses only image acquisition. It must still enter the same autonomous processing flow after the fixture is copied into app-private capture storage.
