# Chromatogram Bench Reference Set

This directory records the first desktop/emulator calibration fixtures for the
ChromaLab chromatogram pipeline. These images are not numeric ground truth. They are
used to validate input preparation, graph splitting, axis/ION extraction, curve
extraction, calculation diagnostics, and final report completeness.

The committed fixture files live under:

```text
composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/
```

Do not commit user-specific absolute source paths. If a source image is replaced, update
the SHA-256, dimensions, expected facts, and the fixture identity test in the same work
slice.

## Boundary Contract

Fixture calibration uses a two-level boundary contract:

- `graph panel bounds` include the complete visible graph block: title/ION, axis labels,
  tick values, time/abundance numbers, signal, and the visible graph frame.
- `plot area bounds` are derived later inside that panel for pixel-level curve
  extraction and deterministic calculations.

The fixture runner must first find the full graph panel. A crop that only captures the
inner plot/signal area is not acceptable when it loses visible tick values, labels, or
early graph context.

## Fixture Manifest

| ID | Resource | Size | SHA-256 | Expected graph count | Core stress case |
| --- | --- | ---: | --- | ---: | --- |
| `bench_01_mz71_screenshot_page` | `bench_01_mz71_screenshot_page.jpg` | 964x1280, 128010 bytes | `8EB64774D29F93C1CCB3D4F9035C96F9075121551600B2814BA7966021ECFD01` | 2 | Printed page photo with two Ion 217/218 chromatograms and page metadata. |
| `bench_02_mz92_belyi_tigr` | `bench_02_mz92_belyi_tigr.jpg` | 576x1280, 57090 bytes | `D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE` | 1 | Existing Belyi Tigr `m/z 92` reference case and report-depth target. |
| `bench_03_small_tic_export` | `bench_03_small_tic_export.jpg` | 381x132, 6682 bytes | `C36A405C937741C6DE834AD9FD3196E658CE42ECBA03AFBBF1D2B47D00437DF4` | 1 | Small low-resolution clean TIC export with visible labeled peaks. |
| `bench_04_stacked_xic_resolution` | `bench_04_stacked_xic_resolution.png` | 534x1110, 165615 bytes | `27D998C2ACA33B12DC3700BFCC88FC3EFB15FAF2A3FE51317AFD220F0E2C3C25` | 4 | Four stacked XIC traces with shared retention-time axis and different mass windows. |
| `bench_05_tic_plus_ions` | `bench_05_tic_plus_ions.png` | 683x807, 162230 bytes | `3F715862316D9D24394F18CC21DFA4C42D6BAC694DD4E1DD8F743BF94984E1F9` | 4 | TIC plus three ion traces with different intensity scales. |
| `bench_06_photo_two_graphs_page` | `bench_06_photo_two_graphs_page.jpg` | 964x1280, 106950 bytes | `04B95E7D8B992FE31708AEAB46E6582329AF1F48B54DB300EF4B664A4EDCB090` | 2 | Printed page photo with perspective distortion and two graph panels. |
| `bench_07_rotated_page_photo` | `bench_07_rotated_page_photo.jpg` | 1280x964, 103875 bytes | `83B60C45E6D9C66BAC5B60A150EE9B283D9BC07E6B5EF4FDF8BD7A107EC3A105` | 1 | Rotated printed page photo; orientation and deskew must be solved before analysis. |
| `bench_08_mz71_duplicate_candidate` | `bench_08_mz71_duplicate_candidate.jpg` | 576x1280, 70617 bytes | `C1EF5E8BC1BD3BB3F9921A9A102FE8B3BADB7ED6BF9EC5E49DFD6EE54D15E0C6` | 1 | Similar `m/z 71` screenshot candidate; tests duplicate/near-duplicate handling and crop consistency. |

## Expected Facts

These facts are allowed to guide analysis QA, but they are not final numeric answers.
Peak counts, areas, FWHM, baseline, S/N, and compound assignments must come from later
validated extraction and calculation stages.

