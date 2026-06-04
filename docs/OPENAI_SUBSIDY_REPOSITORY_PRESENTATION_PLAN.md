# ChromaLab Public Repository Presentation Plan

Status: PUBLIC_REPOSITORY_PRESENTATION_PLAN_READY

This document defines the phased plan for preparing the public ChromaLab repository for OpenAI subsidy, grant, research, and public reviewer evaluation. The goal is not only to make the repository look attractive, but to make it honest, scientifically credible, easy to understand, and easy to verify.

The repository must explain ChromaLab as a serious offline chromatogram analysis platform: a mobile-first scientific tool that combines deterministic chromatographic calculation, computer vision, local OCR, local LLM/VLM assistance, evidence-gated reporting, and privacy-preserving on-device execution.

## Current Repository Audit

The repository has strong technical substance, but the public presentation is not ready.

Observed strengths:

- Kotlin Multiplatform and Compose Multiplatform foundation.
- Android app with camera, gallery, model management, local chat, and analysis workflows.
- Deterministic chromatogram calculation engine with peak metrics, integration settings, baseline handling, and report parameters.
- Rust computer-vision core under `rust/chromalab-cv-core`.
- Local AI runtime work across LiteRT-LM, GGUF, llama.cpp, ML Kit OCR, and model compatibility gating.
- A large validation history across Android fixture runs, calibration benchmarks, OCR recovery experiments, and evidence-package validation.
- Documentation already exists for roadmap, pipeline, reports, model strategy, validation phases, and regression matrices.

Observed public-facing issues:

- `README.md` is currently corrupted with mojibake text and must be replaced with clean English.
- The public narrative is scattered across many phase documents and is hard for a reviewer to follow.
- The repository does not yet have a polished social preview, public documentation index, or complete contribution/security/privacy entry points.
- Real app screenshots should wait until the app can show current output honestly. Until then, public presentation should use report experience concepts and real validation evidence rather than fake screenshots.
- The scientific story is not presented clearly enough: deterministic calculation, AI assistance, evidence gates, and limitations must be separated.
- Validation evidence exists, but it needs a single human-readable summary that shows what works, what is blocked, and what remains experimental.
- The technology stack is strong, but it is not mapped clearly for a reviewer.
- Internal phase documents are valuable, but the public entry points need a cleaner structure.

## Presentation Principles

All public repository content must follow these principles:

- English only for public-facing repository pages.
- Public repository messaging must be separated from grant/subsidy application language.
- Honest status reporting: do not claim production accuracy that has not been validated.
- Scientific credibility before marketing language.
- Clear separation between deterministic calculations and AI-assisted interpretation.
- No unsupported medical, forensic, regulatory, or legal claims.
- No hidden failures: blocked Android fixtures, validation gaps, and model limitations must be described as engineering work in progress.
- No fake screenshots, fake metrics, fake reports, or hardcoded examples.
- Every strong claim should point to code, documentation, validation artifacts, or a known roadmap item.
- The repository should be understandable to three audiences: OpenAI reviewers, scientists/educators, and mobile developers.
- Grant-specific language such as API-credit requests, Codex Security interest, and sole-maintainer context belongs in subsidy documents, not the root README.

## Phase Plan

Work must proceed one phase at a time. Each phase should end with a focused commit, validation, and a short status report.

### RP-1: Public README Rewrite

Objective: Replace the corrupted README with a polished English public entry point.

Deliverables:

- New English `README.md`.
- Clear project title, tagline, and short value proposition.
- Public badges for release status, platform, language, offline-first design, and local AI.
- One-minute summary for reviewers.
- Problem statement: why chromatogram digitization and interpretation from photos/screenshots is hard.
- Product flow: take photo or select image, detect graph, calibrate axes, extract trace, detect peaks, validate evidence, generate report.
- Current status table: working, experimental, blocked, and planned.
- Technology stack table.
- Scientific responsibility statement.
- Quick start section for developers.
- Android beta/release section if current artifacts are available.
- Links to roadmap, pipeline, report spec, validation truth audit, and model strategy.

Acceptance gate:

