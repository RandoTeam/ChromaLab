# Knowledge Pack Rule Engine

Date: 2026-05-20

Phase 6C-2 adds a deterministic rule layer beside retrieval. The Knowledge Pack is now:

- rules;
- retrieval;
- provenance.

It is not only RAG.

## Rule Responsibilities

The rule layer classifies text and emits caveats before any VLM interpretation:

- ion/mz/channel/title text versus peak annotation;
- forbidden-use checks;
- report caveats;
- release-gate caveats.

## Text Classification Rules

| Input class | Deterministic result |
|---|---|
| `Ion 71.00 (70.70 to 71.70)` | `TITLE_OR_CHANNEL` |
| `m/z 57` | `ION_CHANNEL_TERM` |
| `SIM 85` | `METHOD_METADATA` |
| `5.610` near apex with local signal verification | `PEAK_ANNOTATION_CANDIDATE` |
| `5.610` in title/header | `NOT_PEAK_ANNOTATION` |

Peak annotation candidate does not mean reportable peak. It only permits downstream signal verification. Deterministic trace/peak evidence remains the source of numeric truth.

## Caveat Rules

- Missing calibration produces the calibration-required caveat.
- Missing trace produces the trace-required caveat.
- Kovats without same-method reference series produces the no-Kovats caveat.
- Compound names without spectral/RI/library/user evidence produce the no-compound-assignment caveat.

## Enforcement

The runtime rule engine is `KnowledgeRuleEngine`. Tests live in `KnowledgePackV2Test`.
