# TV-3 TurboVec Retrieval A/B Arbitration Benchmark

- Verdict: `TV3_READY_FOR_TV4_BACKEND_PROMOTION_CANDIDATE`
- Selected policy: `HYBRID_UNION_RRF`
- Next step: Proceed to TV-4 backend abstraction/promotion candidate using the selected policy as the benchmark target.
- Pack: `docs/knowledge/chromalab_knowledge_seed_v2.json`
- Pack version: `chromalab-knowledge-v2`
- Pack hash: `99a5343dc303492ee842e3856d87053040a5f1087d3f7e58c8b38c83023da8f7`
- k: `8`

## Policy Summary

| Policy | Status | Hits | Top-1 | Improvements | Regressions | Semantic miss recoveries | Required misses | Safety misses | Safety regressions | Exact top-1 regressions |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| LEXICAL_ONLY | PASS | 9/10 | 7/10 | 0 | 0 | 0 | 1 | 1 | 0 | 0 |
| MINILM_ONLY | REVIEW | 9/10 | 7/10 | 2 | 2 | 0 | 1 | 1 | 2 | 1 |
| BGE_ONLY | REVIEW | 10/10 | 6/10 | 2 | 2 | 1 | 0 | 0 | 1 | 1 |
| HYBRID_LEXICAL_GUARD_BGE | PASS | 10/10 | 7/10 | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| HYBRID_LEXICAL_GUARD_MINILM | PASS | 9/10 | 7/10 | 0 | 0 | 0 | 1 | 1 | 0 | 0 |
| HYBRID_UNION_RRF | PASS | 10/10 | 9/10 | 3 | 0 | 1 | 0 | 0 | 0 | 0 |

## Query Decisions

### sn_signal_to_noise

- Class: `exact_rule`
- Safety critical: `False`
- Required: `kp2-term-sn`
- LEXICAL_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-term-noise, kp2-term-sn, kp2-term-rms-noise, kp2-term-intensity, kp2-ion-channel, kp2-rule-peak-annotation-signal-verified, kp2-caveat-calibration-required, kp2-safety-knowledge-cannot-measure`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-term-sn, kp2-term-noise, kp2-term-rms-noise, kp2-snippet-sparse-trace-warning, kp2-term-peak, kp2-compound-stub-n-c28-alkane, kp2-ms-sim, kp2-axis-title`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-term-sn, kp2-term-noise, kp2-term-rms-noise, kp2-rule-peak-annotation-signal-verified, kp2-ion-channel, kp2-snippet-roi-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-peak-overlap-warning`
- HYBRID_LEXICAL_GUARD_BGE: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-term-noise, kp2-term-sn, kp2-term-rms-noise, kp2-term-intensity, kp2-ion-channel, kp2-rule-peak-annotation-signal-verified, kp2-caveat-calibration-required, kp2-safety-knowledge-cannot-measure`
- HYBRID_LEXICAL_GUARD_MINILM: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-term-noise, kp2-term-sn, kp2-term-rms-noise, kp2-term-intensity, kp2-ion-channel, kp2-rule-peak-annotation-signal-verified, kp2-caveat-calibration-required, kp2-safety-knowledge-cannot-measure`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-term-sn, kp2-term-noise, kp2-term-rms-noise, kp2-rule-peak-annotation-signal-verified, kp2-ion-channel, kp2-snippet-sparse-trace-warning, kp2-term-intensity, kp2-term-peak`

### ion71_title_channel

- Class: `exact_rule`
- Safety critical: `True`
- Required: `kp2-rule-ion-title-not-peak`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-ion-title-not-peak, kp2-ion-channel, kp2-ion-mass-range, kp2-rule-mass-range-not-peak-rt, kp2-ion-ion, kp2-rule-tick-label-geometry, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak`
- MINILM_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-ion-channel, kp2-rule-ion-title-not-peak, kp2-ion-ion, kp2-rule-mass-range-not-peak-rt, kp2-ion-mass-range, kp2-ms-mz, kp2-rule-mz-not-rt, kp2-ms-tic`
- BGE_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-ion-channel, kp2-rule-ion-title-not-peak, kp2-rule-mass-range-not-peak-rt, kp2-ion-mass-range, kp2-ion-ion, kp2-rule-legend-not-peak, kp2-rule-mz-not-rt, kp2-rule-title-header-not-peak`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-ion-title-not-peak, kp2-ion-channel, kp2-ion-mass-range, kp2-rule-mass-range-not-peak-rt, kp2-ion-ion, kp2-rule-tick-label-geometry, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-ion-title-not-peak, kp2-ion-channel, kp2-ion-mass-range, kp2-rule-mass-range-not-peak-rt, kp2-ion-ion, kp2-rule-tick-label-geometry, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-ion-title-not-peak, kp2-ion-channel, kp2-rule-mass-range-not-peak-rt, kp2-ion-mass-range, kp2-ion-ion, kp2-rule-legend-not-peak, kp2-rule-mz-not-rt, kp2-rule-title-header-not-peak`

