# Phase 8D Android Fixture Result

Final rerun: `white_tiger_ion71_20260520_184550`

Package: `com.chromalab.app.validation`

Command:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

Artifact locations:

- Device: `/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_184550/`
- Local pulled copy: `artifacts/phase8d-android-validation/white_tiger_ion71_20260520_184550/white_tiger_ion71_20260520_184550/`

## Result

| Field | Value |
| --- | --- |
| Runtime package | `runtime_evidence_package_run_1779291982128.json` |
| Terminal state | `REVIEW` |
| Report gate | `REVIEW_ONLY` |
| Validator verdict | `REVIEW` |
| Blocking validator issues | 0 |
| Runtime failure class | `VLM_SEMANTIC_LAYER_UNAVAILABLE` |
| Detected graph count | 1 |
| Runtime graph packages | 1 |
| Graph failure packages | 0, because processing reached report export |
| Calibration statuses | `VALID`, `VALID` |
| Peak evidence rows | 2 |
| Stage judge rows | 5 |
| Executed runtime | `DETERMINISTIC` |

## Artifacts Collected

- `runtime_evidence_package_run_1779291982128.json`
- `runtime_evidence_validation_run_1779291982128.json`
- `runtime_evidence_validation_run_1779291982128.md`
- `final_report_contract_run_1779291982128.json`
- `report_run_1779291982128.html`
- `report_run_1779291982128.md`
- `stage_timings_run_1779291982128.json`
- `artifact_manifest_run_1779291982128.json`
- `graph_1_graph_panel_overlay.png`
- `graph_1_plot_area_overlay.png`
- `graph_1_axis_tick_overlay.png`
- `graph_1_trace_overlay.png`
- `graph_1_peak_overlay.png`
- `final_screen.png`
- `logcat_excerpt.txt`

## Interpretation

Phase 8D closed the historical tick/calibration blocker for this validation fixture. The Android run now reaches graphPanel, plotArea, axis/tick evidence, X/Y calibration, trace extraction, peak evidence, report export, and runtime evidence validation.

The result is still `REVIEW_ONLY` because the VLM semantic layer is disabled/unavailable for this validation build. The report does not claim `RELEASE_READY`, and no VLM or knowledge output is used as numeric measurement authority.

## Remaining Limitation

The validator emits one non-blocking warning:

- `package.model_metadata_missing`: no selected/executed model id is present, but model availability diagnostics explicitly record `VALIDATION_FIXTURE` mode with `DISABLED` status.

This is acceptable for Phase 8D because deterministic graph/tick/calibration evidence is now complete and the unavailable VLM semantic layer is classified.
