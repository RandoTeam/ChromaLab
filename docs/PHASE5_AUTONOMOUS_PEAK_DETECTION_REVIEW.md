# Phase 5: Autonomous Peak Detection + Evidence Review

Status: implemented as contracts, evidence mapping, gates, validator coverage, and documentation.

## Product Goal

Autonomous peak detection remains the primary path. Existing `CalculationEngine` output is wrapped in an explicit peak evidence layer so reports can distinguish automatic valid peaks, review-grade automatic peaks, user-confirmed Assisted Review peaks, user-edited peaks, and rejected artifact/noise peaks.

## Scope

- Preserve `CalculationEngine` and chromatographic math.
- Map `CalculationRun.peaks` into `PeakEvidence`.
- Add peak evidence fields to the report contract and runtime evidence package.
- Gate release readiness on peak evidence instead of only “peaks exist”.
- Keep manual peak review as Assisted Review fallback.

## Out Of Scope

- No Phase 6 VLM changes.
- No manual peak editor UI.
- No new peak detection algorithm.
- No deconvolution rewrite.
- No fixture-specific peak tuning.

## Evidence Requirements

Every reportable peak should expose:

- retention time and apex link;
- apex pixel/index when available;
- local maximum evidence;
- height, area, area percent, prominence, width/FWHM when available;
- S/N and baseline evidence when available;
- boundary method and integration window evidence;
- overlap/shoulder/artifact/rejection status;
- calculation or user provenance.

Missing optional fields are represented as `UNKNOWN` or `REVIEW`, never fabricated.

## Gate Rules

- `AUTO_VALID` peaks may satisfy `AUTONOMOUS_PRODUCTION` only when apex, height, area, and boundary evidence are present.
- `AUTO_REVIEW`, `SHOULDER_REVIEW`, and `OVERLAP_REVIEW` peaks keep the report in review unless a future policy explicitly accepts review-grade release.
- `USER_CONFIRMED` and `USER_EDITED` peaks can satisfy Assisted Review gates with visible user provenance.
- `USER_REJECTED`, `ARTIFACT_REJECTED`, `NOISE_REJECTED`, and `INVALID` peaks are not reportable.

## Validation

Phase 5 is validated by peak evidence mapper tests, guided gate tests, runtime evidence validator tests, fixture regressions, Android assembly, desktop compilation, and full desktop tests.
