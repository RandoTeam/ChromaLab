# Phase 9J Autonomous Analysis Truth Audit

Status: `PHASE_9_TRUTH_AUDIT_COMPLETE`

Phase 10 may start: **No**

This is an evidence audit only. No geometry, tick, calibration, chromatographic math, validator, or CalculationEngine repair was made.

## Source Artifacts

- Android suite source: `artifacts/phase9i-final-android/`
- Truth package: `artifacts/phase9j-truth-audit/`
- Summary JSON: `artifacts/phase9j-truth-audit/phase9j_summary.json`
- Summary Markdown: `artifacts/phase9j-truth-audit/phase9j_summary.md`

The `artifacts/` tree is intentionally ignored by git; the truth package exists locally for inspection and the committed docs point to those paths.

No Android rerun was needed because the Phase 9I suite already has RuntimeEvidencePackage, validator JSON/Markdown, report JSON, HTML/Markdown report, and manifests for all 16 runs.

## Product-Level Truth Table

| Fixture | Mode | Expected graph count | Detected graph count | Report gate | Validator verdict | graphPanel | plotArea | X calibration | Y calibration | Trace | Peak evidence | Peak count | Failure class | First failing stage | Runtime evidence | Report JSON | HTML/MD report | Overlays | Product decision | Reason |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | 1 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 12 | PEAK_EVIDENCE_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `white_tiger_ion71` | E2B | 1 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 12 | PEAK_EVIDENCE_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_01_mz71_screenshot_page` | deterministic | 2 | 0 | BLOCKED | REVIEW | PRESENT | PRESENT | VALID | INVALID | MISSING | MISSING | 0 | TICK_LOCALIZATION_FAILURE | Y_CALIBRATION | yes | yes | yes | no | BLOCKED | Y calibration blocked by insufficient usable anchors after OCR/tick pairing. |
| `bench_01_mz71_screenshot_page` | E2B | 2 | 0 | BLOCKED | REVIEW | PRESENT | PRESENT | VALID | INVALID | MISSING | MISSING | 0 | TICK_LOCALIZATION_FAILURE | Y_CALIBRATION | yes | yes | yes | no | BLOCKED | Y calibration blocked by insufficient usable anchors after OCR/tick pairing. |
| `bench_02_mz92_belyi_tigr` | deterministic | 1 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | GRAPH_PANEL_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_02_mz92_belyi_tigr` | E2B | 1 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | GRAPH_PANEL_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_03_small_tic_export` | deterministic | 1 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 3 | PEAK_EVIDENCE_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_03_small_tic_export` | E2B | 1 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 3 | PEAK_EVIDENCE_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_04_stacked_xic_resolution` | deterministic | 4 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_04_stacked_xic_resolution` | E2B | 4 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | UNKNOWN_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_05_tic_plus_ions` | deterministic | 4 | 0 | BLOCKED | REVIEW | PRESENT | PRESENT | VALID | REVIEW | MISSING | MISSING | 0 | CALIBRATION_FAILURE | Y_CALIBRATION | yes | yes | yes | no | BLOCKED | TIC+ions layout not propagated and Y calibration direction is inconsistent. |
| `bench_05_tic_plus_ions` | E2B | 4 | 0 | BLOCKED | REVIEW | PRESENT | PRESENT | VALID | REVIEW | MISSING | MISSING | 0 | CALIBRATION_FAILURE | Y_CALIBRATION | yes | yes | yes | no | BLOCKED | TIC+ions layout not propagated and Y calibration direction is inconsistent. |
| `bench_06_photo_two_graphs_page` | deterministic | 2 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_06_photo_two_graphs_page` | E2B | 2 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | UNKNOWN_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_07_rotated_page_photo` | deterministic | 1 | 1 | REVIEW_ONLY | REVIEW | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `bench_07_rotated_page_photo` | E2B | 1 | 1 | REVIEW_ONLY | PASS | PRESENT | PRESENT | INFERRED | INFERRED | AVAILABLE | AVAILABLE | 1 | UNKNOWN_FAILURE |  | yes | yes | yes | yes | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |

## Contact Sheets

- `artifacts/phase9j-truth-audit/phase9j_contact_sheet_inputs.png`
- `artifacts/phase9j-truth-audit/phase9j_contact_sheet_graphpanels.png`
- `artifacts/phase9j-truth-audit/phase9j_contact_sheet_calibration.png`
- `artifacts/phase9j-truth-audit/phase9j_contact_sheet_reports.png`

## Fixture Evidence

### white_tiger_ion71 / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/runtime_evidence_package_run_1779810235097.json` |
| Validator JSON | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/runtime_evidence_validation_run_1779810235097.json` |
| Validator Markdown | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/runtime_evidence_validation_run_1779810235097.md` |
| Final report JSON | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/final_report_contract_run_1779810235097.json` |
| HTML report | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/report_run_1779810235097.html` |
| Markdown report | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/report_run_1779810235097.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/white_tiger_ion71/deterministic/white_tiger_ion71_20260526_184317/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `PEAK_EVIDENCE_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 31.59 | 4.191e+05 | 2.314e+05 | 6.245 | 0.5424 | 1807 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 2 | 32.31 | 1.587e+05 | 8.784e+04 | 2.371 | 0.5326 | 684.3 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 3 | 33.54 | 4.36e+05 | 2.782e+05 | 7.507 | 0.6059 | 1879 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning |
| 4 | 34.47 | 7.358e+04 | 3.837e+04 | 1.036 | 0.5776 | 317.2 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 5 | 35.39 | 5.062e+05 | 3.914e+05 | 10.56 | 0.7933 | 2182 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 6 | 36.83 | 4.867e+05 | 3.952e+05 | 10.67 | 0.8221 | 2098 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 7 | 38.27 | 4.872e+05 | 4.438e+05 | 11.98 | 0.9248 | 2100 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 8 | 39.09 | 4.871e+05 | 3.476e+05 | 9.382 | 0.7193 | 2100 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 9 | 39.91 | 4.848e+05 | 2.486e+05 | 6.709 | 0.5138 | 2090 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 10 | 40.84 | 4.879e+05 | 4.985e+05 | 13.45 | 1.028 | 2103 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |

### white_tiger_ion71 / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/runtime_evidence_package_run_1779810291646.json` |
| Validator JSON | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/runtime_evidence_validation_run_1779810291646.json` |
| Validator Markdown | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/runtime_evidence_validation_run_1779810291646.md` |
| Final report JSON | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/final_report_contract_run_1779810291646.json` |
| HTML report | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/report_run_1779810291646.html` |
| Markdown report | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/report_run_1779810291646.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/white_tiger_ion71/model_enabled/white_tiger_ion71_20260526_184400/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `PEAK_EVIDENCE_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 31.59 | 4.191e+05 | 2.314e+05 | 6.245 | 0.5424 | 1807 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 2 | 32.31 | 1.587e+05 | 8.784e+04 | 2.371 | 0.5326 | 684.3 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 3 | 33.54 | 4.36e+05 | 2.782e+05 | 7.507 | 0.6059 | 1879 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning |
| 4 | 34.47 | 7.358e+04 | 3.837e+04 | 1.036 | 0.5776 | 317.2 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 5 | 35.39 | 5.062e+05 | 3.914e+05 | 10.56 | 0.7933 | 2182 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 6 | 36.83 | 4.867e+05 | 3.952e+05 | 10.67 | 0.8221 | 2098 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 7 | 38.27 | 4.872e+05 | 4.438e+05 | 11.98 | 0.9248 | 2100 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 8 | 39.09 | 4.871e+05 | 3.476e+05 | 9.382 | 0.7193 | 2100 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 9 | 39.91 | 4.848e+05 | 2.486e+05 | 6.709 | 0.5138 | 2090 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 10 | 40.84 | 4.879e+05 | 4.985e+05 | 13.45 | 1.028 | 2103 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |

### bench_01_mz71_screenshot_page / deterministic

Verdict: **BLOCKED**. Y calibration blocked by insufficient usable anchors after OCR/tick pairing.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_01_mz71_screenshot_page.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/runtime_evidence_package_bench_01_mz71_screenshot_page_20260526_184458.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/runtime_evidence_validation_bench_01_mz71_screenshot_page_20260526_184458.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/runtime_evidence_validation_bench_01_mz71_screenshot_page_20260526_184458.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/final_report_contract_bench_01_mz71_screenshot_page_20260526_184458.json` |
| HTML report | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/report_bench_01_mz71_screenshot_page_20260526_184458.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/report_bench_01_mz71_screenshot_page_20260526_184458.md` |
| GraphPanel overlay | missing |
| PlotArea overlay | missing |
| Axis/tick/calibration overlay | missing |
| Trace overlay | missing |
| Peak overlay | missing |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/final_screen_missing_reason.txt` |
| Graph failure package | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260526_184458/graph_failure_package_bench_01_mz71_screenshot_page_20260526_184458.json` |

Report gate: `BLOCKED`; validator: `REVIEW`; detected report graphs: `0`; failure package count: `1`; runtimeFailureClass: `TICK_LOCALIZATION_FAILURE`.

Peak metrics: not available because the run did not produce a calibrated report graph, or the report gate stopped before peak evidence became reportable.

Blocked root cause:

- Failure class: `TICK_LOCALIZATION_FAILURE`
- First failing stage: `Y_CALIBRATION`
- Graph layout: `DENSE_PEAK_SINGLE_AXIS`; physical graph count: `1`
- Tick subreasons: `INSUFFICIENT_Y_ANCHORS, OCR_NO_NUMERIC_TEXT, HIGH_RESIDUALS`
- Scale subreasons: `LABEL_SEQUENCE_NON_MONOTONIC, INSUFFICIENT_SCALE_ANCHORS, TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`
- Accepted X/Y anchors: `2` / `0`
- Assisted Review may help only if a user can confirm axes/calibration; autonomous production remains blocked.
- Next engineering fix: improve deterministic graph layout and/or Y scale calibration evidence for this fixture class.

### bench_01_mz71_screenshot_page / E2B

Verdict: **BLOCKED**. Y calibration blocked by insufficient usable anchors after OCR/tick pairing.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_01_mz71_screenshot_page.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/runtime_evidence_package_bench_01_mz71_screenshot_page_20260526_184601.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/runtime_evidence_validation_bench_01_mz71_screenshot_page_20260526_184601.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/runtime_evidence_validation_bench_01_mz71_screenshot_page_20260526_184601.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/final_report_contract_bench_01_mz71_screenshot_page_20260526_184601.json` |
| HTML report | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/report_bench_01_mz71_screenshot_page_20260526_184601.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/report_bench_01_mz71_screenshot_page_20260526_184601.md` |
| GraphPanel overlay | missing |
| PlotArea overlay | missing |
| Axis/tick/calibration overlay | missing |
| Trace overlay | missing |
| Peak overlay | missing |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/final_screen_missing_reason.txt` |
| Graph failure package | `artifacts/phase9i-final-android/bench_01_mz71_screenshot_page/model_enabled/bench_01_mz71_screenshot_page_20260526_184601/graph_failure_package_bench_01_mz71_screenshot_page_20260526_184601.json` |

