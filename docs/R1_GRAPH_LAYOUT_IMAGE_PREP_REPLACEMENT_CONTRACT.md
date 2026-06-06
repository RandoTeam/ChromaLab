# R1 Graph/Layout And Image Preparation Replacement Contract

Date: 2026-06-06

Status: `R1_CONTRACT_READY`

Scope: Stage 1-3 contract only. This document does not change runtime behavior,
validators, report gates, chromatographic math, `CalculationEngine`, Android
model policy, or fixture expectations.

## Purpose

R1 defines how ChromaLab will replace the current image preparation,
graph-discovery, and plot-area/layout layers without adding a permanent second
production path.

The current Android truth audit shows that the remaining product blockers are
not solved by adding another model or another retrieval layer. They are upstream
analysis blockers:

- graph/page preparation is inconsistent across screenshots and photos;
- graph count and panel grouping are not stable enough on multi-panel cases;
- plotArea ownership is still weak on TIC plus ions and printed-page fixtures;
- downstream axis, calibration, trace, and peak failures are often caused by
  unstable Stage 1-3 outputs.

R1 therefore covers:

1. Stage 1: image preparation.
2. Stage 2: graph discovery.
3. Stage 3: plotArea and graph layout semantics.

## Orchestration Start Record

### Task Classification

| Domain | Included |
|---|---|
| Product architecture | Replacement contract and promotion gates. |
| Geometry / ROI / plotArea | Stage 2-3 graphPanel, plotArea, graph count, layout. |
| Runtime evidence / validator | Evidence requirements for future implementation. |
| Android performance | Future runtime budget, timeout, and artifact requirements. |
| QA / regression / golden artifacts | Fixture parity and no-regression gates. |
| Product acceptance | Product truth table must drive promotion. |
| Scientific reporting / provenance | Report gates must remain evidence-gated. |
| Security / privacy / storage / export | Artifact paths and diagnostic exports stay separated. |

### Activated Agents

| Agent | Role in R1 |
|---|---|
| Orchestrator | Owns scope, phase boundaries, and replacement order. |
| Research Intelligence Agent | Confirms the replacement direction against current graph-digitization practice and existing research docs. |
| Geometry / Calibration Core Agent | Owns graphPanel, plotArea, layout, and downstream geometry compatibility. |
| QA / Regression Agent | Owns fixture corpus, parity gates, and no-regression criteria. |
| Android Performance & On-Device AI Agent | Owns future Android runtime budget, timeout, and artifact export constraints. |
| Scientific Reporting & Validation Agent | Owns report-gate honesty and evidence provenance. |
| Product Acceptance Agent | Owns whether a Stage 1-3 output is useful for autonomous product goals. |
| Security & Privacy Agent | Owns local artifact/export boundaries. |
| Rust CV Integration Agent | Owns future Rust/Kotlin bridge direction and shadow-mode constraints. |

### Skills Used

| Skill | Why it applies |
|---|---|
| current-web-research-deep | R1 checks current graph digitization and CV method direction before replacement. |
| source-quality-triage | External method references are treated as design input, not production proof. |
| research-synthesis | R1 merges external references, previous DR docs, Phase 9J truth, and current code ownership. |
| geometry-calibration-robust-fit | Stage 1-3 outputs must be stable enough for axis and calibration stages. |
| evidence-package-validator | Future implementation must preserve complete graph-level evidence. |
| regression-benchmark-golden | Replacement must be benchmarked against current fixtures and Phase 9J truth records. |
| report-gate-provenance | No Stage 1-3 success can overclaim report readiness. |
| android-runtime-profiling | Future runtime implementation must be bounded on device. |
| timeout-cache-design | The replacement layer must not recreate bench_01 timeout/no-export behavior. |
| log-safety-audit | Diagnostic artifacts must avoid leaking private paths or user data into reports. |
| secure-export-review | Evidence packages remain diagnostic, not user-report payload by default. |
| test-plan-authoring | This contract defines the test plan before code. |
| definition-of-done | Promotion requires parity, evidence, and retirement decisions. |

### Intentionally Not Activated

| Agent | Reason |
|---|---|
| Trace Extraction / Peak Review Agent | Stage 6 depends on Stage 1-3 but is not changed in R1. |
| Chromatography SME Agent | Consulted through current layout taxonomy and fixture metadata; no graph-count metadata is changed in R1. |
| Compose KMP UI Agent | No UI/runtime change. |
| Visual Design System Agent | No report or UI appearance change. |
| Accessibility & Localization Agent | No user-facing strings changed. |
| VLM Evaluation Agent | E2B/VLM policy is already fixed: advisory only. R1 does not change model behavior. |

