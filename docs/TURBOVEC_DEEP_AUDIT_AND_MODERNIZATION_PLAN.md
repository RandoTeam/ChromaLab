# TurboVec Deep Audit And Modernization Plan

Date: 2026-06-06

Status: `TV5_RUNTIME_PROMOTION_DEFERRED_LEXICAL_ACTIVE`

Scope: product architecture and integration planning only. This document does
not change `CalculationEngine`, chromatographic math, Android runtime behavior,
validators, graph detection, calibration, trace extraction, peak integration, or
report gates.

## Why This Matters

The previous Android validation phases proved that ChromaLab's weak point is not
one isolated model, OCR call, or report renderer. The product needs a full
stage-by-stage audit from user image input to final report.

TurboVec/TurboQuant is interesting because it can make local retrieval over
knowledge, methods, failure evidence, and regression history much lighter and
faster. It does not directly solve graph geometry, tick localization, calibration,
trace extraction, or peak integration. The right modernization is therefore:

```text
Rust deterministic CV + evidence-first benchmarks
    + local Knowledge/RAG retrieval acceleration
    + model-safe semantic assistance
    + strict validators and truth tables
```

not:

```text
add vector search and expect chromatogram analysis to become correct
```

## Source-Grounded TurboVec Notes

Reviewed sources:

- `RyanCodrai/turbovec`: <https://github.com/RyanCodrai/turbovec>
- TurboVec API: <https://github.com/RyanCodrai/turbovec/blob/main/docs/api.md>
- TurboVec PyPI: <https://pypi.org/project/turbovec/>
- TurboVec crates.io: <https://crates.io/crates/turbovec>
- Google Research TurboQuant blog:
  <https://research.google/blog/turboquant-redefining-ai-efficiency-with-extreme-compression/>
- TurboQuant paper: <https://arxiv.org/abs/2504.19874>

Useful facts:

- TurboVec is a Rust vector index with Python bindings.
- It implements a TurboQuant-style compressed vector index.
- It supports 2-bit and 4-bit vector storage.
- `IdMapIndex` supports stable external `u64` ids, which can map to
  `KnowledgeEntry.entryId` through a sidecar table.
- Search supports allowlists, which is valuable for hybrid lexical + dense
  retrieval after ChromaLab policy filters.
- It is local/offline and can be paired with local embedding models.
- PyPI metadata currently marks it as alpha, so Android production adoption
  should be gated.
- It is an index/search component; it does not create embeddings by itself.

## License And OpenAI Subsidy Risk

ChromaLab is Apache-2.0 licensed. TurboVec is MIT licensed.

Practical conclusion:

- MIT is permissive and normally compatible with Apache-2.0 repositories when
  copyright/license notices are preserved.
- TurboVec itself is not a license stopper for the OpenAI OSS/subsidy request.
- The safe path is to list TurboVec in third-party dependency/license docs if we
  add it, keep its MIT notice, and avoid implying that TurboVec is an official
  Google product.
- The underlying TurboQuant work is Google Research/paper work; TurboVec is a
  third-party open-source implementation.
- The real review risks are not license blockers; they are dependency maturity,
  Android packaging, security review, and overclaiming production readiness.

Non-legal note: this is engineering license triage, not formal legal advice.

## What TurboVec Can Improve In ChromaLab

### 1. Knowledge Pack retrieval

Current state:

- `KnowledgeRetrievalEngine` is lexical/BM25-style and local.
- It is safe, deterministic, and already aligned with `used_entry_ids`.

TurboVec role:

- optional dense reranker over curated Knowledge Pack entries;
- better semantic lookup for warning explanations, OCR/text classification, and
  report caveats;
- local compressed storage for larger future knowledge packs.

Boundary:

- lexical/policy filters stay first;
- TurboVec cannot bypass `KnowledgeUsePolicyValidator`;
- every retrieved item still needs `entry_id`, source, allowed use, forbidden use,
  and citation provenance.

### 2. Failure taxonomy and diagnostic retrieval

Current state:

- Phase 9 produced many evidence packages and failure docs.
- The user-visible truth is hard to navigate.

TurboVec role:

- index failure taxonomy, phase closeouts, fixture summaries, and evidence
  notes;
- retrieve similar historical failures by stage/subreason;
- help the orchestrator propose the next engineering fix from prior evidence.

Boundary:

- retrieved failure notes are advisory;
- validators decide pass/review/fail;
- no failure may be reclassified by retrieval alone.

