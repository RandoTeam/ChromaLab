# ChromaLab Knowledge Pack Architecture

The ChromaLab Knowledge Pack is a local/offline-first retrieval layer for semantic VLM/OCR assistance. It exists to reduce hallucination in text classification, warning explanations, and report caveats. It does not measure chromatograms.

## V1 Scope

- Seed file: `docs/knowledge/chromalab_knowledge_seed_v1.json`.
- Runtime contracts: `KnowledgeEntry`, `KnowledgeEntryType`, `KnowledgeSourceRef`, `KnowledgeUsePolicy`, `KnowledgeSearchQuery`, `KnowledgeSearchResult`, `KnowledgeRetrievalContext`, `KnowledgePackVersion`.
- Search: local lexical BM25-style ranker in `KnowledgeRetrievalEngine`; future Android persistence may use SQLite FTS5/Room.
- Seed content: glossary terms, text classification rules, report caveats, retention-index rules, ion/channel terminology, conservative compound-class naming patterns, safety boundaries, and prompt snippets.

## Allowed Use

- Ground VLM warning explanations.
- Improve OCR/VLM text classification.
- Explain why a result is REVIEW/DIAGNOSTIC.
- Provide source-referenced caveats.
- Build bounded snippets for semantic prompts.

## Forbidden Use

Knowledge must never fabricate or override:

- RT, height, area, FWHM, S/N, baseline, Kovats, or calibration coefficients.
- Graph geometry, axis/tick pixels, curve masks, trace points, integration boundaries, or peak metrics.
- Compound identity without explicit evidence.

## VLM Retrieval Contract

When knowledge is supplied to a VLM, pass only bounded snippets:

- `entry_id`
- `version`
- `short_text`
- `allowed_use`
- `forbidden_use`
- `source_ref`

The VLM output must return:

- `used_entry_ids`
- `decision`
- `confidence`
- `unsupported_claims`
- `explanation`

Missing `used_entry_ids` marks the result REVIEW. Forbidden use is rejected and recorded in the runtime evidence package.
# Phase 6C v2 Expansion

Date: 2026-05-20

The Knowledge Pack now has a v2 seed and a source-controlled acquisition scaffold.

## Runtime Shape

- `KnowledgeEntry` includes canonical label, language, license status, trust tier, confidence, last-reviewed date, tags, allowed uses, and forbidden uses.
- `KnowledgeEntryType` now covers known patterns, unit terms, axis terms, mass-spectrometry terms, chromatography method terms, and compound-reference stubs.
- Retrieval supports exact aliases, lexical matching, type filtering, language filtering, and allowed-use filtering.
- Results return source references and forbidden-use policy so VLM prompts can carry bounded snippets only.

## Safety Boundary

Knowledge entries are semantic aids. They cannot generate measured RT, height, area, FWHM, S/N, baseline, Kovats, calibration coefficients, integration boundaries, or compound identities. Any VLM output that uses knowledge for those purposes is rejected by policy.

## Pack Versions

- v1 remains a small Phase 6B seed.
- v2 is the expanded Phase 6C seed for autonomous semantic grounding and report caveats.

## Phase 6C-2 Rules + Retrieval + Provenance

The architecture now separates:

- deterministic rules: `KnowledgeRuleEngine`;
- retrieval: `KnowledgeRetrievalEngine`;
- provenance: source refs, source tiers, claim scopes, and attribution manifests.

Compact retrieval cards are optimized for Gemma E4B/E2B semantic prompts and include only bounded fields. Rule classifications run before VLM use for adversarial OCR cases.

## TurboVec Dense Retrieval Candidate

TurboVec is tracked as a research candidate for optional local dense retrieval,
documented in `docs/TURBOVEC_INTEGRATION_ASSESSMENT.md`.

The current decision is replacement-gated:

- keep `KnowledgeRetrievalEngine` as a backwards-compatible retrieval facade;
- keep `LexicalKnowledgeRetrievalBackend` as the current active ranking owner;
- keep `TurboVecKnowledgeRetrievalBackend` fail-closed as a shadow-unavailable
  candidate until a local index and benchmark pass;
- prototype TurboVec only as a PC-side dense reranker over curated Knowledge Pack
  entries;
- require local embeddings, stable entry-id mapping, retrieval benchmarks, and
  citation-safety tests before any Android runtime dependency is considered;
- never let vector similarity create or override graph geometry, calibration,
  trace data, peak metrics, report gates, or compound identity.

TV-0/TV-1 foundation work is documented in
`docs/TV0_TV1_TURBOVEC_KNOWLEDGE_REPLACEMENT_FOUNDATION.md`.
