# Phase 6 Deep Unblock Plan

Date: 2026-05-20

Verdict entering this work: `PHASE_6_BLOCKED`.

## Task Classification

- Product architecture
- VLM / OCR / local model inference
- Runtime evidence / validator
- Knowledge Pack rules, retrieval, and provenance
- Scientific reporting and chromatographic safety
- Android performance / timeout policy
- Security / privacy / offline packaging
- QA / regression / product acceptance

## Agent Ownership

| Blocker | Owner agent | Supporting agents | Required skills |
|---|---|---|---|
| Runtime evidence package fields empty in normal packages | Orchestrator, QA / Regression | VLM Evaluation, Scientific Reporting | evidence-package-validator, regression-benchmark-golden |
| Android local crop VLM not profiled/boundary-checked | VLM Evaluation, Android Performance | OCR / Text Semantics, Security / Privacy | structured-vlm-json-contract, timeout-cache-design |
| VLM prompt asked for parsed RT | OCR / VLM / Text Semantics | Chromatography SME | vlm-safe-assistant, peak-metric-semantics |
| Legacy/base Knowledge Pack source policy incomplete | Chromatography SME, Scientific Reporting | Security / Privacy, Product Acceptance | chromatography-domain-review, scientific-caveat-writing |
| Closeout docs did not show owner/test evidence | Documentation / Governance | Orchestrator, QA / Regression | definition-of-done, audit-trail-design |

## Fix Plan

1. Wire report-derived runtime evidence builder outputs:
   - stage judge rows for graph panel, plot area, axis/tick visibility, OCR crops, trace overlay, and peak evidence;
   - OCR/VLM crop rows from runtime peak-label evidence;
   - overlay judge rows from selected trace overlay;
   - package-level model runtime profiles from VLM-backed evidence.

2. Instrument Android local crop VLM:
   - use structured task timeout from `VlmStructuredTaskContracts`;
   - apply `ForbiddenVlmBoundaryPolicy` before accepting any JSON field;
   - carry rejected forbidden fields and runtime profile into `VisionLocalTextCropResult` and `PeakLabelEvidence`.

3. Remove VLM numeric prompt pressure:
   - local crop prompt asks only for visible text, class, and confidence;
   - `parsed_retention_time` remains a forbidden legacy field and is recorded/rejected if returned.

4. Align base Knowledge Pack with Phase 6C source policy:
   - add license status, trust tier, bundle flag, and claim scopes;
   - keep restricted NIST/WebBook sources link-only and non-bundleable;
   - remove bundled NIST-derived RI seed data from the base pack.

5. Add tests:
   - builder populates multimodal rows and runtime profiles;
   - validator blocks VLM evidence with missing crop/stage/profile rows;
   - parsed RT JSON field is rejected;
   - base pack restricted sources are link-only and not bundled as seed RI data.

## Expected Closeout Evidence

- Updated code contracts and validator tests.
- `docs/PHASE6_BLOCKER_MATRIX.md` with all closeout-required blockers resolved or explicitly deferred.
- Updated `docs/PHASE6_CLOSEOUT_REPORT.md` with agent signoffs and validation results.
- Required Gradle validation commands run and recorded.

## Scope Boundaries

- Phase 7 is not started.
- CalculationEngine and chromatographic math remain untouched.
- No cloud dependency or external database dump is added.
- VLM and Knowledge Pack remain semantic assistants, not measurement authorities.