### sim_channel

- Class: `exact_rule`
- Safety critical: `False`
- Required: `kp2-ms-sim`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-selected-ion-monitoring, kp2-ms-sim, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ion-mass-range, kp2-ms-tic, kp2-ms-extracted-ion-chromatogram`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-sim, kp2-ms-selected-ion-monitoring, kp2-ion-channel, kp2-ion-ion, kp2-rule-ion-title-not-peak, kp2-ion-mass-range, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-sim, kp2-ms-selected-ion-monitoring, kp2-ms-extracted-ion-chromatogram, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ms-tic, kp2-rule-ion-title-not-peak`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-selected-ion-monitoring, kp2-ms-sim, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ion-mass-range, kp2-ms-tic, kp2-ms-extracted-ion-chromatogram`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-selected-ion-monitoring, kp2-ms-sim, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ion-mass-range, kp2-ms-tic, kp2-ms-extracted-ion-chromatogram`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-ms-sim, kp2-ms-selected-ion-monitoring, kp2-ion-channel, kp2-ion-ion, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic, kp2-ion-mass-range, kp2-rule-ion-title-not-peak`

### peak_label_signal_verification

- Class: `exact_rule`
- Safety critical: `True`
- Required: `kp2-rule-peak-annotation-signal-verified`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-axis-axis-label, kp2-rule-title-header-not-peak, kp2-rule-tick-label-geometry, kp2-rule-axis-label-not-peak, kp2-term-sn, kp2-term-shoulder-peak, kp2-axis-legend`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-axis-axis-label, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak, kp2-axis-legend, kp2-term-peak, kp2-class-biomarker`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-term-peak, kp2-caveat-review-grade-peaks, kp2-rule-axis-label-not-peak, kp2-rule-legend-not-peak, kp2-term-overlap, kp2-rule-ion-title-not-peak, kp2-rule-title-header-not-peak`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-axis-axis-label, kp2-rule-title-header-not-peak, kp2-rule-tick-label-geometry, kp2-rule-axis-label-not-peak, kp2-term-sn, kp2-term-shoulder-peak, kp2-axis-legend`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-axis-axis-label, kp2-rule-title-header-not-peak, kp2-rule-tick-label-geometry, kp2-rule-axis-label-not-peak, kp2-term-sn, kp2-term-shoulder-peak, kp2-axis-legend`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-rule-title-header-not-peak, kp2-axis-axis-label, kp2-term-peak, kp2-rule-legend-not-peak, kp2-axis-legend, kp2-caveat-review-grade-peaks`

### kovats_without_reference

- Class: `safety_boundary`
- Safety critical: `True`
- Required: `kp2-caveat-no-kovats-without-reference`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-n-alkane-reference, kp2-ri-kovats-index, kp2-ri-retention-index, kp2-axis-legend, kp2-ri-missing-reference-caveat, kp2-safety-knowledge-cannot-measure, kp2-class-alkane`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-missing-reference-caveat, kp2-ri-kovats-index, kp2-ri-n-alkane-reference, kp2-ri-retention-index, kp2-compound-stub-n-c28-alkane, kp2-compound-stub-n-c27-alkane, kp2-compound-stub-n-c24-alkane`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-kovats-index, kp2-ri-missing-reference-caveat, kp2-ri-n-alkane-reference, kp2-ri-retention-index, kp2-caveat-no-compound-assignment, kp2-axis-title, kp2-axis-legend`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-n-alkane-reference, kp2-ri-kovats-index, kp2-ri-retention-index, kp2-axis-legend, kp2-ri-missing-reference-caveat, kp2-safety-knowledge-cannot-measure, kp2-class-alkane`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-n-alkane-reference, kp2-ri-kovats-index, kp2-ri-retention-index, kp2-axis-legend, kp2-ri-missing-reference-caveat, kp2-safety-knowledge-cannot-measure, kp2-class-alkane`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-kovats-without-reference, kp2-ri-kovats-index, kp2-ri-n-alkane-reference, kp2-ri-missing-reference-caveat, kp2-ri-retention-index, kp2-axis-legend, kp2-caveat-no-compound-assignment, kp2-compound-stub-n-c28-alkane`

### compound_without_evidence

- Class: `safety_boundary`
- Safety critical: `True`
- Required: `kp2-caveat-no-compound-assignment`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-class-alkane, kp2-class-biomarker, kp2-class-hydrocarbon, kp2-class-n-alkane, kp2-class-pah, kp2-class-sterane`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `kp2-compound-stub-n-c12-alkane`, top ids `kp2-caveat-no-compound-assignment, kp2-term-abundance, kp2-class-compound-class, kp2-term-coelution, kp2-term-tailing-asymmetry, kp2-class-unresolved-complex-mixture, kp2-class-terpane, kp2-compound-stub-n-c12-alkane`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-diagnostic-only-report-explanation, kp2-class-unresolved-complex-mixture, kp2-snippet-sparse-trace-warning, kp2-axis-title, kp2-snippet-roi-warning`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-class-alkane, kp2-class-biomarker, kp2-class-hydrocarbon, kp2-class-n-alkane, kp2-class-pah, kp2-class-sterane`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-class-alkane, kp2-class-biomarker, kp2-class-hydrocarbon, kp2-class-n-alkane, kp2-class-pah, kp2-class-sterane`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-class-unresolved-complex-mixture, kp2-term-abundance, kp2-class-alkane, kp2-snippet-ocr-ambiguity-warning, kp2-class-biomarker, kp2-snippet-diagnostic-only-report-explanation`

