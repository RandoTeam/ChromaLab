# Phase 6: Autonomous Multimodal Intelligence Layer

Phase 6 adds an evidence layer for VLM/OCR/CV cooperation without making VLM a numeric measurement engine.

## Ownership

- CV owns graphPanel, plotArea, axis/tick pixels, trace masks, and curve extraction evidence.
- OCR owns local crop text reading for ticks, axis labels, title/channel text, and peak annotations.
- VLM owns hard crop OCR fallback, semantic classification, overlay judging, retry explanations, and warning summaries.
- Deterministic math owns calibration, RT, height, area, FWHM, S/N, baseline, Kovats, and peak integration.

## Implemented In This Slice

- Shared stage judge contracts.
- VLM JSON boundary policy with forbidden numeric field rejection.
- Stage retry recommendation policy.
- Runtime evidence package fields for stage judges, crop results, disagreements, overlay judgements, and model runtime profiles.
- Runtime evidence validator checks for forbidden VLM numeric fields, crop provenance, timeout profiles, and forbidden retry actions.
- OCR/VLM crop benchmark harness with JSON and Markdown export.

## Out Of Scope

- Changing `ActiveVisionModelBackend` behavior.
- Changing ML Kit OCR behavior.
- Changing graph/trace/peak algorithms.
- Manual review as primary workflow.
- Any CalculationEngine changes.

## Evidence Required

Every multimodal stage result must include task type, source, verdict, confidence, linked evidence ids, crop or overlay path when applicable, warnings, and runtime profile for VLM-backed work.

