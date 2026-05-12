# Belyi Tigr Ion 92 Report Reference Fixture

This fixture records the report-depth target for the Belyi Tigr chromatogram case. It is part of phase 1.2 and should be used to evaluate whether ChromaLab can produce a professional chromatogram report with the same structure and completeness as the supplied reference analysis.

The fixture is a report-format reference, not a locked numeric answer. Future calculations must be derived from the detected graph, calibrated axes, extracted signal, and deterministic peak integration.

## Source Materials

The source materials were provided outside the repository and are not committed in this fixture.

| Asset | Role | SHA-256 | Notes |
| --- | --- | --- | --- |
| `photo_2026-05-10_06-16-01.jpg` | Screenshot/photo containing the chromatogram graph | `D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE` | 576 x 1280 JPEG, phone screenshot/document page context |
| `reference_analysis.md.resolved` | Human reference for report structure and depth | `8D68B75738DD33DFFD74CFB83696F3485C46D3E8AE6A770CAA562A02D0404DB0` | Use as format reference, not as guaranteed numeric truth |

Do not add user-specific absolute paths to this fixture. If assets are copied into a future test dataset, place them under a neutral test-data directory and preserve the hashes above.

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

## Executable Fixture

The phase 1.5 Kotlin fixture lives at:

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/fixtures/BelyiTigrIon92ReportFixture.kt
```

It builds a structured `ChromatogramReport`, validates it with `ReportContractValidator`, and renders it with `ReportMarkdownRenderer`. Its representative numeric values are report-shape placeholders only; they are not calculation ground truth.
