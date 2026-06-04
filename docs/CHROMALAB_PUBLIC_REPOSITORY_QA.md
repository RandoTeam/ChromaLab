# ChromaLab Public Repository QA

Status: RP_10_PUBLIC_REPOSITORY_QA_COMPLETE

Date: 2026-06-04

This document records the final public-presentation QA pass for the current
ChromaLab repository state. It checks the repository as a public reviewer would
read it today: first impression, links, claims, validation honesty, artifact
exposure, local configuration risk, and remaining public documentation gaps.

This QA pass does not claim that ChromaLab is production-ready. It only checks
whether the public repository presentation is coherent and honest.

## Scope

Primary public documents checked:

- [Root README](../README.md)
- [Documentation Index](README.md)
- [Scientific Product Overview](CHROMALAB_SCIENTIFIC_PRODUCT_OVERVIEW.md)
- [Architecture Overview](CHROMALAB_ARCHITECTURE_OVERVIEW.md)
- [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md)
- [Local AI Runtime](CHROMALAB_LOCAL_AI_RUNTIME.md)
- [Report Experience Concept](CHROMALAB_REPORT_EXPERIENCE_CONCEPT.md)
- [Visual Identity](CHROMALAB_VISUAL_IDENTITY.md)
- [Public Messaging Guide](CHROMALAB_PUBLIC_MESSAGING_GUIDE.md)
- [OpenAI Subsidy Summary](OPENAI_SUBSIDY_SUMMARY.md)
- [OpenAI Subsidy Application Narrative](OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md)
- [OpenAI Subsidy Repository Presentation Plan](OPENAI_SUBSIDY_REPOSITORY_PRESENTATION_PLAN.md)

Repository surfaces checked:

- root README;
- public documentation index;
- brand assets under `assets/brand/`;
- tracked artifact and binary exposure;
- ignored local build, artifact, log, and signing configuration paths;
- primary public link graph;
- primary public claim consistency.

## Reviewer First Impression

Result: PASS.

The root README now presents ChromaLab as a serious Android/Kotlin
Multiplatform chromatogram analysis project. It explains:

- product goal;
- scientific problem;
- deterministic calculation boundaries;
- local AI assistance boundaries;
- current validation status;
- technology stack;
- documentation entry points;
- responsible-use and license caveats.

The README does not claim release-ready autonomous analysis, certified
laboratory use, guaranteed calibration, compound identification, or AI-generated
chromatographic metrics.

## Documentation Navigation

Result: PASS.

`docs/README.md` now acts as the public documentation index. It separates:

- first-read documents;
- current product status;
- scientific pipeline;
- reports and evidence;
- local AI and model runtime;
- Rust CV and deep research;
- development and orchestration;
- public presentation and application materials;
- internal phase archive.

This avoids forcing a reviewer to discover the project through internal
`PHASE*`, `DR*`, and `research/*` documents first.

## Link Check

Result: PASS for checked public documents.

Checked local Markdown links in:

- `README.md`;
- `docs/README.md`;
- `docs/OPENAI_SUBSIDY_SUMMARY.md`;
- `docs/OPENAI_SUBSIDY_REPOSITORY_PRESENTATION_PLAN.md`;
- `docs/CHROMALAB_VALIDATION_SUMMARY.md`.

All checked local links resolved.

## README Rendering Review

Result: PASS by source inspection.

README contains:

- centered project title;
- concise badge row;
- SVG hero image from `assets/brand/chromalab-hero.svg`;
- Mermaid pipeline diagram;
- readable tables for capabilities, evidence gates, technology stack, and
  repository map;
- clear documentation links;
- responsible-use and license caveats.

Not performed:

- live GitHub rendering screenshot;
- GitHub social preview screenshot.

Reason: this QA pass ran locally. The source structure is ready for GitHub
rendering, but a final visual browser check can still be done after pushing.

## GitHub Social Preview And Brand Assets

Result: READY_FOR_MANUAL_GITHUB_CONFIGURATION.

Brand assets exist:

- `assets/brand/chromalab-icon.svg`;
- `assets/brand/chromalab-logo-lockup.svg`;
- `assets/brand/chromalab-hero.svg`;
- `assets/brand/chromalab-social-preview.svg`.

The social preview asset is available locally, but GitHub repository social
preview configuration is a repository-hosting action and was not changed in this
local QA pass.

## Public Secret And Local Configuration Review

Result: PASS for tracked sensitive binary exposure; WATCH for local ignored
configuration.

