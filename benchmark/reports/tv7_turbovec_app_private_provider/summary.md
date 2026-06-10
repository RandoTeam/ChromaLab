# TV-7 App-Private TurboVec Provider Prototype Summary

Date: 2026-06-10

Status: `TV7_APP_PRIVATE_PROVIDER_SMOKE_PASSED_RUNTIME_NOT_PROMOTED`

TV-7 moved TurboVec from shell-only feasibility into a debug-only ChromaLab
Android app-private provider smoke check. It does not promote TurboVec as the
active product retrieval owner.

| Field | Value |
|---|---|
| Device | `I2407` |
| ABI | `arm64-v8a` |
| Package | `com.chromalab.app.validation` |
| Run id | `turbovec_app_private_1781085239882` |
| Decision | `PASS` |
| Contract | `TV7_TURBOVEC_APP_PRIVATE_PROVIDER_V1` |
| Backend id | `TURBOVEC_DENSE_SHADOW` |
| Path class | `APP_PRIVATE` |
| Index path | `chromalab_tv7_turbovec/chromalab_tv7_probe.tvim` |
| Vector count | `4` |
| Dimension | `64` |
| Bit width | `4` |
| Index size | `706 bytes` |
| Build / load / query | `1077 ms / 0 ms / 1088 ms` |
| Top ids | `1002, 1001, 1003` |
| Top entry ids | `kp2-term-retention-time, kp2-term-chromatogram, kp2-term-peak` |
| Expected top-1 | `1002` |
| Cleanup | `deleted` |

Interpretation:

- TurboVec `IdMapIndex` can be created, persisted, loaded, queried, and deleted
  inside ChromaLab app-private storage on the connected Android device.
- Result ids map to valid Knowledge Pack v2 entry ids in the tiny provider
  fixture.
- The active product retrieval owner remains `LexicalKnowledgeRetrievalBackend`.
- No graph detection, calibration, trace extraction, peak metrics, report gates,
  E2B authority, or `CalculationEngine` behavior changed.

Next phase: `TV-8 - Real Knowledge Index And Local Query Embedding Gate`.
