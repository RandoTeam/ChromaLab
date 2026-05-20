# Knowledge Pack Adversarial Tests

Date: 2026-05-20

Phase 6C-2 adds adversarial tests for common OCR/VLM failure cases.

## Required Cases

| Case | Expected result |
|---|---|
| `Ion 71.00 (70.70 to 71.70)` | `TITLE_OR_CHANNEL` |
| `m/z 57` | `ION_CHANNEL_TERM` |
| `SIM 85` | `METHOD_METADATA` |
| `5.610` near apex with signal verification | `PEAK_ANNOTATION_CANDIDATE` |
| `5.610` in title/header | `NOT_PEAK_ANNOTATION` |
| Kovats without reference series | Caveat only |
| Compound name without spectral/RI/library evidence | No identification claim |
| Attempted compound identification without evidence | Rejected forbidden use |
| Attempted Kovats fabrication | Rejected forbidden use |
| Bundled entry references restricted/review-only source | Validator failure |

## Test Location

`composeApp/src/commonTest/kotlin/com/chromalab/feature/knowledge/KnowledgePackV2Test.kt`

## Rule

Passing retrieval or classification tests does not make a report release-ready. The evidence gates for geometry, calibration, trace, and peaks still decide release readiness.