## External Method Anchors

R1 uses external method references only to shape the contract. They are not a
claim that ChromaLab already implements those methods.

- WebPlotDigitizer documents a graph-digitization workflow that starts with
  image load, axis calibration, ROI definition, and then extraction. ChromaLab's
  autonomous version must produce equivalent evidence automatically rather than
  hiding it behind a final report.
- OpenCV documents line detection through edge preprocessing plus Hough
  transforms. R1 keeps line/frame/axis evidence as deterministic CV evidence,
  not LLM output.
- Rust `imageproc` exposes Hough line detection on binary images, making it a
  reasonable Rust-side primitive candidate for future Stage 1-3 parity work.
- Plot2Spectra describes automatic plot extraction as plot-region detection,
  edge-based axis refinement, scene text/tick interpretation, then line
  extraction. This supports ChromaLab's decision to stabilize plot region and
  layout before trace/peak work.

Detailed references are saved in
`docs/research/2026-06-06_r1_graph_layout_image_prep_references.md`.

## Current Active Owners

| Layer | Active owner paths | R1 classification |
|---|---|---|
| Image normalization | `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/normalize/` and Android actuals | `ACTIVE_IMPLEMENTATION` |
| Image preprocessing variants | `processing/preprocess/` | `ACTIVE_IMPLEMENTATION` |
| Image quality | `processing/quality/` | `ACTIVE_IMPLEMENTATION` with user-fallback semantics that must not become autonomous proof. |
| Document/page detection | `processing/document/` | `EXPERIMENTAL_SHADOW_OR_FALLBACK` until parity proves page rectification. |
| Perspective correction | `processing/perspective/` | `EXPERIMENTAL_SHADOW_OR_FALLBACK` until homography evidence is stable. |
| Graph candidate generation | `processing/graph/GraphRegionDetector*` | `ACTIVE_IMPLEMENTATION` but too lenient for autonomous acceptance. |
| Embedded screenshot detection | `processing/geometry/ScreenshotEmbeddedChartDetector.kt` | `ACTIVE_IMPLEMENTATION` |
| Graph multiplicity | `processing/geometry/GraphMultiplicityResolver.kt` | `ACTIVE_IMPLEMENTATION` and R1 priority target. |
| Layout classification | `processing/geometry/GraphLayoutClassifier.kt` | `ACTIVE_IMPLEMENTATION` and R1 priority target. |
| PlotArea detection | `processing/geometry/GraphPlotAreaDetector.kt` | `ACTIVE_IMPLEMENTATION` and R1 priority target. |
| App analysis flow | `processing/flow/ProcessingFlowScreen.kt` then `processing/sweep/AutoSweepEngine.kt` | `ACTIVE_IMPLEMENTATION`; Stage 1-3 replacement must avoid accidentally changing curve scoring. |
| Stage 1-5 orchestration | `processing/geometry/GeometryPipelineRunner.kt` | `ACTIVE_IMPLEMENTATION`; future R1 code must integrate here only after shadow parity. |
| Desktop/bench duplicate flow | `processing/bench/OfflineAnalysisRunner.kt` | `EXPERIMENTAL_SHADOW`; useful for parity, not app authority. |
| Rust CV bridge | `rust/chromalab-cv-core/` and Android Rust bridge files | `EXPERIMENTAL_SHADOW`; not production owner yet. |

## Legacy Behavior To Control

The current graph path contains fallback behavior that is useful for assisted
review but risky for autonomous product claims.

| Behavior | Risk | R1 rule |
|---|---|---|
| Full-image or lenient graph fallback | Can turn a failed graph search into a false autonomous candidate. | Keep only as diagnostic/review evidence unless candidate completeness is proven. |
| `canProceed` style flags that always allow continuation | Can hide Stage 1-3 failure until calibration or trace. | Stage 1-3 contract must separate user fallback from autonomous evidence gates. |
| Page/perspective correction without clear artifact evidence | Can shift graph bounds and break calibration. | Promote only with normalized image, transform matrix, residuals, and overlay evidence. |
| VLM graph hints | Can bias candidate selection without geometry. | Advisory only; cannot set graph count, graphPanel, plotArea, or calibration geometry. |
| Multiple overlapping/nested candidates | Can create one physical graph as multiple reports. | Require IoU/nesting collapse, rejection reasons, and physical graph count evidence. |

