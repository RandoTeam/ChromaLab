# Phase 9I Bench 05 TIC + Ions Layout Review

Date: 2026-05-26

## Fixture

`bench_05_tic_plus_ions`

## Layout Question

Phase 9I reviewed whether this fixture should be treated as:

- a single graph with multiple traces;
- TIC + ion/channel panels;
- multiple graph panels;
- title/legend/channel text without separate reportable graph panels.

## Decision

Expected graph count remains `4`.

This is a TIC + ion/channel style fixture. Product, QA, and Chromatography SME signoff do not accept changing the expected count to match the current algorithm output. The current runtime evidence still collapses the fixture into one graph failure package, so the graph layout semantics are incomplete for the product target.

## Current Runtime Result

| Mode | Report graphs | Failure packages | Gate | Validator | Failure |
| --- | ---: | ---: | --- | --- | --- |
| Deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `CALIBRATION_FAILURE` |
| E2B | 0 | 1 | `BLOCKED` | `REVIEW` | `CALIBRATION_FAILURE` |

The selected graphPanel and plotArea are present, and overlays are exported. The blocker is calibration safety: Y evidence is review-grade but direction-inconsistent.

## Calibration Decision

Phase 9I keeps `bench_05` blocked because the current Y fit would make intensity increase as pixel Y increases. That is incompatible with normal image-coordinate chromatogram calibration. The app must not convert this into REVIEW_ONLY or produce peak metrics from that calibration.

## E2B Safety

E2B did not erase deterministic graph candidates, change graph count, change calibration status, or alter numeric metrics in the Phase 9I rerun. It remains advisory only for this fixture.

## Remaining Work

Future repair must improve TIC + ion layout propagation and Y scale evidence. Acceptable future outcomes are:

1. detect/report the expected physical panels with per-panel evidence; or
2. document a scientifically/product-approved unsupported input class with complete graph-level evidence.

Phase 9I does neither, so Phase 9 remains blocked.
