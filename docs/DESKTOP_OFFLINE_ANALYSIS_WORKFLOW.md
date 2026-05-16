# Desktop Offline Analysis Workflow

Status: active desktop-first verification path.

ChromaLab now has a desktop CLI entry for reproducible chromatogram-image analysis
without Android, camera, ADB, or phone logs. This is the primary workflow for
debugging graph preparation, crop/perspective logic, axis calibration, curve
extraction, peak calculation, and report contract issues before they are promoted to
Android.

## Command

Run from the repository root:

```powershell
$img = (Resolve-Path "composeApp\src\desktopTest\resources\fixtures\chromatogram_bench\bench_07_rotated_page_photo.jpg").Path
$out = (Resolve-Path ".").Path + "\build\desktop-offline-analysis\bench_07_rotated_page_photo"
.\gradlew.bat :composeApp:run --args="--offline-analysis --image `"$img`" --out `"$out`" --source bench_07_rotated_page_photo --expected-graphs 1"
```

Use absolute image and output paths when invoking through Gradle. The desktop run
task starts inside the `composeApp` project directory, so repository-relative paths
can be interpreted differently.

## Optional Local VLM Axis OCR

Desktop axis OCR is disabled unless a local OpenAI-compatible vision endpoint is
explicitly configured. This keeps the offline runner deterministic and prevents fake
axis calibration when no model is available.

Example with LM Studio local server:

```powershell
$env:CHROMALAB_DESKTOP_VLM_BASE_URL = "http://127.0.0.1:1234/v1"
$env:CHROMALAB_DESKTOP_VLM_MODEL = "qwen3-vl-2b-instruct"
$env:CHROMALAB_DESKTOP_VLM_MIN_CONFIDENCE = "0.65"
```

The model is asked to read only the isolated X-axis, Y-axis, and title/ION bands.
It must return numeric tick values plus normalized tick positions. A result is marked
`AUTO_ACCEPTED` only when both axes have at least two usable tick anchors and the
confidence gate passes. Otherwise the run remains blocked at axis calibration.

## Outputs

Each run writes:

- `audit.json` - structured machine-readable stage audit.
- `audit_summary.md` - human-readable stage table and warnings.
- `calibrated_report_ui_contract.json` - future report UI contract state.
- `calibrated_report.md` - current report draft or blocked report skeleton.
- `graph_candidates.png` - accepted/rejected graph candidates, final graph panel,
  and plot-area overlay.
- `axis_calibration_diagnostics_graph_N.png` - graph-panel context with X-label,
  Y-label, title/ION, plot-area, and graph-panel zones overlaid.
- `axis_x_label_band_graph_N.png` - crop containing the X-axis tick labels and
  time scale area.
- `axis_y_label_band_graph_N.png` - crop containing the Y-axis tick labels and
  abundance/intensity scale area.
- `axis_title_band_graph_N.png` - crop containing the chromatogram title/ION line.
- `graph_focus_graph_N.png` - selected graph panel crop with plot area and axes
  overlaid.
- `selected_preprocessing_graph_N.png` - selected preprocessing variant crop.
- `peak_overlay_graph_N.png` - peak overlay when the run reaches peak metrics.

The output directory also contains stage folders such as `normalize/`,
`orientation/`, `preprocess/`, and `graph_N/` with intermediate images and masks
created by the shared offline runner.

## Current Diagnostic Result

Initial validation on 2026-05-16:

- `bench_02_mz92_belyi_tigr`: detected 1 graph, but blocks at `crop_quality`.
  The graph panel is found, yet crop-boundary risk flags possible top-edge peak
  clipping and desktop OCR is unavailable.
- `bench_07_rotated_page_photo`: rotates correctly and detects 1 graph, then blocks
  at `axis_calibration` because desktop OCR currently returns `NOT_AVAILABLE`.
- `bench_06_photo_two_graphs_page`: detects the expected 2 graph panels and writes
  multi-graph overlays, then blocks at `axis_calibration` for the same desktop OCR
  gap.

Phase 8.3c.3 connects those isolated bands to an optional local VLM reader. If no
local endpoint is configured, desktop OCR still returns `NOT_AVAILABLE`; this is an
expected honest blocker, not a fallback calculation mode.

These are expected blockers for the next desktop-first slices. They prove the
computer workflow can reproduce the exact failure chain without phone testing.

## Product Rule

The desktop workflow is not allowed to hide failures by injecting manual axis
calibration or fake deterministic values. If automatic OCR/axis calibration is not
available, the run must stop with a blocked stage and preserve artifacts for the next
fix.
