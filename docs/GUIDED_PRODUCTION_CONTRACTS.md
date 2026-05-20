# Guided Production Contracts

Phase 1 defines the data contracts that future guided and manual workflows will write. These contracts are designed to preserve scientific provenance and prevent `AUTO_DIAGNOSTIC` output from masquerading as confirmed production analysis.

## Contract Groups

| Group | Main types | Purpose |
| --- | --- | --- |
| Mode/state | `GuidedDigitizationState`, `GuidedDigitizationMode`, `GuidedWorkflowStep`, `GuidedStepStatus` | Persist workflow state and mode. |
| User provenance | `GuidedUserProvenance`, `GuidedWorkflowAuditEntry` | Record who/what/session changed evidence without storing raw personal identifiers. |
| Geometry | `UserConfirmedGraphPanel`, `UserConfirmedPlotArea`, `GraphPanelConfirmation`, `PlotAreaConfirmation` | Capture graphPanel and plotArea confirmation. |
| Calibration | `ManualCalibrationAnchor`, `UserCalibrationSet`, `CalibrationResidualReport`, `UserConfirmedCalibration` | Capture X/Y anchors, residual evidence, monotonicity, and gate status. |
| Trace | `UserConfirmedTrace`, `TraceConfirmationEvidence`, `TraceQualitySummary` | Capture trace confirmation and quality evidence. |
| Peaks | `UserPeakEditDecision`, `UserConfirmedPeakSet` | Capture future peak review/edit decisions. |
| Gate mapping | `GuidedReportGateMapper`, `GuidedWorkflowGateEvaluation` | Convert guided state into Phase 0 report gates. |

## Geometry Rules

- `graphPanel` includes title, ion/channel, axis labels, tick labels, and plot frame.
- `plotArea` includes only the coordinate rectangle with trace/grid/frame.
- A plotArea confirmation must be linked to the parent graphPanel bounds.
- A confirmation without user provenance or image linkage is not production evidence.

## Calibration Rules

- X and Y calibration each require at least two accepted anchors.
- Three or more accepted anchors per axis are required for robust-fit readiness.
- Accepted and rejected anchors are preserved.
- Residual evidence, RMSE, R2, and monotonicity are stored when available.
- Two-anchor calibration maps to review-grade by default in Phase 1 gate mapping.

## Trace Rules

- Trace confirmation can accept, reject, or mark auto trace evidence for review.
- Overlay and centerline artifact paths are part of the confirmation evidence.
- Sparse/fragmented trace evidence must not silently become release-ready without user confirmation and gate mapping.

## Peak Review Rules

Future peak review supports:

- adding peaks;
- removing peaks;
- merging/splitting peaks;
- adjusting boundaries;
- marking shoulders;
- marking review-grade peaks;
- accepting auto peaks.

Phase 1 stores decisions only. It does not alter peak detection or integration.

## Report Provenance Rules

Manual and guided decisions must be visible in the report provenance:

- mode;
- user confirmation status;
- artifact paths;
- validation warnings;
- audit entries;
- gate mapping decision.

The report must distinguish:

- deterministic auto evidence;
- user-confirmed evidence;
- imported/manual evidence;
- VLM/OCR semantic evidence.

## VLM Boundary

VLM may help read or classify local text crops and judge overlays. It must not populate:

- pixel geometry used for calculation;
- peak RT as final measurement;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index.

## Phase 2 Handoff

The next phase may create graphPanel/plotArea confirmation UI using these contracts. It must use the state machine, persist audit entries, and keep evidence package exports intact.
