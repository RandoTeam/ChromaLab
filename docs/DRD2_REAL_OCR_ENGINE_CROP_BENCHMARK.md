# DR-D2 Real OCR Engine Crop Benchmark

Status: `DR_D2_COMPLETE_RAPIDOCR_INSTALLED_CROP_OCR_WORKS_BUT_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-D2 installs and runs a real PC OCR candidate against DR-C4 text crops. This
tests whether actual OCR output can reproduce the DR-D1 text-role safety targets.

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## OCR Stack

Installed benchmark-only OCR packages:

```powershell
python -m pip install -r tools/benchmark/ocr-requirements.txt
```

Pinned packages:

- `rapidocr==3.8.1`
- `onnxruntime==1.26.0`

RapidOCR model download and loading were verified. The installed model files are
listed in:

`benchmark/reports/drd2_rapidocr_crop_benchmark/summary.json`

## Command

```powershell
python tools/benchmark/run_drd2_rapidocr_crop_benchmark.py
```

Output:

`benchmark/reports/drd2_rapidocr_crop_benchmark/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`RAPIDOCR_INSTALLED_CROP_OCR_WORKS_BUT_NOT_ACCEPTANCE_READY`

## Method Results

| Method | Non-empty OCR | Exact text | Mean similarity | Role accuracy | False tick labels | Forbidden numeric rejected |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_rec_padded_x3_v1` | 205/230 | 68/230 | 0.6178 | 120/230 | 4 | 12/16 |
| `rapidocr_rec_binary_x4_v1` | 187/230 | 52/230 | 0.4869 | 96/230 | 4 | 15/16 |

Timing is recorded in the generated `summary.json`/`summary.md` and may vary
slightly between runs.

## Safety Failures

RapidOCR produced forbidden false tick-label classifications on non-tick text:

- XIC legend text was sometimes read as a pure numeric string.
- Ion/m/z metadata in `bench_06` was sometimes reduced to a pure numeric string.
- Binary preprocessing made some `Abundance` axis titles look like single-digit
  numeric labels.

This is a real safety blocker. These outputs must not enter calibration as tick
labels.

## What This Proves

- RapidOCR is installed and callable on this machine.
- Recognition-only crop OCR is fast enough for PC benchmarking.
- Current crop/preprocessing variants are not accurate enough for acceptance.
- A forbidden numeric safety gate remains mandatory.

## What This Does Not Prove

- It does not prove Android OCR parity.
- It does not prove automatic crop generation from graph panels.
- It does not make semantic layout classification runtime-ready.
- It does not allow OCR/VLM to become numeric authority.

## Next Slice

Next slice completed:

`DR-D3: OCR Crop Preprocessing Grid And Safety Gate`

Result:

- documented in `docs/DRD3_OCR_PREPROCESSING_GRID_AND_SAFETY_GATE.md`;
- best RapidOCR preprocessing reached 0.5348 role accuracy;
- context safety gate reduced false tick labels to zero;
- OCR remains not acceptance-ready, so the next slice should compare a second
  OCR engine and text-box detection.