### knowledge_cannot_create_metrics

- Class: `safety_boundary`
- Safety critical: `True`
- Required: `kp2-safety-knowledge-cannot-measure`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-term-peak-area, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-rule-ion-title-not-peak, kp2-snippet-roi-warning, kp2-snippet-calibration-invalid-warning`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-snippet-calibration-invalid-warning, kp2-snippet-vlm-disagreement-warning, kp2-snippet-roi-warning, kp2-term-peak-area, kp2-ri-kovats-index, kp2-snippet-ocr-ambiguity-warning`
- BGE_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-vlm-semantic-only, kp2-snippet-vlm-disagreement-warning, kp2-caveat-trace-required, kp2-snippet-calibration-invalid-warning, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-snippet-roi-warning`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-term-peak-area, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-rule-ion-title-not-peak, kp2-snippet-roi-warning, kp2-snippet-calibration-invalid-warning`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-term-peak-area, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-rule-ion-title-not-peak, kp2-snippet-roi-warning, kp2-snippet-calibration-invalid-warning`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-snippet-calibration-invalid-warning, kp2-ri-kovats-index, kp2-snippet-roi-warning, kp2-snippet-vlm-disagreement-warning, kp2-term-peak-area, kp2-rule-legend-not-peak`

### calibration_warning

- Class: `warning_explanation`
- Safety critical: `True`
- Required: `kp2-snippet-calibration-invalid-warning`
- LEXICAL_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-calibration-invalid-warning, kp2-ri-missing-reference-caveat, kp2-snippet-roi-warning, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-peak-overlap-warning, kp2-snippet-shoulder-peak-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-vlm-disagreement-warning`
- MINILM_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-axis-calibration-anchor, kp2-snippet-calibration-invalid-warning, kp2-rule-tick-label-geometry, kp2-axis-tick-label, kp2-caveat-calibration-required, kp2-snippet-sparse-trace-warning, kp2-ri-interpolation-caveat, kp2-snippet-shoulder-peak-warning`
- BGE_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-axis-calibration-anchor, kp2-snippet-calibration-invalid-warning, kp2-caveat-calibration-required, kp2-ri-missing-reference-caveat, kp2-rule-tick-label-geometry, kp2-caveat-no-kovats-without-reference, kp2-caveat-image-uncertainty, kp2-axis-tick-label`
- HYBRID_LEXICAL_GUARD_BGE: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-calibration-invalid-warning, kp2-ri-missing-reference-caveat, kp2-snippet-roi-warning, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-peak-overlap-warning, kp2-snippet-shoulder-peak-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-vlm-disagreement-warning`
- HYBRID_LEXICAL_GUARD_MINILM: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-calibration-invalid-warning, kp2-ri-missing-reference-caveat, kp2-snippet-roi-warning, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-peak-overlap-warning, kp2-snippet-shoulder-peak-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-vlm-disagreement-warning`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-calibration-invalid-warning, kp2-axis-calibration-anchor, kp2-ri-missing-reference-caveat, kp2-caveat-calibration-required, kp2-rule-tick-label-geometry, kp2-axis-tick-label, kp2-snippet-sparse-trace-warning, kp2-snippet-shoulder-peak-warning`

