# Phase 9D Geometry / Multi-Graph Repair

## Changes Made

- Added leading-panel inference to the Android line-based graph detector so stacked panel pages can consider a panel above the first detected horizontal line.
- Changed validation sweep region selection so a valid multi-graph result begins from the first reading-order region instead of a later geometry winner.
- Disabled VLM geometry hints for validation fixture runs. Deterministic graphPanel/plotArea/axis/tick/calibration now remains primary in both deterministic and E2B validation modes.

## Android Result

The E2B zero-graph regression on `bench_02_mz92_belyi_tigr` was closed:

| Mode | Phase 9C | Phase 9D Final |
| --- | --- | --- |
| deterministic | 2 metadata graphs, one report graph | 2 metadata graphs, one report graph |
| E2B | 0 graphs / BLOCKED | 2 metadata graphs, one report graph / DIAGNOSTIC_ONLY |

## Remaining Geometry Issues

- `bench_02_mz92_belyi_tigr` still over-detects candidate count in metadata.
- `bench_04_stacked_xic_resolution`, `bench_05_tic_plus_ions`, and `bench_06_photo_two_graphs_page` still process one graph where the fixture class requires panel-aware handling.
- The graph detector still needs stronger scientific-axis-system grouping: shared axes, panel separators, labels, and plotArea containment must drive the one-graph vs multi-graph decision.

No fixture-specific coordinates were introduced.
