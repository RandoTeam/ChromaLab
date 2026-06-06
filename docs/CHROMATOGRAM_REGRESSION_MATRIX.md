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

## Phase 9 Addendum

Phase 9 adds real-device validation of deterministic no-model and model-enabled fixture paths.

Current Android fixture evidence:

- deterministic baseline `white_tiger_ion71_20260520_192547`: graph count 1, X/Y calibration valid, report gate `REVIEW_ONLY`, validator `REVIEW`, zero blocking issues;
- model-enabled E2B fallback `white_tiger_ion71_20260520_192400`: graph count 1, X/Y calibration valid, report gate `REVIEW_ONLY`, validator `PASS`, zero blocking issues;
- E4B FULL_ANALYSIS was not installed and remains a follow-up validation gap.

Regression rule: model activation must not run before deterministic calibration in validation fixture comparison mode, because the model is not the source of graphPanel, plotArea, tick, calibration, trace, or peak metric evidence.

## Phase 9F Addendum

Phase 9F adds axis-scale evidence from OCR label boxes and label projections. Explicit tick marks are no longer the only evidence path, but calibration still requires monotonic, residual-backed anchors.

Current real-device status:

- 16/16 Phase 9F Android exports completed under `artifacts/phase9f-multi-fixture-android-final/`.
- `bench_04`, `bench_06`, and `bench_07` remained or improved to `REVIEW_ONLY`.
- `white_tiger_ion71`, `bench_01`, `bench_02`, and `bench_05` remain `BLOCKED`.
- E2B/model-enabled mode did not regress deterministic graph count or calibration state.
- Phase 9 remains blocked; Phase 10 may not start.

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

## Phase 6 Addendum

Phase 6 adds multimodal stage judge contracts, VLM/OCR boundary checks, OCR/VLM crop benchmark reporting, and model runtime profile evidence. It does not change VLM runtime behavior, ML Kit OCR behavior, geometry, trace extraction, peak detection, or `CalculationEngine`.

Future regression rows that exercise multimodal evidence should record:

- stage judge task id and type;
- source (`CV`, `ML_KIT`, `VLM`, `BOTH`, or deterministic);
- verdict and confidence;
- local crop or overlay path;
- model runtime profile id, duration, timeout, cache hit, and backend;
- OCR/VLM disagreement status;
- rejected forbidden fields;
- retry recommendations;
- whether deterministic evidence proceeded without waiting for VLM;
- whether validator rejected any accepted VLM numeric metric.

VLM/OCR disagreement must not create numeric geometry, calibration, trace, or peak metrics. Strong deterministic geometry, trace, and peak evidence must remain able to proceed without VLM.
# Phase 6C Knowledge Regression Additions

Date: 2026-05-20

Add these knowledge-specific checks to every future release gate that touches VLM/OCR/report semantics:

| Case | Expected behavior |
|---|---|
| Ion title `Ion 71.00 (70.70 to 71.70)` | Retrieves title/channel rule and cannot become `PEAK_ANNOTATION`. |
| m/z or mass-range number | Classified as channel metadata unless independent plot/signal verification exists. |
| Numeric annotation such as `5.610` near plot | May retrieve peak annotation verification rule, but still requires local signal evidence. |
| Kovats requested without reference series | Retrieves caveat and blocks calculated RI/Kovats claim. |
| Compound assignment requested without explicit evidence | Retrieves caveat and blocks compound identity. |
| Knowledge-grounded VLM explanation without `used_entry_ids` | REVIEW/REJECTED. |
| Knowledge entry used to create numeric metric | REJECTED and validator issue. |
| Source with `NEEDS_REVIEW` license | Cannot contribute bundled production entries. |

# Phase 7 Report Regression Additions

Date: 2026-05-20

