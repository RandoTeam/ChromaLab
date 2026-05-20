# Phase 8 Visual And Text Golden Artifacts

## Purpose

Phase 8 requires golden validation for report structure and evidence visibility. Screenshot-level visual diffing is not introduced in this slice; JSON/HTML/Markdown structured goldens are mandatory and executable.

## Golden Cases

| Golden case | Source | Expected status | Required assertions |
| --- | --- | --- | --- |
| `release_ready_single_graph` | Phase 7B complete-evidence fixture | `RELEASE_READY` | JSON includes graphs/citations; HTML/Markdown expose release gate and peak evidence. |
| `review_only_single_graph` | Phase 7B review peak fixture | `REVIEW_ONLY` | Review status is visible; release-ready is not claimed. |
| `diagnostic_only_missing_calibration` | Missing calibration fixture | `DIAGNOSTIC_ONLY` | Calibration absence blocks release and appears in gate reasons. |
| `blocked_missing_graph_evidence` | Empty graph fixture | `BLOCKED` | Missing graph evidence is visible in report gate and validator findings. |
| `multi_graph_report` | Phase 7B multi-graph fixture | `REVIEW_ONLY` | Graph overview and per-graph sections preserve graph order and warnings. |
| `knowledge_vlm_grounded_explanation` | VLM-with-knowledge citation fixture | `RELEASE_READY` if other gates pass | Knowledge citation records include used entry IDs and full entry records. |
| `compound_hypothesis_without_identity_evidence` | Local-knowledge compound hypothesis fixture | `REVIEW_ONLY` | Candidate name is not shown as identified compound. |
| `kovats_caveat_without_reference_series` | Kovats result without reference series | `DIAGNOSTIC_ONLY` | Kovats/RI claim is rejected as release evidence and validator finding appears. |

## Executable Coverage

Executable tests are in:

- `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase8FullRegressionAcceptanceTest.kt`

The tests assert:

- report gate status for every golden case;
- HTML and Markdown gate visibility;
- JSON contract presence for graphs and Knowledge Pack citations;
- export artifact entries for HTML and Markdown;
- multi-graph per-graph sections;
- privacy manifest exclusion of `NEVER_SHARED_BY_DEFAULT` from user-facing report exports.

## Deferred Visual Work

Screenshot-level mobile UI goldens remain deferred because Phase 8 did not launch a Compose UI screenshot harness. This is acceptable for the current review-ready state but should become a Phase 9 production-hardening task if the report UI becomes a release surface.
