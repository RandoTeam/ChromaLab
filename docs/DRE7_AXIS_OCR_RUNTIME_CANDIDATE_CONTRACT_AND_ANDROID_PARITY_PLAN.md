# DR-E7 Axis OCR Runtime Candidate Contract And Android Parity Plan

Status: `DR_E7_COMPLETE_RUNTIME_CANDIDATE_CONTRACT_READY_FOR_IMPLEMENTATION`

Date: 2026-06-04

## Purpose

DR-E7 converts the DR-E4, DR-E5, and DR-E6 PC-side evidence into a runtime
candidate contract. This is a planning and acceptance contract only. It does not
change Android runtime, production graph detection, calibration, trace
extraction, peak integration, `CalculationEngine`, chromatographic math,
validators, model routing, or report rendering.

The next implementation must follow this contract instead of copying benchmark
scripts blindly.

## Evidence Baseline

| Source | Result |
| --- | --- |
| DR-E4 | Repaired X label-band crops recovered 40 safe X anchors across 4 of 5 target X axes. |
| DR-E5 | Repaired crop evidence improved usable axes `18 -> 22` and usable graphs `7 -> 10`, with 0 regressions. |
| DR-E6 | Targeted fallback recovered the final two partial axes; PC-side graph calibration reached 9 `VALID` and 3 `REVIEW`, with 0 partial/invalid graphs and 0 regressions. |

Current PC-side calibration status after DR-E6:

| Metric | Value |
| --- | ---: |
| Graphs scored | 12 |
| Valid graph calibration cases | 9 |
| Review graph calibration cases | 3 |
| Partial graph calibration cases | 0 |
| Invalid graph calibration cases | 0 |
| Usable axes | 24 / 24 |
| Graph regressions vs DR-E5 | 0 |

## Runtime Candidate

Name:

`AXIS_OWNED_OCR_FALLBACK_CANDIDATE_V1`

Scope:

- axis tick-label OCR only;
- axis scale anchor recovery only;
- graph-level calibration evidence only.

Out of scope:

- graphPanel detection;
- plotArea detection;
- trace extraction;
- peak detection;
- chromatographic math;
- report gate weakening;
- VLM numeric authority.

## Candidate Pipeline

For every detected physical graph and axis:

1. Start with existing axis-owned OCR crops.
2. Run the default runtime OCR variant.
3. Build safe OCR anchors using existing geometry-owned label matching rules.
4. If an axis remains invalid, run per-axis fallback variants rather than a
   single global OCR variant.
5. For X axes with tight graph panels or dense labels, add repaired X label-band
   crops:
   - extend below the plotArea using plotArea, graphPanel, and image bounds;
   - add overlapping tiles across the label band;
   - keep all crop bounds normalized and geometry-derived.
6. For photo X axes with unreadable labels, add deep X-band crops:
   - wider vertical band below the plotArea;
   - lower-row band for perspective or panel-boundary errors;
   - overlapping high-density tiles;
   - contrast/sharpen/invert/binary preprocessing variants.
7. Merge safe anchors from all axis-owned OCR sources.
8. Run the existing robust-fit arbitration.
9. Export selected and rejected anchors, OCR rows, crop ids, preprocessing ids,
   residuals, and failure reasons.

## OCR Variant Policy

The benchmark variant ids are evidence labels, not mandatory Android engine
names. Android may use ML Kit or another local OCR backend, but it must preserve
the measured behavior class:

| Runtime behavior | PC evidence source | Required Android equivalent |
| --- | --- | --- |
| Default axis-owned OCR | DR-D6 global/default variant | Existing OCR path or equivalent baseline crop OCR. |
| Per-axis fallback | DR-D6 non-default variants | Retry only failed axes with alternate preprocessing, not the whole graph. |
| Repaired X label-band crops | DR-E4 | Geometry-derived wider X bands and overlapping X tiles. |
| Deep photo X-band recovery | DR-E6 | Wider/lower X bands, dense tiles, scale-up, contrast/sharpen/invert/binary variants. |

Rules:

- OCR output is text evidence only.
- OCR cannot create graphPanel, plotArea, axis lines, tick pixels, calibration
  coefficients, RT, height, area, FWHM, S/N, baseline, Kovats, or peak metrics.
- OCR numeric text must be paired to deterministic geometry-owned label boxes or
  tick-label crop ownership before it can become a scale anchor.
- OCR values from title, ion, m/z, legend, header, or metadata regions remain
  rejected.
- Missing OCR text remains missing evidence; no fabricated labels.

## Anchor Merge Contract

Each anchor source must be preserved:

- `BASELINE_AXIS_OWNED_OCR`
- `REPAIRED_X_LABEL_BAND_OCR`
- `PER_AXIS_OCR_FALLBACK`
- `DEEP_PHOTO_X_BAND_OCR`

