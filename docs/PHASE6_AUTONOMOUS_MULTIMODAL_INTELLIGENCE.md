# Phase 6: Autonomous Multimodal Intelligence Layer

Phase 6 adds an evidence layer for VLM/OCR/CV cooperation without making VLM a numeric measurement engine.

## Ownership

- CV owns graphPanel, plotArea, axis/tick pixels, trace masks, and curve extraction evidence.
- OCR owns local crop text reading for ticks, axis labels, title/channel text, and peak annotations.
- VLM owns hard crop OCR fallback, semantic classification, overlay judging, retry explanations, and warning summaries.
- Deterministic math owns calibration, RT, height, area, FWHM, S/N, baseline, Kovats, and peak integration.

## Implemented In This Slice

- Shared stage judge contracts.
- VLM JSON boundary policy with forbidden numeric field rejection.
- Stage retry recommendation policy.
- Runtime evidence package fields for stage judges, crop results, disagreements, overlay judgements, and model runtime profiles.
- Runtime evidence validator checks for forbidden VLM numeric fields, crop provenance, timeout profiles, and forbidden retry actions.
- OCR/VLM crop benchmark harness with JSON and Markdown export.
- ChromaLab local Knowledge Pack v1 contracts, seed JSON, lexical retrieval, VLM citation/use policy, and validator checks.

## Knowledge Pack Workstream

The Knowledge Pack improves semantic reliability without changing numeric analysis.

- Primary FULL_ANALYSIS model strategy: Gemma-4-E4B LiteRT-LM for hard crop OCR, text classification, overlay judging, warning explanation, and report grounding.
- FAST/fallback strategy: Gemma-4-E2B LiteRT-LM for weaker devices and shorter semantic tasks.
- Seed file: `docs/knowledge/chromalab_knowledge_seed_v1.json`.
- Runtime contracts: `KnowledgeEntry`, `KnowledgeEntryType`, `KnowledgeSourceRef`, `KnowledgeUsePolicy`, `KnowledgeSearchQuery`, `KnowledgeSearchResult`, `KnowledgeRetrievalContext`, and `KnowledgePackVersion`.
- V1 retrieval: local/offline lexical BM25-style search. SQLite FTS5/Room is the planned persistent Android implementation path.
- VLM explanations must cite `used_entry_ids`. Missing citations are REVIEW; forbidden use is rejected.
- Knowledge can classify and explain. It cannot create RT, height, area, FWHM, S/N, baseline, Kovats, calibration, integration, or compound identity.

## Out Of Scope

- Changing `ActiveVisionModelBackend` behavior.
- Changing ML Kit OCR behavior.
- Changing graph/trace/peak algorithms.
- Manual review as primary workflow.
- Any CalculationEngine changes.
- Cloud retrieval or large external chemical database bundling.

## Evidence Required

Every multimodal stage result must include task type, source, verdict, confidence, linked evidence ids, crop or overlay path when applicable, warnings, and runtime profile for VLM-backed work.
# Phase 6C Addendum: Local Knowledge Pack Expansion

Date: 2026-05-20

The Phase 6 multimodal layer now includes a local/offline Knowledge Pack v2. It is used to ground semantic VLM/OCR tasks and report caveats without making the model a measurement engine.

Integration rules:

- VLM receives bounded retrieved snippets only.
- VLM output must cite `used_entry_ids`.
- Unsupported claims produce REVIEW/REJECTED.
- Any attempt to use knowledge for numeric chromatographic metrics is rejected.
- No cloud dependency or hidden remote lookup is introduced.
- Source/license policy controls future external data acquisition.

The Knowledge Pack helps classify text such as `Ion 71.00 (70.70 to 71.70)` as title/channel metadata and helps explain why calibration, trace, peak, Kovats, or compound-identification claims are blocked when evidence is missing.
