# ChromaLab Regression Matrix

## Autonomous-First Realignment

All regression rows now assume `AUTONOMOUS_PRODUCTION` is the target path for normal analyzable images. `AUTO_DIAGNOSTIC` is used when automatic evidence is incomplete. Phase 2/3/4 editor behavior belongs to `ASSISTED_REVIEW` or `MANUAL_ADVANCED`, not the default path.

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

## Phase 2 Addendum

Phase 2 adds guided graphPanel and plotArea confirmation UI/contracts. No fixture expectations, auto-analysis algorithms, calibration math, trace extraction, or peak math changed.

Future regression rows that exercise guided ROI confirmation should record:

- graphPanel suggested bounds;
- graphPanel confirmed bounds;
- graphPanel confirmation source;
- graphPanel warning codes;
- plotArea suggested bounds;
- plotArea confirmed bounds;
- plotArea confirmation source;
- plotArea warning codes;
- whether plotArea was fully inside graphPanel;
- whether plotArea equaled graphPanel and therefore became review-grade;
- serialized `GuidedRoiEditorSnapshot` roundtrip result.

## Phase 3 Addendum

Phase 3 adds guided X/Y calibration anchors. No fixture expectations, auto-analysis algorithms, trace extraction, peak math, or report rendering changed.

Future regression rows that exercise guided calibration should record:

- calibration mode (`GUIDED_PRODUCTION` or `MANUAL_ADVANCED`);
- anchor count by axis;
- anchor source by axis;
- accepted/rejected/outlier anchor ids;
- slope/intercept by axis when available;
- RMSE and max residual by axis;
- monotonicity status;
- calibration gate status;
- whether exactly two anchors caused review-grade output;
- serialized `CalibrationAnchorEditorSnapshot` or guided state roundtrip result.

Guided calibration cannot be used to upgrade `AUTO_DIAGNOSTIC` output. If a regression case needs manual anchors, the mode must explicitly change to guided/manual and the report provenance must show the user-confirmed calibration.

## Phase 4 Addendum

Phase 4 adds guided trace overlay confirmation. No fixture expectations, auto trace extraction algorithms, peak detection, integration math, VLM behavior, or `CalculationEngine` code changed.

Future regression rows that exercise guided trace confirmation should record:

- source trace id;
- trace point count;
- column coverage ratio;
- max gap columns;
- component count;
- branch point count;
- selected component coverage;
- text contamination score;
- frame touch ratio;
- trace confidence;
- trace confirmation decision;
- trace gate status;
- trace overlay artifact path;
- rejected/review warning codes;
- calibration set id linked to trace evidence;
- serialized `TraceOverlayEditorSnapshot` or guided state roundtrip result.

Guided trace confirmation cannot upgrade `AUTO_DIAGNOSTIC` output. If an auto trace is rejected, release output must remain diagnostic or blocked until a later guided/manual workflow supplies acceptable trace evidence.

## Phase 5 Addendum

Phase 5 adds autonomous peak evidence mapping and peak gate semantics. It does not change `CalculationEngine`, peak detection math, integration math, fixture expectations, or VLM behavior.

Future regression rows that exercise peak evidence should record:

- raw detected peak count;
- validated/reportable peak count;
- review peak count;
- rejected artifact/noise peak count;
- user-confirmed/user-edited peak count when Assisted Review is used;
- peak evidence status per reportable peak;
- apex point/pixel linkage;
- local maximum evidence;
- height, area, FWHM, S/N, prominence, and boundary evidence status;
- overlap/shoulder status;
- provenance source (`AUTO_DETECTED`, `USER_CONFIRMED`, `USER_EDITED`, `LABEL_RECOVERED`);
- whether the peak gate is `VALID`, `REVIEW`, `INVALID`, or `MISSING`.

`AUTO_DIAGNOSTIC` cannot use user peak decisions as automatic release evidence. `AUTONOMOUS_PRODUCTION` requires automatic valid peak evidence. `ASSISTED_REVIEW` and `MANUAL_ADVANCED` may satisfy peak gates through explicit user-confirmed or user-edited provenance.
