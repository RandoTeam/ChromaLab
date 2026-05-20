# Phase 5 Research: Autonomous Peak Detection + Evidence Review

Date: 2026-05-20

## Source Quality

This research used official documentation, maintained scientific-tool docs, standards, and Android documentation. Weak blogs, marketing pages, and unverifiable benchmark claims were rejected as implementation drivers.

## Sources

- [SciPy `find_peaks`](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html): current API reference for deterministic 1D peak properties such as height, threshold, distance, prominence, width, and plateau size. Decision: ChromaLab peak evidence should expose peak height, width/FWHM, prominence, and local-maximum evidence separately instead of treating a peak label or candidate line as a peak.
- [MZmine ADAP resolver documentation](https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_adap/adap-resolver.html): maintained chromatogram feature resolving docs covering CWT/ridgeline detection, boundaries, S/N estimation, and peak duration constraints. Decision: Phase 5 should track S/N, boundary evidence, peak width, overlap/shoulder state, and missing metrics as review evidence. It should not invent missing values.
- [OpenMS PeakPicking group](https://www.openms.org/documentation/html/group__PeakPicking.html): maintained OpenMS documentation for high-resolution peak picking classes. Decision: ChromaLab should preserve deterministic algorithm provenance and algorithm version in peak evidence rather than rewriting peak math in this phase.
- [NIST Mass Spectrometry Data Center](https://chemdata.nist.gov/) and [AMDIS description](https://chemdata.nist.gov/dokuwiki/doku.php?id=chemdata%3Aamdisexplained): authoritative GC/MS tooling context for deconvolution and overlapping component review. Decision: overlap/shoulder and unresolved peaks must be review-grade and must not be hidden in report-ready output.
- [W3C PROV overview](https://www.w3.org/TR/prov-overview/) and [PROV namespace](https://www.w3.org/ns/prov): provenance standards for recording entities, activities, agents, and derivation. Decision: user-confirmed/user-edited peak evidence must carry explicit provenance and cannot be merged silently into autonomous evidence.
- [Android Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility) and [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): official guidance relevant if a later Assisted Review peak UI is implemented. Decision: Phase 5 keeps UI out of scope except contracts/docs; future peak controls need non-color-only status, touch-accessible targets, and semantic descriptions.

## Decisions Affecting This Slice

- Keep `CalculationEngine` unchanged. Phase 5 wraps `CalculationRun` peaks into evidence/provenance contracts.
- Store unavailable optional metrics as `UNKNOWN` or review status. Missing FWHM, baseline, or S/N evidence must be visible and never fabricated.
- Treat autonomous peak output as release-capable only when a linked trace, apex/local maximum, nonzero height, boundary evidence, and non-blocking review status exist.
- Treat shoulder, overlap, sparse-trace, low-S/N, label-recovered, or weak-baseline peaks as review-grade evidence.
- Treat user peak confirmation/edit/rejection as Assisted Review provenance, not as invisible automatic output.

## What Not To Adopt

- Do not import or reimplement SciPy, MZmine, OpenMS, or AMDIS algorithms in this phase.
- Do not use VLM/OCR output as peak metrics.
- Do not turn manual peak editing into the primary workflow.
- Do not downgrade evidence requirements to keep legacy automatic reports looking release-ready.
