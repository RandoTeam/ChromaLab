# ChromaLab Report Implementation Plan

Last updated: 2026-05-13

This document tracks the 10-phase plan for turning ChromaLab's chromatogram output into a professional, structured, offline report flow. Work must proceed one phase or one heavy subphase at a time. Each completed slice requires validation and a focused commit.

## Status Legend

- `[x]` Done and committed.
- `[~]` Partially done or wired, but not complete enough for release quality.
- `[ ]` Planned.

## Phase 1 - Report Contract And Calculation Bridge

Goal: define the target report shape, create a strict schema, render it, and connect deterministic `CalculationRun` data without inventing missing upstream values.

- [x] 1.1 Document the full report contract in `REPORT_SPEC.md`.
- [x] 1.2 Add the Belyi Tigr Ion 92 reference fixture notes.
- [x] 1.3 Add structured report schema models.
- [x] 1.4 Add Markdown renderer and contract validator.
- [x] 1.5 Add executable Belyi Tigr report fixture.
- [x] 1.6 Add rendered Belyi Tigr reference report.
- [x] 1.7 Map `CalculationRun` into the structured report contract.
- [x] 1.8 Expose structured Markdown export from calculation results.
- [x] 1.9 Add in-app structured report preview.
- [x] 1.10 Allow upstream metadata in report mapping.
- [x] 1.11 Pass available calculation/source metadata into report export.
- [x] 1.12 Add persisted report metadata contract for `algorithmConfig`.
- [x] 1.13 Record the 10-phase implementation plan.

Exit criteria:

- A calculation can produce a strict structured report.
- Missing crop/OCR/model/Kovats data is explicit, not hidden.
- The reference format is fixed enough that later work has a stable target.

## Phase 2 - Persist Real Processing Metadata

Goal: make the image-processing pipeline save the exact metadata that the report needs.

- [x] 2.1 Write `StoredReportMetadata` from `ProcessingFlowScreen` when a chromatogram is saved.
- [x] 2.2 Persist source image bounds, detected graph bounds, crop confidence, scan mode, and preprocessing steps.
- [~] 2.3 Persist OCR confidence for title, axis labels, and tick labels. Axis/tick confidence is persisted; title confidence remains empty until a dedicated title-OCR stage exists.
- [x] 2.4 Persist selected model, executed model, runtime, backend, device name, stage timings, and total duration.
- [x] 2.5 Persist per-graph warnings without converting them into final report prose too early.
- [x] 2.6 Ensure failed neural vision stages stop the full-analysis report instead of silently producing deterministic-only output.

Exit criteria:

- Report export reads real upstream metadata from storage.
- The report can show what happened during graph preparation and model execution.

## Phase 3 - Photo/Camera Graph Preparation Quality

Goal: make camera and Smart Scan gallery input produce clean graph crops before neural or deterministic analysis.

- [x] 3.1 Audit the current camera/Smart Scan path and document which platform scanner/crop/filter steps are used.
- [x] 3.2 Remove or avoid duplicate direct-photo flows that bypass the stronger preparation path.
- [x] 3.3 Normalize EXIF orientation and source dimensions before all graph detection.
- [x] 3.4 Test preprocessing variants: original, contrast, grayscale, sharpened, binary, scan-style.
- [x] 3.5 Select the best graph-preparation variant by measurable graph/axis/curve quality, not by visual guess.
- [x] 3.6 Store the chosen variant and rejected variants in metadata for audit.

Exit criteria:

- The model does not analyze a full screenshot/document page when a graph crop is required.
- The report knows which crop/filter path produced the final signal.

## Phase 4 - Multi-Graph Detection And Ordering

Goal: support one image or page containing multiple chromatogram graphs.

- [x] 4.1 Detect all graph regions that pass quality filters.
- [x] 4.2 Sort graphs in natural reading order.
- [x] 4.3 Run graph preparation, OCR, calibration, curve extraction, and calculation per graph.
- [x] 4.4 Produce output as graph 1/report 1, graph 2/report 2, graph 3/report 3.
- [x] 4.5 Prevent cross-contamination between graph metadata, peaks, warnings, and model responses.

Exit criteria:

- Multi-graph images do not collapse into one mixed report.
- Each graph has its own source metadata, signal, peak table, warnings, and interpretation.

## Phase 5 - Axis, OCR, And Calibration Hardening

Goal: make pixel-to-unit conversion auditable and accurate enough for real chromatographic calculations.

- [x] 5.1 Extract chromatogram title, ion/channel, sample label, X-axis label, Y-axis label, and tick labels.
- [x] 5.2 Convert OCR/model axis readings into structured calibration candidates.
- [x] 5.3 Validate X/Y tick monotonicity, spacing, and visible range.
- [x] 5.4 Persist `PixelToUnitTransform` and calibration confidence.
- [x] 5.5 Show clear warnings for weak OCR, missing ticks, tilted image, or inconsistent axis geometry.

Exit criteria:

- The report can explain exactly how pixels became retention time and abundance.
- Failed or weak axis extraction is visible and blocks release-quality claims.

## Phase 6 - Calculation Engine Regression Quality

Goal: prove that peak detection, integration, and metrics are correct for synthetic and real chromatograms.

