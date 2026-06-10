# ChromaLab Documentation Index

Status: RP_8_PUBLIC_DOC_INDEX_READY

This index helps public reviewers, contributors, and scientific users navigate
the ChromaLab documentation without reading every internal phase report.

ChromaLab has a large documentation archive because the project tracks Android
validation, local model runtime work, Rust CV experiments, deep research, and
scientific report gates. Start with the documents below before opening the
phase-history files.

## Read These First

| Document | Use it for |
|---|---|
| [Root README](../README.md) | Main public project story, status, stack, roadmap, and responsible-use note. |
| [OpenAI Reviewer Start Here](OPENAI_REVIEWER_START_HERE.md) | One-page reviewer entry point for product value, validation status, credits, and security relevance. |
| [Scientific Product Overview](CHROMALAB_SCIENTIFIC_PRODUCT_OVERVIEW.md) | Product purpose, target users, scientific workflow, and current limitations. |
| [Architecture Overview](CHROMALAB_ARCHITECTURE_OVERVIEW.md) | How Android/KMP, Rust CV, local AI runtime, reports, evidence packages, and validation fit together. |
| [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md) | What the Android validation suite currently proves and what remains blocked. |
| [Analyzer Source Of Truth Index](AUTONOMOUS_ANALYZER_SOURCE_OF_TRUTH_INDEX.md) | Current active docs, code-owner areas, and historical archive rules for analyzer replacement work. |
| [Analyzer Layer Owner Board](AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md) | Stage-by-stage owner map for replacing old analyzer layers without adding permanent duplicate paths. |
| [R1 Graph/Layout Image Prep Contract](R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md) | Current Stage 1-3 replacement contract for image preparation, graph discovery, plotArea, and layout. |
| [R1 Graph/Layout Image Prep Parity Plan](R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md) | Stage 1-3 shadow parity, metrics, fixture corpus, and promotion gates. |
| [R1 Graph/Layout Image Prep Closeout](R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md) | R1 closeout and R2 start decision. |
| [R2 Stage 1-3 Shadow Parity Harness Closeout](R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md) | R2 schema-backed parity harness, output summary, and R3 start decision. |
| [R3 Image Preparation Candidate Closeout](R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md) | R3 Stage 1 image-preparation candidate, preview evidence, and R4 start decision. |
| [R4 Rust Stage 1 Image Preparation Parity Closeout](R4_RUST_STAGE1_IMAGE_PREPARATION_PARITY_CLOSEOUT.md) | R4 Rust Stage 1 parity bridge, decoder-limit findings, and R5 start decision. |
| [R5 Stage 2 Graph Discovery Candidate Closeout](R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_CLOSEOUT.md) | R5 graph-count candidate, overlays, and R6 start decision. |
| [R6 Stage 3 PlotArea Layout Candidate Closeout](R6_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_CLOSEOUT.md) | R6 plotArea/layout candidate, overlays, IoU scoring, and R7 start decision. |
| [R7 Stage 4 Axis Frame Scale Candidate Closeout](R7_STAGE4_AXIS_FRAME_SCALE_CANDIDATE_CLOSEOUT.md) | R7 axis/frame/scale evidence candidate, overlays, scoring, and R8 start decision. |
| [R8 Stage 5 Calibration Strategy Parity Closeout](R8_STAGE5_CALIBRATION_STRATEGY_PARITY_CLOSEOUT.md) | R8 calibration strategy parity candidate, overlays, scoring, and R9 start decision. |
| [R9 Stage 6 Automatic OCR Anchor Candidate Closeout](R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_CLOSEOUT.md) | R9 automatic OCR anchor candidate, overlays, scoring, and R10 start decision. |
| [R10 Runtime OCR Anchor Bridge Candidate Closeout](R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_CLOSEOUT.md) | R10 Rust/runtime-shaped OCR anchor bridge contract, parity records, and R11 start decision. |
| [R11 Integrated Runtime Calibration Closure Closeout](R11_INTEGRATED_RUNTIME_CALIBRATION_CLOSURE_CLOSEOUT.md) | R11 shadow calibration closure records and selected/rejected strategy evidence. |
| [R12 Runtime Evidence Failure Package Closure Closeout](R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSURE_CLOSEOUT.md) | R12 evidence/export closure checks, manifest validation, and R13 start decision. |
| [R13 Android Runtime OCR Anchor Production Bridge Closeout](R13_ANDROID_RUNTIME_OCR_ANCHOR_PRODUCTION_BRIDGE_CLOSEOUT.md) | Android/runtime OCR-anchor row evidence contract, validator checks, and R14 start decision. |
| [R14 Runtime Calibration Promotion Candidate Closeout](R14_RUNTIME_CALIBRATION_PROMOTION_CANDIDATE_CLOSEOUT.md) | Runtime OCR-anchor calibration strategy candidate, coordinate-frame safety, and R15 start decision. |
| [R15 Graph Layout Multi-Panel Runtime Closure Closeout](R15_GRAPH_LAYOUT_MULTI_PANEL_RUNTIME_CLOSURE_CLOSEOUT.md) | Runtime graph-results propagation, resolved physical panel iteration, and multi-panel report aggregation warning. |
| [R15A Multi-Panel Android Evidence Gate](R15A_MULTI_PANEL_ANDROID_EVIDENCE_GATE.md) | Android evidence-gate attempt for R15; records no-device and validation APK build blockers before R16. |
| [Analyzer Stale File Audit](AUTONOMOUS_ANALYZER_STALE_FILE_AUDIT.md) | Which docs, artifacts, generated outputs, and secrets are active, historical, ignored, or local-only. |
| [Local AI Runtime](CHROMALAB_LOCAL_AI_RUNTIME.md) | LiteRT-LM, Gemma, E2B baseline, GGUF, llama.cpp, MTP, model safety, and privacy boundaries. |
| [Report Experience Concept](CHROMALAB_REPORT_EXPERIENCE_CONCEPT.md) | Target design for the future professional report, without fake screenshots or fake metrics. |
| [Public Messaging Guide](CHROMALAB_PUBLIC_MESSAGING_GUIDE.md) | What the repository can claim publicly and what should remain grant/application language. |