## Replacement Contract

### Stage 1 Output: Image Preparation

Future Stage 1 implementation must return a structured result equivalent to:

```text
AnalyzerImagePreparationResult
- sourceProvenance
- sourceImageDimensions
- normalizedImagePath
- candidateVariants[]
- selectedVariant
- rejectedVariants[]
- orientationDecision
- pageRectificationDecision
- qualityMetrics
- warnings[]
- failureSubreason?
- evidenceArtifacts[]
- timing
```

Required evidence:

- original image path or asset id;
- normalized image path;
- dimensions before and after normalization;
- orientation decision;
- page/rectangle candidate table when applicable;
- selected preprocessing variant and score;
- rejected preprocessing variants with reasons;
- quality metrics and warnings;
- artifact paths for visual review.

### Stage 2 Output: Graph Discovery

Future Stage 2 implementation must return a structured result equivalent to:

```text
AnalyzerGraphDiscoveryResult
- imagePreparationId
- graphPanelCandidates[]
- selectedGraphPanels[]
- rejectedGraphPanels[]
- physicalGraphCount
- expectedGraphCount?
- graphMultiplicityStatus
- selectionReason
- failureSubreason?
- evidenceArtifacts[]
- timing
```

Required evidence:

- candidate bounds in normalized image coordinates;
- candidate confidence and score breakdown;
- selected/rejected candidate overlays;
- rejection reasons for text blocks, legends, partial panels, nested duplicates,
  full-image fallbacks, and separator ambiguity;
- physical graph count and report graph count;
- explicit graph-count mismatch classification when applicable.

### Stage 3 Output: PlotArea And Layout

Future Stage 3 implementation must return a structured result equivalent to:

```text
AnalyzerGraphLayoutResult
- graphPanelId
- layoutClass
- physicalGraphCount
- reportGraphCount
- panelGroups[]
- traceGroups[]
- selectedPlotArea
- rejectedPlotAreas[]
- axisFrameEvidence
- labelBandEvidence
- layoutConfidence
- warnings[]
- failureSubreason?
- evidenceArtifacts[]
- timing
```

Required evidence:

- selected graphPanel overlay;
- selected plotArea overlay;
- rejected plotArea candidate table;
- frame/axis/label-band evidence;
- layout class and confidence;
- panel grouping for stacked traces, TIC plus ions, and two-graph pages;
- rejection reasons for graph/text/legend confusion.

## Status And Failure Vocabulary

Stage 1-3 may produce these statuses:

- `VALID`: strong deterministic evidence and no product/scientific blocker.
- `REVIEW`: usable downstream, but with caveats that must remain visible.
- `DIAGNOSTIC`: evidence exists but not sufficient for scientific report claims.
- `BLOCKED`: no safe autonomous continuation.

Stage 1-3 failure classes and subreasons must be explicit:

| Failure class | Example subreasons |
|---|---|
| `IMAGE_PREPARATION_FAILURE` | `SOURCE_IMAGE_UNREADABLE`, `NORMALIZATION_FAILED`, `LOW_RESOLUTION_UNRECOVERABLE` |
| `PAGE_RECTIFICATION_REVIEW` | `PAGE_BOUNDS_AMBIGUOUS`, `PERSPECTIVE_RESIDUAL_HIGH`, `RECTIFICATION_NOT_APPLIED` |
| `GRAPH_PANEL_FAILURE` | `NO_GRAPH_PANEL_CANDIDATE`, `PARTIAL_PANEL_SELECTED`, `FULL_IMAGE_FALLBACK_ONLY` |
| `GRAPH_COUNT_MISMATCH` | `EXPECTED_MULTI_GRAPH_NOT_SPLIT`, `ONE_GRAPH_SPLIT_INTO_MULTIPLE`, `NESTED_DUPLICATE_CANDIDATES` |
| `PLOT_AREA_FAILURE` | `PLOT_AREA_MISSING`, `PLOT_AREA_EQUALS_GRAPH_PANEL`, `PLOT_AREA_OUTSIDE_PANEL` |
| `LAYOUT_CLASSIFICATION_REVIEW` | `STACKED_VS_MULTI_PANEL_AMBIGUOUS`, `TIC_ION_PANEL_GROUPING_AMBIGUOUS` |
| `EVIDENCE_EXPORT_FAILURE` | `MISSING_GRAPH_PANEL_OVERLAY`, `MISSING_PLOT_AREA_OVERLAY`, `MISSING_CANDIDATE_TABLE` |

