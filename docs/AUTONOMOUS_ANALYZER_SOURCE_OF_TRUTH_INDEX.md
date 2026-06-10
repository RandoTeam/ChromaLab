# Autonomous Analyzer Source Of Truth Index

Date: 2026-06-08

Status: `R15A_PLUS_TV6_SOURCE_OF_TRUTH_INDEX_UPDATED`

Scope: source-of-truth plus R15A multi-panel Android evidence gate tracking and
TV-6 Knowledge native feasibility tracking. No chromatographic
math, Android fixture outcomes, model policy, validators, or
`CalculationEngine` behavior were changed.

## Purpose

ChromaLab has many phase reports, research notes, Android run summaries, Rust CV
handoffs, and model-runtime documents. They are useful history, but they cannot
all be treated as current source of truth.

This index defines which documents and code areas should guide future analyzer
replacement work.

## Precedence Order

When documents disagree, use this order:

1. Current code and tests.
2. Current product truth docs listed in this file.
3. Current architecture/source-of-truth docs listed in this file.
4. Current replacement roadmap.
5. Recent truth-audit and validation summaries.
6. Historical phase reports and research notes.

Historical phase docs can explain why something was built, but they do not prove
current behavior.

## Current Entry Points

| Status | Path | Role |
|---|---|---|
| `ACTIVE_SOURCE_OF_TRUTH` | `README.md` | Public project entry point and high-level status. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/README.md` | Documentation navigation index. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMALAB_ARCHITECTURE_OVERVIEW.md` | Current module and pipeline architecture. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_AUTONOMOUS_ANALYSIS_STAGE_MAP.md` | Stage 0-12 analyzer map used for replacement planning. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md` | Replacement protocol: inventory, contract, shadow, promote, retire. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md` | Current Stage 1-3 replacement contract for image prep, graph discovery, plotArea, and layout. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md` | Current Stage 1-3 parity and promotion plan. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md` | R1 closeout and R2 start decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md` | R2 closeout for Stage 1-3 shadow parity harness. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md` | R3 closeout for the Stage 1 image-preparation candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R4_RUST_STAGE1_IMAGE_PREPARATION_PARITY_CLOSEOUT.md` | R4 closeout for the Rust Stage 1 image-preparation parity bridge. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_CLOSEOUT.md` | R5 closeout for the Stage 2 graph discovery candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R6_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_CLOSEOUT.md` | R6 closeout for the Stage 3 plotArea/layout candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R7_STAGE4_AXIS_FRAME_SCALE_CANDIDATE_CLOSEOUT.md` | R7 closeout for the Stage 4 axis/frame/scale evidence candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R8_STAGE5_CALIBRATION_STRATEGY_PARITY_CLOSEOUT.md` | R8 closeout for the Stage 5 calibration strategy parity candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_CLOSEOUT.md` | R9 closeout for the Stage 6 automatic OCR anchor candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_CLOSEOUT.md` | R10 closeout for the Rust/runtime-shaped OCR anchor bridge candidate. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R11_INTEGRATED_RUNTIME_CALIBRATION_CLOSURE_CLOSEOUT.md` | R11 closeout for integrated runtime calibration closure in shadow/parity mode. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSURE_CLOSEOUT.md` | R12 closeout for runtime evidence/export package closure and manifest validation. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R13_ANDROID_RUNTIME_OCR_ANCHOR_PRODUCTION_BRIDGE_CLOSEOUT.md` | R13 closeout for Android/runtime OCR-anchor bridge row evidence and validator safety checks. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R14_RUNTIME_CALIBRATION_PROMOTION_CANDIDATE_CLOSEOUT.md` | R14 closeout for Android runtime OCR-anchor calibration strategy candidate and coordinate-frame safety. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R15_GRAPH_LAYOUT_MULTI_PANEL_RUNTIME_CLOSURE_CLOSEOUT.md` | R15 closeout for runtime graph-results and multi-panel layout/report propagation. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R15A_MULTI_PANEL_ANDROID_EVIDENCE_GATE.md` | R15A Android evidence gate attempt; records no-device and validation APK build blockers before R16. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/AUTONOMOUS_ANALYSIS_EVIDENCE_GATES.md` | Evidence-gate policy for graph, calibration, trace, peak, model, export. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/AUTONOMOUS_PRODUCTION_ARCHITECTURE.md` | Product target and numeric-authority boundary. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMALAB_VALIDATION_SUMMARY.md` | Public validation summary and current blocked/review status. |

## Current Product Truth

| Status | Path | Role |
|---|---|---|
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md` | Product-level truth table for the eight Android fixtures. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_PRODUCT_ACCEPTANCE_TABLE.md` | Product acceptability per fixture/mode. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_SCIENTIFIC_ACCEPTANCE_TABLE.md` | Scientific usability per fixture/mode. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_E2B_ACCEPTANCE_MATRIX.md` | Deterministic vs E2B comparison. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_ENGINEERING_NEXT_FIXES.md` | Current engineering blockers after truth audit. |
| `GENERATED_IGNORED_ARTIFACT_REFERENCE` | `artifacts/phase9j-truth-audit/` | Local truth-audit images, summaries, and contact sheets referenced by docs. Ignored by git. |
| `GENERATED_IGNORED_ARTIFACT_REFERENCE` | `artifacts/phase9i-final-android/` | Local Android evidence packages used by Phase 9J. Ignored by git. |

