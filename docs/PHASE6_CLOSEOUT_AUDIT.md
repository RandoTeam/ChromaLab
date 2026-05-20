# Phase 6 Closeout Audit

Date: 2026-05-20

Verdict: `PHASE_6_BLOCKED`

Phase 6 has strong contract, validator, Knowledge Pack, and documentation coverage, but it is not fully closed because runtime producers do not yet populate the new multimodal stage evidence, knowledge-grounded VLM outputs, and per-call runtime budget profiles in real analysis packages. Closing Phase 6 now would overstate runtime integration.

## Task Classification

- Research / current methods
- VLM / OCR / local model inference
- Runtime evidence / validator
- Scientific reporting / provenance
- Android performance / memory / timeout policy
- Security / privacy / offline packaging
- QA / regression / product acceptance

## Agents Activated

- Orchestrator
- Research Intelligence Agent
- QA / Regression Agent
- Product Acceptance Agent
- VLM Evaluation Agent
- OCR / VLM / Text Semantics Agent
- Android Performance & On-Device AI Agent
- Scientific Reporting & Validation Agent
- Chromatography SME Agent
- Security & Privacy Agent
- Accessibility & Localization Agent

## Skills Used

- current-web-research-deep
- source-quality-triage
- research-synthesis
- vlm-safe-assistant
- structured-vlm-json-contract
- vlm-hallucination-audit
- ocr-local-crops
- ocr-crop-benchmark
- on-device-model-budgeting
- timeout-cache-design
- thermal-memory-guardrails
- evidence-package-validator
- scientific-report-provenance
- evidence-gated-reporting
- audit-trail-design
- golden-artifact-testing
- real-device-validation
- definition-of-done

## Current Source Check

Research note: `docs/research/2026-05-20_phase6_closeout_audit.md`

Sources checked:

- Android LiteRT documentation: `https://developer.android.com/ai/custom`
- ML Kit Text Recognition v2 Android documentation: `https://developers.google.com/ml-kit/vision/text-recognition/v2/android`
- W3C PROV-O Recommendation: `https://www.w3.org/TR/prov-o/`
- SQLite FTS5 documentation: `https://www.sqlite.org/fts5.html`

## Checklist

| Requirement | Status | Evidence files | Tests | Blockers | Action needed |
|---|---|---|---|---|---|
| Autonomous multimodal intelligence layer | `PARTIAL` | `docs/PHASE6_AUTONOMOUS_MULTIMODAL_INTELLIGENCE.md`, `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/multimodal/` | `AutonomousStageJudgeContractsTest` | Contracts exist, but real runtime producers do not yet emit full stage judge evidence. | Wire VLM/OCR/CV runtime paths to produce stage judge results and evidence package rows. |
| VLM/OCR/CV stage judge contracts | `DONE` | `AutonomousStageJudgeContracts.kt`, `VLM_OCR_STAGE_JUDGE_CONTRACTS.md` | `AutonomousStageJudgeContractsTest` | None. | Keep as Phase 7 input. |
| Structured VLM JSON boundaries | `DONE` | `VlmStructuredTaskContracts.kt`, `VLM_JSON_SCHEMA_BOUNDARIES.md` | `vlmForbiddenNumericFieldsAreRejected` | None. | Ensure runtime parser uses the boundary policy before accepting model output. |
| OCR/VLM local crop benchmark harness | `DONE` | `OcrVlmCropBenchmark.kt`, `OCR_VLM_CROP_BENCHMARK.md` | `cropBenchmarkExportsJsonAndMarkdown` | None for harness. | Add larger real crop datasets in later benchmark phases. |
| Model runtime budget / timeout / cache policy | `PARTIAL` | `ModelRuntimeProfile`, `ANDROID_ON_DEVICE_MODEL_BUDGET.md`, `VlmStructuredTaskContracts.kt` | `validatorRecordsTimeoutProfile` | Policy and schema exist, but active Android model calls do not yet emit complete `ModelRuntimeProfile` rows or cache-hit evidence into runtime packages. | Instrument `ActiveVisionModelBackend`, OCR/VLM fallback calls, and any stage judge execution with profile ids, duration, timeout, success/failure, backend, and cache status. |
| Runtime evidence package integration | `PARTIAL` | `RuntimeEvidencePackage.kt`, `RuntimeEvidencePackageValidator.kt` | `RuntimeEvidencePackageValidatorTest` | Schema and validator support exist, but `RuntimeEvidencePackageBuilder` still emits empty stage judge, OCR/VLM crop, overlay judge, and knowledge-output lists for normal reports. | Extend runtime/report mappers to preserve actual multimodal evidence and knowledge retrieval contexts. |
| Validator checks for VLM/OCR/knowledge usage | `DONE` | `RuntimeEvidencePackageValidator.kt`, `KnowledgeRetrievalModels.kt` | `RuntimeEvidencePackageValidatorTest`, `KnowledgePackV2Test` | None. | Keep validator strict; do not weaken to close Phase 6. |
| Gemma model strategy | `DONE` | `GEMMA_LITERTLM_MODEL_STRATEGY.md` | Documentation audit | None. | Keep `Gemma-4-E4B LiteRT-LM` as FULL_ANALYSIS primary and `Gemma-4-E2B LiteRT-LM` as FAST/fallback. |
| Local Knowledge Pack v2 | `DONE` | `CHROMALAB_KNOWLEDGE_PACK_ARCHITECTURE.md`, `docs/knowledge/chromalab_knowledge_seed_v2.json`, `tools/knowledge-builder/` | `KnowledgePackV2Test`, `KnowledgeBuilderArtifactTest` | None for v2. | Do not add large external databases without license/source review. |
| Legacy/base knowledge pack licensing alignment | `PARTIAL` | `ChromaLabBaseKnowledgePack.kt`, `LocalKnowledgePackModels.kt`, `LocalKnowledgePackValidator.kt` | `ChromaLabBaseKnowledgePackTest`, `LocalKnowledgePackSchemaTest` | The legacy/base pack still contains NIST/WebBook-derived source and RI seed entries without Phase 6C source-tier/license-status enforcement or a dedicated attribution manifest. | Reconcile legacy/base pack with Phase 6C policy: add source tier/license fields and validator rules, move restricted NIST/WebBook-derived entries behind link-only/review status, or explicitly deprecate/exclude the legacy pack from production. |
| Source tiers, claim policy, rule engine, adversarial tests | `DONE` | `KNOWLEDGE_PACK_SOURCE_TIERS.md`, `KNOWLEDGE_PACK_CLAIM_POLICY.md`, `KNOWLEDGE_PACK_RULE_ENGINE.md`, `KNOWLEDGE_PACK_ADVERSARIAL_TESTS.md`, `KnowledgeRuleEngine.kt` | `KnowledgePackV2Test` | None. | Keep Knowledge Pack as rules + retrieval + provenance, not measurement. |
| VLM/Knowledge cannot create numeric metrics | `DONE` | `VLM_JSON_SCHEMA_BOUNDARIES.md`, `KNOWLEDGE_PACK_CLAIM_POLICY.md`, `RuntimeEvidencePackageValidator.kt` | `validatorFailsForbiddenVlmNumericOutput`, `knowledgeCannotCreateMetricsAndVlmMustCiteEntries`, `forbiddenCompoundAndKovatsUsesAreRejected` | None. | Maintain block on RT, height, area, FWHM, S/N, baseline, Kovats, calibration, and peak metrics. |
| No cloud dependency / no bundled proprietary database | `DONE` | `KNOWLEDGE_PACK_SOURCE_POLICY.md`, `KNOWLEDGE_PACK_LICENSE_REGISTER.md`, `tools/knowledge-builder/sources.yaml` | `KnowledgeBuilderArtifactTest` | None. | Continue offline-first; keep PubChem/NIST/AMDIS restricted until license review. |
| Real Android runtime evidence validation | `MISSING` | `PHASE6_CLOSEOUT_REPORT.md` notes this as required before runtime success claims | Not run in this audit | No current real Android package proves stage judge/model profile/knowledge evidence is populated end-to-end. | Run real-device analysis after runtime wiring and validate exported evidence package. |

