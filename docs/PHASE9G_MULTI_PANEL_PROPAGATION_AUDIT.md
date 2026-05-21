# Phase 9G Multi-Panel Propagation Audit

## Scope

Phase 9G focused on calibration strategy arbitration. Multi-panel propagation was inspected because Phase 9F still showed mismatches for stacked XIC, TIC plus ions, and two-graph page fixtures.

## Findings

| Fixture | Current issue |
| --- | --- |
| `bench_04_stacked_xic_resolution` | Expected/report metadata count remains 4 while runtime layout evidence often resolves one physical graph package. Final report graph count does not represent the expected panel count. |
| `bench_05_tic_plus_ions` | Expected count remains 4; blocked calibration failures keep graph evidence in failure packages rather than final report graphs. |
| `bench_06_photo_two_graphs_page` | Expected count remains 2; final report output can still contain one graph because aggregate multi-graph report propagation is incomplete. |

## Code-Level Propagation Risk

Current report mapping still centers on a single graph report path. If product acceptance requires aggregate reports for every detected panel in one run, follow-up work must update report options and mapper contracts rather than only the Android suite summarizer.

## Phase 9G Decision

No expected graph counts were changed. Multi-panel propagation remains a blocker unless Android rerun evidence proves the current layout/report propagation is adequate.
