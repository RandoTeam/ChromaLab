# Skill: source-quality-triage

**Skill ID:** `source-quality-triage`
**Skill type:** Research / Evidence / Validation
**Primary owner:** Research Intelligence Agent
**Secondary owners:** Orchestrator, QA / Regression Agent, Scientific Reporting & Validation Agent
**Status:** Authoritative expanded skill specification
**Applies to:** All ChromaLab phases and all research-backed implementation tasks

## Repository Integration

This file is the authoritative `source-quality-triage` skill definition for
ChromaLab's expanded orchestration system.

Registry links:

- `docs/agent-orchestration/expansion/config/additional_skills_registry.json`
- `docs/agent-orchestration/expansion/config/expanded_skills_registry.json`
- `docs/agent-orchestration/expansion/config/agent_activation_matrix.yaml`

## Skill Identity

`source-quality-triage` is the mandatory ChromaLab skill for deciding whether a source found through research is reliable enough to affect implementation, phase contracts, scientific claims, UX decisions, release gates, or runtime behavior.

It must be used with `current-web-research-deep` before implementation decisions. Research without source triage is incomplete.

## Mandatory Activation Triggers

Use this skill whenever research sources may influence:

- Android/KMP/Compose APIs, lifecycle, storage, permissions, performance, or accessibility;
- OCR, ML Kit, VLM, on-device model behavior, local inference, prompts, or structured outputs;
- geometry, calibration, chart digitization, image processing, curve extraction, or peak review methods;
- chromatographic concepts such as RT, height, area, FWHM, S/N, baseline, Kovats/retention index, uncertainty, or report wording;
- security, privacy, exported evidence packages, logs, user images, or sharing;
- UX/design decisions for guided workflows, report UI, validation flows, or error recovery;
- QA strategy, golden artifacts, real-device validation, or release acceptance.

Assume model knowledge may be outdated. Verify current sources and reject weak claims before they affect ChromaLab decisions.

## When Not To Use

Do not run a separate source-quality triage pass when:

- the task does not use external sources and only edits already-approved local orchestration wording;
- the change is mechanical formatting, path correction, or registry linking with no new technical or product decision;
- an existing fresh triage note already covers the exact source set and the current task does not change the decision;
- the user explicitly asks for explanation-only work and no repository decision will be made.

If any source is used to justify implementation, report claims, UX decisions, validation rules, security/privacy behavior, Android/KMP/API choices, OCR/VLM behavior, or chromatographic semantics, this skill must be used.

## Required Inputs

The triage pass requires:

- research question and target decision;
- list of candidate sources with URLs;
- source dates, versions, maintainers, or publication context where available;
- affected ChromaLab area and active phase;
- implementation or product claim the source might support;
- known risks, conflicts, or uncertainty;
- required acceptance criteria and validation type.

If a source has no provenance, date, maintainer, or reproducible method, mark it weak until corroborated.

## Required Outputs

This skill must output:

- source tier for every source;
- authority, freshness, specificity, reproducibility, bias, and implementation readiness assessment;
- accepted, background-only, and rejected source lists;
- conflict resolution when sources disagree;
- decision impact for each accepted source;
- limitations and risks;
- final statement: implementation allowed, review-only, or blocked.

## Source Tiering Rules

Prioritize sources in this order:

1. Official docs, current API references, standards, vendor manuals, and official sample repositories.
2. Maintained repositories with active issue/commit history and clear license.
3. Peer-reviewed papers, scientific references, and recognized domain literature.
4. Reputable engineering articles with reproducible details and corroboration.
5. Forums, discussions, and anecdotes as context only.
6. Marketing or benchmark claims as awareness only unless independently verified.

For ChromaLab release gates, a Tier 4 or weaker source cannot be the sole authority.

## Source Rejection Rules

Reject or downgrade a source when:

- it is undated and concerns a fast-changing API or runtime;
- it is a weak blog, uncited claim, copied snippet, or marketing post without reproducible evidence;
- it describes an obsolete Android/KMP/ML Kit/model/runtime version without compatibility proof;
- it recommends using VLM/LLM as numeric geometry or chromatographic measurement;
- it conflicts with official docs or standards without stronger evidence;
- it cannot be tested against ChromaLab artifacts;
- it lacks license clarity for copied code or examples.

Rejected sources may remain in the research note only to explain what not to adopt.

## Conflict Resolution Procedure

1. State the conflicting claims.
2. Classify each supporting source by tier and quality.
3. Prefer official/current API docs for platform behavior.
4. Prefer maintained implementation evidence for library behavior.
5. Prefer peer-reviewed, standards, or domain references for scientific definitions.
6. If evidence remains unresolved, downgrade the decision to review-only or block implementation.
7. Record the remaining risk and required validation.

