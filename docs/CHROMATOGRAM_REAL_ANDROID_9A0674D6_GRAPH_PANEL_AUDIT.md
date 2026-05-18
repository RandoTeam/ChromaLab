# Real Android Graph Panel Audit: 9a0674d6

Input package:
`runtime_evidence_package_run_1779132296044.json`

Run facts:
- Previous six-report multiplicity bug is fixed: `graphs=1`, `multiplicityStatus=SINGLE_GRAPH`.
- `GRAPH_ROI=1 ms`; total run duration `54.325 s`.
- Slow audit timing was synthesized as `model.graph_region=45.793 s` from the whole graph-selection step, not from a required VLM ROI source.
- Final report remained `DIAGNOSTIC_ONLY`; validator verdict was `FAIL`.

## Candidate Table

| # | Source | Bounds | Score | Contains left Y-axis / Y ticks | Contains bottom X ticks | Contains title / ion text | Horizontal trace coverage | Signal extends outside axes | Outcome |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| 1 | CV | `x=256 y=36 w=284 h=256` | `117.793846` | No. Starts on the right side of the chart and loses the left Y-axis/tick-label region. | Partial. Uses only the right-side plot band. | Partial title tail only. | Partial right-side trace only. | Yes: `plot_area.signal_extends_above_detected_y_axis`. | Incorrectly selected by old score because local plot/tick counts outweighed panel completeness. |
| 2 | CV expanded 4% | `x=246 y=27 w=296 h=274` | `101.525375` | No. Still missing left panel evidence. | Partial. | Partial. | Partial. | Yes. | Duplicate/expanded subregion. |
| 3 | CV expanded 8% | `x=235 y=17 w=307 h=294` | `99.25909` | No. Still missing left panel evidence. | Partial. | Partial. | Partial. | Yes. | Duplicate/expanded subregion. |
| 4 | Screenshot embedded chart | `x=2 y=1 w=538 h=351` | `73.17026` | Yes, as full/near-full normalized panel. Initial plot detector could not localize Y ticks. | Yes, the graphPanel preserves X tick labels and caption. | Yes. | Full trace. | No selected plot yet. | Should be preferred as full graphPanel and then derive plotArea inside it. |
| 5 | Screenshot embedded chart expanded | `x=0 y=0 w=542 h=353` | `25.0456` | Yes. | Yes. | Yes. | Full trace. | No selected plot yet. | Full-image variant; useful as rescue. |
| 6 | Full image fallback | `x=0 y=0 w=542 h=353` | `15.14` | Yes. | Yes. | Yes. | Full trace. | No selected plot yet. | Diagnostic fallback. |

## Root Cause

The selected candidate was a plot/right-side subregion, not a graphPanel. Old scoring rewarded local axis/tick-like geometry and calibration viability, but did not sufficiently require graphPanel completeness:

- left Y-axis region preserved;
- Y tick labels preserved;
- bottom X tick labels preserved;
- title/ion text preserved;
- full horizontal trace coverage;
- rejection of candidates with `plot_area.signal_extends_above_detected_y_axis`;
- rejection/penalty for candidates starting far to the right while a larger enclosing chart panel exists.

The near-full screenshot detector candidate existed, but it lost to the right-side CV candidate because the plot detector initially failed Y-axis localization on the full panel.

## Implemented Contract Update

- GraphPanel scoring now includes explicit completeness fields:
  `containsYAxisRegionScore`, `containsYTickLabelsScore`,
  `containsXAxisRegionScore`, `containsXTickLabelsScore`,
  `titleOrIonPreservedScore`, `fullTraceHorizontalCoverageScore`,
  `leftMarginSafetyScore`, `bottomMarginSafetyScore`,
  `subregionPenalty`, `axisViabilityScore`, and
  `calibrationViabilityScore`.
- Right-side subregions are penalized when they start far from the left edge, are too narrow for the normalized image, or emit `plot_area.signal_extends_above_detected_y_axis`.
- Already-cropped chart panels are explicitly boosted as graphPanel candidates, not rejected for containing title/tick labels.
- Plot-area derivation now has a projection fallback for weak/faint Y-axis localization inside a full graphPanel.
- `Ion 71.00 (70.70 to 71.70)` and title-tail text like `71.70); BELIY TIGR_1.Data.ms` are classified as `TITLE_OR_CHANNEL`, not `PEAK_ANNOTATION`.
- Runtime evidence export now writes graphPanel, plotArea, axis, and tick overlays when geometry ran, including diagnostic runs.
- VLM local-crop fallback is no longer run on non-plot title/text bands during geometry; local VLM OCR is bounded by timeout and limited to plot annotation crops.

## Expected Next Real-Device Check

For the same image, the selected graphPanel should be close to the normalized white chart panel (`~542x353`), not `x=256 y=36 w=284 h=256`. If the final report remains `DIAGNOSTIC_ONLY`, the next blocking stage should be calibration/tick localization, not ROI subregion selection.
