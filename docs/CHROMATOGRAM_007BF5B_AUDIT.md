# Audit Of Commit 007bf5b

Status: hardening audit after `007bf5b`.
Date: 2026-05-18.

## Verdict

`007bf5b` verdict: `ACCEPTABLE_TEST_ONLY` plus `REQUIRES_RUNTIME_IMPLEMENTATION`.

The commit improved fixture observability, but it was not production-ready because
`bench_03` low-resolution peak recovery used fixture expectation times as
`OFFLINE_FIXTURE_HINT` evidence. That evidence is now explicitly marked
`TEST_ONLY`, excluded from production reportable peak counts, and documented as
requiring Android/runtime local OCR or VLM crop evidence before it can be treated as
production recovery.

## Audit Answers

| Question | Finding |
| --- | --- |
| Did `bench_03` pass because of real OCR/VLM local crops? | No. It passed because fixture expected apex labels were converted into offline `FIXTURE_HINT` label evidence and then locally checked against the extracted signal. |
| Did `bench_08` pass because expectations were changed? | Yes, but the change is now evidence-backed by a raw/validated/reportable dense-series table. The old guarded `5 -> 9` contract described an under-detection path, not current signal evidence. |
| Were test contracts relaxed? | The old bench_08 count contract was replaced. This is acceptable only because every counted peak is now required to be linked to a signal apex and marked `isCandidateLineOnly=false`. |
| Were peak counts redefined without runtime evidence? | In `007bf5b`, bench_03 mixed test-only fixture hints into the generic reportable count. This audit fixes that by splitting production and test-only counts. |
| Are recovered peaks generated from signal verification or static metadata? | Both: fixture metadata supplies candidate RT hints, then local signal verification supplies nearest local maximum, height, S/N, curvature, and window evidence. Static metadata alone is no longer production-reportable. |
| Are artifacts sufficient to prove correctness? | Better, but still not complete production proof for bench_03. Artifacts now show label boxes, local windows, target RT lines, and local-maximum markers. Runtime proof still requires ML Kit/VLM crop evidence. |

## `bench_03_small_tic_export`

Root cause:

- The raw extracted signal contains three strong peaks.
- The labeled 5.610 and 8.560 peaks are visible in the image, but the low-resolution
  trace evidence is weak and does not become raw `CalculationEngine` peaks.
- `007bf5b` used expected fixture labels as offline label hints. This was useful for
  proving the recovery contract, but not sufficient for production readiness.

Current hardened behavior:

- `rawDetectedPeakCount = 3`
- `productionReportablePeakCount = 3`
- `testOnlyReportablePeakCount = 5`
- `runtimeRecoveredPeakCount = 0`
- `testOnlyRecoveredPeakCount = 2`
- Fixture labels have `source = FIXTURE_HINT`, `isRuntimeEvidence = false`,
  `evidenceScope = TEST_ONLY`.
- Recovered fixture-only candidates carry `FIXTURE_HINT_ONLY`,
  `LOW_RESOLUTION_RECOVERED`, and `LABEL_EVIDENCE_VERIFIED`.

Production requirement:

- The same `PeakLabelEvidence` contract must be fed by runtime `ML_KIT`, `VLM`, or
  `BOTH` evidence from saved local crops. `FIXTURE_HINT` can never make a recovered
  peak production-reportable.

## `bench_08_mz71_duplicate_candidate`

Root cause:

- The current trace extraction detects a dense Ion 71 series.
- The old guarded fixture expected a base under-detected path and then a tuned 9-peak
  completeness path.
- Current raw detection returns 19 signal-backed peaks; this is not automatically an
  error.

Current hardened behavior:

- `rawCandidatePeakCount = 19`
- `validatedPeakCount = 19`
- `reportablePeakCount = 19`
- `rejectedArtifactPeakCount = 0`
- Each dense-series row records RT, apex pixel X/Y, S/N, FWHM, overlap status,
  baseline quality, width plausibility, artifact suspicion, `isCandidateLineOnly`,
  and `isValidatedApex`.
- A vertical overlay guide is not sufficient evidence. A counted peak must have
  `isValidatedApex = true` and `isCandidateLineOnly = false`.

Decision:

- The old guarded `5 -> 9` expectation is obsolete for this fixture.
- The fixture should not force 9 peaks unless deterministic artifact classification
  proves the extra peaks are frame/grid/text/border artifacts or candidate-only lines.

## Runtime Changes Added By This Audit

- Added shared `PeakLabelEvidence` / `PeakLabelEvidenceResult` contracts.
- Added Android `PeakLabelEvidenceReader` that writes local peak-label crop images and
  runs ML Kit text recognition on those crops.
- Added desktop placeholder reader that reports runtime peak-label OCR as not
  configured instead of pretending fixture hints are runtime evidence.
- Added `GeometryTrace.peakLabelEvidence` and `GeometryTrace.peakLabelCropPaths` so
  runtime reports can expose whether label evidence came from real local OCR/VLM or
  from test-only fixture hints.

## Remaining Runtime Gap

The Android runtime now has a local-crop label evidence reader wired into the geometry
pipeline, but recovered peak promotion in the saved scientific report still needs a
full runtime bridge after signal conversion:

1. read runtime `PeakLabelEvidence`;
2. link each parsed RT through accepted X calibration;
3. verify local signal maximum / shoulder / S/N / prominence / width;
4. mark recovered peaks as `REVIEW` unless the evidence is exceptionally clean;
5. persist recovered peak candidates in the final report contract.

Until that bridge is complete, bench_03 fixture recovery remains test-only evidence,
not production chromatogram recovery.
