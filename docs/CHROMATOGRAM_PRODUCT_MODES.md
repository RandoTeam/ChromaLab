# ChromaLab Product Modes

Phase 4 realigns the product around an autonomous-first architecture. The primary target is no longer a mandatory guided workflow. The app should analyze chromatogram photos and screenshots autonomously as far as evidence allows, then escalate only the failed or low-confidence stages to review tools.

## Modes

| Mode | Purpose | Release-quality allowed? | Current role |
| --- | --- | --- | --- |
| `AUTONOMOUS_PRODUCTION` | Primary target path. The app automatically performs image normalization, graphPanel/plotArea detection, axis/tick/OCR calibration, trace extraction, peak detection, evidence validation, and report generation. | Yes, only when every automatic evidence gate is `VALID`, the evidence package is complete, and the validator has no blocking issues. | Target production architecture; individual gates are being built across phases. |
| `AUTO_DIAGNOSTIC` | Automatic attempt when evidence is incomplete or confidence is low. | No by default. It may show diagnostics, suggestions, and review reasons, but must not look release-ready unless all gates pass under the autonomous contract. | Existing automatic fallback/diagnostic mode. |
| `ASSISTED_REVIEW` | User reviews or corrects only failed/review stages from the autonomous run. | Yes, if corrections are explicit, persisted, evidence-backed, and visible in report provenance. | Phase 2/3/4 editors belong here. |
| `MANUAL_ADVANCED` | Expert fallback for hard images where the user manually defines graphPanel, plotArea, calibration, trace, and later peaks. | Yes, if manual evidence is complete, validated, and disclosed. | Future advanced workflow. |
| `GUIDED_PRODUCTION` | Deprecated compatibility alias for earlier Phase 1-4 guided contracts. | Treated as `ASSISTED_REVIEW` behavior for compatibility. | Do not use for new architecture decisions. |

## AUTONOMOUS_PRODUCTION Rules

`AUTONOMOUS_PRODUCTION`:

- is the primary product target;
- attempts the complete chromatogram pipeline automatically;
- may produce `RELEASE_READY` only with valid graphPanel, plotArea, X/Y calibration, trace, peak evidence, source provenance, evidence package, and validator results;
- must record deterministic evidence for numeric geometry and chromatographic metrics;
- may use VLM/OCR/CV stage judges only as semantic/review/retry evidence;
- must export a runtime evidence package for every terminal state;
- must not require manual axes/calibration for normal images.

`AUTONOMOUS_PRODUCTION` must not:

- hide missing geometry, invalid calibration, sparse trace, missing overlays, or VLM failure;
- use VLM/LLM output as numeric truth;
- treat user-edited/manual evidence as automatic evidence;
- produce release-quality results without a complete audit trail.

## AUTO_DIAGNOSTIC Rules

`AUTO_DIAGNOSTIC`:

- may run automatically from camera, gallery, screenshot, scan, or file input;
- may generate graphPanel, plotArea, axis/tick, OCR, trace, peak, and report suggestions;
- may produce diagnostic or review reports;
- must export evidence for every terminal state;
- must clearly label review/diagnostic output.

`AUTO_DIAGNOSTIC` must not:

- present incomplete evidence as production science;
- use fixture hints or image-specific branches as production evidence;
- use guided/manual confirmations as release evidence;
- rewrite `CalculationEngine` to compensate for upstream failures.

## ASSISTED_REVIEW Rules

`ASSISTED_REVIEW` is the review and repair path for low-confidence autonomous stages.

The user may review or correct:

- graphPanel and plotArea suggestions;
- X and Y calibration anchors;
- extracted trace overlay quality;
- later peak apex/boundary/review decisions.

User intervention must be explicit, persisted, and visible in the report/evidence package as user-reviewed or user-confirmed provenance. Assisted review is not the default happy path; it starts only when the autonomous pipeline needs correction or when the user explicitly asks to review evidence.

Phase 2 ROI editor, Phase 3 calibration editor, and Phase 4 trace overlay screen are Assisted Review tools.

## MANUAL_ADVANCED Rules

`MANUAL_ADVANCED` is an expert fallback for difficult photos, rotated scans, weak traces, missing tick labels, failed automatic geometry, or cases where the user intentionally wants full manual control.

It may allow the user to define:

- graph bounds;
- plot bounds;
- axis/tick anchors;
- calibration values;
- trace points or corrections;
- peak decisions.

Manual values must carry provenance, warnings, and review status. Manual intervention must never be hidden in a release report.

## Report Mode Requirements

Every final report or evidence package must identify:

- processing mode;
- gate status;
- terminal state;
- evidence package status;
- whether evidence is automatic, assisted, or manual;
- user confirmation status if applicable;
- VLM evidence status if applicable.

Legacy `FULL_ANALYSIS` and deprecated `GUIDED_PRODUCTION` labels must not be interpreted as production-ready unless the current release gates pass.

## VLM Boundary

VLM/LLM may assist with:

- local crop OCR;
- title / ion / channel / axis-label reading;
- text classification;
- rough graph hints;
- overlay judging;
- warning explanation.

VLM/LLM must not provide:

- exact numeric geometry used for calculation;
- RT as final measurement;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index;
- final peak count;
- chromatographic quantitative metrics.

## Current Status

Phase 0 defined the original gate contract. Phase 1 created shared guided/manual state contracts. Phase 2, Phase 3, and Phase 4 implemented ROI, calibration, and trace review components that now belong to `ASSISTED_REVIEW` and `MANUAL_ADVANCED`. Phase 5 added autonomous peak evidence. Phase 6 adds multimodal stage judge contracts for OCR/VLM/CV assistance without changing numeric authority.

Future work must prioritize `AUTONOMOUS_PRODUCTION`: automatic evidence first, assisted/manual repair only when a stage fails or falls below confidence.