Report gate: `BLOCKED`; validator: `REVIEW`; detected report graphs: `0`; failure package count: `1`; runtimeFailureClass: `TICK_LOCALIZATION_FAILURE`.

Peak metrics: not available because the run did not produce a calibrated report graph, or the report gate stopped before peak evidence became reportable.

Blocked root cause:

- Failure class: `TICK_LOCALIZATION_FAILURE`
- First failing stage: `Y_CALIBRATION`
- Graph layout: `DENSE_PEAK_SINGLE_AXIS`; physical graph count: `1`
- Tick subreasons: `INSUFFICIENT_Y_ANCHORS, OCR_NO_NUMERIC_TEXT, HIGH_RESIDUALS`
- Scale subreasons: `LABEL_SEQUENCE_NON_MONOTONIC, INSUFFICIENT_SCALE_ANCHORS, TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`
- Accepted X/Y anchors: `2` / `0`
- Assisted Review may help only if a user can confirm axes/calibration; autonomous production remains blocked.
- Next engineering fix: improve deterministic graph layout and/or Y scale calibration evidence for this fixture class.

### bench_02_mz92_belyi_tigr / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_02_mz92_belyi_tigr.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/runtime_evidence_package_run_1779810494490.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/runtime_evidence_validation_run_1779810494490.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/runtime_evidence_validation_run_1779810494490.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/final_report_contract_run_1779810494490.json` |
| HTML report | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/report_run_1779810494490.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/report_run_1779810494490.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260526_184704/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `GRAPH_PANEL_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 8.245 | 1916 | 1824 | 100 | 0.8558 | 257.7 | CALCULATED |  |

### bench_02_mz92_belyi_tigr / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_02_mz92_belyi_tigr.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/runtime_evidence_package_run_1779810584892.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/runtime_evidence_validation_run_1779810584892.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/runtime_evidence_validation_run_1779810584892.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/final_report_contract_run_1779810584892.json` |
| HTML report | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/report_run_1779810584892.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/report_run_1779810584892.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_02_mz92_belyi_tigr/model_enabled/bench_02_mz92_belyi_tigr_20260526_184822/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `GRAPH_PANEL_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 8.245 | 1916 | 1824 | 100 | 0.8558 | 257.7 | CALCULATED |  |

### bench_03_small_tic_export / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_03_small_tic_export.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/runtime_evidence_package_run_1779810604725.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/runtime_evidence_validation_run_1779810604725.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/runtime_evidence_validation_run_1779810604725.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/final_report_contract_run_1779810604725.json` |
| HTML report | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/report_run_1779810604725.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/report_run_1779810604725.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_03_small_tic_export/deterministic/bench_03_small_tic_export_20260526_184951/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `PEAK_EVIDENCE_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 5.732 | 0.332 | 0.1547 | 38.8 | 0.4943 | 104.1 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 2 | 6.362 | 0.3515 | 0.1115 | 27.96 | 0.3595 | 110.2 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning |
| 3 | 7.889 | 0.3177 | 0.1326 | 33.24 | 0.4494 | 99.63 | CALCULATED | calculation_run.peak_warning |

### bench_03_small_tic_export / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_03_small_tic_export.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/runtime_evidence_package_run_1779810632948.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/runtime_evidence_validation_run_1779810632948.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/runtime_evidence_validation_run_1779810632948.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/final_report_contract_run_1779810632948.json` |
| HTML report | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/report_run_1779810632948.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/report_run_1779810632948.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_03_small_tic_export/model_enabled/bench_03_small_tic_export_20260526_185008/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `PEAK_EVIDENCE_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 5.732 | 0.332 | 0.1547 | 38.8 | 0.4943 | 104.1 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning; calculation_run.peak_warning |
| 2 | 6.362 | 0.3515 | 0.1115 | 27.96 | 0.3595 | 110.2 | CALCULATED | calculation_run.peak_warning; calculation_run.peak_warning |
| 3 | 7.889 | 0.3177 | 0.1326 | 33.24 | 0.4494 | 99.63 | CALCULATED | calculation_run.peak_warning |

### bench_04_stacked_xic_resolution / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_04_stacked_xic_resolution.png` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/runtime_evidence_package_run_1779810722868.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/runtime_evidence_validation_run_1779810722868.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/runtime_evidence_validation_run_1779810722868.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/final_report_contract_run_1779810722868.json` |
| HTML report | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/report_run_1779810722868.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/report_run_1779810722868.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/graph_2_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/graph_2_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/graph_2_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/graph_2_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/graph_2_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260526_185040/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `VLM_SEMANTIC_LAYER_UNAVAILABLE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 1.078e+11 | 10.21 | 3.709e+10 | 100 | 3.35e+09 | 219.2 | CALCULATED |  |

