# PC Chromatogram Algorithm Audit

Date: 2026-06-02

Status: first PC-first audit slice complete. This is not a release acceptance.

## Scope

User request: run all chromatogram schemes on PC and improve the analysis logic
toward complete, accurate chromatogram calculations.

Boundary for this slice:

- no `CalculationEngine` changes;
- no chromatographic math changes;
- no fixture-specific coordinate hardcoding;
- no fake calibration or expected-success conversion;
- all calculations and evidence generation run through the desktop/offline path.

## Activated Agents And Skills

Agents:

- Orchestrator
- Research Intelligence
- Geometry / Calibration Core
- Trace Extraction / Peak Review
- Chromatography SME
- Scientific Reporting / Validation
- QA / Regression
- Product Acceptance

Skills:

- current-web-research-deep
- source-quality-triage
- research-synthesis
- geometry-calibration-robust-fit
- trace-extraction-masks
- peak-review-integration
- chromatography-domain-review
- peak-metric-semantics
- evidence-package-validator
- regression-benchmark-golden
- test-plan-authoring
- definition-of-done

## PC Suite Runner

Added script:

```powershell
.\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1
```

Default output:

```text
build\phase-pc-algorithm-audit\
```

The script runs the desktop offline analysis CLI on all eight bench fixtures and
writes one output folder per fixture. Each fixture folder contains:

- `audit.json`
- `audit_summary.md`
- `calibrated_report_ui_contract.json`
- `calibrated_report.md`
- graph candidate overlays
- graph focus artifacts
- axis label crops and diagnostics
- peak overlays when reached

It also writes:

```text
build\phase-pc-algorithm-audit\pc_chromatogram_bench_summary.csv
```

## PC Run Result

Command executed:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1 -OutputRoot "build\phase-pc-algorithm-audit-script" -SkipGradleBuild
```

Result: all eight fixtures ran without process failure; all produced desktop audit
artifacts.

| Fixture | Expected graphs | Detected graphs | Ready for calculation | Blocked stage | Calibrated graphs | Signal-ready graphs | Peak-ready graphs |
| --- | ---: | ---: | --- | --- | ---: | ---: | ---: |
| `bench_01_mz71_screenshot_page` | 2 | 2 | false | `axis_calibration` | 0 | 0 | 0 |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | false | `crop_quality` | 0 | 0 | 0 |
| `bench_03_small_tic_export` | 1 | 1 | false | `axis_calibration` | 0 | 0 | 0 |
| `bench_04_stacked_xic_resolution` | 4 | 4 | false | `axis_calibration` | 0 | 0 | 0 |
| `bench_05_tic_plus_ions` | 4 | 4 | false | `axis_calibration` | 0 | 0 | 0 |
| `bench_06_photo_two_graphs_page` | 2 | 2 | false | `axis_detect` | 0 | 0 | 0 |
| `bench_07_rotated_page_photo` | 1 | 1 | false | `axis_calibration` | 0 | 0 | 0 |
| `bench_08_mz71_duplicate_candidate` | 1 | 1 | false | `axis_calibration` | 0 | 0 | 0 |

## What Actually Works Today On PC

- Fixture identity and image integrity are stable.
- Desktop runner starts and completes all eight fixture runs.
- Graph count is correct for all eight fixtures in the current PC path.
- Multi-panel fixture graph counts are correct on PC for `bench_01`, `bench_04`,
  `bench_05`, and `bench_06`.
- Orientation correction remains exercised by `bench_07`.
- Evidence artifacts are generated for each fixture.

## What Does Not Work Yet

- No fixture reaches automatic calculation-ready state on the PC path without live
  or replayed axis OCR/VLM evidence.
- Automatic axis calibration is the dominant blocker.
- `bench_02_mz92_belyi_tigr` stops earlier at `crop_quality`, before calibration.
- `bench_06_photo_two_graphs_page` stops at `axis_detect`.
- Peak detection and peak metrics are not product-proven in the fully automatic PC
  path because calibration blocks signal conversion first.

## Why This Is Not A CalculationEngine Problem Yet

The PC runner does not reach calibrated signal conversion on these automatic runs.
`CalculationEngine` is only meaningful after graph, plot area, axis calibration, and
signal conversion provide a calibrated signal. The current failures happen before
that point.

The existing desktop tests also prove that when confirmed manual calibration is
provided, the runner can reach signal conversion, peak detection, peak metrics, and
report validation. That means the first priority is automatic calibration evidence,
not peak math rewrites.

## Research-Backed Gaps

See:

```text
docs\research\2026-06-02_pc_chromatogram_algorithm_research.md
```

Summary:

- chart extraction needs component detection plus OCR plus robust regression;
- axis/tick/label evidence must be separate and auditable;
- peak detection requires quality/overlap/baseline evidence, not a single threshold;
- baseline and filtering choices can change integration accuracy, so math changes
  must be benchmarked rather than edited blindly.

## Next Focused Repair Slice

Recommended next slice: PC axis/calibration evidence closure.

Acceptance for that slice:

- add live or replayed axis-band OCR/VLM fixtures for all eight bench images;
- keep OCR/VLM text-only and evidence-bound;
- produce at least REVIEW calibration for fixtures where axis labels are readable;
- keep exact blockers for unreadable/missing labels;
- run the PC suite after the fix;
- do not touch `CalculationEngine` unless calibrated-signal tests prove a defect.

## Validation

Executed:

```powershell
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"
powershell -ExecutionPolicy Bypass -File .\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1 -OutputRoot "build\phase-pc-algorithm-audit-script" -SkipGradleBuild
```

Results:

- `ChromatogramBenchFixtureTest`: passed.
- PC suite runner: all eight fixture runs completed and exported audit artifacts.