## Fixture Contract

R1 uses current metadata as the product contract. It does not change expected
graph counts.

| Fixture | Expected graph count | Required Stage 1-3 behavior |
|---|---:|---|
| `white_tiger_ion71` | 1 | Must remain one graph and must not regress from REVIEW_ONLY downstream behavior. |
| `bench_01_mz71_screenshot_page` | 2 | Must identify two graph units or fail with graph-count/panel evidence before calibration. |
| `bench_02_mz92_belyi_tigr` | 1 | Must not split one physical graph into multiple reports. |
| `bench_03_small_tic_export` | 1 | Must preserve low-resolution REVIEW behavior. |
| `bench_04_stacked_xic_resolution` | 4 | Must preserve stacked panel grouping and report graph propagation. |
| `bench_05_tic_plus_ions` | 4 | Must preserve TIC/ion panel separation and not treat channel text as graph geometry. |
| `bench_06_photo_two_graphs_page` | 2 | Must support photo/page preparation and two graph panels. |
| `bench_07_rotated_page_photo` | 1 | Must preserve rotated-page REVIEW behavior. |

## Rust/Kotlin Boundary

R1 does not mandate that all Stage 1-3 code is Rust immediately. It defines the
boundary for future work:

- Rust may own deterministic pixel-heavy primitives: grayscale/threshold
  variants, line/frame candidates, contour components, projection profiles,
  geometry scoring, and candidate tables.
- Kotlin may own orchestration, evidence packaging, Android asset/runtime
  paths, product gates, and interop with ML Kit/VLM.
- Rust output must be serializable and comparable to benchmark records before it
  can become the production owner.
- Old Kotlin paths remain available only as shadow or fallback until parity is
  proven.

Current Rust status:

- `rust/chromalab-cv-core/` exposes JNI smoke diagnostics and
  `DR2F_AXIS_ELEMENT_CROP_PLAN_V1`;
- it does not currently own image decode/preprocessing, graphPanel detection,
  boundary correction, plotArea detection, multiplicity resolution, layout
  classification, calibration, trace, peaks, or reports;
- R2 must therefore add a Stage 1-3 parity record shape before any Rust
  production switch.

Current app flow constraint:

- `ProcessingFlowScreen` prepares the source image and calls `AutoSweepEngine`;
- `AutoSweepEngine` runs graph/layout, OCR/axis, preprocessing variants, curve
  scoring, and report-adjacent evidence;
- replacing Stage 1-3 must not silently change Stage 6 curve inputs or
  `CalculationEngine` downstream math.

## Promotion Gates

A new Stage 1-3 implementation may be promoted only when all gates pass:

1. It runs in shadow mode against all eight Android validation fixtures.
2. It emits Stage 1-3 evidence packages for deterministic and E2B modes.
3. It does not regress `white_tiger_ion71`, `bench_03_small_tic_export`, or
   `bench_07_rotated_page_photo`.
4. It improves or more precisely classifies `bench_01_mz71_screenshot_page` and
   `bench_05_tic_plus_ions`.
5. It preserves expected graph counts unless Product, QA, and Scientific sign
   off on metadata changes with visual evidence.
6. E2B/VLM cannot erase or replace deterministic graph/layout output.
7. Full-image fallback cannot be selected as autonomous success without
   candidate completeness evidence.
8. Runtime evidence validator sees graphPanel, plotArea, candidate table,
   rejection reasons, and overlays.
9. Android runtime has timeout and export guarantees.
10. Old production owner is demoted or retired in the same broad slice so the
    app does not accumulate two competing permanent paths.

## Retirement Rules

When a replacement is promoted:

- mark old owners as `LEGACY_SHADOW` or remove them if no longer referenced;
- delete tests that only assert obsolete fallback behavior;
- keep historical phase docs as archive only;
- update `docs/AUTONOMOUS_ANALYZER_SOURCE_OF_TRUTH_INDEX.md`;
- update `docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md`;
- update regression matrix and product/scientific acceptance docs;
- do not leave stale files that future code can accidentally call.

## R1 Decision

R1 closes as a contract and parity-planning phase, not an implementation phase.

Next implementation slice should be:

```text
R2 - Stage 1-3 Shadow Parity Harness
```

R2 should run old active Stage 1-3 output and the new Rust/Kotlin candidate
contract side-by-side on the fixture corpus, produce metrics, and only then
allow production integration.
