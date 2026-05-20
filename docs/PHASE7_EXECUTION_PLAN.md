# Phase 7 Execution Plan

Status: COMPLETED
Date: 2026-05-20

## Task Classification

Phase 7 touched product report architecture, autonomous multi-graph reporting, scientific provenance, mobile report UX, visual report presentation, export rendering, runtime evidence, Knowledge/VLM boundaries, security/privacy, accessibility/localization readiness, and QA/golden report coverage.

## Agents Activated

| Agent | Ownership |
| --- | --- |
| Orchestrator | Scope, blocker matrix, closeout, no CalculationEngine/math changes |
| Research Intelligence | Current report, UX, provenance, export/privacy research |
| Product Acceptance | User journey and report acceptance |
| Scientific Reporting & Validation | Report contract, gates, validator behavior |
| Chromatography SME | Peak metric semantics, Kovats/RI caveats, compound assignment boundaries |
| Visual Design System | Professional report visual language and export parity |
| Mobile UX Architect | Phone-readable report flow and evidence hierarchy |
| Compose/KMP UI Implementation | Structured report preview updates |
| Geometry / Calibration Core | Gate evidence alignment for graphPanel, plotArea, calibration |
| Trace Extraction / Peak Review | Trace/peak evidence table and status presentation |
| VLM Evaluation | VLM/Knowledge explanation boundaries |
| OCR / VLM / Text Semantics | Text classification and no numeric model output rules |
| Security & Privacy | Export classification and redaction policy |
| Accessibility & Localization | Non-color-only status labels, semantic report headings |
| QA / Regression | Report renderer, validator, and regression commands |

Android Performance was not used as a code owner because Phase 7 did not change model runtime, inference, image rendering performance, or memory policy.

## Scope Boundaries

- CalculationEngine untouched.
- Chromatographic math untouched.
- Peak detection math untouched.
- VLM/OCR runtime behavior untouched.
- Manual/assisted review remains fallback.
- Report UI/export surfaces must not hide missing evidence.

## Workstreams

1. Add evidence-gated report UI contract v2.
2. Render release gate evidence in mobile, HTML, and Markdown.
3. Add export privacy classes and redaction policies.
4. Add peak evidence/gate columns to report tables.
5. Add validator checks for Kovats reference evidence and knowledge-only compound hypotheses.
6. Document report architecture, visual design, scientific provenance, export/privacy, and QA matrix.
7. Validate report tests and previous phase regressions.

## Closeout Conditions

- Required Phase 7 docs and research notes exist.
- Report contract supports gate evidence, export privacy, and diagnostic artifact separation.
- User-facing renderers show gate status and never silently claim release quality.
- Validator catches unsupported scientific claims added in Phase 7.
- Required validation commands pass or are documented with blocker status.
