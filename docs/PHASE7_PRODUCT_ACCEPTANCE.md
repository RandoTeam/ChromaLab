# Phase 7 Product Acceptance

Date: 2026-05-20

## Target Journey

1. User captures or imports a chromatogram image.
2. Autonomous analysis processes all detected graphs.
3. The report appears with a visible gate status.
4. User sees what passed, what needs review, and what is diagnostic-only.
5. Assisted Review is offered only when gates fail or the user opens evidence details.

## Acceptance Checklist

| Report type | Required behavior | Phase 7 status |
| --- | --- | --- |
| Release-ready | Shows `RELEASE_READY`, complete gate evidence, deterministic metric provenance, no blocking validator issues. | Supported by UI contract and HTML test using explicit valid evidence package. |
| Review-only | Shows `REVIEW_ONLY`, review reasons, evidence statuses, and no release-quality claim. | Supported by gate rendering and report evaluator. |
| Diagnostic-only | Shows `DIAGNOSTIC_ONLY`, missing/invalid evidence, and diagnostic-only copy. | Supported by default report builder with missing evidence package. |
| Blocked | Supported as explicit status in contract and renderer copy. | Supported; not used by current evaluator except terminal-state mapping. |
| Multi-graph | Shows graph overview and per-graph sections in detected order. | Supported structurally in UI contract, HTML, Markdown, and Compose. |

## Product Decision

Phase 7 keeps AUTONOMOUS_PRODUCTION as the primary report flow. Manual/assisted review tools are referenced only as fallback paths when evidence gates fail.
