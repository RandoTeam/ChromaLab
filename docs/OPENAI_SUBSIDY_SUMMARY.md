# OpenAI Subsidy Summary

Status: RP_9_OPENAI_SUBSIDY_SUMMARY_READY

This document is a concise reviewer-facing summary for OpenAI subsidy, API
credit, and Codex Security review. It is application material, not the main
public README.

For general repository messaging, use:

- [Root README](../README.md)
- [Public Messaging Guide](CHROMALAB_PUBLIC_MESSAGING_GUIDE.md)
- [Scientific Product Overview](CHROMALAB_SCIENTIFIC_PRODUCT_OVERVIEW.md)

License note: this repository now declares the Apache License 2.0 at the root.

## One-Page Pitch

ChromaLab is an Apache-2.0 open-source Android/Kotlin Multiplatform
research-alpha project for offline-first autonomous chromatogram image
analysis. It is being built so that a student, educator, or researcher can take
a chromatogram photo or select a screenshot, then receive an auditable analysis
path: graph detection, axis calibration, trace extraction, peak calculation,
evidence validation, and a scientific report that clearly separates verified
measurements from review-only or blocked evidence.

The project is especially relevant to GC/MS learning workflows, petroleum
geochemistry education, analytical chemistry teaching, and research contexts
where users may work with screenshots, printouts, exported images, or limited
access to commercial analysis tools.

The product principle is strict: deterministic code must own chromatographic
numeric measurements. Local AI can assist OCR, semantic classification,
warnings, Knowledge Pack grounded explanations, and report language, but it
must not invent retention times, peak areas, calibration coefficients, compound
identities, or other scientific measurements.

OpenAI support would help a sole maintainer move faster on a technically broad
public project: Android/KMP, Rust CV, local model runtime, scientific report
contracts, validation artifacts, security/privacy review, and automated
regression over real chromatogram fixtures.

## Repository Fit

ChromaLab is a serious public software project rather than a prompt demo. The
repository already contains:

- Android/Kotlin Multiplatform app infrastructure;
- camera/gallery input direction;
- deterministic chromatogram calculation logic;
- local model management and LiteRT/GGUF runtime work;
- evidence-gated report contracts;
- Android validation fixture runs and truth-audit documentation;
- benchmark and regression documentation;
- Rust computer-vision foundation work;
- public documentation for product, architecture, validation, local AI runtime,
  and report experience.

The current status is intentionally honest. Phase 9 validation still contains
blocked fixture classes, and the repository does not claim production-ready
autonomous analysis. The validation artifacts, blocked-case documents, and
truth-audit tables are part of the value: they show what works, what fails, and
what must be fixed before a release-quality scientific claim is made.

## Requested Support

ChromaLab would benefit from:

1. API credits for development and maintenance.
2. Codex Security support for repository security and privacy review.
3. Assistance that improves the engineering process, not cloud dependency in
   the user product.

The app's product direction remains offline-first and local-model oriented.
Credits would primarily support repository development, testing, review,
documentation, and validation.

## How API Credits Would Be Used

API credits would be used for engineering acceleration:

| Area | Planned use |
|---|---|
| Pull-request review | Review Android/KMP, Rust, native bridge, report-contract, and model-runtime changes. |
| Test generation | Add regression tests for graph detection, calibration, trace extraction, report gates, and model boundaries. |
| Fixture analysis | Summarize evidence packages, validator JSON/Markdown, overlays, reports, and blocked runs. |
| Scientific report QA | Check report language for overclaims, missing caveats, unsupported compound identification, and hidden uncertainty. |
| Security review | Inspect file handling, local storage, exports, logs, model import/delete flows, and native-runtime surfaces. |
| Documentation | Improve public README, validation summaries, contributor docs, privacy notes, and education-facing explanations. |
| On-device AI safety | Review prompts, structured JSON contracts, model-gating rules, VLM/OCR boundaries, and forbidden numeric fields. |

## Why Codex Security Matters

ChromaLab processes user-supplied chromatogram images and creates scientific
reports, evidence packages, logs, overlays, model files, and exported artifacts.
Even with an offline-first product, security and privacy are important because
the app handles local files and may share reports or diagnostic artifacts.

Codex Security would be valuable for reviewing:

- Android storage and file-provider behavior;
- import/export paths;
- report and evidence package separation;
- raw log exposure;
- diagnostic artifacts that should never enter user reports;
- model download/import/delete flows;
- dependency and native-runtime risks;
- path handling and accidental local path disclosure;
- Kotlin Multiplatform, Android, Rust, JNI, and C++ bridge surfaces.

The goal is to prevent preventable mistakes early: leaking private chromatogram
images, exporting debug data into user reports, mishandling local model files,
or introducing unsafe file operations.

## Technical Novelty

ChromaLab combines several hard areas:

- mobile chromatogram image digitization;
- graph layout detection from photos and screenshots;
- axis calibration and scale evidence;
- trace extraction and peak calculation;
- local OCR and local AI assistance;
- evidence-gated scientific reports;
- Android real-device validation;
- Rust CV exploration for performance and robustness;
- deterministic numeric authority with model-assistance boundaries.

