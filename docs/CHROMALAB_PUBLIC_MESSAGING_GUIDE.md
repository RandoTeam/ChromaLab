# ChromaLab Public Messaging Guide

Status: PUBLIC_MESSAGING_GUARDRAILS_READY

This guide separates three communication layers:

1. Public repository messaging.
2. Scientific/product documentation.
3. Grant or subsidy application language.

The maintainer's OpenAI subsidy text is useful context, but it should not be copied directly into the public README. Grant applications explain why the project needs support. Repository documentation explains what the project is, what it can do, what it cannot yet do, and how a reader can verify it.

## Core Public Message

Use this as the stable public repository message:

> ChromaLab is a public Android/Kotlin Multiplatform research project for offline-first chromatogram image analysis. It aims to help students, educators, and researchers turn chromatogram photos or screenshots into auditable evidence: graph detection, axis calibration, trace extraction, peak calculation, validation artifacts, and scientific reports. Deterministic algorithms own the numeric chromatographic measurements; local AI assists OCR, semantic classification, warnings, and explanations.

This message is strong enough for public presentation while staying scientifically and legally careful.

## What Belongs In The Public Repository

The following ideas are appropriate for README and public docs:

- ChromaLab is a public Android/KMP chromatogram analysis project.
- The app targets students, educators, researchers, and laboratory-learning contexts.
- The project is relevant to analytical chemistry, GC/MS learning workflows, petroleum geochemistry contexts, and chromatogram interpretation from visual data.
- The product goal is automated analysis from photo/screenshot to evidence-gated report.
- The pipeline includes graph detection, axis/scale calibration, trace extraction, peak calculation, validation, and reporting.
- The app is offline-first and local-model oriented.
- Local AI supports OCR, semantic classification, warnings, explanations, and developer productivity.
- Deterministic code remains responsible for numeric measurements.
- The project is early and actively validated.
- Current limitations, blocked fixtures, and review-only reports are documented.
- Security and privacy matter because the app processes images, reports, evidence packages, logs, exports, local storage, and model files.

## What Belongs Only In Grant/Application Text

The following should not be prominent in the public README unless there is a specific context such as `OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md`:

- "I am applying for API credits."
- "I am interested in Codex Security."
- "I am the sole maintainer."
- "OpenAI support would help me move faster."
- Direct requests for subsidy, credits, or program support.
- Internal grant-field answers.
- Funding-specific framing that makes the repository look like a funding request instead of a product/research project.

These points are valid in the application narrative, but public repository visitors should first see the product, science, architecture, validation, and contribution story.

## Use With Caution

These phrases can be useful, but only with careful qualification.

| Phrase | Risk | Safer repository wording |
|---|---|---|
| `open-source` | The repository currently has no root license. | `public repository` or `public project` until a license is added. |
| `automatic analysis` | Can imply production reliability. | `automation target` or `automated workflow under validation`. |
| `autonomous production` | Current Phase 9 validation is not accepted. | `autonomous product goal` or `autonomous pipeline under active validation`. |
| `scientific report` | Can imply certified scientific validity. | `evidence-gated scientific report` with gate status and limitations. |
| `compound identification` | Requires explicit evidence and validated methods. | `semantic assistance` or `classification support`; avoid unsupported identification claims. |
| `safe` | Can imply audited security. | `privacy-conscious`, `offline-first`, or `designed for local processing`; add validation status. |
| `students and researchers can use it` | Can overstate readiness. | `built for students, educators, and researchers; current outputs require evidence review`. |
| `commercial-tool alternative` | Can imply parity with professional software. | `educational/research assistant where commercial tools may be inaccessible`. |

## Do Not Claim Yet

Do not claim these unless future validation and documentation support them:

- Production-ready autonomous chromatogram analysis.
- 99 percent success rate.
- Certified laboratory or regulatory use.
- Medical, forensic, legal, or industrial decision readiness.
- Guaranteed real-world graph calibration.
- Guaranteed compound identification.
- AI-generated chromatographic numeric metrics.
- Open-source licensing before the license is declared.

## Grant Text To Repository Text Conversion

| Grant/application idea | Repository-safe conversion |
|---|---|
| `I am the primary maintainer` | Add to a maintainer/contributing page, not the README hero. |
| `I need API credits for PR review and tests` | Explain that the project values automated QA, security review, and regression testing. |
| `Codex Security is needed` | Add a security/privacy roadmap item and later a `SECURITY.md` / `PRIVACY.md`. |
| `Useful for students, teachers, researchers` | Keep in README and scientific overview. |
| `GC/MS, petroleum geochemistry, analytical chemistry` | Include as domain relevance, but avoid implying validated coverage for every domain workflow. |
| `Evidence-gated reports and local Knowledge Pack` | Include, because this explains the product architecture. |
| `The project is early but active` | Include, because it is honest and credible. |
| `OpenAI support would help` | Keep in subsidy documents only. |

## Public Tone

The repository should sound:

- serious;
- scientific;
- useful to students;
- technically deep;
- honest about limitations;
- privacy-aware;
- optimistic without hype.

It should not sound:

- like a funding appeal on every page;
- like a finished certified lab product;
- like an AI demo;
- like a black-box compound identifier;
- like a closed internal experiment with no public structure.

## Recommended Next Writing Phase

The next public writing phase should be RP-3: Scientific And Product Narrative.

RP-3 should use this guide to produce `docs/CHROMALAB_SCIENTIFIC_PRODUCT_OVERVIEW.md` with:

- the scientific problem;
- target users;
- chromatogram pipeline explanation;
- deterministic calculation principles;
- local AI boundaries;
- evidence gates;
- education and research value;
- known limitations;
- responsible-use language.

RP-3 should not repeat the grant application fields. Those belong in `docs/OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md`.