## Current Product Status

Use these documents to understand what works, what is review-only, and what is
blocked.

| Document | Use it for |
|---|---|
| [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md) | Public validation story and fixture-level status. |
| [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md) | Product-level truth table for the eight Android validation fixtures. |
| [Phase 9J Product Acceptance Table](PHASE9J_PRODUCT_ACCEPTANCE_TABLE.md) | Product decision per fixture and mode. |
| [Phase 9J Scientific Acceptance Table](PHASE9J_SCIENTIFIC_ACCEPTANCE_TABLE.md) | Scientific usability classification per fixture. |
| [Phase 9J E2B Acceptance Matrix](PHASE9J_E2B_ACCEPTANCE_MATRIX.md) | Deterministic vs E2B model-enabled comparison. |
| [Phase 9J Engineering Next Fixes](PHASE9J_ENGINEERING_NEXT_FIXES.md) | Current engineering blockers and next repairs. |

## Scientific Pipeline

These documents explain the target chromatogram analysis flow.

| Document | Use it for |
|---|---|
| [Pipeline](../PIPELINE.md) | End-to-end processing stages. |
| [Roadmap](../ROADMAP.md) | Project roadmap and near-term development direction. |
| [Autonomous Production Architecture](AUTONOMOUS_PRODUCTION_ARCHITECTURE.md) | Target automatic photo/screenshot-to-report workflow. |
| [Autonomous Analysis Evidence Gates](AUTONOMOUS_ANALYSIS_EVIDENCE_GATES.md) | Gate rules for graph, calibration, trace, peak, model, and export evidence. |
| [Chromatogram Failure Taxonomy](CHROMATOGRAM_FAILURE_TAXONOMY.md) | Failure classes used by runtime and validators. |
| [Chromatogram Graph Layout Taxonomy](CHROMATOGRAM_GRAPH_LAYOUT_TAXONOMY.md) | Layout classes for single graph, stacked traces, TIC+ions, and multi-panel pages. |
| [Regression Dataset](CHROMATOGRAM_REGRESSION_DATASET.md) | Fixture and dataset status. |
| [Regression Matrix](CHROMATOGRAM_REGRESSION_MATRIX.md) | Regression tracking and acceptance matrix. |

