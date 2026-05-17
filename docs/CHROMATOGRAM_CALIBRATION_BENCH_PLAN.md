# Chromatogram Calibration Bench Plan

Status: active planning contract.

## Current Tracking State

This is the active phase document for the current chromatography stabilization work.
It belongs to `ROADMAP.md` -> `Next MVP Phase` -> `1. Stabilize Chromatography Core`.
The work has not moved away from the main product plan; the current implementation
track is the desktop/emulator-first calibration bench that must make photo analysis
auditable before Android/device parity and model-assisted stages are treated as stable.

Current execution point:

- Main roadmap item: `Stabilize Chromatography Core`.
- Active plan: this document.
- Active phase: `Phase 8 - Model-Assisted Stages`, using the calibrated
  desktop/emulator bench artifacts, structured report contract, and local knowledge
  pack from earlier phases.
- Latest completed work slice: `Phase 8.3c.5c.15 - skeleton graph trunk-path centerline candidate`.
- Next work slice: `Phase 8.3c.5c.16 - fragmentation-aware trace reconstruction and acceptance review`.

From this point forward, every completed bench phase/subphase must be recorded in
this document before or together with its implementation commit. The shorter fixture
README under `docs/reference/chromatogram_bench/` is only a fixture manifest and
artifact summary; it is not the primary plan.

## Execution Ledger

