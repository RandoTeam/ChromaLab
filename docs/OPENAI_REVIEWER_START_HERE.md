# OpenAI Reviewer Start Here

Status: OPENAI_REVIEWER_ENTRY_READY

This one-page entry point is for OpenAI, Codex, subsidy, API credit, and
security reviewers who need the shortest honest path through the ChromaLab
repository.

## What ChromaLab Is

ChromaLab is an Apache-2.0 Android/Kotlin Multiplatform research project for
offline-first chromatogram image analysis. The product goal is to let a user
take a chromatogram photo or select a screenshot, then have the app analyze the
visual graph and produce an auditable report.

The intended analysis pipeline is:

```text
photo/screenshot
  -> graph/layout detection
  -> axis and scale calibration
  -> trace extraction
  -> deterministic peak calculation
  -> evidence validation
  -> scientific report/export
```

Local AI can help OCR, semantic classification, warnings, Knowledge Pack
grounded explanations, and report language. It must not create chromatographic
numeric metrics.

## Why It Matters

Many students, educators, and researchers work with chromatogram screenshots,
printouts, exported images, or photos of instrument software. Turning these
visuals into calibrated signals and peak tables is hard, especially when axes
are blurred, graph panels are stacked, or multiple ion traces appear together.

ChromaLab is trying to make that workflow inspectable:

- users can see what graph was detected;
- calibration evidence can be reviewed;
- trace and peak evidence can be exported;
- reports are gated as `RELEASE_READY`, `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, or
  `BLOCKED`;
- blocked cases remain visible instead of being hidden behind polished output.

## Current Validation Status

ChromaLab is not production-ready autonomous chromatogram analysis software.
Phase 9 runtime validation remains blocked by real fixture failures.

Current public evidence:

- Android validation fixtures exist;
- report and evidence exports are produced for audited runs;
- E2B model-enabled mode is treated as the supported FAST/weaker-device baseline
  and must not degrade deterministic graph count, calibration, trace, peak
  metrics, or report gates;
- review-only and blocked fixtures are documented;
- zero current Android fixtures are claimed as universally release-ready.

Start with:

- [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md)
- [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md)
- [Phase 9J Product Acceptance Table](PHASE9J_PRODUCT_ACCEPTANCE_TABLE.md)
- [Phase 9J Scientific Acceptance Table](PHASE9J_SCIENTIFIC_ACCEPTANCE_TABLE.md)
- [Phase 9J E2B Acceptance Matrix](PHASE9J_E2B_ACCEPTANCE_MATRIX.md)
- [Public Repository QA](CHROMALAB_PUBLIC_REPOSITORY_QA.md)

## What Codex/API Credits Would Help With

API credits and Codex support would help the maintainer improve:

- Android/KMP and Rust code review;
- regression test generation;
- validation fixture analysis;
- report contract and scientific language checks;
- model-runtime boundary checks;
- documentation quality;
- security/privacy review of local storage, exports, diagnostics, and model
  handling.

This support would accelerate development and review. It is not intended to make
the user product cloud-dependent.

## Security And Privacy Relevance

ChromaLab handles:

- chromatogram photos and screenshots;
- generated reports;
- runtime evidence packages;
- overlays and graph crops;
- diagnostic bundles;
- logs;
- local model files;
- native runtime bridges.

Security and privacy review matters because even offline-first software can leak
private images, local paths, raw logs, or debug artifacts through exports or
diagnostic bundles.

Read:

- [SECURITY.md](../SECURITY.md)
- [PRIVACY.md](../PRIVACY.md)
- [CONTRIBUTING.md](../CONTRIBUTING.md)

## Reviewer Takeaway

ChromaLab is credible because it is honest about its current state. It has a
serious architecture, real validation artifacts, local AI/runtime work, public
documentation, and OSS entry points. It still needs engineering work before
production autonomous analysis can be claimed.
