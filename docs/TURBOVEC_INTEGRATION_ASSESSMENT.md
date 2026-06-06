# TurboVec Integration Assessment

Date: 2026-06-06

Status: `RESEARCH_CANDIDATE`

## Summary

TurboVec is a candidate retrieval backend for ChromaLab's local Knowledge Pack and
developer-side RAG experiments. It is not a chromatogram analysis algorithm and
must not be wired into graph detection, axis calibration, trace extraction, peak
metrics, or report gates.

The right integration path is:

1. keep the existing lexical `KnowledgeRetrievalEngine` as the default and safety
   baseline;
2. prototype TurboVec as an optional dense reranker for local Knowledge Pack
   entries on desktop/dev machines;
3. benchmark retrieval quality, memory, latency, index size, and citation
   correctness;
4. consider Android/native integration only after dependency, packaging, and
   safety gates pass.

## Sources Reviewed

| Source | What was checked | Integration impact |
|---|---|---|
| [RyanCodrai/turbovec](https://github.com/RyanCodrai/turbovec) | Rust vector index with Python bindings, MIT license, local search, SIMD kernels, LangChain/LlamaIndex/Haystack/Agno integrations. | Useful as a local dense retrieval candidate, not as a numeric chromatogram pipeline component. |
| [TurboVec API reference](https://github.com/RyanCodrai/turbovec/blob/main/docs/api.md) | `TurboQuantIndex`, `IdMapIndex`, `allowlist`, `.tv`, `.tvim`, stable external ids. | `IdMapIndex` maps well to ChromaLab `KnowledgeEntry.entryId` if we prototype dense retrieval. |
| [TurboVec PyPI](https://pypi.org/project/turbovec/) | `turbovec 0.7.0`, released 2026-05-30, Python >= 3.9, alpha classifier, optional framework extras. | Good for PC-side prototype; alpha status blocks immediate production Android adoption. |
| [TurboVec crates.io](https://crates.io/crates/turbovec) | Rust crate availability; latest crate version observed as 0.8.0 on 2026-06-06. | Rust path may become more relevant for our Rust CV/tooling direction than Python-only app integration. |
| [Google Research TurboQuant blog](https://research.google/blog/turboquant-redefining-ai-efficiency-with-extreme-compression/) | TurboQuant targets vector quantization for vector search and KV cache compression. | Confirms the underlying algorithm is relevant to retrieval/memory, not chromatogram measurement. |
| [TurboQuant arXiv paper](https://arxiv.org/abs/2504.19874) | Online vector quantization with near-optimal distortion rate and nearest-neighbor search claims. | Supports a research prototype, but ChromaLab still needs local benchmark proof before adoption. |

## What TurboVec Is

TurboVec is an approximate vector index built on the TurboQuant vector
quantization idea. It stores embedding vectors in a compressed representation and
searches them locally. It supports:

- Rust and Python APIs;
- 2-bit and 4-bit compressed indexes;
- stable external ids through `IdMapIndex`;
- search-time filtering through allowlists;
- local persistence through `.tv` and `.tvim` files;
- framework adapters for LangChain, LlamaIndex, Haystack, and Agno.

TurboVec still needs embeddings from another model. It does not create
embeddings, parse chromatogram images, read axes, calibrate charts, extract
traces, integrate peaks, or validate scientific reports.

## Where It Fits In ChromaLab

ChromaLab already has a local/offline-first Knowledge Pack:

- runtime contracts: `KnowledgeEntry`, `KnowledgeSearchQuery`,
  `KnowledgeSearchResult`, `KnowledgeRetrievalContext`;
- current search: lexical BM25-style ranking in `KnowledgeRetrievalEngine`;
- safety contract: retrieved entries may ground OCR/semantic/warning/report
  explanations, but they cannot create numeric chromatographic evidence.

TurboVec can fit as an optional backend behind a retrieval abstraction:

```text
KnowledgeSearchQuery
    -> lexical candidate filter (existing KnowledgeRetrievalEngine)
    -> optional dense rerank (TurboVec IdMapIndex)
    -> KnowledgeRetrievalContext with used entry ids
    -> bounded snippets for E2B/E4B semantic prompts
```

The lexical engine should remain in the path even if TurboVec is enabled. It
provides deterministic filters for entry type, allowed use, language, exact
aliases, and safety boundaries. TurboVec can rerank or expand candidates, but it
must not bypass `KnowledgeUsePolicyValidator`.

## Proposed Integration Phases

### TV-0: No-runtime adoption decision

- Do not add TurboVec to Android/KMP runtime yet.
- Do not add Python runtime dependencies to the Android app.
- Do not replace `KnowledgeRetrievalEngine`.
- Record TurboVec as a research candidate only.

Exit gate: this document exists and the product boundary is clear.

### TV-1: Desktop prototype indexer

Build a local script/tool outside Android runtime that:

- reads `docs/knowledge/chromalab_knowledge_seed_v2.json`;
- assigns stable `u64` ids to `KnowledgeEntry.entryId` values;
- embeds only bounded search text with a local embedding model;
- writes an `IdMapIndex` `.tvim` file;
- writes a JSON sidecar mapping `u64 -> entryId`;
- never sends private chromatogram images, logs, or reports to a cloud service.

Exit gate: index builds repeatably on PC and can be deleted/rebuilt.

### TV-2: Retrieval A/B benchmark

Compare the current lexical engine with TurboVec-assisted retrieval on reviewed
queries:

- OCR text classification;
- ion/channel/title rejection;
- warning explanation;
- report caveat retrieval;
- Phase 9 failure-taxonomy lookup;
- Knowledge citation rendering.

Metrics:

- top-k hit rate against expected `entryId`;
- citation correctness and `used_entry_ids`;
- latency;
- memory use;
- index size;
- rebuild time;
- unsupported claim rate when used in model prompts.

Exit gate: dense retrieval improves at least one reviewed semantic task without
increasing forbidden-use or missing-citation failures.

### TV-3: Kotlin/Rust abstraction

If TV-2 passes, add a backend interface around retrieval:

```text
KnowledgeRetrievalBackend
    LexicalKnowledgeRetrievalBackend
    OptionalDenseKnowledgeRetrievalBackend
```

Required behavior:

- lexical backend remains default;
- dense backend is optional;
- all results still pass `KnowledgeUsePolicyValidator`;
- every dense result records index version, embedding model id, bit width, and
  sidecar version;
- dense retrieval can be disabled without changing report behavior.

Exit gate: tests prove dense retrieval cannot produce forbidden numeric evidence
or uncited model explanations.

### TV-4: Android feasibility review

Only after desktop A/B proof, evaluate Android packaging:

- Rust crate compatibility with Android NDK targets;
- BLAS/linking implications from dependencies;
- JNI/KMP bridge size and maintenance cost;
- index file size for the curated Knowledge Pack;
- app-private storage policy;
- upgrade/delete behavior;
- offline rebuild vs bundled prebuilt index.

Exit gate: Android native proof-of-concept loads an index from app-private
storage, executes deterministic queries, and preserves existing report gates.

## What Not To Do

- Do not use TurboVec for graph detection.
- Do not use TurboVec for axis/tick pixel localization.
- Do not use TurboVec for calibration coefficients.
- Do not use TurboVec for RT, height, area, FWHM, S/N, baseline, Kovats, or
  compound identification.
- Do not replace deterministic validators with dense similarity scores.
- Do not add cloud embeddings or remote vector search.
- Do not ship TurboVec in Android production while the integration is unbenchmarked.

## Safety Requirements

Any TurboVec-backed retrieval must preserve the existing Knowledge Pack contract:

- retrieved snippets are bounded;
- model prompts include `entry_id`, version, allowed use, forbidden use, and
  source refs;
- model outputs must return `used_entry_ids`;
- missing `used_entry_ids` remains REVIEW;
- forbidden numeric use remains rejected;
- report gates remain controlled by deterministic evidence and validators.

## Decision

Adopt TurboVec as a research candidate for local Knowledge Pack dense retrieval,
starting with a PC-side prototype and benchmark. Do not integrate it into Android
runtime or chromatogram analysis calculations until benchmark and packaging gates
pass.

## Deep Audit Follow-Up

The broader modernization plan is documented in
`docs/TURBOVEC_DEEP_AUDIT_AND_MODERNIZATION_PLAN.md`.

That follow-up separates two tracks:

- TurboVec/TurboQuant as a local retrieval and research-memory accelerator;
- Rust deterministic CV and evidence-gated validation as the real path for
  improving graph, axis, calibration, trace, and peak analysis.