## Current Pipeline And Failure Contracts

| Status | Path | Role |
|---|---|---|
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md` | Failure classes and blocker semantics. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_GRAPH_LAYOUT_TAXONOMY.md` | Graph layout classes and graph-count semantics. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_REGRESSION_DATASET.md` | Fixture/dataset inventory. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_REGRESSION_MATRIX.md` | Regression matrix and acceptance tracking. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/RUNTIME_EVIDENCE_REAL_DEVICE_VALIDATION_CHECKLIST.md` | Required Android evidence/export artifacts. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMATOGRAM_SCIENTIFIC_REPORT_SPEC.md` | Scientific report boundaries and caveats. |
| `ACTIVE_SOURCE_OF_TRUTH` | `REPORT_SPEC.md` | Root report specification. |
| `ACTIVE_SOURCE_OF_TRUTH` | `PIPELINE.md` | Root pipeline summary. |
| `ACTIVE_SOURCE_OF_TRUTH` | `ROADMAP.md` | Public roadmap. |

## Current Runtime / Model / Knowledge Sources

| Status | Path | Role |
|---|---|---|
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMALAB_LOCAL_AI_RUNTIME.md` | Local AI roles, E2B baseline, GGUF/LiteRT/MTP boundaries. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/GEMMA_LITERTLM_MODEL_STRATEGY.md` | Gemma/LiteRT model catalog and install strategy. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/CHROMALAB_KNOWLEDGE_PACK_ARCHITECTURE.md` | Knowledge Pack rules, retrieval, provenance, TurboVec candidate note. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TURBOVEC_INTEGRATION_ASSESSMENT.md` | TurboVec role and boundaries. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TURBOVEC_DEEP_AUDIT_AND_MODERNIZATION_PLAN.md` | TurboVec/Rust modernization planning. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV0_TV1_TURBOVEC_KNOWLEDGE_REPLACEMENT_FOUNDATION.md` | Knowledge retrieval backend separation, TurboVec fail-closed candidate contract, and benchmark guardrails. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV2_TURBOVEC_KNOWLEDGE_INDEX_PROTOTYPE_CLOSEOUT.md` | PC-only TurboVec Knowledge Pack v2 index build, MiniLM/BGE benchmark result, and TV-3 decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV3_RETRIEVAL_AB_ARBITRATION_POLICY_CLOSEOUT.md` | PC-only lexical/dense/hybrid retrieval arbitration result and TV-4 decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV4_KNOWLEDGE_RETRIEVAL_BACKEND_PROMOTION_CANDIDATE_CLOSEOUT.md` | Kotlin-side hybrid retrieval policy candidate and TV-5 decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV5_DENSE_PROVIDER_PROMOTION_REJECTION_GATE_CLOSEOUT.md` | Dense-provider gate decision keeping TurboVec PC/dev-only and lexical as the active product retrieval owner. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/TV6_ANDROID_NATIVE_FEASIBILITY_SPIKE_CLOSEOUT.md` | Android Rust target compile feasibility for TurboVec and remaining on-device load/query blocker. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md` | Current plan superseding additive TurboVec-only integration. |

