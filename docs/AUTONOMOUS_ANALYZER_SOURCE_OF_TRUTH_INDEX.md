# Autonomous Analyzer Source Of Truth Index

Date: 2026-06-06

Status: `R0_SOURCE_OF_TRUTH_INDEX_READY`

Scope: inventory only. No runtime behavior, validators, chromatographic math,
Android validation results, or report gates were changed.

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
| `ACTIVE_TEST` | `benchmark/schemas/` | Benchmark record schemas for truth, predictions, metrics, evidence packages, and report claims. |
| `ACTIVE_TEST` | `benchmark/examples/` | Source-controlled benchmark examples, including Phase 9J-derived records. |
| `ACTIVE_TEST` | `benchmark/annotations/` | Graph/layout/OCR annotation records used by research benchmark phases. |
| `SOURCE_CONTROLLED_GENERATED_OUTPUT` | `benchmark/reports/` | Small tracked benchmark summaries and overlays. Update only with benchmark phase intent. |

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
| Knowledge Pack | `feature/knowledge/`, `docs/knowledge/`, `tools/knowledge-builder/` |
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
| `HISTORICAL_REFERENCE` | `docs/R1_*` through `docs/R7_*` | Runtime/model phase history. Use current `CHROMALAB_LOCAL_AI_RUNTIME.md` for active model policy. |

## Current Replacement Direction

The next implementation phase should not install TurboVec or port another Rust
algorithm immediately.

The next phase should use the owner board in
`docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md` and choose a broad replacement
slice with a clear promotion/retirement gate.

Current completed broad phase:

```text
R1 - Graph/Layout And Image Preparation Replacement Contract
```

Decision:

- R1 created the Stage 1-3 replacement contract and parity plan;
- R1 did not change runtime behavior;
- the next phase should build a shadow parity harness before production
  integration.

Recommended next broad phase:

```text
R2 - Stage 1-3 Shadow Parity Harness
```

## R0 Agent Swarm Summary

| Workstream | Result |
|---|---|
| Documentation inventory | Current docs index exists, but old `PHASE*`, `DR*`, and research files must be treated as archive unless promoted here. |
| Implementation owner scan | Active analyzer ownership is concentrated in `feature/processing`, with `CalculationEngine` protected and Rust still shadow/prototype. |
| Tests/fixtures scan | Fixture coverage is strong as stress/evidence coverage, but not every fixture has locked final scientific numeric truth. |
| Artifact/generated scan | `artifacts/` remains ignored; `benchmark/` has tracked evidence-like files; `build_log.txt` and Gradle wrapper jar need separate infrastructure cleanup. |
