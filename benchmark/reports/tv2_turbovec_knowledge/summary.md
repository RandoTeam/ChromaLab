# TV-2 TurboVec Knowledge Retrieval Benchmark

- Verdict: `TV2_READY_FOR_TV3_AB_EVALUATION`
- Next step: Proceed to TV-3 A/B evaluation with the passing dense profile(s).
- Pack: `docs/knowledge/chromalab_knowledge_seed_v2.json`
- Pack version: `chromalab-knowledge-v2`
- k: `8`

## Backend Summary

| Backend | Status | Hits | Top-1 hits | Improvements | Regressions | Safety regressions |
|---|---|---:|---:|---:|---:|---:|
| lexical_bm25 | PASS | 9/10 | 7/10 | 0 | 0 | 0 |
| minilm | PASS | 9/10 | 7/10 | 2 | 2 | 0 |
| bge_base | PASS | 10/10 | 6/10 | 2 | 2 | 0 |

## Query Cases

### sn_signal_to_noise

- Query: `S/N signal to noise`
- Expected: `kp2-term-sn`
- Lexical top ids: `kp2-term-noise, kp2-term-sn, kp2-term-rms-noise, kp2-term-intensity, kp2-ion-channel, kp2-rule-peak-annotation-signal-verified, kp2-caveat-calibration-required, kp2-safety-knowledge-cannot-measure`
- minilm top ids: `kp2-term-sn, kp2-term-noise, kp2-term-rms-noise, kp2-snippet-sparse-trace-warning, kp2-term-peak, kp2-compound-stub-n-c28-alkane, kp2-ms-sim, kp2-axis-title`
- bge_base top ids: `kp2-term-sn, kp2-term-noise, kp2-term-rms-noise, kp2-rule-peak-annotation-signal-verified, kp2-ion-channel, kp2-snippet-roi-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-peak-overlap-warning`

### ion71_title_channel

- Query: `Ion 71.00 (70.70 to 71.70) text classification`
- Expected: `kp2-rule-ion-title-not-peak`
- Lexical top ids: `kp2-rule-ion-title-not-peak, kp2-ion-channel, kp2-ion-mass-range, kp2-rule-mass-range-not-peak-rt, kp2-ion-ion, kp2-rule-tick-label-geometry, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak`
- minilm top ids: `kp2-ion-channel, kp2-rule-ion-title-not-peak, kp2-ion-ion, kp2-rule-mass-range-not-peak-rt, kp2-ion-mass-range, kp2-ms-mz, kp2-rule-mz-not-rt, kp2-ms-tic`
- bge_base top ids: `kp2-ion-channel, kp2-rule-ion-title-not-peak, kp2-rule-mass-range-not-peak-rt, kp2-ion-mass-range, kp2-ion-ion, kp2-rule-legend-not-peak, kp2-rule-mz-not-rt, kp2-rule-title-header-not-peak`

### sim_channel

- Query: `SIM selected ion monitoring channel`
- Expected: `kp2-ms-sim, kp2-ms-selected-ion-monitoring`
- Lexical top ids: `kp2-ms-selected-ion-monitoring, kp2-ms-sim, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ion-mass-range, kp2-ms-tic, kp2-ms-extracted-ion-chromatogram`
- minilm top ids: `kp2-ms-sim, kp2-ms-selected-ion-monitoring, kp2-ion-channel, kp2-ion-ion, kp2-rule-ion-title-not-peak, kp2-ion-mass-range, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic`
- bge_base top ids: `kp2-ms-sim, kp2-ms-selected-ion-monitoring, kp2-ms-extracted-ion-chromatogram, kp2-ion-channel, kp2-ion-ion, kp2-ms-eic, kp2-ms-tic, kp2-rule-ion-title-not-peak`

### peak_label_signal_verification

- Query: `peak label requires signal verification`
- Expected: `kp2-rule-peak-annotation-signal-verified`
- Lexical top ids: `kp2-rule-peak-annotation-signal-verified, kp2-axis-axis-label, kp2-rule-title-header-not-peak, kp2-rule-tick-label-geometry, kp2-rule-axis-label-not-peak, kp2-term-sn, kp2-term-shoulder-peak, kp2-axis-legend`
- minilm top ids: `kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-axis-axis-label, kp2-rule-legend-not-peak, kp2-rule-title-header-not-peak, kp2-axis-legend, kp2-term-peak, kp2-class-biomarker`
- bge_base top ids: `kp2-rule-peak-annotation-signal-verified, kp2-term-peak, kp2-caveat-review-grade-peaks, kp2-rule-axis-label-not-peak, kp2-rule-legend-not-peak, kp2-term-overlap, kp2-rule-ion-title-not-peak, kp2-rule-title-header-not-peak`

### kovats_without_reference

- Query: `Kovats without reference series`
- Expected: `kp2-caveat-no-kovats-without-reference`
- Lexical top ids: `kp2-caveat-no-kovats-without-reference, kp2-ri-n-alkane-reference, kp2-ri-kovats-index, kp2-ri-retention-index, kp2-axis-legend, kp2-ri-missing-reference-caveat, kp2-safety-knowledge-cannot-measure, kp2-class-alkane`
- minilm top ids: `kp2-caveat-no-kovats-without-reference, kp2-ri-missing-reference-caveat, kp2-ri-kovats-index, kp2-ri-n-alkane-reference, kp2-ri-retention-index, kp2-compound-stub-n-c28-alkane, kp2-compound-stub-n-c27-alkane, kp2-compound-stub-n-c24-alkane`
- bge_base top ids: `kp2-caveat-no-kovats-without-reference, kp2-ri-kovats-index, kp2-ri-missing-reference-caveat, kp2-ri-n-alkane-reference, kp2-ri-retention-index, kp2-caveat-no-compound-assignment, kp2-axis-title, kp2-axis-legend`

