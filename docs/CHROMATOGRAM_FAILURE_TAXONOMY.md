# ChromaLab Failure Taxonomy

## Purpose

Phase 8 failures must be classified instead of hidden, converted into expected passes, or buried in debug logs. Each terminal non-release result needs a failure class, evidence, severity, retry policy, Assisted Review policy, and release-block decision.

## Taxonomy

| Failure class | Definition | Evidence required | Severity | Autonomous retry allowed | Assisted Review appropriate | Release report blocked |
| --- | --- | --- | --- | --- | --- | --- |
| `IMAGE_DECODE_FAILURE` | Image bytes cannot be decoded into a usable bitmap. | Source image path/hash, decode error, terminal evidence package. | Blocking | Yes with alternate decoder/import path | No | Yes |
| `ORIENTATION_FAILURE` | Orientation correction fails or crops a valid chromatogram graph. | Original/normalized/rotated overlays, orientation decision, graphPanel overlay. | Blocking | Yes | Yes if graph remains visible | Yes |
| `GRAPH_PANEL_FAILURE` | Full graphPanel cannot be selected or partial panel is selected. | Candidate table, selected/rejected overlays, graphPanel status. | Blocking | Yes | Yes | Yes |
| `MULTI_GRAPH_SPLIT_FAILURE` | Physical graph count is wrong or one graph becomes many pseudo-graphs. | Multiplicity resolver output, per-candidate overlays, expected graph count. | Blocking | Yes | Yes | Yes |
| `PLOT_AREA_FAILURE` | PlotArea is missing, outside graphPanel, zero-area, or equal to full panel without review. | PlotArea overlay, graphPanel relation, validator finding. | Blocking | Yes | Yes | Yes |
| `AXIS_DETECTION_FAILURE` | X/Y axes cannot be localized. | Axis overlay, line evidence, origin evidence, candidate rejection reasons. | Blocking | Yes | Yes | Yes |
| `TICK_LOCALIZATION_FAILURE` | Tick marks or deterministic label-band tick geometry cannot be linked to usable pixel positions. | Tick overlay or explicit missing reason, tick candidate table, axis linkage, graph failure package. | Blocking | Yes with projection/grid/label-band rescue | Yes | Yes |
| `OCR_TICK_FAILURE` | Tick label OCR is missing, ambiguous, or inconsistent. | OCR crop, OCR/VLM disagreement, accepted/rejected text table. | Review | Yes with expanded/enhanced crop | Yes | Yes unless user-confirmed calibration exists |
| `CALIBRATION_FAILURE` | X/Y calibration is missing, invalid, non-monotonic, or has blocking residuals. | Calibration anchors, residuals, fit status, validator report. | Blocking | Yes | Yes | Yes |
| `TRACE_EXTRACTION_FAILURE` | No usable trace is extracted. | Raw/clean mask, rejected components, selected trace overlay. | Blocking | Yes | Yes | Yes |
| `SPARSE_TRACE_REVIEW` | Trace exists but is sparse, fragmented, contaminated, or low-confidence. | Trace quality metrics, overlay, warnings. | Review | Yes | Yes | Yes for release-ready |
| `PEAK_DETECTION_FAILURE` | Peaks cannot be detected from valid trace/calibration evidence. | Trace, peak detector status, warnings. | Blocking | Yes | Yes | Yes |
| `PEAK_EVIDENCE_FAILURE` | Peaks exist but lack apex, boundary, metric, or provenance evidence. | Peak evidence table, overlay, validator findings. | Blocking | Yes | Yes | Yes |
| `KNOWLEDGE_GROUNDING_FAILURE` | Knowledge/VLM explanation lacks entry IDs or violates claim policy. | Knowledge context, used entry IDs, unsupported claims, validator finding. | Review | No numeric retry; semantic retry only | Yes | Blocks unsupported scientific claims |
| `VLM_MODEL_UNAVAILABLE` | Chromatogram vision model is not installed, activated, or loadable for VLM semantic/OCR-assist tasks. It must not stop deterministic graphPanel/plotArea/axis attempts. | Model readiness log, model availability diagnostics, runtime evidence package, validation run manifest, deterministic fallback stage timings. | Review before geometry; blocking only if fallback was not attempted or semantic VLM evidence is required for the claim. | Yes after model install/activation; deterministic fallback must still run | Yes after deterministic evidence exists | Yes if release report depends on VLM/Knowledge claims or fallback was not attempted |
| `MODEL_ASSET_MISSING` | Model registry/selection points to a missing local model file. | Model id, expected path if available, file existence/size diagnostics, load attempt result. | Review/blocking for VLM tasks | Yes after model placement/import | No until model is available | Yes if VLM evidence is required |
| `MODEL_LOAD_FAILED` | Local model file exists but runtime failed to load it. | Runtime backend, sanitized error, timeout/load log, model diagnostics. | Review/blocking for VLM tasks | Yes with fallback model or smaller crop/model | No until model is available | Yes if VLM evidence is required |
| `MODEL_NOT_CONFIGURED` | No selected or discoverable chromatogram VLM is configured for the package. | Model diagnostics, model manager state, validation package id. | Review for semantic layer | Yes after import/selection | No until model is selected | Yes if VLM evidence is required |
| `VLM_SEMANTIC_LAYER_UNAVAILABLE` | Deterministic analysis can run, but VLM semantic explanation/OCR fallback/overlay judging is unavailable. | Model diagnostics, stage judge status, report warnings. | Review | Yes after model install/activation | Yes for explanation review | No if deterministic evidence is complete and report makes no VLM-dependent claims |
| `CV_FALLBACK_GRAPH_PANEL_FAILURE` | Deterministic fallback ran after VLM unavailability but still could not find a graphPanel. | Deterministic candidate table, stage timings, model diagnostics, selected/rejected overlays if produced. | Blocking | Yes | Yes | Yes |
| `VLM_TIMEOUT` | VLM did not return before timeout. | Model runtime profile, timeout flag, fallback decision. | Review | Yes with smaller crop or lower model | Yes if semantic evidence needed | No if deterministic evidence is complete |
| `VLM_UNSUPPORTED_CLAIM` | VLM produced an unsupported scientific claim or numeric metric. | Rejected output, forbidden fields, validator finding. | Blocking | No, unless prompt/schema is fixed | Yes for explanation review | Yes |
| `REPORT_GATE_FAILURE` | Report gate status is missing or overclaims release readiness. | Report contract, gate evaluation, validator output. | Blocking | No | No | Yes |
| `EXPORT_PRIVACY_FAILURE` | User report includes raw logs, raw prompts, private paths, or never-shared artifacts. | Export manifest, HTML/Markdown/JSON output, privacy finding. | Blocking | No | No | Yes |
| `PERFORMANCE_TIMEOUT` | A stage exceeds configured runtime budget. | Stage timings, model runtime profile, timeout status. | Review | Yes with bounded retry | Yes if result unavailable | Blocks release only when evidence is incomplete |
| `UNKNOWN_FAILURE` | Failure cannot be assigned to a known class. | Full evidence package and reviewer notes. | Blocking | No until classified | Yes after classification | Yes |

