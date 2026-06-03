# DR-D3 OCR Preprocessing Grid And Safety Gate

Status: `DR_D3_COMPLETE_SAFETY_GATE_REMOVES_FALSE_TICK_LABELS_OCR_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-D3 tests whether a small RapidOCR preprocessing grid can improve OCR text-role
quality and whether a strict context safety gate can remove forbidden numeric
tick-label failures.

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Command

```powershell
python tools/benchmark/run_drd3_ocr_preprocessing_grid.py
```

Output:

`benchmark/reports/drd3_ocr_preprocessing_grid/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`SAFETY_GATE_REMOVES_FALSE_TICK_LABELS_OCR_NOT_ACCEPTANCE_READY`

## Best Results

| Mode | Variant | Role accuracy | False tick labels | Mean similarity | Forbidden numeric rejected |
| --- | --- | ---: | ---: | ---: | ---: |
| raw classifier | `rapidocr_autocontrast_p6_s3_v1` | 0.5348 | 2 | 0.6114 | 14/16 |
| safe context gate | `rapidocr_autocontrast_p6_s3_v1` | 0.5348 | 0 | 0.6114 | 16/16 |

## What Improved

- Best raw preprocessing improved from DR-D2 `120/230` role pass to `123/230`.
- False tick labels dropped from DR-D2 best `4` to DR-D3 raw best `2`.
- The context safety gate reduced false tick labels from `2` to `0`.
- Forbidden numeric rejection reached `16/16` under the safe context gate.

## What Did Not Improve Enough

- Role accuracy remains only `0.5348`.
- Exact text match remains too low for acceptance.
- RapidOCR still misreads some XIC/ion/axis-title regions as numeric strings.
- Safety gate prevents dangerous calibration input, but it does not recover the
  lost semantic information.

## Safety Gate Rule

The benchmark safety gate is:

```text
Pure numeric OCR may become tick_label only inside a tick-label candidate context.
Pure numeric OCR from non-tick context is rejected until deterministic geometry
proves tick ownership.
```

In DR-D3 this context is derived from DR-C4 annotation labels, so it is an
upper-bound safety test. Runtime still needs automatic graph/axis/tick context.

## Next Slice

Next slice completed:

`DR-D4: Second OCR Engine And Text-Box Detection Benchmark`

Result:

- documented in `docs/DRD4_SECOND_OCR_ENGINE_TEXT_BOX_BENCHMARK.md`;
- EasyOCR 1.7.2 was installed and verified as a second OCR benchmark engine;
- full-image text-box detection was compared against RapidOCR;
- result remains not acceptance-ready, so OCR must be constrained by
  deterministic graph/axis/label-band crop ownership before Android runtime
  integration.