| ID | Expected visible metadata | Required preparation/audit behavior |
| --- | --- | --- |
| `bench_01_mz71_screenshot_page` | Top graph is visible as `Ion 217.00: 0301002.D`; lower graph is visible as `Ion 218.00: 0301002.D`; X axis is time, Y axis is abundance. | Split the two graph panels in top-to-bottom order and reject surrounding page metadata. |
| `bench_02_mz92_belyi_tigr` | `Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\data.ms`; X axis is time, Y axis is abundance. | Preserve the existing dominant-peak discrepancy warning contract from the Belyi Tigr fixture. |
| `bench_03_small_tic_export` | `TIC Scan CK-1.D`; acquisition time in minutes; labeled peaks around 3.244, 3.890, 4.647, 5.610, and 8.560. | Handle low-resolution graph export without treating labels as signal peaks. |
| `bench_04_stacked_xic_resolution` | Four XIC traces for `198,0315` with mass windows `0,2`, `0,02`, `0,002`, and `0,0002 Da`; time is in seconds. | Split all four panels and keep each trace's intensity scale separate. |
| `bench_05_tic_plus_ions` | `TIC: NERPA1.D`; ion traces `326.00`, `360.00`, and `394.00`; intensity labels in Russian. | Split TIC and ion panels, preserve per-graph title/ION, and avoid leaking peaks across panels. |
| `bench_06_photo_two_graphs_page` | Top graph appears to be `Ion 83.00`; lower graph appears to be `Ion 92.00`; sample context is printed above the graphs. | Correct document/page perspective enough to analyze two graph panels in top-to-bottom order. |
| `bench_07_rotated_page_photo` | Rotated page with a visible `Ion 71.00` chromatogram. | Detect and correct orientation before graph splitting; ignore non-graph page context. |
| `bench_08_mz71_duplicate_candidate` | Similar visible `Ion 71.00` page/screenshot context to `bench_01`. | Keep as a separate fixture until near-duplicate detection is implemented and measured. |

## Current Executable Gate

The first executable gate is:

```text
composeApp/src/desktopTest/kotlin/com/chromalab/feature/processing/fixtures/ChromatogramBenchFixtureTest.kt
```

It verifies that all eight fixture resources remain readable and match the expected
dimensions, byte sizes, SHA-256 hashes, expected graph counts, and fixture tags. It does
not validate calculation yet.

Phase 1.1 added `OfflineAnalysisRunner` coverage to the same test. The runner now
executes the platform processing entry points for every fixture and records an honest
stage audit. On desktop, OCR, axis detection, and curve extraction still depend on actual
implementations that are incomplete or stubbed, so the current runner output is a
diagnostic baseline rather than a successful full analysis.

Phase 1.2 adds local debug artifacts for every bench run:

- `audit.json` - full structured stage audit;
- `audit_summary.md` - compact human-readable stage summary;
- `graph_candidates.png` - source image with candidate and selected graph rectangles.

These artifacts are generated in the test run's temporary output directory. They are not
committed as static golden files yet because graph detection is still being calibrated.

Phase 1.3 adds real desktop graph-region detection. Phase 1.4 adds stacked/multi-panel
axis-panel detection. Current graph-count calibration:

- passing and strict: all eight bench fixtures.

Phase 2.1 adds per-graph preprocessing variant ranking. The audit now records the
ranked source/grayscale/contrast/sharpened/scan-style/binary/morphology candidates,
their image metrics, the selected variant, and the image path passed into OCR, axis,
and curve-mask stages. On desktop the variants are still generated as source copies
until the desktop preprocessor is upgraded; on Android the ranking uses the real
scanner/preprocessing outputs.

Phase 2.2 replaces the desktop source-copy preprocessing stub with the same shared
deterministic preprocessing math used by Android. Fixture runs also write
`selected_preprocessing_graph_N.png` for each detected graph, showing the exact selected
prepared crop that was routed into OCR, axis, and curve-mask stages.

Phase 2.3 adds a crop-quality gate to each per-graph audit. The gate records crop area,
edge contacts, full-image fallback, broad edge-touching crops, and whether the crop is
safe to use for later calculation. Large page/screenshot fallbacks and broad edge crops
are now explicit blockers instead of silently flowing into deterministic calculation.

Phase 2.4 adds graph-region refinement before OCR, axis detection, curve masking, and
curve extraction. The audit now records original versus refined crop bounds, whether
the crop changed, the area reduction, and refinement warnings. Broad printed-page or
screenshot crops are tightened conservatively, while rotated/landscape page risk stays
an explicit calculation blocker instead of being treated as a successful crop.