## Reports And Evidence

These documents define what a trustworthy output should contain.

| Document | Use it for |
|---|---|
| [Report Specification](../REPORT_SPEC.md) | Target report contract and scientific output requirements. |
| [Report Experience Concept](CHROMALAB_REPORT_EXPERIENCE_CONCEPT.md) | User-facing report information architecture and visual direction. |
| [Autonomous Peak Evidence Model](AUTONOMOUS_PEAK_EVIDENCE_MODEL.md) | Peak evidence requirements and review boundaries. |
| [Assisted Review Fallback Workflow](ASSISTED_REVIEW_FALLBACK_WORKFLOW.md) | User-assisted fallback behavior when automatic evidence is weak. |
| [Assisted Peak Review Workflow](ASSISTED_PEAK_REVIEW_WORKFLOW.md) | Peak review and edit workflow. |

## Local AI And Model Runtime

These documents explain how models are used without becoming numeric authority.

| Document | Use it for |
|---|---|
| [Local AI Runtime](CHROMALAB_LOCAL_AI_RUNTIME.md) | Public runtime strategy and model boundaries. |
| [Gemma LiteRT-LM Model Strategy](GEMMA_LITERTLM_MODEL_STRATEGY.md) | Gemma/LiteRT model catalog and installation strategy. |
| [Android On-Device Model Budget](ANDROID_ON_DEVICE_MODEL_BUDGET.md) | Android memory, performance, and model budget notes. |
| [Desktop Model Runtime Architecture](DESKTOP_MODEL_RUNTIME_ARCHITECTURE.md) | Desktop/local runtime direction. |
| [Knowledge Pack Architecture](CHROMALAB_KNOWLEDGE_PACK_ARCHITECTURE.md) | Local grounded explanation layer. |
| [TV-0/TV-1 TurboVec Knowledge Replacement Foundation](TV0_TV1_TURBOVEC_KNOWLEDGE_REPLACEMENT_FOUNDATION.md) | Knowledge retrieval backend separation, TurboVec fail-closed candidate contract, and benchmark guardrails. |
| [TV-2 TurboVec Knowledge Index Prototype Closeout](TV2_TURBOVEC_KNOWLEDGE_INDEX_PROTOTYPE_CLOSEOUT.md) | PC-only TurboVec MiniLM/BGE index build, lexical-vs-dense benchmark, and TV-3 decision. |
| [TV-3 Retrieval A/B Arbitration Policy Closeout](TV3_RETRIEVAL_AB_ARBITRATION_POLICY_CLOSEOUT.md) | PC-only lexical/dense/hybrid policy benchmark and TV-4 decision. |
| [TV-4 Knowledge Retrieval Backend Promotion Candidate Closeout](TV4_KNOWLEDGE_RETRIEVAL_BACKEND_PROMOTION_CANDIDATE_CLOSEOUT.md) | Kotlin-side hybrid retrieval policy candidate and TV-5 decision. |
| [TV-5 Dense Provider Promotion/Rejection Gate Closeout](TV5_DENSE_PROVIDER_PROMOTION_REJECTION_GATE_CLOSEOUT.md) | Decision gate keeping TurboVec PC/dev-only and lexical as the active runtime retrieval owner. |
| [TV-6 Android Native Feasibility Spike Closeout](TV6_ANDROID_NATIVE_FEASIBILITY_SPIKE_CLOSEOUT.md) | Android Rust target compile feasibility for TurboVec and next on-device load/query gate. |

## Rust CV And Deep Research

These documents describe the research and implementation direction for stronger
graph, axis, scale, and trace extraction.

