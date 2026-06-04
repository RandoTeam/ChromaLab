# Security Policy

ChromaLab is a research-alpha Android/Kotlin Multiplatform project for
chromatogram image analysis. Security reports are welcome, especially for local
storage, exports, evidence packages, model files, and native runtime surfaces.

## Reporting A Vulnerability

Preferred path:

1. Use GitHub's private vulnerability reporting for this repository if it is
   available.
2. If private reporting is not available, open a minimal public issue titled
   `Security contact request` and do not include exploit details, private
   chromatogram images, logs, model paths, or personal data.

Do not post sensitive material publicly. In particular, do not attach:

- private chromatogram images;
- user reports;
- runtime evidence packages;
- logcat output containing paths or file names;
- model files;
- keystores or signing configuration;
- local `local.properties` values.

## Scope

Security-relevant areas include:

- Android storage and scoped-storage behavior;
- report export and share flows;
- evidence package and diagnostic bundle generation;
- user image import and local file handling;
- model download, import, activation, and deletion;
- local Knowledge Pack and report artifacts;
- native runtime surfaces, including JNI, Rust, C++, and llama.cpp/LiteRT
  integration;
- path handling and accidental local path disclosure;
- logs and diagnostics.

## Out Of Scope

Please do not report the following as security issues unless they expose private
data or create a real exploit path:

- known research-alpha analysis failures;
- review-only or blocked validation fixture results;
- scientific accuracy disagreements;
- missing production features;
- lack of certified laboratory, medical, forensic, or regulatory suitability.

## Project Security Posture

ChromaLab is designed as an offline-first/local-processing tool. That does not
remove security responsibility: local images, reports, logs, diagnostic bundles,
and model files still need careful handling.

The project is not yet security-audited. Treat public builds and artifacts as
research-alpha software.