| Slice | Status | Commit | Scope |
| --- | --- | --- | --- |
| Phase 0.1 | Done | `2e2d48f` | Import eight neutral chromatogram fixtures and identity test. |
| Phase 1.1 | Done | `dcfcee3` | Add offline bench runner and honest blocked stage audit. |
| Phase 1.2 | Done | `e7b5316` | Persist JSON/Markdown/PNG audit artifacts. |
| Phase 1.3 | Done | `5159098` | Add real desktop graph-region detection. |
| Phase 1.4 | Done | `d01d1b0` | Tighten multi-panel graph splitting. |
| Phase 2.1 | Done | `ed76935` | Add preprocessing variant audit. |
| Phase 2.2 | Done | `d235520` | Align deterministic preprocessing across desktop and Android. |
| Phase 2.3 | Done | `410e145` | Add crop-quality gate. |
| Phase 2.4 | Done | `484cab8` | Refine broad graph crops. |
| Phase 2.5 | Done | `41bf0a3` | Lock crop quality contracts. |
| Phase 2.6 | Done | `97a9a04` | Add crop-boundary safety diagnostics. |
| Phase 2.7.1 | Done | `7cec0a6` | Correct right-angle page orientation. |
| Phase 2.7.2 | Done | `7b509e2` | Preserve rotated graph-panel bounds. |
| Phase 2.8.0 | Done | `d80e06c` | Document graph boundary contract. |
| Phase 2.8.1 | Done | `406fd60` | Preserve broad photographed graph panels. |
| Phase 2.8.2 | Done | `5d44df7` | Tighten photographed graph-panel bounds. |
| Phase 2.9 | Done | `820530d` | Add audited plot-area detection. |
| Phase 2.10.1 | Done | `183ffd7` | Add desktop curve-mask audit. |
| Phase 2.10.2 | Done | `250a52e` | Add desktop curve-point extraction. |
| Phase 2.11.1 | Done | `9e8598a` | Add desktop axis geometry detection. |
| Phase 2.11.2 | Done | `60eb1fd` | Add axis-calibration readiness gate. |
| Phase 2.11.3 | Done | `76fe69b` | Add confirmed manual calibration contract. |
| Phase 2.11.4 | Done | `6f92d20` | Remove identity/`px` calibration fallback from user flow. |
| Phase 2.11.5 | Done | `d4dc42a` | Add manual X/Y calibration gate to user flow. |
| Phase 2.11.6.1 | Done | `92dcc53` | Use OCR tick anchors for manual calibration prefill and overlay hints. |
| Phase 2.11.6.2 | Done | `4c98d4d` | Add focused graph-panel view for manual calibration review. |
| Phase 2.11.6.3 | Done | `6925b75` | Add editable tick-anchor UX for manual calibration. |
| Phase 2.12.1 | Done | `52b9183` | Add manual calibration focus artifacts for bench fixture review. |
| Phase 2.12.2 | Done | `f62f92b` | Review generated focus artifacts and lock visual acceptance thresholds before numeric integration work. |
| Phase 5.1 | Done | `8a37951` | Start calibrated curve-to-signal conversion from confirmed axis calibration before peak integration. |
| Phase 5.2 | Done | `547573c` | Add audited peak detection readiness gate on calibrated signal data and verify it on clean, two-graph, and rotated fixtures. |
| Phase 5.3 | Done | `d9db9d2` | Review peak metrics, boundaries, and integration quality on calibrated real-fixture signals before report rendering. |
| Phase 5.4 | Done | `a4113e8` | Add per-peak audit rows and visual peak overlay artifacts for clean, two-graph, and rotated fixtures. |
| Phase 5.5 | Done | `256960f` | Add fixture-specific dominant/missed/false peak sanity checks before report rendering. |
| Phase 5.6 | Done | `4a630d9` | Recover labeled apexes on compact annotated TIC exports without regressing photographed multi-graph pages. |
| Phase 5.7 | Done | `3a25850` | Suppress right-frame false peaks on large photographed plots without breaking weak channels. |
| Phase 5.8a | Done | `e7b0bb0` | Add candidate/rejection diagnostics to explain photographed trace under-detection without changing accepted results. |
| Phase 5.8b.1 | Done | `5eabe69` | Research plot digitizer, morphology, line-detection, and chromatography peak-picking references; decide artifact-first path. |
| Phase 5.8b.2 | Done | `32aca3f` | Add internal trace-artifact diagnostics and PNG masks before any noise-threshold or completeness tuning. |
| Phase 5.8b.3 | Done | `5e13ac3` | Use trace-artifact diagnostics to guard cleanup/tuning for missed peaks without accepting contaminated graph-2 artifacts. |
| Phase 5.8b.4 | Done | `4f0acd4` | Review artifact-suppressed hypothesis and apply controlled completeness tuning only where the guard allows it. |
| Phase 5.8b.5 | Done | `77b88d7` | Review tuned peak quality and false-positive controls before broadening fixture scope. |
| Phase 5.8b.6 | Done | `e26cffc` | Broaden guarded completeness review to additional hard fixtures only after quality gates pass. |
| Phase 5.8b.7 | Done | `db56d9b` | Restore curve/signal readiness for weak stacked ion panels before report validation. |
| Phase 5.8b.8 | Done | `4053c22` | Review sparse stacked ion peak quality before report validation. |
| Phase 6.1 | Done | `c54fbb3` | Validate the structured report contract against calibrated fixture audits. |
| Phase 6.2 | Done | `e28aecf` | Add missing peak-table report columns to the calibrated audit contract. |
| Phase 6.3 | Done | `e642368` | Render calibrated audit report sections from the structured contract. |
| Phase 6.4 | Done | `178b9c9` | Connect rendered report artifacts to visual graph evidence and export/UI planning. |
| Phase 6.5 | Done | `0a9582f` | Prepare calibrated report contract for mobile/export UI wiring. |
| Phase 6.6 | Done | `1b7020f` | Wire calibrated report contract into the export/report UI surface. |
| Phase 7.1 | Done | `6bd442f` | Define local knowledge pack schema. |
| Phase 7.2 | Done | `4707498` | Add m/z 92 alkylbenzene knowledge data. |
| Phase 7.3 | Done | `30df15d` | Add n-paraffin Kovats reference support. |
| Phase 7.4 | Done | `2fd439f` | Expand common chromatogram/ion coverage and wire local knowledge into report interpretation. |
| Phase 8.1 | Done | `d9b32dc` | Define strict model-assisted stage contract and model eligibility rules. |
| Phase 8.2 | Done | `839db7b` | Route model-stage outcomes into processing metadata and final report audit. |
| Phase 8.3a | Done | `2412a84` | Validate saved-report model audit propagation through options builder and final report mapper. |
| Phase 8.3b.1 | Done | `145fbf5` | Add logcat-visible report-audit marker for Android device runs. |
| Phase 8.3b.2a | Done | `a3c1750` | Block normal Android analysis from opening manual X/Y calibration screens; failed auto-axis calibration now blocks report saving. |
| Phase 8.3c.1 | Done | `49f31b4` | Add desktop offline analysis CLI and artifact writer for reproducible local image-to-audit runs. |
| Phase 8.3c.2 | Done | `a3ad0fc` | Add per-graph desktop axis-label/title diagnostic crops before OCR/model scale reading. |
| Phase 8.3c.3 | Done | `7c59bea` | Connect desktop axis OCR to isolated axis-label bands through an optional local OpenAI-compatible VLM endpoint. |
| Phase 8.3c.4 | Done | `54ee00e` | Add audit-visible desktop VLM axis-OCR failure and acceptance warnings before live model tuning. |
| Phase 8.3c.5a | Done | `499e3c4` | Add a desktop VLM response replay harness and fixture response to validate axis parser/gates without a live endpoint. |
| Phase 8.3c.5b.1 | Done | `2501d75` | Add LM Studio API-token support and document the live endpoint authentication setup. |
| Phase 8.3c.5b.2 | Done | `9f4d9d9` | Add fast `/models` preflight diagnostics and optional model auto-selection before sending image requests. |
| Phase 8.3c.5b.3 | Done | `edb2319` | Bypass blocking `/models` discovery when a model id is explicit, expose live VLM request timeouts, and verify that the current three-band Qwen3-VL request times out while smaller one-image requests return. |
| Phase 8.3c.5c.1 | Done | `ad4ca28` | Split desktop VLM axis OCR into separate X/Y requests with axis-specific warning codes, use the orientation-corrected source image for OCR instead of binary preprocessing, compact the axis JSON contract, and verify live `bench_07` still blocks honestly on X/Y request timeouts. |
| Phase 8.3c.5c.2 | Done | `525a12f` | Review current code and current CV/plot-digitizer/OCR options, document that VLM must not own pixel positions, and set the next slice to a deterministic CV geometry adapter spike. |
| Phase 8.3c.5c.3a | Done | `5d04bf7` | Audit the real image-selection-to-report path, compare Android runtime with desktop/offline bench order, and document critical geometry, calibration, persistence, and report-gating gaps before the CV adapter spike. |
| Phase 8.3c.5c.3b | Done | `354af27` | Research 2026 CV, OCR, plot-digitizer, chromatogram calculation, report, and device-runtime methods; document what is inaccurate, slow, outdated, or missing in ChromaLab before the CV adapter spike. |
| Phase 8.3c.5c.3c | Done | `f007b8f` | Add desktop CV geometry spike artifacts (`cv_geometry_audit.json`, `cv_geometry_overlay.png`) with line segment, connected-component, contour, and frame candidates for fixture review before production OpenCV/BoofCV integration. |
| Phase 8.3c.5c.3d | Done | `38ce74c` | Review the generated CV geometry spike artifacts on `bench_03`, `bench_06`, and `bench_07`; accept the spike as diagnostic evidence only; define the production OpenCV/BoofCV scope around perspective, graph-panel, plot-frame, axis, tick, OCR-value, and residual metrics. |
| Phase 8.3c.5c.4 | Done | `ee67e52` | Add an audit-visible `perspectiveGeometry` contract, replace desktop perspective identity-copy with measured homography warp output, make desktop document fallback expose real image dimensions, and verify the contract on clean, two-graph, and rotated fixtures. |
| Phase 8.3c.5c.5 | Done | `990cd74` | Extend `perspectiveGeometry` with document, graph-panel, and plot-area quadrilateral candidates plus aggregate residual metrics; verify candidate counts and accepted plot candidates on the full fixture gate and persistent clean/two-graph/rotated artifacts. |
| Phase 8.3c.5c.6 | Done | `c36b6c2` | Add an OpenCV-backed desktop benchmark candidate source for document contours and plot-frame Hough evidence behind the existing `perspectiveGeometry` contract; keep Android as an explicit no-op backend until native parity is evaluated; verify OpenCV candidates on the full fixture gate and persistent clean/two-graph/rotated artifacts. |
| Phase 8.3c.5c.7 | Done | `38eca4c` | Add audit-visible deterministic axis/tick geometry: desktop OpenCV Hough line evidence, projection-derived tick positions, axis/origin candidates, and explicit OCR-matching readiness; keep Android as an explicit no-op backend until native parity is evaluated; verify on the full fixture gate and persistent clean/two-graph/rotated artifacts. |
| Phase 8.3c.5c.8 | Done | `59cca15` | Replace OCR-owned pixel anchors with values-only OCR matched to deterministic CV tick positions; add finite X/Y calibration residual metrics and report gates; verify on the full fixture gate plus persistent clean/two-graph/rotated artifacts. |
| Phase 8.3c.5c.9 | Done | `d153e8b` | Add audited skeleton/centerline trace candidates with Zhang-Suen thinning, centerline coverage, skeleton support, branch/wide-cluster diagnostics, and overlay evidence on desktop and Android while preserving the existing calculation signal until visual parity review. |
| Phase 8.3c.5c.10 | Done | `8036cd7` | Compare skeleton centerline candidates against the preserved calculation signal; record selection decision, matched-column ratio, median/P95/max pixel deltas, and warnings in JSON/Markdown audits; keep `selectedForSignal=false` because real fixture parity still needs visual acceptance. |
| Phase 8.3c.5c.11 | Done | `7e23f7e` | Add per-graph centerline parity overlay artifacts on desktop and Android, expose large-delta threshold/count/ratio in the audit and report evidence contract, and verify overlays on clean, stacked, TIC-plus-ion, photographed two-graph, and rotated fixtures without switching calculation input. |
| Phase 8.3c.5c.12 | Done | `3221c9e` | Classify centerline parity failures by branch-near columns and signed peak-edge/top-edge direction, color-code those classes in parity overlays, expose counts/ratios in JSON/Markdown audits, and verify that calculation input still remains unchanged. |
| Phase 8.3c.5c.13 | Done | `e8bff9f` | Add an audit-only branch-pruned centerline hypothesis, generate per-graph branch-pruned overlays, expose pruned parity/improvement metrics in JSON/Markdown/report evidence, and verify on clean, stacked, TIC-plus-ion, photographed two-graph, and rotated fixtures without switching the calculation signal. |
| Phase 8.3c.5c.14 | Done | `6b709b9` | Add continuity-interpolated branch-pruned candidates behind metric-safe selection, expose interpolated-column counts in audits/reports, verify real fixture overlays, and keep the candidate audit-only because hard photographed/rotated graphs still fail overlap acceptance. |
| Phase 8.3c.5c.15 | Done | `this commit` | Add an audit-only skeleton graph trunk-path candidate with node/edge/endpoint/junction/fragmentation metrics, write per-graph trunk-path overlays, expose parity metrics in JSON/Markdown/report evidence, and verify that fragmented photographed/rotated traces still cannot safely drive calculation signal. |

This document defines the desktop/emulator-first calibration plan for ChromaLab's
chromatogram image analysis, graph splitting, deterministic calculation, and final
report flow. It is separate from camera/device optimization. Camera, Android scanner,
LiteRT/GGUF runtime performance, and weak-device behavior are validated only after the
core analysis path is stable on reproducible fixtures.

## Product Intent

ChromaLab must accept real-world chromatogram images without requiring the user to
manually satisfy the pipeline. The user may roughly photograph, import, or provide a
file. The app must handle preparation, graph detection, multi-graph splitting, axis
reading, curve extraction, peak calculation, and report generation.

The target pipeline is:

```text
image/file
  -> image normalization
  -> graph detection and splitting
  -> per-graph crop/deskew/filter selection
  -> title/ION/axis/tick extraction
  -> curve extraction
  -> deterministic calculation
  -> structured report
```

For a page with several plots, the output must preserve this order:

```text
graph 1 -> report 1
graph 2 -> report 2
graph 3 -> report 3
graph 4 -> report 4
```

## Graph Boundary Contract

ChromaLab must treat graph localization as two separate rectangles:

- `graph panel bounds` - the full visible graph block: title/ION text, Y-axis label
  and tick values, X-axis/time values, axis lines, signal, and visible plot frame.