## Current Rust / Benchmark Sources

| Status | Path | Role |
|---|---|---|
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2A_RUST_CV_CORE_FOUNDATION.md` | Rust CV core foundation and boundary. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2B_RUST_AXIS_ELEMENT_BRIDGE_PROTOTYPE.md` | Rust axis element bridge prototype state. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2C_ANDROID_NDK_RUST_TARGET_SETUP.md` | Android Rust target setup. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2D_RUST_ANDROID_JNI_BRIDGE_CONTRACT.md` | JNI bridge contract. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2E_ANDROID_RUST_BRIDGE_SMOKE_RUN.md` | Android bridge smoke evidence. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2F_RUST_AXIS_ELEMENT_CROP_BLOCK_TRANSFER.md` | Rust axis-element crop transfer. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2G_RUST_ANDROID_CORPUS_PARITY.md` | Rust Android corpus parity status. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DRB_GROUND_TRUTH_CORPUS_AND_METRICS.md` | Ground-truth corpus direction. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DRB2_PHASE9J_BENCHMARK_RECORDS.md` | Phase 9J truth converted into benchmark records. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DRB3_BENCHMARK_SCORING_AND_TRUTH_GAPS.md` | Benchmark scoring gaps. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md` | Stage 1-3 replacement contract. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md` | Stage 1-3 shadow parity plan. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md` | Stage 1-3 contract closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md` | Stage 1-3 shadow parity harness closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md` | Stage 1 image-preparation candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R4_RUST_STAGE1_IMAGE_PREPARATION_PARITY_CLOSEOUT.md` | Rust Stage 1 image-preparation parity closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_CLOSEOUT.md` | Stage 2 graph discovery candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R6_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_CLOSEOUT.md` | Stage 3 plotArea/layout candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R7_STAGE4_AXIS_FRAME_SCALE_CANDIDATE_CLOSEOUT.md` | Stage 4 axis/frame/scale evidence candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R8_STAGE5_CALIBRATION_STRATEGY_PARITY_CLOSEOUT.md` | Stage 5 calibration strategy parity candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_CLOSEOUT.md` | Stage 6 automatic OCR anchor candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_CLOSEOUT.md` | Runtime OCR anchor bridge candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R11_INTEGRATED_RUNTIME_CALIBRATION_CLOSURE_CLOSEOUT.md` | Integrated runtime calibration closure candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSURE_CLOSEOUT.md` | Runtime evidence failure-package closure closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R13_ANDROID_RUNTIME_OCR_ANCHOR_PRODUCTION_BRIDGE_CLOSEOUT.md` | Android/runtime OCR-anchor production bridge closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R14_RUNTIME_CALIBRATION_PROMOTION_CANDIDATE_CLOSEOUT.md` | Runtime OCR-anchor calibration strategy candidate closeout and next-step decision. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/R15_GRAPH_LAYOUT_MULTI_PANEL_RUNTIME_CLOSURE_CLOSEOUT.md` | Graph layout and multi-panel runtime propagation closeout and next-step decision. |
| `ACTIVE_TEST` | `benchmark/schemas/` | Benchmark record schemas for truth, predictions, metrics, evidence packages, and report claims. |
| `ACTIVE_TEST` | `benchmark/examples/` | Source-controlled benchmark examples, including Phase 9J-derived records. |
| `ACTIVE_TEST` | `benchmark/examples/stage123_shadow_parity/` | R2 Stage 1-3 parity example records. |
| `ACTIVE_TEST` | `benchmark/examples/r3_image_preparation_candidate/` | R3 Stage 1 image-preparation candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r4_rust_stage1_image_preparation_parity/` | R4 Rust Stage 1 parity records. |
| `ACTIVE_TEST` | `benchmark/examples/r5_stage2_graph_discovery_candidate/` | R5 Stage 2 graph discovery candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r6_stage3_plotarea_layout_candidate/` | R6 Stage 3 plotArea/layout candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r7_stage4_axis_frame_scale_candidate/` | R7 Stage 4 axis/frame/scale candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r8_stage5_calibration_strategy_parity_candidate/` | R8 Stage 5 calibration strategy parity candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r9_stage6_automatic_ocr_anchor_candidate/` | R9 Stage 6 automatic OCR anchor candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r10_runtime_ocr_anchor_bridge_candidate/` | R10 runtime OCR anchor bridge candidate records. |
| `ACTIVE_TEST` | `benchmark/examples/r11_runtime_calibration_closure_candidate/` | R11 runtime calibration closure candidate records. |
| `ACTIVE_TEST` | `benchmark/annotations/` | Graph/layout/OCR annotation records used by research benchmark phases. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/` | Small tracked benchmark summaries and overlays. Update only with benchmark phase intent. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/stage123_shadow_parity/` | R2 Stage 1-3 parity summary. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r3_image_preparation_candidate/` | R3 Stage 1 image-preparation summary and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r4_rust_stage1_image_preparation_parity/` | R4 Rust Stage 1 parity summary and Rust per-fixture reports. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r5_stage2_graph_discovery_candidate/` | R5 Stage 2 graph discovery summary, overlays, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/` | R6 Stage 3 plotArea/layout summary, overlays, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/` | R7 Stage 4 axis/frame/scale summary, overlays, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/` | R8 Stage 5 calibration strategy summary, overlays, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/` | R9 Stage 6 automatic OCR anchor summary, overlays, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/` | R10 runtime OCR anchor bridge summary, overlays, bridge inputs/outputs, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r11_runtime_calibration_closure_candidate/` | R11 runtime calibration closure summary, overlays, details, and contact sheet. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/r12_runtime_evidence_failure_package_closure/` | R12 runtime evidence/export closure summary. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/tv2_turbovec_knowledge/` | TV-2 compact lexical vs TurboVec MiniLM/BGE Knowledge retrieval benchmark summary. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/tv3_retrieval_ab_arbitration/` | TV-3 compact retrieval policy arbitration benchmark summary. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/tv5_dense_provider_gate/` | TV-5 compact dense-provider promotion/defer decision summary. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/tv6_android_native_feasibility/` | TV-6 compact Android native compile-feasibility summary. |
| `GENERATED_IGNORED_ARTIFACT_REFERENCE` | `artifacts/tv2-turbovec-knowledge/` | TV-2 local venv, downloaded models, TurboVec `.tvim` indexes, sidecars, build manifests, and ignored heavy artifacts. |
| `GENERATED_IGNORED_ARTIFACT_REFERENCE` | `artifacts/tv6-turbovec-android-feasibility/` | TV-6 local TurboVec Android compile probe crate, logs, cargo tree, and target build output. |

## Active Code Owner Areas

| Area | Active owner paths |
|---|---|
| Input and capture | `composeApp/src/commonMain/kotlin/com/chromalab/feature/capture/`, `composeApp/src/androidMain/kotlin/com/chromalab/feature/capture/` |
| Processing pipeline | `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/flow/`, `processing/pipeline/`, `processing/sweep/` |
| Image preparation | `processing/normalize/`, `processing/preprocess/`, `processing/quality/`, `processing/document/`, `processing/perspective/` |
| Graph and layout | `processing/graph/`, `processing/geometry/GraphLayoutClassifier.kt`, `GraphMultiplicityResolver.kt`, `ScreenshotEmbeddedChartDetector.kt` |
| Axis/tick/OCR geometry | `processing/axis/`, `processing/geometry/AxisTickGeometryDetector.kt`, `TickLocalizationPipeline.kt`, `TickOcrMatcher.kt`, `processing/ocr/` |
| Calibration | `processing/calibration/`, `processing/geometry/AxisScaleResolver.kt`, `CalibrationStrategyEnsemble.kt` |
| Trace extraction | `processing/curve/`, `processing/signal/` |
| Peak evidence and calculations | `feature/calculation/`, `processing/peaks/`, `feature/reports/PeakEvidenceMapper.kt` |
| Model/VLM/GGUF/LiteRT | `processing/inference/`, `processing/model/`, Android model manager files |
| Knowledge Pack | `feature/knowledge/`, `docs/knowledge/`, `tools/knowledge-builder/`, `tools/knowledge-retrieval/` |
| Evidence validation | `processing/debug/`, `feature/validation/` |
| Reports/export | `feature/reports/`, `processing/export/`, `calculation/export/` |
| Rust CV | `rust/chromalab-cv-core/`, `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/rust/`, `tools/rust/` |

## Historical Archive

| Status | Pattern | Rule |
|---|---|---|
| `HISTORICAL_REFERENCE` | `docs/PHASE0*` through `docs/PHASE9I*` | Historical phase evidence. Do not treat as current truth unless referenced by current source-of-truth docs. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/PHASE9J_*` | Current product truth audit and acceptance tables. |
| `HISTORICAL_REFERENCE` | `docs/research/*.md` | Research notes. Use for source discovery and rationale, not current behavior. |
| `HISTORICAL_REFERENCE` | `docs/DR1*`, `docs/DRC*`, `docs/DRD*`, `docs/DRE*` | Research/prototype wave history unless explicitly promoted by current owner board. |
| `ACTIVE_SOURCE_OF_TRUTH` | `docs/DR2*`, `docs/DRB*` listed above | Current Rust/benchmark foundation references. |
| `HISTORICAL_REFERENCE` | older `docs/R*` files not listed as active above | Runtime/model phase history. Use current `CHROMALAB_LOCAL_AI_RUNTIME.md` for active model policy. |