Phase 2.5 adds executable crop-bound contracts for stable clean fixtures and strengthens
the quality gate for photographed pages. If a broad printed-page crop is only improved
by edge trimming, it remains blocked with `crop.refinement_not_precise_for_broad_context`
instead of being allowed into calculation. This keeps the pipeline honest until a real
page/plot-bound detector is implemented for hard photographed pages.

Phase 2.6 adds two hard safety diagnostics. Rotated landscape page crops now expose
`crop.right_angle_rotation_required_before_analysis`, and crops whose upper boundary
may cut vertical peak tops expose `crop.signal_touches_top_edge_possible_clipped_peaks`.
Calculation readiness now requires both crop-quality acceptance and crop-boundary
safety, so plausible-looking rectangles cannot silently drop first peaks.

Phase 2.7 adds the first active fix for those diagnostics. The shared pipeline now
corrects right-angle page orientation after EXIF normalization and before graph
detection, with Android and desktop rotators using the same common contract. The
rotated page fixture is analyzed as `964x1280` after correction. A graph-boundary
corrector then checks the selected ROI against horizontal-axis and left-axis evidence
and expands the crop only when it recovers significant lost top/left content. On
rotated photographed pages it preserves the left panel-label band as part of the graph
block, so the output region represents the visible graph panel rather than only the
inner plot area. This protects early peaks and graph context when a tall peak is
mistaken for the Y axis.

Phase 2.8.1 extends the same graph-panel boundary mode to broad photographed page
fixtures. When any selected graph candidate looks like a large page/photo crop, the
whole run preserves panel labels and uses wider axis-search margins, larger gap
closing for faint photographed axes, and vertical signal-top recovery. This prevents
the current hard fixtures from silently clipping early/tall peaks. The remaining
`bench_01` and `bench_06` broad-context panels are still blocked before calculation
because the next detector must separate page metadata from the final graph panel and
then derive audited plot-area bounds inside that panel.

Phase 2.8.2 tightens those photographed-page graph-panel bounds. The boundary corrector
now ignores short page-metadata strokes when recovering the signal top, avoids selecting
the physical paper edge as the X axis, searches farther left when the visible X-axis
segment starts mid-graph, expands the right panel edge through connected graph evidence
instead of jumping to the page edge, and prevents broad page candidates from expanding
above their original top. `bench_01` and `bench_06` now have executable crop-bound
contracts for every graph, and their crop-quality/crop-boundary gates pass. They remain
blocked honestly at `curve_extract` until plot-area extraction is audited.

Phase 2.9 adds a separate audited plot-area detector inside each accepted graph panel.
The runner now records `plot_area` as its own stage, writes the detected inner bounds to
`audit.json`, renders them in `audit_summary.md`, and overlays them in orange on
`graph_candidates.png`. OCR still receives the full graph panel so titles, ION labels,
axis captions, and tick values are preserved, while axis/curve stages receive the inner
plot area. All eight bench fixtures now pass the plot-area gate; desktop calculation is
still blocked honestly at `curve_extract` until real desktop curve masks are available.
If a future difficult image cannot produce reliable plot-area bounds, the application
should expose a diagnostic/manual adjustment path instead of saving a partial final
report.

Phase 2.10.1 replaces the desktop curve-mask stub with deterministic mask generation
against the audited plot area. Every bench graph now writes `graph_N/mask_raw.png` and
`graph_N/mask_clean.png`, records raw/clean pixel counts and suppression passes in the
audit, and must keep a non-empty cleaned mask before the pipeline can continue. This is
still not final curve-point extraction: some low-resolution and photographed fixtures
retain text/background artifacts in the mask, so the runner remains blocked honestly at
`curve_extract` until the point extractor and stricter mask QA are implemented.

Phase 2.10.2 adds desktop curve-point extraction from `mask_clean.png`. The extractor
keeps narrow/tall chromatogram peaks by taking the top of the strongest per-column ink
cluster and only interpolates short gaps. Every bench graph now writes
`graph_N/curve_overlay.png` and records non-zero point counts and coverage. Calculation
readiness was also tightened: extracted curve points are not enough for a scientific
result, so the runner stays blocked at `axis_ocr`/`axis_detect` until axis labels,
axis geometry, and origin are available.

Phase 2.11.1 replaces the desktop axis detector stub with real geometry detection
inside each audited plot area. The detector finds long dark X/Y-axis runs, records an
origin at their intersection, exposes axis confidence in `audit.json` and
`audit_summary.md`, and requires every bench graph to pass the axis-geometry gate. This
does not unlock numeric calculation: OCR/manual scale calibration is still required
before time and intensity values can be trusted.

