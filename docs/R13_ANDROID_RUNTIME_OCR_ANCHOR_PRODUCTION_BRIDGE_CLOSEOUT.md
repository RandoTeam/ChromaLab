# R13 Android Runtime OCR Anchor Production Bridge Closeout

Date: 2026-06-07

Status: `R13_ANDROID_RUNTIME_OCR_ANCHOR_BRIDGE_IMPLEMENTED_NOT_PROMOTED`

Scope: runtime evidence/export contract only. R13 does not change
chromatographic math, `CalculationEngine`, calibration coefficients, graph
selection, report gates, or E2B authority.

## Why R13 Exists

R10 and R11 proved that safe OCR anchor rows can be represented in benchmark
records and consumed by shadow calibration closure. Android runtime packages
still needed equivalent row-level evidence so reviewers can see whether a
calibration failure is caused by missing OCR text, missing deterministic pixel
geometry, forbidden text, missing crop provenance, or later calibration
propagation.

## Implementation

R13 adds a runtime OCR-anchor bridge evidence contract:

- `RuntimeOcrAnchorBridgeRow`;
- `RuntimeOcrAnchorBridgeBuilder`;
- `GeometryTrace.runtimeOcrAnchorRows`;
- `RuntimeEvidenceGraphPackage.runtimeOcrAnchorRows`;
- `RuntimeGraphFailurePackage.runtimeOcrAnchorRows`;
- validator summary rows and Markdown rendering.

Each row records:

- graph id and graph index;
- axis;
- raw OCR text and parsed numeric value;
- deterministic pixel coordinate when available;
- crop path or explicit missing-crop reason;
- confidence;
- geometry source;
- numeric source;
- projection source and residual slot;
- accepted/rejected/semantic-only status;
- rejection reason.

The bridge is generated from existing deterministic tick/OCR and
AxisScaleResolver evidence. It does not create new calibration authority.

## Safety Rules Enforced

R13 validator checks block:

- accepted rows without parsed numeric value;
- accepted rows without deterministic pixel geometry;
- accepted rows with title, ion, m/z, SIM/channel, scan, or method text;
- accepted rows from rejected/semantic-only geometry sources;
- VLM/E2B numeric authority;
- rows without crop path or explicit missing-crop reason;
- rejected/semantic-only rows without rejection reason;
- graph-index mismatches between row and containing package.

E2B remains allowed only for OCR/semantic assistance. It cannot create pixel
geometry, calibration coefficients, trace, peak metrics, or report gates.

## Validation Result

Targeted validator tests pass:

```powershell
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RuntimeEvidencePackageValidatorTest"
```

The test coverage proves:

- RuntimeEvidencePackageBuilder carries OCR-anchor bridge rows from
  GeometryTrace into graph packages;
- validator summary exposes the row table;
- accepted rows without deterministic pixels fail validation;
- accepted forbidden scale text fails validation;
- graph failure packages can carry runtime OCR-anchor bridge evidence.

## Product Meaning

R13 closes the Android/runtime evidence contract gap for OCR anchor rows. It
does not close Phase 9 and does not promote runtime calibration. Existing
runtime failures remain honest until R14 connects Android anchor rows into the
calibration ensemble and Android validation reruns prove no regression.

## Next Phase

```text
R14 - Runtime Calibration Promotion Candidate
```

R14 should feed Android runtime anchor rows into `CalibrationStrategyEnsemble`
as a named strategy source, preserve legacy White Tiger fallback, export
selected/rejected strategy evidence per graph, and prove that E2B cannot alter
strategy selection or numeric metrics.
