# Skill: current-web-research-deep

**Skill ID:** `current-web-research-deep`
**Category:** Research / Source Validation / Technical Due Diligence
**Owner Agent:** `research_intelligence_agent`
**Required For:** All implementation phases and any task that depends on current libraries, APIs, models, mobile UX practice, scientific methods, security/privacy rules, or performance constraints.
**Status:** Authoritative skill specification.

## Repository Integration

This file is the authoritative `current-web-research-deep` skill definition for
ChromaLab's expanded orchestration system.

Registry links:

- `docs/agent-orchestration/expansion/config/additional_skills_registry.json`
- `docs/agent-orchestration/expansion/config/expanded_skills_registry.json`
- `docs/agent-orchestration/expansion/config/agent_activation_matrix.yaml`

## Skill Identity

`current-web-research-deep` is the mandatory ChromaLab research skill for any phase or task that depends on current technical methods, APIs, libraries, UX practices, VLM/OCR behavior, Android/KMP behavior, scientific reporting, or chromatographic claims.

It is paired with `source-quality-triage`. This skill discovers and organizes sources; source-quality triage decides whether those sources are strong enough to influence implementation or product claims.

## Mandatory Activation Triggers

Use this skill before implementation or phase approval when work involves:

- Android, Kotlin, KMP, Compose, CameraX, ML Kit, scoped storage, export, permissions, lifecycle, accessibility, localization, or performance APIs;
- chart digitization, image preprocessing, graphPanel/plotArea detection, homography, axes, ticks, calibration, skeletonization, curve extraction, or peak review methods;
- OCR, VLM, local model inference, structured model output, prompt contracts, model memory/latency, or hallucination controls;
- chromatographic terminology, RT, height, area, FWHM, S/N, baseline, Kovats/retention index, or scientific report language;
- mobile UX flow, guided calibration, zoom/pan annotation, report UI, error recovery, or user confirmation;
- security/privacy for user images, logs, runtime evidence packages, report sharing, or exported artifacts;
- test strategy, golden artifacts, real-device validation, benchmark methodology, or release gates.

Assume model knowledge may be outdated for these areas. Do not proceed from memory alone.

## When Not To Use

Do not run a new deep research cycle when:

- the task is a pure mechanical edit to already-approved docs with no technical decision;
- the user asks only to copy, move, or link existing files;
- the current phase already has a fresh, directly applicable research note and the task does not change the decision;
- the work is a formatting-only change that does not affect APIs, methods, UX, report claims, validation, or runtime behavior.

Even in these cases, do not contradict existing research notes or source-quality decisions.

## Required Inputs

Before research starts, collect:

- exact research question and decision to be made;
- active phase and affected ChromaLab area;
- platform and runtime constraints;
- current code path or docs to inspect;
- relevant failure classes, evidence artifacts, or validation gaps;
- acceptance criteria and forbidden changes;
- source-quality requirements from `source-quality-triage`;
- expected output location under `docs/research/`.

If the research question is too broad, split it into focused questions before searching.

## Required Outputs

This skill must produce:

- a markdown research note under `docs/research/YYYY-MM-DD_<phase>_<topic>.md`;
- source table with links, source type/tier, date or version, relevance, and limitations;
- findings tied to ChromaLab decisions;
- option comparison when multiple methods exist;
- adoption and rejection rationale;
- risks and assumptions;
- implementation guardrails;
- required tests and validation;
- handoff summary for the Orchestrator and implementing agents.

Weak blogs, uncited claims, stale examples, and vendor marketing may be listed as context only. They must not drive implementation decisions.

## Research Query Planning Procedure

1. Convert the task into one or more decision questions.
2. Identify the source types needed: official docs, maintained repositories, peer-reviewed sources, standards, current API references, benchmarks, or domain references.
3. Define recency expectations. Android/KMP, ML Kit, VLM/runtime, model, storage, and UX practices require current sources.
4. Define what would change in ChromaLab if the research is accepted.
5. Define what not to adopt before searching, such as VLM numeric measurement or fixture-specific shortcuts.

## Source Discovery Procedure

1. Search official documentation first.
2. Search maintained repositories and official samples second.
3. Search peer-reviewed, standards, or domain references for scientific and algorithmic claims.
4. Use engineering blogs, forums, and discussions only for context, symptoms, or implementation caveats.
5. Capture source dates, versions, maintainers, and license/maintenance status where relevant.
6. Stop and escalate if only weak or contradictory sources exist for a release-critical decision.