Phase 2.11.2 adds an explicit scale-calibration contract to the offline runner. The
runner now emits an `axis_calibration` stage per graph, records candidate
pixel-to-value points, value spans, units, source state, and warnings, and blocks at
`axis_calibration` until both axes have confirmed calibration. Desktop OCR is still not
implemented, so the bench currently records `MANUAL_REQUIRED` instead of pretending
that pixel coordinates can be converted into time/intensity units.

Phase 2.11.3 adds confirmed manual calibration input to the same contract. Bench runs
can now pass per-graph X/Y pixel-to-value points through `OfflineAnalysisInput`, and
the runner records them as `MANUAL_CONFIRMED` only when both axes have sufficient
non-zero spans. Without those points the bench stays blocked at `axis_calibration`;
with confirmed points the scale gate passes and later calculation stages can be tested
without relying on unavailable desktop OCR.

Phase 2.11.4 removes the user-flow identity calibration fallback. The processing screen
now blocks signal conversion, preview, and export when X/Y calibration is missing or
invalid instead of fabricating `px` units. Calibration errors are treated as
non-skippable full-analysis blockers, matching the offline `axis_calibration` gate.

Phase 2.11.5 wires that contract into the user-facing flow. If OCR cannot produce
confirmed X/Y scale points, the pipeline pauses on manual X/Y calibration screens
instead of saving a partial result or fabricating `px` units. Manual calibration skip is
disabled in the full-analysis path, and touch positions are converted from the displayed
full image back into graph-region-relative pixels, matching the curve extractor's
coordinate system.

Phase 2.11.6.1 improves that manual gate with OCR tick anchors. When raw OCR elements
contain usable X/Y tick labels, the manual calibration screens prefill their two
calibration points from the detected tick positions and draw subtle anchor marks on the
axis overlay. If no reliable anchors exist, the screens still fall back to manual point
placement without fabricating pixel-unit calibration.

Phase 2.11.6.2 makes the manual calibration review focus on the selected graph panel
instead of the full source page. The X/Y calibration screens can now zoom the displayed
image to the graph region while preserving graph-region-relative calibration math, so
touch points, OCR tick anchors, and preview ticks stay aligned with the curve extraction
coordinate system.

Phase 2.11.6.3 makes OCR tick anchors editable in the manual calibration screens. The
screens now expose compact P1/P2 selectors and detected tick chips; choosing a chip
fills the active calibration point while keeping drag/tap manual correction available.
This keeps OCR assistance auditable instead of automatically accepting weak tick reads.

Phase 2.12.1 adds fixture artifacts for manual calibration focus review. Each bench run
now writes `manual_calibration_graph_N.png` beside the existing graph overlay and curve
artifacts, showing the focused graph-panel crop with plot-area and axis guide overlays.
The fixture test requires this artifact for every detected graph, so manual calibration
visual QA cannot disappear silently.

Phase 2.12.2 locks acceptance checks for those focus artifacts. The fixture test now
verifies that each artifact is readable, matches the graph-panel crop dimensions, does
not show the full source page when a graph crop exists, keeps the plot area inside the
focused panel, preserves graph-panel context outside the plot area, and keeps the
plot-area ratio within a sane range.

Phase 5.1 adds the calibrated curve-to-signal gate. The offline runner now emits a
`signal_convert` stage after curve extraction. Without confirmed X/Y calibration, the
stage is skipped with an explicit `signal_convert.axis_calibration_required` warning.
With confirmed manual calibration, the runner converts curve points through
`SignalConverter`, records point count, time range, intensity range, duplicates, gaps,
and sort validity, and requires this signal audit before later calculation work.

Phase 5.2 adds the audited peak-detection readiness gate. The runner now emits
`peak_detection` only after calibrated signal conversion, runs the deterministic
`CalculationEngine`, and records peak count, significant peak count, dominant peak
time/height/area share, boundary method, integration mode, clamp-negative, max-width,
min-S/N, and warnings. Missing signal data produces `peak_detection.signal_required`
instead of a partial result. The executable fixture test validates this gate on
`bench_03_small_tic_export`, `bench_06_photo_two_graphs_page`, and
`bench_07_rotated_page_photo`, covering the best clean graph, a hard two-graph page,
and a rotated page.