### 3. Research corpus for modern methods

Current state:

- Deep-research waves are documented, but method lookup is manual.

TurboVec role:

- local research index over official docs, reviewed papers, accepted GitHub
  methods, OCR/CV notes, Rust implementation notes, and benchmark results;
- faster offline recall during future stage-specific research.

Boundary:

- research retrieval informs implementation;
- implementation still needs isolated tests, Android reruns, and evidence gates.

### 4. Model prompt grounding

Current state:

- E2B/E4B can assist OCR/semantic explanations.
- Models cannot be numeric authority.

TurboVec role:

- provide compact local context cards for E2B/E4B semantic prompts;
- reduce hallucinated report explanations;
- improve title/ion/channel/tick-text classification.

Boundary:

- model output must include `used_entry_ids`;
- missing citations remain REVIEW;
- unsupported claims are captured;
- model output cannot create geometry, calibration, RT, height, area, FWHM, S/N,
  baseline, Kovats, peak count, or compound identity.

## What TurboVec Cannot Fix

TurboVec will not solve:

- graphPanel discovery;
- plotArea detection;
- one graph vs multi-panel layout semantics;
- axis/tick/grid geometry;
- OCR crop placement;
- calibration anchor formation;
- trace mask extraction;
- baseline/noise calculation;
- peak integration;
- Android export reliability.

Those remain deterministic CV/Rust/runtime/validator problems.

## End-To-End Audit Points

The audit must evaluate every stage with real images, not summaries.

| Stage | Current risk | Modernization direction | TurboVec role |
|---|---|---|---|
| 0 Input/provenance | Hidden differences between screenshot, photo, fixture, report export. | Hashing, source class, orientation, device profile, privacy metadata. | Index provenance/failure notes only. |
| 1 Image preparation | Old preprocessing variants may be heavy and not evidence-ranked. | Rust image normalization, deskew/rectification candidates, variant scoring. | Retrieve prior best preprocessing decisions. |
| 2 Graph discovery | 0-graph and wrong graph-count cases remain. | Rust graph layout candidate generation, frame/axis/trace density scoring, multi-panel grouping. | Retrieve similar fixture failure patterns. |
| 3 Plot area/layout | Stacked/TIC+ions/two-graph cases are weak. | Explicit layout taxonomy, panel grouping, shared-axis logic, report propagation. | Retrieve layout rules/caveats. |
| 4 Axis/grid/labels | Tick-only logic was too brittle. | Multi-evidence scale resolver: frame, grid, OCR label boxes, regular sequences. | Retrieve text classification rules only. |
| 5 Calibration | Strategy arbitration can regress if one path overrides another. | Calibration ensemble with residual-backed arbitration and regression shields. | No numeric role. |
| 6 Trace extraction | Sparse/dense/stacked traces need more robust masks. | Rust trace mask/centerline extraction, branch pruning, grid/text suppression. | Retrieve trace failure taxonomy. |
| 7 Peak detection/integration | Metrics depend on upstream trace/calibration quality. | Keep math audited; improve evidence around baseline/noise/flags before changing calculations. | No numeric role. |
| 8 Model assistance | Model must help without becoming authority. | E2B baseline semantic mode, strict forbidden-field filters, disagreement evidence. | Provide bounded local context cards. |
| 9 Evidence validation | Product truth is still hard to inspect. | Per-stage artifact completeness, contact sheets, product/scientific truth tables. | Retrieve historical failures and expected evidence. |
| 10 Reports | Reports can look complete while still REVIEW/BLOCKED. | Evidence-first report design; no release-ready overclaim. | Ground caveats and explanations. |
| 11 Export/privacy | Diagnostic artifacts and user reports must remain separated. | Privacy classes, manifest checks, no raw private paths in user report. | Index docs only; no private artifact upload. |
| 12 Acceptance | One-fixture acceptance is invalid. | Multi-fixture truth audit, Android + PC corpus, deterministic/E2B comparison. | Retrieval for QA triage, not acceptance. |

## Integration Plan

### Phase TV-A: Dependency and license gate

Goal: decide whether TurboVec is safe to prototype locally.

Work:

- record current TurboVec version on PyPI and crates.io;
- record MIT license notice requirements;
- inspect Rust dependencies and Android build implications;
- create a third-party license entry if the dependency is added;
- document that this is third-party TurboVec, not official Google code.

Exit criteria:

- no GPL/AGPL-style blocker;
- alpha/maturity risk recorded;
- no Android dependency added yet.

### Phase TV-B: Local knowledge index prototype

Status: complete through TV-2. See
`docs/TV2_TURBOVEC_KNOWLEDGE_INDEX_PROTOTYPE_CLOSEOUT.md`.

Goal: prove TurboVec can index ChromaLab knowledge without touching Android.

Work:

- build a PC-only prototype script/tool;
- read `docs/knowledge/chromalab_knowledge_seed_v2.json`;
- embed bounded searchable text with a local embedding model;
- map `KnowledgeEntry.entryId` to stable `u64`;
- build `IdMapIndex`;
- persist `.tvim` plus JSON sidecar;
- run local search queries.

Exit criteria:

- index builds locally;
- search works offline;
- no cloud upload;
- no Android runtime changes.

Result:

- both MiniLM and BGE profiles built repeatable local TurboVec `.tvim` indexes;
- stable `u64 -> entryId` sidecars were generated without collisions;
- generated index artifacts remain under ignored
  `artifacts/tv2-turbovec-knowledge/`;
- compact benchmark summaries are tracked under
  `benchmark/reports/tv2_turbovec_knowledge/`.

### Phase TV-C: Hybrid lexical + dense benchmark

Status: complete through TV-3. See
`docs/TV3_RETRIEVAL_AB_ARBITRATION_POLICY_CLOSEOUT.md`.

TV-2 benchmark data showed dense retrieval is useful but not a complete
replacement for lexical/rule-like retrieval. TV-3 evaluated explicit policy
candidates and selected `HYBRID_UNION_RRF` as the next benchmark target.

Goal: prove dense retrieval improves real ChromaLab tasks.

Work:

- build a reviewed query set for OCR/semantic/report tasks;
- compare lexical-only vs lexical-filtered dense rerank;
- record top-k expected entry hit rate;
- test `used_entry_ids`;
- test forbidden numeric use rejection;
- record latency, memory, and index size.

Exit criteria:

- dense retrieval improves selected semantic tasks;
- no forbidden-use regression;
- no missing-citation regression;
- lexical-only fallback remains valid.

Result:

- `HYBRID_UNION_RRF` reached 10/10 expected-entry hits and 9/10 top-1 hits;
- it recovered the lexical miss for the natural-language photo-only
  compound-identification caveat;
- it preserved safety-critical exact-rule top-1 behavior;
- dense-only MiniLM and BGE remain rejected as promotion targets.

### Phase TV-D: Rust/Kotlin retrieval abstraction

Status: complete through TV-4. See
`docs/TV4_KNOWLEDGE_RETRIEVAL_BACKEND_PROMOTION_CANDIDATE_CLOSEOUT.md`.

Goal: make backend choice explicit without changing scientific behavior.

Work:

- introduce a retrieval backend interface only after TV-C passes;
- keep lexical backend default;
- make dense backend optional and disableable;
- export retrieval backend diagnostics;
- ensure report behavior is identical when dense backend is unavailable.

### Phase TV-E: Dense provider promotion or rejection gate

Status: complete through TV-5. See
`docs/TV5_DENSE_PROVIDER_PROMOTION_REJECTION_GATE_CLOSEOUT.md`.

Goal: decide whether the TV-4 hybrid policy gets a real Android/runtime dense
provider now, stays PC/dev-only, or is rejected as an active product path.

Decision:

- keep `LexicalKnowledgeRetrievalBackend` as the active product owner;
- keep `HybridUnionRrfKnowledgeRetrievalBackend` as a candidate policy;
- keep `TurboVecKnowledgeRetrievalBackend` fail-closed;
- do not add TurboVec as an Android runtime dependency yet.

Reason:

- no product runtime provider loads TurboVec indexes;
- Android native packaging and storage/delete/import behavior are not proven;
- embedding model memory and latency are not validated on Android;
- Phase 9 analyzer acceptance remains blocked, so Knowledge retrieval must not
  add another active runtime owner.

Exit criteria:

- tests prove dense retrieval cannot affect numeric gates;
- report validators remain strict;
- no CalculationEngine changes.

Result:

- `HYBRID_UNION_RRF_CANDIDATE` was added as an explicit backend id;
- `KnowledgeRetrievalArbitrationHint` and `KnowledgeRetrievalQueryClass` were
  added to make safety-critical exact-rule pinning explicit;
