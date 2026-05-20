# Phase 8B Export Workflow Review

## Scope

The fixture validation mode exports developer validation artifacts. It does not change normal user report export policy.

## Export Location

Android debug validation artifacts are exported to:

`/sdcard/Download/ChromaLab/validation/<run_id>/`

The Android implementation uses `MediaStore.Downloads` on Android Q+ and public Downloads fallback on older devices.

## Artifact Manifest

Each run writes an `artifact_manifest_<id>.json` that records:

- required artifact slot;
- file name;
- availability;
- public location when saved;
- missing reason when unavailable.

Run `white_tiger_ion71_20260520_162317` produced:

- `runtime_evidence_package_white_tiger_ion71_20260520_162317.json`
- `runtime_evidence_validation_white_tiger_ion71_20260520_162317.json`
- `runtime_evidence_validation_white_tiger_ion71_20260520_162317.md`
- `final_report_contract_white_tiger_ion71_20260520_162317.json`
- `report_white_tiger_ion71_20260520_162317.html`
- `report_white_tiger_ion71_20260520_162317.md`
- `stage_timings_white_tiger_ion71_20260520_162317.json`
- `log_summary_white_tiger_ion71_20260520_162317.md`
- `artifact_manifest_white_tiger_ion71_20260520_162317.json`

The manifest marks graphPanel, plotArea, axis/tick, calibration, trace, and peak overlays unavailable with the explicit reason:

`Terminal failure happened before overlay generation.`

## Privacy Review

- Camera/gallery/photo picker is bypassed only for a bundled app asset.
- Raw VLM prompts/internal traces are not added to user-facing report artifacts by this change.
- Normal `USER_REPORT` exports remain separate from the validation artifact directory.
- Missing artifact reasons are operational and should not include private user paths.
- The fixture image is committed intentionally as a developer validation asset; it is not a user image export.
- The side-by-side validation install preserved existing `com.chromalab.app` data; no uninstall or data deletion was performed.
- User-facing report artifacts do not include raw logcat. The logcat excerpt was collected externally under local `artifacts/` for Phase 8B validation evidence.

## Manual Check Result

For run `white_tiger_ion71_20260520_162317`:

- runtime evidence JSON exists: yes;
- validator JSON/Markdown exists: yes;
- report contract JSON exists: yes;
- HTML/Markdown exports exist: yes;
- manifest records overlay availability or explicit missing reasons: yes;
- no raw debug logs are included in user report files: yes.

The remaining blocker is runtime model availability, not export workflow.