| Case | Expected report behavior | Evidence artifacts | Status |
|---|---|---|---|
| Release-ready complete evidence | HTML/Markdown/Compose show `RELEASE_READY`, complete gate matrix, and peak evidence columns. | UI contract v2, HTML, Markdown, validator report, evidence package. | Covered by report renderer tests with explicit valid evidence package. |
| Missing evidence package | Report remains `DIAGNOSTIC_ONLY`; release-quality claim is blocked. | Gate matrix shows `Evidence package = MISSING`. | Covered by Markdown/contract tests. |
| Review-only trace/calibration warning | Report displays review gate and review reasons before peak metrics. | Gate matrix, graph warnings, validator reasons. | Covered by existing gate/validator tests; fixture expansion remains Phase 8. |
| Knowledge-only compound name | Rendered as candidate hypothesis, not identified compound. | Peak table and validator warning. | Covered by report validator test. |
| Kovats without reference series | RI is caveated/rejected; no release-quality RI claim. | Kovats section and validator finding. | Covered by report validator test. |
| Multi-graph report | Graph overview and per-graph sections preserve graph order and per-graph evidence. | UI contract graph sequence. | Structurally supported; broad golden export deferred to Phase 8. |

# Phase 8 Full Regression Additions

Date: 2026-05-20

Phase 8 converts the regression matrix into an explicit dataset and failure-taxonomy gate:

- dataset inventory: `docs/CHROMATOGRAM_REGRESSION_DATASET.md`;
- failure taxonomy: `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md`;
- regression runner contract: `docs/PHASE8_REGRESSION_RUNNER.md`;
- visual/text goldens: `docs/PHASE8_VISUAL_GOLDENS.md`;
- Android validation checklist: `docs/PHASE8_REAL_ANDROID_VALIDATION.md`.

Phase 8 must not upgrade any dataset row to `RELEASE_READY` unless its evidence package, validator JSON/Markdown, report JSON, overlays, privacy manifest, and report gate all pass. The current desktop fixture suite still keeps autonomous calibration and downstream signal/peak gates blocked where calibration is unavailable; that remains correct diagnostic behavior, not a failure to be hidden.

Real Android validation was not completed in the Phase 8 desktop slice because no device was attached to `adb`. Until the documented Android checklist is executed or Product Acceptance explicitly accepts a deferral, Phase 8 remains review-ready rather than fully closed.

# Phase 8B Fixture-Driven Android Validation Additions

Date: 2026-05-20

Phase 8B adds a deterministic Android validation row that avoids camera/gallery/photo picker ambiguity:

- fixture id: `white_tiger_ion71`;
- asset: `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg`;
- metadata: `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.metadata.json`;
- safe install path: build and install `androidApp/build/outputs/apk/validation/androidApp-validation.apk`, which uses package `com.chromalab.app.validation` and preserves existing `com.chromalab.app` data;
- trigger: `adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71`;
- required result: real autonomous pipeline run after acquisition bypass, runtime evidence export, validator JSON/Markdown, final report contract JSON, report exports, stage timings, overlays when available, and manifest.

Phase 8B device run `white_tiger_ion71_20260520_162317` reached the real processing path and exported terminal artifacts, but stopped before graph analysis because the required chromatogram VLM was unavailable. The run is classified as `VLM_MODEL_UNAVAILABLE`, report gate `BLOCKED`, validator verdict `FAIL`, and Phase 9 remains blocked until model availability is resolved and the fixture is rerun.

Phase 8D tracks the later fixture run `white_tiger_ion71_20260520_170118`: deterministic graph stages reached `Y_CALIBRATION`, then failed as `TICK_LOCALIZATION_FAILURE` because Y tick label evidence was insufficient. The regression requirement is now that graph-stage failures export a `RuntimeGraphFailurePackage` and validator graph failure summary rather than ending with only `package.graphs_missing`.

Expected status remains `DIAGNOSTIC_ONLY` or `REVIEW_ONLY` until autonomous axis/tick/calibration evidence is valid. A release-ready result from this fixture is only acceptable when all Phase 0-8 evidence gates pass.

Phase 8D final run `white_tiger_ion71_20260520_184550` reaches report export with:

- graph count: `1`;
- X/Y calibration: `VALID` / `VALID`;
- runtime graph package: present;
- trace and peak overlays: present;
- runtime evidence validator: `REVIEW` with 0 blocking issues;
- report gate: `REVIEW_ONLY`;
- runtime failure class: `VLM_SEMANTIC_LAYER_UNAVAILABLE`.

This fixture row is now considered closed for tick localization and calibration. It remains review-only until a configured VLM semantic layer is available or Product Acceptance explicitly allows deterministic-only release policy for this fixture class.

