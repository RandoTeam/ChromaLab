# DR-B1 Benchmark Schema Examples And Validation Runner

Status: `DR_B1_COMPLETE`

Date: 2026-06-03

## Purpose

DR-B1 turns the DR-B benchmark contracts into a reproducible validation slice.
It adds example JSON records and a schema runner. It does not change production
analysis, `CalculationEngine`, chromatographic math, Android runtime, or report
validators.

## What Was Added

| Artifact | Path |
| --- | --- |
| Minimal synthetic complete truth example | `benchmark/examples/synthetic_minimal/truth.json` |
| Minimal synthetic prediction example | `benchmark/examples/synthetic_minimal/prediction.json` |
| Minimal synthetic metrics example | `benchmark/examples/synthetic_minimal/metrics.json` |
| Minimal synthetic evidence package example | `benchmark/examples/synthetic_minimal/evidence-package.json` |
| Minimal synthetic report claims example | `benchmark/examples/synthetic_minimal/report-claims.json` |
| Phase 9J White Tiger review prediction example | `benchmark/examples/phase9j_white_tiger_review/prediction.json` |
| Phase 9J White Tiger review metrics example | `benchmark/examples/phase9j_white_tiger_review/metrics.json` |
| Phase 9J White Tiger review evidence package example | `benchmark/examples/phase9j_white_tiger_review/evidence-package.json` |
| Phase 9J White Tiger review report claims example | `benchmark/examples/phase9j_white_tiger_review/report-claims.json` |
| Schema validation runner | `tools/benchmark/validate_benchmark_schemas.py` |
| Runner dependency file | `tools/benchmark/requirements.txt` |

## Validation Command

```powershell
python -m pip install -r tools/benchmark/requirements.txt
python tools/benchmark/validate_benchmark_schemas.py
```

Expected output:

```text
Benchmark schema validation passed: 5 schemas, 9 example documents.
```

## Why Two Example Classes

`synthetic_minimal` proves the complete schema path for release-ready synthetic
truth.

`phase9j_white_tiger_review` proves that current real audit results can be
represented without pretending they are release-ready. It stays `REVIEW_ONLY`
and preserves `PEAK_EVIDENCE_FAILURE`.

## Guardrails Preserved

- The examples do not create new production evidence.
- The Phase 9J example does not upgrade review-grade output.
- VLM/LLM remains forbidden as numeric authority.
- Report claims are explicitly `SUPPORTED`, `REVIEW`, `REJECTED`, or
  `MISSING_EVIDENCE`.

## Next Slice

Recommended next slice:

`DR-B2: Convert Phase 9J Truth Audit To Benchmark Records`

Goal:

- generate benchmark prediction/metrics/report-claims records for all eight
  Phase 9J fixtures and both modes;
- keep blocked/review decisions exactly as the audit recorded them;
- produce the first benchmark summary table.
