# Phase 6 Current VLM/OCR/CV Audit

Date: 2026-05-20

## Current Call Sites

- `VisionModelBackend`
  - Defines local crop OCR, text classification, overlay judgement, and warning summary interfaces.
  - Current contract is permissive and does not carry task ids, runtime profile ids, timeout status, or forbidden field metadata.
- `ActiveVisionModelBackend.android.kt`
  - Implements local crop VLM OCR fallback.
  - Uses a 180 second engine timeout internally plus a 6 second caller timeout in peak label OCR.
  - Parses JSON with regex and falls back to plain text.
  - Current parser can read `parsed_retention_time`, which is acceptable only as semantic text evidence and must not become final RT without signal verification.
- `AxisOcrReader.android.kt`
  - Uses ML Kit for axis labels and local tick crops.
  - Tick pixel positions are deterministic geometry; OCR is only label text.
- `PeakLabelEvidenceReader.android.kt`
  - Writes peak label crops, runs ML Kit, optionally runs VLM only on plot-area local crops when ML Kit is empty/weak/ambiguous.
  - Merges ML Kit and VLM evidence and marks VLM as text-only/no peak metrics.
- `RuntimePeakRecoveryEvaluator`
  - Converts runtime peak-label evidence into recovered candidates only after local signal verification.
  - Fixture hints remain test-only.
- `ChartAnalysisReader.android.kt`
  - Can use a VLM-first axis OCR path and normalize model-returned positions.
  - Risk: model-suggested positions must not become calibration pixel geometry unless paired with deterministic tick/crop evidence.
  - Phase 6 documents this as a follow-up guardrail; this slice does not rewire VLM behavior.
- `GeometryPipelineRunner`, `ScreenshotEmbeddedChartDetector`, `GraphMultiplicityResolver`
  - Deterministic CV owns graphPanel, plotArea, multiplicity, and geometry evidence.
  - VLM is not required as the numeric geometry source.
- `RuntimeEvidencePackage` and `RuntimeEvidencePackageValidator`
  - Before Phase 6, they validated crop paths and VLM text-only warnings for peak label evidence.
  - They did not have generalized stage judge rows, model runtime profiles, OCR/VLM disagreement rows, or forbidden VLM output checks.

## Risks Found

1. VLM crop output parsing is not backed by a reusable schema boundary.
2. VLM/OCR result provenance is scattered across peak-label evidence only.
3. Timeout/caching/model runtime data is not represented as a first-class evidence table.
4. Overlay judge and warning summary paths are interface-level only and not evidenced in the runtime package.
5. Benchmarking of ML Kit vs VLM crops is not formalized.
6. VLM-first axis OCR needs stricter downstream provenance before it can participate in calibration evidence.

## Phase 6 Patch Direction

Phase 6 adds contracts and validator support around the existing paths:

- stage judge results;
- local crop OCR/VLM results;
- OCR/VLM disagreement records;
- overlay judge records;
- model runtime profiles;
- strict forbidden field rejection;
- retry recommendation policy;
- OCR/VLM crop benchmark JSON and Markdown outputs.

This does not change CalculationEngine, full-auto geometry, VLM backend behavior, or chromatographic math.
