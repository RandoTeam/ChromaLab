# Phase 7 Report Validator Rules

Date: 2026-05-20

## Rules Added or Confirmed

1. Report gate status is derived from graphPanel, plotArea, calibration, trace, peak evidence, evidence package, source provenance, and validator findings.
2. Missing evidence package keeps reports out of `RELEASE_READY` by default.
3. Peak tables must have peak evidence status and peak gate status where available.
4. VLM, OCR, model-suggested, local-knowledge, and unknown numeric chromatographic value sources remain validator errors.
5. Calculated Kovats/RI requires explicit reference retention times.
6. Knowledge/model-only compound names without identity evidence are warnings and must be rendered as hypotheses.
7. VLM-with-Knowledge report explanations require `usedEntryIds`.
8. Knowledge Pack citation records cannot create numeric chromatographic metrics.
9. Unsupported Knowledge/VLM claims are validator-visible review findings.
10. Empty graph reports are `BLOCKED`, not silently diagnostic.

## Release-Ready Overclaim Protection

`ChromatogramReportUiContractBuilder.build()` accepts an explicit evidence package status. If a caller does not pass valid evidence, the generated UI contract keeps release-ready claims blocked.

## Test Coverage

- `ProfessionalReportGateRenderingTest`
- `Phase7BReportEvidenceHardeningTest`
- `ReportScientificClaimValidatorTest`
- `Phase0ProductContractsTest`
- `RuntimeEvidencePackageValidatorTest`
