# Autonomous Peak Evidence Model

## Purpose

The peak evidence model separates deterministic peak calculations from release claims. A peak is not accepted because it appears in a table; it is accepted because the report can show the source trace, apex evidence, metric evidence, boundary evidence, and provenance.

## Core Types

- `PeakEvidence`: one evidence row per detected/reviewed peak.
- `PeakEvidenceStatus`: `AUTO_VALID`, `AUTO_REVIEW`, `USER_CONFIRMED`, `USER_EDITED`, `USER_REJECTED`, `ARTIFACT_REJECTED`, `NOISE_REJECTED`, `SHOULDER_REVIEW`, `OVERLAP_REVIEW`, `INVALID`.
- `PeakMetricEvidence`: metric value plus status and source. Missing values stay `UNKNOWN`.
- `PeakBoundaryEvidence`: start/end RT, boundary method, integration method, baseline method, and status.
- `PeakProvenance`: calculation run id, source signal id, pipeline version, algorithm version, trace source id, and whether user intervention occurred.
- `PeakGateStatus`: `VALID`, `REVIEW`, `INVALID`, `MISSING`.

## Autonomous Mapping

`PeakEvidenceMapper` maps existing `CalculationRun` and `PeakResult` data. It does not run detection, integrate peaks, or alter chromatographic math.

Mapping decisions:

- linked nearest signal sample becomes apex evidence;
- height, area, width, prominence, S/N, and boundary data keep deterministic source;
- missing optional FWHM or baseline-at-apex is marked unknown;
- low confidence, low S/N, shoulder, overlap, and unresolved peaks are review-grade;
- rejected peaks are not reportable.

## Assisted Review Mapping

Assisted Review can add user decisions through `UserConfirmedPeakSet` and `UserPeakEditDecision`. User intervention must remain visible in report provenance and must not be merged silently into autonomous evidence.

## VLM Boundary

VLM/OCR may help explain warnings or read labels in earlier layers. VLM/OCR must not populate RT, height, area, FWHM, S/N, baseline, Kovats, apex coordinates, or final peak metrics.
