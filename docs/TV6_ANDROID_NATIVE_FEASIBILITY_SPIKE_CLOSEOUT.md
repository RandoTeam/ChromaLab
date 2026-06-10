# TV-6 Android Native Feasibility Spike Closeout

Date: 2026-06-10

Status: `TV6_NATIVE_COMPILE_FEASIBLE_RUNTIME_NOT_PROVEN`

Scope: Knowledge retrieval native feasibility only. TV-6 does not change Android
runtime retrieval, chromatogram analysis, graph detection, calibration, trace
extraction, peak metrics, report gates, validators, E2B authority, or
`CalculationEngine`.

## Decision

TurboVec should continue toward an Android native runtime probe, but it is not
promoted into product runtime in TV-6.

TV-6 proves:

- the local Rust + Android NDK toolchain is usable;
- the existing ChromaLab Rust workspace still checks for `aarch64-linux-android`;
- TurboVec Rust crate `0.8.1` can be compiled in an isolated probe for:
  - `aarch64-linux-android`;
  - `x86_64-linux-android`.

TV-6 does not prove:

- loading a `.tvim`/dense index from Android app-private storage;
- executing a real query on device;
- memory behavior on weak devices;
- cold-start/index-load latency;
- import/delete/update behavior;
- JNI/KMP provider API shape;
- citation-safety behavior in Android runtime.

## Evidence Collected

Ignored local artifacts:

```text
artifacts/tv6-turbovec-android-feasibility/
```

Tracked summary:

```text
benchmark/reports/tv6_android_native_feasibility/summary.json
benchmark/reports/tv6_android_native_feasibility/summary.md
```

Key checks:

| Check | Result | Notes |
|---|---|---|
| `adb devices` | No target connected | On-device runtime proof could not be collected. |
| Rust local toolchain | PASS | `rustc 1.96.0`, `cargo 1.96.0`. |
| TurboVec crate discovery | PASS | `cargo search` reports `turbovec = "0.8.1"`. |
| Existing Rust CV Android target check | PASS | `chromalab-cv-core` checks for `aarch64-linux-android`. |
| TurboVec probe `aarch64-linux-android` | PASS | Isolated crate with `turbovec = "0.8.1"` checks successfully. |
| TurboVec probe `x86_64-linux-android` | PASS after target install | Required installing `x86_64-linux-android` for pinned Rust `1.96.0`. |

## Native Dependency Notes

The `cargo tree` for the Android probe shows TurboVec pulling numerical and
parallel dependencies such as:

- `faer`;
- `gemm`;
- `ndarray`;
- `rayon`;
- `statrs`;
- `nalgebra`;
- `matrixmultiply`;
- `pulp`.

These dependencies compile for Android targets in the probe, but they increase
the need for runtime memory, startup, thread, and binary-size checks before any
product promotion.

## Product Boundary

TV-6 keeps the current product shape:

```text
KnowledgeRetrievalEngine
    -> active: LexicalKnowledgeRetrievalBackend
    -> candidate: HybridUnionRrfKnowledgeRetrievalBackend
    -> fail-closed: TurboVecKnowledgeRetrievalBackend
```

No source-controlled Android dependency, JNI provider, bundled index, or
embedding model was added.

## Why This Is Not Promotion Yet

Compile success is necessary but not sufficient. ChromaLab needs one active
Knowledge retrieval owner, not several runtime paths. Promoting TurboVec now
would add a second active retrieval layer without evidence for:

- app-private index loading;
- deterministic query results;
- weak-device memory budget;
- safety-policy preservation;
- clean lexical retirement/demotion path.

## Next TurboVec Step

Next TurboVec phase:

```text
TV-6B - On-Device TurboVec Load And Query Probe
```

TV-6B should run only with an Android device/emulator connected. It should:

1. build a minimal native library/provider probe;
2. package or push a tiny test index;
3. load the index from app-private storage;
4. execute deterministic top-k queries;
5. measure cold load, query latency, and memory;
6. verify all result ids map back to valid `KnowledgeEntry.entryId` values;
7. keep lexical as the active product owner until the probe passes.

If TV-6B passes, the following phase can design a real runtime provider and
lexical-retirement path. If TV-6B fails, TurboVec remains PC/dev-only.

## Validation

TV-6 validation is compile/feasibility validation only. Android fixture analysis
and Phase 9 acceptance are unchanged.

