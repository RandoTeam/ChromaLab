# Phase 7 Mobile Report UX

Date: 2026-05-20

## Primary Flow

The report starts with the autonomous result:

1. Report title and gate status.
2. Run metadata.
3. Overview.
4. Quality and release evidence summary.
5. Per-graph sections.
6. Technical appendix and export manifest.

## UX Rules

- Do not lead with manual review controls.
- Show `Review needed` only when gates fail or warnings exist.
- Keep evidence details expandable or secondary.
- Use text labels for evidence states.
- Keep raw warning codes in the appendix.
- Dense peak tables are scrollable and include evidence/gate columns.

## Accessibility

- Report section titles expose heading semantics.
- Evidence chips expose content descriptions.
- Status names are visible text, not color-only indicators.
- New dense controls were not added; future row actions must use 48dp touch targets.

## Phase 8 Candidate

A dedicated graph selector for very large multi-graph reports is deferred to Phase 8. Phase 7 supports multi-graph structure and export parity.