## Source Quality Requirements

Implementation-impacting decisions require reliable sources:

- official docs, current API docs, maintained repositories, peer-reviewed sources, and standards take priority;
- weak blogs, uncited claims, unmaintained examples, forum anecdotes, and marketing claims cannot be sole authority;
- conflicting sources must be documented and resolved through `source-quality-triage`;
- every accepted source must have clear relevance and limitations;
- every rejected or background-only source must be marked as such.

## Source Capture Format

Each source entry must include:

```markdown
### Source: <title>

- URL:
- Source type / tier:
- Date or version:
- Maintainer / publisher:
- Why it is relevant:
- Limitations:
- ChromaLab decision impact:
- Use status: accepted / background-only / rejected
```

## Research Synthesis Handoff

The handoff to implementation or phase closeout must include:

- research note path;
- confidence label: `HIGH`, `MEDIUM`, or `LOW`;
- selected approach;
- rejected alternatives;
- affected files or contracts;
- required tests and validation artifacts;
- risks and rollback plan;
- explicit statement that source-quality triage was applied.

Implementation must not start if the handoff is missing or confidence is too low for a release-critical decision.

## ChromaLab-Specific Research Areas

Research must be tailored to ChromaLab's product boundaries:

- AUTO_DIAGNOSTIC is diagnostic unless every evidence gate passes.
- GUIDED_PRODUCTION is the reliable target path.
- MANUAL_ADVANCED is the fallback for difficult graphs.
- VLM/LLM may assist with OCR, text classification, overlay judging, and warning explanation only.
- VLM/LLM must not provide exact numeric geometry, RT, height, area, FWHM, S/N, baseline, Kovats, final peak count, or chromatographic metrics.
- `CalculationEngine` must not be rewritten unless a proven isolated bug exists after validated upstream input.

## Failure Conditions

Research fails or must be reopened if:

- no current official or maintained source was checked for a high-recency topic;
- source quality was not triaged;
- implementation decisions depend on weak blogs, uncited claims, stale snippets, or marketing;
- model/VLM capability claims are not verified by documentation or project-specific tests;
- scientific claims lack chromatography/domain support;
- the research note does not explain what not to adopt;
- the recommendation cannot be validated with ChromaLab artifacts.

## Validation Checklist

Before the Orchestrator accepts this skill's output, verify:

- [ ] Research question is specific.
- [ ] Official docs or current primary sources were checked when available.
- [ ] Maintained repository or implementation evidence was checked where relevant.
- [ ] Peer-reviewed, standards, or domain sources were checked for scientific claims.
- [ ] Weak sources are marked background-only or rejected.
- [ ] Source-quality triage is present.
- [ ] Decision impact and risks are explicit.
- [ ] Required tests and artifacts are listed.
- [ ] Research note is saved under `docs/research/`.

## Definition of Done

This skill is complete only when the research note is saved, sources are triaged, implementation guardrails are explicit, weak sources are prevented from driving decisions, and the Orchestrator can trace each recommendation to reliable current evidence.

---

## 1. Purpose

This skill defines how ChromaLab agents perform deep, current, source-backed research before making technical, UX, scientific, AI/VLM, or product decisions.

The project explicitly assumes that model memory can be outdated. Any agent using this skill must verify current information from primary or high-quality sources before making design or implementation decisions.

This skill prevents:

- outdated API usage;
- fabricated library capabilities;
- unsafe use of VLM/LLM outputs;
- weak or non-reproducible scientific claims;
- design decisions based only on intuition;
- copy-pasted implementation patterns that do not match current Android/KMP/Compose behavior;
- release gates based on assumptions rather than evidence.

---

## 2. When This Skill Is Mandatory

Use this skill before any work involving:

1. **Android / Kotlin / KMP / Compose**
   - Android APIs;
   - CameraX;
   - ML Kit;
   - Jetpack Compose;
   - file/export behavior;
   - storage permissions;
   - state restoration;
   - performance, memory, thermal behavior;
   - on-device model runtime.