Checks performed:

- no tracked APK/AAB/DEX package files were found;
- no tracked keystore/JKS/signing key files were found;
- `artifacts/`, `build/`, `logs/`, `.gradle/`, `.kotlin/`, and
  `local.properties` are ignored;
- `local.properties` exists locally and contains signing-related configuration,
  but it is not tracked by Git.

Required handling:

- do not commit `local.properties`;
- do not publish local signing values;
- keep release signing files outside Git;
- prefer a documented secret-management path before public release automation.

Known tracked findings:

- source code contains expected signing-property names such as
  `RELEASE_STORE_PASSWORD` and `RELEASE_KEY_PASSWORD` because Gradle reads them
  from local properties;
- source code contains many normal uses of the word `token` in model/runtime
  code and docs;
- internal runtime-validation XML/log artifacts exist under documentation
  history and are not primary public entry points.

No tracked secret values were intentionally added by this public presentation
work.

## Artifact Exposure Review

Result: PASS for primary public presentation; WATCH for repository size and
internal archive clarity.

`artifacts/` is ignored and is described as local validation output. The public
documentation links to truth-audit summaries rather than asking reviewers to
start from large raw artifact folders.

Internal phase and runtime-validation history remains available. This is useful
for auditability, but it should not replace the public documentation index.

## Claim Consistency Review

Result: PASS.

The primary public docs consistently say:

- ChromaLab is under active validation;
- Phase 9 is not accepted as production autonomous analysis;
- blocked and review-only cases remain;
- local AI assists OCR/semantic/warning/report work;
- deterministic code owns geometry, calibration, trace, peak metrics, and
  report gates;
- E2B is a supported FAST/weaker-device baseline but cannot override
  deterministic evidence;
- no release-ready autonomous claim is made for all fixtures;
- no compound identification guarantee is made;
- no root open-source license is declared yet.

## Validation Honesty

Result: PASS.

The repository now exposes validation status through:

- [Validation Evidence Summary](CHROMALAB_VALIDATION_SUMMARY.md);
- [Phase 9J Autonomous Analysis Truth Audit](PHASE9J_AUTONOMOUS_ANALYSIS_TRUTH_AUDIT.md);
- [Phase 9J Product Acceptance Table](PHASE9J_PRODUCT_ACCEPTANCE_TABLE.md);
- [Phase 9J Scientific Acceptance Table](PHASE9J_SCIENTIFIC_ACCEPTANCE_TABLE.md);
- [Phase 9J E2B Acceptance Matrix](PHASE9J_E2B_ACCEPTANCE_MATRIX.md);
- [Phase 9J Engineering Next Fixes](PHASE9J_ENGINEERING_NEXT_FIXES.md).

The public story does not hide that several fixtures remain blocked or
review-only.

## Application Material Separation

Result: PASS.

Grant/subsidy material is separated from general README messaging:

- `docs/OPENAI_SUBSIDY_SUMMARY.md` is a concise reviewer summary;
- `docs/OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md` is longer application source
  text;
- `docs/CHROMALAB_PUBLIC_MESSAGING_GUIDE.md` explains which grant-language
  ideas should not be copied into the general README.

## Remaining Public Gaps

These gaps remain after RP-10:

1. No root `LICENSE` file is declared.
2. No root `CONTRIBUTING.md` exists.
3. No root `SECURITY.md` exists.
4. No root `PRIVACY.md` exists.
5. GitHub social preview should be configured manually using
   `assets/brand/chromalab-social-preview.svg`.
6. Real app screenshots should wait until current app output can be shown
   honestly without masking blocked/review states.
7. Internal phase history is still large; the public documentation index now
   mitigates this, but future cleanup may archive or group phase history more
   tightly.

These are public-presentation gaps, not runtime fixes.

## Final RP-10 Decision

Public repository presentation status:

```text
PUBLIC_PRESENTATION_READY_WITH_DOCUMENTED_LIMITATIONS
```

Meaning:

- the root README is presentable;
- the documentation index is navigable;
- visual identity assets exist;
- product, science, architecture, validation, local AI runtime, report concept,
  and subsidy materials are documented;
- public claims are honest;
- no production-readiness claim is made;
- remaining policy and social-preview gaps are explicit.

This does not change the product validation verdict. ChromaLab remains a
research/education project under active validation, and Phase 9 runtime blockers
must still be resolved before production autonomous analysis can be claimed.