## Current Replacement Direction

The next Android analyzer implementation phase should not install TurboVec or
port another Rust algorithm immediately.

The next phase should use the owner board in
`docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md` and choose a broad replacement
slice with a clear promotion/retirement gate.

Current completed broad phases:

```text
R1 - Graph/Layout And Image Preparation Replacement Contract
R2 - Stage 1-3 Shadow Parity Harness
R3 - Stage 1 Image Preparation Candidate
R4 - Rust Stage 1 Image Preparation Parity Bridge
R5 - Stage 2 Graph Discovery Candidate
R6 - Stage 3 PlotArea And Layout Semantics Candidate
R7 - Stage 4 Axis, Frame, And Scale Evidence Candidate
R8 - Stage 5 Calibration Strategy Parity Candidate
R9 - Stage 6 Automatic OCR Anchor Candidate
R10 - Stage 6 Runtime OCR Anchor Bridge Candidate
R11 - Integrated Runtime Calibration Closure
R12 - Runtime Evidence And Failure Package Closure
R13 - Android Runtime OCR Anchor Production Bridge
R14 - Runtime Calibration Promotion Candidate
R15 - Graph Layout And Multi-Panel Runtime Closure
```

Decision:

- R1 created the Stage 1-3 replacement contract and parity plan;
- R2 created schema-backed Stage 1-3 shadow parity records and summary reports;
- R3 created a PC-side Stage 1 image-preparation candidate and preview evidence;
- R4 created a Rust Stage 1 parity bridge with 8/8 selected-variant parity and
  8/8 PASS/REVIEW status parity against R3;
