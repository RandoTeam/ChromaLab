# Phase 9E Visual Diagnostic Atlas

Initial source artifacts: `artifacts/phase9d-final-multi-fixture-android/`

Final Phase 9E rerun artifacts: `artifacts/phase9e-multi-fixture-android-final2/`

The atlas was built before code repair. It references local diagnostic artifacts only; bulky images are not committed into docs.

| Fixture | Mode inspected | Normalized/evidence root | Expected graphs | Detected report/failure graphs | Selected evidence | Tick/OCR/calibration evidence | Human layout | Phase 9E diagnosis |
| --- | --- | --- | ---: | ---: | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | deterministic | `artifacts/phase9d-final-multi-fixture-android/bench_01_mz71_screenshot_page/deterministic/bench_01_mz71_screenshot_page_20260521_071632/` | 2 | 1 failure package | graphPanel and plotArea present | X ticks 8, Y ticks 8, accepted X anchors 2, accepted Y anchors 0, X/Y calibration invalid | printed page with two chromatograms | Tick pixels exist, but OCR crop text is missing/weak; graph count is still under-detected for the two-panel page. |
| `bench_02_mz92_belyi_tigr` | deterministic | `artifacts/phase9d-final-multi-fixture-android/bench_02_mz92_belyi_tigr/deterministic/bench_02_mz92_belyi_tigr_20260521_071817/` | 1 | 1 report graph, metadata count 2 | graph package present | graph result diagnostic, multiplicity review with duplicate/nested candidates | one physical graph | Over-splitting remains a layout-semantics issue; dense same-axis candidates should collapse to one physical graph. |
| `bench_04_stacked_xic_resolution` | deterministic | `artifacts/phase9d-final-multi-fixture-android/bench_04_stacked_xic_resolution/deterministic/bench_04_stacked_xic_resolution_20260521_072112/` | 4 | 1 failure package | graphPanel and plotArea present | X ticks 3, Y ticks 2, accepted X anchors 1, accepted Y anchors 0, calibration invalid | four stacked XIC panels | Panel generation misses stacked graph units; tick OCR lacks numeric anchors for the selected panel. |
| `bench_05_tic_plus_ions` | deterministic | `artifacts/phase9d-final-multi-fixture-android/bench_05_tic_plus_ions/deterministic/bench_05_tic_plus_ions_20260521_072207/` | 4 | 1 failure package | graphPanel and plotArea present | X ticks 12, Y ticks 9, accepted X anchors 8, accepted Y anchors 2, X valid, Y review | TIC plus ion panels | OCR pairing is contaminated and panel semantics are incomplete; Y calibration remains non-release. |
| `bench_06_photo_two_graphs_page` | deterministic | `artifacts/phase9d-final-multi-fixture-android/bench_06_photo_two_graphs_page/deterministic/bench_06_photo_two_graphs_page_20260521_072301/` | 2 | 1 failure package | graphPanel and plotArea present | X ticks 10, Y ticks 12, accepted X anchors 2, accepted Y anchors 0, X review, Y invalid | photographed two-graph page | OCR reads unrelated/merged page text; graph count remains under-detected. |

## Artifact Gaps

- Phase 9D exported graph failure packages, validator output, reports, and manifests for all 16 runs.
- Pulled public artifacts include overlays for successful runs, but terminal failure manifests still often record overlay slots as missing with explicit reasons.
- Phase 9E keeps missing overlays visible in summary output rather than treating app-private paths as complete public evidence.

## Final Rerun Addendum

The final Phase 9E Android suite exported all 16 deterministic/model-enabled runs, but 12 remained `BLOCKED`. The visual/evidence atlas now identifies these as inspectable runtime failures rather than evidence outages:

- `white_tiger_ion71`: `DENSE_PEAK_SINGLE_AXIS`, one detected graph, blocked by insufficient X/Y anchors and OCR no numeric text.
- `bench_01_mz71_screenshot_page`: one detected graph against expected two; X anchors exist but Y anchors are missing and values are non-monotonic/high-residual.
- `bench_02_mz92_belyi_tigr`: graph count fixed to one, but tick marks/OCR numeric anchors are missing.
- `bench_04_stacked_xic_resolution`: stacked expected count still under-detected as one panel and calibration anchors are insufficient.
- `bench_05_tic_plus_ions`: TIC-plus-ion expected count still under-detected as one panel; plot frame/tick marks are missing in the selected panel evidence.
- `bench_06_photo_two_graphs_page`: candidate metadata sees two graph candidates, but only one report graph is produced, so it remains diagnostic/review rather than accepted.

## Repair Implications

- Tick failures are no longer treated as generic `TICK_LOCALIZATION_FAILURE`; they must carry subreasons such as `OCR_NO_NUMERIC_TEXT`, `INSUFFICIENT_Y_ANCHORS`, or `HIGH_RESIDUALS`.
- Layout fixes can collapse same-axis pseudo-panels for `bench_02`, but cannot create missing stacked/two-page panels without upstream graphPanel generation improvements.