2. **Computer Vision / Chart Digitization**
   - ROI detection;
   - graphPanel / plotArea extraction;
   - line detection;
   - tick detection;
   - homography / perspective correction;
   - skeletonization;
   - raster curve extraction;
   - calibration from image coordinates.

3. **OCR / VLM / On-device AI**
   - local crop OCR;
   - VLM model selection;
   - structured VLM output;
   - on-device inference limits;
   - hallucination controls;
   - model latency or memory claims.

4. **Chromatography / Scientific Reporting**
   - peak definitions;
   - RT / height / area / FWHM / S/N;
   - baseline correction;
   - Kovats / retention index;
   - uncertainty labeling;
   - report provenance;
   - scientific caveats.

5. **UX / UI / Design**
   - guided workflows;
   - image annotation interfaces;
   - zoom/pan/drag interactions;
   - mobile calibration UX;
   - visual design systems;
   - accessibility;
   - localization.

6. **Security / Privacy / Export**
   - user image storage;
   - evidence package exports;
   - logs;
   - report sharing;
   - redaction;
   - Android scoped storage.

7. **QA / Benchmarking**
   - golden image tests;
   - real-device validation;
   - artifact validation;
   - test matrix design.

---

## 3. Non-Negotiable Rules

1. Do not rely on memory for current APIs, libraries, model capabilities, or mobile platform behavior.
2. Prefer primary sources:
   - official documentation;
   - API references;
   - maintained project repositories;
   - peer-reviewed papers;
   - standards;
   - vendor docs.
3. If primary sources are unavailable, use multiple independent secondary sources and clearly mark confidence.
4. Do not cite or use low-quality blogs as authoritative unless no better source exists.
5. Do not use AI-generated summaries as sources.
6. Do not adopt code snippets without checking license and compatibility.
7. Do not make performance claims without device/runtime evidence or credible benchmarks.
8. Do not make scientific claims without chromatography-domain validation.
9. Do not use VLM/LLM capabilities as factual unless verified by current documentation or project-specific tests.
10. Every research output must include:
    - searched question;
    - sources;
    - source quality;
    - findings;
    - decision impact;
    - open risks;
    - recommended next action.

---

## 4. Required Research Output Location

Every research task must create or update a markdown file under:

```text
docs/research/
```

Recommended naming:

```text
docs/research/YYYY-MM-DD_<phase>_<topic>.md
```

Examples:

```text
docs/research/2026-05-18_phase0_android_mlkit_text_recognition.md
docs/research/2026-05-18_phase2_compose_zoom_pan_annotation_ui.md
docs/research/2026-05-18_phase3_chart_digitization_calibration.md
docs/research/2026-05-18_phase6_on_device_vlm_crop_ocr.md
```

---

## 5. Source Quality Tiers

### Tier A — Primary / Preferred

Use whenever available.

- Official platform docs.
- Official API references.
- Official vendor documentation.
- Maintained library documentation.
- Standards documents.
- Peer-reviewed papers.
- Official sample repositories.
- Source code of maintained libraries.

### Tier B — Strong Secondary

Use when Tier A does not fully answer.

- Maintained technical guides.
- Reputable engineering blogs from vendor/platform teams.
- Well-maintained open-source examples.
- Conference talks with linked code.
- Scientific review articles.

### Tier C — Weak / Context Only

Use only for context, not final authority.

- Personal blogs.
- Forum posts.
- StackOverflow answers.
- Reddit threads.
- AI-generated content.
- Unmaintained repositories.

### Tier D — Rejected

Do not rely on these as sources.

- Undated content about APIs that change often.
- Content without author/source provenance.
- Copy-paste code farms.
- Unlicensed code snippets.
- Claims without reproducible evidence.
- Vague AI/model benchmark posts with no methodology.

---

## 6. Minimum Source Requirements

For each research question:

1. Use at least **2 Tier A sources** when possible.
2. Use at least **3 total sources** for non-trivial implementation choices.
3. For medical/scientific or chromatography semantics, include at least:
   - one chromatography/scientific source;
   - one implementation or standards source when relevant.
4. For Android APIs, include official Android documentation.
5. For ML Kit, include official ML Kit documentation.
6. For model/runtime capabilities, include official model/runtime documentation or project-specific benchmark evidence.
7. For UX/design, include current Android/Material/Compose guidance plus at least one practical implementation reference.

