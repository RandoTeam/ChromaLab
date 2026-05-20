# Autonomous-First Product Research - 2026-05-20

## Scope

This note supports the Phase 4 product correction from manual/guided-first workflow to autonomous-first chromatogram analysis with assisted review fallback.

## Source Matrix

| Source | Quality | Relevance | Decision affected | Not adopted |
| --- | --- | --- | --- | --- |
| [WebPlotDigitizer](https://www.automeris.io/) and [WebPlotDigitizer docs](https://automeris.io/docs/digitize/) | Maintained project / product docs | Mature graph digitization uses calibration, masking, auto extraction, and user-verifiable correction. | ChromaLab should attempt autonomous extraction first, but expose evidence and correction tools. | Do not import AGPL code or treat a generic digitizer as chromatogram-specific validation. |
| [PlotDigitizer](https://plotdigitizer.com/) | Current graph digitization product docs | Shows current tools combine automatic trace extraction with zoom/correction workflows. | Assisted review should repair failed stages, not replace autonomous evidence by default. | Do not copy marketing claims about accuracy without validation. |
| [NIST AI Risk Management Framework](https://www.nist.gov/itl/ai-risk-management-framework) | Authoritative standard-like framework | Emphasizes trustworthiness, documentation, and risk management for AI-assisted systems. | Release gates must preserve evidence, warnings, and human/AI role separation. | Do not convert NIST categories into app UI without product validation. |
| [FDA transparency principles for ML-enabled medical devices](https://www.fda.gov/medical-devices/software-medical-device-samd/transparency-machine-learning-enabled-medical-devices-guiding-principles) | Authoritative regulatory guidance | Human-AI team performance requires clear limitations and user-facing evidence. | Assisted review must disclose user intervention and model limitations. | ChromaLab is not medical-device software; use principles, not regulatory claims. |
| [W3C PROV Overview](https://www.w3.org/TR/prov-overview/) | W3C standard | Provenance should capture entities, activities, and agents supporting trust and reproducibility. | RuntimeEvidencePackage and report contracts should distinguish automatic, assisted, and manual evidence. | Do not implement full PROV serialization in this slice. |

## Decisions

- `AUTONOMOUS_PRODUCTION` becomes the primary target path.
- `AUTO_DIAGNOSTIC` remains automatic but incomplete/review/diagnostic unless gates pass.
- `ASSISTED_REVIEW` owns Phase 2 ROI, Phase 3 calibration, and Phase 4 trace review components.
- `MANUAL_ADVANCED` remains expert fallback.
- `GUIDED_PRODUCTION` is retained only as a compatibility alias for earlier state/docs.

## Rejected Sources / Claims

- Marketing claims from generic chart extraction tools do not prove chromatogram-grade quantitative validity.
- VLM chart-to-table papers and demos are not sufficient to authorize numeric geometry, peak RT, area, height, FWHM, S/N, baseline, or Kovats metrics.
- User confirmation is not a substitute for missing evidence; it is provenance-backed intervention.