Phase 5.3 adds the audited peak-metrics and integration review gate. After
`peak_detection`, the runner now emits `peak_metrics` from the real `CalculationRun`
and records retention-time ordering, total area, area percent sum, boundary width
range, invalid numeric/boundary counts, non-positive area/height counts, missing
width count, low S/N count, low-confidence count, overlap-review count, and peak
warnings. Structural metric failures block calculation readiness at `peak_metrics`.
The fixture test validates this on the same clean, two-graph, and rotated real
examples before those values can feed report validation.

Phase 5.4 adds per-peak audit rows and visual peak overlay artifacts. The JSON and
Markdown audit now expose each detected peak's apex time, boundaries, height, area,
area percent, S/N, confidence, overlap state, and warning count. Calibrated fixture
runs also write `peak_overlay_graph_N.png`, drawn on the manual calibration focus
image with apex and left/right integration boundary markers. The fixture test validates
these overlays on the clean `bench_03`, two-graph `bench_06`, and rotated `bench_07`
examples.

Phase 5.5 adds fixture-specific peak sanity expectations. The offline runner now emits
`peak_sanity` after peak metrics and can require minimum peak counts plus expected apex
retention times with tolerance. `bench_06` and `bench_07` pass the new minimum-peak
sanity gate. Phase 5.6 then recovers the `bench_03` labeled apexes at 3.244, 3.890,
4.647, 5.610, and 8.560 min by suppressing floating text components on compact
low-resolution plot masks while preserving usable coverage on the photographed
two-graph and rotated fixtures.

Phase 5.7 suppresses right-frame line artifacts on photographed plots. The curve-mask
preparer now removes narrow, tall components at the right plot edge only when retained
column coverage stays usable. The fixture test verifies this on `bench_06` and
`bench_07` so right-border lines cannot re-enter the peak table as late false peaks.

Phase 5.8a adds peak-candidate diagnostics without changing accepted peak behavior.
`audit.json` and `audit_summary.md` now expose the detection signal source, noise
method/value, total candidates, rejected candidates, and top rejection reasons. The
current `bench_06` calibrated run shows that graph 1 has many visible candidates but
most are rejected by the prominence/noise gate; a stricter noise experiment also showed
that graph 2 can turn bleed-through artifacts into false peaks, so artifact review must
come before any threshold loosening.

Phase 5.8b.1 records the external pixel-detection research in
`docs/CHROMATOGRAM_PIXEL_DETECTION_RESEARCH.md`. The accepted direction is to borrow
architecture from plot digitizers and chromatogram feature detectors, but implement the
core locally: trace evidence first, artifact masks second, and peak-threshold tuning
only after artifact-heavy graphs are protected.

Phase 5.8b.2 adds the first non-destructive artifact audit. Each desktop curve-mask
run now writes `graph_N/trace_artifacts.png` and records `traceArtifactAudit` in JSON.
Gray pixels show the current cleaned mask; red pixels mark internal artifact-risk
components. On calibrated `bench_06`, graph 1 is mainly a top-band/text risk while graph
2 is flagged as high internal artifact risk, which matches the observed false-positive
risk when thresholds are loosened.

Phase 5.8b.3 adds the guard layer before threshold tuning. Each desktop bench graph now
also writes `graph_N/trace_artifact_suppressed_mask.png`, records the cleanup hypothesis
retained ratio and column coverage, and exposes whether later threshold relaxation is
allowed. `bench_06` graph 2 is explicitly blocked from threshold relaxation because its
internal artifact risk is high; graph 1 remains eligible for later controlled review.
Accepted peaks still come from the current clean mask in this phase.

Phase 5.8b.4 applies the first guarded completeness tuning. The offline peak audit now
records the detection profile, base peak count, tuned peak count, controlled tuning
state, and tuning reason. The tuning pass is selected only for clearly under-detected
graphs with `thresholdRelaxationAllowed=true`. On calibrated `bench_06`, graph 1 moves
from 2 to 14 accepted peaks, graph 2 remains blocked by artifact risk, and the rotated
fixture remains on the default profile because it was not under-detected.

Phase 5.8b.5 adds per-peak quality controls for guarded tuning. The audit now records
`guardedQualityReview`, peak width, and per-peak `qualityFlags`. A guarded run is
rejected if too many peaks are below the default S/N reference, low area share, or very
narrow. The current calibrated `bench_06` graph 1 guarded table keeps 14 peaks with 3
lower-than-default S/N review flags and no low-area or narrow-boundary flags.

