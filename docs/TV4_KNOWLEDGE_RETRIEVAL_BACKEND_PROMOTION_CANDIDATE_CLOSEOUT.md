# TV-4 Knowledge Retrieval Backend Promotion Candidate Closeout

Date: 2026-06-08

Status: `TV4_BACKEND_PROMOTION_CANDIDATE_READY`

Scope: Knowledge retrieval contracts and tests only. TV-4 did not add a
TurboVec Android dependency, did not change Android runtime analysis behavior,
did not modify chromatogram geometry, calibration, trace extraction, peak
metrics, report gates, validators, or `CalculationEngine`.

## Goal

TV-4 moved the TV-3 selected policy into the Kotlin Knowledge retrieval layer as
a promotion candidate:

```text
HYBRID_UNION_RRF
```

The active default remains lexical:

```text
KnowledgeRetrievalEngine.search()
    -> LexicalKnowledgeRetrievalBackend
```

## Implementation

Added:

- `KnowledgeRetrievalBackendId.HYBRID_UNION_RRF_CANDIDATE`;
- `KnowledgeRetrievalQueryClass`;
- `KnowledgeRetrievalArbitrationHint`;
- `HybridUnionRrfKnowledgeRetrievalPolicy`;
- `HybridUnionRrfKnowledgeRetrievalBackend`.

Behavior:

- the candidate policy accepts a lexical context plus optional dense contexts;
- it fuses ranked entry ids with reciprocal-rank fusion;
- it pins lexical top-1 for safety-critical exact-rule queries;
- it drops unknown entry ids by requiring every fused result to map to a
  `KnowledgeEntry`;
- it preserves `usedEntryIds`, backend diagnostics, retrieval stage, source refs,
  and policy validation compatibility;
- when no dense context is supplied, it is lexical-compatible and disableable.

## What Did Not Change

- `LexicalKnowledgeRetrievalBackend` remains the default active owner.
- `TurboVecKnowledgeRetrievalBackend` remains fail-closed and shadow-unavailable.
- No `.tvim` index is loaded in app runtime.
- No Python, TurboVec, or embedding model dependency was added to Android/KMP.
- No model or retrieval output can create numeric chromatogram evidence.

## Tests Added

The Knowledge retrieval tests now prove:

- the hybrid candidate returns the same `usedEntryIds` as lexical when dense
  context is absent;
- dense semantic context can recover the compound-identification caveat that
  lexical missed in TV-3;
- safety-critical exact-rule queries pin lexical top-1 even when dense ranks a
  different entry first;
- `KnowledgeUsePolicyValidator` still rejects forbidden numeric metric use when
  the retrieval context is hybrid.

## Decision

TV-4 passes as a backend promotion candidate. It does not yet make TurboVec the
active retrieval owner because there is still no Kotlin/Rust dense index loader
or Android packaging proof.

Next TurboVec phase:

```text
TV-5 - Dense Provider Promotion Or Rejection Gate
```

TV-5 must decide whether to implement a real local dense provider for the
candidate policy, keep TurboVec PC/dev-only, or reject the runtime promotion
path and leave lexical as the only active product owner.

## Validation

Commands run:

```powershell
git diff --check
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgeRetrievalLayerTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgePackV2Test"
.\gradlew.bat :composeApp:compileKotlinDesktop
```

No Android build is required because TV-4 does not add an Android runtime
dependency or change analyzer behavior.
