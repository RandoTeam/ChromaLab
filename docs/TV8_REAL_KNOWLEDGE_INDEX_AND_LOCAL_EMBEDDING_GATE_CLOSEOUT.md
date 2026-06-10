# TV-8 Real Knowledge Index And Local Query Embedding Gate Closeout

Date: 2026-06-10

Status: `TV8_REAL_INDEX_PASSED_LOCAL_EMBEDDING_BLOCKED_RUNTIME_NOT_PROMOTED`

## Scope

TV-8 tested whether the Android app process can load and query a real
Knowledge Pack v2 TurboVec index from app-private storage. It did not promote
TurboVec into active product retrieval.

TV-8 did not change:

- chromatogram graph detection;
- axis, tick, calibration, trace, peak, or report-gate behavior;
- E2B authority;
- `CalculationEngine`;
- active user-facing Knowledge retrieval.

## Implementation

Added a debug-only real-index gate:

- Rust/JNI bridge contract: `TV8_TURBOVEC_REAL_KNOWLEDGE_INDEX_GATE_V1`;
- Android debug intent: `com.chromalab.app.DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE`;
- PC query-vector builder:
  `tools/knowledge-retrieval/build_tv8_android_query_vectors.py`;
- Android adb runner:
  `tools/android/Run-TurboVecKnowledgeIndexGate.ps1`;
- tracked compact result under
  `benchmark/reports/tv8_real_knowledge_index_embedding_gate/`.

The gate imports these ignored local artifacts into app-private storage:

```text
artifacts/tv2-turbovec-knowledge/minilm/chromalab_knowledge_v2_minilm.tvim
artifacts/tv2-turbovec-knowledge/minilm/chromalab_knowledge_v2_minilm_sidecar.json
artifacts/tv8-turbovec-knowledge-index-gate/chromalab_knowledge_v2_minilm_queries.json
```

The query vectors are generated on PC using:

```text
sentence-transformers/all-MiniLM-L6-v2
```

The Android app does not currently have a MiniLM/ONNX/TFLite embedding runtime.
That is the TV-8 promotion blocker.

## Android Result

| Field | Value |
|---|---|
| Device | `I2407` |
| ABI | `arm64-v8a` |
| Package | `com.chromalab.app.validation` |
| Run id | `turbovec_knowledge_gate_1781086551463` |
| Decision | `BLOCKED_LOCAL_QUERY_EMBEDDING_UNAVAILABLE` |
| Status | `PASS` |
| Contract | `TV8_TURBOVEC_REAL_KNOWLEDGE_INDEX_GATE_V1` |
| Backend id | `TURBOVEC_DENSE_SHADOW` |
| Path class | `APP_PRIVATE` |
| Profile | `minilm` |
| Model | `sentence-transformers/all-MiniLM-L6-v2` |
| Entry count | `112` |
| Dimension | `384` |
| Bit width | `4` |
| Index size | `25,938 bytes` |
| Load ms | `0` |
| Query ms | `2478` |
| Query embedding runtime | `PC_SENTENCE_TRANSFORMERS_REFERENCE` |
| Local Android embedding available | `false` |
| Real index gate passed | `true` |
| Cleanup result | `deleted,deleted,deleted` |
| Runtime promotion | `false` |
| Active retrieval owner unchanged | `true` |

| Query | Status | Required missing | Forbidden present |
|---|---|---:|---:|
| `sn_signal_to_noise` | `PASS` | 0 | 0 |
| `ion71_title_channel` | `PASS` | 0 | 0 |
| `knowledge_cannot_create_metrics` | `PASS` | 0 | 0 |

Ignored device/local artifacts:

```text
artifacts/tv8-turbovec-knowledge-index-gate/turbovec_knowledge_gate_1781086551463/
```

Tracked summaries:

```text
benchmark/reports/tv8_real_knowledge_index_embedding_gate/summary.json
benchmark/reports/tv8_real_knowledge_index_embedding_gate/summary.md
```

## Decision

TV-8 proves that a real MiniLM Knowledge Pack v2 TurboVec index can be loaded,
queried, mapped back to valid Knowledge entries, and cleaned up from app-private
storage.

TV-8 blocks runtime promotion because query embeddings are still PC-generated.
This is not acceptable for active offline Android retrieval.

The active product retrieval owner remains:

```text
LexicalKnowledgeRetrievalBackend
```

TurboVec remains:

```text
debug/provider candidate, not active product retrieval
```

## Next Phase

```text
TV-8B - Android Local Embedding Runtime Selection
```

TV-8B must choose and validate one Android-local embedding path for MiniLM or
formally reject TurboVec as Android runtime retrieval. Do not proceed to TV-9
promotion until local/offline query embeddings are proven on device.
