# 2026-06-03 DR-2B Rust Bridge Correctness Research

Scope: current Rust correctness, FFI, Android, and image-processing research for the first ChromaLab Rust bridge prototype.

## Sources Checked

- Rust 2024 unsafe extern blocks:
  https://doc.rust-lang.org/edition-guide/rust-2024/unsafe-extern.html
  - Relevant because future Android/JNI boundaries will need explicit unsafe FFI.
  - Adopted now: keep DR-2B core safe and avoid FFI until a dedicated phase.

- Rust Reference external blocks:
  https://doc.rust-lang.org/reference/items/external-blocks.html
  - Relevant because non-Rust language boundaries are inherently unsafe.
  - Adopted now: bridge remains JSON/library/CLI compile target, not JNI.

- Rustonomicon FFI:
  https://doc.rust-lang.org/nomicon/ffi.html
  - Relevant because future exported structs/functions need `repr(C)` and safe wrappers.
  - Adopted now: do not expose FFI types prematurely.

- Android targets in the rustc book:
  https://doc.rust-lang.org/rustc/platform-support/android.html
  - Relevant because Android native integration is a future target.
  - Adopted now: keep Android link/build as a later phase that must use the NDK.

- imageproc docs:
  https://docs.rs/imageproc/latest/imageproc/
  - Relevant as a pure Rust image-processing candidate.
  - Adopted now: no dependency yet; first validate geometry bridge.

- Kornia-rs paper:
  https://arxiv.org/abs/2505.12425
  - Relevant because it supports a typed Rust CV direction instead of wrapper-first OpenCV dependency.
  - Adopted now: document that Rust core should be typed/evidence-first.

## Decision

The correct Rust path is not a shallow rewrite. The sequence should be:

1. typed evidence contracts;
2. bridge validation against current artifacts;
3. executable/linker setup;
4. corpus comparison;
5. image crop rendering;
6. graph/panel/layout algorithms;
7. FFI/JNI only after the core proves itself on PC artifacts.
