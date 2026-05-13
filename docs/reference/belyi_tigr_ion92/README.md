# Belyi Tigr Ion 92 Report Reference Fixture

This fixture records the report-depth target for the Belyi Tigr chromatogram case. It is part of phase 1.2 and should be used to evaluate whether ChromaLab can produce a professional chromatogram report with the same structure and completeness as the supplied reference analysis.

The fixture is a report-format reference, not a locked numeric answer. Future calculations must be derived from the detected graph, calibrated axes, extracted signal, and deterministic peak integration.

## Source Materials

The chromatogram screenshot is now committed as a neutral desktop-test resource. The human reference analysis remains outside the repository and is tracked only by hash.

| Asset | Role | SHA-256 | Notes |
| --- | --- | --- | --- |
| `composeApp/src/desktopTest/resources/fixtures/belyi_tigr_ion92/photo_2026-05-10_06-16-01.jpg` | Screenshot/photo containing the chromatogram graph | `D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE` | 576 x 1280 JPEG, 57090 bytes, phone screenshot/document page context |
| `reference_analysis.md.resolved` | Human reference for report structure and depth | `8D68B75738DD33DFFD74CFB83696F3485C46D3E8AE6A770CAA562A02D0404DB0` | Use as format reference, not as guaranteed numeric truth |

Do not add user-specific absolute paths to this fixture. Keep committed assets under neutral test-data/resource directories and preserve the hashes above.

## Visible Chromatogram Facts

- The input image is a screenshot/photo of a document page, not a clean graph export.
- The graph must be cropped before analysis. Phone UI, dark document background, and surrounding article text are not part of the chromatogram.
- The graph title reads approximately: `Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\data.ms`.
- The analysis appears to be a GC-MS extracted ion chromatogram.
- The ion/channel is `m/z 92.00` with visible range `91.70 to 92.70`.
- The sample label is `BELIY TIGR_1.D\data.ms`.
- The X-axis is retention time in minutes.
- The Y-axis is abundance.
- The Y-axis range is visually about `0` to `10000`.
- Major X-axis tick labels are visible around `10, 15, 20, 25, 30, 35, 40, 45, 50, 55`.
- The baseline is low compared with the highest peaks.
- A very tall early peak is visible near the left side of the plotted signal.

## Known Reference Tension

The supplied reference analysis describes a dominant peak near `49.0 min` with height around `9500`. In the supplied screenshot, the visually dominant peak appears early in the run near the left side of the graph, while the later peaks around `49 min` appear much smaller.

This tension is intentional in the fixture:

- the report renderer should follow the reference document's structure and depth;
- the calculation pipeline must not blindly reproduce the reference document's numeric claims;
- any final dominant-peak statement must be supported by extracted signal data;
- if extracted data contradicts the reference text, the report must flag the discrepancy instead of hiding it.

## Expected Report Sections

The report produced for this fixture must include the sections defined in `REPORT_SPEC.md`:

1. Overview.
2. Source and graph preparation.
3. Axis calibration.
4. Peak table.
5. Interactive or rendered graph.
6. Chromatographic quality.
7. Kovats index analysis.
8. Distribution and chemical interpretation.
9. Warnings and red flags.
10. Technical appendix.

## Required Fixture Checks

The report should be considered incomplete if any of these checks fail:

- The report states that the source image needed graph cropping.
- The report shows or records the detected graph bounds.
- The report identifies the title, ion/channel, sample label, X-axis, and Y-axis.
- The report records axis calibration confidence.
- The report provides a peak table ordered by retention time.
- The peak table includes RT, height, area, area percent, FWHM, base width, S/N, asymmetry, compound candidate, carbon number, Kovats index, confidence, and flags.
- The report includes baseline and noise metrics.
- The report includes chromatographic quality metrics.
- The report includes model/runtime metadata and total analysis duration.
- The report warns if selected model and executed runtime differ.
- The report warns if a required neural vision stage failed.
- The report does not invent missing values.
- The report distinguishes calculated values from inferred chemical interpretation.
- The report handles the early dominant peak versus reference-text tension explicitly.