| Document | Use it for |
|---|---|
| [Deep Research Wave Plan](CHROMATOGRAM_DEEP_RESEARCH_WAVE_PLAN_20260603.md) | Research wave organization. |
| [Analyzer Layer Replacement Roadmap](AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md) | Replacement protocol: inventory, contract, shadow/parity, promotion, retirement. |
| [R1 Graph/Layout Image Prep Contract](R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md) | Contract for replacing Stage 1-3 without adding a permanent duplicate production path. |
| [R1 Graph/Layout Image Prep Parity Plan](R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md) | How the replacement must prove parity before promotion. |
| [R1 Graph/Layout Image Prep Closeout](R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md) | What R1 completed and what R2 must build next. |
| [R2 Stage 1-3 Shadow Parity Harness Closeout](R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md) | PC-side Stage 1-3 parity records and next implementation step. |
| [R3 Image Preparation Candidate Closeout](R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md) | PC-side Stage 1 image-preparation candidate and next Rust parity bridge step. |
| [R4 Rust Stage 1 Image Preparation Parity Closeout](R4_RUST_STAGE1_IMAGE_PREPARATION_PARITY_CLOSEOUT.md) | Rust Stage 1 image-preparation parity records and next graph discovery step. |
| [R5 Stage 2 Graph Discovery Candidate Closeout](R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_CLOSEOUT.md) | PC-side graph-count candidate records and next plotArea/layout step. |
| [R6 Stage 3 PlotArea Layout Candidate Closeout](R6_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_CLOSEOUT.md) | PC-side plotArea/layout candidate records and next axis/frame/scale evidence step. |
| [R7 Stage 4 Axis Frame Scale Candidate Closeout](R7_STAGE4_AXIS_FRAME_SCALE_CANDIDATE_CLOSEOUT.md) | PC-side axis/frame/scale evidence records and next calibration parity step. |
| [R8 Stage 5 Calibration Strategy Parity Closeout](R8_STAGE5_CALIBRATION_STRATEGY_PARITY_CLOSEOUT.md) | PC-side calibration strategy parity records and next automatic OCR anchor step. |
| [R9 Stage 6 Automatic OCR Anchor Candidate Closeout](R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_CLOSEOUT.md) | PC-side automatic OCR anchor records and next runtime/Rust bridge step. |
| [R10 Runtime OCR Anchor Bridge Candidate Closeout](R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_CLOSEOUT.md) | Rust/runtime-shaped OCR anchor bridge records and next integrated calibration step. |
| [R11 Integrated Runtime Calibration Closure Closeout](R11_INTEGRATED_RUNTIME_CALIBRATION_CLOSURE_CLOSEOUT.md) | Shadow calibration strategy closure from R10 bridge rows and next runtime evidence package step. |
| [R12 Runtime Evidence Failure Package Closure Closeout](R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSURE_CLOSEOUT.md) | Runtime evidence/export package closure, manifest checks, and next Android OCR-anchor production bridge step. |
| [R13 Android Runtime OCR Anchor Production Bridge Closeout](R13_ANDROID_RUNTIME_OCR_ANCHOR_PRODUCTION_BRIDGE_CLOSEOUT.md) | Runtime OCR-anchor bridge rows in Android evidence packages and next calibration promotion candidate step. |
| [R14 Runtime Calibration Promotion Candidate Closeout](R14_RUNTIME_CALIBRATION_PROMOTION_CANDIDATE_CLOSEOUT.md) | Android runtime OCR-anchor rows inside calibration arbitration and next graph layout/multi-panel step. |
| [R15 Graph Layout Multi-Panel Runtime Closure Closeout](R15_GRAPH_LAYOUT_MULTI_PANEL_RUNTIME_CLOSURE_CLOSEOUT.md) | Runtime graph-results propagation and explicit unsupported aggregate multi-panel report state. |
| [R15A Multi-Panel Android Evidence Gate](R15A_MULTI_PANEL_ANDROID_EVIDENCE_GATE.md) | Current Android evidence-gate blocker for R15 validation coverage. |
| [End-To-End Gap Audit](CHROMATOGRAM_END_TO_END_GAP_AUDIT_20260603.md) | Full pipeline gaps from image to report. |
| [Graph Layout And Axis Scale Research Handoff](DR1_GRAPH_LAYOUT_AXIS_SCALE_RESEARCH_HANDOFF.md) | Research handoff for graph/layout/axis scale work. |
| [Ground Truth Corpus And Metrics](DRB_GROUND_TRUTH_CORPUS_AND_METRICS.md) | Benchmark and ground-truth direction. |
| [DR-B2 Phase 9J Benchmark Records](DRB2_PHASE9J_BENCHMARK_RECORDS.md) | Phase 9J truth converted into benchmark records. |
| [DR-B3 Benchmark Scoring And Truth Gaps](DRB3_BENCHMARK_SCORING_AND_TRUTH_GAPS.md) | Current scoring gaps and benchmark limitations. |
| [Rust CV Core Foundation](DR2A_RUST_CV_CORE_FOUNDATION.md) | Rust deterministic CV foundation. |

