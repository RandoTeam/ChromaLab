# Autonomous Analyzer Layer Replacement Roadmap

Date: 2026-06-07

Status: `R15_ROADMAP_UPDATED`

Scope: roadmap status tracking. This roadmap does not add runtime dependencies,
does not modify `CalculationEngine`, and does not change chromatographic math,
Android analyzer behavior, model policy, or report gates.

## Core Decision

ChromaLab should not keep adding parallel analysis layers. The modernization
path is layer replacement with strict gates:

```text
inventory old layer
-> define target contract
-> build replacement in isolated/shadow mode
-> compare against truth corpus
-> promote only if gates pass
-> retire old path and stale docs
-> commit one completed slice
```

Shadow mode is allowed only for validation and parity measurement. It must not
become a permanent second production path.

## Why The Previous Plan Needed Tightening

The previous TurboVec plan correctly identified that TurboVec/TurboQuant is not
a graph/axis/calibration algorithm. It was still too loose in one area: it
described TurboVec as an optional layer. That is acceptable for research, but it
is not acceptable as the final architecture.

The corrected rule is:

- during research, run old and new side by side only to measure;
- during production, keep one active implementation per responsibility;
- when a replacement is accepted, update source-of-truth docs and remove or
  clearly retire stale references;
- when a replacement fails, remove the experimental runtime path and keep only
  the evidence/report explaining why.

## Non-Negotiable Architecture Rules

1. One responsibility has one production owner.
2. A replacement must define its contract before code changes.
3. A replacement must be benchmarked against real fixtures before promotion.
4. A replacement must not silently lower validator strictness.
5. A replacement must not hide BLOCKED as REVIEW.
6. A replacement must not hardcode fixture coordinates, run ids, image sizes, or
   expected peak counts.
7. Old code paths may remain temporarily only behind a named parity/shadow gate.
8. Old docs must be marked historical or updated when ownership changes.
9. Generated artifacts stay ignored unless policy explicitly says otherwise.
10. Every phase ends with validation, documentation, and a focused commit.

## Layer Replacement Protocol

Every future phase must follow this protocol.

### 1. Inventory

Record:

- current owner files;
- current source-of-truth docs;
- current tests;
- current Android/desktop artifacts;
- current known blockers;
- current stale or historical docs that could mislead future agents.

Output:

- a layer inventory table;
- a list of files that are active, historical, generated, or deprecated.

### 2. Contract

Define:

- input schema;
- output schema;
- evidence artifacts;
- validator expectations;
- report-gate impact;
- forbidden behavior;
- rollback behavior.

No implementation starts before this contract exists.

### 3. Isolated Replacement

Build the new layer without changing production authority:

- Rust or TurboVec prototype may run off to the side;
- Kotlin production path remains unchanged;
- all outputs must be comparable to the existing contract;
- no result is accepted without evidence.

### 4. Shadow/Parity Run

Run old and new paths on the same corpus:

- all eight Android validation fixtures where applicable;
- PC fixture corpus;
- known failure cases;
- deterministic and E2B modes when model semantics are relevant.

Record:

- correctness;
- speed;
- memory;
- artifact completeness;
- validator impact;
- product/scientific usability.

### 5. Promotion Gate

The new layer becomes production owner only if:

- it meets or exceeds old correctness on accepted fixtures;
- it does not regress White Tiger / bench_03 / bench_07 style witnesses;
- it improves or precisely classifies blocked fixtures;
- it preserves evidence completeness;
- it preserves or improves performance enough for the target mode;
- Product, QA, and Scientific acceptance do not block it.

### 6. Retirement

After promotion:

- remove or disable the old production path;
- update source-of-truth docs;
- mark old phase docs as historical when needed;
- remove misleading references from README/docs index;
- keep only necessary regression artifacts and summaries.

### 7. Closeout

Every phase must close with:

- changed files;
- exact active owner after the phase;
- tests and Android/PC validation;
- remaining blockers;
- next phase recommendation;
- commit hash.

## Replacement Stack

The stack below is ordered. Do not skip ahead to lower layers until the current
layer has an owner, a truth corpus, and a replacement gate.

