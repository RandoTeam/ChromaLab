# Phase 7 Scientific Provenance Report Research

Date: 2026-05-20

## Scope

Research provenance and scientific caveat practices for image-derived chromatographic reports.

## Sources Used

| Source | URL | Quality tier | Relevant finding | Phase 7 decision |
| --- | --- | --- | --- | --- |
| W3C PROV-O | https://www.w3.org/TR/prov-o/ | W3C recommendation | PROV-O provides classes/properties for representing provenance across systems and domains. | Report contract separates deterministic values, source provenance, model/runtime trace, and export artifacts. |
| NIST uncertainty reporting | https://www.nist.gov/pml/nist-technical-note-1297/nist-tn-1297-7-reporting-uncertainty | Authoritative measurement guidance | Measurement reports should provide contextual information needed to interpret results. | Release-ready claims require visible graph, calibration, trace, peak, validator, and evidence-package status. |
| Eurachem analytical uncertainty guide | https://eurachem.org/index.php/publications/guides/quam | Authoritative analytical chemistry guidance | Analytical measurement reports should distinguish result, method, and uncertainty context. | ChromaLab labels review/diagnostic metrics and does not hide missing calibration, trace, or peak evidence. |
| IUPAC Gold Book, chromatography peak widths | https://old.goldbook.iupac.org/html/P/P04466.html | Authoritative terminology source | Peak width terminology is retention-dimension based. | Report peak columns use RT, FWHM, base width, S/N, baseline, overlap, and evidence flags explicitly. |

## Decisions

- Deterministic calculations are the only source for numeric chromatographic metrics.
- Knowledge Pack and VLM outputs may explain, classify, or warn, but cannot identify compounds or create measured values.
- Kovats/RI values require explicit reference series provenance; otherwise caveats are shown.
- Local-knowledge compound labels are rendered as candidate hypotheses unless identity evidence is attached.

## Rejected Sources

Forum answers and uncited chemistry summaries were not used for implementation decisions.
