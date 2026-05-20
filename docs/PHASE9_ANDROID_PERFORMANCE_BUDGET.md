# Phase 9 Android Performance Budget

Date: 2026-05-20

## Observed Timings

Final deterministic no-model run `white_tiger_ion71_20260520_192547`:

- geometry pipeline: about 5.4 s;
- report/export path completed;
- no model load.

Final model-enabled E2B run `white_tiger_ion71_20260520_192400`:

- deterministic geometry pipeline: about 5.3 s;
- E2B model load: completed after calibration through LiteRT GPU;
- report/export path completed;
- no pre-calibration VLM timeouts affected geometry.

Earlier failing run `white_tiger_ion71_20260520_191649`:

- graph selection stage: about 140 s;
- E2B loaded successfully but VLM crop/full-image calls timed out;
- calibration regressed to blocked.

## Policy

- Validation fixture model-enabled mode must activate Gemma after deterministic X/Y calibration.
- Deterministic graphPanel, plotArea, tick, and calibration stages must not wait on model loading.
- VLM crop/full-image calls must remain bounded by per-call timeouts.
- No repeated full-image VLM calls are allowed in deterministic geometry candidate selection.
- E4B is the FULL_ANALYSIS target, but if E4B is absent, E2B may be used as FAST/fallback.
- Perfetto should be used for deeper runtime traces in a later production hardening pass.
