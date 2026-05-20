# Phase 9 Android Validation Results

Date: 2026-05-20

## Fixture Runs

| Run | Mode | Graphs | X/Y calibration | Report gate | Validator | Blocking issues | Runtime failure class | Notes |
| --- | --- | ---: | --- | --- | --- | ---: | --- | --- |
| `white_tiger_ion71_20260520_192547` | Deterministic no-model | 1 | VALID / VALID | REVIEW_ONLY | REVIEW | 0 | VLM_SEMANTIC_LAYER_UNAVAILABLE | Expected baseline warning; exports completed. |
| `white_tiger_ion71_20260520_192400` | Model-enabled E2B fallback | 1 | VALID / VALID | REVIEW_ONLY | PASS | 0 | UNKNOWN_FAILURE | E2B loaded after deterministic calibration; no numeric regression. |
| `white_tiger_ion71_20260520_191649` | Earlier model-enabled attempt | 0 | failed before report graph | BLOCKED | REVIEW | 0 | TICK_LOCALIZATION_FAILURE | Used as failure evidence; fixed by deferring model activation until after deterministic calibration. |

## Artifact Roots

Pulled local artifact copies:

```text
artifacts/phase9-android-validation/white_tiger_ion71_20260520_192547/
artifacts/phase9-android-validation/white_tiger_ion71_20260520_192400/
```

Device export roots:

```text
/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_192547/
/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_192400/
```

## Result

The final code path validates both required modes on real Android:

- deterministic no-model path remains REVIEW_ONLY with one graph and valid calibration;
- model-enabled E2B path remains REVIEW_ONLY with one graph and valid calibration;
- model activation no longer corrupts graph selection, tick localization, or calibration.

Phase 9 does not claim RELEASE_READY for this fixture because trace/peak/report evidence remains review-grade.
