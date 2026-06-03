# DR-1R Axis-Label OCR Crop Sweep

Status: `DR1R_EVIDENCE_COMPLETE`

Scope: evidence-only desktop/PC prototype. This slice does not change `CalculationEngine`, chromatographic math, production calibration selection, validators, Android runtime behavior, or report gates.

## Task Classification

- Deep research follow-up prototype
- Axis label OCR crop preparation
- OCR preprocessing evidence
- Graph-level artifact export
- QA / regression golden artifacts

## Agents Activated

- Orchestrator: kept this as a DR-1Q follow-up, not a production algorithm repair.
- Research Intelligence: checked current graph digitization and OCR preprocessing methods.
- Geometry / Calibration Core: owned axis label band geometry and crop provenance.
- OCR / VLM Text Semantics: owned OCR backend boundary and text-only evidence policy.
- QA / Regression: ran compile and PC bench replay suite.
- Scientific Reporting / Validation: kept outputs diagnostic and non-overclaiming.
- Product Acceptance: classified this as evidence needed before deciding the next OCR implementation step.
- Security / Privacy: prevented committing unlicensed external images.

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `ocr-local-crops`
- `ocr-crop-benchmark`
- `geometry-calibration-robust-fit`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `test-plan-authoring`
- `definition-of-done`

## Research Notes

- Plot2Spectra is relevant because it combines plot region detection, edge-constrained axis alignment, scene text detection/recognition for tick labels, and plot line extraction. Source: https://pubs.rsc.org/en/content/articlehtml/2022/dd/d1dd00036e.
- Plot2Spec includes example plot images and code under GPL-3.0, so it is suitable for method review and possibly a clearly attributed external fixture path. Source: https://github.com/MaterialEyes/Plot2Spec.
- PaddleOCR current documentation exposes PP-OCRv5 mobile detector/recognizer configuration and GPU device selection in the OCR pipeline. Source: https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html.
- Tesseract's own guidance emphasizes rescaling, binarisation, deskewing, borders, and page segmentation mode selection for small OCR crops. Source: https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html.
- PlotDigitizer/WebPlotDigitizer-style calibration still depends on known calibration values and deliberately selected calibration points, which is useful as a quality reference but not enough for our autonomous target. Source: https://plotdigitizer.com/docs.
- Scatteract is relevant as a chart extraction reference because it combines OCR with robust regression to map pixels into chart coordinates. Source: https://arxiv.org/abs/1704.06687.

## Desktop OCR Backend Audit

| Backend | DR-1R status | Evidence |
| --- | --- | --- |
| Android ML Kit | unavailable on desktop | Android-only in this repository |
| Desktop Tesseract CLI | unavailable | `tesseract --version` was not found |
| `pytesseract` | unavailable | Python import check failed |
| OpenCV / EasyOCR / PaddleOCR Python | unavailable | Python import checks failed |
| PIL | available | Used for crop rendering and image metrics |
| VLM replay OCR | available only where replay evidence exists | `bench_07_rotated_page_photo` |

DR-1R therefore does not claim desktop OCR recognition. It exports OCR-ready crop variants and objective image metrics so future OCR engines can be compared against the same crops.

## Prototype Added

Desktop offline analysis now writes per graph:

- `graph_N/axis_label_crop_sweep/axis_label_crop_sweep_graph_N.json`
- `graph_N/axis_label_crop_sweep/axis_label_crop_sweep_contact_sheet_graph_N.png`
- Crop variants for each available band:
  - `original`
  - `grayscale`
  - `scale2_grayscale`
  - `scale4_grayscale`
  - `scale4_contrast`
  - `scale4_otsu_threshold`
  - `scale4_otsu_threshold_inverted`

Each variant records:

- source band;
- crop path;
- scale;
- dimensions;
- mean luma and luma standard deviation;
- dark pixel ratio;
- foreground pixel ratio;
- edge density;
- connected component count;
- text-like connected component count;
- warnings such as low contrast, sparse foreground, or no text-like components.

