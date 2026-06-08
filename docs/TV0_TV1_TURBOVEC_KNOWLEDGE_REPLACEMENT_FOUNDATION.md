# TV-0/TV-1 TurboVec Knowledge Replacement Foundation

Date: 2026-06-08

Status: `TV0_TV1_FOUNDATION_COMPLETE`

Scope: Knowledge retrieval owner separation, benchmark guardrails, and
TurboVec dependency gate. This slice does not add TurboVec as an Android/KMP
runtime dependency, does not modify `CalculationEngine`, and does not change
chromatographic geometry, calibration, trace, peak metrics, model authority, or
report gates.

## External Dependency Gate

TurboVec remains a promising but gated candidate:

| Source | Current observed fact | Product decision |
|---|---|---|
| PyPI | `turbovec 0.7.0`, released 2026-05-30, MIT, Python >= 3.9, alpha classifier. | PC prototype only; alpha status blocks direct Android production adoption. |
| docs.rs | Rust crate documentation shows `turbovec 0.8.0`; API exports `TurboQuantIndex` and `IdMapIndex`. | Rust path is viable for later local prototype, but must be benchmarked before promotion. |
| GitHub README | Local Rust/Python vector index, `.tv` / `.tvim`, stable ids, allowlist filtering, no cloud service. | Useful for Knowledge retrieval ranking/storage, not image analysis or metrics. |

TurboVec still requires a separate local embedding model. No embedding model is
selected or bundled in this slice.

## Active Owner After This Slice

| Responsibility | Owner after TV-0/TV-1 | Notes |
|---|---|---|
| Knowledge retrieval facade | `KnowledgeRetrievalEngine` | Backwards-compatible entry point for existing callers. |
| Active ranking backend | `LexicalKnowledgeRetrievalBackend` | Existing BM25-style lexical behavior moved behind a named backend. |
| Dense candidate backend | `TurboVecKnowledgeRetrievalBackend` | Shadow-unavailable/fail-closed contract only; returns no results until benchmark promotion. |
| Policy and safety | `KnowledgeUsePolicyValidator`, `KnowledgeRuleEngine` | Non-replaceable safety gates. |

The old lexical ranker is no longer hidden inside the facade. It is now a named
backend that can be compared, promoted, demoted, or retired according to the
replacement protocol.

## Contract Additions

Added retrieval metadata so future dense results cannot be silent:

- `KnowledgeRetrievalBackendId`
- `KnowledgeRetrievalSafetyStatus`
- `KnowledgeEmbeddingModelManifest`
- `KnowledgeDenseIndexManifest`
- `KnowledgeRetrievalDiagnostics`

`KnowledgeSearchResult` and `KnowledgeRetrievalContext` now expose backend id,
index version, retrieval stage, safety status, diagnostics, and `usedEntryIds`.

## Benchmark Guardrails

The first goldens are source-controlled as unit tests. They verify that current
lexical retrieval still finds expected Knowledge entries for:

- S/N / signal-to-noise;
- Ion 71 title/channel text classification;
- SIM selected-ion-monitoring semantics;
- peak label signal-verification rule;
- Kovats caveat;
- compound-assignment caveat;
- knowledge-cannot-create-metrics safety boundary.

TurboVec candidate behavior is fail-closed: until a local index and embedding
benchmark pass, `TurboVecKnowledgeRetrievalBackend` returns no snippets and marks
diagnostics as `SHADOW_UNAVAILABLE`.

## What Was Not Done

- No TurboVec crate or Python package was added to Android/KMP runtime.
- No local embedding model was selected, downloaded, or bundled.
- No dense index was generated.
- No report or analyzer behavior was changed.
- No Phase 9 acceptance claim was made.

## Next TurboVec Slice

The next TurboVec-specific implementation slice is:

```text
TV-2 - PC TurboVec Knowledge Index Prototype
```

Required work:

1. choose a local/offline embedding model candidate;
2. build a PC-only indexer over `docs/knowledge/chromalab_knowledge_seed_v2.json`;
3. write `.tvim` plus a stable `u64 -> entryId` sidecar;
4. record latency, memory, index size, top-k ids, and unsupported-claim checks;
5. keep Android runtime unchanged until TV-3/TV-6 gates pass.

The analyzer runtime phase sequence is still blocked at R15A Android evidence
gate; TurboVec work must not be used to bypass that blocker.
