# OpenAI Subsidy Application Narrative

Status: DRAFT_FOR_REVIEW

This document converts the maintainer's application notes into a polished English narrative for OpenAI subsidy, API credit, and Codex Security review. It should be treated as an application-writing source document, not as a replacement for the public README.

This text is intentionally grant-oriented. For public repository messaging, use `docs/CHROMALAB_PUBLIC_MESSAGING_GUIDE.md` and adapt only the product, scientific, security, privacy, and education-relevance ideas. Do not copy funding-request language into the README.

License note: the repository now declares the Apache License 2.0 at the root.

## Role

Primary maintainer.

## Why This Repository Fits The Program

ChromaLab is a public Android/Kotlin Multiplatform project for automated chromatogram image analysis. The app is being built to take a chromatogram photo, screenshot, or future file import and turn it into an auditable scientific workflow: graph detection, axis and scale calibration, trace extraction, peak detection, evidence validation, and a report that clearly separates verified measurements from review-only or blocked evidence.

The project is useful for students, educators, and researchers working in GC/MS, petroleum geochemistry, analytical chemistry, and related laboratory-learning contexts where access to expensive commercial analysis tools may be limited. ChromaLab is designed as an offline-first tool so that users can learn, inspect, and validate chromatographic analysis without sending scientific images or reports to a cloud service by default.

The repository already contains Android/KMP infrastructure, a deterministic chromatographic calculation engine, local model management, evidence-gated report contracts, real Android validation fixtures, a local Knowledge Pack direction, Rust computer-vision work, and a growing regression/benchmark documentation set. The project is early, but it is active and technically serious.

## Areas Of Interest

- Codex Security.
- API credits for development and maintenance.

## How API Credits Would Be Used

API credits would support development and maintenance of the ChromaLab public project. The primary use is not to replace the app's offline model strategy, but to accelerate safe engineering work around the repository.

Planned uses:

1. Automated pull-request review for Android/KMP, Rust, native bridge, and report-contract changes.
2. Test generation and test maintenance for chromatogram processing, report generation, fixture validation, and model-runtime boundaries.
3. Android/KMP code analysis for lifecycle bugs, file-handling issues, model-loading regressions, and UI/report consistency.
4. Validation of scientific report language so that outputs do not overclaim, hide uncertainty, or imply unsupported compound identification.
5. Regression analysis over chromatogram fixture runs, evidence packages, overlays, validator JSON/Markdown, and final report contracts.
6. Safer development of an on-device AI/OCR pipeline by reviewing prompts, structured JSON contracts, model-gating rules, and failure handling.
7. Documentation and release-quality support for public README updates, validation summaries, security notes, and education-facing explanations.

The goal is to shorten the path toward a stable tool that students, educators, and researchers can realistically use and inspect.

## Why Codex Security Matters

ChromaLab handles user-supplied chromatogram images, local model files, generated scientific reports, runtime evidence packages, logs, and exported artifacts. Even when the product is offline-first, security and privacy still matter because the app reads files, stores data locally, processes images, exports reports, and may share artifacts with external tools or users.

Codex Security would be useful for reviewing:

- Android storage and file-provider handling;
- import/export paths and generated report artifacts;
- local logs and diagnostic output;
- evidence packages that may contain user images or analysis metadata;
- model download/import/delete workflows;
- dependency and native-runtime risks;
- unsafe sharing or accidental data disclosure;
- path handling, file permissions, and lifecycle cleanup;
- Kotlin Multiplatform, Android, Rust, JNI, and C++ bridge surfaces.

For a scientific public project, the security goal is to prevent preventable mistakes early: leaking private chromatogram images, exporting debug-only data into user reports, exposing local paths, mishandling model files, or allowing unsafe file operations.

## Additional Context For Reviewers

I am the sole maintainer and main developer of ChromaLab. The project is still early, but it is actively developed and has moved beyond a simple demo. It already includes:

- Android/Kotlin Multiplatform application infrastructure;
- local camera/gallery input flows;
- deterministic chromatogram calculation logic;
- evidence-gated report contracts;
- local AI model management;
- validation fixtures and artifact exports;
- work toward autonomous analysis of real chromatograms;
- Rust computer-vision experiments for faster and more robust graph/axis analysis.

Support from OpenAI/Codex would help turn this from an active research-grade prototype into a more reliable public tool for education and scientific learning.

## Short Application Answers

These versions can be used directly in short application fields.

### Role

Primary maintainer.

### Why This Repository Meets The Requirements

ChromaLab is a public Android/Kotlin Multiplatform project for automated chromatogram image analysis. It is designed to detect chromatogram graphs, axes, calibration evidence, traces, and peaks, then generate an auditable scientific report. The project is especially useful for students, educators, and researchers in GC/MS, petroleum geochemistry, and analytical chemistry where access to commercial tools may be limited. The repository already includes an Android/KMP pipeline, evidence-gated reporting, local model management, validation fixtures, and active work toward autonomous real-chromatogram analysis.

### Interested In

Codex Security and API credits for project development.

### How API Credits Would Be Used

API credits would support development and maintenance of ChromaLab: automated PR review, test generation, Android/KMP and Rust code analysis, report-contract validation, regression review over chromatogram fixtures, and safer development of the on-device AI/OCR pipeline. The goal is to accelerate a stable public release for students, educators, and researchers while keeping scientific evidence, privacy, and security central to the project.

### Additional Information

I am the sole maintainer and main developer of ChromaLab. The project is early but actively developed: it already has an Android/KMP pipeline, deterministic calculation logic, evidence-gated reports, a local Knowledge Pack direction, validation fixtures, and active work on autonomous analysis of real chromatograms. Codex support would help me move faster while keeping the tool safer, more reliable, and more useful for education and science.

### Why Codex Security Is Needed

ChromaLab processes user-supplied chromatogram images, generates scientific reports, stores runtime evidence packages, and exports files. Codex Security would help review Android/KMP code, file handling, local storage, exports, sharing, logs, dependencies, and native runtime bridges. This is important for preventing data leaks, unsafe exports, debug artifact exposure, and file-handling mistakes in a scientific public tool.

## Reviewer Narrative

ChromaLab sits at the intersection of scientific education, mobile computer vision, local AI, and trustworthy reporting. Its core product idea is intentionally practical: a student or researcher should be able to take a photo or choose a screenshot of a chromatogram and receive an analysis that explains what was detected, what was calculated, what evidence supports the result, and what still requires review.

The project is not trying to make AI the source of chromatographic truth. Measurements should come from deterministic and auditable code. AI is used where it is valuable: OCR assistance, semantic classification, warning explanations, Knowledge Pack grounded interpretation, and developer acceleration. This separation is central to the project's safety model.

OpenAI support would be especially valuable because the maintainer is working alone on a technically broad project: Android/KMP UI, native runtime integration, Rust computer vision, model management, OCR/VLM contracts, scientific report quality, validation artifacts, and security/privacy review. API credits and Codex Security would directly improve the quality and velocity of this work.
