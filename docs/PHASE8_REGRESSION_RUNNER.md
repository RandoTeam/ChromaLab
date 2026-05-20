# Phase 8 Regression Runner

## Purpose

Phase 8 adds a manifest-level regression runner contract around the existing desktop bench runner and report validators. It does not replace `ChromatogramBenchFixtureTest` and it does not run chromatographic math itself.

## Current Implementation

The executable Phase 8 runner is test-only and lives in:

- `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase8FullRegressionAcceptanceTest.kt`

It validates:

- dataset inventory coverage;
- required artifact classes;
- failure taxonomy completeness;
- JSON and Markdown regression summary generation;
- report golden JSON/HTML/Markdown output;
- overclaim blocking;
- privacy manifest filtering;
- terminal-state evidence package creation for report fixtures.

## Output Contract

The runner summary has two required exports:

- `phase8_regression_summary.json`
- `phase8_regression_summary.md`

Each result row must include:

- dataset id;
- expected graph count;
- report gate status;
- failure class;
- RuntimeEvidencePackage path;
- validator JSON path;
- validator Markdown path;
- HTML/Markdown export paths;
- timing;
- model/runtime status when available.

## Relationship To Existing Bench Tests

`ChromatogramBenchFixtureTest` remains the heavy desktop runner for actual image fixtures. It reads real image resources, runs offline analysis, writes visual artifacts, and asserts stage-level diagnostics. Phase 8 adds an acceptance layer that makes the dataset, artifacts, taxonomy, and report gates explicit.

## Android Runner Gap

No connected Android device was available during this Phase 8 slice. The Android runner remains manual until a device/emulator is attached and an instrumentation command can export the required artifacts. See `docs/PHASE8_REAL_ANDROID_VALIDATION.md`.

## Non-Goals

- No fixture-specific coordinates beyond the existing bench fixture manifest.
- No CalculationEngine or peak math changes.
- No conversion of failing cases to expected passes.
- No cloud upload or remote validation dependency.
