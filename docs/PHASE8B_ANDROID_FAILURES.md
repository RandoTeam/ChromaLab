# Phase 8B Android Failures

## Current Status

No fixture-mode Android runtime failure has been classified yet in this work slice. The validation entry point now makes the run deterministic enough to classify failures without manual photo picker ambiguity.

Device `a36d1946` was attached, but installing the rebuilt debug APK failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because the installed `com.chromalab.app` signature does not match the generated debug APK. The existing app was not uninstalled to avoid deleting device data without explicit approval.

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

## Current Fixture Expectation

The White Tiger Ion 71 fixture may currently end in axis/tick/OCR/calibration review. That must be classified using the Phase 8 taxonomy and must not be converted into a release-ready report without evidence.
