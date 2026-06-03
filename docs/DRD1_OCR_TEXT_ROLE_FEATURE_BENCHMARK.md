# DR-D1 OCR Text-Role Feature Benchmark

Status: `DR_D1_COMPLETE_TEXT_ROLE_RULES_WORK_WITH_PERFECT_TEXT_REAL_OCR_NOT_AVAILABLE`

Date: 2026-06-03

## Purpose

DR-D1 defines and scores the OCR/text-role features needed after DR-C7. The
target is not a final OCR engine yet. The target is a measurable safety contract
for text roles:

- tick labels;
- ion/m/z metadata;
- TIC chart title;
- XIC/legend text;
- axis titles;
- other non-tick numeric text.

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Command

```powershell
python tools/benchmark/prototype_drd1_ocr_text_role_features.py
```

Output:

`benchmark/reports/drd1_ocr_text_role_feature_benchmark/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`TEXT_ROLE_RULES_WORK_WITH_PERFECT_TEXT_REAL_OCR_NOT_AVAILABLE`

Local OCR availability in the current Python environment:

| Engine | Available |
| --- | --- |
| `pytesseract` | false |
| `easyocr` | false |
| `paddleocr` | false |
| `rapidocr_onnxruntime` | false |
| `onnxruntime` | false |
| `cv2` | false |
| `tesseract` executable | false |

## Method Results

| Method | Role accuracy | Correct labels | False tick labels | Forbidden numeric rejected | Semantic feature pass |
| --- | ---: | ---: | ---: | ---: | ---: |
| `no_ocr_available_v1` | 0.0000 | 0/230 | 0 | 1.0000 | 11/16 |
| `regex_on_perfect_text_v1` | 0.9435 | 217/230 | 0 | 1.0000 | 16/16 |
| `annotation_role_oracle_v1` | 1.0000 | 230/230 | 0 | 1.0000 | 16/16 |

## What This Proves

- The current local benchmark environment has no real OCR engine.
- If text is read correctly, simple role rules can safely separate tick labels
  from Ion/TIC/XIC/legend numeric text.
- `regex_on_perfect_text_v1` has zero false tick labels on the 16 annotated
  forbidden numeric text cases.
- The immediate blocker is real OCR text and text-box extraction, not the
  high-level text-role rules.

## What This Does Not Prove

- It does not prove ML Kit, RapidOCR, PaddleOCR, Tesseract, E2B, or any Android
  OCR path can read the P0 crops.
- It does not prove text boxes can be paired to graph panels.
- It does not make any fixture release-ready.
- It does not allow VLM/E2B to become numeric authority.

## Runtime Capability Gap

The next slice needs an actual OCR candidate and crop pipeline that can produce:

- text string;
- text bounding box;
- confidence;
- panel/graph ownership;
- role classification;
- forbidden numeric rejection reason.

## Next Slice

Recommended next slice:

`DR-D2: Real OCR Engine Installation And Crop OCR Benchmark`

Goal:

- install or integrate one real PC OCR candidate;
- run it on DR-C4 label/text crops;
- score OCR text, box, and role accuracy against the same DR-D1 targets;
- keep E2B/VLM advisory only.