| Order | Layer | Current problem | Replacement target | TurboVec/TurboQuant role | Promotion gate |
|---:|---|---|---|---|---|
| 0 | Source-of-truth control | Many phase docs exist; some are historical and can mislead future work. | Active/historical/deprecated file map and layer owner board. | Index docs only after source-of-truth map exists. | Every active layer has one owner doc and one code owner path. |
| 1 | Truth corpus and fixture registry | Product truth is fragmented across phase reports and artifacts. | Canonical corpus registry with expected layout, graph count, evidence status, and artifact policy. | Later retrieval over corpus notes. | Every fixture has metadata, expected class, and current result. |
| 2 | Evidence/export validation | Runs can look complete while missing the evidence users need. | Evidence package validator as the product truth gate. | Retrieve historical failure templates only. | Missing evidence fails; no silent timeout/no export. |
| 3 | Image preparation | Old preprocessing may be heavy and not ranked by evidence. | Rust image preparation contract with parity against current fixtures. | No direct runtime role. | New prep beats or equals old on graph discovery inputs. |
| 4 | Graph layout and graph count | 0-graph and wrong-count cases remain. | Rust graph layout classifier and candidate resolver. | Retrieve layout rules only. | Correct graph count or precise unsupported class. |
| 5 | PlotArea and axis/frame evidence | Plot area/axis evidence is inconsistent on real images. | Rust plot/frame/axis evidence contract. | No numeric role. | Axis/frame evidence improves without report overclaim. |
| 6 | Scale/calibration | Tick-only and single-strategy approaches regress. | Calibration strategy ensemble with deterministic arbitration. | No numeric role. | Usable old calibration cannot be replaced by invalid new result. |
| 7 | OCR label and semantic classification | OCR text can contaminate tick labels and ion/title evidence. | Local crop OCR + rule classifier + model advisory text. | Knowledge retrieval for classification caveats. | Text cannot create pixel geometry or metrics. |
| 8 | Trace extraction | Sparse/dense/stacked trace handling is incomplete. | Rust trace mask/centerline evidence with parity tests. | No direct role. | Trace evidence complete or precise failure. |
| 9 | Peak evidence | Metrics depend on upstream correctness and need stronger flags. | Evidence-rich peak review around existing calculation math. | Report caveat retrieval only. | No metric changes without isolated proof. |
| 10 | Knowledge retrieval | Lexical retrieval is safe but limited for larger local knowledge. | Replace lexical ranker with gated hybrid or dense backend only after benchmark proof. | Primary candidate layer. | Dense retrieval improves semantic tasks and old ranker is retired or demoted to policy filter. |
| 11 | Model semantic layer | E2B must help without becoming authority. | E2B baseline semantic mode with cited bounded context. | Supplies compact context cards. | No geometry/calibration/metric/report gate regression. |
| 12 | Report and user truth | Reports can look polished without product clarity. | Truth-first report with evidence tables and clear blocked/review status. | Grounded explanations only. | User can see what worked and what did not. |

## TurboVec Replacement Position

TurboVec should not be treated as "one more optional retrieval helper" forever.
It has one possible ownership target:

```text
Knowledge retrieval ranking/storage for local semantic context.
```

It does not replace:

- `KnowledgeUsePolicyValidator`;
- deterministic rule classification;
- report validators;
- graph/axis/calibration/trace/peak stages.

Correct final shape if TurboVec passes:

```text
Knowledge rules and policy filters
-> TurboVec-backed retrieval/ranking
-> bounded citation cards
-> E2B/E4B semantic prompt
-> used_entry_ids / unsupported_claims validation
```

Correct final shape if TurboVec fails:

```text
Knowledge rules and policy filters
-> existing lexical retrieval
-> bounded citation cards
```

In both cases, there is one active retrieval owner. The rejected path is not kept
as a confusing runtime alternative.

## Stale-File Control

Each replacement phase must update a file-state table.

Allowed file states:

- `ACTIVE_SOURCE_OF_TRUTH`
- `ACTIVE_IMPLEMENTATION`
- `ACTIVE_TEST`
- `HISTORICAL_REFERENCE`
- `GENERATED_IGNORED_ARTIFACT`
- `DEPRECATED_PENDING_REMOVAL`
- `REMOVED`

Rules:

- README and `docs/README.md` should link only to active source-of-truth or
  clearly historical documents.
- Historical phase docs may remain, but they must not be presented as current
  behavior.
- If a new layer becomes active, the old layer's docs must be updated in the same
  phase or the phase is not complete.
- Generated validation artifacts should remain under ignored `artifacts/` unless
  a policy explicitly promotes a small summary file.

## Automatic Checks Per Phase

Minimum checks before moving to the next layer:

- `git diff --check`;
- targeted unit tests for the changed layer;
- source-of-truth doc updated;
- active/historical file map updated;
- no unrelated dirty files staged;
- no validator weakening;
- no CalculationEngine change unless separately approved.

Additional checks by layer:

| Layer type | Required check |
|---|---|
| Rust CV | Rust format/check/test or documented linker blocker, plus Kotlin parity test if bridge is touched. |
| Android runtime | `assembleAndroidMain`, validation APK build, targeted Android fixture rerun. |
| Knowledge retrieval | retrieval benchmark, forbidden-use tests, citation tests. |
| Model/E2B | deterministic vs E2B comparison and forbidden numeric field audit. |
| Report/export | report tests, manifest checks, privacy/export review. |

## Current Replacement Progress

Completed:

- `R0 - Source-of-Truth And Layer Inventory`;
- `R1 - Graph/Layout And Image Preparation Replacement Contract`;
- `R2 - Stage 1-3 Shadow Parity Harness`;
- `R3 - Stage 1 Image Preparation Candidate`;
- `R4 - Rust Stage 1 Image Preparation Parity Bridge`;
- `R5 - Stage 2 Graph Discovery Candidate`;
- `R6 - Stage 3 PlotArea And Layout Semantics Candidate`;
- `R7 - Stage 4 Axis, Frame, And Scale Evidence Candidate`;
- `R8 - Stage 5 Calibration Strategy Parity Candidate`;
- `R9 - Stage 6 Automatic OCR Anchor Candidate`;
- `R10 - Stage 6 Runtime OCR Anchor Bridge Candidate`;
- `R11 - Integrated Runtime Calibration Closure`;
- `R12 - Runtime Evidence And Failure Package Closure`;
- `R13 - Android Runtime OCR Anchor Production Bridge`;
- `R14 - Runtime Calibration Promotion Candidate`;
- `R15 - Graph Layout And Multi-Panel Runtime Closure`.

R0 established source-of-truth control. R1 defined the Stage 1-3 contract. R2
added schema-backed PC shadow parity records and reports. R3 added a PC-side
Stage 1 candidate with normalized-image hashes, preprocessing variant ranking,
quality metrics, preview artifacts, and schema-backed records. R4 added a
Rust Stage 1 parity bridge with 8/8 selected-variant parity and 8/8 PASS/REVIEW
status parity against R3. R5 added a Stage 2 graph discovery candidate with 8/8
graph-count pass in shadow mode. R6 added a Stage 3 plotArea/layout candidate
with 8/8 layout-class pass and REVIEW-only plotArea evidence. R7 added Stage 4
axis/frame/scale evidence with 12 P0 annotated manual-review scale graphs and
REVIEW-only axis/frame/scale output. R8 added Stage 5 calibration strategy
parity with 12 selected manual-review scoring fits and REVIEW-only calibration
strategy output. R9 added Stage 6 automatic OCR anchor candidate evidence with
12 automatic OCR candidate graphs, 9 valid graph decisions, 3 review graph
decisions, and 155 accepted OCR anchors. R10 added a Rust/runtime-shaped OCR
anchor bridge candidate with 8 records, 4/4 scoreable fixture parity, 155
accepted bridge rows, and 20 rejected bridge rows. R10 remains REVIEW because
the source crop image files are not persisted and Android runtime generation is
not yet proven. R11 added shadow calibration closure records from R10 bridge
rows, selected 12 graph calibration fits from 155 accepted bridge anchors, and
kept the same crop-file/runtime-generation blocker explicit. R12 added
evidence/export closure checks over 16 Phase 9J-derived Android records,
recording 16/16 core artifact completeness, 0 no-export states, 4/4 blocked
runs with graph failure packages, and 0 release-ready runs.

None of these phases changed Android runtime behavior, validators,
chromatographic math, report gates, graph-count metadata, model policy, or
`CalculationEngine`.

## Current Completed Slice

R13 added Android/runtime OCR-anchor bridge rows to runtime evidence packages.
Rows now carry graph id, axis, OCR text/value, deterministic pixel coordinate,
crop path or missing-crop reason, geometry/numeric source, status, and rejection
reason. Validator coverage blocks unsafe accepted rows and VLM numeric
authority. R13 remains evidence/export work only.

R14 added those Android/runtime OCR-anchor rows as a named calibration strategy
candidate inside `CalibrationStrategyEnsemble`. The strategy records coordinate
frames, converts image-absolute rows to plot-relative coordinates, rejects
unsafe rows before fitting, and exports selected/rejected strategy summaries in
runtime evidence. R14 remains a promotion candidate and does not accept Phase 9.

R15 added runtime multi-panel graph propagation. `GeometryPipelineResult` now
has per-graph `graphResults`; multi-panel runs use
`GraphMultiplicityResolution.resolvedGraphPanels` as the source of physical
graph units; TIC+ion text hints are semantic-only; and stored per-graph reports
emit `multi_panel_report_aggregation_unsupported` when a combined multi-graph
report is not represented.

## Next Phase To Run

The next phase should still not be TurboVec dependency installation and should
not directly switch production to Rust.

The next phase should be:

```text
R16 - Trace Extraction Evidence Candidate
```

Purpose:

- define trace mask/centerline evidence after upstream graph layout and
  calibration evidence are stable enough;
- compare trace overlays across sparse, dense, stacked, and multi-panel cases;
- keep `CalculationEngine` untouched.

Deliverables:

- trace evidence contract;
- shadow trace candidate and overlays;
- trace failure reasons for unsupported cases;
- no fixture-specific coordinate hardcoding;
- source-of-truth docs updated;
- PC/Android validation command output when runtime files are touched;
- focused commit.

If Android rerun evidence shows R15 still leaves a critical combined multi-graph
report aggregation gap, close that R15 blocker before starting R16.