- No mojibake.
- No Russian text in public README.
- No overclaiming of validation status.
- The first screen of the GitHub repository must immediately explain what ChromaLab is and why it matters.

Suggested commit message:

```text
Rewrite public README for reviewer presentation
```

### RP-2: Visual Identity And Icon

Objective: Create a clean visual identity that communicates chromatography, local AI, and scientific evidence without looking like a generic AI demo.

Deliverables:

- Repository icon concept.
- App icon assets or design specification.
- GitHub social preview image.
- README hero image or compact visual banner.
- Color and typography direction for public assets.
- Icon usage rules.

Design direction:

- Scientific, precise, calm, modern.
- Visual motifs may include a chromatogram trace, axis grid, evidence markers, and a compact local-AI signal.
- Avoid medical-diagnostic symbolism, exaggerated brain icons, noisy gradients, or misleading "magic AI" visuals.

Acceptance gate:

- Icon is recognizable at small sizes.
- Visuals support the scientific product story.
- Assets do not imply finished clinical or regulatory capability.

Suggested commit message:

```text
Add public ChromaLab visual identity assets
```

### RP-3: Scientific And Product Narrative

Objective: Write the deep English explanation of what the product does and why the architecture matters.

Deliverables:

- `docs/CHROMALAB_PUBLIC_MESSAGING_GUIDE.md` as the guardrail for repository-safe claims.
- `docs/CHROMALAB_SCIENTIFIC_PRODUCT_OVERVIEW.md`.
- Explanation of chromatograms, axes, calibration, traces, peaks, baseline, integration, S/N, FWHM, area percent, and report gates.
- User story for students, laboratory learners, researchers, and field users.
- Explanation of the autonomous target workflow.
- Explanation of why deterministic methods and local AI are both needed.
- Limitations and responsible-use section.

Core message:

ChromaLab is not trying to replace chromatography instruments. It is building an offline-first analysis assistant that can turn captured chromatogram visuals into auditable, evidence-gated calculations and educational/research reports.

Acceptance gate:

- A non-project reviewer can understand the scientific purpose.
- A scientific reviewer can see that the project is careful about evidence and uncertainty.
- AI is described as assistance, not numeric authority.
- The document uses repository-safe public messaging instead of copying grant application text.

Suggested commit message:

```text
Add scientific product overview
```

### RP-4: Architecture And Technology Map

Objective: Explain the full software architecture in a clean, reviewer-friendly way.

Deliverables:

- `docs/CHROMALAB_ARCHITECTURE_OVERVIEW.md`.
- Architecture diagram.
- Pipeline diagram.
- Runtime model map.
- Technology table:
  - Kotlin Multiplatform.
  - Compose Multiplatform.
  - Android.
  - Rust CV core.
  - ML Kit OCR and document scanning.
  - LiteRT-LM.
  - GGUF and llama.cpp.
  - Room and SQLite.
  - Kotlin serialization.
  - Koin.
  - Python benchmark and artifact tooling where applicable.
- Explanation of Android runtime, desktop validation, Rust migration, and evidence exports.

Acceptance gate:

- Reviewer can understand which languages and runtimes are used.
- Reviewer can see why Rust is being introduced.
- Reviewer can see which stages run locally and which stages are deterministic.

Suggested commit message:

```text
Add public architecture overview
```

### RP-5: Validation Evidence And Benchmark Summary

Objective: Convert the validation history into a clean public evidence story.

Deliverables:

- `docs/CHROMALAB_VALIDATION_SUMMARY.md`.
- Links to Phase 9J truth audit once available.
- Summary of Android fixture validation.
- Summary of desktop calibration and OCR recovery benchmarks.
- Summary of known blockers and next engineering fixes.
- Explanation of report gates:
  - RELEASE_READY.
  - REVIEW_ONLY.
  - DIAGNOSTIC_ONLY.
  - BLOCKED.
- Explanation of evidence artifacts:
  - runtime evidence package.
  - validator JSON/Markdown.
  - overlays.
  - calibration tables.
  - report contract JSON.

Acceptance gate:

- No hidden blocked cases.
- Public readers can see that validation is real, not marketing.
- The document makes the project more credible, not less.

Suggested commit message:

```text
Add public validation evidence summary
```

### RP-6: Model And Runtime Story

Objective: Explain local AI model support and why it matters for privacy, students, and weaker devices.

Deliverables:

- `docs/CHROMALAB_LOCAL_AI_RUNTIME.md`.
- Explanation of LiteRT-LM role.
- Explanation of Gemma E2B baseline FAST mode for weaker devices.
- Explanation of E4B/full-analysis target if available.
- Explanation of GGUF and llama.cpp support.
- Explanation of MTP speculative decoding for text-only GGUF chat.
- Explanation of model manager and compatibility checks.
- Safety rules:
  - AI cannot create chromatographic numeric metrics.
  - AI cannot override deterministic calibration.
  - AI can help OCR, semantic classification, warnings, and report explanations.

Acceptance gate:

- Local AI value is clear.
- Device and model limitations are honest.
- E2B baseline role is described as supported, not experimental-only.

Suggested commit message:

```text
Document local AI runtime strategy
```

### RP-7: Report Experience Concept

Objective: Describe the intended professional report experience without pretending the current Android app already renders a finished production UI.

Deliverables:

- `docs/CHROMALAB_REPORT_EXPERIENCE_CONCEPT.md`.
- Clear statement that this is a target design, not current production UI proof.
- Report information architecture:
  - gate summary;
  - source preview;
  - graph sections;
  - evidence gate matrix;
  - calibration evidence;
  - trace evidence;
  - peak table and peak detail cards;
  - scientific caveats;
  - model contribution boundaries;
  - technical appendix and exports.
- Design direction for a serious mobile scientific report.
- Rules for when real screenshots may be added later.

Acceptance gate:

- No fake screenshots or fake report metrics.
- The report target is understandable without reading internal phase documents.
- The concept keeps blocked/review/diagnostic states visible.
- The concept does not weaken evidence gates or imply production readiness.

Suggested commit message:

```text
Document report experience concept
```

### RP-8: Documentation Cleanup And Public Index

Objective: Make the documentation navigable for a public repository.

Deliverables:

- `docs/README.md` as documentation index.
- Public docs grouped by purpose:
  - Product overview.
  - Architecture.
  - Scientific pipeline.
  - Validation.
  - Model runtime.
  - Development.
  - Internal phase archive.
- Links from root README into the right docs.
- Cleanup of stale or corrupted text where public-facing.
- Optional `CONTRIBUTING.md`.
- Optional `SECURITY.md`.
- Optional `PRIVACY.md`.

Acceptance gate:

- A reviewer can find the important docs in under one minute.
- Internal phase history remains available but does not overwhelm the main public story.
- No broken public links.

Suggested commit message:

```text
Add public documentation index
```

### RP-9: OpenAI Subsidy Summary Pack

Objective: Prepare a concise English package specifically for grant/subsidy review.

Deliverables:

- `docs/OPENAI_SUBSIDY_APPLICATION_NARRATIVE.md`.
- `docs/OPENAI_SUBSIDY_SUMMARY.md`.
- One-page pitch.
- Field-ready application answers:
  - role;
  - repository eligibility;
  - areas of interest;
  - API-credit usage;
  - additional maintainer context;
  - Codex Security rationale.
- Impact statement.
- Technical novelty.
- Why local OpenAI-compatible AI matters.
- Student and education value.
- GC/MS, petroleum geochemistry, and analytical chemistry relevance.
- Scientific validation plan.
- Compute/model support request.
- Security and privacy review need:
  - user chromatogram images;
  - local reports;
  - evidence packages;
  - logs;
  - export/share workflows;
  - Android/KMP/Rust/native dependency surfaces.
- Milestones and expected outcomes.
- Risks and mitigation plan.
- License-status check before using the word "open-source" externally.

Acceptance gate:

- The document explains why the project deserves support.
- The ask is concrete.
- The proposed milestones are realistic and auditable.
- The application text is clear enough to paste into a subsidy form.
- The security rationale is explicit and tied to real repository risks.
- The text does not claim open-source licensing until the root license is added.
- The document remains clearly labeled as application material, not general README copy.

