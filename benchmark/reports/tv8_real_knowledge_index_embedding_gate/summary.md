# TV-8 Real Knowledge Index And Local Query Embedding Gate Summary

Date: 2026-06-10

Status: `TV8_REAL_INDEX_PASSED_LOCAL_EMBEDDING_BLOCKED_RUNTIME_NOT_PROMOTED`

TV-8 tested a real Knowledge Pack v2 MiniLM TurboVec index from inside the
ChromaLab Android app process. It does not promote TurboVec as active product
retrieval because Android-local query embedding is still unavailable.

| Field | Value |
|---|---|
| Device | `I2407` |
| ABI | `arm64-v8a` |
| Package | `com.chromalab.app.validation` |
| Run id | `turbovec_knowledge_gate_1781086551463` |
| Decision | `BLOCKED_LOCAL_QUERY_EMBEDDING_UNAVAILABLE` |
| Contract | `TV8_TURBOVEC_REAL_KNOWLEDGE_INDEX_GATE_V1` |
| Backend id | `TURBOVEC_DENSE_SHADOW` |
| Path class | `APP_PRIVATE` |
| Profile | `minilm` |
| Model | `sentence-transformers/all-MiniLM-L6-v2` |
| Entries | `112` |
| Dimension | `384` |
| Bit width | `4` |
| Index size | `25,938 bytes` |
| Load / query | `0 ms / 2478 ms` |
| Query embedding runtime | `PC_SENTENCE_TRANSFORMERS_REFERENCE` |
| Local Android embedding available | `false` |
| Real index gate passed | `true` |
| Cleanup | `deleted,deleted,deleted` |

| Query | Status | Required missing | Forbidden present |
|---|---|---:|---:|
| `sn_signal_to_noise` | `PASS` | 0 | 0 |
| `ion71_title_channel` | `PASS` | 0 | 0 |
| `knowledge_cannot_create_metrics` | `PASS` | 0 | 0 |

Interpretation:

- Real Knowledge Pack MiniLM `.tvim` load/query works in app-private storage.
- Stable TurboVec ids map back to valid Knowledge Pack v2 entries.
- Import cleanup works; no app-private TV-8 files remained after the run.
- The active product retrieval owner remains `LexicalKnowledgeRetrievalBackend`.
- Runtime promotion is blocked until ChromaLab has an Android-local MiniLM
  embedding path or rejects mobile dense retrieval.

Next phase: `TV-8B - Android Local Embedding Runtime Selection`.