## Domain Interpretation Target

The reference report expects the app to explain the case as an alkylbenzene-oriented GC-MS/EIC analysis when the evidence supports it.

The local domain knowledge pack should eventually provide:

- `m/z 92` aromatic/alkylbenzene interpretation;
- alkylbenzene homolog series notes;
- likely compound names, formulas, and carbon numbers;
- Kovats index literature ranges;
- warnings for internal standard, contamination, co-elution, weak crop, weak axis calibration, and unsupported model/runtime.

Until that local knowledge pack exists, reports must mark compound assignments and chemical explanations as lower-confidence model/domain interpretation, not as fully verified values.

## Future Automation Use

This fixture should later become a regression case for:

- graph crop detection;
- OCR of title and axes;
- axis calibration;
- curve extraction;
- peak detection and integration;
- report rendering completeness;
- runtime metadata correctness;
- warning generation.

The fixture intentionally does not define exact final peak counts, areas, or compound assignments yet. Those belong to later phases after the image-to-signal extraction and calculation engine are validated against real data.

## Real-Photo Fixture

Phase 6.4 adds the first executable real-photo fixture for this case:

```text
composeApp/src/desktopTest/kotlin/com/chromalab/feature/calculation/algorithm/BelyiTigrRealPhotoFixtureTest.kt
```

The test verifies that the committed JPEG keeps its expected SHA-256, byte size, and image dimensions. It also locks the analysis contract that this input needs graph cropping, selected/rejected preprocessing metadata, and explicit warnings for crop, axis confidence, and the early-dominant-peak reference tension.

This fixture still does not lock peak areas, FWHM, baseline, noise, or compound assignments. Those values must come from later graph extraction and calculation validation, not from manual reference prose.

## Executable Fixture

The phase 1.5 Kotlin fixture lives at:

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/fixtures/BelyiTigrIon92ReportFixture.kt
```

It builds a structured `ChromatogramReport`, validates it with `ReportContractValidator`, and renders it with `ReportMarkdownRenderer`. Its representative numeric values are report-shape placeholders only; they are not calculation ground truth.

The phase 1.6 rendered Markdown reference is:

```text
docs/reference/belyi_tigr_ion92/rendered_report.md
```

Use it as the first visual/textual target for the future report screen and export flow.

## Calculation Run Bridge

Phase 1.7 adds the first production bridge from a real deterministic `CalculationRun` into the structured `ChromatogramReport` contract:

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/CalculationRunReportMapper.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/calculation/export/CalculationRunReportExporter.kt
```

This bridge exports `chromatogram_report.md` from calculation data without inventing source-image OCR, crop, neural-model, or Kovats values that are not stored in the run yet. Missing upstream data remains explicit in the rendered report and is carried as warnings.

Phase 1.8 exposes this bridge in the calculation export UI as a separate Markdown export action. The older HTML report remains available, but the Markdown export is the first user-facing output backed by the strict report contract.

Phase 1.9 adds an in-app structured report preview to the calculation export screen. The preview renders the structured report object directly, including validation counts, graph summary, peak preview, and the highest-priority warnings before the user saves the Markdown file.

Phase 1.10 extends `CalculationRunReportOptions` so upstream stages can pass real graph-source metadata into the report bridge:

- detected graph/source bounds;
- crop confidence;
- preprocessing and scan mode;
- title, axis, and tick OCR confidence;
- OCR/model-derived chromatogram identification;
- upstream axis calibration;
- selected and executed model/runtime metadata;
- additional report or graph warnings.

When these fields are provided, the mapper uses them directly. When they are absent, the report keeps explicit missing-metadata warnings instead of pretending that crop, OCR, or model-stage information exists.

Phase 1.11 wires the calculation export caller to this options contract. `AnalysisFlowScreen` now passes metadata available from `ChromatogramEntity` and the restored `DigitalSignal` into `ExportCalculationScreen`: source type, source display name, stored signal point count, scan mode, ion channel when present, and deterministic execution metadata. Crop bounds and OCR confidence are still not invented; they remain absent until the processing flow persists them.
