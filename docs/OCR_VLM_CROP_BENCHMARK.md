# OCR/VLM Crop Benchmark

The Phase 6 benchmark harness compares OCR/VLM crop results against known crop expectations.

## Case Fields

- case id;
- crop path;
- crop kind;
- expected text;
- expected text class;
- notes.

## Observation Fields

- case id;
- source: ML Kit or VLM;
- raw text;
- normalized text;
- text class;
- confidence;
- duration;
- error code.

## Outputs

- JSON report with schema `ocr-vlm-crop-benchmark-1.0`;
- Markdown report with case table;
- character error rate per OCR source;
- class match flags;
- disagreement flag;
- final accepted semantic result.

## Acceptance Rule

The harness accepts a crop result only when text class matches and character error rate is within threshold. Accepted crop text remains semantic evidence; it does not become final RT, calibration, area, height, S/N, or peak metric.

