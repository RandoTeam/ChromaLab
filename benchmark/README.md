# ChromaLab Benchmark Contracts

Status: `DR_B_CONTRACT_BASELINE`

Purpose: provide machine-readable contracts for automatic stage-by-stage
chromatogram analysis scoring.

These files are contracts only. They do not change production analysis,
`CalculationEngine`, chromatographic math, validators, Android runtime, or
report rendering.

## Schemas

| Schema | Purpose |
| --- | --- |
| `schemas/truth.schema.json` | Ground-truth annotations for synthetic, real paired, and real diagnostic fixtures. |
| `schemas/prediction.schema.json` | Normalized pipeline output used for benchmark scoring. |
| `schemas/metrics.schema.json` | Stage-by-stage benchmark scores and product decisions. |
| `schemas/evidence-package.schema.json` | Required evidence artifact manifest for benchmark runs. |
| `schemas/report-claims.schema.json` | Release/review/diagnostic claim validation contract. |

## Corpus Classes

- `synthetic`: generated chromatogram image with complete numeric truth.
- `real_paired`: real screenshot/photo with reference signal or vendor/raw export.
- `real_unpaired_diagnostic`: real image without numeric ground truth; useful for
  diagnostic behavior and evidence completeness, not release-quality accuracy.

## Rule

No method is accepted because it looks visually better. It must improve the
relevant schema-backed metric without violating report gates.

## Validate

Install the local benchmark validation dependency:

```powershell
python -m pip install -r tools/benchmark/requirements.txt
```

Run schema and example validation:

```powershell
python tools/benchmark/validate_benchmark_schemas.py
```

Expected result after DR-B2 Phase 9J benchmark conversion:

```text
Benchmark schema validation passed: 5 schemas, 73 example documents.
```

DR-B2 adds generated records under
`benchmark/examples/phase9j_truth_audit/`. Regenerate them with:

```powershell
python tools/benchmark/generate_phase9j_records.py --clean
```

Score the generated Phase 9J benchmark records with:

```powershell
python tools/benchmark/score_phase9j_benchmarks.py
```

The score report is written to:

`benchmark/reports/phase9j_truth_audit_score/`

Create graph/layout annotation workflow records with:

```powershell
python tools/benchmark/plan_drc1_annotations.py
python tools/benchmark/build_drc2_annotation_workflow.py --clean
```

The workflow records are written to:

`benchmark/annotations/drc2_graph_layout_annotation_workflow/`

Render initial P0 graph layout annotation overlays with:

```powershell
python tools/benchmark/render_drc3_annotations.py
```

The initial annotation records and overlays are written to:

`benchmark/annotations/drc3_initial_graph_layout_annotations/`

Build initial P0 tick/text-role annotations with:

```powershell
python tools/benchmark/build_drc4_tick_text_annotations.py --clean
```

The tick/text-role annotations and overlays are written to:

`benchmark/annotations/drc4_tick_text_role_annotations/`

Score current Phase 9J P0 graph/layout output against annotation truth with:

```powershell
python tools/benchmark/score_drc5_p0_graph_layout.py
```

The P0 graph/layout score report is written to:

`benchmark/reports/drc5_p0_graph_layout_score/`

Compare PC-side P0 graph-layout prototype methods with the current Android
baseline:

```powershell
python tools/benchmark/prototype_drc6_p0_graph_layout_methods.py
```

The DR-C6 method comparison report and overlays are written to:

`benchmark/reports/drc6_p0_graph_layout_method_comparison/`

Compare P0 panel semantic layout classifiers:

```powershell
python tools/benchmark/prototype_drc7_panel_semantic_classifier.py
```

The DR-C7 semantic classifier report is written to:

`benchmark/reports/drc7_panel_semantic_classifier/`

Build OCR/text-role feature benchmark targets:

```powershell
python tools/benchmark/prototype_drd1_ocr_text_role_features.py
```

The DR-D1 OCR/text-role benchmark report is written to:

`benchmark/reports/drd1_ocr_text_role_feature_benchmark/`
