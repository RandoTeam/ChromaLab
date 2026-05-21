# Phase 9F Tick-Only Failure Audit

Phase 9E showed that explicit tick marks were an insufficient calibration gate for real Android screenshots/photos. Phase 9F audited the final Phase 9E artifacts under `artifacts/phase9e-multi-fixture-android-final2/` and reran the suite after adding axis-scale evidence.

## Findings

| Fixture | Numeric labels visible/OCR evidence | Grid/frame evidence | Explicit tick evidence | Tick-only failure mode | Phase 9F action |
| --- | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | X labels visible; Y labels were not reliably collected after broad plotArea selection. | Frame exists but plotArea expands too far. | Weak/tiny. | Requiring explicit tick/Y anchors blocks calibration. | Added OCR-label-box evidence and wider Y label band, but final run still has 0 Y anchors. |
| `bench_01_mz71_screenshot_page` | X labels collected; Y labels not collected. Title/ion text was present. | Partial frame. | Some X tick support. | Bad semantic text and missing Y anchors block calibration. | Added rejection of title/mass-range text as scale labels. |
| `bench_02_mz92_belyi_tigr` | OCR label evidence exists but does not form accepted anchor sets. | Frame evidence incomplete. | Missing. | Tick marks missing and OCR sequence insufficient. | Resolver records precise scale subreasons instead of generic failure. |
| `bench_04_stacked_xic_resolution` | Enough evidence after Phase 9F to reach report generation. | Layout evidence now reaches review report. | Not decisive. | Previously blocked due tick-only path. | Improved to `REVIEW_ONLY` in both modes. |
| `bench_05_tic_plus_ions` | Numeric labels remain unavailable/unusable. | Plot frame missing/inconsistent. | Missing. | Cannot resolve scale because graph/plot frame is not trustworthy. | Remains blocking with `AXIS_FRAME_INCONSISTENT`. |
| `bench_06_photo_two_graphs_page` | Evidence sufficient for review report. | Multi-graph page evidence complete enough for report. | Not decisive. | Previously blocked by tick-only path. | Improved to `REVIEW_ONLY` in both modes. |

## Root Cause

The old gate treated explicit tick localization as a prerequisite. In real screenshots/photos, tick marks can be cropped, blurred, hidden by frame/grid lines, or absent. Phase 9F moved calibration evidence toward a resolver model that can accept OCR label boxes and label projections as review-grade scale evidence while still rejecting semantic-only or non-axis text.

## Remaining Gap

Phase 9F does not fully solve cases where no usable Y label geometry is collected or where the plot frame itself is inconsistent. Those failures remain correctly blocked.
