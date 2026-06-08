# ChromaLab Validation Evidence Summary

Status: RP_5_VALIDATION_SUMMARY_READY

This document summarizes the current public validation story for ChromaLab. It is intentionally evidence-first: it shows what the app currently proves, what remains review-only, what is blocked, and what future validation must measure before release-quality autonomous analysis can be claimed.

## Executive Validation Truth

Current state:

- Phase 9 is not accepted as production autonomous validation.
- Phase 10 must not be presented as started or approved by validation evidence.
- R15A multi-panel Android evidence gate has not produced new Android fixture
  evidence yet because no adb target was connected and the fresh validation APK
  build failed in the native llama/Vulkan shader-generator host toolchain step.
- The Android validation suite has real artifact coverage across 8 fixtures and 2 modes.
- No fixture is currently `RELEASE_READY`.
- Most successful-looking outputs are `REVIEW_ONLY`, not final scientific proof.
- Two fixture families remain blocked in both deterministic and E2B modes.
- E2B baseline mode did not regress deterministic graph count, calibration, or metrics in Phase 9J evidence, but it also did not fix the blocked calibration/layout cases.

The current product truth is:

| Metric | Count |
|---|---:|
| Android fixtures | 8 |
| Modes per fixture | 2 |
| Total Android runs | 16 |
| `RELEASE_READY` runs | 0 |
| `REVIEW_ONLY` / review product decisions | 12 |
| `BLOCKED` product decisions | 4 |
| Runtime evidence package present | 16 |
| Validator output present | 16 |
| Final report JSON present | 16 |
| HTML/Markdown report present | 16 |

Source: [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md).

Latest gate attempt: [R15A Multi-Panel Android Evidence Gate](R15A_MULTI_PANEL_ANDROID_EVIDENCE_GATE.md).

## Report Gates

ChromaLab validates reports through gates rather than visual appearance.

| Gate | Meaning |
|---|---|
| `RELEASE_READY` | Required evidence is complete and no critical blocker remains. |
| `REVIEW_ONLY` | Useful analysis exists, but a human must review evidence before relying on it. |
| `DIAGNOSTIC_ONLY` | Useful for debugging or method development, not scientific reporting. |
| `BLOCKED` | A critical stage failed or required evidence is missing. |

The current suite has no `RELEASE_READY` runs. This is not a presentation failure; it is an honest validator result.

## Phase 9J Android Fixture Summary

| Fixture | Deterministic result | E2B result | Product class | Scientific class | Current blocker or limitation |
|---|---|---|---|---|---|
| `white_tiger_ion71` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Peak evidence remains review-grade; not release-ready. |
| `bench_01_mz71_screenshot_page` | `BLOCKED` / validator `REVIEW` | `BLOCKED` / validator `REVIEW` | `NEEDS_ENGINEERING_FIX` | not scientifically usable | Y calibration blocked by insufficient usable anchors after OCR/tick pairing. |
| `bench_02_mz92_belyi_tigr` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Graph/evidence gate remains review-grade. |
| `bench_03_small_tic_export` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Peak evidence remains review-grade. |
| `bench_04_stacked_xic_resolution` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Stacked/multi-trace evidence remains review-grade. |
| `bench_05_tic_plus_ions` | `BLOCKED` / validator `REVIEW` | `BLOCKED` / validator `REVIEW` | `NEEDS_ENGINEERING_FIX` | not scientifically usable | TIC+ions layout not propagated and Y calibration direction is inconsistent. |
| `bench_06_photo_two_graphs_page` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Two-graph page result remains review-grade. |
| `bench_07_rotated_page_photo` | `REVIEW_ONLY` / validator `REVIEW` | `REVIEW_ONLY` / validator `PASS` | `ACCEPTABLE_REVIEW` | review-grade only | Rotated photo result remains review-grade. |

Detailed per-run artifacts and peak tables are in [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md).

## Android Artifact Package

Phase 9J summarizes artifacts from:

```text
artifacts/phase9i-final-android/
artifacts/phase9j-truth-audit/
```

Truth-audit summary files:

```text
artifacts/phase9j-truth-audit/phase9j_summary.json
artifacts/phase9j-truth-audit/phase9j_summary.md
```

Contact sheets:

```text
artifacts/phase9j-truth-audit/phase9j_contact_sheet_inputs.png
artifacts/phase9j-truth-audit/phase9j_contact_sheet_graphpanels.png
artifacts/phase9j-truth-audit/phase9j_contact_sheet_calibration.png
artifacts/phase9j-truth-audit/phase9j_contact_sheet_reports.png
```

Expected artifact classes:

- RuntimeEvidencePackage JSON.
- Validator JSON.
- Validator Markdown.
- Final report contract JSON.
- HTML report.
- Markdown report.
- GraphPanel overlay where graph evidence reached that stage.
- PlotArea overlay where graph evidence reached that stage.
- Axis/tick/calibration overlay where calibration evidence reached that stage.
- Trace overlay where trace evidence reached that stage.
- Peak overlay where peak evidence reached that stage.
- Graph failure package when graph-stage failure needs explicit failure evidence.
- Stage timings and runtime/model diagnostics.

Artifact completeness is strong at the package/report level: all 16 Phase 9J runs have evidence package, validator output, report contract, and report exports. Some overlays are missing in blocked cases because the run did not reach those stages; those cases must keep explicit failure packages and missing-artifact reasons.

## E2B Baseline Validation

E2B is treated as the baseline FAST/weaker-device production model path, but it remains advisory for geometry and numeric science.

Phase 9J E2B result:

| Check | Result |
|---|---|
| E2B changed graph count by itself | No |
| E2B changed calibration by itself | No |
| E2B changed peak metrics by itself | No |
| E2B hid deterministic failure classes | No |
| E2B regressed deterministic result | No |
| E2B fixed blocked calibration/layout cases | No |

Source: [Phase 9J E2B Acceptance Matrix](PHASE9J_E2B_ACCEPTANCE_MATRIX.md).

Interpretation:

- E2B is safe enough in this validation slice as an advisory baseline.
- E2B should not be removed or treated as experimental-only.
- E2B must continue to be tested against deterministic baseline for every fixture.
- E2B cannot be used as evidence that a run is release-ready unless deterministic evidence gates also pass.

## Benchmark Record Conversion

DR-B converted Phase 9J truth into schema-backed benchmark records so future method changes can be scored against the same cases.

Current DR-B coverage:

| Benchmark item | Count |
|---|---:|
| Phase 9J benchmark cases | 16 |
| Fixtures | 8 |
| Modes | deterministic and E2B baseline |
| Generated schema-backed JSON records | 64 |
| Review decisions | 12 |
| Blocked decisions | 4 |
| Release-ready decisions | 0 |

Each case has:

- `prediction.json`;
- `metrics.json`;
- `evidence-package.json`;
- `report-claims.json`.

Source docs:

- [DR-B Ground Truth Corpus And Automatic Metrics](DRB_GROUND_TRUTH_CORPUS_AND_METRICS.md)
- [DR-B2 Phase 9J Benchmark Records](DRB2_PHASE9J_BENCHMARK_RECORDS.md)
- [DR-B3 Benchmark Scoring And Fixture Truth Gaps](DRB3_BENCHMARK_SCORING_AND_TRUTH_GAPS.md)

## R2 Stage 1-3 Shadow Parity Harness

R2 adds a PC-side schema-backed parity harness for Stage 1 image preparation,
Stage 2 graph discovery, and Stage 3 plotArea/layout.

It writes:

- `benchmark/examples/stage123_shadow_parity/`
- `benchmark/reports/stage123_shadow_parity/summary.json`
- `benchmark/reports/stage123_shadow_parity/summary.md`

R2 result:

| Item | Count / status |
|---|---:|
| Stage 1-3 parity records | 36 |
| Fixtures represented | 8 |
| Schemas validated | 6 |
| Example documents validated | 109 |
| Current Android baseline graph-count pass | 8 / 16 |
| Current Android baseline layout pass | 2 / 16 |
| E2B Stage 1-3 graph-count regressions | 0 |

Interpretation:

- R2 is measurement only.
- It does not change Android runtime behavior, validators, report gates, graph
  counts, chromatographic math, E2B policy, or `CalculationEngine`.
- Phase 9 remains blocked for production autonomous acceptance.

## R3 Image Preparation Candidate

R3 adds a PC-side Stage 1 image-preparation candidate.

It writes:

- `benchmark/examples/r3_image_preparation_candidate/`
- `benchmark/reports/r3_image_preparation_candidate/summary.json`
- `benchmark/reports/r3_image_preparation_candidate/summary.md`
- `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png`

R3 result:

| Item | Count / status |
|---|---:|
| Stage 1 candidate records | 8 |
| Fixtures represented | 8 |
| Stage 1 PASS fixtures | 5 |
| Stage 1 REVIEW fixtures | 3 |
| Schemas validated | 6 |
| Example documents validated | 117 |

Review fixtures:

- `bench_03_small_tic_export`: low-resolution input;
- `bench_04_stacked_xic_resolution`: low contrast and weak variant score;
- `bench_05_tic_plus_ions`: low contrast and weak variant score.

Interpretation:

- R3 is shadow-only Stage 1 evidence.
- It does not fix graph/layout/calibration blockers.
- It prepares the next Rust Stage 1 parity bridge.

## R4 Rust Stage 1 Image Preparation Parity

R4 ports the R3 Stage 1 image-preparation candidate into a Rust shadow bridge.

It writes:

- `benchmark/examples/r4_rust_stage1_image_preparation_parity/`
- `benchmark/reports/r4_rust_stage1_image_preparation_parity/summary.json`
- `benchmark/reports/r4_rust_stage1_image_preparation_parity/summary.md`
- `benchmark/reports/r4_rust_stage1_image_preparation_parity/rust_reports/`

R4 result:

| Item | Count / status |
|---|---:|
| Rust Stage 1 parity records | 8 |
| Fixtures represented | 8 |
| Selected variant parity vs R3 | 8/8 |
| PASS/REVIEW status parity vs R3 | 8/8 |
| Source file SHA parity vs R3 | 8/8 |
| Normalized byte SHA parity vs R3 | 2/8 |
| Schemas validated | 6 |
| Example documents validated | 125 |

Interpretation:

- R4 is shadow-only Rust Stage 1 evidence.
- PNG fixtures match source and normalized byte hashes.
- JPEG fixtures keep `NORMALIZED_SHA_MISMATCH` review notes because Pillow and
  Rust decode normalized RGB bytes differently.
- It does not fix graph/layout/calibration blockers.
- It prepares Stage 2 graph discovery candidate work.

## R5 Stage 2 Graph Discovery Candidate

R5 adds a PC-side Stage 2 graph discovery candidate that consumes R4 Rust Stage
1 evidence.

It writes:

- `benchmark/examples/r5_stage2_graph_discovery_candidate/`
- `benchmark/reports/r5_stage2_graph_discovery_candidate/summary.json`
- `benchmark/reports/r5_stage2_graph_discovery_candidate/summary.md`
- `benchmark/reports/r5_stage2_graph_discovery_candidate/contact_sheet_graph_discovery.png`

R5 result:

| Item | Count / status |
|---|---:|
| Stage 2 graph discovery records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Stage evidence status | REVIEW |
| Schemas validated | 6 |
| Example documents validated | 133 |

Interpretation:

- R5 is shadow-only Stage 2 graph-count evidence.
- It does not prove final graphPanel bounds, plotArea, calibration, trace, peak,
  report readiness, or product acceptance.
- It prepares Stage 3 plotArea/layout candidate work.

## R6 Stage 3 PlotArea And Layout Candidate