- [~] 6.1 Apply boundary method, negative clamp, max peak width, and integration mode in `CalculationEngine`.
- [x] 6.2 Replace stale `commonTest` calculation tests that target old APIs.
- [ ] 6.3 Add synthetic fixtures with known peak RT, height, area, width, S/N, and overlap behavior.
- [ ] 6.4 Add real-photo fixtures, starting with Belyi Tigr Ion 92.
- [ ] 6.5 Validate the early dominant peak tension in the Belyi Tigr screenshot instead of blindly copying reference text.
- [ ] 6.6 Add regression gates for area percent, FWHM, baseline, noise, and confidence flags.

Exit criteria:

- Calculation output can be trusted before chemical interpretation is layered on top.
- Known broken/stale tests are replaced with current API coverage.

## Phase 7 - Local Knowledge Pack And Chemical Interpretation

Goal: make chemical interpretation offline, structured, and conservative.

- [ ] 7.1 Define local knowledge-pack schema for chromatogram types, ion fragments, compound classes, carbon numbers, and Kovats references.
- [ ] 7.2 Add `m/z 92` and alkylbenzene-oriented reference data.
- [ ] 7.3 Add n-paraffin reference series support for Kovats calculations.
- [ ] 7.4 Distinguish calculated, detected, inferred, local-knowledge, and model-suggested values.
- [ ] 7.5 Add warning rules for co-elution, contamination, weak baseline, weak crop, and unsupported model/runtime.

Exit criteria:

- The app does not rely on small-model memory for domain facts.
- Chemical conclusions show basis and confidence.

## Phase 8 - Professional Report UI And Export

Goal: make the user-facing report match the reference depth and read like a professional analytical document.

- [~] 8.1 Structured Markdown report exists.
- [~] 8.2 In-app structured preview exists.
- [ ] 8.3 Build final report screen with clear sections, readable tables, warnings, and technical appendix.
- [ ] 8.4 Add rendered graph overlay with signal, baseline, peaks, boundaries, and labels.
- [ ] 8.5 Add report metadata: graph preview, analysis time, model name, executed runtime, device, and stage timings.
- [ ] 8.6 Export polished Markdown/HTML/PDF-ready output from the same structured report.
- [ ] 8.7 Apply mobile UI/UX guardrails from `docs/MOBILE_UI_UX_GUARDRAILS.md`.
- [ ] 8.8 Convert technical audit warnings into compact user-facing quality states, with details kept in the report appendix.

Exit criteria:

- The final report is complete, readable, and traceable.
- It follows the reference format without becoming free-form LLM prose.
- The mobile UI stays minimal, visual, and task-focused; it must not expose raw debug/audit data as the primary experience.
- Camera/photo analysis works as an assisted automatic flow: the user should not have to manually align the graph with pixel-level precision.

## Phase 9 - Model Runtime Separation And GGUF/VLM Reliability

Goal: keep chromatography models, chat models, OCR-only models, LiteRT, and GGUF behavior separate and honest.

- [~] 9.1 Shared downloaded/custom model pool exists.
- [~] 9.2 Chat hides OCR/document-only models from normal text assistant selection.
- [ ] 9.3 Enforce that full chromatogram photo analysis requires a vision-capable model.
- [ ] 9.4 Enforce GGUF base model plus matching `mmproj` before image analysis.
- [ ] 9.5 Record selected runtime and actual executed runtime in report metadata.
- [ ] 9.6 Diagnose and fix empty GGUF chat responses and stuck GGUF image-analysis stages.
- [ ] 9.7 Prevent LiteRT from being silently used when a GGUF model was selected.

Exit criteria:

- The app never pretends that the selected model/runtime was used when another engine actually ran.
- GGUF failures are clear and actionable.

## Phase 10 - Alpha Release Validation

Goal: ship the next alpha only after the full flow is reproducible on target devices.

- [ ] 10.1 Define alpha validation checklist for camera, Smart Scan gallery, imported files, models, chat, reports, and exports.
- [ ] 10.2 Test on the current strong device with LiteRT reference models.
- [ ] 10.3 Test on Xiaomi Mi 8 or another weak device without weakening analysis depth.
- [ ] 10.4 Confirm that weak devices fail honestly when required neural stages cannot complete.
- [ ] 10.5 Build debug and release artifacts.
- [ ] 10.6 Update README, roadmap, pipeline docs, release notes, and known issues.
- [ ] 10.7 Commit, tag, push, and publish the alpha release.

Exit criteria:

- The release notes describe what works, what is experimental, and what is blocked.
- The alpha build can be installed and tested without losing the scientific audit trail.

## Current Completed Commit Trail

- `b77828f` - Document chromatogram report contract.
- `367ce9d` - Add Belyi Tigr report reference fixture.
- `bf704b3` - Add chromatogram report schema models.
- `02ca281` - Add report renderer and contract validator.
- `22f6ea7` - Add Belyi Tigr executable report fixture.
- `4b0445f` - Add Belyi Tigr rendered report reference.
- `0bb15d9` - Map calculation runs to report contract.
- `0e1d242` - Expose structured report markdown export.
- `2bb8ac7` - Add structured report preview.
- `a15d669` - Allow upstream metadata in report mapping.
- `c123e8d` - Pass calculation metadata to report export.
- `791e556` - Persist report metadata contract.

## Next Recommended Slice

Start Phase 6.3: add synthetic fixtures with known peak RT, height, area, width, S/N, and overlap behavior.
