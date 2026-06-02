# Base Skill Index

## Purpose

This index maps base ChromaLab orchestration skills to their authoritative files,
expected use, related agents, and validation role. Expanded skills live under
`../expansion/skills/` and are indexed in
`../expansion/SKILLS_EXPANSION.md`.

## Product Direction

- `AUTO_DIAGNOSTIC`: automatic attempt; diagnostic by default; evidence is always exported.
- `GUIDED_PRODUCTION`: user-confirmed graphPanel, plotArea, calibration, trace, and peaks before release-quality output.
- `MANUAL_ADVANCED`: fallback for difficult images where the user defines geometry, calibration, trace, or peak decisions.

## Mandatory Cross-Cutting Rules

- Use current web research before relying on modern APIs, models, libraries, UX patterns, security rules, or scientific methods.
- Use source-quality triage before research affects implementation, report language, or phase acceptance.
- Do not rewrite CalculationEngine unless an isolated reproducible bug is proven after upstream validation.
- VLM/LLM may assist with OCR, semantics, overlay judging, and warning explanations, but must not provide numeric geometry or chromatographic metrics.
- Every runtime terminal state must produce evidence or a blocking failure explaining why evidence could not be exported.

## Skill Inventory

| Skill | Authoritative File | Use When | Primary Output |
| --- | --- | --- | --- |
| `SKILL_00_WEB_RESEARCH_PROTOCOL` | `SKILL_00_WEB_RESEARCH_PROTOCOL.md` | Before any phase or technical implementation | Research note and source-quality plan |
| `SKILL_01_KMP_COMPOSE_ARCHITECTURE` | `SKILL_01_KMP_COMPOSE_ARCHITECTURE.md` | UI/shared architecture changes | Architecture plan and validation checklist |
| `SKILL_02_STATE_MACHINE_PERSISTENCE` | `SKILL_02_STATE_MACHINE_PERSISTENCE.md` | Guided workflow state, resume, save/restore | State contract and persistence gates |
| `SKILL_03_IMAGE_VIEWER_ROI_EDITOR` | `SKILL_03_IMAGE_VIEWER_ROI_EDITOR.md` | Zoom/pan/drag ROI UI | ROI editor acceptance and artifact plan |
| `SKILL_04_GEOMETRY_CALIBRATION_ROBUST_FIT` | `SKILL_04_GEOMETRY_CALIBRATION_ROBUST_FIT.md` | Axes/ticks/manual anchors/calibration | Calibration residual and review gates |
| `SKILL_05_OCR_LOCAL_CROPS` | `SKILL_05_OCR_LOCAL_CROPS.md` | ML Kit/local OCR around labels/ticks | OCR crop evidence and classification |
| `SKILL_06_VLM_SAFE_ASSISTANT` | `SKILL_06_VLM_SAFE_ASSISTANT.md` | Gemma/Qwen local crop OCR and overlay judge | Bounded VLM contract and warnings |
| `SKILL_07_TRACE_EXTRACTION_MASKS` | `SKILL_07_TRACE_EXTRACTION_MASKS.md` | Curve extraction from plotArea | Trace mask and centerline evidence |
| `SKILL_08_PEAK_REVIEW_INTEGRATION` | `SKILL_08_PEAK_REVIEW_INTEGRATION.md` | Peak overlay, edit, merge, split | Peak review and integration provenance |
| `SKILL_09_EVIDENCE_PACKAGE_VALIDATOR` | `SKILL_09_EVIDENCE_PACKAGE_VALIDATOR.md` | Runtime evidence and gates | Validator diagnostics and gate result |
| `SKILL_10_ANDROID_RUNTIME_EXPORT_LOGGING` | `SKILL_10_ANDROID_RUNTIME_EXPORT_LOGGING.md` | Device evidence export, logcat, Downloads | Exportable evidence package plan |
| `SKILL_11_REGRESSION_BENCHMARK_GOLDEN` | `SKILL_11_REGRESSION_BENCHMARK_GOLDEN.md` | Dataset and regression matrix | Fixture matrix and golden artifact rules |
| `SKILL_12_REPORT_GATE_PROVENANCE` | `SKILL_12_REPORT_GATE_PROVENANCE.md` | Release vs diagnostic reports | Report contract gates and provenance labels |
| `SKILL_13_PERFORMANCE_TIMEOUTS` | `SKILL_13_PERFORMANCE_TIMEOUTS.md` | VLM/CV timing, blocking, caching | Timeout, cache, and heartbeat policy |
| `SKILL_14_SECURITY_PRIVACY_STORAGE` | `SKILL_14_SECURITY_PRIVACY_STORAGE.md` | File export and user data handling | Safe storage/export policy |
| `SKILL_15_GIT_BRANCH_REVIEW_PROTOCOL` | `SKILL_15_GIT_BRANCH_REVIEW_PROTOCOL.md` | Multi-agent branches, merges, reviews | Merge plan and review checklist |
| `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY` | `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY.md` | Deep GitHub/literature/forum method discovery before core analysis changes | Source matrix, adoption/rejection decisions, and implementation handoff |

## Related Expanded Skills

- `current-web-research-deep`
- `source-quality-triage`
- `evidence-gated-reporting`
- `real-device-validation`
- `test-plan-authoring`
- `scientific-report-provenance`
- `secure-export-review`
- `mobile-accessibility-review`
- `definition-of-done`

## Validation

- Every skill file must include purpose, when to use, when not to use, context, procedure, inputs, outputs, artifacts, ChromaLab constraints, validation criteria, anti-patterns, example usage, failure modes, handoff, related agents, related skills, and definition of done.
- Registry entries in `../config/skills.json` and `../config/skills_registry.json` must reference real files.
- Skill changes require `git diff --check` and registry parse validation.

## Definition of Done

The skill index is complete when every listed file exists, every skill is actionable
without additional chat context, and all mandatory cross-cutting rules are reflected
in phase prompts and closeout templates.