## Graph-Stage Failure Packages

Phase 8D adds graph-stage failure packages for terminal failures after deterministic graph processing begins. A graph-stage failure package is required for:

- graphPanel, plotArea, axis, tick, OCR tick, calibration, and CV fallback graph failures;
- Android validation fixture terminal failures that reached graph processing but cannot produce a final graph report;
- any `BLOCKED` report where the selected graph context exists but calibrated signal/report generation is impossible.

The package must include graphPanel/plotArea bounds or explicit missing reasons, axis/tick candidate evidence, OCR crop evidence or missing reasons, accepted/rejected anchors, calibration status/residual summaries when available, warnings, failure class, failure stage, and stage timings.

## Phase 9C Multi-Fixture Clarifications

Phase 9C keeps `TICK_LOCALIZATION_FAILURE` as the correct blocking class when deterministic graph context exists but usable tick pixels and OCR values cannot be paired into calibration anchors. `GRAPH_PANEL_FAILURE` remains appropriate only when panel selection or multiplicity is the first failing evidence gate.

Model-enabled runs must not rewrite graph count, panel selection, tick geometry, calibration anchors, or numeric chromatographic metrics. If E2B/VLM disagrees with deterministic geometry, the result is review evidence only; dropping deterministic graph candidates without deterministic rejection evidence remains a runtime blocker.

For suite summaries, graph count must distinguish final report graphs from graph-level failure packages. A fixture can have zero report graphs but still have graph-level evidence; missing graph-level evidence for a graph-stage failure remains an export/evidence blocker.

## Failure Closure Policy

1. Attach evidence before changing expected status.
2. Identify the pipeline stage.
3. Decide `fix now`, `defer`, or `unsupported with product caveat`.
4. Do not turn a failure into expected behavior without QA, Product Acceptance, and domain signoff.
5. Critical overclaims, privacy leaks, missing evidence packages, and VLM/Knowledge numeric claims must be fixed before release readiness.

## Runtime Failure-Class Field

Phase 8B adds a first-class `runtimeFailureClass` field to runtime evidence packages and final report contract metadata. Rules:

- `runtimeFailureClass` must be absent when terminal state is `PASS`.
- `runtimeFailureClass` is required for non-pass terminal states.
- Runtime evidence package and embedded final report contract must agree.
- Fixture-driven Android validation must use these taxonomy values instead of generic failure strings.
## Phase 9D Failure Taxonomy Notes

Observed active failure classes:

- `TICK_LOCALIZATION_FAILURE`: still blocking `bench_01`, `bench_04`, `bench_05`, and `bench_06`. Evidence required: graph package, tick candidates, OCR crops, accepted/rejected anchors, and calibration invalid reason.
- `GRAPH_PANEL_FAILURE`: still used for `bench_02` metadata over-detection and low-confidence diagnostic graph cases. Evidence required: selected/rejected graphPanel candidates and multiplicity resolution.
- `VLM_SEMANTIC_LAYER_UNAVAILABLE`: acceptable as a deterministic-mode warning only when deterministic evidence remains available and no release-ready claim is made.

Phase 9D does not reclassify any BLOCKED fixture as expected unsupported input.
