# Phase 7B Report Evidence Hardening

Status: implemented

## Goal

Phase 7B hardens the Phase 7 report layer before Phase 8 regression by closing deferred evidence issues:

- first-class Knowledge Pack citation records;
- typed visual evidence statuses;
- golden report acceptance assertions;
- minimum multi-graph report usability;
- privacy/export safety recheck;
- validator overclaim tests.

## Scope Boundaries

No CalculationEngine, chromatographic math, peak detection math, VLM numeric boundary, or image-processing behavior was changed. Assisted/manual review remains fallback only. Missing evidence stays visible in report gates and exports.

## Implementation Summary

First-class citation records were added to `ChromatogramReport`. The calculation-run report mapper now preserves Knowledge Pack matches as structured citations. HTML, Markdown, Compose, and JSON surfaces expose those records in the evidence/technical appendix.

Loose visual evidence strings were replaced with `ReportVisualEvidenceStatus`. Evidence chips now show status text and Compose semantics include the status value.

The normal report export manifest no longer lists raw device logs or `NEVER_SHARED_BY_DEFAULT` artifacts. Runtime evidence and diagnostic bundles remain separate export classes.

## Multi-Graph Acceptance

The report overview table preserves per-graph title/channel, peak count, calibration status, trace status, peak evidence status, and section status. HTML, Markdown, and JSON golden assertions verify that graph-level warnings and indexes survive export.

## Overclaim Prevention

Validator and golden tests cover:

- missing calibration, trace, or peak evidence cannot produce `RELEASE_READY`;
- missing graph evidence produces visible `BLOCKED`;
- VLM explanations without used Knowledge Pack entries are review evidence;
- Knowledge Pack citations cannot create numeric peak metrics;
- compound hypotheses without identity evidence are not rendered as identified compounds;
- Kovats/RI cannot be calculated without reference retention times.

## Remaining Phase 8 Work

A richer interactive multi-graph selector remains a Phase 8 candidate. Phase 7B only guarantees minimum usability and export correctness.
