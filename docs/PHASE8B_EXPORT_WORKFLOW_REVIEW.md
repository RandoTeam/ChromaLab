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

## Privacy Review

- Camera/gallery/photo picker is bypassed only for a bundled app asset.
- Raw VLM prompts/internal traces are not added to user-facing report artifacts by this change.
- Normal `USER_REPORT` exports remain separate from the validation artifact directory.
- Missing artifact reasons are operational and should not include private user paths.
- The fixture image is committed intentionally as a developer validation asset; it is not a user image export.

## Remaining Manual Check

After a real device run, inspect the exported directory and confirm:

- runtime evidence JSON exists;
- validator JSON/Markdown exists;
- report contract JSON exists;
- HTML/Markdown exports open;
- manifest records overlay availability or explicit missing reasons;
- no raw debug logs are included in user report files.
