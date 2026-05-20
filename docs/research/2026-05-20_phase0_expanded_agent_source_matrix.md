# Phase 0 Research - Expanded Agent Source Matrix

Date: 2026-05-20
Phase: Phase 0 - Evidence-Based Product Reset / AUTO_DIAGNOSTIC Freeze
Agents covered: Orchestrator, Research Intelligence, QA / Regression, Product Acceptance, Scientific Reporting & Validation, Chromatography SME, VLM Evaluation, Security & Privacy, Android Performance & On-Device AI, Mobile UX Architect
Skills: `current-web-research-deep`, `source-quality-triage`, `research-synthesis`, `evidence-gated-reporting`, `scientific-report-provenance`, `runtime-validation`, `vlm-boundary-enforcement`, `regression-matrix`, `product-risk-review`, `definition-of-done`
Research confidence: MEDIUM-HIGH

## Research Question

Which current sources should govern Phase 0 product-mode, evidence, release-gate, VLM-boundary, runtime-export, UX, accessibility, and scientific-reporting decisions?

## ChromaLab Context

Phase 0 is not an algorithm-improvement phase. It freezes the existing full-auto photo/screenshot flow as `AUTO_DIAGNOSTIC`, defines `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` as future reliable paths, and ensures release-quality claims require graphPanel, plotArea, X/Y calibration, trace, evidence package, and validator gates.

## Source Quality Matrix

