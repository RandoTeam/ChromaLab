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
