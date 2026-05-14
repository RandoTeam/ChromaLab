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

## Next Phase

Phase 2.7 should add actual right-angle orientation correction before graph detection,
then implement a photographed-page plot-bound detector that preserves first peaks while
removing page/header context.
