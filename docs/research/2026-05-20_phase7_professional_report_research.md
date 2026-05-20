# Phase 7 Professional Report Research

Date: 2026-05-20

## Scope

Research current guidance for professional scientific/mobile reports, evidence status presentation, dense data tables, and export-ready report structure. Model knowledge may be outdated, so Phase 7 decisions use current official or authoritative sources only.

## Sources Used

| Source | URL | Quality tier | Relevant finding | Implementation decision |
| --- | --- | --- | --- | --- |
| Android Developers, Compose semantics | https://developer.android.com/develop/ui/compose/accessibility/semantics | Official current API docs | Compose semantics provide accessibility/testing context for non-text UI and custom components. | Report status/evidence chips need semantic descriptions and report headings should be exposed as headings. |
| Android Developers, Compose gestures | https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures | Official current API docs | Built-in components include focus/accessibility behavior; custom gesture handling needs explicit semantics. | Phase 7 report remains mostly scroll/table/read-only; no custom manual interaction is added. |
| Material Design, data tables | https://m1.material.io/components/data-tables.html | Official design reference | Data tables display raw data, use header rows, and may be placed on a surface with related visualization. | Peak tables retain clear headers, horizontal overflow, and evidence/status columns instead of unlabelled debug columns. |
| NIST TN 1297 reporting uncertainty | https://www.nist.gov/pml/nist-technical-note-1297/nist-tn-1297-7-reporting-uncertainty | Authoritative measurement reporting reference | Reports should include enough context for interpreting measurement results and uncertainty. | Report surfaces must show calibration/trace/peak evidence and review/diagnostic caveats beside metric claims. |
| W3C PROV-O | https://www.w3.org/TR/prov-o/ | W3C recommendation | PROV-O models provenance as reusable, interoperable entities/activities/agents. | ChromaLab reports separate measured values, model/runtime evidence, Knowledge Pack context, and user/diagnostic artifacts. |

## Decisions

- The report must lead with report gate status and release-quality claim state.
- The mobile surface and HTML/Markdown exports must show the same gate evidence categories.
- Peak tables must show evidence status and peak gate before numeric metrics.
- Compound names from local knowledge or model context are shown as hypotheses unless explicit identity evidence exists.
- Diagnostic artifacts are separated from user-facing report surfaces.

## Rejected Sources

Weak blog posts, marketing-only AI report claims, and uncited UI examples were not used.
