# TV-7 App-Private TurboVec Provider Prototype Closeout

Date: 2026-06-10

Status: `TV7_APP_PRIVATE_PROVIDER_SMOKE_PASSED_RUNTIME_NOT_PROMOTED`

## Scope

TV-7 tested whether TurboVec can run from inside the ChromaLab Android app
process using app-private storage. It did not promote TurboVec into active
product Knowledge retrieval.

TV-7 did not change:

- chromatogram graph detection;
- axis, tick, calibration, trace, peak, or report-gate behavior;
- E2B authority;
- `CalculationEngine`;
- active user-facing Knowledge retrieval.

## Implementation

Added a debug-only app-private provider smoke path:

- Rust/JNI bridge contract: `TV7_TURBOVEC_APP_PRIVATE_PROVIDER_V1`;
- Android debug intent: `com.chromalab.app.DEBUG_TURBOVEC_APP_PRIVATE`;
- Android diagnostics exporter under
  `/sdcard/Download/ChromaLab/runtime/turbovec-app-private/<runId>/`;
- adb runner: `tools/android/Run-TurboVecAppPrivateSmoke.ps1`;
- tracked compact result under
  `benchmark/reports/tv7_turbovec_app_private_provider/`.

The probe uses a tiny deterministic `IdMapIndex` fixture:

```text
ids: 1001, 1002, 1003, 1004
dimension: 64
bit width: 4
expected query top-1: 1002
```

The Android diagnostic maps returned ids back to real Knowledge Pack v2
entries before accepting the smoke result:

```text
1001 -> kp2-term-chromatogram
1002 -> kp2-term-retention-time
1003 -> kp2-term-peak
1004 -> kp2-term-peak-apex
```

## Android Result

| Field | Value |
|---|---|
| Device | `I2407` |
| ABI | `arm64-v8a` |
| Package | `com.chromalab.app.validation` |
| Run id | `turbovec_app_private_1781085239882` |
| Decision | `PASS` |
| Status | `PASS` |
| Backend id | `TURBOVEC_DENSE_SHADOW` |
| Path class | `APP_PRIVATE` |
| Index relative path | `chromalab_tv7_turbovec/chromalab_tv7_probe.tvim` |
| Index size | `706 bytes` |
| Build ms | `1077` |
| Load ms | `0` |
| Query ms | `1088` |
| Top ids | `1002, 1001, 1003` |
| Top entry ids | `kp2-term-retention-time, kp2-term-chromatogram, kp2-term-peak` |
| Top-1 OK | `true` |
| All ids valid | `true` |
| All entry ids valid | `true` |
| Query timed out | `false` |
| Cleanup result | `deleted` |
| Index exists after cleanup | `false` |
| Runtime promotion | `false` |
| Active retrieval owner unchanged | `true` |

Ignored device/local artifacts:

```text
artifacts/tv7-turbovec-app-private/turbovec_app_private_1781085239882/
```

Tracked summaries:

```text
benchmark/reports/tv7_turbovec_app_private_provider/summary.json
benchmark/reports/tv7_turbovec_app_private_provider/summary.md
```

## Build Environment Note

The validation APK build had a pre-existing Windows host-toolchain blocker in
the llama/Vulkan shader-generator step. The build was repaired by using
`w64devkit` as the host compiler for `vulkan-shaders-gen` and by making the
generated host toolchain pass the toolchain bin path to CMake.

This is build infrastructure only. It does not change model runtime behavior.

## Product Boundary

The active product retrieval owner remains:

```text
LexicalKnowledgeRetrievalBackend
```

TurboVec remains:

```text
debug-only provider smoke / dense replacement candidate
```

TV-7 proves app-private index load/query feasibility for a tiny fixture. It does
not prove:

- real Knowledge Pack v2 dense index size;
- local query embedding on Android;
- MiniLM latency or memory;
- citation safety policy with dense results;
- lexical demotion or removal.

## Decision

TV-7 passes as an app-private provider feasibility gate.

Next phase:

```text
TV-8 - Real Knowledge Index And Local Query Embedding Gate
```

TV-8 must use a real Knowledge Pack v2 TurboVec index and a local/offline query
embedding path. If local embedding is not practical on Android, TurboVec must
remain PC/dev-only or debug-provider-only instead of becoming a permanent
duplicate retrieval layer.
