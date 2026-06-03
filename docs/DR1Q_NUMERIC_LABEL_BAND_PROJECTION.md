# DR-1Q Numeric Label-Band Projection Prototype

Status: `DR1Q_PROTOTYPE_COMPLETE`

Scope: evidence-only desktop/PC prototype. This slice does not change `CalculationEngine`, chromatographic math, report gates, validators, Android runtime behavior, or production calibration selection.

## Task Classification

- Deep research follow-up prototype
- Axis label OCR geometry
- OCR-to-axis projection evidence
- Runtime audit export
- QA / regression golden artifacts

## Agents Activated

- Orchestrator: kept this as the next DR-1P follow-up, not a broad repair phase.
- Research Intelligence: checked current graph-digitizer method direction before implementation.
- Geometry / Calibration Core: owned label-band geometry and deterministic tick projection evidence.
- OCR / VLM Text Semantics: owned numeric label classification and semantic text rejection.
- QA / Regression: ran compile, PC replay suite, and fixture regression test.
- Scientific Reporting / Validation: kept calibration evidence auditable and non-overclaiming.
- Product Acceptance: classified the result as diagnostic evidence, not production acceptance.

## Skills Used

- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `ocr-local-crops`
- `geometry-calibration-robust-fit`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `test-plan-authoring`

## Method Notes

External digitizer tools still depend on known calibration positions plus known values. PlotDigitizer documents the usual XY calibration workflow with X1/X2/Y1/Y2 markers and recommends choosing calibration points far apart for accuracy: https://plotdigitizer.com/docs. WebPlotDigitizer similarly treats two points on each axis as the core calibration input: https://automeris.io/docs/digitize/. Plot2Spectra is closer to our autonomous target because it detects plot regions and uses scene text detection/recognition for tick labels below the x-axis: https://pubs.rsc.org/en/content/articlehtml/2022/dd/d1dd00036e.

DR-1Q therefore records not just OCR text, but whether the numeric label box can be geometrically projected to an axis/tick/frame context.

## Prototype Added

The existing `OfflineAxisElementGraphAudit` now includes:

- `labelBands`
  - `xLabelBand`
  - `yLabelBand`
  - `titleBand`
- label-band nodes:
  - `X_LABEL_BAND`
  - `Y_LABEL_BAND`
  - `TITLE_BAND`
- `labelProjections`
  - OCR node id
  - axis
  - raw text
  - numeric value
  - label box
  - label center
  - source band
  - projected pixel
  - plot-relative projected pixel
  - nearest deterministic tick pixel
  - nearest tick distance
  - projection method
  - status: `ACCEPTED`, `REVIEW`, `REJECTED`
  - rejection reason
  - source and crop path

Projection rules:

- ML Kit numeric label boxes in X/Y label bands can be projected.
- If a deterministic tick is close enough, projection status is `ACCEPTED`.
- If only the OCR label-box center is available, projection status is `REVIEW`.
- VLM numeric text is not accepted as pixel geometry unless tied to a deterministic tick crop.
- title/ion/m/z/SIM/scan text is rejected as scale evidence.
- Numeric text outside X/Y bands is rejected with an explicit reason.

The desktop overlay now draws:

- graph panel;
- plot area;
- X label band;
- Y label band;
- title/ion rejection band;
- axes;
- tick positions;
- OCR boxes;
- calibration anchors.

## PC Bench Replay Result

Output root:

`C:\VietnAi\Hromotograth\build\dr1q-label-band-projection`

Suite summary:

`C:\VietnAi\Hromotograth\build\dr1q-label-band-projection\pc_chromatogram_bench_summary.csv`