Phase 5.8b.6 broadens the executable guarded-quality contract to additional hard
fixtures. `bench_08_mz71_duplicate_candidate` now exercises guarded completeness beyond
`bench_06`: it moves from 5 base peaks to 9 reviewed peaks, with one lower-than-default
S/N flag and no low-area or narrow-boundary flags. `bench_01` remains blocked from
threshold relaxation by trace artifacts, while `bench_02` remains default because its
base table is already above the under-detection gate. `bench_04` and `bench_05` are not
yet eligible for guarded peak tuning because weak stacked panels still block earlier at
curve/signal extraction.

Phase 5.8b.7 restores signal conversion for sparse stacked ion panels without changing
peak thresholds. Sparse XIC/ion traces can now pass the curve-usability gate when they
contain enough extracted points, while the audit records low-column-coverage acceptance
and localized sparse evidence. `bench_04` graphs 3-4 and `bench_05` graphs 2-4 now reach
signal conversion and peak detection instead of stopping at `signal_convert.curve_points_required`.

Phase 5.8b.8 adds sparse stacked-ion peak quality review before report validation.
Sparse graph peak tables now carry a separate `sparseTraceQualityReview`, report
confidence warnings, and per-peak sparse flags without changing default S/N thresholds
or enabling guarded completeness tuning. `bench_04` sparse graphs review `4` and `1`
peaks; `bench_05` sparse graphs review `4`, `9`, and `4` peaks, with low-area and
overlap flags where the recovered weak ion traces are most ambiguous.

Phase 6.1 adds structured report-contract validation to each calibrated offline audit.
The audit now records `report_validation` and a `reportContract` section matrix for
overview, preparation, axis calibration, peak table, graph rendering, chromatographic
quality, Kovats, interpretation, warnings, and appendix.

Phase 6.2 fills the peak-table contract fields that are already available from
calculation output. Offline peak rows now include FWHM, USP tailing factor, EP
asymmetry factor, and explicit compound/Kovats `NOT_CALCULATED` statuses when local
assignment evidence is absent. Calculation-ready fixture reports no longer block on
peak-table structure; missing chemistry remains a warning and future interpretation
task.

Phase 6.3 adds the first calibrated report artifact for fixture runs:

- `calibrated_report.md` - graph-first report artifact rendered from the structured
  offline audit contract.

The artifact includes overview, human-readable warnings, per-graph preparation, axis
calibration, peak table, chromatographic quality, Kovats/interpretation, section
readiness, and a technical appendix. The main report surface keeps raw warning codes
out of user-facing text; raw codes remain in the appendix for audit/debug work.
Executable checks require the artifact to be written, preserve graph/report order, keep
full peak-table columns, show missing chemistry as not calculated, and keep sparse
trace warning codes in the appendix. This is still a contract artifact, not the final
mobile report UI.

Phase 6.4 connects that report artifact to generated visual evidence. Each per-graph
report section now includes a `Visual Evidence` table with stable artifact paths for:

- `graph_candidates.png`;
- `selected_preprocessing_graph_N.png`;
- `manual_calibration_graph_N.png`;
- `graph_N/curve_overlay.png`;
- `peak_overlay_graph_N.png` when peak metrics are available;
- trace-artifact masks for the technical appendix.

The fixture tests verify that the referenced artifacts exist for calibrated reports.
The table also records whether each artifact belongs near the main report section or
the technical appendix, which is the first bridge toward the future mobile/export UI
without treating raw Markdown as the final phone presentation.

Phase 6.5 adds the first explicit mobile/export UI contract artifact:

- `calibrated_report_ui_contract.json` - structured contract for rendering the
  calibrated report without parsing `calibrated_report.md` as the phone UI.

The contract records:

- `rawMarkdownIsFinalUi=false`;
- main report sections versus technical appendix sections;
- graph/report order;
- visual evidence placement near preparation, axis calibration, rendered graph, and
  peak table sections;
- trace-artifact masks as appendix-only evidence;
- export artifacts and whether they are user-facing.

Fixture tests parse the JSON contract and verify that main report sections do not expose
raw warning codes, that the appendix does, and that generated visual evidence artifacts
exist when the contract marks them as generated.

Phase 6.6 connects the same idea to the real report/export surface:

