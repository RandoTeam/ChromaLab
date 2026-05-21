# Phase 9D E2B Regression Repair

## Root Cause

Phase 9C artifacts showed E2B could become active before all validation geometry/calibration work finished. The E2B path then made a later graph attempt worse than the deterministic baseline.

## Changes Made

- Validation fixture runs disable VLM geometry hints in `AutoSweepEngine`.
- The model-enabled validation path defers Gemma/E2B loading until no unprocessed graph region remains for the deterministic flow.
- Deterministic candidates remain primary; E2B may explain/classify/judge after deterministic geometry, but cannot erase candidates or create numeric geometry/calibration.

## Android Result

`bench_02_mz92_belyi_tigr` no longer regresses to zero graphs in E2B mode. Deterministic and E2B now both produce one report graph with `DIAGNOSTIC_ONLY` and validator `REVIEW`.

## Remaining Limitation

`metadataDetectedGraphCount=2` remains for `bench_02`, so Phase 9D still cannot pass product acceptance.