### compound_without_evidence

- Query: `compound assignment without explicit evidence`
- Expected: `kp2-caveat-no-compound-assignment`
- Lexical top ids: `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-class-alkane, kp2-class-biomarker, kp2-class-hydrocarbon, kp2-class-n-alkane, kp2-class-pah, kp2-class-sterane`
- minilm top ids: `kp2-caveat-no-compound-assignment, kp2-term-abundance, kp2-class-compound-class, kp2-term-coelution, kp2-term-tailing-asymmetry, kp2-class-unresolved-complex-mixture, kp2-class-terpane, kp2-compound-stub-n-c12-alkane`
- bge_base top ids: `kp2-caveat-no-compound-assignment, kp2-class-compound-class, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-diagnostic-only-report-explanation, kp2-class-unresolved-complex-mixture, kp2-snippet-sparse-trace-warning, kp2-axis-title, kp2-snippet-roi-warning`

### knowledge_cannot_create_metrics

- Query: `knowledge cannot create numeric metrics`
- Expected: `kp2-safety-knowledge-cannot-measure`
- Lexical top ids: `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-term-peak-area, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-rule-ion-title-not-peak, kp2-snippet-roi-warning, kp2-snippet-calibration-invalid-warning`
- minilm top ids: `kp2-safety-knowledge-cannot-measure, kp2-caveat-trace-required, kp2-snippet-calibration-invalid-warning, kp2-snippet-vlm-disagreement-warning, kp2-snippet-roi-warning, kp2-term-peak-area, kp2-ri-kovats-index, kp2-snippet-ocr-ambiguity-warning`
- bge_base top ids: `kp2-safety-knowledge-cannot-measure, kp2-caveat-vlm-semantic-only, kp2-snippet-vlm-disagreement-warning, kp2-caveat-trace-required, kp2-snippet-calibration-invalid-warning, kp2-ri-kovats-index, kp2-rule-legend-not-peak, kp2-snippet-roi-warning`

### calibration_warning

- Query: `calibration invalid warning missing anchors residual checks`
- Expected: `kp2-snippet-calibration-invalid-warning, kp2-caveat-calibration-required`
- Lexical top ids: `kp2-snippet-calibration-invalid-warning, kp2-ri-missing-reference-caveat, kp2-snippet-roi-warning, kp2-snippet-ocr-ambiguity-warning, kp2-snippet-peak-overlap-warning, kp2-snippet-shoulder-peak-warning, kp2-snippet-sparse-trace-warning, kp2-snippet-vlm-disagreement-warning`
- minilm top ids: `kp2-axis-calibration-anchor, kp2-snippet-calibration-invalid-warning, kp2-rule-tick-label-geometry, kp2-axis-tick-label, kp2-caveat-calibration-required, kp2-snippet-sparse-trace-warning, kp2-ri-interpolation-caveat, kp2-snippet-shoulder-peak-warning`
- bge_base top ids: `kp2-axis-calibration-anchor, kp2-snippet-calibration-invalid-warning, kp2-caveat-calibration-required, kp2-ri-missing-reference-caveat, kp2-rule-tick-label-geometry, kp2-caveat-no-kovats-without-reference, kp2-caveat-image-uncertainty, kp2-axis-tick-label`

### ocr_ambiguity_warning

- Query: `ambiguous OCR crop provenance uncertain text into metrics`
- Expected: `kp2-snippet-ocr-ambiguity-warning`
- Lexical top ids: `kp2-rule-legend-not-peak, kp2-snippet-ocr-ambiguity-warning, kp2-rule-ion-title-not-peak, kp2-rule-tick-label-geometry, kp2-rule-title-header-not-peak, kp2-rule-peak-annotation-signal-verified, kp2-rule-axis-label-not-peak, kp2-rule-mass-range-not-peak-rt`
- minilm top ids: `kp2-snippet-ocr-ambiguity-warning, kp2-axis-calibration-anchor, kp2-caveat-image-uncertainty, kp2-term-abundance, kp2-rule-axis-label-not-peak, kp2-term-resolution, kp2-term-intensity, kp2-ms-scan`
- bge_base top ids: `kp2-caveat-image-uncertainty, kp2-snippet-ocr-ambiguity-warning, kp2-rule-peak-annotation-signal-verified, kp2-rule-legend-not-peak, kp2-caveat-vlm-semantic-only, kp2-rule-title-header-not-peak, kp2-axis-title, kp2-axis-calibration-anchor`

### photo_alone_cannot_identify_compound

- Query: `Can the app identify a compound from a chromatogram photo alone?`
- Expected: `kp2-caveat-no-compound-assignment`
- Lexical top ids: `kp2-ms-eic, kp2-ms-extracted-ion-chromatogram, kp2-ri-n-alkane-reference, kp2-term-chromatogram, kp2-ms-tic, kp2-class-compound-class, kp2-ms-selected-ion-monitoring, kp2-term-coelution`
- minilm top ids: `kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-ms-extracted-ion-chromatogram, kp2-ms-eic, kp2-term-abundance, kp2-term-peak, kp2-term-coelution, kp2-term-resolution`
- bge_base top ids: `kp2-ms-extracted-ion-chromatogram, kp2-caveat-no-compound-assignment, kp2-ms-sim, kp2-term-abundance, kp2-caveat-image-uncertainty, kp2-term-chromatogram, kp2-class-compound-class, kp2-rule-peak-annotation-signal-verified`
