# ChromaLab Validation Evidence Summary

Status: RP_5_VALIDATION_SUMMARY_READY

This document summarizes the current public validation story for ChromaLab. It is intentionally evidence-first: it shows what the app currently proves, what remains review-only, what is blocked, and what future validation must measure before release-quality autonomous analysis can be claimed.

## Executive Validation Truth

Current state:

- Phase 9 is not accepted as production autonomous validation.
- Phase 10 must not be presented as started or approved by validation evidence.
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
- Rust Android bridge parity can be checked over a corpus.

## What The Current Validation Does Not Prove

The current validation does not prove:

- production-ready autonomous chromatogram analysis;
- release-ready output on the 8 Android fixtures;
- 99 percent success rate;
- fully robust graph layout handling;
- fully robust axis scale calibration;
- scientific accuracy against vendor/reference peak metrics;
- universal support for GC/MS, petroleum geochemistry, or analytical chemistry workflows;
- compound identification capability;
- security-audited storage/export behavior.

## Current Engineering Blockers

Highest-priority blockers:

1. `bench_01_mz71_screenshot_page`: Y calibration blocked by insufficient usable anchors after OCR/tick pairing.
2. `bench_05_tic_plus_ions`: TIC+ions layout propagation and Y calibration direction remain blocked.
3. Multi-panel graph semantics need stronger truth and report propagation.
4. Axis scale and label evidence need ground truth for method comparisons.
5. Trace and peak evidence need reference metrics before release-quality accuracy can be claimed.
6. Public documentation still needs later real screenshots only when they show current app output honestly.
7. Security/privacy/export review remains a public roadmap item.

## Reviewer Takeaway

ChromaLab's validation story is credible because it does not hide failure. The current evidence shows a serious Android validation system, complete artifact exports, E2B baseline safety checks, and benchmark conversion. It also shows that the project is not yet release-ready as an autonomous chromatogram analyzer.

The next validation goal is not to make the summary look better. The next validation goal is to add ground truth, repair P0 fixture classes, and prove improvement through the same evidence gates.