| Fixture | Expected graphs | Detected graphs | Ready | Blocked at | Calibrated graphs |
| --- | ---: | ---: | --- | --- | ---: |
| bench_01_mz71_screenshot_page | 2 | 2 | false | axis_calibration | 0 |
| bench_02_mz92_belyi_tigr | 1 | 1 | false | crop_quality | 0 |
| bench_03_small_tic_export | 1 | 1 | false | axis_calibration | 0 |
| bench_04_stacked_xic_resolution | 4 | 4 | false | axis_calibration | 0 |
| bench_05_tic_plus_ions | 4 | 4 | false | axis_calibration | 0 |
| bench_06_photo_two_graphs_page | 2 | 2 | false | axis_detect | 0 |
| bench_07_rotated_page_photo | 1 | 1 | true | not_blocked | 1 |
| bench_08_mz71_duplicate_candidate | 1 | 1 | false | axis_calibration | 0 |

## Label Projection Evidence Table

| Fixture | Graph | Label projections | Accepted | Review | Scale pairs | Accepted scale pairs | Main blocker |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| bench_01_mz71_screenshot_page | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_01_mz71_screenshot_page | 2 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_02_mz92_belyi_tigr | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_03_small_tic_export | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_04_stacked_xic_resolution | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_04_stacked_xic_resolution | 2 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_04_stacked_xic_resolution | 3 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_04_stacked_xic_resolution | 4 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_05_tic_plus_ions | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_05_tic_plus_ions | 2 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_05_tic_plus_ions | 3 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_05_tic_plus_ions | 4 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_06_photo_two_graphs_page | 1 | 0 | 0 | 0 | 0 | 0 | Y axis missing; OCR labels missing; label projection missing |
| bench_06_photo_two_graphs_page | 2 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |
| bench_07_rotated_page_photo | 1 | 6 | 5 | 1 | 2 | 2 | none |
| bench_08_mz71_duplicate_candidate | 1 | 0 | 0 | 0 | 0 | 0 | OCR labels missing; label projection missing |

## Bench 07 Projection Detail

| Raw text | Axis | Band | Method | Status | Note |
| --- | --- | --- | --- | --- | --- |
| `5.00` | X | X label band | nearest deterministic tick | ACCEPTED | tick distance 5.44 px |
| `30.00` | X | X label band | nearest deterministic tick | ACCEPTED | tick distance 11.36 px |
| `55.00` | X | X label band | nearest deterministic tick | ACCEPTED | tick distance 7.84 px |
| `0` | X | X label band | OCR label box center | REVIEW | tick distance 29.5 px |
| `200000` | Y | Y label band | nearest deterministic tick | ACCEPTED | tick distance 6.82 px |
| `400000` | Y | Y label band | nearest deterministic tick | ACCEPTED | tick distance 12.72 px |

Artifact examples:

- `C:\VietnAi\Hromotograth\build\dr1q-label-band-projection\bench_07_rotated_page_photo\graph_1\axis_element_graph.json`
- `C:\VietnAi\Hromotograth\build\dr1q-label-band-projection\bench_07_rotated_page_photo\graph_1\axis_element_graph_overlay.png`

## Findings

1. DR-1Q confirms that the current PC replay suite has usable axis OCR replay only for `bench_07_rotated_page_photo`.
2. For the other 15 graph packages, the evidence blocker is upstream OCR absence: `axis_element_graph.ocr_labels_missing` plus `axis_element_graph.label_projection_missing`.
3. Label-band geometry is now represented explicitly even when OCR is absent, so future OCR/crop fixes can be evaluated visually.
4. The initial Y label band was too narrow for bench_07 and rejected visible Y numeric labels; DR-1Q widened the diagnostic Y band to match the existing AxisScaleResolver tolerance.
5. The prototype cleanly separates accepted tick-supported projections from review-only label-box projections.
6. No production calibration behavior changed; the layer only records evidence.

## Next Research/Prototype Target

Recommended next wave: local axis-label OCR crop sweep.

Required focus:

- generate and persist expanded X/Y label-band crops per graph;
- run local OCR/crop replay where available;
- compare crop variants by numeric label count and semantic rejection count;
- keep title/ion/m/z rejection separate from scale labels;
- only after evidence improves, consider feeding accepted projections into calibration strategies.