- R5 created a Stage 2 graph discovery candidate with 8/8 graph-count pass in
  shadow mode;
- R6 created a Stage 3 plotArea/layout candidate with 8/8 layout-class pass and
  REVIEW-only plotArea evidence in shadow mode;
- R7 created a Stage 4 axis/frame/scale evidence candidate with 12 annotated
  manual-review scale graphs and REVIEW-only axis/frame/scale evidence;
- R8 created a Stage 5 calibration strategy parity candidate with 12 selected
  manual-review scoring fits and REVIEW-only calibration strategy evidence;
- R9 created a Stage 6 automatic OCR anchor candidate with 12 PC-side automatic
  OCR candidate graphs, 9 valid graph decisions, 3 review graph decisions, and
  155 accepted OCR anchors;
- R10 created a Rust/runtime-shaped OCR anchor bridge candidate with 8 records,
  4/4 scoreable fixture parity, 155 accepted bridge rows, and 20 rejected
  bridge rows;
- R11 created shadow calibration closure records from R10 bridge rows with
  12 selected calibration graph fits and 155 accepted bridge anchors;
- R12 closed the evidence/export package audit over 16 Phase 9J-derived Android
  records with 16/16 core artifact completeness, 0 no-export states, 4/4 blocked
  runs carrying graph failure packages, and 0 release-ready runs;