- `plot area bounds` - the inner numeric plotting rectangle used for pixel-to-unit
  calibration, curve extraction, peak detection, and integration.

The first detector target is always `graph panel bounds`. The app must not crop directly
to the inner signal/plot area and lose tick labels, time values, abundance labels, or
early graph context. Once the full graph panel is stable, a later stage derives and
audits the inner `plot area bounds` from that panel. This keeps OCR/calibration and
deterministic calculation from using different, implicit crops.

## Non-Negotiable Rules

- Do not weaken analysis depth to make a hard fixture pass faster.
- Do not present deterministic-only approximations as completed neural vision analysis.
- Do not let an LLM invent numeric values, peak tables, axis ranges, or compound IDs.
- Use model output only as structured assistance for recognition/interpretation when
  the stage contract supports it.
- Missing values stay explicit: `not detected`, `not calculated`,
  `insufficient confidence`, or `failed`.
- Every phase must preserve stage audit data so failures can be localized.
- The final report target remains the professional visual contract in `REPORT_SPEC.md`,
  not terminal-style text or raw Markdown as the main mobile UI.

## Initial Reference Image Set

The first calibration set contains eight user-provided chromatogram images. They must
be imported into neutral repo fixture paths before executable tests are written. Do not
store user-specific absolute source paths in committed fixture metadata.

| ID | Source filename | Size | SHA-256 | Fixture role |
| --- | --- | ---: | --- | --- |
| `bench_01_mz71_screenshot_page` | `photo_2026-05-06_20-14-23.jpg` | 964x1280, 128010 bytes | `8EB64774D29F93C1CCB3D4F9035C96F9075121551600B2814BA7966021ECFD01` | Printed page photo with two Ion 217/218 chromatograms and surrounding page metadata; tests crop, graph splitting, and page/background rejection. |
| `bench_02_mz92_belyi_tigr` | `photo_2026-05-10_06-16-01.jpg` | 576x1280, 57090 bytes | `D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE` | Existing Belyi Tigr `m/z 92` reference screenshot; tests report-depth target and dominant-peak tension. |
| `bench_03_small_tic_export` | `images.jpg` | 381x132, 6682 bytes | `C36A405C937741C6DE834AD9FD3196E658CE42ECBA03AFBBF1D2B47D00437DF4` | Small clean TIC export; tests low-resolution axis/tick reading and labeled peaks. |
| `bench_04_stacked_xic_resolution` | `632.png` | 534x1110, 165615 bytes | `27D998C2ACA33B12DC3700BFCC88FC3EFB15FAF2A3FE51317AFD220F0E2C3C25` | Four stacked XIC graphs with different mass windows; tests multi-graph splitting and shared X-axis handling. |
| `bench_05_tic_plus_ions` | `624.png` | 683x807, 162230 bytes | `3F715862316D9D24394F18CC21DFA4C42D6BAC694DD4E1DD8F743BF94984E1F9` | TIC plus several ion traces; tests graph grouping, per-ION metadata, and different intensity scales. |
| `bench_06_photo_two_graphs_page` | `photo_2026-05-06_20-12-22.jpg` | 964x1280, 106950 bytes | `04B95E7D8B992FE31708AEAB46E6582329AF1F48B54DB300EF4B664A4EDCB090` | Printed page photo with two chromatograms and perspective/page noise; tests document correction and graph order. |
| `bench_07_rotated_page_photo` | `photo_2026-05-06_20-12-11.jpg` | 1280x964, 103875 bytes | `83B60C45E6D9C66BAC5B60A150EE9B283D9BC07E6B5EF4FDF8BD7A107EC3A105` | Rotated printed page photo; tests orientation detection, rotation correction, and axis recovery. |
| `bench_08_mz71_duplicate_candidate` | `photo_2026-05-07_20-42-40.jpg` | 576x1280, 70617 bytes | `C1EF5E8BC1BD3BB3F9921A9A102FE8B3BADB7ED6BF9EC5E49DFD6EE54D15E0C6` | Similar/duplicate `m/z 71` phone screenshot candidate; tests duplicate handling and crop consistency. |

Duplicate and near-duplicate handling is part of the bench. Use SHA-256 for exact
identity and a later perceptual hash/feature signature for visual near-duplicates.

## Phase 0 - Fixture Intake And Expected Facts

Goal: turn the eight images into reproducible, neutral project fixtures.

- [x] Copy images into a neutral fixture directory:
  `composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/`.
- [x] Record filename, dimensions, byte size, SHA-256, expected graph count, rotation
  requirement, and fixture tags.
- [x] Add a fixture manifest under `docs/reference/chromatogram_bench/`.
- [x] Mark duplicate or near-duplicate candidates without deleting them.
- [x] Add first human expected facts for each image:
  - graph count;
  - known title or ION when visible;
  - rough X/Y axis labels and ranges;
  - special hazards such as rotated page, poor crop, multiple graphs, stacked traces,
    phone UI, page text, or perspective distortion.
- [x] Add a lightweight desktop fixture identity test.

Exit criteria:

- the reference set is committed inside the repo;
- a developer can identify which fixture failed and what stage it is meant to stress;
- no user-specific absolute file paths are committed.

## Phase 1 - Offline Analysis Runner And Stage Audit

Goal: run the image pipeline without camera or Android scanner dependency.

- [x] Add a desktop/emulator-friendly fixture runner that accepts an image path and
  runs the same core image-processing logic used by app imports.
- [ ] Emit complete structured stage audit per input and per graph:
  - [x] normalized image metadata;
  - [x] graph candidates;
  - [x] selected graph crops;
  - [x] rejected graph candidates;
  - [x] preprocessing/filter variant ranking;
  - [x] OCR/model outputs when available;
  - [x] axis calibration candidates;
  - [x] extracted signal diagnostics or an explicit extraction block;
  - [ ] calculation warnings from a real calculation stage;
  - [ ] report validation result from a real report stage.
- [x] Save visual debug artifacts for failed stages when tests run locally.
- [x] Add a clear failure reason when a stage cannot proceed.

Phase 1.1 status:

- `OfflineAnalysisRunner` now executes the platform pipeline entry points without
  camera or Android scanner dependency.
- The runner emits structured audit records for normalization, document detection,
  preprocessing, graph-region detection, per-graph OCR, axis detection, curve mask,
  curve extraction, calculation, and report validation.
- On desktop, several platform actuals are still stubs. The runner records those
  limitations honestly as stage warnings or `SKIPPED` stages instead of treating the
  output as a completed analysis.

Next Phase 1.2 work slice:

1. Persist the audit output as JSON and a compact human-readable summary artifact.
2. Save visual debug artifacts for the current graph candidates and selected regions.
3. Add the first regression assertions for expected graph counts so the desktop stubs
   are visible as known failures/limitations instead of hidden behavior.

Phase 1.2 status:

- `OfflineAnalysisAuditArtifacts` serializes every runner result to pretty JSON and a
  compact Markdown summary.
- The desktop fixture test writes `audit.json`, `audit_summary.md`, and
  `graph_candidates.png` into each local fixture run directory.
- Multi-graph fixtures now assert that current desktop graph splitting limitations are
  visible through `graph.count_mismatch.*` warnings instead of hidden behind a false
  pass.

Next Phase 1.3 work slice:

1. Replace or supplement desktop graph-region stubs with real candidate detection.
2. Keep the JSON/summary/PNG artifacts as the primary diagnostic output.
3. Start enforcing expected graph counts once candidate detection is implemented.

Phase 1.3 status:

- The desktop graph detector now uses ImageIO-based bright-panel, line, contour, and
  density passes instead of returning the full image.
- `bench_01` expected facts were corrected from the old `m/z 71` one-graph note to
  the visible two-graph Ion 217/218 printed-page fixture.
- The bench now strictly enforces graph counts for the corrected `bench_01` and the
  single-graph fixtures that currently pass.
- Current graph-count status:
  - pass: `bench_01`, `bench_02`, `bench_03`, `bench_07`, `bench_08`;
  - known mismatch with explicit warning: `bench_04` expected 4 / detected 3,
    `bench_05` expected 4 / detected 3, `bench_06` expected 2 / detected 3.

Next Phase 1.4 work slice:

1. Improve stacked/multi-panel split precision for `bench_04`, `bench_05`, and
   `bench_06`.
2. Preserve the strict graph-count contract for fixtures that already pass.
3. Use generated `graph_candidates.png` overlays as the primary diagnostic artifact
   when tuning each remaining split.

Phase 1.4 status:

- Added an axis-panel detector pass for stacked and multi-panel chromatograms.
- `bench_04`, `bench_05`, and `bench_06` now produce the expected graph count.
- All eight bench fixtures now have strict graph-count assertions in the executable
  desktop test.

Next Phase 2.1 work slice:

1. Move from graph-count calibration into crop quality and preparation quality.
2. Add preprocessing/filter variant audit so every graph records which prepared image
   variant should drive OCR/axis/curve extraction.
3. Keep calculation blocked until usable axis and curve extraction are available.

Exit criteria:

- each fixture can be run to either a structured report or a structured stage failure;
- failure location is visible without reading raw logs.

## Phase 2 - Image Preparation And Graph Detection

Goal: make graph detection robust before calculating peaks.

- [x] Normalize right-angle orientation before graph search.
- [ ] Remove phone UI/document background from graph search.
- [ ] Detect page/document bounds where possible.
- [ ] Run a filter sweep for hard images:
  - grayscale;
  - contrast;
  - sharpened;
  - threshold/binary;
  - line-preserving variants.
- [ ] Select the best graph-region candidates using geometry, axis density, line
  density, tick/label evidence, and confidence scoring.
- [ ] Reject surrounding article text, toolbar/status bars, page margins, hands,
  binder rings, and notebook backgrounds.

Phase 2.1 status:

- Offline runner now ranks preprocessing variants per detected graph and records the
  selected variant in `audit.json` and `audit_summary.md`.
- The selected variant path is used by downstream OCR, axis detection, and curve mask
  stages instead of blindly reusing the default contrast image.
- Ranking is deterministic and auditable: dark pixel ratio, edge density, contrast,
  horizontal line strength, vertical line strength, score, rank, and warnings are
  recorded per variant.
- Android uses the real preprocessing variants already produced by the app scanner
  pipeline. Desktop currently ranks the available variant files, but those desktop
  variants are still source copies until the desktop preprocessor is upgraded.
- Axis labels, units, tick reading, curve extraction, and calculation remain blocked
  for later phases; this slice only makes the preparation input explicit.

Phase 2.2 status:

- Desktop preprocessing no longer emits source copies. It now uses the same shared
  deterministic preprocessing math as Android for grayscale, contrast-enhanced,
  sharpened, scan-style, binary, and morphology variants.
- The shared preprocessing path keeps desktop fixture calibration closer to Android
  phone behavior while preserving platform-specific image loading/saving only.
- The desktop bench test now writes `selected_preprocessing_graph_N.png` for each
  detected graph so the exact selected variant crop can be inspected visually.
- Full axis/tick/unit extraction and curve calculation remain blocked for later
  phases; this slice only makes prepared input parity and visual QA auditable.

Next Phase 2.3 work slice:

1. Re-run crop-quality tuning against the eight fixtures using the real desktop
   preprocessing variants.
2. Improve rejection of non-graph context when selected crops still include toolbar,
   article text, page margins, or document background.
3. Keep full calculations blocked until usable axes and curves exist.

Phase 2.3 status:

- Offline graph audit now includes a crop-quality gate per selected graph region:
  area ratio, edge contacts, full-image status, broad-edge crop status, and
  calculation-readiness.
- Large full-image fallbacks and broad edge-touching crops are recorded as
  `not calculation-ready`, so later deterministic calculation cannot silently start
  from a full page/screenshot crop.
- Small clean exports may still use the full image when that image is itself the
  graph; the gate distinguishes this from large page/screenshot fallback.
- Fixture tests now require context-heavy printed/rotated cases to be flagged while
  normal graph crops remain calculation-ready at the crop-quality gate.

Next Phase 2.4 work slice:

1. Use the crop-quality warnings to tighten the actual graph bounds for broad
   printed-page and screenshot crops.
2. Add orientation/page-bound diagnostics for the rotated printed-page case instead
   of accepting full-image fallback as a final crop.
3. Keep full calculations blocked until usable axes and curves exist.

Phase 2.4 status:

- Added a shared graph-region refinement stage with Android and desktop pixel samplers.
- The offline runner now records original versus refined crop bounds and routes OCR,
  axis detection, curve masking, and curve extraction through the refined crop.
- Broad edge-touching page/screenshot crops are tightened conservatively before crop
  quality is evaluated.
- Landscape/rotated full-page risk is now an explicit crop-quality blocker through
  `crop.possible_rotated_page_or_landscape_scan`.
- This phase improves preparation bounds only. Axis/tick reading, curve extraction,
  and real calculation remain blocked until later phases provide usable data.

Next Phase 2.5 work slice:

1. Inspect refined crop artifacts visually for all eight fixtures and tighten any
   graph bounds that still include document/page context.
2. Promote refined-crop quality expectations from broad warnings to per-fixture crop
   contracts where the visible graph bounds are stable enough.
3. Keep calculations blocked until axis calibration and curve extraction are usable.

Phase 2.5 status:

- Visually inspected the latest graph overlays and selected preprocessing crops.
- Stable graph crops for clean screenshot/export/multi-panel fixtures are now locked
  in the executable bench test with per-graph crop-bound contracts.
- Broad printed-page crops that were only improved by edge trimming are no longer
  accepted as calculation-ready. They now expose
  `crop.refinement_not_precise_for_broad_context` and remain blocked.
- Rotated/landscape page risk remains blocked and is also marked as unresolved broad
  context when only edge trimming was possible.
- A direct plot-frame line-refinement experiment was rejected for this phase because
  it could cut real graph signal on photographed pages; it should be revisited as a
  dedicated page/plot-bound detector instead of being committed as a weak heuristic.

Next Phase 2.6 work slice:

1. Implement a dedicated plot-bound detector for photographed printed pages using
   page/document context, axis evidence, and visual crop artifacts.
2. Fix unresolved broad-context fixtures (`bench_01`, `bench_06`, `bench_07`) without
   allowing edge-trim-only crops to pass as calculation-ready.
3. Preserve the stable crop-bound contracts already locked for clean fixtures.

Phase 2.6 status:

- Added explicit right-angle rotation diagnostics for landscape/rotated page crops.
  `bench_07` now exposes `crop.right_angle_rotation_required_before_analysis` instead
  of being treated as an ordinary graph crop.
- Added crop-boundary risk analysis for clipped peak tops. If long vertical signal
  runs touch the top edge of the selected crop, the graph is blocked with
  `crop.signal_touches_top_edge_possible_clipped_peaks`.
- Calculation readiness now requires both crop-quality acceptance and crop-boundary
  acceptance. A crop can no longer pass only because its rectangle is plausible.
- The current audit intentionally blocks suspicious cases:
  - `bench_07`: right-angle rotation required;
  - `bench_02`: possible top-edge clipped dominant peak;
  - `bench_06` lower graph: possible top-edge clipped signal;
  - `bench_01`, `bench_06`, `bench_07`: unresolved broad photographed-page context.
- This phase adds hard diagnostics and blocks unsafe crops. It does not yet auto-rotate
  the page or solve exact plot bounds for photographed pages.

Next Phase 2.7 work slice:

1. Add a right-angle orientation correction stage before graph detection and verify it
   on `bench_07`.
2. Add a real photographed-page plot-bound detector that uses frame/axis evidence
   without cutting peak tops.
3. Re-test `bench_01`, `bench_02`, and `bench_06` specifically for first-peak
   preservation before allowing calculations.

Phase 2.7 status:

- Added a shared right-angle orientation correction stage after EXIF normalization and
  before preprocessing/graph detection.
- Added Android and desktop rotators for the common orientation stage, so the app flow
  and offline fixture runner use the same correction contract.
- `bench_07_rotated_page_photo` now rotates from `1280x964` to `964x1280` before
  graph detection and no longer carries `crop.right_angle_rotation_required_before_analysis`.
- Added graph-boundary correction after region selection. It searches an expanded
  window for the horizontal axis and left axis, then expands the ROI only when it
  recovers a significant lost top/left boundary. For rotated photographed pages, it
  also preserves the left panel-label band so the selected graph block includes the
  full visible graph area, not only the inner plot area.