If the minimum cannot be met, explicitly state:

```text
Research confidence: LOW
Reason: <why sources were insufficient>
Decision impact: do not implement irreversible architecture based solely on this research
```

---

## 7. Research Workflow

### Step 1 — Define the Research Question

Every research note must start with:

```text
Research question:
What exact decision are we trying to make?
```

Examples:

```text
How should a Compose Multiplatform image annotation editor implement zoom/pan and draggable ROI handles with state restoration?
```

```text
What is the safest architecture for local VLM crop OCR without allowing the model to provide numeric chromatographic measurements?
```

```text
Which calibration residual checks are appropriate for pixel-to-unit transform in chart digitization?
```

### Step 2 — Define Constraints

Include:

- target platform;
- Android version assumptions;
- memory/latency limits;
- offline requirement;
- scientific correctness requirements;
- privacy requirements;
- UI constraints;
- testability requirements.

### Step 3 — Search Current Sources

Search by source type:

1. Official docs.
2. Maintained libraries.
3. Academic/scientific papers.
4. Open-source examples.
5. Benchmarks.
6. Security/privacy guidelines.
7. UX/design references.

### Step 4 — Triage Sources

For each source record:

```text
Source:
URL:
Source tier:
Date or version:
Maintainer:
Why relevant:
Limitations:
Use / do not use:
```

### Step 5 — Extract Findings

Findings must be actionable:

```text
Finding:
Evidence:
Applies to ChromaLab? yes/no/partial
Implementation implication:
Risk:
```

### Step 6 — Compare Options

Use an option matrix:

| Option | Sources | Pros | Cons | Risks | Testability | Decision |
| --- | --- | --- | --- | --- | --- | --- |

### Step 7 — Make a Recommendation

Recommendation must state:

- selected approach;
- rejected alternatives;
- why;
- required tests;
- rollback plan;
- open questions.

### Step 8 — Add Implementation Guardrails

Every research note must end with:

```text
Implementation guardrails:
- ...
```

Examples:

```text
- Do not use VLM crop OCR output as numeric measurement.
- Store localCropPath and rawText for every VLM/OCR result.
- If ML Kit and VLM disagree, mark REVIEW unless local signal evidence confirms.
```

---

## 8. Required Research Note Template

Use this template exactly unless the Orchestrator allows a stronger format.

```markdown
# Research Note: <topic>

Date: <YYYY-MM-DD>
Phase: <phase>
Agent: <agent>
Skill: current-web-research-deep
Research confidence: HIGH / MEDIUM / LOW

## 1. Research Question

<question>

## 2. ChromaLab Context

<why this matters for the project>

## 3. Constraints

- Platform:
- Offline requirement:
- Performance/memory:
- Scientific correctness:
- Privacy/security:
- UX:
- Testability:

## 4. Sources Reviewed

| # | Source | Tier | Date/version | Relevance | Limitations |
| --- | --- | --- | --- | --- | --- |
| 1 | ... | A/B/C/D | ... | ... | ... |

## 5. Findings

### Finding 1

- Evidence:
- Applies to ChromaLab:
- Implementation implication:
- Risk:

### Finding 2

- Evidence:
- Applies to ChromaLab:
- Implementation implication:
- Risk:

## 6. Options Considered

| Option | Pros | Cons | Risks | Testability | Decision |
| --- | --- | --- | --- | --- | --- |
| ... | ... | ... | ... | ... | ... |

## 7. Recommendation

<recommended path>

## 8. Rejected Approaches

<what not to do and why>

## 9. Implementation Guardrails

- ...
- ...

## 10. Required Tests / Validation

- ...
- ...

## 11. Open Questions

- ...
```

---

## 9. Domain-Specific Research Checklists

### 9.1 Android / KMP / Compose Checklist

Research must answer:

- Which APIs are current and stable?
- Are they Android-only or KMP-compatible?
- What are the lifecycle implications?
- How does state survive process death?
- How are files exported under scoped storage?
- How do permissions work on target SDK?
- What official samples exist?
- What testing strategy is possible?

Required source types:

- official Android docs;
- official Compose docs;
- KMP/Compose Multiplatform docs if applicable;
- maintained sample code.

### 9.2 Image Annotation / Guided UI Checklist

Research must answer:

