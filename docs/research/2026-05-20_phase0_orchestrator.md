# Phase 0 Research - Orchestrator

Scope: evidence-based product reset, audit trail, report gating, and mobile validation.

## Sources Checked

- FDA, General Principles of Software Validation:
  https://www.fda.gov/files/medical%20devices/published/General-Principles-of-Software-Validation---Final-Guidance-for-Industry-and-FDA-Staff.pdf
  - Relevant because Phase 0 treats chromatogram analysis as validated software, not a demo pipeline.
  - Decision affected: every release-quality claim needs documented inputs, expected outputs, validation, and limitations.
  - Do not adopt: medical-device process overhead or regulatory language in the user UI.

- FDA computerized systems audit trail guidance:
  https://www.fda.gov/inspections-compliance-enforcement-and-criminal-investigations/fda-bioresearch-monitoring-information/guidance-industry-computerized-systems-used-clinical-trials
  - Relevant because reports must be reconstructable from runtime evidence.
  - Decision affected: RuntimeEvidencePackage must exist for terminal states, not only successful runs.
  - Do not adopt: clinical-trial-specific compliance workflow.

- FDA data integrity ALCOA guidance:
  https://www.fda.gov/media/119267/download
  - Relevant because scientific claims must be attributable, original, accurate, and available for review.
  - Decision affected: evidence package and validator output become release gates.
  - Do not adopt: paper-signature or regulated-lab recordkeeping requirements in Phase 0.

- Android ANR/performance vitals:
  https://developer.android.com/topic/performance/vitals/anr
  - Relevant because previous ROI/VLM stages could block for minutes without useful evidence.
  - Decision affected: long stages need timing, timeout, and terminal-state evidence.
  - Do not adopt: Play Console monitoring as the primary development validator.

## Phase 0 Decisions

- Full-auto is renamed and treated as `AUTO_DIAGNOSTIC`.
- `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` are target modes, not UI work in this phase.
- A report is not release-quality just because it has a peak table.
- A validator `FAIL` with evidence is better than a silent polished report.

## Explicit Non-Adoptions

- No CalculationEngine rewrite.
- No production release claim from VLM-only geometry.
- No fixture-specific image branches.