### bench_04_stacked_xic_resolution / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_04_stacked_xic_resolution.png` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/runtime_evidence_package_run_1779810825697.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/runtime_evidence_validation_run_1779810825697.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/runtime_evidence_validation_run_1779810825697.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/final_report_contract_run_1779810825697.json` |
| HTML report | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/report_run_1779810825697.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/report_run_1779810825697.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/graph_2_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/graph_2_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/graph_2_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/graph_2_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/graph_2_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_04_stacked_xic_resolution/model_enabled/bench_04_stacked_xic_resolution_20260526_185209/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `UNKNOWN_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 1.078e+11 | 10.21 | 3.709e+10 | 100 | 3.35e+09 | 219.2 | CALCULATED |  |

### bench_05_tic_plus_ions / deterministic

Verdict: **BLOCKED**. TIC+ions layout not propagated and Y calibration direction is inconsistent.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_05_tic_plus_ions.png` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/runtime_evidence_package_bench_05_tic_plus_ions_20260526_185353.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/runtime_evidence_validation_bench_05_tic_plus_ions_20260526_185353.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/runtime_evidence_validation_bench_05_tic_plus_ions_20260526_185353.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/final_report_contract_bench_05_tic_plus_ions_20260526_185353.json` |
| HTML report | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/report_bench_05_tic_plus_ions_20260526_185353.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/report_bench_05_tic_plus_ions_20260526_185353.md` |
| GraphPanel overlay | missing |
| PlotArea overlay | missing |
| Axis/tick/calibration overlay | missing |
| Trace overlay | missing |
| Peak overlay | missing |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/final_screen_missing_reason.txt` |
| Graph failure package | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260526_185353/graph_failure_package_bench_05_tic_plus_ions_20260526_185353.json` |

