# Skill: SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY

## Skill Name

Deep Research Method Discovery

## Skill Purpose

Forces a deep, current, implementation-oriented research pass before ChromaLab changes
core chromatogram analysis methods. It is designed for discovering whether failures
come from deterministic geometry, OCR/VLM, trace extraction, peak integration,
runtime constraints, or report/evidence design.

## When To Use

- The user asks to deeply study current methods, GitHub repositories, Reddit/forum
  discussions, scientific software, or papers before implementation.
- A task may alter graphPanel, plotArea, axes, ticks, calibration, trace extraction,
  baseline, peak detection, peak integration, VLM/OCR prompts, model runtime, or
  report gates.
- A phase claims production-grade autonomous analysis, high success rates, or
  scientific accuracy.
- Existing ChromaLab failures are unclear and may be caused by deterministic
  algorithms rather than LLM/model availability.

## When Not To Use

Do not use for trivial documentation edits, typo fixes, release packaging, UI-only
cosmetic work, or a bug whose root cause is already reproduced and isolated.

## Required Context

- Current failing fixtures, artifacts, and regression matrix.
- Active product boundary and forbidden changes.
- Current ChromaLab pipeline stage being studied.
- Existing implementation files and tests for that stage.
- Prior research notes under `docs/research/`.

## Procedure

1. Freeze implementation until the research brief exists.
2. Define one stage only: acquisition, normalization, graph layout, plotArea, axes,
   ticks, calibration, OCR/VLM, trace, baseline, peak, report, export, or runtime.
3. Search at least four source classes when relevant:
   - maintained GitHub repositories;
   - peer-reviewed or preprint literature;
   - official docs for libraries/runtimes;
   - active technical communities such as Reddit, Stack Overflow, issues, or
     project discussions.
4. Prefer sources from the last 18 months, but include older canonical algorithms
   when they are still used in production tools.
5. Build a source matrix with source quality, freshness, implementation maturity,
   licensing, device/runtime fit, evidence requirements, and adoption decision.
6. Compare discovered methods against ChromaLab's actual failures and fixtures.
7. Separate:
   - adopt now;
   - prototype only;
   - reject;
   - monitor;
   - needs dataset or ground truth first.
8. Produce a phase-sized implementation plan with acceptance criteria, tests,
   fixture coverage, and rollback criteria.

## Required Source Matrix Columns

| Column | Meaning |
| --- | --- |
| Source | Link and title |
| Type | OFFICIAL_DOCS / MAINTAINED_OPEN_SOURCE / PEER_REVIEWED / PREPRINT / FORUM_OR_DISCUSSION / COMMERCIAL_TOOL / UNVERIFIED |
| Date / freshness | Publication or last active date |
| Method | Algorithm, library, workflow, or product pattern |
| Relevance | ChromaLab stage and failure class |
| Evidence needed | Fixtures, overlays, labels, timing, metrics |
| Adoption decision | ADOPT / PROTOTYPE / REJECT / MONITOR |
| Risk | Accuracy, licensing, runtime, privacy, overclaim, maintenance |

## ChromaLab Constraints

- Do not modify `CalculationEngine` unless a calibrated-signal test proves an
  isolated math bug.
- Do not use VLM/LLM as numeric authority for geometry, calibration coefficients,
  RT, height, area, FWHM, S/N, baseline, Kovats, or final peak metrics.
- Do not hardcode fixture coordinates, filenames, image dimensions, or expected
  peak counts.
- Do not weaken validators or report gates.
- Do not claim 99% success or 100% accuracy without locked ground truth, metric
  definitions, and regression evidence.

## Recommended Research Tracks

For graph/axis/calibration:

- chart element detection and object detection;
- plot frame / grid / tick line detection;
- OCR label role classification;
- robust pixel-to-value regression;
- perspective/rotation correction;
- uncertainty scoring and residual analysis.

For trace/peak:

- curve skeletonization and mask extraction;
- baseline correction such as SNIP, ALS, rolling-ball, polynomial, or wavelet;
- peak picking under overlap and drift;
- parametric peak fitting only after calibrated signal exists;
- chromatographic metric provenance.

For VLM/OCR:

- local crop OCR benchmark;
- structured JSON contracts;
- self-consistency / multi-pass confidence;
- hallucination rejection;
- advisory-only model gating.

## Outputs

- Research note:
  `docs/research/YYYY-MM-DD_<stage>_deep_method_discovery.md`
- Optional method comparison:
  `docs/research/YYYY-MM-DD_<stage>_method_matrix.md`
- Implementation brief:
  `docs/<PHASE>_<stage>_RESEARCH_HANDOFF.md`
- Updated orchestrator or phase plan when the research changes work order.

## Validation Criteria

- At least one maintained implementation or production tool is inspected when code
  adoption is proposed.
- At least one scientific or technical source supports method-level claims when
  scientific accuracy is affected.
- Forum/Reddit claims are treated as signals, not authority.
- Every adopted method maps to a ChromaLab fixture, stage, artifact, and test.
- Every rejected method includes a reason.

## Anti-Patterns

- Link dumping without adoption/rejection decisions.
- Claiming a method is "best" without fixture evidence.
- Letting commercial marketing claims drive implementation.
- Treating VLM extraction benchmarks as permission to bypass deterministic
  calibration.
- Starting broad rewrites before one stage has a locked test plan.

## Required Handoff

```markdown
## Skill Handoff: SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY
- Stage studied:
- Sources searched:
- Strongest methods found:
- Adopt now:
- Prototype only:
- Reject:
- Fixtures affected:
- Required tests:
- Risks:
- Next implementation phase:
```

## Related Agents

- AGENT_00_ORCHESTRATOR
- research_intelligence_agent
- qa_regression_agent
- product_acceptance_agent
- chromatography_sme_agent
- scientific_reporting_validation_agent
- vlm_evaluation_agent
- android_performance_ondevice_ai_agent

## Related Skills

- current-web-research-deep
- source-quality-triage
- research-synthesis
- method-comparison-matrix
- geometry-calibration-robust-fit
- ocr-local-crops
- trace-extraction-masks
- peak-review-integration
- evidence-package-validator
- regression-benchmark-golden

## Definition of Done

- Research outputs exist and are linked from the phase plan.
- Source matrix is complete enough for Product/QA/Scientific review.
- No implementation starts before adoption/rejection decisions are recorded.
- The next phase is one stage only and has concrete validation commands.
