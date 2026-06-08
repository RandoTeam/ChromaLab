# TV-5 Dense Provider Gate Summary

Date: 2026-06-08

Status: `TV5_RUNTIME_PROMOTION_DEFERRED_LEXICAL_ACTIVE`

## Decision

TurboVec is not promoted into ChromaLab Android/product runtime in TV-5.

Active retrieval owner:

```text
LexicalKnowledgeRetrievalBackend
```

Candidate policy:

```text
HybridUnionRrfKnowledgeRetrievalBackend
```

Fail-closed dense backend:

```text
TurboVecKnowledgeRetrievalBackend
```

## Gate Result

TV-5 passes as a decision gate because it prevents an unproven dense provider
from becoming a hidden active runtime layer.

TurboVec remains PC/dev-only until Android native packaging, index loading,
storage, memory, latency, and citation-safety behavior are proven.

## Next

Next TurboVec phase:

```text
TV-6 - Android Native Feasibility Spike
```