Report gate: `BLOCKED`; validator: `REVIEW`; detected report graphs: `0`; failure package count: `1`; runtimeFailureClass: `CALIBRATION_FAILURE`.

Peak metrics: not available because the run did not produce a calibrated report graph, or the report gate stopped before peak evidence became reportable.

Blocked root cause:

- Failure class: `CALIBRATION_FAILURE`
- First failing stage: `Y_CALIBRATION`
- Graph layout: `DENSE_PEAK_SINGLE_AXIS`; physical graph count: `1`
- Tick subreasons: `NON_MONOTONIC_TICK_VALUES, OCR_NO_NUMERIC_TEXT, HIGH_RESIDUALS`
- Scale subreasons: `LABEL_SEQUENCE_NON_MONOTONIC, SCALE_FIT_HIGH_RESIDUAL, TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`
- Accepted X/Y anchors: `9` / `2`
- Assisted Review may help only if a user can confirm axes/calibration; autonomous production remains blocked.
- Next engineering fix: improve deterministic graph layout and/or Y scale calibration evidence for this fixture class.

### bench_05_tic_plus_ions / E2B

Verdict: **BLOCKED**. TIC+ions layout not propagated and Y calibration direction is inconsistent.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_05_tic_plus_ions.png` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/runtime_evidence_package_bench_05_tic_plus_ions_20260526_185451.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/runtime_evidence_validation_bench_05_tic_plus_ions_20260526_185451.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/runtime_evidence_validation_bench_05_tic_plus_ions_20260526_185451.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/final_report_contract_bench_05_tic_plus_ions_20260526_185451.json` |
| HTML report | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/report_bench_05_tic_plus_ions_20260526_185451.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/report_bench_05_tic_plus_ions_20260526_185451.md` |
| GraphPanel overlay | missing |
| PlotArea overlay | missing |
| Axis/tick/calibration overlay | missing |
| Trace overlay | missing |
| Peak overlay | missing |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/final_screen_missing_reason.txt` |
| Graph failure package | `artifacts/phase9i-final-android/bench_05_tic_plus_ions/model_enabled/bench_05_tic_plus_ions_20260526_185451/graph_failure_package_bench_05_tic_plus_ions_20260526_185451.json` |