- real calculation exports now include `chromatogram_report_ui_contract.json`;
- the final HTML report is rendered directly from the structured UI contract rather
  than by parsing raw Markdown;
- the Compose report preview consumes the UI contract for graph order and visual
  evidence placement;
- graph focus, curve overlay, and peak overlay markers appear next to the report
  sections they support;
- raw warning codes and export manifests stay inside the technical appendix path.

Phase 7.4 connects the offline scientific knowledge pack to report interpretation:

- built-in coverage now includes GC-MS TIC, EIC, XIC, and SIM modes;
- current fixture/reference channels include `m/z 57`, `71`, `83`, `91`, `92`, `191`,
  `217`, `218`, `198.0315`, `326`, `360`, and `394`;
- report interpretation can show local-knowledge class hypotheses with provenance and
  assignment cautions;
- Kovats reports may show the formula and local n-paraffin RI scale, but measured
  reference retention times are not invented and Kovats values remain not calculated
  until same-method references are supplied.

Phase 8.1 defines the strict model-assisted stage contract:

- graph-region and title/ION/axis extraction are required VLM contract stages for full
  photo chromatogram analysis;
- axis-structure model output is an optional hint;
- chemical interpretation stays local-knowledge/model-suggested and cannot become a
  final compound assignment by itself;
- numeric peak data remains deterministic-only;
- GGUF VLM analysis requires base + `mmproj`, and OCR/document-only model families are
  excluded from strict chromatogram VLM selection.

Phase 8.2 routes model-stage outcomes into saved report metadata:

- strict VLM contract timings can appear as `model.graph_region` and
  `model.title_ion_axis`;
- selected/executed model metadata, runtime, backend, device, and timings stay in the
  same evidence chain used by report export;
- if full photo analysis has no executed vision runtime, required VLM stages are
  stored as `FAILED` report warnings instead of being hidden in logs.

Phase 8.3a validates saved-report propagation:

- processing metadata is saved through `ChromatogramEntity.algorithmConfig` and read
  back by `buildCalculationReportOptions`;
- final report mapping preserves selected model metadata, device name, stage timings,
  and failed required model-stage warnings;
- a selected GGUF/VLM with no executed runtime remains visibly blocked in the report
  through failed model-stage warnings plus `runtime.executed_unknown`.

Phase 8.3b.1 prepares Android/device validation:

- each user-flow chromatogram auto-save now emits a compact `PIPELINE[REPORT_AUDIT]`
  logcat line;
- the marker includes selected/executed model, runtime, device, timings, and warning
  codes so LiteRT/GGUF device runs can be checked without manually decoding Room data.

Phase 8.3c.5c.16 adds audit-only fragment reconstruction evidence:

- fixture runs now write `graph_N/centerline_fragment_reconstruction_overlay.png`;
- `curveCenterline` records retained/discarded skeleton components, raw/interpolated
  reconstructed columns, coverage, P95, large-delta, and decision metrics;
- the candidate remains `selectedForSignal=false` because clean and hard-photo CLI
  checks still do not satisfy visual/P95/large-delta acceptance.

Phase 8.3c.5c.17 adds signal-guided guard tuning:

- fragment reconstruction now receives the preserved trace as a guide corridor;
- retained skeleton components and short-gap interpolation are rejected when they
  drift outside the guide distance;
- `curveCenterline` records guide columns, guide distance, guide-matched pixels,
  rejected pixels, and rejected interpolated columns;
- clean, two-graph, and rotated CLI runs confirm the tuned candidate is more
  constrained, but hard fixtures still have enough large-delta columns that
  `selectedForSignal=false` remains required.

Phase 8.3c.5c.18 adds reconstructed-trace residual taxonomy:

- every fragment-reconstruction large-delta column is classified as peak-top
  candidate, branch/edge ambiguity, baseline gap, frame/text artifact, crop
  boundary, signal-guide mismatch, or unclassified;
- `curveCenterline` records the residual acceptance gate and per-class counts;
- `centerline_fragment_reconstruction_overlay.png` colors residual classes for
  visual review;
- clean, two-graph, and rotated CLI runs confirm residual counts sum exactly to
  large-delta counts, and the candidate remains `selectedForSignal=false`.

## Next Phase

Phase 8.3c.5c.19 should reduce branch/edge residual ambiguity before any centerline
or reconstructed path is allowed to drive signal conversion. Android model-assisted
report-audit validation remains a separate device parity track.
