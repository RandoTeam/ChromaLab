# TV-5 Dense Provider Promotion Or Rejection Gate Closeout

Date: 2026-06-08

Status: `TV5_RUNTIME_PROMOTION_DEFERRED_LEXICAL_ACTIVE`

Scope: Knowledge retrieval decision gate only. TV-5 does not change Android
runtime behavior, chromatogram analysis, graph detection, calibration, trace
extraction, peak metrics, report gates, validators, E2B authority, or
`CalculationEngine`.

## Decision

Do not promote TurboVec or any dense provider into ChromaLab product runtime yet.

The active Knowledge retrieval owner remains:

```text
LexicalKnowledgeRetrievalBackend
```

The existing Kotlin hybrid policy remains a candidate:

```text
HybridUnionRrfKnowledgeRetrievalBackend
```

The TurboVec runtime backend remains fail-closed:

```text
TurboVecKnowledgeRetrievalBackend -> SHADOW_UNAVAILABLE
```

## Why Runtime Promotion Is Deferred

TV-2 and TV-3 proved that dense retrieval is useful on PC for semantic/caveat
Knowledge Pack queries. TV-4 proved that ChromaLab can represent the selected
hybrid arbitration policy in Kotlin without changing product behavior.

That is not enough to make TurboVec an active Android/runtime dependency.

Open blockers:

- no Kotlin/Rust dense provider currently loads `.tvim` indexes in app runtime;
- Android native packaging for TurboVec has not been proven;
- embedding model bundling, storage, import/delete, and memory behavior are not
  validated on Android;
- TurboVec PyPI remains alpha and PC tooling is not a production mobile runtime;
- no Android query benchmark proves deterministic latency, memory, and
  citation-safety behavior;
- analyzer Phase 9 is still blocked, so retrieval modernization must not create
  a second active runtime path while core evidence is unresolved.

## What Remains Active

| Layer | TV-5 state | Reason |
|---|---|---|
| `KnowledgeRetrievalEngine` | active facade | Keeps current public API stable. |
| `LexicalKnowledgeRetrievalBackend` | active ranking owner | Safe, deterministic, tested, and policy-compatible. |
| `HybridUnionRrfKnowledgeRetrievalBackend` | candidate only | Useful policy shape, but no active dense provider yet. |
| `TurboVecKnowledgeRetrievalBackend` | fail-closed shadow | Prevents accidental runtime dense retrieval without index/provider proof. |
| TV-2/TV-3 PC artifacts | ignored/local dev artifacts | Useful for research, not product runtime. |

## Safety Boundaries Preserved

TV-5 preserves the existing Knowledge Pack boundaries:

- retrieved entries can ground OCR/semantic warning explanations;
- `used_entry_ids` and policy validation remain mandatory;
- vector similarity cannot create graph geometry, pixel coordinates,
  calibration coefficients, trace evidence, peak metrics, Kovats values, or
  compound identity;
- E2B remains advisory and cannot use retrieval to override deterministic
  evidence.

## Product Outcome

TV-5 is a rejection of immediate runtime promotion, not a rejection of TurboVec
research.

TurboVec remains promising for:

- PC-side Knowledge Pack evaluation;
- future local/offline semantic retrieval;
- developer/research assistant indexes;
- compact citation-grounded report explanations after native feasibility is
  proven.

It is not yet an Android production retrieval owner.

## Next Phase

Next TurboVec phase:

```text
TV-6 - Android Native Feasibility Spike
```

TV-6 should test native packaging and local index loading separately from
product promotion. Passing TV-6 may reopen runtime provider work. Failing TV-6
keeps TurboVec PC/dev-only and leaves lexical as the single active product
retrieval owner.

Analyzer work remains separate. R15A/R16 decisions must continue from Android
fixture evidence, not from TurboVec retrieval status.

## Validation

TV-5 validates the decision by keeping runtime behavior unchanged and re-running
the Knowledge-layer checks that prove lexical retrieval remains active and safe.