| Source | Type | Authority | Freshness | Specificity | Reproducibility | Use in Phase 0 | Decision |
| --- | --- | ---: | ---: | ---: | ---: | --- | --- |
| [FDA General Principles of Software Validation](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/general-principles-software-validation) | Official guidance | 5 | 3 | 4 | 4 | Evidence before claims; verification and validation language | Accepted for audit-trail mindset, not regulatory UI text |
| [FDA Computerized Systems Used in Clinical Trials](https://www.fda.gov/inspections-compliance-enforcement-and-criminal-investigations/fda-bioresearch-monitoring-information/guidance-industry-computerized-systems-used-clinical-trials) | Official guidance | 5 | 3 | 3 | 3 | Audit trail and reconstructability concepts | Accepted for evidence-package principle only |
| [Android Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) | Official docs | 5 | 4 | 5 | 5 | Exportable evidence and report artifacts | Accepted |
| [Android shared documents/files](https://developer.android.com/training/data-storage/shared/documents-files) | Official docs | 5 | 5 | 5 | 5 | Future share/save/export behavior and real-device package requirements | Accepted |
| [Android ANR vitals](https://developer.android.com/topic/performance/vitals/anr) | Official docs | 5 | 4 | 5 | 5 | Long-running VLM/ROI stages require timings, timeout, and evidence | Accepted |
| [ML Kit Text Recognition v2 for Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) | Official docs | 5 | 4 | 5 | 5 | OCR crop provenance; local text extraction | Accepted |
| [Google AI Edge LiteRT delegates](https://ai.google.dev/edge/litert/performance/delegates) | Official docs | 5 | 4 | 4 | 4 | On-device acceleration is runtime evidence, not scientific validity | Accepted |
| [Google AI Edge LiteRT inference](https://ai.google.dev/edge/litert/inference) | Official docs | 5 | 4 | 4 | 4 | Runtime/model metadata and performance expectations | Accepted |
| [Jetpack Compose state saving](https://developer.android.com/develop/ui/compose/state-saving) | Official docs | 5 | 5 | 5 | 5 | Future guided workflow state persistence | Accepted for future Phase 1+ constraints |
| [Android touch target guidance](https://support.google.com/accessibility/android/answer/7101858) | Official guidance | 5 | 4 | 4 | 5 | Future guided controls and report actions must be usable on mobile | Accepted |
| [WCAG 2.2](https://www.w3.org/TR/WCAG22/) | Standard | 5 | 4 | 4 | 4 | User-facing report/status contrast, text, and target-size review | Accepted for accessibility baseline |
| [WebPlotDigitizer repository](https://github.com/automeris-io/WebPlotDigitizer) | Maintained open-source | 4 | 4 | 4 | 4 | Plot digitization usually requires calibration, axes, and user-verifiable extraction | Accepted as method reference |
| [PlotDigitizer docs](https://plotdigitizer.com/docs) | Product docs | 3 | 3 | 4 | 3 | Manual calibration/extraction UX references | Background; not authority for ChromaLab correctness |
| [SciPy `find_peaks`](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html) | Official library docs | 5 | 4 | 4 | 5 | Peak properties such as prominence/width require a validated 1D signal | Accepted for semantic guardrails, not implementation change |
| [IUPAC retention index](https://goldbook.iupac.org/terms/view/R05360) | Scientific standard | 5 | 4 | 5 | 5 | Kovats/retention index requires standards and valid retention data | Accepted |
| [NIST gas chromatographic retention data](https://webbook.nist.gov/chemistry/gc-ri/) | Government scientific database | 5 | 4 | 5 | 4 | Retention index references require clear source/provenance | Accepted |

## Agent Decisions

### Orchestrator

- Phase 0 can define gates and docs, but must not start Phase 1.
- Release-quality language must be blocked unless evidence gates pass.
- Research and source-quality triage remain mandatory because model knowledge may be outdated.

### Research Intelligence

- Official docs, maintained repositories, peer-reviewed sources, standards, and current API docs take priority.
- Weak blogs, uncited claims, forum anecdotes, and marketing claims are background-only.
- Any future implementation must save phase-specific research under `docs/research/`.

### QA / Regression and Product Acceptance

- Validator `FAIL` with evidence is acceptable; silent failure is not.
- Regression matrix must preserve previous real failures as rows.
- Phase 0 closes as product-reset complete, not production-analysis complete.

### Scientific Reporting and Chromatography SME

- A peak table is release-quality only when source geometry, calibration, trace, and evidence are valid or user-confirmed.
- Image-derived values must remain diagnostic/review-grade if upstream evidence is incomplete.
- Kovats or retention-index output needs valid retention data and reference series provenance.

### VLM Evaluation

- VLM/LLM may assist with local crop OCR, title/ion/channel/axis label reading, text classification, overlay judging, and warning explanation.
- VLM/LLM must not populate RT, height, area, FWHM, S/N, baseline, Kovats, exact pixel geometry, final peak count, or quantitative chromatographic metrics.
- VLM timeout or disagreement must become evidence/warning, not a hidden numeric source.

### Security & Privacy

- Evidence packages can contain user images, file paths, model metadata, and logs; future phases need export review and redaction rules.
- Storage/export should follow Android shared document and SAF guidance rather than broad storage permissions.

### Android Performance & On-Device AI

- Runtime performance, model selection, delegates, timeouts, and thermal behavior affect reliability but cannot weaken scientific gates.
- Long VLM/ROI stages need timings and terminal-state evidence.

### Mobile UX Architect

- `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` are future workflows, not Phase 0 UI.
- Future guided state must persist user confirmations and preserve image-coordinate precision.

## Rejected Approaches

- Treating `AUTO_DIAGNOSTIC` as production by default.
- Trusting VLM-derived geometry or chromatographic metrics.
- Hiding missing graphPanel, plotArea, calibration, trace, or evidence behind polished report UI.
- Using weak blogs or uncited claims to justify implementation.
- Rewriting `CalculationEngine` without an isolated, reproducible bug after upstream evidence is validated.

## Implementation Guardrails

- Phase 0 may update docs/contracts/gate metadata only.
- No Guided UI, manual calibration UI, trace editing, peak editing, peak math changes, broad geometry rewrite, or image-specific fixes.
- Every terminal runtime state must have an evidence-export requirement.
- `RELEASE_READY` requires valid or user-confirmed graphPanel, plotArea, X calibration, Y calibration, trace, evidence package, source provenance, and no blocking validator issues.

## Required Tests / Validation

- `git diff --check`
- `.\gradlew.bat :composeApp:compileKotlinDesktop`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :composeApp:desktopTest --rerun-tasks` or targeted tests with explicit risk if the full suite is too slow.

## Open Questions

- Future phases must prove real Android evidence export across all terminal states.
- Future phases must decide whether axis/tick status should become direct `RELEASE_READY` blockers in addition to X/Y calibration status.
- Future UX phases must design clear report language for review-grade numeric output.