R6 adds a PC-side Stage 3 plotArea/layout candidate that consumes R5 graphPanel
candidates.

It writes:

- `benchmark/examples/r6_stage3_plotarea_layout_candidate/`
- `benchmark/reports/r6_stage3_plotarea_layout_candidate/summary.json`
- `benchmark/reports/r6_stage3_plotarea_layout_candidate/summary.md`
- `benchmark/reports/r6_stage3_plotarea_layout_candidate/contact_sheet_plotarea_layout.png`
- `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/`

R6 result:

| Item | Count / status |
|---|---:|
| Stage 3 plotArea/layout records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| P0 annotated truth fixtures | 4 |
| Annotated graphPanel mean IoU | 0.739372 |
| Annotated plotArea mean IoU | 0.62146 |
| Stage evidence status | REVIEW |
| Schemas validated | 6 |
| Example documents validated | 141 |

Interpretation:

- R6 is shadow-only Stage 3 plotArea/layout evidence.
- It does not prove axis scale, calibration, trace, peak, report readiness, or
  product acceptance.
- Photo/page plotArea localization remains REVIEW and needs stronger
  axis/frame/scale evidence before any runtime promotion.

## R7 Stage 4 Axis, Frame, And Scale Evidence Candidate

R7 adds a PC-side Stage 4 axis/frame/scale evidence candidate that consumes R6
plotArea/layout candidates and DR-C4 manual-review tick/text annotations.

It writes:

- `benchmark/examples/r7_stage4_axis_frame_scale_candidate/`
- `benchmark/reports/r7_stage4_axis_frame_scale_candidate/summary.json`
- `benchmark/reports/r7_stage4_axis_frame_scale_candidate/summary.md`
- `benchmark/reports/r7_stage4_axis_frame_scale_candidate/contact_sheet_axis_frame_scale.png`
- `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/`

R7 result:

| Item | Count / status |
|---|---:|
| Stage 4 axis/frame/scale records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| P0 annotated truth fixtures | 4 |
| Manual-review scale graph count | 12 |
| Mean x-axis support | 0.519346 |
| Mean y-axis support | 0.45788 |
| Annotated x-axis mean pixel error | 21.482143 |
| Annotated y-axis mean pixel error | 110.5 |
| Stage evidence status | REVIEW |
| Schemas validated | 7 |
| Example documents validated | 149 |

Interpretation:

- R7 is shadow-only Stage 4 axis/frame/scale evidence.
- DR-C4 anchors are scoring truth only; they are not runtime calibration.
- High axis alignment errors on page/photo cases show that calibration parity
  must still improve or precisely classify upstream geometry before promotion.

## R8 Stage 5 Calibration Strategy Parity Candidate

R8 adds a PC-side Stage 5 calibration strategy parity candidate that consumes R7
axis/frame/scale evidence and DR-C4 manual-review tick/text annotations.

It writes:

- `benchmark/examples/r8_stage5_calibration_strategy_parity_candidate/`
- `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/summary.json`
- `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/summary.md`
- `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/contact_sheet_calibration_strategy.png`
- `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/`

R8 result:

| Item | Count / status |
|---|---:|
| Stage 5 calibration strategy records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Annotated truth fixtures | 4 |
| Selected calibration graph count | 12 |
| Annotated anchor count | 194 |
| Selected X mean RMSE | 0.000287 |
| Selected Y mean RMSE | 0.231479 |
| Stage evidence status | REVIEW |
| Schemas validated | 8 |
| Example documents validated | 157 |

Interpretation:

- R8 is shadow-only Stage 5 calibration strategy parity evidence.
- It shows that all-anchor and endpoint strategies are coherent when DR-C4
  manual-review anchors are available.
- It rejects geometry-only/no-numeric candidates.
- It does not prove automatic OCR anchors or Android runtime calibration.

## R9 Stage 6 Automatic OCR Anchor Candidate

R9 adds PC-side automatic OCR anchor candidate records that consume R8
calibration strategy evidence plus DRD/DRE OCR reports.

