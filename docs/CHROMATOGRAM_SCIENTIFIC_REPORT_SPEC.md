# ChromaLab Scientific Report Spec

Date: 2026-05-20

## Required Report Sections

1. Cover/summary: source label, graph count, total peaks, report gate status, processing mode, model/runtime, duration, device.
2. Executive result: release-ready, review-only, diagnostic-only, or blocked state.
3. Graph overview: title/ion/channel, peak count, calibration status, trace status, peak evidence, reportability.
4. Per-graph evidence: source/geometry, axis calibration, rendered graph/overlay, peak table, quality, warnings.
5. Peak table: RT, height, area, area %, FWHM, base width, S/N, asymmetry/tailing if available, overlap/shoulder, compound status, confidence, flags, evidence gate.
6. Evidence appendix: gate matrix, validator reasons, model/runtime, value provenance, export manifest.
7. Knowledge citation appendix: used entry IDs, source refs, allowed/forbidden use, trust tier, unsupported claims, and rejection status for scientific/domain explanations.

## Scientific Boundaries

- RT, height, area, FWHM, S/N, baseline, Kovats/RI, and integration metrics must come from deterministic math or user/imported evidence, not VLM/Knowledge.
- Compound names from local knowledge or model context are hypotheses unless library/spectrum/RI/method/user evidence is attached.
- Kovats/RI is not reportable as calculated without same-method reference-series retention times.
- Missing calibration, trace, or peak evidence blocks release-quality claims.
- Knowledge Pack and VLM explanations can explain, classify, and caveat. They cannot create measured RT, height, area, FWHM, S/N, baseline, Kovats/RI, calibration coefficients, or final peak metrics.
- VLM-generated scientific/domain explanations without used Knowledge Pack entry IDs are review evidence or omitted.

## Presentation Rules

- Missing values render as `not calculated`, `not detected`, or `insufficient confidence`.
- Review/diagnostic state must be visible before the peak table.
- Technical warning codes belong in the appendix, but user-facing warning summaries remain visible.
- Visual evidence statuses must use typed status labels so user-facing reports do not rely only on color.
