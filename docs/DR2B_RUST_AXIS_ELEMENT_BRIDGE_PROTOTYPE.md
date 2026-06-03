# DR-2B Rust Axis Element Bridge Prototype

Status: `DR2B_BRIDGE_RUNTIME_READY_AFTER_SETUP1`

Scope: Rust bridge prototype only. This phase does not change `CalculationEngine`, chromatographic math, Kotlin production analysis, Android runtime behavior, report gates, validators, or calibration selection.

## What Was Built

DR-2B adds a Rust bridge from existing ChromaLab evidence JSON into typed Rust crop-planning contracts.

Input contract:

- `axis_element_graph.json`
- image width/height from the normalized image/audit context

Rust output contract:

- graph index
- source label-band count
- accepted crop plans
- rejected crop bands with explicit reasons
- crop variant list matching DR-1R

This is not a new graph detector. It is a correctness boundary: Rust must first reproduce and validate existing geometry evidence before it is allowed to own deeper CV stages.

## Why This Is Not A Superficial Rust Port

The Rust core is being built around typed evidence, not around copying Kotlin code line-for-line.

Principles:

1. Use Rust for deterministic geometry and evidence contracts first.
2. Keep OCR/VLM text separate from geometry authority.
3. Keep title/metadata bands without axis authority.
4. Preserve rejection reasons instead of silently dropping bad inputs.
5. Keep `unsafe` out of the core until an FFI phase requires it.
6. Add FFI only behind safe wrappers and explicit contracts.
7. Avoid OpenCV wrapper dependency as the first move; wrappers can recreate native dependency fragility before the core logic is stable.
8. Prefer integer geometry and saturating/clamped operations for crop bounds.
9. Validate Rust against existing ChromaLab artifacts before changing production behavior.

## Current Research References

- Rust 2024 requires unsafe extern blocks for FFI boundaries: https://doc.rust-lang.org/edition-guide/rust-2024/unsafe-extern.html
- Rust Reference external blocks describe why FFI is unsafe and must be wrapped: https://doc.rust-lang.org/reference/items/external-blocks.html
- Rustonomicon FFI chapter explains `repr(C)` and safe high-level wrappers around unsafe internals: https://doc.rust-lang.org/nomicon/ffi.html
- Rust Android target support tracks recent Android NDK LTS support: https://doc.rust-lang.org/rustc/platform-support/android.html
- `imageproc` is a pure Rust image-processing candidate for later phases: https://docs.rs/imageproc/latest/imageproc/
- Kornia-rs is relevant as a modern Rust CV direction because it favors typed Rust image/tensor operations over C++ wrapper-first design: https://arxiv.org/abs/2505.12425

## Code Added

Rust library API:

- `plan_crops_from_axis_element_graph_json`
- `AxisElementGraphCropBridgeReport`
- `AxisElementBridgeError`

Rust CLI target:

```text
chromalab_cv_bridge <axis_element_graph.json> <image_width> <image_height>
```

The CLI is now executable after SETUP-1 added a manual MSVC environment script. Earlier `vcvars64.bat` / `LaunchDevCmd.bat` calls failed in this shell, but direct MSVC/Windows SDK environment setup works.

## Toolchain Reality

Local Rust is installed under ignored:

`C:\VietnAi\Hromotograth\artifacts\rust-toolchain`

Installed:

- `rustc 1.96.0`
- `cargo 1.96.0`

MSVC/GCC linker is not on the default PATH. SETUP-1 found the installed MSVC/Windows SDK paths and added PowerShell environment scripts to run Rust tests and builds through MSVC.

DR-2B/SETUP-1 validation is:

- `cargo fmt --all -- --check`
- `cargo check --workspace --all-targets`
- `cargo test --workspace`
- Kotlin desktop compile sanity

The Rust bridge CLI was also run over all 18 DR-1R `axis_element_graph.json` packages.

## Validation Result

Command:

```powershell
.\tools\rust\Run-RustCoreChecks.ps1
```

- Rust fmt check passed.
- Rust workspace all-targets check passed.
- `cargo test --workspace` passed: 6 tests.
- Library, test modules, and CLI target compile.
- `chromalab_cv_bridge` processed all 18 DR-1R graph packages.

Additional command:

```powershell
.\gradlew.bat :composeApp:compileKotlinDesktop
```

Result:

- Passed.

## What This Proves

DR-2B proves:

- Rust can parse existing axis element graph evidence.
- Rust can preserve label-band semantics.
- Rust can reject invalid crop bands explicitly.
- Rust can expose a bridge report shape suitable for future PC/Android integration.

DR-2B does not prove:

- graph detection quality;
- OCR quality;
- calibration quality;
- Android JNI integration;
- production runtime speed.

## Next Step

Recommended next phase: DR-2C Android NDK + Rust Android target setup.

Goal:

1. Verify/install Android NDK.
2. Add Rust Android targets.
3. Build Rust library for Android ABIs.
4. Do not wire into production app until PC bridge parity is documented.