### ocr_ambiguity_warning

- Class: `warning_explanation`
- Safety critical: `True`
- Required: `kp2-snippet-ocr-ambiguity-warning`
- LEXICAL_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-rule-legend-not-peak, kp2-snippet-ocr-ambiguity-warning, kp2-rule-ion-title-not-peak, kp2-rule-tick-label-geometry, kp2-rule-title-header-not-peak, kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-rule-mass-range-not-peak-rt`
- MINILM_ONLY: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-ocr-ambiguity-warning, kp2-axis-calibration-anchor, kp2-caveat-image-uncertainty, kp2-term-abundance, kp2-rule-axis-label-not-peak, kp2-term-resolution, kp2-term-intensity, kp2-ms-scan`
- BGE_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-caveat-image-uncertainty, kp2-snippet-ocr-ambiguity-warning, kp2-rule-peak-annotation-signal-verified, kp2-rule-legend-not-peak, kp2-caveat-vlm-semantic-only, kp2-rule-title-header-not-peak, kp2-axis-title, kp2-axis-calibration-anchor`
- HYBRID_LEXICAL_GUARD_BGE: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-rule-legend-not-peak, kp2-snippet-ocr-ambiguity-warning, kp2-rule-ion-title-not-peak, kp2-rule-tick-label-geometry, kp2-rule-title-header-not-peak, kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-rule-mass-range-not-peak-rt`
- HYBRID_LEXICAL_GUARD_MINILM: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-rule-legend-not-peak, kp2-snippet-ocr-ambiguity-warning, kp2-rule-ion-title-not-peak, kp2-rule-tick-label-geometry, kp2-rule-title-header-not-peak, kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-rule-mass-range-not-peak-rt`
- HYBRID_UNION_RRF: rank `1`, top1 `True`, missing `none`, forbidden `none`, top ids `kp2-snippet-ocr-ambiguity-warning, kp2-caveat-image-uncertainty, kp2-rule-legend-not-peak, kp2-rule-peak-annotation-signal-verified, kp2-axis-calibration-anchor, kp2-rule-title-header-not-peak, kp2-rule-axis-label-not-peak, kp2-rule-ion-title-not-peak`

### photo_alone_cannot_identify_compound

- Class: `natural_language`
- Safety critical: `True`
- Required: `kp2-caveat-no-compound-assignment`
- LEXICAL_ONLY: rank `None`, top1 `False`, missing `kp2-caveat-no-compound-assignment`, forbidden `none`, top ids `kp2-ms-eic, kp2-ms-extracted-ion-chromatogram, kp2-ri-n-alkane-reference, kp2-term-chromatogram, kp2-ms-tic, kp2-class-compound-class, kp2-ms-selected-ion-monitoring, kp2-term-coelution`
- MINILM_ONLY: rank `None`, top1 `False`, missing `kp2-caveat-no-compound-assignment`, forbidden `none`, top ids `kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic, kp2-term-abundance, kp2-term-peak, kp2-term-coelution, kp2-term-resolution`
- BGE_ONLY: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-ms-extracted-ion-chromatogram, kp2-caveat-no-compound-assignment, kp2-ms-sim, kp2-term-abundance, kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-class-compound-class, kp2-rule-peak-annotation-signal-verified`
- HYBRID_LEXICAL_GUARD_BGE: rank `2`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-ms-extracted-ion-chromatogram, kp2-caveat-no-compound-assignment, kp2-ms-sim, kp2-term-abundance, kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-class-compound-class, kp2-rule-peak-annotation-signal-verified`
- HYBRID_LEXICAL_GUARD_MINILM: rank `None`, top1 `False`, missing `kp2-caveat-no-compound-assignment`, forbidden `none`, top ids `kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic, kp2-term-abundance, kp2-term-peak, kp2-term-coelution, kp2-term-resolution`
- HYBRID_UNION_RRF: rank `8`, top1 `False`, missing `none`, forbidden `none`, top ids `kp2-ms-extracted-ion-chromatogram, kp2-term-chromatogram, kp2-ms-eic, kp2-caveat-image-uncertainty, kp2-term-abundance, kp2-class-compound-class, kp2-term-coelution, kp2-caveat-no-compound-assignment`
