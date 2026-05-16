# Model-Assisted Stage Contract

Phase 8.1 defines how on-device models may participate in chromatogram analysis.
The goal is strict scientific separation: models may assist recognition and
interpretation, but they must not replace deterministic numeric calculation or invent
compound assignments.

## Strict Photo Analysis Stages

`ModelAssistedAnalysisContract` defines the current strict photo-analysis contract:

| Stage | Mode | Role |
| --- | --- | --- |
| `model.graph_region` | Required vision contract | Graph/panel recognition hint before CV refinement. |
| `model.title_ion_axis` | Required vision contract | Title, ION/channel, and axis text extraction. |
| `model.axis_structure` | Optional vision hint | Axis/grid structure hint only. |
| `model.chemical_interpretation` | Local knowledge only | Class-level context and cautions; model text stays lower confidence. |
| `calculation.numeric_results` | Deterministic only | Signal conversion, peak metrics, integration, and numeric report values. |

## Non-Negotiable Rules

- Deterministic image preparation, graph refinement, curve extraction, and
  `CalculationEngine` remain the source of numeric peak data.
- VLM output cannot create peak heights, areas, FWHM, baseline, S/N, Kovats values, or
  final compound names.
- Required vision stages must fail clearly in full photo analysis. They cannot be
  replaced by deterministic-only output and still produce a completed scientific
  report.
- GGUF chromatogram image analysis requires both the base `.gguf` and the matching
  `mmproj` projector.
- OCR/document-only model families are excluded from strict chromatogram VLM analysis
  unless a future validated adapter is explicitly added.

## Current Implementation

- `ModelRegistry.isChromatogramVisionModel()` now delegates to the strict eligibility
  contract.
- `blocksFullAnalysisSkip()` uses the same contract markers for non-skippable model
  failures.
- `ProcessingFlowScreen` uses the shared missing-VLM message when photo analysis cannot
  load a strict chromatogram vision model.
- `ProcessingReportMetadataBuilder` augments saved report metadata with model-stage
  timing entries for strict VLM contracts when a vision runtime is executed.
- Full-analysis metadata records failed required VLM stages as structured report
  warnings when no executed vision model/runtime is present.
- Report metadata already records selected/executed model, runtime, backend, device,
  and stage timings; those fields remain required evidence for Phase 8 validation.

## Audit Entries

Saved reports can now contain these model-stage timing IDs:

- `model.graph_region`;
- `model.title_ion_axis`.

If a full photo analysis has no executed LiteRT/GGUF/MIXED vision runtime, the same
required stages are stored as `FAILED` report warnings. This keeps failed model stages
visible in the final report audit instead of only in runtime logs.