- The executable bench test now locks the rotated fixture so the selected graph must
  preserve the full left graph panel and at least 80% of the visible chromatogram
  panel width after orientation/boundary correction.
- Audit artifacts now record orientation correction metadata, and visual overlays are
  drawn on the oriented image when a page was rotated.
- Remaining limitation: this is not yet a full photographed-page plot-bound detector.
  `bench_01` and `bench_06` still need dedicated page/plot-bound work before full
  calculation can be allowed on hard photographed pages.

Next Phase 2.8 work slice:

1. Implement the photographed-page graph-panel detector for `bench_01` and `bench_06`
   using frame, axis, title/ION, tick-label, and signal evidence without cutting
   dominant first peaks or visible time/abundance values.
2. Re-check `bench_02` and `bench_08` to ensure phone screenshot imports remain stable
   after the photographed-page detector is added.
3. Add an explicit second-stage `plot area bounds` audit inside the accepted graph
   panel before enabling curve extraction and calculation.
4. Keep calculation blocked until both graph-panel bounds and plot-area bounds are
   accepted by the audit.

Exit criteria:

- all fixtures produce the expected number of graph candidates or a precise failure;
- selected crop bounds are recorded for every graph;
- no calculation starts from a full screenshot/page when a graph crop is required.

## Phase 3 - Multi-Graph Splitting And Ordering

Goal: handle pages with 2, 3, or 4 graphs as first-class inputs.

- [ ] Split stacked plots and multi-panel figures into independent graph records.
- [ ] Sort graphs top-to-bottom and left-to-right with row grouping.
- [ ] Preserve shared context such as page metadata while keeping per-graph axis,
  ION, signal, peaks, warnings, and report blocks separate.
- [ ] Detect if a graph is blank/near-empty and report that honestly instead of
  inventing peaks.
- [ ] Add fixture expectations for `bench_04`, `bench_05`, `bench_06`, and future
  multi-graph inputs.

Exit criteria:

- multi-graph images render as `graph N -> report N`;
- warnings and peaks cannot leak from one graph to another.

## Phase 4 - Axis, ION, Title, And Tick Extraction

Goal: make pixel-to-unit calibration auditable.

- [ ] Extract chromatogram title, ION/channel, sample name, and mode when visible.
- [ ] Read X/Y labels and units in Russian and English where visible.
- [ ] Extract major ticks and estimate visible ranges.
- [ ] Store confidence per field instead of a single broad OCR confidence.
- [ ] Support time axes in minutes and seconds.
- [ ] Support abundance/intensity/counts axes with large values and localized
  formatting.

Exit criteria:

- every report can explain how pixels became RT/intensity values;
- weak OCR/calibration blocks report generation only when required confidence is not met.

## Phase 5 - Curve Extraction And Deterministic Calculation

Goal: calibrate the scientific calculation independently from model/runtime issues.

- [x] Extract raw curve points from each graph crop.
- [ ] Keep raw, smoothed, baseline, corrected, and integrated signals auditable.
- [x] Apply current `CalculationEngine` settings:
  - boundary method;
  - clamp negative;
  - max width;
  - integration mode;
  - noise method and S/N threshold.
- [ ] Compare detected peaks against fixture expected facts and visual sanity checks.
- [ ] Flag missed dominant peaks, false peaks from text/grid/axis lines, and blank
  graph false positives.

Phase 5.1 status:

- The offline runner emits `signal_convert` after curve extraction.
- Missing confirmed X/Y calibration skips signal conversion with an explicit
  `signal_convert.axis_calibration_required` warning.
- Confirmed manual calibration converts curve points through `SignalConverter` and
  audits point count, time range, intensity range, duplicate count, gap count, and sort
  validity.

Phase 5.2 status:

- The offline runner emits `peak_detection` only after a calibrated `DigitalSignal`
  exists.
- Peak detection runs through the current deterministic `CalculationEngine` using the
  offline balanced calculation parameters and records peak count, significant peak
  count, dominant peak time/height/area share, baseline method, boundary method,
  integration mode, clamp-negative, max-width, min-S/N, and warnings.
- Missing signal data skips peak detection with `peak_detection.signal_required`; the
  pipeline does not fabricate a deterministic-only result.
- Acceptance is tested on real fixture examples: clean `bench_03_small_tic_export`,
  hard two-graph photographed `bench_06_photo_two_graphs_page`, and rotated
  `bench_07_rotated_page_photo`.

Phase 5.3 status:

- The offline runner emits `peak_metrics` after `peak_detection` and only when a real
  `CalculationRun` exists.
- The audit now records retention-time ordering, total integrated area, area percent
  sum, maximum height, first/last peak time, boundary width range, invalid numeric
  count, invalid boundary count, non-positive area/height counts, missing width count,
  low S/N count, low-confidence count, overlap-review count, and peak-warning count.
- Calculation readiness now requires the peak-metrics gate. Structural metric failures
  block at `peak_metrics` instead of flowing into report validation.
- The fixture gate verifies this stage on the clean `bench_03_small_tic_export`, hard
  two-graph `bench_06_photo_two_graphs_page`, and rotated
  `bench_07_rotated_page_photo` examples.

Phase 5.4 status:

- The offline audit now stores a per-peak snapshot with peak number, apex RT, left/right
  boundaries, height, area, area percent, S/N, confidence, overlap state, and warning
  count.
- The Markdown summary renders this per-peak snapshot so individual peak rows can be
  reviewed without parsing the full JSON.
- Desktop fixture runs with manual calibration now write `peak_overlay_graph_N.png`
  beside the existing focus artifacts. The overlay draws left/right integration
  boundaries and apex markers in the same graph-panel coordinate system used by
  calibration.
- The executable fixture test validates those overlays on `bench_03_small_tic_export`,
  `bench_06_photo_two_graphs_page`, and `bench_07_rotated_page_photo`.

Phase 5.5 status:

- The offline runner accepts `peakSanityExpectations` and emits an explicit
  `peak_sanity` stage after `peak_metrics`.
- Peak sanity can require a minimum peak count and can lock expected apex retention
  times with a fixture-specific tolerance. Missing expected apexes now block
  calculation at `peak_sanity` instead of flowing into report validation.
- The Markdown artifact adds a `Peak Sanity` table with expectations, matched/missing
  apexes, unexpected candidates, tolerance, and warnings.
- The fixture test now proves the gate on three real examples: the two-graph photo and
  rotated page pass minimum-peak sanity, while `bench_03_small_tic_export` intentionally
  blocks because the current extraction misses several labeled apexes at 3.244, 3.890,
  4.647, 5.610, and 8.560 min.

Phase 5.6 status:

- The desktop curve-mask preparer now uses the detected x-axis/origin to remove floating
  text components on compact low-resolution plots. This targets peak labels and title
  text that are inside the plot rectangle but do not connect back to the signal baseline.
- The cleanup is guarded by column-coverage readiness and is limited to compact plots,
  so weak/near-empty large photographed channels such as `bench_06` graph 2 are not
  stripped below the usable curve threshold.
- `bench_03_small_tic_export` now passes the expected-apex sanity contract: all five
  labeled apexes at 3.244, 3.890, 4.647, 5.610, and 8.560 min are matched.
- The full `ChromatogramBenchFixtureTest` still passes for the clean compact export,
  two-graph photographed page, and rotated photographed page.

Phase 5.7 status:

- The desktop curve-mask preparer now suppresses narrow, tall right-frame line
  components before curve extraction. The cleanup is guarded by retained column
  coverage so it cannot remove enough signal to make a weak channel unusable.
- The fixture contract now verifies that `bench_06_photo_two_graphs_page` and
  `bench_07_rotated_page_photo` apply `right_frame_lines` suppression and do not accept
  detected peaks on the right plot frame.
- `bench_06` keeps both photographed graphs signal-ready after cleanup, and `bench_07`
  keeps the rotated-page peak train while dropping the right-edge frame artifact.
- The full `ChromatogramBenchFixtureTest` passes after the frame-line cleanup.

Completed Phase 5.8a work slice:

