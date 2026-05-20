# ChromaLab Regression Matrix

Phase 0 matrix for future phases. Every later phase must update this file when behavior, gates, fixtures, or known Android failures change.

## Baseline Matrix

| Case | Expected graph count | Expected mode behavior | Release report allowed? | Required evidence artifacts | Expected diagnostic if not solvable | Previous bugs / current status |
| --- | ---: | --- | --- | --- | --- | --- |
| Clean screenshot | 1 | `AUTO_DIAGNOSTIC` may become release-ready only if all gates pass. | Yes, only with valid graphPanel, plotArea, X/Y calibration, trace, evidence package, source provenance, and no validator blockers. | Original, normalized, graphPanel, plotArea, axes, ticks, calibration, masks, trace, peak overlay, report JSON, validator JSON/Markdown. | Diagnostic if calibration or trace evidence is missing. | Baseline screenshot class. |
| Android screenshot / embedded graph | 1 | AUTO should detect or diagnose without making VLM mandatory. | Only if gates pass. | Bright-panel candidates, multiplicity resolution, selected/rejected overlays, timings, VLM status. | ROI/geometry diagnostic with candidates. | Past ROI failure and right-side crop. |
| Already-cropped white chart panel | 1 | AUTO should not require a dark surrounding page. | Only if gates pass. | Near-full graphPanel, plotArea inside, axes/ticks/calibration, trace overlay. | Plot/calibration diagnostic. | Past `no_accepted_bright_panel`. |
| Photo with mild perspective | 1 unless page has real multiple panels | AUTO may be `REVIEW_ONLY` if rectification/calibration is marginal. | Review-only unless geometry/calibration evidence is valid. | Source provenance, rectification status, before/after overlay, residuals. | Perspective/axis review. | Perspective uncertainty. |
| Graph with title/ion text | 1 | Title/ion numbers are metadata, not peak labels. | Yes if plotArea excludes title/channel and calibration is valid. | Text classification overlay, local OCR crops, title/channel evidence. | Text contamination review. | `Ion 71.00 (70.70 to 71.70)` misread risk. |
| Graph with labels inside plotArea | 1 | Peak labels may create review candidates only after local signal verification. | Normal detected peaks only if validated; recovered labels are review-grade. | OCR crop, label box, signal window, local max, decision table. | OCR_NOT_READ, LOW_CONFIDENCE_TEXT, or NO_LOCAL_SIGNAL. | bench_03 label recovery remains test-only unless runtime OCR proves it. |
| Weak/faint peaks | 1 | AUTO may diagnose or review sparse traces. | No if trace confidence is weak. | Raw/clean mask, rejected components, centerline, trace metrics. | CURVE_FAILURE or review trace. | Sparse trace false confidence risk. |
| Dense peaks | 1 | Raw, validated, reportable, and rejected peaks must remain separate. | Only validated/reportable peaks. | Peak overlay with apex alignment and artifact table. | Review if artifact suspicion is unresolved. | bench_08 19 validated peaks evidence-backed in current fixture contract. |
| Real multi-graph page | Actual physical graph count | AUTO may emit multiple reports only when panels are spatially distinct. | Per-graph gates. | Multiplicity resolver output and per-graph overlays. | MULTI_GRAPH_REVIEW. | bench_04/05/06 classes. |
| One graph producing many ROI candidates | 1 | Overlapping/nested candidates must collapse to one physical graph. | Only if selected graph gates pass. | Candidate table, NMS/rejection reasons, selected graphPanel/plotArea overlays. | DIAGNOSTIC_ONLY if no full candidate is valid. | Past six pseudo-graphs. |
| Missing tick labels | 1 | Calibration cannot be release-ready without valid or user-confirmed anchors. | No unless future guided/manual mode confirms anchors. | Tick attempts, OCR crops, rejected anchors, calibration status. | CALIBRATION_FAILURE. | Axis/tick localization failures. |
| Invalid calibration case | 1 | AUTO must block release-quality report. | No. | Accepted/rejected anchors, residuals, validator Markdown. | CALIBRATION_FAILURE or DIAGNOSTIC_ONLY. | Invalid X/Y fits in Android package. |

## Required Fixture Pack

- `bench_01` printed two graphs.
- `bench_02` m/z 92 screenshot.
- `bench_03` low-res TIC.
- `bench_04` stacked XIC.
- `bench_05` TIC plus ions.
- `bench_06` photo two graphs.
- `bench_07` rotated page.
- `bench_08` m/z 71 screenshot.

## Regression Rules

- Do not relax fixture tolerances without documented root cause and updated evidence table.
- Do not use fixture hints as production evidence.
- Do not hardcode image-specific coordinates, filenames, run ids, or expected counts.
- Guided/manual user confirmations must be explicit persisted evidence, not inferred from fixture metadata.
- `AUTO_DIAGNOSTIC` must not be upgraded to guided/user-confirmed release evidence.
- After Phase 2 and every later phase, re-check all previous phase guarantees.
- A new real Android failure becomes a regression matrix row and, when possible, a fixture.

## Required Validation Artifacts

For every fixture or real-device case used to close a phase:

- RuntimeEvidencePackage or desktop evidence package.
- Validator JSON.
- Validator Markdown.
- Selected graphPanel overlay.
- Selected plotArea overlay.
- Axis/tick overlay.
- Calibration residual evidence.
- Curve masks and selected trace overlay.
- Peak overlay and raw/validated/reportable/rejected counts.
- Stage timings.
- Final report contract JSON.

## Phase 0 Status

This matrix is accepted as the baseline. It does not imply that every row currently passes release gates; it defines how future work must prove or diagnose each class.

## Phase 1 Addendum

Phase 1 adds shared guided/manual state contracts. No fixture expectations or image-analysis algorithms changed.

Future regression rows should record, when guided/manual workflows are used:

- `GuidedDigitizationMode`;
- confirmed graphPanel/plotArea IDs;
- calibration anchor counts by axis;
- calibration residual report status;
- trace confirmation status;
- peak review status;
- guided release-gate result.