Suggested commit message:

```text
Add OpenAI subsidy summary
```

### RP-10: Final Public Repository QA

Objective: Check the public repository as if it were being reviewed today.

Deliverables:

- Fresh clone/readability checklist.
- Link check.
- Public secret/artifact review.
- README rendering review.
- GitHub social preview review.
- Release asset review.
- Documentation index review.
- Final public repository readiness note.

Acceptance gate:

- Public repository first impression is clean.
- No corrupted text remains in primary public docs.
- No private paths, accidental secrets, or oversized accidental artifacts are exposed.
- Validation status is honest and easy to find.

Suggested commit message:

```text
Complete public repository presentation QA
```

## Recommended Immediate Next Step

RP-1, RP-2, RP-3, RP-4, RP-5, RP-6, RP-7, RP-8, RP-9, and RP-10 are complete.

Reason:

- The root README now gives a clean first impression.
- The visual identity now gives the project a public scientific signal.
- The scientific/product overview now explains the product value without using grant-specific language.
- The architecture overview now explains how Kotlin Multiplatform, Android, Rust CV, LiteRT-LM, GGUF, evidence packages, reports, and validation fit together.
- The validation evidence summary now converts Phase 9J, DR-B benchmark scoring, and Rust parity work into a public reviewer-facing evidence story.
- The local AI runtime story now explains LiteRT-LM, E2B baseline, GGUF, llama.cpp, MTP, model safety boundaries, and privacy.
- The report experience concept now shows what a professional ChromaLab report should contain without adding fake screenshots or fake metrics.
- The public documentation index now helps reviewers find the main product, science, validation, runtime, report, and development docs quickly.
- The OpenAI subsidy summary now separates application material from general README messaging.
- The final public repository QA pass now checks links, readability, public claims, and accidental exposure risks.

The public presentation work can now move from documentation setup into policy cleanup or runtime engineering, depending on project priority.

## Public README Target Structure

The rewritten README should use this structure:

1. Hero: name, icon/banner, tagline.
2. One-sentence summary.
3. Why ChromaLab exists.
4. What the app does.
5. Current product status.
6. Scientific workflow.
7. Local AI and privacy model.
8. Technology stack.
9. Validation and known limitations.
10. Report experience concept and later real screenshots when current output is honest enough.
11. Developer quick start.
12. Documentation links.
13. Responsible-use statement.
14. Roadmap.
15. License and contribution status.

## Reviewer-Facing Claims To Use Carefully

Allowed if supported by current code/docs:

- Offline-first Android/KMP chromatogram analysis platform.
- Deterministic calculation engine for peak metrics.
- Local AI model manager with LiteRT-LM and GGUF support.
- Rust CV core work in progress.
- Evidence-gated validation and report contracts.
- Local OCR/VLM assistance for graph interpretation and explanations.

Avoid or qualify:

- Fully production-ready autonomous analysis.
- 99 percent success rate.
- Medical, forensic, legal, or regulatory suitability.
- Guaranteed compound identification.
- Guaranteed real-world calibration on arbitrary photos.
- AI-generated chromatographic numeric metrics.

## Definition Of Done For The Public Presentation Work

The public repository presentation is complete when:

- The root README is clean English and visually polished.
- The repository has a recognizable icon or visual identity.
- The main docs explain product purpose, scientific workflow, architecture, model runtime, validation, and limitations.
- A public documentation index exists and separates main docs from internal phase archive.
- Validation evidence is summarized honestly.
- Report experience is documented honestly, and real screenshots are added only when they show current app output without masking blocked/review states.
- OpenAI subsidy summary exists and is reviewer-ready.
- Public links render correctly.
- No primary public document contains corrupted text.
- Every completed phase has a focused commit.

Current RP-10 decision:

```text
PUBLIC_PRESENTATION_READY_WITH_DOCUMENTED_LIMITATIONS
```

Remaining public gaps are tracked in `docs/CHROMALAB_PUBLIC_REPOSITORY_QA.md`.
