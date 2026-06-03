# DR-D3 OCR Preprocessing Grid And Safety Gate

Verdict: `SAFETY_GATE_REMOVES_FALSE_TICK_LABELS_OCR_NOT_ACCEPTANCE_READY`
Text labels: `230`

## Best Variants

| Mode | Variant | Role accuracy | False tick labels | Mean similarity | Forbidden numeric rejected |
| --- | --- | ---: | ---: | ---: | ---: |
| `raw_role_classifier` | `rapidocr_autocontrast_p6_s3_v1` | 0.5348 | 2 | 0.6114 | 14/16 |
| `safe_context_gate` | `rapidocr_autocontrast_p6_s3_v1` | 0.5348 | 0 | 0.6114 | 16/16 |

## Variant Summary

| Mode | Variant | Exact text | Mean similarity | Role accuracy | False tick labels | Mean time/label |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `raw_role_classifier` | `rapidocr_rgb_p2_s2_v1` | 73/230 | 0.5882 | 0.5261 | 4 | 0.0413s |
| `raw_role_classifier` | `rapidocr_rgb_p6_s3_v1` | 68/230 | 0.6178 | 0.5217 | 4 | 0.0391s |
| `raw_role_classifier` | `rapidocr_rgb_p10_s4_v1` | 60/230 | 0.5944 | 0.5261 | 3 | 0.0334s |
| `raw_role_classifier` | `rapidocr_autocontrast_p6_s3_v1` | 66/230 | 0.6114 | 0.5348 | 2 | 0.0359s |
| `raw_role_classifier` | `rapidocr_sharp_autocontrast_p6_s3_v1` | 65/230 | 0.5977 | 0.5304 | 3 | 0.0349s |
| `raw_role_classifier` | `rapidocr_binary55_p6_s3_v1` | 54/230 | 0.5001 | 0.4000 | 5 | 0.0361s |
| `raw_role_classifier` | `rapidocr_binary65_p8_s4_v1` | 52/230 | 0.4391 | 0.4174 | 5 | 0.0342s |
| `safe_context_gate` | `rapidocr_rgb_p2_s2_v1` | 73/230 | 0.5882 | 0.5261 | 0 | 0.0413s |
| `safe_context_gate` | `rapidocr_rgb_p6_s3_v1` | 68/230 | 0.6178 | 0.5217 | 0 | 0.0391s |
| `safe_context_gate` | `rapidocr_rgb_p10_s4_v1` | 60/230 | 0.5944 | 0.5261 | 0 | 0.0334s |
| `safe_context_gate` | `rapidocr_autocontrast_p6_s3_v1` | 66/230 | 0.6114 | 0.5348 | 0 | 0.0359s |
| `safe_context_gate` | `rapidocr_sharp_autocontrast_p6_s3_v1` | 65/230 | 0.5977 | 0.5304 | 0 | 0.0349s |
| `safe_context_gate` | `rapidocr_binary55_p6_s3_v1` | 54/230 | 0.5001 | 0.4000 | 0 | 0.0361s |
| `safe_context_gate` | `rapidocr_binary65_p8_s4_v1` | 52/230 | 0.4391 | 0.4174 | 0 | 0.0342s |

## Safety Gate Effect

Raw best false tick labels: `2`
Safe best false tick labels: `0`

Rejected examples:
- `bench_04_stacked_xic_resolution` `legend` `XIC(198,0315±0,002)` -> OCR `134036013801`
- `bench_04_stacked_xic_resolution` `legend` `XIC(198,0315±0,0002)` -> OCR `13401360138014`

## Interpretation

- Preprocessing grid did not make RapidOCR acceptance-ready on P0 crops.
- Context safety gate removes false tick-label safety failures, but role accuracy remains too low.
- OCR must improve before semantic layout classifier can be trusted in Android runtime.
- Next work should compare a second OCR engine and add real text-box detection/crop generation.