# Phase 9B Multi-Fixture Android Regression Additions

Date: 2026-05-20

Phase 9B invalidates the previous single-fixture Phase 9 acceptance attempt. Future Android production validation must include:

- at least five real fixtures when available;
- deterministic and E2B model-enabled mode for every selected fixture;
- exact expected graph-count comparison;
- runtime evidence package, validator JSON/Markdown, final report JSON, report exports, manifest, and stage timings for every run;
- explicit failure class for every blocked or failed run;
- deterministic-vs-E2B comparison proving model mode does not regress graph count, calibration, trace, peak evidence, or numeric metrics.

Current Phase 9B suite result:

| Fixture class | Current Android result | Required before acceptance |
| --- | --- | --- |
| Ion 71 / White Tiger | 1 graph, REVIEW_ONLY in both modes | May remain review-only, but cannot be the only acceptance fixture. |
| Printed two-graph page | 0 graphs, BLOCKED | Graph detection/tick localization must produce graph-level evidence for both panels or classify unsupported. |
| Belyi Tigr m/z 92 single graph | deterministic detects 2 graphs; E2B detects 0 | Fix single-graph split regression and E2B model-enabled regression. |
| Small TIC export | 1 graph, DIAGNOSTIC_ONLY | Improve graph/calibration evidence before release-ready claims. |
| Stacked XIC / TIC plus ions | 0 graphs, BLOCKED | Multi-graph split and tick localization must work on stacked pages. |
| Rotated photographed page | 1 graph, REVIEW_ONLY in both modes | Keep orientation path stable while closing release evidence gaps. |

Phase 9 remains `PHASE_9B_BLOCKED_RUNTIME_FAILURE`; Phase 10 must not start until these blockers are fixed or formally deferred by Product, Scientific, and QA.

# Phase 9C Multi-Fixture Repair Status

Date: 2026-05-20

Phase 9C repaired evidence and OCR-anchor handling but did not pass acceptance.

| Fixture class | Phase 9C deterministic | Phase 9C E2B | Remaining blocker |
| --- | --- | --- | --- |
| Ion 71 / White Tiger | 1 graph, REVIEW_ONLY | 1 graph, REVIEW_ONLY | Review-only baseline, not release-ready. |
| Printed page / m/z 71 | 1 graph package, BLOCKED | 1 graph package, BLOCKED | Expected 2 graphs; `TICK_LOCALIZATION_FAILURE`. |
| Belyi Tigr m/z 92 | 1 graph, DIAGNOSTIC_ONLY | 1 graph package, BLOCKED | E2B regression remains. |
| Small TIC export | 1 graph, DIAGNOSTIC_ONLY | 1 graph, DIAGNOSTIC_ONLY | Diagnostic graphPanel confidence. |
| Stacked XIC | 1 graph package, BLOCKED | 1 graph package, BLOCKED | Expected 4 graphs; `TICK_LOCALIZATION_FAILURE`. |
| TIC plus ions | 1 graph package, BLOCKED | 1 graph package, BLOCKED | Expected 4 graphs; `TICK_LOCALIZATION_FAILURE`. |
| Two-graph page photo | 1 graph package, BLOCKED | 1 graph package, BLOCKED | Expected 2 graphs; `TICK_LOCALIZATION_FAILURE`. |
| Rotated page photo | 1 graph, REVIEW_ONLY | 1 graph, REVIEW_ONLY | Review-only baseline, not release-ready. |

Artifacts are local under `artifacts/phase9c-multi-fixture-android/`. They are not committed because they include diagnostic logs and device-local paths.

Phase 9 remains blocked; Phase 10 must not start.
## Phase 9D Regression Matrix Update

| Area | Phase 9D Result |
| --- | --- |
| Android fixture exports | PASS: 16/16 final runs exported evidence/report/validator artifacts |
| E2B graph-count regression | IMPROVED: `bench_02` no longer collapses to zero graphs in E2B mode |
| Tick localization | FAIL: required fixtures remain BLOCKED |
| Multi-panel graph count | FAIL: stacked XIC, TIC+ions, and two-graph page remain unresolved |
| VLM numeric boundaries | PASS: no VLM numeric metric authority introduced |
| CalculationEngine | PASS: untouched |