The important technical idea is not "AI reads a chart." The important idea is a
structured pipeline where visual evidence, deterministic calculation, local AI
assistance, validators, and report gates each have explicit responsibilities.

## Education And Scientific Value

ChromaLab is aimed at students, educators, and researchers who need to inspect
chromatogram visuals and understand how analysis decisions are made.

Potential value:

- explain chromatogram analysis through visible evidence;
- support learning workflows where commercial tools are inaccessible;
- help users inspect graph detection, calibration, trace extraction, and peak
  evidence;
- keep scientific uncertainty visible;
- provide local/offline workflows for sensitive or unpublished scientific
  images;
- make blocked or review-only cases explicit instead of hiding them behind a
  polished output.

The project is relevant to analytical chemistry, GC/MS education, petroleum
geochemistry learning contexts, and adjacent research workflows. It is not
currently presented as certified laboratory software.

## Current Validation Status

Current evidence shows:

- real Android validation artifacts exist;
- all Phase 9J audited runs produced report/evidence outputs;
- E2B model-enabled mode did not regress deterministic graph count,
  calibration, trace, peak metrics, or report gates in the audited slice;
- several fixtures remain `REVIEW_ONLY`;
- some fixtures remain blocked by graph layout, axis scale, calibration, or
  TIC+ions layout propagation;
- zero current Android fixtures are claimed as `RELEASE_READY`;
- Phase 10 must not start until Phase 9 blockers are resolved or explicitly
  accepted with evidence.

Supporting documents:

- [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md)
- [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md)
- [Phase 9J Product Acceptance Table](PHASE9J_PRODUCT_ACCEPTANCE_TABLE.md)
- [Phase 9J Scientific Acceptance Table](PHASE9J_SCIENTIFIC_ACCEPTANCE_TABLE.md)
- [Phase 9J E2B Acceptance Matrix](PHASE9J_E2B_ACCEPTANCE_MATRIX.md)

## Proposed Milestones

| Milestone | Expected outcome | Evidence |
|---|---|---|
| Security/privacy baseline | Safer file handling, export separation, and diagnostic artifact policy | `SECURITY.md`, `PRIVACY.md`, reviewed export paths |
| Regression quality | More fixture truth data and stricter benchmark scoring | benchmark records, validator outputs, regression summaries |
| Graph/layout repair | Fewer blocked graph count and layout cases | Android fixture reruns, graph overlays, acceptance tables |
| Axis/scale repair | Stronger calibration evidence for screenshots/photos | calibration anchors, residuals, subreason tables |
| Report quality | Clearer evidence-gated reports and caveats | report contracts, HTML/Markdown exports, report QA |
| Model safety | Better local OCR/semantic assistance without numeric overreach | model diagnostics, forbidden-field rejection, E2B/E4B comparisons |

Each milestone should be auditable through repository commits, validation
artifacts, and public documentation updates.

## Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Overclaiming autonomous accuracy | Keep validation gates strict and publish truth-audit results. |
| AI inventing scientific values | Keep deterministic code as numeric authority and reject forbidden model fields. |
| Weak graph/calibration robustness | Build benchmark truth, compare methods, and keep blocked cases visible. |
| Privacy leakage through exports | Separate user reports, technical evidence, diagnostic bundles, logs, and model paths. |
| License and contribution clarity | Keep `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, and `PRIVACY.md` aligned with current project status. |
| Single-maintainer bandwidth | Use Codex support for review, tests, documentation, and security checks. |

## Field-Ready Application Answers

### Role

Primary maintainer.

### Why This Repository Fits

ChromaLab is a public Android/Kotlin Multiplatform project for offline-first
chromatogram image analysis. It aims to help students, educators, and
researchers turn chromatogram photos or screenshots into auditable evidence:
graph detection, axis calibration, trace extraction, peak calculation,
validation artifacts, and evidence-gated reports. The project is early but
technically serious, with Android/KMP infrastructure, deterministic calculation
logic, local model management, real validation fixtures, report contracts, and
Rust CV work.

### Areas Of Interest

- Codex Security.
- API credits for development and maintenance.

### API Credit Usage

API credits would support repository development: automated code review, test
generation, Android/KMP and Rust analysis, report-contract review, regression
analysis over chromatogram fixtures, safer on-device AI/OCR contracts, and
public documentation quality. The credits would accelerate engineering and
validation; they are not intended to make the user product cloud-dependent.

### Additional Context

ChromaLab is maintained by a single primary developer. The project is actively
developed and already tracks its limitations through truth audits, validators,
fixture matrices, report gates, and blocked-case documentation. Support would
help turn a research-grade prototype into a more reliable public tool for
education and scientific learning.

### Codex Security Rationale

ChromaLab handles user chromatogram images, local model files, generated
scientific reports, evidence packages, logs, and export/share workflows. Codex
Security would help review Android/KMP file handling, storage, exports,
diagnostic artifacts, model import/delete flows, dependencies, and native
runtime bridges so the project avoids preventable privacy and security issues.