- R13 added Android/runtime OCR-anchor bridge row evidence to runtime packages
  and graph failure packages, plus validator checks for deterministic pixel
  geometry, forbidden text, crop provenance, and VLM numeric authority;
- R14 added `ANDROID_RUNTIME_OCR_ANCHOR` as a named calibration strategy
  candidate, coordinate-frame handling for bridge rows, safety rejection before
  fitting, and selected/rejected strategy evidence in runtime graph packages;
- R15 added per-graph geometry results, resolved physical-panel iteration, and
  explicit multi-panel report aggregation warnings for unsupported one-section
  stored reports;
- R1/R2/R3/R4/R5/R6/R7/R8/R9/R10/R11/R12/R13/R14/R15 did not change Android
  analyzer math, validators, model authority, or `CalculationEngine`.

Recommended next Android analyzer gate:

```text
R15A retry - Multi-Panel Android Evidence Gate
```

Current TurboVec/Knowledge track:

```text
TV-0/TV-1 - Knowledge retrieval backend foundation
TV-2 - PC-only TurboVec Knowledge index prototype
```

TV-2 passes as a PC-only prototype and the next Knowledge retrieval phase is:

```text
TV-3 - Retrieval A/B Evaluation And Arbitration Policy
```

TV-3 selected `HYBRID_UNION_RRF` as the benchmark target. TV-4 is complete and
added a Kotlin-side promotion candidate without changing the active default
owner. TV-5 is complete and deferred runtime dense-provider promotion. TV-6 is
complete as compile feasibility and proved TurboVec `0.8.1` checks for Android
Rust targets, but no device was connected for runtime proof. The active
Knowledge retrieval owner remains `LexicalKnowledgeRetrievalBackend`. The next
Knowledge retrieval phase is:

```text
TV-6B - On-Device TurboVec Load And Query Probe
```

This does not change Android analyzer phase order. TurboVec remains Knowledge
retrieval only and must not become chromatogram geometry, calibration, trace,
peak, or report-gate authority.

## R0 Agent Swarm Summary

| Workstream | Result |
|---|---|
| Documentation inventory | Current docs index exists, but old `PHASE*`, `DR*`, and research files must be treated as archive unless promoted here. |
| Implementation owner scan | Active analyzer ownership is concentrated in `feature/processing`, with `CalculationEngine` protected and Rust still shadow/prototype. |
| Tests/fixtures scan | Fixture coverage is strong as stress/evidence coverage, but not every fixture has locked final scientific numeric truth. |
| Artifact/generated scan | `artifacts/` remains ignored; `benchmark/` has tracked evidence-like files; `build_log.txt` and Gradle wrapper jar need separate infrastructure cleanup. |