## Recency Rules

High-recency topics require current sources:

- Android, Compose, KMP, ML Kit, storage/export, permissions, accessibility, lifecycle, and performance APIs;
- VLM/runtime/model formats, GPU/NPU/Vulkan/NNAPI behavior, model quantization, and on-device inference;
- security/privacy and sharing behavior;
- active libraries used in production.

Stable topics such as linear regression or basic chromatographic definitions may use older authoritative references, but current implementation constraints still require current sources.

## Domain-Specific Source Rules

- Android/KMP/Compose: official docs or official samples are required.
- OCR/ML Kit: official ML Kit documentation and project validation are required.
- VLM/model behavior: model/runtime documentation plus ChromaLab evaluation evidence are required.
- Geometry/calibration/chart digitization: technical method source plus implementation evidence and internal artifacts are required.
- Chromatography/scientific reporting: domain references, standards, or peer-reviewed sources are required.
- Security/privacy/export: official Android storage/sharing docs and privacy review are required.
- UX/design/accessibility: current platform guidelines and real workflow validation are required.

## ChromaLab-Specific Source Quality Rules

Sources must reinforce these product boundaries:

- AUTO_DIAGNOSTIC cannot be promoted to production without evidence gates.
- GUIDED_PRODUCTION requires user-confirmed or valid graphPanel, plotArea, calibration, trace, and report evidence.
- VLM/LLM is an OCR/semantic/judge assistant only, never a numeric measurement engine.
- Fixture hints and screenshot-specific observations are test-only unless generalized and validated.
- `CalculationEngine` changes require proof of an isolated bug after upstream input is validated.

Any source that encourages bypassing these boundaries must be rejected for production implementation.

## Research Handoff Format

Use this handoff after triage:

```markdown
## Source Quality Handoff

- Research note:
- Decision:
- Accepted sources:
- Background-only sources:
- Rejected sources:
- Conflicts:
- Confidence: HIGH / MEDIUM / LOW
- Implementation allowed: yes / review-only / no
- Required validation:
- Remaining risk:
```

## Failure Conditions

Triage fails or blocks implementation if:

- sources are missing for a release-critical decision;
- weak sources are used as primary authority;
- no official/current source was checked for a fast-changing API;
- scientific claims lack domain support;
- source conflicts are unresolved;
- source limitations are not documented;
- the recommendation cannot be validated in ChromaLab.

## Validation Checklist

Before accepting triage:

- [ ] Every source has a tier.
- [ ] Every accepted source has relevance and limitations.
- [ ] Weak sources are background-only or rejected.
- [ ] Recency expectations are satisfied or risk is recorded.
- [ ] Conflicts are resolved or escalated.
- [ ] Implementation decision is traceable to accepted sources.
- [ ] Required tests or artifacts are listed.

## Definition of Done

This skill is complete only when every source is classified, weak sources are prevented from driving implementation, accepted sources support the claimed decision, unresolved conflicts are escalated, and the Orchestrator can decide whether implementation is allowed, review-only, or blocked.

---

## 1. Purpose

This skill defines how ChromaLab agents evaluate the quality, freshness, authority, and implementation value of sources found during research.

It exists because "web research" is not enough. Agents must not treat every search result as equal. They must classify sources, reject weak sources, prefer primary evidence, identify stale APIs, detect speculative claims, and separate verified engineering methods from blog-level opinion.

This skill is mandatory whenever an agent uses external sources for:

- Android / Kotlin / Compose / KMP implementation;
- ML Kit or OCR behavior;
- on-device VLM inference;
- Gemma / Qwen / other local vision model use;
- image processing / chart digitization;
- chromatographic metrics and scientific interpretation;
- UX / UI / accessibility / localization;
- security, storage, exports, logs, privacy;
- testing, benchmarks, golden artifacts, and real-device validation;
- report provenance and release-quality gates.

---

## 2. Non-negotiable rule

A source is not usable merely because it was found.

Every source must be triaged before it can influence code, architecture, report wording, or acceptance criteria.

If source quality cannot be established, the agent must either:

1. downgrade the source to "background only";
2. find stronger corroborating sources;
3. mark the decision as unresolved;
4. ask Orchestrator for review.

---

## 3. Required source categories

Agents must classify each source into one of these categories.

### 3.1 Primary authoritative source

Examples:

- official Android / Google / Jetpack / ML Kit documentation;
- official Kotlin / Compose / KMP documentation;
- official model documentation from the model author/vendor;
- official library documentation;
- source code repository of the library itself;
- peer-reviewed paper for a scientific/computational method;
- recognized standards or vendor manuals relevant to chromatographic terminology.

Use for:

- APIs;
- implementation behavior;
- compatibility;
- safety constraints;
- scientific formula definitions;
- official limitations.

Weight: **highest**.

### 3.2 Maintained implementation source

Examples:

- active open-source repository;
- issue tracker discussion with maintainer involvement;
- current examples from official sample projects;
- widely used package with recent commits and clear license.

Use for:

- implementation patterns;
- edge cases;
- platform quirks;
- practical constraints.

Weight: **high**, but below official docs.

### 3.3 Peer-reviewed / technical literature

Examples:

- paper on curve extraction;
- paper on skeletonization;
- paper on chromatographic peak detection;
- paper on robust regression;
- paper on OCR/document analysis;
- academic survey.

Use for:

- algorithms;
- mathematical methods;
- validation criteria;
- domain reasoning.

Weight: **high for methods**, but not sufficient for Android API behavior.

### 3.4 Engineering blog / tutorial

Examples:

- developer blog;
- StackOverflow answer;
- Medium article;
- YouTube transcript;
- vendor marketing article.

Use for:

- idea generation;
- implementation hints;
- examples requiring verification.

Weight: **medium to low**.

Must be corroborated by stronger sources before influencing production code.

### 3.5 Community discussion

Examples:

- Reddit;
- forum;
- GitHub issue without maintainer confirmation;
- Discord/Telegram notes;
- comment threads.

Use for:

- symptoms;
- known pain points;
- user reports;
- possible edge cases.

Weight: **low**.

Do not use as sole source for production decisions.

### 3.6 Marketing / product claim

Examples:

- benchmark claims from vendors;
- model capability claims;
- vague "state-of-the-art" statements;
- product pages without technical details.

Use for:

- awareness only.

Weight: **very low** unless corroborated by benchmarks and docs.

---

## 4. Source quality scoring

Every source used in a research note must be scored.

### 4.1 Score dimensions

Use a 0–5 score for each dimension:

| Dimension | Meaning |
|---|---|
| Authority | Is the source primary/official/peer-reviewed? |
| Freshness | Is it current enough for the specific task? |
| Specificity | Does it directly answer the implementation question? |
| Reproducibility | Can the method/API/claim be tested? |
| Bias risk | Is the source marketing-heavy or self-serving? |
| Implementation readiness | Can it be translated into code safely? |

### 4.2 Required format

Each source entry must include:

```markdown
### Source: <title>

- URL:
- Source type:
- Date / version:
- Authority score: 0-5
- Freshness score: 0-5
- Specificity score: 0-5
- Reproducibility score: 0-5
- Bias risk: low / medium / high
- Implementation readiness: low / medium / high
- Use in this task:
- Limitations:
- Decision impact:
```

### 4.3 Minimum standard

A source can affect implementation only if:

- authority score >= 3, or it is corroborated by a stronger source;
- specificity score >= 3;
- implementation readiness is medium or high;
- limitations are documented.

---

## 5. Recency rules

The agent must consider whether the topic changes quickly.

### 5.1 High-recency topics

Require recent sources and/or official docs:

- Android SDK behavior;
- Jetpack Compose APIs;
- Kotlin Multiplatform APIs;
- ML Kit OCR APIs;
- on-device model runtimes;
- model formats and quantization;
- GPU/NPU/NNAPI/Vulkan/Metal delegate behavior;
- Android storage/export policy;
- privacy/security expectations;
- accessibility APIs.

Preferred source age:

- official docs: current online docs;
- examples: preferably within 12–18 months;
- libraries: maintained and compatible with current toolchain.

### 5.2 Medium-recency topics

- image processing implementations;
- chart digitization workflows;
- mobile UX patterns;
- report design;
- testing frameworks.

Preferred source age:

- recent if implementation-specific;
- older accepted methods allowed if still standard and verified.

### 5.3 Stable topics

- basic linear regression;
- robust fitting concepts;
- chromatographic definitions;
- trapezoidal integration;
- retention index formulas.

Older sources may be acceptable, but domain formulas still require authoritative citation.

---

## 6. Conflict handling

If sources disagree, the agent must not silently choose one.

Required conflict resolution:

1. identify conflicting claims;
2. classify each source;
3. prefer primary official docs for API behavior;
4. prefer peer-reviewed or domain standards for scientific definitions;
5. prefer maintained implementation evidence for library behavior;
6. document unresolved uncertainty;
7. escalate to Orchestrator if decision affects architecture or release gates.

