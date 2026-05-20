# Phase 8C Deterministic CV Fallback

## Rule

VLM availability is not a precondition for deterministic geometry. If the VLM is missing, ChromaLab records model diagnostics and continues:

1. image normalization;
2. screenshot/embedded-chart graphPanel candidates;
3. full-image graphPanel search;
4. graph multiplicity resolution;
5. plotArea detection;
6. axis/tick localization;
7. OCR crop attempts where available;
8. calibration, trace, peak, report gates.

## Code Changes

- `ProcessingFlowScreen` no longer throws at `IMAGE_QUALITY` when `ensureModelLoaded()` returns false.
- Android `ChartAnalysisReader.readAxisLabels()` now falls back to ML Kit OCR when the VLM is missing or low-confidence instead of throwing before calibration can classify the deterministic failure.
- Runtime evidence packages carry `modelAvailabilityDiagnostics`.
- Terminal validation failure export writes `model_availability_diagnostics_<run_id>.json` and includes the diagnostics in the log summary.
- `RuntimeEvidencePackageValidator` fails the specific bad case `package.deterministic_fallback_not_attempted` when model unavailability prevents deterministic geometry from starting.
- `VisionAnalysisGuard` no longer treats model-availability messages as non-skippable geometry blockers.
- `ModelManager.canLoadForChromatogramVision()` now applies the common chromatogram VLM eligibility allow-list to imported and built-in models.

## Expected Failure Mapping

| Situation | Primary failure class |
| --- | --- |
| Model unavailable before geometry attempted | `VLM_MODEL_UNAVAILABLE` plus validator `package.deterministic_fallback_not_attempted` |
| Deterministic graphPanel fallback attempted and no panel found | `GRAPH_PANEL_FAILURE` or `CV_FALLBACK_GRAPH_PANEL_FAILURE` |
| GraphPanel found but axes/ticks fail | `AXIS_DETECTION_FAILURE`, `TICK_LOCALIZATION_FAILURE`, or `OCR_TICK_FAILURE` |
| Axes/ticks found but fit fails | `CALIBRATION_FAILURE` |
| VLM unavailable but deterministic evidence is complete and no VLM/Knowledge claims are used | Not a release blocker by itself |
