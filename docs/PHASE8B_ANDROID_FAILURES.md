# Phase 8B Android Failures

## Current Status

Fixture-mode Android runtime failure is now classified from a real device run.

Device `a36d1946` had an existing `com.chromalab.app` install signed with a different certificate. The existing app was not uninstalled to avoid deleting device data. A side-by-side validation build was installed as `com.chromalab.app.validation`.

## Classified Failure: `white_tiger_ion71_20260520_162317`

| Field | Value |
| --- | --- |
| Fixture | `white_tiger_ion71` |
| Package | `com.chromalab.app.validation` |
| Artifact directory | `/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_162317/` |
| Global report gate | `BLOCKED` |
| Validator verdict | `FAIL` |
| Runtime failure class | `VLM_MODEL_UNAVAILABLE` |
| Failed stage | `IMAGE_QUALITY` / model readiness guard |
| Blocking issues | `package.executed_runtime_missing`, `package.model_metadata_missing`, `package.graphs_missing` |
| Old axis detection failure reached | No |
| Critical blocker | Yes: model unavailable prevents autonomous image analysis |

Failure message:

`AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first.`

Decision: keep Phase 8B blocked. The validation path is functional and exports artifacts, but autonomous fixture analysis cannot validate graphPanel/plotArea/axis/tick/calibration until the required chromatogram vision model is installed or activated.

## Required Failure Recording

Every non-release fixture run must record:

- global report gate;
- validator verdict;
- `runtimeFailureClass`;
- failing autonomous stage;
- blocking issues;
- warnings;
- artifact manifest path;
- whether Assisted Review would be required.

## Critical Blockers

The following remain critical blockers if observed in a fixture run:

- no runtime evidence package export;
- no validator JSON/Markdown export;
- app crash;
- one physical graph becomes multiple reports;
- partial graphPanel when the full panel exists;
- `RELEASE_READY` with missing or invalid calibration;
- `RELEASE_READY` with missing trace/peak evidence;
- VLM/Knowledge numeric metric creation;
- user report includes `NEVER_SHARED_BY_DEFAULT` artifacts;
- report UI unreadable on phone.

Observed in this run:

- no app crash;
- no fake successful report;
- no release-ready overclaim;
- no CalculationEngine or chromatographic math change;
- runtime evidence package, validator JSON, validator Markdown, report JSON, report HTML/Markdown, stage timings, and manifest were exported.

## Current Fixture Expectation

The White Tiger Ion 71 fixture may currently end in axis/tick/OCR/calibration review. That must be classified using the Phase 8 taxonomy and must not be converted into a release-ready report without evidence.

Current blocker precedes axis/tick/OCR/calibration. Re-run after the chromatogram VLM is available to determine whether the historical `AXIS_DETECTION_FAILURE` still occurs.
