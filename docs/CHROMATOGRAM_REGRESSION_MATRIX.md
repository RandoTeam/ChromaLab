# ChromaLab Regression Matrix

Phase 0 matrix for future phases. Each phase must update this file when behavior changes.

| Case | Expected graph count | Expected mode behavior | Release report allowed? | Required evidence artifacts | Expected diagnostic if not solvable | Previous bugs / current status |
| --- | ---: | --- | --- | --- | --- | --- |
| Clean screenshot | 1 | `AUTO_DIAGNOSTIC` may become release-ready only if all gates pass. | Yes, only with valid graphPanel, plotArea, X/Y calibration, trace, evidence package. | original, normalized, graphPanel, plotArea, axes, ticks, calibration, masks, trace, peak overlay, report JSON. | Diagnostic if calibration or trace missing. | Baseline fixture class. |
| Android screenshot / embedded graph | 1 | AUTO should detect or diagnose, not wait on VLM. | Only if gates pass. | bright-panel candidates, multiplicity resolution, overlays, timings. | ROI/geometry diagnostic with candidates. | Past ROI failure and right-side crop. |
| Already-cropped white chart panel | 1 | AUTO should not require dark surrounding page. | Only if gates pass. | graphPanel near full panel, plotArea inside, axes/ticks/calibration. | Plot/calibration diagnostic. | Past `no_accepted_bright_panel`. |
| Photo with mild perspective | 1 | AUTO may be REVIEW if rectification/calibration marginal. | Review-only unless geometry/calibration valid. | provenance, rectification status, before/after overlay, residuals. | Perspective/axis review. | Perspective uncertainty. |
| Graph with title/ion text | 1 | Title/ion numbers are metadata, not peak labels. | Yes if plotArea excludes title and calibration valid. | text classification overlay, local OCR crops. | Text contamination review. | `Ion 71.00 (70.70 to 71.70)` misread risk. |
| Graph with labels inside plotArea | 1 | Peak labels may create review candidates only after signal verification. | Normal peaks only if detected; recovered labels are review-grade. | OCR crop, label box, signal window, local max, decision table. | OCR_NOT_READ or NO_LOCAL_SIGNAL. | bench_03 label recovery is test-only unless runtime OCR proves it. |
| Weak/faint peaks | 1 | AUTO may diagnose or review sparse traces. | No if trace confidence weak. | raw/clean mask, rejected components, centerline, trace metrics. | CURVE_FAILURE or review trace. | Sparse trace false confidence risk. |
| Dense peaks | 1 | Count raw, validated, reportable, rejected separately. | Only validated/reportable peaks. | peak overlay with apex alignment and artifact table. | Review if artifact suspicion unresolved. | bench_08 19 validated peaks evidence-backed. |
| Multi-graph real page | actual graph count | AUTO may emit multiple only when panels are spatially distinct. | Per-graph gates. | multiplicity resolver output and per-graph overlays. | MULTI_GRAPH_REVIEW. | bench_04/05/06 classes. |
| One graph producing many ROI candidates | 1 | Duplicates/nested candidates must collapse to one graph. | Only selected graph gates pass. | candidate table, NMS/rejection reasons. | DIAGNOSTIC_ONLY if no full candidate valid. | Past six pseudo-graphs. |
| Missing tick labels | 1 | Calibration cannot be release-ready without valid or confirmed anchors. | No, unless user-confirmed in future guided/manual mode. | tick attempts, OCR crops, rejected anchors. | CALIBRATION_FAILURE. | Axis/tick localization failures. |
| Invalid calibration case | 1 | AUTO must block release-quality report. | No. | accepted/rejected anchors, residuals, validator markdown. | CALIBRATION_FAILURE or DIAGNOSTIC_ONLY. | Invalid X/Y fits in Android package. |

## Required Fixture Pack

- `bench_01` printed two graphs.
- `bench_02` m/z 92 screenshot.
- `bench_03` low-res TIC.
- `bench_04` stacked XIC.
- `bench_05` TIC plus ions.
- `bench_06` photo two graphs.
- `bench_07` rotated page.
- `bench_08` m/z 71 screenshot.
