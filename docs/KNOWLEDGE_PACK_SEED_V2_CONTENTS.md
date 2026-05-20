# Knowledge Pack Seed v2 Contents

Date: 2026-05-20

Seed file: `docs/knowledge/chromalab_knowledge_seed_v2.json`

Runtime seed: `ChromaLabKnowledgeSeedV2`

## Included Categories

- Core chromatogram terms: chromatogram, retention time, peak, apex, area, abundance, baseline, noise, S/N, FWHM, integration boundaries, shoulder/overlap/coelution, resolution, tailing/asymmetry.
- GC/MS terms: TIC, EIC, SIM, SCAN, m/z, ion/channel, mass range, extracted ion chromatogram, selected ion monitoring.
- Axis/graph terms: X/Y axes, tick labels, axis labels, plot area, graph panel, title, legend, grid line, calibration anchor.
- Kovats/RI rules: Kovats index, retention index, n-alkane reference series, C10-C40 naming pattern, interpolation caveat, missing-reference caveat.
- Chemical/domain terms: alkane, n-alkane, hydrocarbon, aromatic hydrocarbon, biomarker, sterane, terpane, PAH, compound class, unresolved complex mixture.
- Text-classification rules: ion/channel title is not peak label, m/z/mass range is not RT, title/header numbers are not peak annotations, peak annotations require local signal verification, tick labels require tick geometry, axis/legend text is not peak label.
- Report caveats: calibration required, trace evidence required, no compound assignment without explicit evidence, no Kovats without reference series, review-grade peak labelling, VLM semantic-only caveat, image-derived uncertainty.
- Safe prompt snippets: ROI, calibration invalid, sparse trace, peak overlap, shoulder peak, OCR ambiguity, VLM disagreement, diagnostic-only report explanations.
- Compound reference stubs: n-C10 through n-C40 alkane labels and aliases.

## Explicit Non-Contents

- No measured RT values.
- No measured peak heights or areas.
- No FWHM/S/N/baseline values.
- No proprietary spectra.
- No NIST/AMDIS/WebBook database rows.
- No compound identification claims.

Every entry includes allowed-use and forbidden-use fields. The forbidden-use policy blocks metric fabrication, calibration override, integration override, and unsupported compound identification.

## Phase 6C-2 Metadata

Every runtime entry now also declares:

- `claimScopes`, always including `NOT_MEASUREMENT`;
- source trust tier;
- confidence;
- source references.

Compact retrieval cards for Gemma E4B/E2B include only entry ID, short card text, allowed uses, forbidden uses, source refs, confidence, and trust tier.
