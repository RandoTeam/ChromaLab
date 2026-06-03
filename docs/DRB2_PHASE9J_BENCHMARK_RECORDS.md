# DR-B2 Phase 9J Benchmark Records

Status: `DR_B2_COMPLETE`

Date: 2026-06-03

## Purpose

DR-B2 converts the Phase 9J autonomous analysis truth audit into benchmark
records. It does not change production analysis, Android runtime,
`CalculationEngine`, chromatographic math, validators, graph detection,
calibration, trace extraction, peak integration, or report rendering.

The goal is to make the current product truth machine-readable so future
research waves can prove improvement against the same cases.

## Source

| Source | Path |
| --- | --- |
| Phase 9J truth audit summary | `artifacts/phase9j-truth-audit/phase9j_summary.json` |
| Phase 9J truth audit markdown | `artifacts/phase9j-truth-audit/phase9j_summary.md` |

## Generated Records

Generator:

```powershell
python tools/benchmark/generate_phase9j_records.py --clean
```

Output root:

`benchmark/examples/phase9j_truth_audit/`

Each of the 16 Phase 9J runs now has:

- `prediction.json`
- `metrics.json`
- `evidence-package.json`
- `report-claims.json`

The output folder also includes:

- `summary.json`
- `summary.md`

## Fixture Coverage

| Fixture | Modes |
| --- | --- |
| `white_tiger_ion71` | deterministic, E2B baseline |
| `bench_01_mz71_screenshot_page` | deterministic, E2B baseline |
| `bench_02_mz92_belyi_tigr` | deterministic, E2B baseline |
| `bench_03_small_tic_export` | deterministic, E2B baseline |
| `bench_04_stacked_xic_resolution` | deterministic, E2B baseline |
| `bench_05_tic_plus_ions` | deterministic, E2B baseline |
| `bench_06_photo_two_graphs_page` | deterministic, E2B baseline |
| `bench_07_rotated_page_photo` | deterministic, E2B baseline |

## Current Truth Counts

From `benchmark/examples/phase9j_truth_audit/summary.json`:

| Metric | Count |
| --- | ---: |
| Benchmark cases | 16 |
| REVIEW decisions | 12 |
| BLOCKED decisions | 4 |
| RELEASE_READY decisions | 0 |
| Validator REVIEW | 10 |
| Validator PASS | 6 |

Failure-class counts:

| Failure class | Count |
| --- | ---: |
| `PEAK_EVIDENCE_FAILURE` | 4 |
| `TICK_LOCALIZATION_FAILURE` | 2 |
| `GRAPH_PANEL_FAILURE` | 2 |
| `VLM_SEMANTIC_LAYER_UNAVAILABLE` | 3 |
| `UNKNOWN_FAILURE` | 3 |
| `CALIBRATION_FAILURE` | 2 |

## Guardrails Preserved

- Review and blocked results remain review and blocked.
- No `RELEASE_READY` result was created by the benchmark conversion.
- E2B is represented as `e2b_baseline`, not as numeric authority.
- Missing artifacts are recorded as missing-artifact reasons.
- Report claims stay `SUPPORTED`, `REVIEW`, `REJECTED`, or
  `MISSING_EVIDENCE`.

## Validation

Command:

```powershell
python tools/benchmark/validate_benchmark_schemas.py
```

Expected output:

```text
Benchmark schema validation passed: 5 schemas, 73 example documents.
```

## Next Slice

Recommended next slice:

`DR-B3: Benchmark Summary Scoring And Fixture Truth Gaps`

Goal:

- compute a compact score table from all benchmark records;
- list missing ground-truth fields per fixture;
- decide which fixtures need annotation before DR-C graph/layout method
  comparisons.
