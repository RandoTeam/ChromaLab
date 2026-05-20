# Phase 7B Report Evidence Hardening Research

Date: 2026-05-20

## Scope

Phase 7B reviewed current authoritative guidance for scientific provenance, mobile-accessible report status presentation, and export privacy. The implementation decisions are limited to report contracts, renderers, validators, and golden assertions. No chromatographic math, peak detection, VLM numeric boundary, or CalculationEngine behavior was changed.

## Source Quality Triage

| Source | Type | Use | Decision |
| --- | --- | --- | --- |
| W3C PROV-O Recommendation, https://www.w3.org/TR/prov-o/ | Standard | Provenance records should identify entities, activities, agents, and derivation. | Use for first-class citation records and explicit generated-by fields. |
| Android Compose Semantics, https://developer.android.com/develop/ui/compose/accessibility/semantics | Official docs | Status chips and evidence controls should expose semantic descriptions, not rely only on color. | Use for typed visual evidence status labels in Compose. |
| Android shared documents and Storage Access Framework, https://developer.android.com/training/data-storage/shared/documents-files | Official docs | User-visible exports should be distinct from app-private diagnostics and user-selected sharing. | Use for separating user reports from technical evidence and diagnostic bundles. |
| Android secure file sharing, https://developer.android.com/training/secure-file-sharing | Official docs | Shareable files should be intentionally exposed and mediated; internal files should not be leaked by default. | Use for removing `NEVER_SHARED_BY_DEFAULT` artifacts from normal report manifests. |
| NIST TN 1297 reporting uncertainty, https://www.nist.gov/pml/nist-technical-note-1297/nist-tn-1297-7-reporting-uncertainty | Authoritative metrology guidance | Scientific reports must state uncertainty/limitations clearly enough that results are not overclaimed. | Use for release/review/diagnostic caveat visibility. |

Rejected source categories: weak blogs, unsourced UX advice, marketing-only export/privacy claims, and outdated API examples.

## Decisions

1. Report Knowledge Pack usage is a provenance record, not free text. Each scientific/domain explanation now needs used entry IDs, full entry records, source refs, allowed/forbidden use, trust tier, generator, unsupported claims, and rejection reason.
2. VLM-with-knowledge output without used entry IDs is not accepted as factual report evidence. It is validator-visible review evidence.
3. Knowledge Pack citations cannot create measured RT, height, area, FWHM, S/N, baseline, Kovats/RI, calibration, or peak metrics.
4. Visual evidence status is typed and stable across mobile UI, HTML, Markdown, and JSON export.
5. User-facing report exports do not list raw device logs or `NEVER_SHARED_BY_DEFAULT` artifacts. Diagnostic evidence remains a separate export class.