## Phase 9E Regression Matrix Update

| Area | Phase 9E Requirement |
| --- | --- |
| Graph layout semantics | `GraphLayoutClassifier` must record layout class, physical graph count, and review reasons before graph count finalization. |
| Tick localization | Graph-stage tick/calibration failures must include subreasons and anchor evidence. |
| Android suite summary | Phase 9E summary must expose expected graph count, layout class, anchor counts, calibration statuses, and subreasons. |
| E2B advisory safety | Model-enabled mode must not change graph count, layout class, calibration anchors, or numeric metrics without deterministic rejection evidence. |
| Acceptance | Phase 9 remains blocked until Android rerun proves no critical BLOCKED fixtures or Product/QA/Scientific explicitly approve limitations. |

Final Phase 9E rerun result:

| Area | Phase 9E Result |
| --- | --- |
| Android fixture exports | PASS: 16/16 final runs exported evidence/report/validator artifacts |
| Layout evidence | PARTIAL: layout class and physical count are recorded, but stacked/TIC/two-page semantics remain incomplete |
| Tick localization | FAIL: 12/16 runs remain blocked by tick/calibration failures |
| Failure subreasons | PASS: blocked tick/calibration graph-stage packages include explicit subreasons |
| E2B advisory safety | PASS: E2B did not reduce graph count/calibration state or create numeric metrics in the final rerun |
| Product acceptance | FAIL: Phase 9 remains `PHASE_9B_BLOCKED_RUNTIME_FAILURE` |

## Phase 9G Regression Matrix Update

| Area | Phase 9G Requirement |
| --- | --- |
| White Tiger regression shield | `white_tiger_ion71` must not regress from a previously valid/review calibration solely because the new resolver fails. |
| Calibration strategy ensemble | Legacy tick localization, axis-scale resolver, OCR label fit, regular sequence, grid/frame, and endpoint fallback strategy records must be evaluated or explicitly rejected. |
| Validator evidence | Graph-stage calibration failures must include selected and rejected strategy evidence. |
| Android suite summary | Phase 9G summary must aggregate all graph failure packages, not only the first one. |
| Multi-panel propagation | Still blocked unless rerun evidence proves expected graph counts and report propagation are correct. |
| Acceptance | Phase 10 remains blocked until Android rerun proves no critical runtime blocker or Product/QA/Scientific explicitly approve limitations. |

Final Phase 9G rerun result:

| Area | Phase 9G Result |
| --- | --- |
| Android fixture exports | PASS: 16/16 final runs exported evidence/report/validator artifacts |
| Validator blocking issues | PASS: 0 validator blocking issues after no-tick-candidate reason export |
| Calibration ensemble evidence | PASS: blocked calibration packages expose selected/rejected strategy evidence or explicit arbitration missing reason |
| White Tiger regression | FAIL: `white_tiger_ion71` remains `BLOCKED` in deterministic and E2B modes |
| Tick/calibration closure | FAIL: `white_tiger`, `bench_01`, `bench_02`, and `bench_05` remain blocked |
| E2B advisory safety | PASS: no E2B graph-count/calibration regression observed |
| Product acceptance | FAIL: Phase 9 remains `PHASE_9B_BLOCKED_RUNTIME_FAILURE` |

## Phase 9I Regression Matrix Update

| Area | Phase 9I Result |
| --- | --- |
| Android fixture exports | PASS: final all-fixture rerun exported 16/16 evidence/report/validator artifact sets |
| Timeout/no-export reliability | IMPROVED: `bench_01` deterministic no-export was not reproduced with 360 second suite wait budget |
| Missing terminal evidence | PASS: no final Phase 9I run missed RuntimeEvidencePackage or validator output |
| E2B advisory safety | PASS: E2B did not regress graph count, gate, failure class, export status, or numeric metrics |
| bench_01 | FAIL: remains `BLOCKED` at `TICK_LOCALIZATION_FAILURE` due insufficient Y anchors |
| bench_05 | FAIL: remains `BLOCKED` at `CALIBRATION_FAILURE` due direction-inconsistent Y calibration evidence |
| Product acceptance | FAIL: Phase 9 remains `PHASE_9B_BLOCKED_RUNTIME_FAILURE`; Phase 10 must not start |

