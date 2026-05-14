# Chromatogram Calibration Bench Plan

Status: active planning contract.

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
  - [ ] preprocessing/filter variant ranking;
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

- [ ] Normalize orientation and remove phone UI/document background from graph search.
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

- [ ] Extract raw curve points from each graph crop.
- [ ] Keep raw, smoothed, baseline, corrected, and integrated signals auditable.
- [ ] Apply current `CalculationEngine` settings:
  - boundary method;
  - clamp negative;
  - max width;
  - integration mode;
  - noise method and S/N threshold.
- [ ] Compare detected peaks against fixture expected facts and visual sanity checks.
- [ ] Flag missed dominant peaks, false peaks from text/grid/axis lines, and blank
  graph false positives.

Exit criteria:

- calculation failures can be separated from crop/OCR/signal extraction failures;
- peak tables are ordered by retention time and include all report-contract metrics
  that can be calculated.

## Phase 6 - Structured Report Validation

Goal: turn each fixture result into the future professional report contract.

- [ ] Validate every fixture against `REPORT_SPEC.md`.
- [ ] Render per-graph overview, preparation, axis calibration, graph overlay, peak
  table, quality, Kovats/domain interpretation, warnings, and appendix.
- [ ] Keep raw codes and debug details secondary.
- [ ] Show missing metadata as missing, not invented.
- [ ] Use the Belyi Tigr rendered report as shape/depth reference, not numeric truth.

Exit criteria:

- fixture reports are visually and structurally complete enough to drive future UI work;
- a report cannot pass if it is only terminal-like text or raw Markdown on the phone.

## Phase 7 - Offline Domain Knowledge Pack

Goal: prepare local scientific context without relying on model memory.

- [ ] Extend the local knowledge pack for common chromatogram types:
  - GC-MS TIC;
  - GC-MS EIC/XIC;
  - SIM/ion-channel traces.
- [ ] Add common ions and interpretation notes beyond the initial `m/z 92` case.
- [ ] Add oil, condensate, gas, alkane, alkylbenzene, and related compound-class
  notes as structured offline data.
- [ ] Keep compound assignments as hypotheses unless supported by retention index,
  spectrum/library evidence, local rules, or user data.

Exit criteria:

- report interpretation can cite offline local knowledge provenance;
- model-suggested chemical explanations remain lower-confidence when unsupported.

## Phase 8 - Model-Assisted Stages

Goal: add models only where they improve recognition without replacing calculations.

- [ ] Run deterministic-only stages first where they are sufficient.
- [ ] Use LiteRT/GGUF VLM assistance for hard graph bounds, title/ION extraction, and
  interpretation only through strict structured contracts.
- [ ] Require base GGUF plus matching `mmproj` for GGUF image analysis.
- [ ] Keep OCR/document-only models out of strict chromatogram analysis unless a
  validated adapter exists.
- [ ] Fail clearly when a required neural vision stage fails.

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
