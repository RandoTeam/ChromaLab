# Chromatogram Report Contract vNext

Date: 2026-05-20

## Contract Version

`ChromatogramReportUiContract` is now `chromalab.chromatogram_report_ui.v2`.

## v2 Additions

- `reportGateEvidence`: structured `GateEvidence` matrix.
- `reportGateBlockingReasons`: release-blocking reason codes.
- `reportGateReviewReasons`: review reason codes.
- `knowledgeCitations`: first-class Knowledge Pack citation records for scientific/domain explanations.
- `ReportVisualEvidenceStatus`: typed report visual evidence status.
- `ReportExportPrivacyClass`: export artifact audience/sensitivity.
- `redactionPolicy`: per-artifact privacy rule.
- `diagnosticOnly`: marks evidence/debug artifacts that should not be shared by default.

## Compatibility

The core `ChromatogramReport` schema is additive. Existing reports can omit `knowledgeCitations`; renderers display "No Knowledge Pack citations recorded." Visual evidence status values are serialized as enum names.

## Required Renderer Parity

| Surface | Gate status | Gate matrix | Peak evidence | Knowledge citations | Export privacy |
| --- | --- | --- | --- | --- |
| Compose mobile report | Yes | Yes | Yes | Appendix summary | Appendix |
| HTML report | Yes | Yes | Yes | Full appendix records | Export manifest |
| Markdown report | Yes | Yes | Yes | Full appendix records | Technical appendix summary |

## Future vNext Items

- Add native PDF renderer when KMP export policy is settled.
- Add richer interactive multi-graph selector during Phase 8 if report-regression scope allows.