Required format:

```markdown
## Source conflict

- Claim A:
- Supporting sources:
- Claim B:
- Supporting sources:
- Resolution:
- Confidence:
- Implementation decision:
- Remaining risk:
```

---

## 7. Anti-patterns

Agents must avoid:

1. using blog snippets as production truth;
2. copying unsupported code from random examples;
3. relying on old Android APIs without checking current docs;
4. using VLM marketing claims as proof of measurement accuracy;
5. treating a benchmark as evidence for ChromaLab's workflow;
6. citing a source but ignoring its limitations;
7. adopting a method without testing it on ChromaLab artifacts;
8. assuming desktop image-processing examples transfer to Android runtime;
9. assuming OCR confidence is calibrated without validation;
10. using community anecdotes as release criteria.

---

## 8. Required source quality gates by task type

### 8.1 Android / Compose / KMP UI

Minimum source set:

- official Android / Compose / Kotlin documentation;
- at least one current maintained example or official sample;
- accessibility guideline source if UI is user-facing.

### 8.2 OCR / ML Kit

Minimum source set:

- official ML Kit documentation;
- Android text recognition API behavior;
- at least one validation/benchmark source or internal ChromaLab benchmark;
- documented limitations.

### 8.3 VLM / local model inference

Minimum source set:

- official model card or vendor docs;
- runtime documentation;
- device resource constraints;
- internal evaluation harness results.

Marketing claims alone are never enough.

### 8.4 Geometry / calibration / chart digitization

Minimum source set:

- at least one algorithmic/technical source;
- one implementation-oriented source;
- internal ChromaLab regression artifact.

### 8.5 Chromatography / scientific metrics

Minimum source set:

- domain reference or recognized standard;
- explicit formula/definition;
- ChromaLab CalculationEngine behavior trace;
- caveat if image-derived metrics are not release-grade.

### 8.6 Report / provenance / release gate

Minimum source set:

- scientific reporting/audit/provenance reference;
- internal evidence package contract;
- QA release gate review.

### 8.7 Security / privacy / exports

Minimum source set:

- Android storage/export official docs;
- privacy/security guideline source;
- internal artifact redaction review.

---

## 9. Integration with `current-web-research-deep`

`current-web-research-deep` finds candidate sources.

`source-quality-triage` decides which sources are reliable enough to use.

Workflow:

```text
Research question
 -> current-web-research-deep
 -> source-quality-triage
 -> research synthesis
 -> implementation decision
 -> tests / validation
```

No research note is complete until source-quality triage is applied.

---

## 10. Integration with Orchestrator

The Orchestrator must reject a phase closeout if:

- research notes contain untriaged sources;
- implementation decisions rely on weak sources;
- high-recency topics used stale sources without explanation;
- source conflicts are unresolved;
- sources do not support the claimed decision;
- agent did not document limitations.

---

## 11. Output artifacts

This skill produces or modifies:

- `docs/research/YYYY-MM-DD_<topic>.md`
- source matrix sections inside research notes;
- source conflict notes;
- phase closeout research summary;
- Orchestrator review comments.

---

## 12. Research note source matrix template

```markdown
# Source Quality Matrix

| Source | Type | Authority | Freshness | Specificity | Reproducibility | Bias | Use | Decision |
|---|---|---:|---:|---:|---:|---|---|---|
|  |  |  |  |  |  |  |  |  |

## Accepted sources

1. ...

## Background-only sources

1. ...

## Rejected sources

1. ...

## Conflicts

1. ...
```

---

## 13. Acceptance criteria

This skill is correctly applied when:

1. every cited source has a quality classification;
2. every implementation-impacting source has enough authority or corroboration;
3. weak sources are marked background-only;
4. stale sources are not used for current APIs without verification;
5. conflicts are documented;
6. research limitations are explicit;
7. Orchestrator can trace every implementation decision back to reliable evidence.

---

## 14. Failure conditions

A task must be blocked or reopened if:

- it used untriaged sources;
- it used low-quality sources as implementation authority;
- it relied on VLM/vendor marketing for measurement claims;
- it adopted an API without official/current documentation;
- it changed scientific report semantics without domain validation;
- it closed a phase without research quality review.

---

## 15. Minimal checklist

Before implementation:

- [ ] Sources found.
- [ ] Sources classified.
- [ ] Current official docs checked where relevant.
- [ ] Source quality matrix completed.
- [ ] Conflicts documented.
- [ ] Accepted/rejected/background sources separated.
- [ ] Implementation decision tied to accepted sources.
- [ ] Limitations documented.
- [ ] Orchestrator review completed.