## Blocking Items

1. `RuntimeEvidencePackageBuilder` and current runtime/report mappers do not yet populate multimodal evidence lists for normal analysis:
   - `stageJudgeResults`
   - `ocrVlmCropResults`
   - `ocrVlmDisagreements`
   - `overlayJudgeResults`
   - `knowledgeGroundedVlmOutputs`
   - package-level `knowledgeRetrievalContexts`
   - package-level `modelRuntimeProfiles`

2. Active Android VLM/OCR call sites are not yet instrumented to emit complete `ModelRuntimeProfile` data:
   - model id;
   - runtime backend;
   - duration;
   - timeout;
   - success/failure;
   - cache hit/miss;
   - memory/thermal metadata when available.

3. Active VLM parsing and optional judge paths are not fully wired to the Phase 6 boundary contracts:
   - `ActiveVisionModelBackend.android.kt` still parses VLM crop output directly and does not apply `ForbiddenVlmBoundaryPolicy` before accepting semantic crop output;
   - overlay judge and warning summary paths are still interface-level/nullable rather than populated evidence rows;
   - VLM-first axis OCR paths require stricter provenance before any text position can participate in calibration.

4. Legacy/base Knowledge Pack licensing is not aligned with the stricter Phase 6C v2 policy. `ChromaLabBaseKnowledgePack.kt` still defines NIST/WebBook sources and RI entries through the older `LocalKnowledgePackModels.kt` schema, which lacks license status, trust tier, claim scope, and fail-closed restricted-source validation.

5. No real Android evidence package has been validated after Phase 6C showing the new multimodal and Knowledge Pack evidence populated end-to-end.

## Non-Blocking Residual Risks

- The OCR/VLM crop benchmark harness is seeded only with small contract tests; larger real crop fixtures are still needed.
- Knowledge Pack v2 remains an in-memory/local JSON seed; SQLite FTS5/Room FTS is documented as a future packaging path, not required for the current pack size.
- VLM runtime behavior was intentionally not changed in this audit.
- The v2 source register records ChEBI as attribution-required and future-candidate; a dedicated attribution manifest should be generated before any broader ChEBI-derived data import.

## Decision

Phase 6 must remain blocked until the five blocking items above are completed or the phase contract is explicitly narrowed. Phase 7 must not start from this audit state.

## Deep Unblock Follow-Up

Date: 2026-05-20

Follow-up plan and blocker matrix:

- `docs/PHASE6_DEEP_UNBLOCK_PLAN.md`
- `docs/PHASE6_BLOCKER_MATRIX.md`

The deep unblock work closed the code-level blockers for runtime evidence population, local crop VLM runtime profiles, structured VLM boundary enforcement, validator enforcement for VLM-backed peak-label evidence, and legacy/base Knowledge Pack source-policy alignment.

The only remaining item from this audit is real Android device package validation. Product Acceptance approved deferring that item to the real-device validation gate because this desktop repository context cannot generate a physical-device evidence package. The deferral does not permit release/device-performance claims and remains mandatory before release validation.

Superseding closeout verdict is recorded in `docs/PHASE6_CLOSEOUT_REPORT.md`.
