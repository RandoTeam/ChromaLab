# TV-6 Android Native Feasibility Summary

Date: 2026-06-10

Status: `TV6_NATIVE_COMPILE_FEASIBLE_RUNTIME_NOT_PROVEN`

## Result

TurboVec is compile-feasible for Android Rust targets in an isolated local probe,
but it is not runtime-promoted.

## Checks

| Check | Result |
|---|---|
| `adb devices` | No connected Android target |
| Rust local toolchain | `rustc 1.96.0`, `cargo 1.96.0` |
| Latest crate from `cargo search` | `turbovec = "0.8.1"` |
| Existing ChromaLab Rust `aarch64-linux-android` check | PASS |
| TurboVec probe `aarch64-linux-android` | PASS |
| TurboVec probe `x86_64-linux-android` | PASS after installing target for Rust `1.96.0` |

## Decision

Do not promote TurboVec into Android product runtime yet.

Next: `TV-6B - On-Device TurboVec Load And Query Probe`.

