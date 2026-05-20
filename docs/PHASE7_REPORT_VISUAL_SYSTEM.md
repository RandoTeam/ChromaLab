# Phase 7 Report Visual System

Date: 2026-05-20

## Visual Direction

Scientific, clinical, compact, and evidence-first. The report should look like an autonomous analytical result, not a debug console and not a manual editing workflow.

## Status Vocabulary

| Status | Visual treatment | Meaning |
| --- | --- | --- |
| RELEASE_READY | Green/valid chip plus text label | Required evidence gates passed. |
| REVIEW_ONLY | Amber/review chip plus text label | One or more gates require review. |
| DIAGNOSTIC_ONLY | Red/diagnostic chip plus text label | Required evidence is missing or invalid. |
| BLOCKED | Red/blocked chip plus text label | Report cannot be used until blockers are resolved. |

## Component Rules

- Use status labels in text; never rely on color alone.
- Evidence badges are compact and adjacent to the section they explain.
- Peak tables put evidence/gate state before numeric metrics.
- Raw warning codes are appendix content, not the primary visual experience.
- HTML uses print-safe, high-contrast table and callout styling.

## Export Parity

HTML, Markdown, and Compose must share the same gate status, gate evidence categories, peak evidence states, and privacy artifact classes.
