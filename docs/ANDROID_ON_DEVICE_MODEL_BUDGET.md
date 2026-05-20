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

## Privacy

Diagnostic packages may include crop paths and task ids. Full prompts should not be exported by default if they contain user image context or sensitive filenames. Store prompt ids and schema ids instead.

