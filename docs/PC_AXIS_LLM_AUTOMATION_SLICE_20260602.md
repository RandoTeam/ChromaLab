# PC Axis LLM Automation Slice

Date: 2026-06-02

Status: focused PC automation improvement complete. This is not Phase 9 acceptance.

## Goal

Continue the PC-first chromatogram analysis work toward fully autonomous operation:
the user supplies a photo, screenshot, or gallery image, and ChromaLab performs graph
detection, axis calibration, trace extraction, peak detection, and reporting without
manual calibration.

This slice only improves reproducible axis/OCR evidence handling on the PC bench
runner. It does not modify `CalculationEngine`, chromatographic math, or validators.

## Current Research Implication

Recent chart extraction work continues to support ChromaLab's evidence-separated
pipeline:

- ChartRecover, published 2026-05-20, uses element detection plus coordinate
  transformation through axis tick alignment and adaptive conversion:
  https://www.nature.com/articles/s44172-026-00691-8
- ExChart / CHI 2026 decomposes extraction into coordinate-system understanding,
  visual mark interpretation, and precise value recovery, and notes that fully
  automatic MLLM extraction remains unreliable without verification:
  https://exchart.github.io/
- Chart2CSV reports that VLMs still misinterpret many complex chart data points,
  which supports keeping VLM outputs advisory/evidence-bound rather than numeric
  authority:
  https://openreview.net/forum?id=b0B6JQF8Xj
- Self-Ensembling VLM work from 2026-05-26 improves chart extraction by repeated
  sampling and uncertainty estimation, suggesting future use for OCR/semantic
  confidence, not direct chromatographic metric creation:
  https://arxiv.org/abs/2605.27298

## Implementation Decision

The desktop suite can now use per-fixture recorded VLM axis responses when they are
available:

```powershell
.\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1 `
  -OutputRoot "build\phase-pc-axis-llm-audit" `
  -ReplayRoot "docs\reference\chromatogram_bench"
```

Runner behavior:

- looks for `axis_vlm_replay_<fixture_id>.json`;
- supports the existing alias `axis_vlm_replay_bench_07.json` for
  `bench_07_rotated_page_photo`;
- preserves any existing live `CHROMALAB_DESKTOP_VLM_RESPONSE_FILE` when no
  per-fixture replay exists;
- leaves fixtures blocked honestly when neither replay nor live endpoint exists;
- writes `axisReplayFile` into `pc_chromatogram_bench_summary.csv`.

## Product Rule

Replay files are not fake calibration. They are recorded model axis-label evidence
used to test parser, gate, calibration, signal, peak, and report stages
deterministically. Missing replay or model evidence remains a blocker.

For production, the same path must be driven by a live local VLM/OCR engine that
reads the current image crops. VLM output may provide text and confidence; it must
not create chromatographic metrics or bypass geometry/calibration gates.

## Current Coverage

Only one vetted replay exists today:

```text
docs\reference\chromatogram_bench\axis_vlm_replay_bench_07.json
```

The next evidence task is to collect real local VLM axis-band outputs for the other
bench fixtures and store only vetted responses with provenance.

## Validation Run

Command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\chromatogram-bench\run_pc_chromatogram_bench_suite.ps1 `
  -OutputRoot "build\phase-pc-axis-llm-audit" `
  -ReplayRoot "docs\reference\chromatogram_bench" `
  -SkipGradleBuild
```

Result:

| Fixture | Expected graphs | Detected graphs | Ready | Blocked stage | Calibrated graphs | Signal-ready graphs | Peak-ready graphs | Replay |
| --- | ---: | ---: | --- | --- | ---: | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | 2 | 2 | false | `axis_calibration` | 0 | 0 | 0 | none |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | false | `crop_quality` | 0 | 0 | 0 | none |
| `bench_03_small_tic_export` | 1 | 1 | false | `axis_calibration` | 0 | 0 | 0 | none |
| `bench_04_stacked_xic_resolution` | 4 | 4 | false | `axis_calibration` | 0 | 0 | 0 | none |
| `bench_05_tic_plus_ions` | 4 | 4 | false | `axis_calibration` | 0 | 0 | 0 | none |
| `bench_06_photo_two_graphs_page` | 2 | 2 | false | `axis_detect` | 0 | 0 | 0 | none |
| `bench_07_rotated_page_photo` | 1 | 1 | true | `not_blocked` | 1 | 1 | 1 | `axis_vlm_replay_bench_07.json` |
| `bench_08_mz71_duplicate_candidate` | 1 | 1 | false | `axis_calibration` | 0 | 0 | 0 | none |

Interpretation:

- The replay-root wiring is working and visible in the CSV summary.
- The suite does not hide missing LLM/OCR evidence for the other seven fixtures.
- `bench_07_rotated_page_photo` proves the downstream PC path can run through
  calibration, signal conversion, peak detection, peak metrics, and report
  validation once accepted axis evidence exists.
- The next real product step is live local VLM/OCR collection for the remaining
  fixtures, followed by per-fixture replay capture only after the outputs are
  inspected and accepted as evidence.
