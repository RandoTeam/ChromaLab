# R2 Stage 1-3 Shadow Parity Harness Closeout

Date: 2026-06-06

Verdict: `R2_SHADOW_PARITY_HARNESS_READY_PRODUCTION_UNCHANGED`

Scope completed: PC-side shadow/parity harness for Stage 1-3 records. R2 did
not change Android runtime behavior, validators, report gates, graph-count
expectations, chromatographic math, model policy, or `CalculationEngine`.

## Purpose

R2 makes Stage 1-3 replacement measurable before any production switch.

It converts current Android baseline outputs and existing PC/research candidates
into a shared `Stage123ParityRecord` shape:

- Stage 1: image preparation status and truth gaps;
- Stage 2: graph discovery and graph count;
- Stage 3: plotArea/layout class;
- evidence source paths;
- promotion decision.

The result is not a fix. It is a controlled measurement layer that prevents
future Rust/CV work from becoming another untracked parallel implementation.

## Files Created

| Path | Role |
|---|---|
| `benchmark/schemas/stage123-parity-record.schema.json` | Schema for Stage 1-3 shadow parity records. |
| `tools/benchmark/run_stage123_shadow_parity.py` | PC harness that materializes R2 records and summary reports. |
| `benchmark/examples/stage123_shadow_parity/` | 36 schema-backed Stage 1-3 parity example records. |
| `benchmark/reports/stage123_shadow_parity/summary.json` | R2 machine-readable summary. |
| `benchmark/reports/stage123_shadow_parity/summary.md` | R2 human-readable summary. |
| `docs/R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md` | This closeout. |

## Files Updated

| Path | Change |
|---|---|
| `benchmark/schemas/metrics.schema.json` | Added `image_preparation` as a scoreable benchmark stage. |
| `tools/benchmark/validate_benchmark_schemas.py` | Added validation for `stage123-parity-record.json` examples. |
| `benchmark/README.md` | Added R2 generation and validation commands. |
| `docs/AUTONOMOUS_ANALYZER_SOURCE_OF_TRUTH_INDEX.md` | Added R2 harness/report docs and artifacts as active benchmark sources. |
| `docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md` | Marked R2 complete and set R3 as the next implementation slice. |
| `docs/AUTONOMOUS_ANALYZER_LAYER_REPLACEMENT_ROADMAP.md` | Updated progress so R0/R1/R2 are no longer treated as future first phases. |
| `docs/README.md` | Added R2 to the documentation index. |
| `docs/CHROMATOGRAM_REGRESSION_MATRIX.md` | Added R2 regression-matrix update. |
| `docs/CHROMALAB_VALIDATION_SUMMARY.md` | Added R2 benchmark harness summary. |
| `ROADMAP.md` | Added R2 status to the 2026 public roadmap. |
| `PIPELINE.md` | Clarified that R2 is shadow-only benchmark tooling, not runtime pipeline behavior. |

## Harness Inputs

| Source | Role |
|---|---|
| `benchmark/reports/phase9j_truth_audit_score/summary.json` | Current active Android baseline records. |
| `benchmark/examples/phase9j_truth_audit/` | Prediction/evidence source paths for current baseline. |
| `benchmark/reports/drc6_p0_graph_layout_method_comparison/summary.json` | PC graph-count prototype candidates. |
| `benchmark/reports/drc7_panel_semantic_classifier/summary.json` | Annotation upper-bound layout candidates. |
| `composeApp/src/androidMain/assets/validation/*.metadata.json` | Fixture graph-count expectations and source image paths. |

## Harness Output Summary

| Source | Records | Graph count pass | Layout pass | Promotion decision |
|---|---:|---:|---:|---|
| `android_phase9j_current` | 16 | 8 | 2 | `BASELINE_RECORD_ONLY` |
| `label_band_assisted_axis_projection_v1` | 4 | 4 | 2 | `DO_NOT_PROMOTE_RESEARCH_ONLY` |
| `full_width_axis_projection_v1` | 4 | 3 | 1 | `DO_NOT_PROMOTE_RESEARCH_ONLY` |
| `geometry_only_panel_count_v1` | 4 | 4 | 2 | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |
| `annotation_text_role_panel_family_v1` | 4 | 4 | 3 | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |
| `annotation_text_role_page_context_upper_bound_v1` | 4 | 4 | 4 | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |

Total:

- records: `36`;
- fixtures: `8`;
- schemas validated: `6`;
- example documents validated: `109`.

## Interpretation

R2 proves the measurement harness works, not that Stage 1-3 are fixed.

Key findings:

1. Current Android Stage 1-3 baseline still fails graph count on multi-panel P0
   fixtures.
2. Current Android baseline exposes too little locked Stage 1 image-prep truth.
3. PC graph-count prototypes show useful signals but are research-only.
4. Annotation upper-bound results show that text-role and page-context features
   can solve layout class, but only if automated later.
5. E2B is neutral for Stage 1-3 graph count in these records; it does not
   regress deterministic graph count.

## Commands

Generate R2 records:

```powershell
python tools\benchmark\run_stage123_shadow_parity.py --clean
```

Validate benchmark schemas and examples:

```powershell
python tools\benchmark\validate_benchmark_schemas.py
```

Expected validation result:

```text
Benchmark schema validation passed: 6 schemas, 109 example documents.
```

## Follow-Up

```text
R3 - Stage 1 Image Preparation Candidate
```

R3 is now completed as a PC-side shadow candidate. The next phase should be:

```text
R4 - Rust Stage 1 Image Preparation Parity Bridge
```
