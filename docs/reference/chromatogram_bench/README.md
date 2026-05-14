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
| `bench_01_mz71_screenshot_page` | `bench_01_mz71_screenshot_page.jpg` | 964x1280, 128010 bytes | `8EB64774D29F93C1CCB3D4F9035C96F9075121551600B2814BA7966021ECFD01` | 1 | Phone screenshot/document page context with one `m/z 71` chromatogram. |
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
| `bench_01_mz71_screenshot_page` | `Ion 71.00 (70.70 to 71.70): BELIY TIGR_1.D\data.ms`; X axis is time, Y axis is abundance. | Crop away phone UI, document text, and dark background before graph analysis. |
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
not validate graph-detection accuracy or calculation yet.

Phase 1.1 adds `OfflineAnalysisRunner` coverage to the same test. The runner now
executes the platform processing entry points for every fixture and records an honest
stage audit. On desktop, graph detection, OCR, axis detection, and curve extraction still
depend on desktop actual implementations that are incomplete or stubbed, so the current
runner output is a diagnostic baseline rather than a successful full analysis.

Phase 1.2 adds local debug artifacts for every bench run:

- `audit.json` - full structured stage audit;
- `audit_summary.md` - compact human-readable stage summary;
- `graph_candidates.png` - source image with candidate and selected graph rectangles.

These artifacts are generated in the test run's temporary output directory. They are not
committed as static golden files yet because graph detection is still being calibrated.

## Next Phase

Phase 1 should add an offline runner that can process these fixture images without the
camera or Android document scanner and emit per-stage audit data for each graph.
