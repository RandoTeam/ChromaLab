# Peak Review Report Provenance

## Purpose

Peak provenance makes clear whether a peak metric came from autonomous deterministic calculation, recovered label evidence, user confirmation, user editing, or rejection.

## Report Contract

`PeakEvidenceAndRecoveryReport` now carries a `peakEvidenceTable` plus counts for:

- review peaks;
- rejected peaks;
- user-confirmed peaks;
- user-edited peaks;
- runtime recovered peaks;
- test-only recovered peaks;
- rejected recovered candidates.

The peak evidence table is exported through the runtime evidence package and summarized by the validator.

## Provenance Classes

- `AUTO_DETECTED`: deterministic `CalculationRun` output.
- `LABEL_RECOVERED`: local OCR/VLM label hint verified against signal evidence.
- `USER_CONFIRMED`: user accepted evidence in Assisted Review.
- `USER_EDITED`: user changed boundaries/status/interpretation in Assisted Review.
- `USER_REJECTED`: user excluded a peak.
- `IMPORTED`: future imported method/library evidence.

## Scientific Caveats

- A peak label is not a peak unless local signal evidence confirms it.
- Missing optional metrics are not filled with guesses.
- Shoulder/overlap peaks are review-grade.
- Kovats and compound interpretation remain separate evidence layers and cannot be inferred from peak shape alone.
- VLM/OCR text is not a numeric peak metric source.
