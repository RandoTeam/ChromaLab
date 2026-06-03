# SETUP-1 Workstation And Execution Plan

Status: `SETUP1_READY_FOR_INCREMENTAL_PHASES`

Purpose: make ChromaLab development repeatable, evidence-driven, and fast enough for PC-first algorithm work before Android runtime validation.

## Operating Rule

We work in short phases. Each phase must produce:

1. exact scope;
2. files changed;
3. artifacts generated;
4. tests/checks run;
5. what improved;
6. what remains blocked;
7. commit hash.

No phase is accepted from a summary alone.

## Agents Activated

- Orchestrator: owns phase order and prevents mixed-scope work.
- Research Intelligence: checks current official docs before toolchain/runtime choices.
- Geometry / Calibration Core: owns deterministic CV/Rust stage boundaries.
- OCR / VLM Text Semantics: owns text-only OCR/VLM boundaries.
- Android Performance & On-Device AI: owns future Android/NDK/model runtime constraints.
- Trace Extraction / Peak Review: owns downstream trace/peak stages in the pipeline map.
- Chromatography SME: owns scientific interpretation and graph-class expectations.
- Scientific Reporting / Validation: owns report gates, evidence, and caveats.
- QA / Regression: owns tests, corpus runs, and reproducible scripts.
- Product Acceptance: blocks technically passing but product-useless outcomes.
- Security / Privacy: owns artifact paths, local installs, and export safety.

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `geometry-calibration-robust-fit`
- `ocr-local-crops`
- `trace-extraction-masks`
- `peak-review-integration`
- `chromatography-domain-review`
- `scientific-report-provenance`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `android-runtime-profiling`
- `secure-export-review`
- `test-plan-authoring`
- `definition-of-done`

## Current Workstation State

| Area | Status | Evidence |
| --- | --- | --- |
| CPU | ready | AMD Ryzen 5 5500U, 6 cores / 12 logical processors |
| RAM | ready | about 32 GB |
| Gradle/Kotlin desktop | ready | `:composeApp:compileKotlinDesktop` passes |
| Rust local toolchain | ready | local ignored `artifacts/rust-toolchain`, Rust 1.96.0 |
| MSVC Build Tools | ready | VS Build Tools 2022 17.14.33, MSVC 14.44.35207 |
| Windows SDK | ready | 10.0.26100.0 |
| Rust linker | ready | manual MSVC env script runs `cargo test` |
| Rust tests | ready | 6 tests passed |
| Rust bridge corpus | ready | 18 graph packages processed |
| OCR PC runtime | partial | RapidOCR/ONNX temporary venv works; PaddleOCR v5/Tesseract not yet benchmarked |
| Android NDK | not yet verified | next setup subphase |
| Android device validation | not part of this slice | use later runtime phases |

## Repeatable Commands

Rust checks:

```powershell
.\tools\rust\Run-RustCoreChecks.ps1
```

Rust tests through MSVC:

```powershell
.\tools\rust\Run-RustCoreTestsMsvc.ps1
```

Rust bridge corpus:

```powershell
.\tools\rust\Run-RustBridgeCorpus.ps1
```

Kotlin desktop sanity:

```powershell
.\gradlew.bat :composeApp:compileKotlinDesktop
```

## What Was Fixed In SETUP-1

Previously Rust could only be compile-checked because linker discovery failed. The machine already had Build Tools, but Visual Studio batch environment scripts failed in this shell. SETUP-1 added a manual MSVC environment script:

- `tools/rust/Enter-MsvcRustEnvironment.ps1`
- `tools/rust/Run-RustCoreTestsMsvc.ps1`

This uses:

- MSVC `14.44.35207`;
- Windows SDK `10.0.26100.0`;
- local Rust toolchain under ignored `artifacts/rust-toolchain`.

Result: `cargo test --workspace` now runs successfully.

## Rust Bridge Corpus Result

Input:

`build/dr1r-axis-label-crop-sweep`

Output:

`build/setup1-rust-bridge-corpus`

| Metric | Value |
| --- | ---: |
| graph packages | 18 |
| source label bands per graph | 3 |
| accepted Rust crop plans | 54 |
| rejected Rust crop bands | 0 |

This proves the Rust bridge can consume current ChromaLab axis-element evidence and produce crop plans for every current graph package.

## Phase Order

1. `SETUP-1`: workstation and phase discipline foundation. Current phase.
2. `DR-2C`: Android NDK + Rust Android target setup.
3. `DR-2D`: Rust bridge corpus comparison against Kotlin DR-1R metadata.
4. `DR-2E`: Rust crop rendering implementation and visual parity contact sheets.
5. `DR-2F`: OCR text classification and forbidden-number filtering.
6. `DR-2G`: OCR label-to-geometry pairing.
7. `DR-2H`: axis scale/calibration evidence from Rust.
8. `DR-2I`: graph layout classifier in Rust.
9. `DR-2J`: trace mask/centerline prototype in Rust.
10. `DR-2K`: peak evidence handoff and report contract integration.
11. `DR-2L`: Android JNI/FFI bridge.
12. `DR-2M`: full PC corpus benchmark.
13. `DR-2N`: Android real-device validation.

Each phase must close with docs, validation, and a focused commit.

## Immediate Next Step

Recommended next phase:

`DR-2C Android NDK + Rust Android target setup`

Goal:

- verify Android SDK/NDK path;
- install NDK if missing;
- add Rust Android targets;
- build Rust library for Android ABIs;
- do not wire it into production app yet.
