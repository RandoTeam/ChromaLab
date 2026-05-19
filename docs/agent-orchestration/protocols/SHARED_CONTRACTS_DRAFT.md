# Shared Contracts Draft

These are conceptual Kotlin contracts. Orchestrator must adapt to existing code style.

## Geometry

- GraphPanelBounds
- PlotAreaBounds
- AxisGeometry
- TickAnchor
- ManualCalibrationAnchor
- AxisCalibrationFit
- CalibrationResidualReport

## User confirmations

- UserConfirmedGraphPanel
- UserConfirmedPlotArea
- UserConfirmedCalibration
- UserConfirmedTrace
- UserPeakEditDecision

## Evidence

- RuntimeEvidencePackage
- GeometryEvidence
- CalibrationEvidence
- TraceEvidence
- PeakEvidence
- VlmOcrEvidence
- ReportGateStatus

## Modes

- AUTO_DIAGNOSTIC
- GUIDED_PRODUCTION
- MANUAL_ADVANCED

## Status enums

- VALID
- REVIEW
- INVALID
- USER_CONFIRMED_VALID
- USER_CONFIRMED_REVIEW
- DIAGNOSTIC_ONLY
