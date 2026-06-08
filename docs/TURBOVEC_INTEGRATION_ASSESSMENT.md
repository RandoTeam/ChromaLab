# TurboVec Integration Assessment

Date: 2026-06-08

Status: `TV2_PC_INDEX_PROTOTYPE_COMPLETE`

## Summary

TurboVec is a candidate retrieval backend for ChromaLab's local Knowledge Pack and
developer-side RAG experiments. It is not a chromatogram analysis algorithm and
must not be wired into graph detection, axis calibration, trace extraction, peak
metrics, or report gates.

The right integration path is replacement-gated, not permanently additive:

1. keep the existing lexical `KnowledgeRetrievalEngine` as the active safety
   baseline while research is running;
2. prototype TurboVec only in shadow/parity mode for local Knowledge Pack
   retrieval;
3. benchmark retrieval quality, memory, latency, index size, citation
   correctness, and forbidden-use behavior;
4. promote TurboVec only if it becomes the single active retrieval owner behind
   the existing policy/rule gates;
5. retire or clearly demote the old ranker after promotion so the app does not
   accumulate confusing parallel retrieval paths.

## Sources Reviewed

| Source | What was checked | Integration impact |
|---|---|---|
| [RyanCodrai/turbovec](https://github.com/RyanCodrai/turbovec) | Rust vector index with Python bindings, MIT license, local search, SIMD kernels, LangChain/LlamaIndex/Haystack/Agno integrations. | Useful as a local dense retrieval candidate, not as a numeric chromatogram pipeline component. |
| [TurboVec API reference](https://github.com/RyanCodrai/turbovec/blob/main/docs/api.md) | `TurboQuantIndex`, `IdMapIndex`, `allowlist`, `.tv`, `.tvim`, stable external ids. | `IdMapIndex` maps well to ChromaLab `KnowledgeEntry.entryId` if we prototype dense retrieval. |
| [TurboVec PyPI](https://pypi.org/project/turbovec/) | `turbovec 0.7.0`, released 2026-05-30, Python >= 3.9, alpha classifier, optional framework extras. | Good for PC-side prototype; alpha status blocks immediate production Android adoption. |
| [TurboVec docs.rs](https://docs.rs/turbovec/latest/turbovec/) | Rust crate documentation observed as `turbovec 0.8.0`; exports `TurboQuantIndex` and `IdMapIndex`. | Rust path may become more relevant for our Rust tooling direction than Python-only integration, but still requires local benchmark proof. |
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
- current facade: `KnowledgeRetrievalEngine`;
- current active ranking backend: `LexicalKnowledgeRetrievalBackend`;
- current dense candidate: fail-closed `TurboVecKnowledgeRetrievalBackend`;
- safety contract: retrieved entries may ground OCR/semantic/warning/report
  explanations, but they cannot create numeric chromatographic evidence.

TurboVec can fit only as a replacement-gated backend behind the retrieval
facade:

```text
KnowledgeSearchQuery
    -> KnowledgeRetrievalEngine facade
    -> active backend: LexicalKnowledgeRetrievalBackend
    -> future candidate: TurboVec-backed retrieval/ranking after benchmark pass
    -> KnowledgeRetrievalContext with used entry ids
    -> bounded snippets for E2B/E4B semantic prompts
```

Lexical and dense ranking may run side by side only during benchmark/shadow
evaluation. If TurboVec passes, it must become the single active ranking/storage
owner and the old lexical ranking path must be retired or demoted to exact
filter/policy support. TurboVec must never bypass `KnowledgeUsePolicyValidator`.

## Proposed Integration Phases

### TV-0/TV-1: Backend foundation and benchmark guardrails

- Do not add TurboVec to Android/KMP runtime yet.
- Do not add Python runtime dependencies to the Android app.
- Keep `KnowledgeRetrievalEngine` as facade.
- Move lexical ranking behind `LexicalKnowledgeRetrievalBackend`.
- Add fail-closed `TurboVecKnowledgeRetrievalBackend` contract.
- Add retrieval diagnostics and first citation benchmark goldens.

Exit gate: backend separation exists, lexical behavior still passes, and the
TurboVec candidate cannot return results until a local benchmark/index exists.

### TV-2: Desktop prototype indexer

Status: complete. Closeout:
`docs/TV2_TURBOVEC_KNOWLEDGE_INDEX_PROTOTYPE_CLOSEOUT.md`.

Build a local script/tool outside Android runtime that:

- reads `docs/knowledge/chromalab_knowledge_seed_v2.json`;
- assigns stable `u64` ids to `KnowledgeEntry.entryId` values;
- embeds only bounded search text with a local embedding model;
- writes an `IdMapIndex` `.tvim` file;
- writes a JSON sidecar mapping `u64 -> entryId`;
- never sends private chromatogram images, logs, or reports to a cloud service.

Exit gate result: PASS. Both `sentence-transformers/all-MiniLM-L6-v2` and
`BAAI/bge-base-en-v1.5` built repeatable PC-only TurboVec indexes with stable
sidecars and no id collisions. Dense retrieval produced semantic/caveat
improvements and 0 safety regressions, but also had non-safety rank regressions
on exact/rule-like tasks. Therefore TurboVec is ready for TV-3 A/B evaluation,
not active runtime promotion.

### TV-3: Retrieval A/B benchmark

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

### TV-4: Kotlin/Rust abstraction

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

### TV-5: Promotion or rejection

After benchmark proof:

- promote TurboVec-backed ranking as the single active retrieval owner; or
- reject it, remove runtime references, and keep lexical retrieval active.

Exit gate: one active retrieval owner remains.

### TV-6: Android feasibility review

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

Adopt TurboVec as a replacement-gated candidate for local Knowledge Pack dense
retrieval. TV-0/TV-1 separated the active lexical backend from the facade and
added fail-closed TurboVec diagnostics. TV-2 proved that PC-only TurboVec indexes
can be built and benchmarked for Knowledge Pack v2. Do not integrate TurboVec
into Android runtime or chromatogram analysis calculations until TV-3 retrieval
policy and later packaging gates pass.

## Deep Audit Follow-Up

The broader modernization plan is documented in
`docs/TURBOVEC_DEEP_AUDIT_AND_MODERNIZATION_PLAN.md`.

That follow-up separates two tracks:

- TurboVec/TurboQuant as a local retrieval and research-memory accelerator;
- Rust deterministic CV and evidence-gated validation as the real path for
  improving graph, axis, calibration, trace, and peak analysis.

The layer-by-layer replacement protocol is documented in
`docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md`. That roadmap is the
current source for deciding the first phase and prevents permanent additive
runtime layers.