Report gate: `BLOCKED`; validator: `REVIEW`; detected report graphs: `0`; failure package count: `1`; runtimeFailureClass: `CALIBRATION_FAILURE`.

Peak metrics: not available because the run did not produce a calibrated report graph, or the report gate stopped before peak evidence became reportable.

Blocked root cause:

- Failure class: `CALIBRATION_FAILURE`
- First failing stage: `Y_CALIBRATION`
- Graph layout: `DENSE_PEAK_SINGLE_AXIS`; physical graph count: `1`
- Tick subreasons: `NON_MONOTONIC_TICK_VALUES, OCR_NO_NUMERIC_TEXT, HIGH_RESIDUALS`
- Scale subreasons: `LABEL_SEQUENCE_NON_MONOTONIC, SCALE_FIT_HIGH_RESIDUAL, TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`
- Accepted X/Y anchors: `9` / `2`
- Assisted Review may help only if a user can confirm axes/calibration; autonomous production remains blocked.
- Next engineering fix: improve deterministic graph layout and/or Y scale calibration evidence for this fixture class.

### bench_06_photo_two_graphs_page / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_06_photo_two_graphs_page.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/runtime_evidence_package_run_1779811067085.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/runtime_evidence_validation_run_1779811067085.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/runtime_evidence_validation_run_1779811067085.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/final_report_contract_run_1779811067085.json` |
| HTML report | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/report_run_1779811067085.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/report_run_1779811067085.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260526_185549/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `VLM_SEMANTIC_LAYER_UNAVAILABLE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 58.81 | 1.438e+05 | 1.983e+05 | 100 | 1.601 | 55.7 | CALCULATED | calculation_run.peak_warning |

### bench_06_photo_two_graphs_page / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_06_photo_two_graphs_page.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/runtime_evidence_package_run_1779811202522.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/runtime_evidence_validation_run_1779811202522.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/runtime_evidence_validation_run_1779811202522.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/final_report_contract_run_1779811202522.json` |
| HTML report | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/report_run_1779811202522.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/report_run_1779811202522.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_06_photo_two_graphs_page/model_enabled/bench_06_photo_two_graphs_page_20260526_185754/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `UNKNOWN_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 58.81 | 1.438e+05 | 1.983e+05 | 100 | 1.601 | 55.7 | CALCULATED | calculation_run.peak_warning |

### bench_07_rotated_page_photo / deterministic

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_07_rotated_page_photo.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/runtime_evidence_package_run_1779811255328.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/runtime_evidence_validation_run_1779811255328.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/runtime_evidence_validation_run_1779811255328.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/final_report_contract_run_1779811255328.json` |
| HTML report | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/report_run_1779811255328.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/report_run_1779811255328.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_07_rotated_page_photo/deterministic/bench_07_rotated_page_photo_20260526_190009/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `REVIEW`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `VLM_SEMANTIC_LAYER_UNAVAILABLE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 5.619 | 2.258e+05 | 1.32e+05 | 100 | 0.5445 | 213.8 | CALCULATED |  |

### bench_07_rotated_page_photo / E2B

Verdict: **REVIEW**. Produces report/peaks but evidence gate remains review-grade; not release-ready.

| Evidence | Path/status |
| --- | --- |
| Input image | `composeApp/src/androidMain/assets/validation/bench_07_rotated_page_photo.jpg` |
| RuntimeEvidencePackage | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/runtime_evidence_package_run_1779811320242.json` |
| Validator JSON | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/runtime_evidence_validation_run_1779811320242.json` |
| Validator Markdown | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/runtime_evidence_validation_run_1779811320242.md` |
| Final report JSON | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/final_report_contract_run_1779811320242.json` |
| HTML report | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/report_run_1779811320242.html` |
| Markdown report | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/report_run_1779811320242.md` |
| GraphPanel overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/graph_1_graph_panel_overlay.png` |
| PlotArea overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/graph_1_plot_area_overlay.png` |
| Axis/tick/calibration overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/graph_1_axis_tick_overlay.png` |
| Trace overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/graph_1_trace_overlay.png` |
| Peak overlay | `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/graph_1_peak_overlay.png` |
| Final screen | missing; missing reason: `artifacts/phase9i-final-android/bench_07_rotated_page_photo/model_enabled/bench_07_rotated_page_photo_20260526_190102/final_screen_missing_reason.txt` |
| Graph failure package | missing |

Report gate: `REVIEW_ONLY`; validator: `PASS`; detected report graphs: `1`; failure package count: `0`; runtimeFailureClass: `UNKNOWN_FAILURE`.

First 10 peaks:

| # | RT | Height | Area | Area % | FWHM | S/N | Evidence | Flags |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 5.619 | 2.258e+05 | 1.32e+05 | 100 | 0.5445 | 213.8 | CALCULATED |  |

## Bottom Line

The app currently produces review-grade reports for six fixture classes and remains blocked on two supported fixture classes. No fixture is `RELEASE_READY`. Phase 9 remains blocked; Phase 10 must not start.