It writes:

- `benchmark/examples/r9_stage6_automatic_ocr_anchor_candidate/`
- `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/summary.json`
- `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/summary.md`
- `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/contact_sheet_automatic_ocr_anchor_candidate.png`
- `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/`

R9 result:

| Item | Count / status |
|---|---:|
| Stage 6 automatic OCR anchor records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Annotated truth fixtures | 4 |
| Automatic OCR candidate graphs | 12 |
| Valid candidate graphs | 9 |
| Review candidate graphs | 3 |
| Accepted OCR anchors | 155 |
| Rejected OCR anchors | 20 |
| Mean fit RMSE px | 0.725662 |
| Mean truth tick RMSE px | 12.601638 |
| Stage evidence status | REVIEW/MISSING |
| Schemas validated | 9 |
| Example documents validated | 165 |

Interpretation:

- R9 is shadow-only Stage 6 automatic OCR anchor evidence.
- It shows that PC-side safe OCR can recover usable anchors on the four P0
  annotated fixtures.
- It does not prove Android runtime OCR anchor generation or runtime
  calibration.
- It cannot upgrade Android Phase 9 blocked/review outcomes.

## R10 Runtime OCR Anchor Bridge Candidate

R10 adds a Rust/runtime-shaped OCR anchor bridge candidate that consumes R9
safe OCR anchor evidence and validates the rows through a deterministic Rust
contract.

It writes:

- `benchmark/examples/r10_runtime_ocr_anchor_bridge_candidate/`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/summary.json`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/summary.md`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/contact_sheet_runtime_ocr_anchor_bridge.png`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/bridge_inputs/`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/bridge_outputs/`
- `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/`

R10 result:

| Item | Count / status |
|---|---:|
| Runtime OCR bridge records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Scoreable fixtures | 4 |
| Anchor-count parity pass | 4/4 |
| Bridge accepted anchors | 155 |
| Bridge rejected anchors | 20 |
| Missing source crop image files | 155 |
| Stage evidence status | REVIEW/MISSING |
| Schemas validated | 10 |
| Example documents validated | 173 |

Interpretation:

- R10 is shadow-only Rust/runtime OCR anchor bridge evidence.
- It proves safe anchor rows can be carried with pixel geometry, numeric OCR
  values, source crop references, confidence, residual/projection fields, and
  forbidden-source rejection.
- It does not prove Android runtime OCR generation because the rows still come
  from R9 benchmark evidence.
- It cannot upgrade Android Phase 9 blocked/review outcomes until R11 connects
  the bridge rows to calibration ensemble parity and Android evidence packages.

## R11 Integrated Runtime Calibration Closure Candidate

R11 adds shadow calibration closure records that consume R10 runtime-shaped OCR
anchor bridge rows and fit selected/rejected calibration strategies.

It writes:

- `benchmark/examples/r11_runtime_calibration_closure_candidate/`
- `benchmark/reports/r11_runtime_calibration_closure_candidate/summary.json`
- `benchmark/reports/r11_runtime_calibration_closure_candidate/summary.md`
- `benchmark/reports/r11_runtime_calibration_closure_candidate/contact_sheet_runtime_calibration_closure.png`
- `benchmark/reports/r11_runtime_calibration_closure_candidate/details/`

R11 result:

| Item | Count / status |
|---|---:|
| Runtime calibration closure records | 8 |
| Fixtures represented | 8 |
| Graph-count pass | 8/8 |
| Layout-class pass | 8/8 |
| Selected calibration graphs | 12 |
| Accepted bridge anchors used | 155 |
| Rejected bridge anchors audited | 20 |
| Missing source crop image files | 155 |
| Stage evidence status | REVIEW/MISSING |
| Schemas validated | 11 |
| Example documents validated | 181 |

Interpretation:

- R11 is shadow-only calibration closure evidence.
- It shows R10 safe bridge rows can create review-grade calibration strategy
  fits for the scoreable P0 fixtures.
- It does not prove Android runtime OCR anchor generation or production
  calibration.
- It cannot upgrade Android Phase 9 blocked/review outcomes until Android
  RuntimeEvidencePackage rows and persisted crop files show equivalent evidence.

## R12 Runtime Evidence And Failure Package Closure

R12 audits the tracked Phase 9J benchmark records for runtime evidence/export
closure. It does not change Android runtime analysis behavior, chromatographic
math, model policy, report gates, or `CalculationEngine`.

It writes:

- `benchmark/reports/r12_runtime_evidence_failure_package_closure/summary.json`
- `benchmark/reports/r12_runtime_evidence_failure_package_closure/summary.md`

R12 result:

| Item | Count / status |
|---|---:|
| Audited fixture/mode records | 16 |
| Fixtures represented | 8 |
| Core artifact complete records | 16/16 |
| No-export states | 0 |
| Blocked records | 4 |
| Blocked records with graph failure package | 4/4 |
| Blocked records missing first failing stage | 0 |
| Review-only records | 12 |
| Release-ready records | 0 |

Interpretation:

- R12 closes the current evidence/package accountability gap for the tracked
  Phase 9J records.
- `bench_01_mz71_screenshot_page` and `bench_05_tic_plus_ions` remain blocked
  in deterministic and E2B modes.
- Phase 9 remains not accepted because no fixture is release-ready and runtime
  analysis correctness is still review/blocked.

## R13 Android Runtime OCR Anchor Production Bridge

R13 adds Android/runtime OCR-anchor bridge rows to runtime evidence packages.
It does not promote calibration and does not change chromatographic math,
report gates, model policy, or `CalculationEngine`.

R13 evidence row fields include:

- graph id and graph index;
- axis;
- OCR raw text and parsed numeric value;
- deterministic pixel coordinate when available;
- crop file path or explicit missing-crop reason;
- geometry source and numeric source;
- accepted/rejected/semantic-only status;
- rejection reason.

Validator coverage now blocks accepted anchors without deterministic pixel
geometry, accepted forbidden scale text such as m/z or ion labels, VLM/E2B
numeric authority, missing crop provenance, and graph-index mismatches.

Product interpretation:

- R13 closes the runtime evidence contract gap for OCR anchors.
- R13 does not make `bench_01` or `bench_05` release-ready.
- R13 prepared the runtime evidence needed for R14 calibration strategy
  candidate work.

## R14 Runtime Calibration Promotion Candidate

R14 feeds Android/runtime OCR-anchor bridge rows into
`CalibrationStrategyEnsemble` as a named strategy source:

```text
ANDROID_RUNTIME_OCR_ANCHOR
```

R14 adds coordinate-frame safety to runtime OCR-anchor rows:

- AxisScaleResolver-derived rows are plot-relative;
- fallback tick-OCR rows are image-absolute and must be converted through the
  selected plotArea;
- rows without coordinate frame are invalid.

The strategy consumes only safe accepted rows. It rejects missing numeric
values, missing deterministic pixel coordinates, forbidden title/ion/m/z/SIM
text, VLM/E2B numeric authority, rejected or semantic-only geometry sources,
and rows outside the selected plot frame after coordinate conversion.

Product interpretation:

- R14 improves calibration evidence plumbing and arbitration visibility.
- R14 does not change `CalculationEngine`, graph detection, trace extraction,
  peak metrics, report gates, graph-count metadata, or E2B authority.
- R14 does not accept Phase 9; Android fixture reruns still need to prove that
  `bench_01` and `bench_05` improve without regressing White Tiger, `bench_03`,
  `bench_07`, deterministic/E2B parity, or evidence completeness.
- The next runtime step is R15 graph layout and multi-panel runtime closure.

## R15 Graph Layout And Multi-Panel Runtime Closure

R15 adds runtime propagation for multi-panel graph truth. It keeps expected
fixture graph counts unchanged:

| Fixture | Expected graph units | R15 propagation rule |
|---|---:|---|
| `bench_04_stacked_xic_resolution` | 4 | Preserve four resolved physical graph units. |
| `bench_05_tic_plus_ions` | 4 | Preserve four graph units; TIC+ion naming requires deterministic text support. |
| `bench_06_photo_two_graphs_page` | 2 | Preserve two resolved physical graph units. |

Product interpretation:

- R15 makes runtime graph iteration use resolved physical panels rather than raw
  pseudo-panel candidates.
- R15 adds per-graph geometry result evidence, stable graph indexes, and an
  explicit `multi_panel_report_aggregation_unsupported` warning when a stored
  report section covers only one graph from a multi-panel run.
- R15 does not change `CalculationEngine`, chromatographic math, trace
  extraction, peak metrics, validators, or E2B authority.
- R15 does not accept Phase 9; Android fixture reruns still need to prove that
  multi-panel evidence and reports are complete without regression.

## Benchmark Scoring Result

DR-B3 scoring result:

| Stage | PASS | REVIEW | FAIL |
|---|---:|---:|---:|
| Graph panel | 8 | 8 | 0 |
| Calibration | 0 | 14 | 2 |
| Trace | 0 | 12 | 4 |
| Peaks | 0 | 12 | 4 |
| Report claims | 0 | 12 | 4 |
| Evidence package | 16 | 0 | 0 |

Product interpretation:

- Evidence package reliability is currently the strongest validation result.
- Release-quality numeric accuracy is not yet proven.
- Calibration, trace, and peak evidence remain review/fail areas.
- No run should be marketed as release-ready.

## Fixture Priority

DR-B3 assigns fixture priorities for the next method-comparison waves.

| Priority | Meaning | Fixtures |
|---|---|---|
| P0 | Blocks method comparison or has critical graph-count/runtime issue | `bench_01_mz71_screenshot_page`, `bench_04_stacked_xic_resolution`, `bench_05_tic_plus_ions`, `bench_06_photo_two_graphs_page` |
| P1 | Review-grade case needing stronger truth before release metrics | `bench_02_mz92_belyi_tigr`, `bench_03_small_tic_export`, `bench_07_rotated_page_photo`, `white_tiger_ion71` |
| P2 | Lower-priority review case | One fixture/mode record in DR-B3 scoring |

P0 fixtures should drive the next graph-layout, axis-scale, and report-propagation repair work.

## Missing Truth Data

The current real fixtures are valuable diagnostics, but they do not yet contain enough ground truth to prove release-quality numeric accuracy.

Missing truth fields counted across Phase 9J benchmark cases:

| Truth field | Cases needing it |
|---|---:|
| `input_image_hash` | 16 |
| `tick_or_grid_positions` | 16 |
| `numeric_label_boxes` | 16 |
| `calibration_anchors` | 16 |
| `trace_reference` | 16 |
| `peak_reference_metrics` | 16 |
| `report_claim_expectations` | 16 |
| `axis_endpoints` | 2 |

This means the current corpus is strong for product truth and failure classification, but not yet sufficient for release-grade numeric accuracy claims.

## Rust CV Validation

Rust CV is being validated as an infrastructure and parity layer before it becomes production analysis authority.

DR-2G Rust Android corpus parity result:

| Metric | Result |
|---|---:|
| Graph packages | 18 |
| Fixture ids | 10 |
| Android decision | PASS |
| Android pass/fail | 18 / 0 |
| PC/Android parity | PASS |

Source: [DR-2G Rust Android Corpus Parity](DR2G_RUST_ANDROID_CORPUS_PARITY.md).

Interpretation:

- Rust JNI bridge parity is promising for axis-element crop planning.
- Rust does not yet own graph detection, calibration, trace extraction, peak integration, or `CalculationEngine`.
- The next Rust gate must validate image buffer ownership, format, stride, checksum, and Android memory constraints before larger CV transfer.

## What The Current Validation Proves

The current validation proves:

- the Android validation package can run multiple fixtures side by side;
- deterministic and E2B modes can be compared;
- runtime evidence packages and validator outputs are produced for all Phase 9J runs;
- report exports exist for all Phase 9J runs;
- E2B did not regress deterministic geometry/calibration/metrics in the audited slice;
- blocked fixtures are visible with failure classes and next engineering fixes;
- benchmark schemas can encode the Phase 9J truth audit for future scoring;
- R12 evidence/package closure checks show 16/16 tracked runs have core
  artifacts, 0 no-export states, and 4/4 blocked runs with graph failure
  packages;
- Rust/runtime-shaped OCR anchor bridge rows can be validated without allowing
  VLM pixel geometry or numeric calibration authority;
- Rust Android bridge parity can be checked over a corpus.

## What The Current Validation Does Not Prove

The current validation does not prove:

- production-ready autonomous chromatogram analysis;
- release-ready output on the 8 Android fixtures;
- 99 percent success rate;
- fully robust graph layout handling;
- fully robust axis scale calibration;
- Android-generated OCR anchor rows feeding runtime calibration;
- scientific accuracy against vendor/reference peak metrics;
- universal support for GC/MS, petroleum geochemistry, or analytical chemistry workflows;
- compound identification capability;
- security-audited storage/export behavior.

## R15A Multi-Panel Android Evidence Gate

R15A attempted to verify R15 on Android for `bench_04`, `bench_05`,
`bench_06`, and regression witnesses. The gate did not produce new Android
artifacts because adb reported no connected device or emulator. A fresh
validation APK build also failed in the native llama/Vulkan shader-generator
host build because the Windows host linker libraries were unavailable.

Interpretation:

- R15A did not prove R15 multi-panel propagation on Android.
- R15A did not prove an E2B regression or no-regression result.
- The current product truth remains Phase 9J/R12 plus R15 implementation
  status.
- The next executable action is an R15A retry after device and toolchain
  readiness, not R16.

## TV-0/TV-1 TurboVec Knowledge Replacement Foundation

TV-0/TV-1 started the Knowledge retrieval replacement track without changing
Android analysis behavior. `KnowledgeRetrievalEngine` is now a facade,
`LexicalKnowledgeRetrievalBackend` is the active ranking owner, and
`TurboVecKnowledgeRetrievalBackend` is fail-closed as `SHADOW_UNAVAILABLE` until
a PC benchmark/index exists.

Interpretation:

- TurboVec is not part of graph detection, calibration, trace extraction, peak
  metrics, report gates, or `CalculationEngine`.
- Current Knowledge retrieval behavior remains lexical and local/offline.
- First benchmark goldens now verify expected citations and safety boundaries
  before any dense index promotion.
- Android runtime promotion remains blocked until later TurboVec benchmark and
  native packaging gates.

## Current Engineering Blockers

Highest-priority blockers:

1. `bench_01_mz71_screenshot_page`: Y calibration blocked by insufficient usable anchors after OCR/tick pairing.
2. `bench_05_tic_plus_ions`: TIC+ions layout propagation and Y calibration direction remain blocked.
3. Multi-panel graph semantics need stronger truth and report propagation.
4. R15A Android evidence gate requires a connected adb target and a validation
   APK build environment with working native host shader-generator tooling.
5. Axis scale and label evidence need ground truth for method comparisons.
6. Trace and peak evidence need reference metrics before release-quality accuracy can be claimed.
7. Public documentation still needs later real screenshots only when they show current app output honestly.
8. Security/privacy/export review remains a public roadmap item.

## Reviewer Takeaway

ChromaLab's validation story is credible because it does not hide failure. The current evidence shows a serious Android validation system, complete artifact exports, E2B baseline safety checks, and benchmark conversion. It also shows that the project is not yet release-ready as an autonomous chromatogram analyzer.

The next validation goal is not to make the summary look better. The next validation goal is to add ground truth, repair P0 fixture classes, and prove improvement through the same evidence gates.