## R2 Stage 1-3 Shadow Parity Harness Update

Date: 2026-06-06

R2 adds schema-backed PC shadow parity records for Stage 1 image preparation,
Stage 2 graph discovery, and Stage 3 plotArea/layout. R2 does not change runtime
behavior or acceptance status.

| Area | R2 Result |
| --- | --- |
| Benchmark examples | PASS: 36 `stage123-parity-record.json` examples generated |
| Schema validation | PASS: 6 schemas and 109 example documents validated |
| Current Android baseline graph count | PARTIAL: 8/16 Stage 1-3 records match expected graph count |
| Current Android baseline layout class | FAIL/PARTIAL: 2/16 Stage 1-3 records match expected layout class |
| E2B Stage 1-3 comparison | PASS: no E2B graph-count regression in current R2 records |
| PC prototype signal | REVIEW: `label_band_assisted_axis_projection_v1` reaches 4/4 graph-count pass on P0 fixtures but is research-only |
| Annotation upper bound | REVIEW: page-context/text-role upper bound reaches 4/4 layout pass but is not automatic runtime evidence |
| Product acceptance | FAIL/UNCHANGED: Phase 9 remains blocked; R2 is measurement only |

## R3 Image Preparation Candidate Update

Date: 2026-06-06

R3 adds a PC-side Stage 1 image-preparation candidate. It does not change
runtime behavior or acceptance status.

| Area | R3 Result |
| --- | --- |
| Benchmark examples | PASS: 8 Stage 1 `stage123-parity-record.json` examples generated |
| Schema validation | PASS: 6 schemas and 117 example documents validated |
| Fixture coverage | PASS: all 8 Android validation fixtures processed |
| Visual evidence | PASS: source/selected preview contact sheet generated |
| Stage 1 status | PASS: 5 fixtures; REVIEW: 3 fixtures |
| Review warnings | `bench_03` low resolution; `bench_04` and `bench_05` low contrast / weak variant score |
| Runtime impact | NONE: Android runtime, validators, reports, graph counts, and `CalculationEngine` unchanged |
| Product acceptance | FAIL/UNCHANGED: Phase 9 remains blocked; R3 is Stage 1 shadow evidence only |

## R4 Rust Stage 1 Image Preparation Parity Update

Date: 2026-06-06

R4 adds a Rust Stage 1 image-preparation parity bridge. It does not change
runtime behavior or acceptance status.

| Area | R4 Result |
| --- | --- |
| Benchmark examples | PASS: 8 Rust Stage 1 `stage123-parity-record.json` examples generated |
| Fixture coverage | PASS: all 8 Android validation fixtures processed |
| Selected variant parity | PASS: 8/8 match R3 |
| PASS/REVIEW status parity | PASS: 8/8 match R3 |
| Source file SHA parity | PASS: 8/8 match R3 |
| Normalized byte SHA parity | REVIEW: 2/8 match R3; JPEG fixtures differ by Rust/Pillow decoder bytes |
| Runtime impact | NONE: Android runtime, validators, reports, graph counts, and `CalculationEngine` unchanged |
| Product acceptance | FAIL/UNCHANGED: Phase 9 remains blocked; R4 is Stage 1 Rust shadow evidence only |

## R5 Stage 2 Graph Discovery Candidate Update

Date: 2026-06-06

R5 adds a PC-side Stage 2 graph discovery candidate. It does not change runtime
behavior or acceptance status.

| Area | R5 Result |
| --- | --- |
| Benchmark examples | PASS: 8 Stage 2 `stage123-parity-record.json` examples generated |
| Fixture coverage | PASS: all 8 Android validation fixtures processed |
| Graph-count pass | PASS: 8/8 expected physical graph counts matched |
| Stage status | REVIEW: graphPanel localization is candidate-only until Stage 3 scoring |
| Visual evidence | PASS: graph discovery overlays and contact sheet generated |
| Runtime impact | NONE: Android runtime, validators, reports, graph counts, and `CalculationEngine` unchanged |
| Product acceptance | FAIL/UNCHANGED: Phase 9 remains blocked; R5 is graph-count shadow evidence only |