Each accepted anchor must export:

- `fixtureId` / runtime graph id;
- axis `X` or `Y`;
- OCR text;
- parsed numeric value;
- pixel coordinate from deterministic geometry or label-box center;
- source crop id;
- preprocessing id;
- OCR confidence;
- text similarity or parser confidence if available;
- source label ownership reason;
- anchor source;
- residual after selected fit.

Each rejected OCR row must export:

- OCR text;
- crop id;
- preprocessing id;
- rejection reason;
- whether it matched a truth/evidence label;
- whether it was forbidden semantic text;
- whether it lacked pixel geometry;
- whether numeric parsing failed.

## Calibration Arbitration Contract

The selected calibration candidate must be produced by the existing robust-fit
gate. DR-E7 does not authorize new chromatographic math.

Required arbitration behavior:

- `VALID` beats `REVIEW`; `REVIEW` beats `INVALID`.
- More OCR rows do not automatically win; residuals, monotonicity, anchor count,
  and forbidden-text rejection still decide.
- Two-anchor calibration remains `REVIEW` unless the existing gate explicitly
  marks it stronger.
- A candidate with no pixel geometry is invalid.
- A candidate using title, ion, m/z, legend, or metadata numeric text is invalid.
- If a fallback OCR source improves one axis but harms another, keep the better
  per-axis result; do not globally switch variants.
- No fallback may regress an already `VALID` axis unless the old evidence is
  explicitly invalidated.

## Android Parity Plan

Run the runtime candidate first in validation mode only.

Required fixtures:

1. `bench_01_mz71_screenshot_page`
2. `bench_04_stacked_xic_resolution`
3. `bench_05_tic_plus_ions`
4. `bench_06_photo_two_graphs_page`

Preferred full parity fixtures:

1. `white_tiger_ion71`
2. `bench_01_mz71_screenshot_page`
3. `bench_02_mz92_belyi_tigr`
4. `bench_03_small_tic_export`
5. `bench_04_stacked_xic_resolution`
6. `bench_05_tic_plus_ions`
7. `bench_06_photo_two_graphs_page`
8. `bench_07_rotated_page_photo`

Modes:

- deterministic/no-model baseline;
- E2B baseline mode.

E2B rule:

- E2B may help OCR/semantic warnings and explanations;
- E2B must not erase deterministic graph candidates;
- E2B must not change graph count, pixel geometry, calibration coefficients,
  trace metrics, peak metrics, or report gates by itself.

## Android Parity Acceptance

Minimum acceptance for the runtime candidate:

- no missing runtime evidence package;
- no missing validator output;
- no app crash;
- no timeout/no export;
- no regression on White Tiger, `bench_03`, or `bench_07`;
- no graph count regression;
- no E2B regression;
- every graph-stage failure exports graph-level evidence;
- each axis exports baseline and fallback OCR attempts;
- every fallback accepted anchor has source crop/preprocessing provenance;
- no forbidden semantic number becomes a scale anchor.

PC-to-Android target:

| Fixture class | Expected parity target |
| --- | --- |
| Screenshot page | preserve or improve current calibration status |
| Stacked XIC | recover repaired X label-band anchors where PC did |
| TIC plus ion panels | recover at least the same graph-level `VALID`/`REVIEW` calibration classes as PC when graph layout matches |
| Two-graph photo page | recover deep photo X-band labels or export precise OCR visibility failure |

Phase cannot pass if:

- Android still produces opaque `TICK_LOCALIZATION_FAILURE`;
- Android loses graph-level evidence;
- Android report gate claims release-ready with missing/invalid calibration;
- E2B mode worsens deterministic geometry/calibration/metrics;
- runtime uses fixture-specific coordinates.

## Evidence Export Additions

Runtime evidence must add or preserve:

- axis OCR attempt table;
- crop plan table;
- preprocessing attempt table;
- per-axis fallback reason;
- selected and rejected anchor table;
- robust fit candidate table;
- residual table;
- anchor source counts;
- graph-level calibration decision delta;
- E2B disagreement table when model mode is enabled.

## Implementation Boundary

Next implementation must be minimal:

- add runtime crop generation equivalent to repaired/deep X bands;
- add per-axis OCR fallback orchestration;
- merge fallback anchors before robust-fit arbitration;
- export evidence tables.

Do not implement:

- new chromatographic calculations;
- new report gates;
- new trace extraction;
- new peak detection;
- VLM numeric authority;
- fixture-specific coordinate paths.

## Next Slice

Recommended next slice:

`DR-E8: Android Runtime Axis OCR Candidate Implementation`

Goal:

- implement `AXIS_OWNED_OCR_FALLBACK_CANDIDATE_V1` in validation/runtime code;
- keep it behind validation or diagnostic gating until Android parity is proven;
- run targeted Android parity before claiming product improvement.
