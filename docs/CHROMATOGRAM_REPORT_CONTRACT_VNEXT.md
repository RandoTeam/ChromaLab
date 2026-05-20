# Chromatogram Report Contract vNext

Date: 2026-05-20

## Contract Version

`ChromatogramReportUiContract` is now `chromalab.chromatogram_report_ui.v2`.

## v2 Additions

- `reportGateEvidence`: structured `GateEvidence` matrix.
- `reportGateBlockingReasons`: release-blocking reason codes.
- `reportGateReviewReasons`: review reason codes.
- `ReportExportPrivacyClass`: export artifact audience/sensitivity.
- `redactionPolicy`: per-artifact privacy rule.
- `diagnosticOnly`: marks evidence/debug artifacts that should not be shared by default.

## Compatibility

The core `ChromatogramReport` schema remains unchanged. UI contract v2 is additive for render/export surfaces.

## Required Renderer Parity

| Surface | Gate status | Gate matrix | Peak evidence | Export privacy |
| --- | --- | --- | --- | --- |
| Compose mobile report | Yes | Yes | Yes | Appendix |
| HTML report | Yes | Yes | Yes | Export manifest |
| Markdown report | Yes | Yes | Yes | Technical appendix summary |

## Future vNext Items

- Promote visual evidence string statuses to a typed enum.
- Add first-class Knowledge Pack explanation records to report model.
- Add native PDF renderer when KMP export policy is settled.