- How to implement zoom/pan without losing coordinate precision?
- How to map screen coordinates to image coordinates?
- How to render draggable handles?
- How to support large images efficiently?
- How to persist ROI edits?
- How to avoid gesture conflicts?
- How to provide accessibility for handles and calibration points?
- How to display review/invalid states clearly?

### 9.3 Geometry / Calibration Checklist

Research must answer:

- What chart digitization methods are used for raster plots?
- How to fit pixel-to-unit transforms?
- When is a linear transform sufficient?
- When is homography required?
- What residual thresholds are reasonable?
- How to detect non-monotonic anchors?
- How to reject outliers?
- How to communicate calibration uncertainty?

### 9.4 OCR / VLM Checklist

Research must answer:

- What OCR engines are current and supported?
- How reliable are small local crops?
- What preprocessing improves OCR?
- What confidence scores are available?
- How to handle text orientation?
- What is the current on-device VLM runtime path?
- What latency/memory should be expected?
- How to structure JSON outputs?
- How to prevent hallucinated numeric data?

### 9.5 Chromatography Checklist

Research must answer:

- What metrics are valid for image-derived chromatograms?
- How should uncertainty be labeled?
- What baseline/peak metrics require raw data rather than image digitization?
- How should recovered/review peaks be marked?
- What scientific caveats are required?
- What Kovats/retention index inputs are mandatory?
- What should never be claimed from insufficient image evidence?

### 9.6 Security / Privacy Checklist

Research must answer:

- Where are original user images stored?
- What is exported in evidence packages?
- Are logs safe to share?
- Do evidence packages contain private images or metadata?
- How should redaction work?
- How are files shared under Android scoped storage?
- What should never be logged?

### 9.7 QA / Regression Checklist

Research must answer:

- How should golden artifact tests be stored?
- How to compare image overlays?
- How to validate runtime evidence packages?
- How to structure real-device validation?
- How to avoid fixture-specific hacks?
- How to define PASS/REVIEW/FAIL per image class?

---

## 10. Decision Confidence Labels

Every recommendation must use one:

### HIGH

Use only when:

- primary sources agree;
- implementation path is clear;
- tests can validate behavior;
- risks are understood.

### MEDIUM

Use when:

- sources are good but incomplete;
- implementation requires project-specific validation;
- rollback is straightforward.

### LOW

Use when:

- sources are weak;
- model/runtime behavior is uncertain;
- requires prototype before architecture commitment;
- should not be used for release-critical decisions.

---

## 11. Research-to-Implementation Handoff

Before any agent implements code based on research, it must provide:

```text
Research note:
Decision:
Affected files:
Required tests:
Risks:
Rollback plan:
Orchestrator approval:
```

No implementation starts until the Orchestrator accepts the handoff.

---

## 12. Anti-Patterns

Reject the task or return to research if any occur:

1. “I know this from memory.”
2. “This library probably supports it.”
3. “VLM should be able to do this.”
4. “No need to test; compile passed.”
5. “This worked on one image, so it is general.”
6. “The fixture was updated to match output.”
7. “The API name looks right.”
8. “We can claim release-quality with REVIEW evidence.”
9. “We can use title numeric values as peak labels.”
10. “Logs are safe because they are internal.”

---

## 13. Required Orchestrator Checks

The Orchestrator must reject phase closeout if:

- required research notes are missing;
- sources are not listed;
- source quality is not triaged;
- recommendation is not tied to tests;
- current API behavior was assumed without source;
- scientific claims were not reviewed by Chromatography SME;
- VLM behavior was not reviewed by VLM Evaluation Agent;
- UI/UX behavior was not reviewed by UX/Design agents;
- privacy/export changes were not reviewed by Security & Privacy Agent.

---

## 14. Skill Output Summary

Every invocation of this skill must end with:

```text
Research complete: yes/no
Research confidence: HIGH/MEDIUM/LOW
Implementation allowed: yes/no
Required follow-up:
Sources saved in:
```

---

## 15. ChromaLab-Specific Guiding Principle

For ChromaLab, research is not academic decoration. It is a release gate.

If a technical method, UX pattern, VLM capability, or scientific claim cannot be verified from current sources and project-specific evidence, it must remain diagnostic or experimental.

Release-quality chromatogram reports require evidence, calibration, trace validation, and provenance.
