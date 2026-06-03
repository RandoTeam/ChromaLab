# DR-1P Axis Element Graph Prototype

Status: `DR1P_PROTOTYPE_COMPLETE`

Scope: evidence-only desktop/PC prototype. This slice does not change `CalculationEngine`, chromatographic math, report gates, validators, Android runtime behavior, or production calibration selection.

## Task Classification

- Deep research follow-up prototype
- Axis / tick / calibration evidence
- OCR label geometry evidence
- Graph-level runtime audit export
- QA / regression golden artifacts

## Agents Activated

- Orchestrator: kept scope limited to DR-1P evidence export, not algorithm repair.
- Research Intelligence: mapped DR-1 research into a concrete prototype target.
- Geometry / Calibration Core: owned axis, tick, anchor, and scale-pair evidence model.
- OCR / VLM Text Semantics: separated numeric OCR labels from semantic/non-scale text.
- QA / Regression: ran desktop compile and PC bench replay suite.
- Scientific Reporting / Validation: preserved evidence-gated reporting behavior.
- Product Acceptance: kept this as a diagnostic prototype, not a claimed production fix.

## Skills Used

- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `geometry-calibration-robust-fit`
- `ocr-local-crops`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `test-plan-authoring`

## Prototype Added

`OfflineAxisElementGraphAudit` is now emitted for every offline graph audit. It records:

- graph panel and plot area nodes;
- X/Y axis line nodes;
- axis origin node;
- deterministic X/Y tick-position nodes;
- OCR numeric label nodes;
- OCR semantic/text label nodes rejected as scale labels;
- calibration anchor nodes;
- graph edges for containment, axis definition, ticks, OCR-to-axis projection, and anchor use;
- scale candidate pairs derived from calibration anchor pixel span;
- blockers and warnings explaining why scale evidence is incomplete.

Desktop offline analysis now writes per graph:

- `graph_N/axis_element_graph.json`
- `graph_N/axis_element_graph_overlay.png`

The existing `audit_summary.md` also contains an `Axis Element Graph Prototype` section.

## PC Bench Replay Result

Output root:

`C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph`

Suite summary:

`C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph\pc_chromatogram_bench_summary.csv`

| Fixture | Expected graphs | Detected graphs | Ready | Blocked at | Calibrated graphs |
| --- | ---: | ---: | --- | --- | ---: |
| bench_01_mz71_screenshot_page | 2 | 2 | false | axis_calibration | 0 |
| bench_02_mz92_belyi_tigr | 1 | 1 | false | crop_quality | 0 |
| bench_03_small_tic_export | 1 | 1 | false | axis_calibration | 0 |
| bench_04_stacked_xic_resolution | 4 | 4 | false | axis_calibration | 0 |
| bench_05_tic_plus_ions | 4 | 4 | false | axis_calibration | 0 |
| bench_06_photo_two_graphs_page | 2 | 2 | false | axis_detect | 0 |
| bench_07_rotated_page_photo | 1 | 1 | true | not_blocked | 1 |
| bench_08_mz71_duplicate_candidate | 1 | 1 | false | axis_calibration | 0 |

## Axis Element Graph Evidence Table

| Fixture | Graph | Nodes | Edges | Scale pairs | Accepted pairs | Main blockers |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| bench_01_mz71_screenshot_page | 1 | 16 | 14 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_01_mz71_screenshot_page | 2 | 19 | 17 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_02_mz92_belyi_tigr | 1 | 33 | 31 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_03_small_tic_export | 1 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_04_stacked_xic_resolution | 1 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_04_stacked_xic_resolution | 2 | 8 | 6 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_04_stacked_xic_resolution | 3 | 8 | 6 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_04_stacked_xic_resolution | 4 | 8 | 6 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_05_tic_plus_ions | 1 | 16 | 14 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_05_tic_plus_ions | 2 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_05_tic_plus_ions | 3 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_05_tic_plus_ions | 4 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_06_photo_two_graphs_page | 1 | 33 | 32 | 0 | 0 | Y axis missing; OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_06_photo_two_graphs_page | 2 | 7 | 5 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |
| bench_07_rotated_page_photo | 1 | 64 | 56 | 2 | 2 | none |
| bench_08_mz71_duplicate_candidate | 1 | 39 | 37 | 0 | 0 | OCR labels missing; X/Y scale candidates insufficient; residual fit not ready |

## Findings

1. The prototype exported graph-level axis evidence for every detected graph in the PC replay suite.
2. The new graph object can distinguish “detected axis/tick geometry exists” from “scale evidence is sufficient”.
3. `bench_07_rotated_page_photo` is the only replay fixture where both X and Y scale candidate pairs are accepted.
4. Most blocked fixtures fail scale graph completeness because no OCR label evidence reaches calibration anchor formation, not because the graph container lacks axis/tick nodes.
5. `bench_06_photo_two_graphs_page` additionally exposes a concrete Y-axis missing blocker in graph 1.
6. This confirms DR-1P should feed the next wave toward OCR label-band recovery and label-to-axis projection, not another blind tick-only patch.

## Artifact Examples

- `C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph\bench_07_rotated_page_photo\graph_1\axis_element_graph.json`
- `C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph\bench_07_rotated_page_photo\graph_1\axis_element_graph_overlay.png`
- `C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph\bench_01_mz71_screenshot_page\graph_1\axis_element_graph.json`
- `C:\VietnAi\Hromotograth\build\dr1p-axis-element-graph\bench_01_mz71_screenshot_page\graph_1\axis_element_graph_overlay.png`

## Next Research/Prototype Target

Recommended next wave: numeric label-band graph recovery.

Required focus:

- recover OCR numeric boxes in X/Y label bands;
- preserve semantic text rejection for title/ion/m/z labels;
- project numeric labels to axis/grid evidence;
- emit accepted/rejected label-to-scale edges before any calibration strategy consumes them.
