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

Expected result after R2 Stage 1-3 parity record conversion:

```text
Benchmark schema validation passed: 6 schemas, 125 example documents.
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

Build Stage 1-3 shadow parity records for image preparation, graph discovery,
and plotArea/layout:

```powershell
python tools/benchmark/run_stage123_shadow_parity.py --clean
python tools/benchmark/validate_benchmark_schemas.py
```

The R2 Stage 1-3 parity records are written to:

`benchmark/examples/stage123_shadow_parity/`

The R2 summary report is written to:

`benchmark/reports/stage123_shadow_parity/`

Build the R3 Stage 1 image-preparation candidate records:

```powershell
python tools/benchmark/run_r3_image_preparation_candidate.py --clean
python tools/benchmark/validate_benchmark_schemas.py
```

The R3 Stage 1 records are written to:

`benchmark/examples/r3_image_preparation_candidate/`

The R3 summary report and contact sheet are written to:

`benchmark/reports/r3_image_preparation_candidate/`

Build the R4 Rust Stage 1 image-preparation parity records:

```powershell
.\tools\rust\Install-LocalRustToolchain.ps1
python tools/benchmark/run_r4_rust_stage1_image_preparation_parity.py --clean
python tools/benchmark/validate_benchmark_schemas.py
```

The R4 Rust Stage 1 records are written to:

`benchmark/examples/r4_rust_stage1_image_preparation_parity/`

The R4 summary report and Rust per-fixture reports are written to:

`benchmark/reports/r4_rust_stage1_image_preparation_parity/`

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

Install PC OCR benchmark dependencies:

```powershell
python -m pip install -r tools/benchmark/ocr-requirements.txt
```

Run the RapidOCR crop OCR benchmark:

```powershell
python tools/benchmark/run_drd2_rapidocr_crop_benchmark.py
```

The DR-D2 RapidOCR crop benchmark report is written to:

`benchmark/reports/drd2_rapidocr_crop_benchmark/`

Run the RapidOCR preprocessing grid and safety-gate benchmark:

```powershell
python tools/benchmark/run_drd3_ocr_preprocessing_grid.py
```

The DR-D3 OCR preprocessing grid report is written to:

`benchmark/reports/drd3_ocr_preprocessing_grid/`

Run the second OCR engine and full-image text-box detection benchmark:

```powershell
python tools/benchmark/run_drd4_second_ocr_text_box_benchmark.py
```

The DR-D4 OCR text-box detection report is written to:

`benchmark/reports/drd4_second_ocr_text_box_benchmark/`

Run the axis-owned OCR crop planner prototype:

```powershell
python tools/benchmark/run_drd5_axis_owned_ocr_crop_planner.py
```

The DR-D5 axis-owned OCR report and crop-plan overlays are written to:

`benchmark/reports/drd5_axis_owned_ocr_crop_planner/`

Run the axis-owned OCR preprocessing grid:

```powershell
python tools/benchmark/run_drd6_axis_owned_ocr_preprocessing_grid.py
```

The DR-D6 preprocessing grid report is written to:

`benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/`

Build axis scale candidates from safe owned OCR evidence:

```powershell
python tools/benchmark/run_dre1_axis_scale_candidate_builder.py
```

The DR-E1 scale candidate report is written to:

`benchmark/reports/dre1_axis_scale_candidate_builder/`

Run the robust axis-scale fit and outlier rejection benchmark:

```powershell
python tools/benchmark/run_dre2_robust_axis_scale_fit_benchmark.py
```

The DR-E2 robust fit report is written to:

`benchmark/reports/dre2_robust_axis_scale_fit/`

Classify remaining missing anchors by crop coverage and OCR evidence:

```powershell
python tools/benchmark/run_dre3_missing_anchor_coverage_benchmark.py
```

The DR-E3 missing-anchor coverage report is written to:

`benchmark/reports/dre3_missing_anchor_coverage/`

Prototype repaired X label-band crops for remaining non-valid scale axes:

```powershell
python tools/benchmark/run_dre4_label_band_crop_coverage_repair.py
```

The DR-E4 crop coverage repair report and overlays are written to:

`benchmark/reports/dre4_label_band_crop_coverage_repair/`

Run repaired label-band crops through end-to-end robust calibration scoring:

```powershell
python tools/benchmark/run_dre5_repaired_crop_calibration_pipeline.py
```

The DR-E5 repaired crop calibration report is written to:

`benchmark/reports/dre5_repaired_crop_calibration_pipeline/`

Recover remaining partial calibration axes with targeted OCR fallback:

```powershell
python tools/benchmark/run_dre6_remaining_axis_ocr_recovery.py
```

The DR-E6 remaining-axis OCR recovery report and overlay are written to:

`benchmark/reports/dre6_remaining_axis_ocr_recovery/`
