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

## Full Bench Suite

Run all eight desktop bench fixtures from the repository root:

```powershell
.\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1
```

Optional output root:

```powershell
.\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1 -OutputRoot "build\phase-pc-algorithm-audit"
```

The suite writes one desktop analysis folder per fixture plus
`pc_chromatogram_bench_summary.csv`. This is the preferred PC-first regression
entrypoint before Android reruns or algorithm repair.

## Optional Local VLM Axis OCR

Desktop axis OCR is disabled unless a local OpenAI-compatible vision endpoint is
explicitly configured. This keeps the offline runner deterministic and prevents fake
axis calibration when no model is available.

Example with LM Studio local server:

```powershell
$env:CHROMALAB_DESKTOP_VLM_BASE_URL = "http://127.0.0.1:1234/v1"
$env:CHROMALAB_DESKTOP_VLM_MODEL = "qwen3-vl-2b-instruct"
$env:CHROMALAB_DESKTOP_VLM_MIN_CONFIDENCE = "0.65"
$env:CHROMALAB_DESKTOP_VLM_TIMEOUT_MS = "300000"
```

When `CHROMALAB_DESKTOP_VLM_MODEL` is set, the runner uses that model directly for
the image request. The `/models` endpoint is used only for automatic model discovery
when no explicit model id is configured, so a slow discovery response cannot block a
known-good local model.

If LM Studio Server Settings has authentication enabled, create an API token in
Developer > Server Settings > Manage Tokens and pass it to the desktop runner:

```powershell
$env:CHROMALAB_DESKTOP_VLM_API_TOKEN = "<LM Studio API token>"
```

For deterministic parser/gate debugging without a running model, use a recorded
response file:

```powershell
$env:CHROMALAB_DESKTOP_VLM_RESPONSE_FILE = "C:\VietnAi\Hromotograth\docs\reference\chromatogram_bench\axis_vlm_replay_bench_07.json"
```

Replay mode goes through the same JSON parser, tick-position mapping, acceptance
gate, axis calibration, signal conversion, peak detection, and report validation
path as a live model response. It adds the warning
`desktop_axis_vlm.replay_response_file` so diagnostic replay output is not mistaken
for a live model result.

The model is asked to read isolated X-axis and Y-axis bands through separate live
requests. It must return numeric tick values plus normalized tick positions. A result
is marked `AUTO_ACCEPTED` only when both axes have at least two usable tick anchors
and the confidence gate passes. Otherwise the run remains blocked at axis calibration.

Desktop VLM/OCR warnings are propagated into the stage audit and report warning
sections. Important warning codes include:

- `desktop_axis_vlm.endpoint_not_configured` - no local OpenAI-compatible endpoint
  is configured.
- `desktop_axis_vlm.replay_response_file` - a recorded response file was used
  instead of a live model call.
- `desktop_axis_vlm.replay_file_read_failed` - the configured replay response file
  could not be read.
- `desktop_axis_vlm.auth_required` - LM Studio rejected the `/models` preflight
  with `401` or `403`; provide `CHROMALAB_DESKTOP_VLM_API_TOKEN` or disable auth.
- `desktop_axis_vlm.models_http_status_N` - `/models` preflight replied with a
  non-success status.
- `desktop_axis_vlm.models_request_timeout` - `/models` preflight did not answer
  within the short diagnostic timeout.
- `desktop_axis_vlm.model_auto_selected` - no model id was configured, so the first
  model from `/models` was used.
- `desktop_axis_vlm.model_not_discovered` - no usable model id was available.
- `desktop_axis_vlm.http_status_N` - endpoint replied with a non-success status.
- `desktop_axis_vlm.request_timeout` - the configured model did not finish the VLM
  image request within `CHROMALAB_DESKTOP_VLM_TIMEOUT_MS`.
- `desktop_axis_vlm.x_request_timeout` and `desktop_axis_vlm.y_request_timeout` -
  an isolated live axis-band request timed out. This usually means the crop is still
  too broad/noisy for the local VLM and should be narrowed before accepting OCR.
- `desktop_axis_vlm.response_content_missing` - endpoint response had no message
  content.
- `desktop_axis_vlm.x_response_json_unparseable` and
  `desktop_axis_vlm.y_response_json_unparseable` - an isolated axis response did not
  contain the required JSON object.
- `desktop_axis_vlm.response_json_unparseable` - model output did not contain the
  required JSON object.
- `desktop_axis_vlm.x_requires_two_ticks` and
  `desktop_axis_vlm.y_requires_two_ticks` - fewer than two tick anchors were found
  for calibration.
- `desktop_axis_vlm.confidence_below_threshold` - tick anchors were present, but
  confidence did not pass the configured gate.

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

Phase 8.3c.5a adds replay mode for recorded VLM JSON responses. On
`bench_07_rotated_page_photo`, the included replay file drives the full desktop path
through `AUTO_ACCEPTED` OCR, confirmed axis calibration, signal conversion, peak
detection, and report validation. Without either replay file or local endpoint,
desktop OCR still returns `NOT_AVAILABLE`; this remains an expected honest blocker,
not a fallback calculation mode, and is visible as
`desktop_axis_vlm.endpoint_not_configured`.

These are expected blockers for the next desktop-first slices. They prove the
computer workflow can reproduce the exact failure chain without phone testing.

## Product Rule

The desktop workflow is not allowed to hide failures by injecting manual axis
calibration or fake deterministic values. If automatic OCR/axis calibration is not
available, the run must stop with a blocked stage and preserve artifacts for the next
fix.