- the hybrid candidate is lexical-compatible when no dense context is supplied;
- policy validation still rejects forbidden numeric metric use.

### Phase TV-E: Android feasibility spike

Goal: determine whether TurboVec belongs in Android app runtime.

Work:

- test Rust crate Android NDK compatibility;
- inspect native library size;
- inspect BLAS/faer/ndarray implications;
- test app-private index loading;
- measure memory and latency on attached devices;
- compare against SQLite FTS5/Room and small in-memory lexical retrieval.

Exit criteria:

- if Android cost is too high, keep TurboVec PC/dev-only;
- if cost is acceptable, create a gated Android prototype;
- no production switch until validation passes.

### Phase TV-F: Developer/research assistant index

Goal: use TurboVec to improve our development process before product runtime.

Work:

- index docs, phase reports, failure taxonomy, and research notes;
- retrieve prior failures by stage/subreason;
- create a local "what broke before" search workflow for the orchestrator;
- use it during Rust CV and Android validation phases.

Exit criteria:

- faster and more truthful debugging;
- no user-facing product claims;
- no private artifacts committed.

## Full Modernization Plan Beyond TurboVec

TurboVec is one support technology. The main analyzer still needs a larger
modernization path.

### Modernization Group 1: Truth corpus and benchmark harness

- freeze all current Android/desktop fixtures;
- add external complex chromatogram images only with license/source review;
- define expected graph/layout/calibration/trace/peak truth where possible;
- generate contact sheets and stage truth tables automatically;
- track per-stage accuracy, not only final report gate.

### Modernization Group 2: Rust deterministic CV core

- move image preparation, layout candidates, graph detection, plotArea, axis/grid
  evidence, OCR crop planning, and trace masks into Rust step by step;
- do not port broken Kotlin heuristics blindly;
- implement clean contracts and compare old vs new on the corpus;
- keep Kotlin as orchestration/report/UI layer.

### Modernization Group 3: Axis and calibration evidence

- keep calibration ensemble;
- add per-strategy score tables;
- require monotonicity/residual evidence;
- use OCR labels only when linked to geometry;
- reject title/ion/m/z/SIM text as scale labels.

### Modernization Group 4: Trace and peak evidence

- separate trace extraction from peak calculation;
- improve masks, centerlines, sparse/dense/stacked handling;
- preserve `CalculationEngine` unless an isolated math bug is proven;
- expand peak evidence: baseline, S/N basis, integration windows, overlap flags.

### Modernization Group 5: Model and Knowledge safety

- E2B remains supported FAST baseline;
- model output remains semantic/advisory;
- TurboVec may improve retrieval context;
- every model-assisted report statement must be cited or marked unsupported.

### Modernization Group 6: Product report truth

- report must expose what worked and what did not;
- RELEASE_READY only when all required evidence gates pass;
- REVIEW/DIAGNOSTIC must still be useful and visually clear;
- no hidden BLOCKED state behind polished HTML.

## Current Next TurboVec Step

Do not integrate TurboVec into Android product runtime yet, and do not install
it as an active runtime dependency before TV-7 app-private provider and later
packaging/citation gates pass.

This plan is now subordinate to the replacement roadmap in
`docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md`.

Completed foundation:

```text
TV-0/TV-1 - TurboVec Knowledge Replacement Foundation
TV-2 - PC-only Knowledge Pack TurboVec prototype
TV-3 - Retrieval A/B Evaluation And Arbitration Policy
TV-4 - Knowledge Retrieval Backend Promotion Candidate
TV-5 - Dense Provider Promotion Or Rejection Gate
TV-6 - Android Native Feasibility Spike
TV-6B - On-Device TurboVec Load And Query Probe
```

The next TurboVec-specific work slice is:

```text
TV-7 - App-Private TurboVec Provider Prototype
```

TV-5 decided that the selected hybrid policy remains a candidate and does not
get a real product runtime dense provider yet. TV-6 proved compile-level Android
native feasibility for TurboVec `0.8.1`. TV-6B proved shell-level on-device
load/query with a tiny `.tvim` index. Storage/import/delete behavior in
app-private directories, real Knowledge index memory, provider lifecycle,
latency under app startup constraints, and citation safety remain unproven.
TV-7 must prove those before runtime promotion can reopen.

In parallel, the main chromatogram analyzer remains blocked at the R15A Android
evidence gate. TurboVec can help retrieve research and failure context, but it is
not the replacement for deterministic geometry and calibration work.
