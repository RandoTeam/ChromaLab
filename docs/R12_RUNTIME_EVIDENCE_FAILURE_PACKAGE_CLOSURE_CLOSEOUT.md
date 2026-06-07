# R12 Runtime Evidence And Failure Package Closure Closeout

Date: 2026-06-07

Status: `R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSED`

Scope: evidence/export contract only. R12 did not change Android runtime
analysis behavior, chromatographic math, report gates, model policy,
`CalculationEngine`, graph-count metadata, or validator severity.

## Why R12 Exists

R0-R11 created shadow benchmark contracts and calibration evidence, but Phase 9
was still not accepted because product reviewers need every Android run to be
inspectable. R12 closes the evidence/package accountability layer first:

- no silent timeout/no-export state;
- every run needs core RuntimeEvidencePackage, validator, report, and manifest
  artifacts;
- blocked graph-stage runs need a graph failure package;
- missing overlays must be explicit, not hidden.

This phase does not upgrade Android `BLOCKED` or `REVIEW_ONLY` outcomes with
R10/R11 benchmark-only evidence.

## Implementation

R12 added stricter validation contracts in
`AutonomousValidationFixtureContracts`:

- run summaries now report missing RuntimeEvidencePackage, validator JSON,
  validator Markdown, final report JSON, and export manifest as explicit
  issues;
- artifact manifests can be validated for required slots, duplicate slots,
  available-artifact locations, and missing-artifact reasons;
- blocked or graph-stage failure manifests must include a graph failure package;
- required validation slots are centralized for test and runner reuse.

R12 also added a source-controlled audit generator:

```powershell
python tools/benchmark/run_r12_runtime_evidence_failure_package_audit.py --clean
```

The generator consumes tracked Phase 9J benchmark records and writes:

- `benchmark/reports/r12_runtime_evidence_failure_package_closure/summary.json`;
- `benchmark/reports/r12_runtime_evidence_failure_package_closure/summary.md`.

## Audit Result

| Metric | Result |
|---|---:|
| Audited fixtures | 8 |
| Audited runs | 16 |
| Core artifact complete runs | 16 / 16 |
| No-export states | 0 |
| Blocked runs | 4 |
| Blocked runs with graph failure package | 4 / 4 |
| Blocked runs missing first failing stage | 0 |
| Review-only runs | 12 |
| Release-ready runs | 0 |

R12 result: core evidence/export closure is complete for the tracked Phase 9J
records. The product remains not release-ready because four runs are still
`BLOCKED`, twelve are `REVIEW_ONLY`, and no fixture is `RELEASE_READY`.

## Blocked Runs Preserved

| Fixture | Modes | Gate | Failure | Stage |
|---|---|---|---|---|
| `bench_01_mz71_screenshot_page` | deterministic, E2B | `BLOCKED` | `TICK_LOCALIZATION_FAILURE` | `Y_CALIBRATION` |
| `bench_05_tic_plus_ions` | deterministic, E2B | `BLOCKED` | `CALIBRATION_FAILURE` | `Y_CALIBRATION` |

R12 deliberately keeps these runs blocked. The phase only proves that the
failure state is inspectable with core artifacts and graph failure packages.

## Tests

R12 adds contract coverage for:

- missing evidence/export artifacts in a terminal run summary;
- blocked manifests missing required artifact slots;
- graph failure package requirement;
- missing reasons for expected-but-unavailable artifacts;
- available records missing locations;
- duplicate manifest slots.

## Product Meaning

R12 improves trust in the validation process. It does not improve chromatogram
analysis correctness. Phase 9 is still not accepted, and Phase 10 must not
start from this result.

## Next Phase

```text
R13 - Android Runtime OCR Anchor Production Bridge
```

R13 should produce Android runtime OCR anchor rows equivalent to the R10/R11
benchmark rows, persist crop files or explicit missing-crop reasons, and prove
whether `bench_01` and `bench_05` are still blocked by anchor propagation,
layout, or calibration.
