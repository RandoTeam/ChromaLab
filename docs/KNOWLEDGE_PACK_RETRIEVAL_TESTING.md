# Knowledge Pack Retrieval Testing

Date: 2026-05-20

## Required Behaviors

- Exact alias lookup returns the expected entry.
- Lexical query returns deterministic top-K results.
- Type filtering restricts result entry types.
- Language filtering restricts result languages.
- Allowed-use filtering restricts results to entries usable for the requested purpose.
- Search results preserve forbidden-use policy and source references.
- Ion/channel title text retrieves the title/channel rule, not peak annotation.
- Kovats and compound-assignment caveats are retrievable.
- Knowledge-grounded VLM explanations must cite used entry IDs.
- Unsupported claims produce REVIEW/REJECTED.

## Current Test Coverage

- `KnowledgeRetrievalLayerTest`
- `KnowledgePackV2Test`
- `ChromaLabKnowledgeSeedFileTest`
- `KnowledgeBuilderArtifactTest`

## Validation Philosophy

Retrieval is allowed to supply bounded snippets for semantic grounding only. It cannot create RT, height, area, FWHM, S/N, baseline, Kovats, or final calibration/integration values. Validator tests must continue to fail any knowledge or VLM output that tries to create numeric chromatographic metrics.
