# DR-2A Rust CV Core Foundation

Status: `DR2A_FOUNDATION_READY`

Scope: Rust foundation only. This phase does not modify `CalculationEngine`, chromatographic math, Kotlin production analysis flow, validators, Android runtime behavior, or report gates.

## Task Classification

- Product architecture
- Rust deterministic CV core foundation
- Geometry / ROI / label-band contracts
- OCR crop evidence contracts
- QA / regression setup

## Agents Activated

- Orchestrator: kept this to one foundation slice, not a full rewrite.
- Research Intelligence: checked current Rust install/edition guidance and mobile integration direction.
- Geometry / Calibration Core: owned image/rectangle/axis/label-band contracts.
- OCR / VLM Text Semantics: enforced that title/metadata bands have no axis authority.
- Android Performance & On-Device AI: kept Android/Kotlin integration as a later FFI phase.
- QA / Regression: added Rust check script and ran `cargo check --tests`.
- Scientific Reporting / Validation: preserved evidence boundaries and no-calculation-change rule.
- Product Acceptance: approved Rust as the future deterministic core direction, but not as an accepted production migration yet.

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `geometry-calibration-robust-fit`
- `ocr-local-crops`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `test-plan-authoring`
- `definition-of-done`

## Current Research References

- Official Rust install page: https://www.rust-lang.org/tools/install
- Cargo install guidance: https://doc.rust-lang.org/cargo/getting-started/installation.html
- Rust 2024 edition guide: https://doc.rust-lang.org/edition-guide/editions/creating-a-new-project.html
- Rust 2024 resolver guidance: https://doc.rust-lang.org/beta/edition-guide/rust-2024/cargo-resolver.html
- Windows Rust setup guidance: https://learn.microsoft.com/windows/dev-environment/rust/setup

## Toolchain

System `rustc`/`cargo` were not installed. DR-2A installed a local ignored Rust toolchain under:

`C:\VietnAi\Hromotograth\artifacts\rust-toolchain`

Installed version:

- `rustc 1.96.0`
- `cargo 1.96.0`

The install used `rustup-init` with `--no-modify-path`, so system PATH was not changed.

## Workspace Added

Rust workspace:

`C:\VietnAi\Hromotograth\rust`

Crate:

`C:\VietnAi\Hromotograth\rust\chromalab-cv-core`

Initial crate purpose:

- define image geometry contracts;
- define safe rectangle clipping/intersection behavior;
- define axis and label-band types;
- define crop variant names matching DR-1R;
- define `plan_axis_label_crops` for safe crop planning from already supplied label bands;
- preserve explicit rejection reasons for empty/out-of-image crop bands.

## Important Boundary

DR-2A does not detect graph panels, axes, ticks, calibration, traces, or peaks.

It only creates the first Rust-side contract layer that later phases can use for:

1. PC mass benchmark execution.
2. Android FFI/JNI integration.
3. Deterministic geometry evidence export.
4. OCR crop generation and classification.

## Architecture Direction

Target split:

```text
Photo/screenshot
-> Kotlin acquisition/provenance
-> Rust deterministic CV core
   -> image geometry
   -> graph/panel layout
   -> axis/frame/grid geometry
   -> OCR crop plans
   -> label-to-geometry pairing
   -> calibration evidence
   -> trace masks/centerlines
-> OCR/VLM text-only assistance
-> Kotlin CalculationEngine/report contracts
```

Rust must not become numeric scientific authority beyond deterministic geometry/evidence. Chromatographic calculations remain in the existing calculation layer unless a separate approved phase proves an isolated bug.

## Validation

Command:

```powershell
.\tools\rust\Run-RustCoreChecks.ps1
```

This runs:

- `cargo fmt --all -- --check`
- `cargo check --workspace --tests`

`cargo test` is not part of DR-2A validation because this PC does not currently expose an MSVC/GCC linker on PATH. The test modules are compiled by `cargo check --tests`; executable test running can be enabled after Windows C++ Build Tools or another supported linker is available.

## Next Step

Recommended next phase: DR-2B Rust image/crop bridge prototype.

Goal:

- feed DR-1R crop-band data into Rust;
- produce the same accepted/rejected crop plan from Rust;
- compare Kotlin-generated crop metadata vs Rust crop metadata;
- still do not change production analysis behavior.