1. Peak detection audit now records the detection signal source, noise method/value,
   total local-maximum candidates, rejected candidate count, and dominant rejection
   reasons.
2. On `bench_06`, graph 1 currently exposes `77` candidates with `75` rejected by
   `prominence_below_threshold`; graph 2 exposes `90` candidates with `81` rejected
   by the same reason. This confirms the current under-detection is downstream of
   curve extraction and inside the candidate/noise/prominence gate, not an absence
   of visible trace candidates.
3. A local first-difference MAD experiment increased graph 1 completeness but also
   accepted many bleed-through artifacts on graph 2, so it was not accepted. The next
   slice must first classify or suppress non-edge artifacts before changing noise or
   prominence behavior.

Completed Phase 5.8b.1 work slice:

1. Reviewed plot digitizer, morphology, line-detection, baseline, peak-picking, and
   chromatography feature-detection references.
2. Recorded the technical decision in `docs/CHROMATOGRAM_PIXEL_DETECTION_RESEARCH.md`:
   no threshold loosening until internal plot artifacts are classified or suppressed.
3. The chosen direction is an auditable trace-evidence layer: component/continuity,
   skeleton/centerline, straight-line/text/bleed-through artifact score, and then
   dynamic peak constraints.

Completed Phase 5.8b.2 work slice:

1. `CurveMaskResult` now carries a non-destructive `traceArtifactAudit` with artifact
   mask path, baseline row, artifact pixels/ratio, floating component count, vertical
   and horizontal straight-line component counts, top-band component count, and warning
   tags.
2. Desktop bench runs now write `graph_N/trace_artifacts.png`. The PNG keeps clean-mask
   evidence in gray and marks internal artifact-risk pixels in red.
3. `bench_06` calibrated audit now distinguishes the two hard panels: graph 1 is about
   `4.8%` artifact-risk pixels with top-band text risk, while graph 2 is about `23%`
   artifact-risk pixels and is flagged as high internal artifact risk.
4. Accepted peak behavior is unchanged in this slice.

Completed Phase 5.8b.3 work slice:

1. `traceArtifactAudit` now records a separate cleanup hypothesis mask path, retained
   pixel ratio, retained column coverage, and threshold-relaxation guard.
2. Desktop bench runs now write `graph_N/trace_artifact_suppressed_mask.png` beside the
   current clean mask. The current accepted peak table still uses `mask_clean.png`; this
   slice does not change accepted peaks.
3. Artifact-heavy graphs block future threshold/noise loosening through
   `trace_artifact.threshold_relaxation_blocked` and
   `peak_detection.threshold_relaxation_blocked_by_trace_artifacts`.
4. The fixture contract protects `bench_06` graph 2 from threshold relaxation while
   keeping graph 1 eligible for later controlled completeness review.

Completed Phase 5.8b.4 work slice:

1. Peak detection now records the active detection profile, base peak count, tuned peak
   count, controlled tuning state, and tuning reason in JSON/Markdown audit artifacts.
2. A guarded completeness pass can lower the offline bench `minSnr` only when:
   `thresholdRelaxationAllowed=true`, the base run is valid, the base run is clearly
   under-detected, many candidates were rejected by prominence, and the tuned result
   stays within bounded peak-count limits.
3. On calibrated `bench_06`, graph 1 uses `guarded_completeness` and increases from `2`
   accepted peaks to `14`; graph 2 stays on the default profile because its artifact
   guard blocks threshold relaxation.
4. The rotated page fixture stays on the default profile because it already has enough
   base peaks; this prevents the tuning pass from reintroducing late/right-frame false
   positives.

Completed Phase 5.8b.5 work slice:

1. Guarded completeness now has an explicit `guardedQualityReview` audit with review
   peak count, lower-than-default S/N count, low-area-share count, narrow-boundary
   count, accepted state, and warnings.
2. Each guarded peak row records `qualityFlags` and `widthBase`, so recovered peaks can
   be reviewed individually instead of hidden behind aggregate peak counts.
3. The guarded run is rejected if too many peaks are below the default S/N reference,
   too many have low area share, or too many have very narrow boundaries.
4. On calibrated `bench_06`, graph 1 keeps the guarded `14`-peak table with `3`
   lower-than-default S/N review flags and no low-area or narrow-boundary flags; graph 2
   still has no guarded quality review because tuning remains blocked by artifacts.

Completed Phase 5.8b.6 work slice:

1. The calibrated fixture contract now broadens guarded-completeness review beyond
   `bench_06` by running additional hard fixtures `bench_01`, `bench_02`, and
   `bench_08` through the same manual-calibration peak path.
2. `bench_08_mz71_duplicate_candidate` is the first additional accepted guarded case:
   the base table has `5` peaks, guarded completeness reviews `9` peaks, and the
   quality review records `1` lower-than-default S/N peak with `0` low-area-share and
   `0` narrow-boundary peaks.
3. `bench_01_mz71_screenshot_page` stays on the default profile because both graph
   panels block threshold relaxation through the trace-artifact guard.
4. `bench_02_mz92_belyi_tigr` exposes `thresholdRelaxationAllowed=true`, but stays on
   the default profile because its base table has `11` peaks and is not treated as
   under-detected.
5. `bench_04_stacked_xic_resolution` and `bench_05_tic_plus_ions` are intentionally
   not promoted into guarded peak tuning yet: some weak stacked ion panels block
   earlier at `curve_extract`/`signal_convert`, so the next work must repair extraction
   before peak-completeness logic can honestly evaluate them.

Completed Phase 5.8b.7 work slice:

1. `CurveExtractionResult` now separates dense curve coverage from sparse trace
   readiness. A sparse XIC/ion trace can be considered usable when there are at least
   `24` extracted points and at least `5%` column coverage, even if the continuous
   baseline is not visible.
2. Sparse trace acceptance is auditable: low-coverage accepted traces receive
   `curve_extract.sparse_trace_low_column_coverage_accepted`; localized sparse evidence
   receives `curve_extract.sparse_trace_localized_review_required`.
3. `bench_04_stacked_xic_resolution` now converts all four panels to signal:
   graph 3 has `80` curve points / `15.9%` coverage / `4` detected peaks, and graph 4
   has `34` curve points / `6.7%` coverage / `1` detected peak with localized sparse
   review required.
4. `bench_05_tic_plus_ions` now converts all four panels to signal: graphs 2-4 remain
   sparse at `86`, `91`, and `35` curve points, with `4`, `9`, and `4` detected peaks.
5. No peak thresholds were loosened in this slice; the new behavior only prevents
   sparse but real ion traces from being blocked at `signal_convert.curve_points_required`.

Completed Phase 5.8b.8 work slice:

1. Sparse stacked-ion peak tables now receive a separate
   `sparseTraceQualityReview` audit rather than silently flowing into the future
   final report as normal dense traces.
2. Sparse traces always require report confidence text through
   `peak_detection.sparse_trace_report_confidence_required`; localized traces also
   keep `peak_detection.sparse_trace_localized_review_required`.
3. Sparse peak rows carry per-peak flags such as `sparse_trace.low_column_coverage`,
   `sparse_trace.localized_evidence`, `sparse_peak.low_area_share`, and
   `sparse_peak.overlap_review` where applicable.
4. `bench_04` sparse graphs remain on the default peak detector with no threshold
   relaxation: graph 3 has `4` reviewed peaks, graph 4 has `1` localized reviewed peak.
5. `bench_05` sparse graphs remain on the default peak detector with no threshold
   relaxation: graph 2 has `4` reviewed peaks, graph 3 has `9` reviewed peaks with
   `4` low-area-share and `6` overlap-review flags, and graph 4 has `4` reviewed
   peaks with `4` overlap-review flags.
6. Guarded completeness remains independent: sparse trace readiness does not lower
   `minSnr`, does not populate `tunedPeakCount`, and does not switch the detection
   profile away from `default`.

Next Phase 6.1 work slice:

1. Validate the calibrated fixture audits against the structured final report
   contract, including sparse-trace confidence wording and guarded-completeness
   wording.
2. Keep raw debug warnings secondary to professional report sections.
3. Show missing domain/metadata explicitly instead of inventing chemical conclusions.

Exit criteria:

