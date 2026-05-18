# Real Android ROI Multiplicity Audit, e332cec8

## Real Run Diagnosis

The old blocking ROI error disappeared on the real Android run, but the runtime
emitted six pseudo-graph reports for one physical chromatogram. The root cause
was not chemistry or `CalculationEngine`: `AutoSweepEngine` converted
`GeometryTrace.roiCandidates` back into a `GraphRegionResult`, and
`ProcessingFlowScreen` treated those overlapping ROI candidates as separate
physical graphs.

## Candidate Table

| Graph | Source | Graph panel | Plot area | Area | Axis confidence | Tick count X/Y | Calibration X/Y | Status | Why it became a report |
| --- | --- | --- | --- | ---: | ---: | ---: | --- | --- | --- |
| 1 | `VLM_HINT` | `x=54,y=35,w=483,h=276` | `x=58,y=46,w=477,h=234` | 133308 | 0.90 | 31/17 | `VALID/INVALID` | `DIAGNOSTIC_ONLY` | ROI candidate was promoted to `GraphRegionResult.filteredRegions`. |
| 2 | `USER` | `x=37,y=25,w=505,h=296` | `x=40,y=35,w=495,h=245` | 149480 | 0.90 | 32/17 | `VALID/INVALID` | `DIAGNOSTIC_ONLY` | Expanded duplicate of the same panel. |
| 3 | `USER` | `x=54,y=35,w=483,h=276` | `x=58,y=46,w=477,h=234` | 133308 | 0.90 | 31/17 | `VALID/INVALID` | `DIAGNOSTIC_ONLY` | Exact duplicate of graph 1. |
| 4 | `USER` | `x=212,y=0,w=330,h=333` | `x=214,y=67,w=328,h=217` | 109890 | 0.90 | 17/5 | `VALID/INVALID` | `DIAGNOSTIC_ONLY` | Nested/right-side subregion of the same chart. |
| 5 | `USER` | `x=246,y=27,w=296,h=274` | `x=249,y=67,w=293,h=219` | 81104 | 0.90 | 14/2 | `VALID/INVALID` | `DIAGNOSTIC_ONLY` | Nested peak-cluster subregion. |
| 6 | `USER` | `x=256,y=36,w=284,h=256` | `x=258,y=67,w=282,h=219` | 72704 | 0.90 | 12/5 | `INVALID/INVALID` | `DIAGNOSTIC_ONLY` | Nested peak-cluster subregion. |

Pairwise overlap confirmed the candidates were duplicates/subregions:

- Graphs 1 and 3: IoU `1.000`.
- Graphs 1 and 2: IoU `0.892`, graph 1 fully contained in graph 2.
- Graphs 4, 5, and 6: containment `1.000` for the smaller nested panel against a larger candidate.
- Graphs 1/2/3 vs 4/5/6: high overlap and shared axis system, not distinct frames.

## Implemented Contract

- `GraphMultiplicityResolver` now runs after ROI candidate generation.
- Overlapping candidates are rejected by `DUPLICATE_IOU`.
- Nested candidates are rejected by `NESTED_INSIDE_SELECTED_PANEL`.
- Dense peak clusters that share the same axis system are rejected as
  `SAME_AXIS_SYSTEM` / `SUBREGION_NOT_GRAPH_PANEL`.
- `AutoSweepEngine` now exposes only `multiplicityResolution.resolvedGraphPanels`
  as physical graph regions. Raw ROI candidates remain in `GeometryTrace` for
  evidence, but they no longer drive the multi-graph loop.
- `ScreenshotEmbeddedChartDetector` now supports
  `ALREADY_CROPPED_CHART_PANEL`, where the normalized image itself is mostly the
  white chart panel.

## Evidence Export

The report screen now automatically exports, for every displayed terminal report:

- `runtime_evidence_package_<runId>.json`
- `runtime_evidence_validation_<runId>.json`
- `runtime_evidence_validation_<runId>.md`

These are saved through the platform export layer (`Downloads/ChromaLab` on
Android/desktop). Diagnostic-only reports are not treated as success, but they
now produce validator-readable evidence.

## Timing Guard

The VLM ROI hint is skipped when deterministic CV/screenshot candidates already
exist. If VLM is needed, its ROI hint timeout is bounded to 8 seconds. Tick OCR
crop reading is also bounded to 8 seconds per evaluated physical candidate.
