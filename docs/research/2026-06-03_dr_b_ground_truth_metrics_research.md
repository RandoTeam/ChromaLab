# DR-B Ground Truth And Metrics Research

Date: 2026-06-03

Status: `DR_B_RESEARCH_READY`

Scope: automatic ground-truth corpus and metric contracts for ChromaLab.

This is a contract/research slice. It does not change production algorithms,
`CalculationEngine`, chromatographic math, validators, Android runtime, or report
rendering.

## Research Question

How should ChromaLab score full autonomous chromatogram analysis automatically
so that graph detection, calibration, trace extraction, peak metrics, and report
claims are validated by machine-readable evidence rather than manual visual
judgment?

## Source Quality Handoff

| Source | Type | Use status | Decision impact |
| --- | --- | --- | --- |
| [JSON Schema 2020-12](https://json-schema.org/draft/2020-12/json-schema-validation) | Standard | Accepted | Use as the schema family for truth, prediction, metrics, evidence package, and report claims contracts. |
| [COCO evaluation / pycocotools](https://github.com/cocodataset/cocoapi/blob/master/PythonAPI/pycocotools/cocoeval.py) | Maintained benchmark reference | Accepted as metric pattern | Use IoU/AP-style thinking for graphPanel, plotArea, axis/tick/text boxes. |
| [scikit-image metrics](https://scikit-image.org/docs/stable/api/skimage.metrics.html) | Maintained scientific library docs | Accepted as metric reference | Use SSIM/Hausdorff-style image/shape metrics as references for overlays, masks, and centerlines. |
| [W3C PROV-O](https://www.w3.org/TR/prov-o/) | Standard | Accepted | Use provenance principles for linking prediction, source image, run id, evidence, and report claims. |
| [RO-Crate](https://www.researchobject.org/ro-crate/technical_overview) | Research object packaging spec | Background / future packaging | Useful later for portable benchmark packages; not required for initial schema contracts. |
| [BagIt RFC 8493](https://www.rfc-editor.org/info/rfc8493) | Packaging standard | Background / future packaging | Useful for artifact package checksums; initial DR-B uses simple artifact manifest paths and optional hashes. |
| [ChartRecover 2026](https://www.nature.com/articles/s44172-026-00691-8) | Current peer-reviewed method | Accepted for stage metrics | Confirms need for chart elements, tick matching, coordinate transform, and true-coordinate extraction scoring. |
| [MZmine](https://github.com/mzmine/mzmine), [OpenMS](https://github.com/OpenMS/OpenMS), [SciPy signal](https://docs.scipy.org/doc/scipy/reference/signal.html), [pybaselines](https://pybaselines.readthedocs.io/en/stable/introduction.html) | Maintained scientific software/docs | Accepted as downstream metric references | Support peak/baseline/integration metric categories after digitized signal exists. |

Confidence: `HIGH` for schema/metric direction. Actual thresholds remain
`REVIEW` until DR-B runs current fixtures and synthetic cases.

Implementation allowed: schema and documentation contracts only. Algorithmic
implementation remains out of scope for this slice.

## DR-B Metric Families

| Family | Metric examples | Release impact |
| --- | --- | --- |
| Graph layout | expected vs detected physical graph count, graphPanel IoU, plotArea IoU, candidate rejection precision | Wrong graph count or partial crop blocks release. |
| Axis and ticks | axis endpoint error, tick/grid detection AP, label-to-tick assignment accuracy | Missing or inconsistent X/Y geometry blocks calibrated numeric claims. |
| OCR/text roles | CER/WER, numeric parse accuracy, text role macro-F1, false tick-label rate | Bad role classification can block calibration or report explanations. |
| Calibration | X/Y RMSE, max residual, monotonicity, accepted/rejected anchor counts | Invalid X/Y calibration blocks RT, height, area, trace, and peaks. |
| Trace | mask IoU, centerline Hausdorff, x-coverage, max gap, contamination, signal NRMSE | Trace without calibrated evidence is diagnostic/review only. |
| Peaks | apex RT error, boundary IoU, area/height/FWHM/S/N error, false discovery rate | Peak metrics are release claims only when evidence passes. |
| Report claims | supported/review/rejected/missing evidence per claim | Unsupported scientific claims block release. |
| Evidence package | required artifact presence, privacy class, hashes, terminal failure class | Missing evidence is a product blocker. |

## Corpus Classes

### Synthetic

Purpose: complete numeric truth.

Use for:

- graph/axis/trace/peak metric calibration;
- stress cases: blur, JPEG, perspective, rotation, grid/text collisions, low
  resolution, multi-panel layouts;
- validating that a method does not overfit a few real screenshots.

### Real Paired

Purpose: real image complexity with reference signal or vendor/raw export.

Use for:

- release-quality numeric validation;
- peak RT/area/height comparison;
- trace/signal error checks.

### Real Unpaired Diagnostic

Purpose: real image failure behavior when no numeric truth exists.

Use for:

- diagnostic gates;
- evidence completeness;
- graph/layout robustness;
- blocked/review truth packages.

Do not use unpaired real images as proof of release-quality numeric accuracy.

## Contracts Created

| File | Role |
| --- | --- |
| `benchmark/schemas/truth.schema.json` | Ground truth annotations. |
| `benchmark/schemas/prediction.schema.json` | Normalized pipeline output for scoring. |
| `benchmark/schemas/metrics.schema.json` | Per-stage metric result and decision. |
| `benchmark/schemas/evidence-package.schema.json` | Minimum artifact manifest for terminal states. |
| `benchmark/schemas/report-claims.schema.json` | Evidence-gated report claim validation. |
| `benchmark/README.md` | Contract directory overview. |

## Guardrails

- Expected fixture metadata is validation truth, not a production selection hint.
- No model output can create numeric geometry, calibration, trace, peak, or
  compound identity truth.
- Visual regression can support UI/overlay checks, but not scientific numeric
  validity.
- `RELEASE_READY` is zero-tolerance for missing graphPanel, plotArea, X/Y
  calibration, trace, peak evidence, evidence package, or unsupported claims.
- `CalculationEngine` remains unchanged unless later benchmark evidence proves an
  isolated calculation bug after upstream input is valid.

## Next Work After This Slice

1. Add minimal example truth/prediction/metrics files for the current eight
   fixtures.
2. Build a schema validation runner.
3. Convert Phase 9J truth audit into benchmark prediction records.
4. Add first synthetic generator plan and one trivial synthetic fixture.
5. Only then start DR-C graph/axis benchmark experiments.