- calculation failures can be separated from crop/OCR/signal extraction failures;
- peak tables are ordered by retention time and include all report-contract metrics
  that can be calculated.

## Phase 6 - Structured Report Validation

Goal: turn each fixture result into the future professional report contract.

- [x] Validate calibrated fixture audits against the `REPORT_SPEC.md` section contract.
- [ ] Render per-graph overview, preparation, axis calibration, graph overlay, peak
  table, quality, Kovats/domain interpretation, warnings, and appendix.
- [ ] Keep raw codes and debug details secondary.
- [ ] Show missing metadata as missing, not invented.
- [ ] Use the Belyi Tigr rendered report as shape/depth reference, not numeric truth.

Completed Phase 6.1 work slice:

1. `OfflineAnalysisAudit` now includes a structured `reportContract` audit that
   checks the section family required by `REPORT_SPEC.md`: overview, source and graph
   preparation, axis calibration, peak table, rendered graph, chromatographic quality,
   Kovats, interpretation, warnings, and technical appendix.
2. The offline runner records a `report_validation` stage after calculation readiness.
   Stage success means validation ran; `reportContract.ready` records whether the
   current data is enough for the final report contract.
3. Clean, hard photographed, rotated, two-graph, and sparse stacked-ion fixtures now
   exercise the report-contract audit in desktop tests.
4. Sparse/XIC panels propagate report-level confidence requirements into the
   `peak_table`, rendered graph, chromatographic quality, and interpretation sections.
5. Current honest report-contract blockers are in the peak table: offline peak audit
   rows still do not expose `peak_fwhm_column`, `peak_asymmetry_column`, or
   `compound_candidate_columns`.
6. Kovats and chemical interpretation remain warning-level sections for now: they
   must render as not calculated / missing local knowledge instead of inventing values.

Completed Phase 6.2 work slice:

1. `OfflinePeakAudit` now exposes FWHM (`widthHalfHeight`), USP tailing factor, and
   EP asymmetry factor directly from `PeakResult`.
2. Peak rows now carry explicit assignment/Kovats statuses. When the app has no
   compound, formula, carbon-number, or Kovats evidence, the audit records
   `NOT_CALCULATED` instead of leaving the report-table columns structurally absent.
3. The audit summary peak table now includes FWHM, tailing, asymmetry, compound status,
   formula status, carbon status, and Kovats status.
4. The report-contract peak table no longer blocks on FWHM, asymmetry/tailing, or
   compound/Kovats columns for calculation-ready fixtures. Missing chemistry remains
   warning-level and must render as not calculated.
5. Fresh fixture audits show `reportContract.ready=true` for the clean TIC, rotated,
   two-graph, guarded/sparse fixtures that are calculation-ready. `bench_02` remains
   blocked only by source/crop boundary clearance, not by peak-table structure.

Completed Phase 6.3 work slice:

1. Render calibrated offline-audit results into report-section artifacts, preserving
   graph/report ordering through `calibrated_report.md`.
2. Keep warnings human-readable in the main report surface and raw warning codes in
   the technical appendix.
3. Render per-graph preparation, axis calibration, peak table, chromatographic quality,
   Kovats/interpretation, and report-section readiness from the structured contract.
4. Require the calibrated report artifact in desktop fixture tests, including graph
   ordering, full peak-table columns, explicit not-calculated chemistry, and sparse
   warning code separation.
5. Preserve the Belyi Tigr reference shape/depth without claiming final mobile UI
   completion yet. The Markdown artifact is a contract artifact, not the finished
   phone report UI.

Completed Phase 6.4 work slice:

1. Connect the rendered calibrated report artifact to the available visual evidence:
   graph candidate overlay, manual calibration focus image, curve overlay, and peak
   overlay.
2. Add per-graph `Visual Evidence` report sections that list the stable artifact paths,
   intended report placement, and generation status.
3. Keep trace-artifact masks and cleanup hypotheses in the technical appendix path,
   while keeping graph focus, curve overlay, and peak overlay available for the future
   main report surface.
4. Require desktop fixture tests to prove that every referenced visual artifact exists
   for calibrated fixture reports.
5. Keep graph/report ordering and raw-code separation locked while preparing export/UI
   integration.

Completed Phase 6.5 work slice:

1. Define how the calibrated report artifact maps into the actual mobile/export report
   surface without showing raw Markdown as the finished phone UI.
2. Preserve the visual-evidence contract so graph focus, curve overlay, and peak overlay
   can be rendered near their related sections.
3. Keep technical appendix data secondary and explicit.
4. Add `calibrated_report_ui_contract.json` as a structured UI/export contract beside
   `calibrated_report.md`.
5. Mark `rawMarkdownIsFinalUi=false`, keep main report sections free of raw warning
   codes, and keep raw codes visible only in the technical appendix contract.
6. Require fixture tests to parse the UI contract, verify graph order, verify visual
   evidence placement, and verify generated artifacts exist when marked as generated.

Completed Phase 6.6 work slice:

1. Wire the calibrated report UI contract into the real report/export surface instead
   of relying on raw Markdown parsing.
2. Render graph focus, curve overlay, and peak overlay near their related report
   sections.
3. Keep technical appendix details behind secondary UI/export affordances.
4. Add the common `chromatogram_report_ui_contract.json` export for real calculation
   reports.
5. Render HTML directly from the structured UI contract instead of parsing Markdown.
6. Keep the Compose report preview ordered by the UI contract and show visual evidence
   chips next to preparation, axis, graph overlay, and peak-table sections.

Next Phase 7 work slice:

1. Start the offline domain knowledge pack for common chromatogram types and ions.
2. Keep chemical assignments as explicit hypotheses unless backed by local rules,
   retention-index data, spectrum/library evidence, or user input.

Exit criteria:

- fixture reports are visually and structurally complete enough to drive future UI work;
- a report cannot pass if it is only terminal-like text or raw Markdown on the phone.

## Phase 7 - Offline Domain Knowledge Pack

Goal: prepare local scientific context without relying on model memory.

- [x] Extend the local knowledge pack for common chromatogram types:
  - GC-MS TIC;
  - GC-MS EIC/XIC;
  - SIM/ion-channel traces.
- [x] Add common ions and interpretation notes beyond the initial `m/z 92` case.
- [x] Add oil, condensate, gas, alkane, alkylbenzene, and related compound-class
  notes as structured offline data.
- [x] Keep compound assignments as hypotheses unless supported by retention index,
  spectrum/library evidence, local rules, or user data.

Completed Phase 7.1-7.3 work slices:

1. Define the local knowledge-pack schema, validator, and initial base-pack storage.
2. Add conservative GC-MS EI `m/z 92` / alkylbenzene-oriented reference data.
3. Add C7-C30 n-paraffin reference-series support for Kovats calculations.

Completed Phase 7.4 work slice:

1. Expand built-in chromatogram modes to GC-MS TIC, EIC, XIC, and SIM.
2. Add bench/reference ion-channel coverage for `m/z 57`, `71`, `83`, `91`, `92`,
   `191`, `217`, `218`, `198.0315`, `326`, `360`, and `394`.
3. Add conservative compound-class notes for normal paraffins, alkylbenzenes,
   petroleum-biomarker channels, and method-targeted extracted channels.
4. Wire local knowledge into structured report interpretation as class-level
   hypotheses with source notes and assignment cautions.
5. Keep Kovats output scientific: formula and local RI scale can be shown, but measured
   reference retention times are not fabricated and Kovats values stay not calculated
   until same-method references are supplied.

Exit criteria:

- report interpretation can cite offline local knowledge provenance;
- model-suggested chemical explanations remain lower-confidence when unsupported.

## Phase 8 - Model-Assisted Stages

Goal: add models only where they improve recognition without replacing calculations.

- [ ] Run deterministic-only stages first where they are sufficient.
- [ ] Use LiteRT/GGUF VLM assistance for hard graph bounds, title/ION extraction, and
  interpretation only through strict structured contracts.
  - [x] Phase 8.1 defines the strict stage contract and model eligibility gates.
- [x] Require base GGUF plus matching `mmproj` for GGUF image analysis.
- [x] Keep OCR/document-only models out of strict chromatogram analysis unless a
  validated adapter exists.
