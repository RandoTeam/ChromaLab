# Chromatogram Graph Layout Taxonomy

Phase 9E makes graph layout a deterministic evidence layer before graph count finalization. VLM/E2B may add semantic notes, but it cannot decide the final graph count.

| Layout class | Expected graph count rule | Panel separation rule | Shared-axis rule | Tick/calibration rule | Report rule | Failure mode |
| --- | --- | --- | --- | --- | --- | --- |
| `SINGLE_TRACE_SINGLE_AXIS` | One physical graph. | One graphPanel and one plotArea. | One X/Y axis system. | Fit X/Y from tick anchors inside that panel. | One graph section. | Calibration/tick failure if anchors are insufficient. |
| `DENSE_PEAK_SINGLE_AXIS` | One physical graph even if detector creates overlapping pseudo-panels. | Overlapping or near-touching candidates with same axis evidence collapse to one. | Shared X/Y axes. | Use the best complete panel only. | One graph section with dense peak caveat if needed. | `MULTI_GRAPH_SPLIT_FAILURE` if split remains. |
| `STACKED_TRACES_SHARED_AXIS` | Multiple reportable graph units when stacked traces have distinct channels/panels. | Vertical panels with common horizontal extent and visible separators. | X axis may be shared; Y scaling may differ by panel. | Per-panel calibration evidence required. | Multiple graph sections in reading order. | Wrong graph count or per-panel calibration failure. |
| `MULTI_PANEL_SEPARATE_AXES` | One graph per distinct axis system. | Separators or independent frames/axes. | Axes are not shared. | Per-panel tick/anchor evidence. | Multiple graph sections. | `MULTI_GRAPH_SPLIT_FAILURE`. |
| `TIC_PLUS_ION_PANELS` | TIC and each ion panel are separate graph units if distinct panels exist. | Channel labels and separated traces support grouping. | X may be shared; Y differs. | Per-panel calibration and channel metadata. | Multiple graph sections with method/channel notes. | Misclassifying channel text as peak/tick text. |
| `TWO_GRAPH_PAGE` | Two graph units. | Two vertically separated panels on a page/photo. | Usually separate Y axes; X may share scale. | Per-panel evidence required. | Two graph sections. | Only one processed graph or wrong reading order. |
| `EMBEDDED_SCREENSHOT_GRAPH` | Usually one graph. | White chart panel embedded in screenshot/page context. | One axis system. | Preserve title/axis/tick label margins. | One graph section. | Partial plot crop. |
| `ROTATED_PAGE_GRAPH` | Usually one graph after orientation correction. | Page/photo context may dominate. | One axis system after rotation. | Orientation evidence plus calibration. | Review if rotation confidence low. | Orientation or graphPanel failure. |
| `LOW_RES_EXPORT_GRAPH` | Usually one graph. | Small chart export. | One axis system, often weak OCR. | Low-resolution tick labels may remain diagnostic. | Diagnostic/review unless anchors validate. | `OCR_TICK_FAILURE` or `LOW_RESOLUTION_LABELS_UNREADABLE`. |
| `UNKNOWN_REVIEW` | Do not finalize as release-ready. | Evidence insufficient. | Unknown. | Calibration must block release. | Review/diagnostic only. | Exact failure class and subreason required. |

## Phase 9E Rules

- Layout classification is recorded as `layoutClassification` in `GraphMultiplicityResolution`.
- Same-axis duplicate/subregion candidates may be collapsed only by deterministic overlap/nesting evidence.
- Stacked and multi-panel fixtures require graph-count evidence; missing panels remain blockers unless Product, QA, and Scientific sign off.
- E2B/VLM cannot override `layoutClass`, graph count, graphPanel bounds, plotArea, tick pixels, anchors, or numeric chromatographic metrics.

## R15 Runtime Propagation Rules

- Runtime multi-panel processing must use
  `GraphMultiplicityResolution.resolvedGraphPanels` as the physical-panel source,
  not raw graph detector pseudo-panel lists.
- `GeometryPipelineResult.graphResults` preserves per-graph geometry, axis,
  OCR, calibration strategy, report status, and warnings in stable 1-based
  reading order.
- Deterministic text hints may name `TIC_PLUS_ION_PANELS` only after separated
  panels already exist; text hints cannot create graph count.
- `bench_04_stacked_xic_resolution` remains a 4-graph-unit fixture,
  `bench_05_tic_plus_ions` remains a 4-graph-unit fixture, and
  `bench_06_photo_two_graphs_page` remains a 2-graph-unit fixture unless
  Product, QA, and Scientific sign off on a metadata change.
- If a stored report section covers only one graph while runtime detected
  multiple physical panels, the report must carry
  `multi_panel_report_aggregation_unsupported` instead of silently presenting
  itself as a complete combined report.
