# R1 Graph/Layout And Image Preparation Closeout

Date: 2026-06-06

Verdict: `R1_CONTRACT_READY_R2_CAN_START`

Scope completed: replacement contract and parity plan for Stage 1-3. No code,
runtime behavior, validators, report gates, graph-count metadata,
chromatographic math, or `CalculationEngine` were changed.

## Workstream Output

| Workstream | Output |
|---|---|
| Orchestrator | Bound R1 to Stage 1-3 and prevented a new permanent duplicate layer. |
| Geometry / Calibration | Defined graphPanel, plotArea, layout, graph-count, and downstream-compatibility contracts. |
| QA / Regression | Defined eight-fixture parity corpus, regression witnesses, and promotion gates. |
| Android Runtime | Captured timeout/export requirements for future implementation. |
| Rust CV | Defined Rust as a future deterministic primitive owner, not immediate production owner. |
| Product Acceptance | Preserved Phase 9J product truth and blocked/review states. |
| Scientific Reporting | Preserved report-gate honesty and evidence requirements. |
| Security / Privacy | Kept diagnostic artifacts separate from user reports and avoided private path exposure in contract output. |
| Research | Cross-checked current graph-digitization/CV direction and saved source notes under `docs/research/`. |

## Files Created

| Path | Role |
|---|---|
| `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md` | Stage 1-3 replacement contract. |
| `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md` | Stage 1-3 parity and promotion plan. |
| `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md` | R1 closeout and next-step decision. |
| `docs/research/2026-06-06_r1_graph_layout_image_prep_references.md` | External method reference notes. |

## Files Updated

| Path | Change |
|---|---|
| `docs/AUTONOMOUS_ANALYZER_SOURCE_OF_TRUTH_INDEX.md` | Promoted R1 docs as active replacement contracts. |
| `docs/AUTONOMOUS_ANALYZER_LAYER_OWNER_BOARD.md` | Updated R1 status and next implementation slice. |
| `docs/README.md` | Added R1 docs to the documentation index. |

## Important Decisions

1. R1 does not implement new algorithms.
2. Stage 1-3 must be replaced through shadow parity, not by copying old Kotlin
   heuristics into Rust.
3. Full-image fallback and always-proceed behavior may remain assisted/review
   tools, but cannot be counted as autonomous graph success.
4. Rust can own deterministic pixel-heavy primitives after parity.
5. Kotlin remains the right owner for orchestration, evidence packaging,
   Android paths, and product/report gates.
6. E2B remains baseline FAST/weaker-device model mode, but advisory only for
   Stage 1-3 geometry and graph count.
7. The current app flow goes through `ProcessingFlowScreen`, `AutoSweepEngine`,
   and `GeometryPipelineRunner`; future Stage 1-3 work must not accidentally
   change curve scoring or downstream calculation inputs.
8. Current Rust CV is diagnostics/crop-planning only. It is not yet a graph or
   layout authority.

## Agent Findings Integrated

| Agent/workstream | Finding integrated into R1 |
|---|---|
| Stage 1-3 implementation scan | `AutoSweepEngine` couples graph/layout with OCR/axis, preprocessing variant scoring, and curve scoring; replacement must isolate Stage 1-3 outputs. |
| Fixture/QA scan | Android fixtures have graph counts `1,2,1,1,4,4,2,1`; Stage 1 normalized truth and graphPanel/plotArea annotations remain incomplete for some fixtures. |
| Rust CV scan | Rust has JNI/probe/crop-plan support but lacks image prep, graphPanel, plotArea, multiplicity, and layout classifiers. |
| Benchmark scan | DR-C5/DR-C6/DR-C7 show graph/layout research progress but no runtime-ready replacement. |

## R1 Blockers

None for the contract phase.

Implementation remains blocked until R2 builds the Stage 1-3 shadow parity
harness and produces fixture records.

## Validation

Command:

```powershell
git diff --check
```

Result: recorded in final response after command execution.

No Gradle command is required for R1 because only Markdown documentation changed.

## Follow-Up

```text
R2 - Stage 1-3 Shadow Parity Harness
```

R2 is now completed as a shadow benchmark harness. It:

- emit `Stage123ParityRecord` for current active Stage 1-3 output;
- emit the same record shape for the replacement candidate path;
- compare all eight Android validation fixtures;
- keep production behavior unchanged until parity and promotion gates pass.

The next phase should be `R3 - Stage 1 Image Preparation Candidate`.