- [x] Fail clearly when a required neural vision stage fails.
- [x] Validate saved-report propagation for model-stage warnings and timings.
- [x] Emit a compact Android/logcat marker for saved report audit evidence.
- [ ] Validate the same audit behavior on Android/device runs.

Completed Phase 8.1 work slice:

1. Add `ModelAssistedAnalysisContract` as the shared strict contract for model-assisted
   chromatogram stages.
2. Define required VLM contracts for graph-region and title/ION/axis extraction,
   optional VLM hints for axis structure, local-knowledge-only chemical interpretation,
   and deterministic-only numeric calculation.
3. Route chromatogram model eligibility through the contract so GGUF image analysis
   requires base + `mmproj`, and OCR/document-only families are rejected for strict
   chromatogram VLM analysis.
4. Reuse the same contract markers for non-skippable full-analysis failures.
5. Document the contract in `docs/MODEL_ASSISTED_STAGE_CONTRACT.md`.

Completed Phase 8.2 work slice:

1. Add model-stage audit augmentation to stored processing metadata.
2. Record strict VLM contract timings (`model.graph_region`, `model.title_ion_axis`)
   from the processing sweep envelope when a LiteRT/GGUF/MIXED vision runtime executes.
3. Record failed required VLM stages as structured `FAILED` report warnings when full
   analysis has no executed vision model/runtime.
4. Preserve selected/executed model metadata and route model-stage warnings through the
   same stored metadata path used by final report export.

Completed Phase 8.3a work slice:

1. Add a regression test for the full saved-report chain:
   `buildProcessingReportMetadataConfig -> ChromatogramEntity.algorithmConfig ->
   buildCalculationReportOptions -> CalculationRunReportMapper`.
2. Verify that selected model metadata, device name, stage timings, and failed required
   model-stage warnings survive from processing metadata into the final report.
3. Verify that a selected GGUF/VLM with no executed runtime remains visibly blocked in
   the final report through `model.execution_missing`,
   `model.graph_region.required_vision_failed`,
   `model.title_ion_axis.required_vision_failed`, and `runtime.executed_unknown`.
4. Keep Android/device model-run validation as Phase 8.3b instead of marking it done
   without a real device execution.

Completed Phase 8.3b.1 work slice:

1. Add a compact `REPORT_AUDIT` summary builder for saved processing report metadata.
2. Log `PIPELINE[REPORT_AUDIT] REPORT_AUDIT ...` after each chromatogram graph is
   inserted on Android/user-flow auto-save.
3. Include chromatogram id, source, graph indexes, processing mode, selected model,
   executed model, executed runtime, device, stage timings, and warning codes in one
   logcat-visible line.
4. Keep actual Android model-run validation as Phase 8.3b.2 because it still requires
   running LiteRT/GGUF analysis on a device and capturing the emitted audit line.

Exit criteria:

- model failure does not silently produce a fake completed report;
- selected model, executed model, runtime, and timing appear in the report audit.

## Phase 9 - Android And Device Parity

Goal: after desktop/emulator accuracy is stable, verify phone behavior.

- [ ] Compare Android imported-photo results with desktop fixture runner results.
- [ ] Test camera/Smart Scan behavior on real devices separately.
- [ ] Verify weak devices without reducing scientific depth.
- [ ] Optimize memory, model loading, context, and batching only after correctness
  failures are separated from performance failures.

Exit criteria:

- phone differences are diagnosed as input/camera/runtime/device issues, not unknown
  scientific calculation changes.

## Completed Work Slices

- Phase 0.1:

1. Import the eight images into neutral fixture storage.
2. Create a fixture manifest with hashes, dimensions, expected graph count, and tags.
3. Add a lightweight validation test that verifies the fixture files did not change.
4. Commit that fixture baseline before implementing analysis logic.

- Phase 1.1:

1. Add `OfflineAnalysisRunner`.
2. Run normalize, document detection, preprocess, graph detection, OCR, axis detection,
   curve mask, and curve extraction stages without camera dependency.
3. Add desktop fixture coverage that confirms every bench image produces stage audit and
   an honest blocked state until usable curve extraction exists.

- Phase 1.2:

1. Add JSON and Markdown audit artifact rendering.
2. Add local PNG graph-candidate overlays for desktop bench runs.
3. Make multi-graph mismatch warnings part of the executable bench contract.

- Phase 1.3:

1. Replace desktop full-image graph-region fallback with real ImageIO-based candidate
   detection.
2. Add bright-panel detection for phone screenshot imports with dark UI/background.
3. Correct `bench_01` expected facts to two Ion 217/218 graph panels.
4. Start strict graph-count assertions for fixtures that now pass.

- Phase 1.4:

1. Add axis-panel detection for stacked and multi-panel chromatograms.
2. Expand compact stacked-panel crops to include the full panel context when needed.
3. Promote `bench_04`, `bench_05`, and `bench_06` to strict graph-count assertions.

- Phase 2.1:

1. Add preprocessing variant models and platform rankers.
2. Rank source/grayscale/contrast/sharpened/scan-style/binary/morphology variants per
   selected graph region.
3. Record selected preprocessing input and all variant metrics in JSON and Markdown
   audit artifacts.
4. Route OCR, axis detection, and curve mask preparation through the selected variant.

- Phase 2.2:

1. Move deterministic preprocessing stage math into shared common code.
2. Make Android and desktop preprocessors use that shared math while keeping only
   image decode/encode platform-specific.
3. Add per-graph `selected_preprocessing_graph_N.png` bench artifacts for visual QA.

- Phase 2.3:

1. Add per-graph crop-quality audit fields.
2. Mark large full-image fallbacks and broad edge-touching crops as not
   calculation-ready.
3. Enforce the crop-quality gate in bench tests for context-heavy fixtures while
   preserving clean full-image exports.

- Phase 2.4:

1. Add shared graph-region refinement with Android and desktop pixel samplers.
2. Record original and refined crop bounds in JSON/Markdown audit artifacts.
3. Route downstream OCR, axis, and curve-mask stages through the refined crop.
4. Add rotated/landscape page-risk diagnostics as a calculation blocker.

- Phase 2.5:

1. Add stable per-graph crop-bound contracts for clean fixtures.
2. Add crop-quality fields for original area, unresolved broad context, and
   edge-trim-only refinement.
3. Keep edge-trim-only broad printed-page crops blocked instead of treating them as
   ready for calculation.

- Phase 2.6:

1. Add crop-boundary risk diagnostics for top-edge clipped peaks.
2. Add explicit right-angle rotation warnings for rotated page crops.
3. Make calculation readiness depend on both crop quality and crop-boundary safety.

- Phase 2.7:

1. Add shared right-angle orientation correction before graph detection.
2. Add Android and desktop image rotators for the orientation stage.
3. Add axis-aligned graph-boundary correction to recover lost top/left graph content.
4. Preserve the full graph-panel left band on rotated photographed pages instead of
   returning only the inner plot area.
5. Lock the rotated fixture against regressions that would drop early peaks or the
   left graph-panel context.

- Phase 5.1:

1. Add calibrated signal conversion after confirmed axis calibration.
2. Keep signal conversion skipped when manual/OCR calibration is missing.
3. Record calibrated signal diagnostics in JSON and Markdown audit artifacts.

- Phase 5.2:

1. Add audited peak detection after calibrated signal conversion.
2. Run the deterministic `CalculationEngine` for the readiness gate and record the
   peak-detection calculation parameters.
3. Keep peak detection skipped until calibrated signal data exists.
4. Validate the gate on the clean, two-graph photographed, and rotated real bench
   examples.

- Phase 5.3:

1. Add audited peak metrics and integration review after peak detection.
2. Block calculation readiness when structural peak metrics are invalid.
3. Record area, boundary, S/N, confidence, overlap, and warning diagnostics in JSON and
   Markdown audit artifacts.
4. Validate the gate on the same clean, two-graph photographed, and rotated real bench
   examples.

- Phase 5.4:

1. Add per-peak audit rows to the offline peak detection audit.
2. Render the per-peak snapshot in Markdown audit artifacts.
3. Write peak overlay PNG artifacts for calibrated fixture runs.
4. Validate overlay generation and per-peak boundary sanity on the clean, two-graph
   photographed, and rotated real bench examples.
