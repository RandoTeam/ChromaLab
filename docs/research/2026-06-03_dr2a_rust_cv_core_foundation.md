# 2026-06-03 DR-2A Rust CV Core Foundation Research

Scope: Rust foundation and current setup guidance for ChromaLab's future deterministic CV core.

## Sources Checked

- Rust install page:
  https://www.rust-lang.org/tools/install
  - Relevant because Rust was not installed on this PC.
  - Adopted now: local rustup install with no system PATH modification.

- Cargo install guide:
  https://doc.rust-lang.org/cargo/getting-started/installation.html
  - Relevant because Cargo is the build/check entry point.
  - Adopted now: Rust workspace with `cargo check`.

- Rust 2024 project creation guidance:
  https://doc.rust-lang.org/edition-guide/editions/creating-a-new-project.html
  - Relevant because DR-2A starts a new crate.
  - Adopted now: `edition = "2024"`.

- Rust 2024 resolver guidance:
  https://doc.rust-lang.org/beta/edition-guide/rust-2024/cargo-resolver.html
  - Relevant because Rust 2024 implies resolver 3.
  - Adopted now: workspace `resolver = "3"`.

- Microsoft Windows Rust setup:
  https://learn.microsoft.com/windows/dev-environment/rust/setup
  - Relevant because native test binaries need a Windows linker.
  - Adopted now: document linker limitation and validate with `cargo check --tests`.

## Decision

Rust is now the planned deterministic CV core direction, but DR-2A only adds foundation contracts. Production Kotlin analysis, validators, reports, and chromatographic math are unchanged.
