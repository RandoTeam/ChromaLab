# Phase 9D Tick Localization / Calibration Repair

## Changes Tested

- Android OCR crop reading now upscales small tick crops before ML Kit OCR.
- OCR crop candidate ranking now adds axis-distance plus cross-axis bias, preferring text in the expected local label area without making OCR numeric authority.

An additional hard rejection of out-of-band OCR text was tested and rejected because it regressed `bench_07_rotated_page_photo` from REVIEW to BLOCKED. That rejection was removed before the final suite.

## Final Android Result

Tick localization remains the primary runtime blocker:

| Fixture | Modes Affected | Final Failure |
| --- | --- | --- |
| `bench_01_mz71_screenshot_page` | deterministic, E2B | `TICK_LOCALIZATION_FAILURE` |
| `bench_04_stacked_xic_resolution` | deterministic, E2B | `TICK_LOCALIZATION_FAILURE` |
| `bench_05_tic_plus_ions` | deterministic, E2B | `TICK_LOCALIZATION_FAILURE` |
| `bench_06_photo_two_graphs_page` | deterministic, E2B | `TICK_LOCALIZATION_FAILURE` |

## Required Next Repair

- Build a deterministic tick-pixel candidate table independent of OCR.
- Pair OCR text only to deterministic tick pixels.
- Add label-band detection for each physical panel.
- Export accepted/rejected anchors and residuals for every attempted graph.

No VLM pixel coordinates or fabricated tick labels were used.
