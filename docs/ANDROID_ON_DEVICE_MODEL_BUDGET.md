# Android On-Device Model Budget

Phase 6 records model budget data for every VLM-backed evidence task.

## Runtime Profile Fields

- profile id;
- task id;
- model id;
- runtime backend;
- input image size;
- crop size;
- duration;
- timeout;
- timed out flag;
- success/failure;
- cache hit/miss;
- memory before/after if available;
- thermal warning if available;
- error code.

## Policy

- Do not run full-image VLM if deterministic candidates are strong.
- Prefer local crops over whole images.
- Cache per image/crop hash.
- Use bounded timeouts.
- FULL_ANALYSIS may run deeper model checks.
- FAST mode should minimize VLM calls.
- VLM timeout produces evidence and a warning, not a silent block.

## Phase 6 Unblock Runtime Rules

- Local crop VLM OCR uses the `OCR_CROP_READ` structured-task timeout of 6 seconds.
- Full-image advisory graph/axis tasks use structured-task timeouts of 8 seconds.
- `ActiveVisionModelBackend` records a `ModelRuntimeProfile` for local crop VLM success and failure paths.
- The profile is propagated through `VisionLocalTextCropResult` and `PeakLabelEvidence`, then exported by `RuntimeEvidencePackageBuilder`.
- Validator closeout requires VLM-backed peak-label evidence to have a matching crop row, stage judge row, and model runtime profile.
- Real Android device timing and thermal measurements are still release/device-validation evidence; desktop closeout does not claim real-device performance certification.

## Privacy

Diagnostic packages may include crop paths and task ids. Full prompts should not be exported by default if they contain user image context or sensitive filenames. Store prompt ids and schema ids instead.
