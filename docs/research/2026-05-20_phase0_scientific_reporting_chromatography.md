# Phase 0 Research - Scientific Reporting / Chromatography SME

Date: 2026-05-20
Phase: Phase 0
Agents: Scientific Reporting & Validation Agent, Chromatography SME Agent
Skills: `current-web-research-deep`, `source-quality-triage`, `scientific-report-provenance`, `evidence-gated-reporting`
Research confidence: HIGH for gate language; MEDIUM for later algorithm choices

## Research Question

What scientific-reporting and chromatography constraints must Phase 0 enforce before any image-derived chromatogram report can be treated as release-quality?

## Sources Reviewed

| Source | Tier | Relevance | Limitation | Decision impact |
| --- | --- | --- | --- | --- |
| [IUPAC Gold Book: retention index](https://goldbook.iupac.org/terms/view/R05360) | Official scientific reference | Defines Kováts/retention index and required n-alkane bracketing/retention data. | Does not define image digitization quality gates. | Kovats output cannot be release-quality without valid retention times and reference series provenance. |
| [NIST Gas Chromatographic Retention Data](https://webbook.nist.gov/chemistry/gc-ri/) | Government scientific database | Shows retention-index data as reference metadata, not model-inferred values. | Database does not validate ChromaLab image extraction. | Report must preserve reference/provenance and not invent RI values from VLM. |
| [SciPy `find_peaks`](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html) | Maintained library docs | Peak properties such as prominence and width are defined on validated 1D signals. | Not chromatogram-specific and not an Android implementation. | Peak rows require validated signal/trace evidence; candidate lines or labels alone are not peaks. |
| [OpenMS peak picking / pyOpenMS feature detection](https://pyopenms.readthedocs.io/en/latest/user_guide/feature_detection.html) | Maintained scientific software docs | Separates raw signal, feature detection, and algorithm metadata. | Targets MS data, not rasterized photos. | ChromaLab report should separate raw, validated, review, reportable, and rejected peaks. |
| [MOCCA paper](https://pubs.acs.org/doi/10.1021/acscentsci.2c01042) | Peer-reviewed method source | Modern chromatography workflow with overlapping peak/deconvolution concerns. | Later-phase method reference; not a Phase 0 implementation target. | Do not rewrite `CalculationEngine` in Phase 0; capture future review risk. |

## Findings

- Scientific metrics require validated upstream data. RT, height, area, FWHM, S/N, baseline, and Kovats are not valid just because a report row exists.
- Image-derived signals need provenance and uncertainty labels; missing graphPanel, plotArea, calibration, or trace evidence must block release-ready status.
- Recovered peak labels are only hints until local signal evidence confirms a maximum or shoulder.

## Phase 0 Decision

`RELEASE_READY` requires valid or user-confirmed graphPanel, plotArea, X calibration, Y calibration, trace, evidence package, source provenance, and no validator blockers.

## Rejected Approaches

- VLM-derived RT/height/area/FWHM/S/N/baseline/Kovats values.
- Treating OCR labels as peaks without local signal verification.
- Changing `CalculationEngine` to hide weak upstream evidence.

## Required Validation

- Report gate tests for missing calibration/evidence.
- VLM boundary tests for forbidden numeric metrics.
- Regression matrix rows for weak/faint peaks, dense peaks, labels inside plotArea, missing tick labels, and invalid calibration.