## PC Bench Replay Result

Output root:

`C:\VietnAi\Hromotograth\build\dr1r-axis-label-crop-sweep`

Suite summary:

`C:\VietnAi\Hromotograth\build\dr1r-axis-label-crop-sweep\pc_chromatogram_bench_summary.csv`

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

Additional single-source runs:

| Source | Expected graphs | Detected graphs | Ready | Blocked at |
| --- | ---: | ---: | --- | --- |
| white_tiger_ion71_fixture | 1 | 1 | false | axis_calibration |
| belyi_tigr_ion92_extra | 1 | 1 | false | crop_quality |

## Crop Sweep Evidence Table

| Fixture | Graphs with crop sweep | Variants per graph | Best text-like component count range | Available OCR backend |
| --- | ---: | ---: | ---: | --- |
| bench_01_mz71_screenshot_page | 2 | 21 | 85-109 | none |
| bench_02_mz92_belyi_tigr | 1 | 21 | 59 | none |
| bench_03_small_tic_export | 1 | 21 | 43 | none |
| bench_04_stacked_xic_resolution | 4 | 21 | 7-22 | none |
| bench_05_tic_plus_ions | 4 | 21 | 27-139 | none |
| bench_06_photo_two_graphs_page | 2 | 21 | 111-113 | none |
| bench_07_rotated_page_photo | 1 | 21 | 93 | desktop_vlm_axis_ocr replay |
| bench_08_mz71_duplicate_candidate | 1 | 21 | 66 | none |
| white_tiger_ion71_fixture | 1 | 21 | 66 | none |
| belyi_tigr_ion92_extra | 1 | 21 | 59 | none |

Total DR-1R artifacts:

- 18 crop sweep JSON reports.
- 18 crop sweep contact sheets.
- 378 crop variant images.

## Findings

1. Axis label bands and crop variants are now inspectable for every detected graph in the PC replay suite plus White Tiger and the extra Belyi Tigr source.
2. The current PC environment cannot evaluate true OCR accuracy because no desktop OCR backend is installed or wired into the repository.
3. Several blocked fixtures contain text-like crop components even without OCR recognition, especially `bench_01`, `bench_05`, `bench_06`, `bench_07`, and White Tiger. These are good next targets for an actual OCR backend.
4. `bench_04_stacked_xic_resolution` has very weak text-like crop signals in several panels, so it likely needs expanded bands, higher-resolution source, or Android ML Kit/VLM crop review rather than tick-only logic.
5. `bench_07_rotated_page_photo` remains the only PC fixture with replay OCR evidence sufficient for calibration, consistent with DR-1Q.
6. The next implementation slice should connect a real OCR reader to these generated crops, not add more calibration heuristics.

## Artifact Examples

- `C:\VietnAi\Hromotograth\build\dr1r-axis-label-crop-sweep\white_tiger_ion71_fixture\graph_1\axis_label_crop_sweep\axis_label_crop_sweep_contact_sheet_graph_1.png`
- `C:\VietnAi\Hromotograth\build\dr1r-axis-label-crop-sweep\bench_05_tic_plus_ions\graph_4\axis_label_crop_sweep\axis_label_crop_sweep_graph_4.json`
- `C:\VietnAi\Hromotograth\build\dr1r-axis-label-crop-sweep\bench_07_rotated_page_photo\graph_1\axis_label_crop_sweep\axis_label_crop_sweep_graph_1.json`

## Next Step

Recommended next wave: DR-1S, real OCR backend evaluation on the DR-1R crop corpus.

Candidate paths:

1. Android ML Kit crop OCR replay on all generated label-band crops.
2. PaddleOCR PP-OCRv5 mobile/server comparison on PC if dependency and license policy are approved.
3. Tesseract numeric-only crop benchmark if desktop Tesseract is installed and configured.
4. E2B/VLM crop OCR as text-only advisory evidence, never as pixel geometry or chromatographic metric authority.
