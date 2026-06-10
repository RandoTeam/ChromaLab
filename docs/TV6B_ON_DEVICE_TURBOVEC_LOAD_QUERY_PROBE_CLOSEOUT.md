# TV-6B On-Device TurboVec Load And Query Probe Closeout

Date: 2026-06-10

Status: `TV6B_ON_DEVICE_SHELL_LOAD_QUERY_PASSED_RUNTIME_NOT_PROMOTED`

Scope: Android on-device TurboVec shell probe only. TV-6B does not change
Android app runtime retrieval, chromatogram analysis, graph detection,
calibration, trace extraction, peak metrics, report gates, validators, E2B
authority, or `CalculationEngine`.

## Decision

TurboVec has passed the first real on-device load/query probe, but it is still
not promoted into ChromaLab product runtime.

TV-6B proves:

- an `arm64-v8a` Android device can execute a Rust binary linked with
  `turbovec = "0.8.1"`;
- the binary can create an `IdMapIndex`;
- the binary can write a `.tvim` index on the device filesystem;
- the binary can load the `.tvim` index back;
- the binary can execute deterministic top-k query;
- returned ids map to the expected stable external ids.

TV-6B does not prove:

- app-private storage loading inside the ChromaLab app;
- JNI/KMP provider API shape;
- bundled or imported Knowledge Pack dense index loading;
- weak-device memory budget for a real Knowledge Pack index;
- cold-start behavior for real MiniLM/BGE-sized indexes;
- lexical retirement or demotion;
- product report behavior with a TurboVec backend.

## Device And Probe

| Field | Value |
|---|---|
| Device model | `I2407` |
| ABI | `arm64-v8a` |
| Android SDK | `36` |
| Probe binary | `tv6b_turbovec_probe` |
| Probe location | `/data/local/tmp/tv6b_turbovec_probe` |
| Index path | `/data/local/tmp/chromalab_tv6b_probe.tvim` |
| Vector count | 4 |
| Dimension | 64 |
| Bit width | 4 |
| Index size | 706 bytes |

The probe used a tiny orthogonal vector fixture with stable ids:

```text
1001, 1002, 1003, 1004
```

The expected query top-1 id was `1002`.

## Final On-Device Runs

| Run | Status | Top ids | Build ms | Load ms | Query ms | RSS before KB | RSS after KB |
|---:|---|---|---:|---:|---:|---:|---:|
| 1 | PASS | `[1002, 1001, 1003]` | 166 | 0 | 159 | 3916 | 4948 |
| 2 | PASS | `[1002, 1001, 1003]` | 163 | 0 | 158 | 3692 | 4980 |
| 3 | PASS | `[1002, 1001, 1003]` | 183 | 0 | 158 | 3688 | 5036 |

All final runs returned valid ids and stable top-k order.

## Probe Notes

An earlier smoke run used magnitude-biased test vectors and returned valid ids
from a real device execution, but its top-1 expectation was not a valid ranking
fixture. The final scored probe replaced it with orthogonal vectors and passed
3/3 runs.

## Cleanup

The temporary executable and `.tvim` file were removed from `/data/local/tmp`
after evidence collection.

Ignored local artifacts:

```text
artifacts/tv6b-turbovec-android-load-query/
```

Tracked summary:

```text
benchmark/reports/tv6b_turbovec_android_load_query/summary.json
benchmark/reports/tv6b_turbovec_android_load_query/summary.md
```

## Product Boundary

The product retrieval owner remains:

```text
LexicalKnowledgeRetrievalBackend
```

The candidate policy remains:

```text
HybridUnionRrfKnowledgeRetrievalBackend
```

The fail-closed runtime placeholder remains:

```text
TurboVecKnowledgeRetrievalBackend -> SHADOW_UNAVAILABLE
```

TV-6B proves Android device execution feasibility. It does not add TurboVec to
the app, does not change user reports, and does not create a second active
runtime retrieval owner.

## Next TurboVec Step

Next phase:

```text
TV-7 - App-Private TurboVec Provider Prototype
```

TV-7 should move from shell probe to app/runtime feasibility:

1. create a debug-only app-private provider prototype;
2. load a tiny `.tvim` index from app-private storage;
3. map result ids back to `KnowledgeEntry.entryId`;
4. report backend diagnostics through existing Knowledge contracts;
5. keep lexical as active owner until provider evidence passes;
6. define the exact lexical-retirement/demotion path before promotion.

If TV-7 passes, TurboVec can become a real promotion candidate. If TV-7 fails,
TurboVec remains PC/dev-only or shell-probe-only.