## Development And Orchestration

Use these documents when working in the repository.

| Document | Use it for |
|---|---|
| [Agent Orchestration Pack](agent-orchestration/README.md) | Multi-agent workflow, phase governance, and quality gates. |
| [Agent Skill Selection Protocol](agent-orchestration/protocols/AGENT_SKILL_SELECTION_PROTOCOL.md) | How specialist agents and skills are selected. |
| [Quality Gates](agent-orchestration/protocols/QUALITY_GATES.md) | Validation expectations for phase work. |
| [No Hardcode Policy](agent-orchestration/protocols/NO_HARDCODE_POLICY.md) | Rules against fixture-specific hardcoding. |
| [Report Implementation Plan](../REPORT_IMPLEMENTATION_PLAN.md) | Report implementation planning. |
| [Contributing](../CONTRIBUTING.md) | Contribution rules, setup, validation, and no-overclaiming boundaries. |
| [Security Policy](../SECURITY.md) | Vulnerability reporting and security scope. |
| [Privacy](../PRIVACY.md) | Offline-first data handling, exports, diagnostics, and model-file privacy. |

## Public Presentation And Application Materials

These are useful for repository review and funding/subsidy context. Grant
materials are not a replacement for the public README.

| Document | Use it for |
|---|---|
| [Visual Identity](CHROMALAB_VISUAL_IDENTITY.md) | Logo, color, and identity direction. |
| [Public Messaging Guide](CHROMALAB_PUBLIC_MESSAGING_GUIDE.md) | Repository-safe wording and claims. |
| [Public Repository QA](CHROMALAB_PUBLIC_REPOSITORY_QA.md) | Final public-presentation QA, claim check, link check, and remaining gaps. |
| [OpenAI Subsidy Repository Presentation Plan](OPENAI_SUBSIDY_REPOSITORY_PRESENTATION_PLAN.md) | Public presentation phase plan. |
| [OpenAI Reviewer Start Here](OPENAI_REVIEWER_START_HERE.md) | One-page reviewer entry point for OpenAI/Codex reviewers. |
| [OpenAI Subsidy Summary](OPENAI_SUBSIDY_SUMMARY.md) | Concise reviewer-ready subsidy, API credit, and Codex Security summary. |
| [OpenAI Subsidy Application Narrative](OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md) | Grant/application source text, not general README copy. |

## Internal Phase Archive

The repository contains many `PHASE*`, `DR*`, and `research/*` files. They are
valuable audit history, but they should not be the first entry point for public
readers.

Use the archive when you need:

- exact Android validation phase history;
- root-cause investigations;
- regression closeout details;
- deep research source notes;
- implementation handoff records.

Recommended archive paths:

- `docs/PHASE*.md`
- `docs/DR*.md`
- `docs/research/*.md`
- `artifacts/`

## Current Public Documentation Gaps

The public repository presentation still needs real screenshots only when they
show current app output honestly.

Do not fill remaining gaps with placeholder claims. Add real screenshots only
when they show current app output without masking review or blocked states.
