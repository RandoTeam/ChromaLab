# ChromaLab chromatogram report

This file is the phase 1.6 rendered-reference output for the Belyi Tigr Ion 92 executable fixture.

It demonstrates the target report shape only. Representative numbers are placeholders from visible fixture facts and must not be treated as validated calculation ground truth.

## 1. Overview

| Field | Value |
| --- | --- |
| Report ID | belyi_tigr_ion92_report_reference |
| Schema | 1.0.0-phase-1.3 |
| Source | Belyi Tigr Ion 92 screenshot fixture |
| Graphs detected | 1 |
| Analysis type | GC-MS |
| Mode | EIC |
| Ion/channel | m/z 92.00 |
| Sample | BELIY TIGR_1 |
| Detected peaks | 35 |
| Dominant peak | #1 |
| Total analysis time | 900000 ms |
| Selected model | Fixture selected vision model (LITERT) |
| Executed model | Fixture executed vision model (LITERT) |
| Executed runtime | MIXED |

## 2. Source and graph preparation

| Field | Value |
| --- | --- |
| Source bounds | x=0, y=0, 576x1280 |
| Detected graph bounds | x=7, y=425, 560x478 |
| Crop confidence | 82.00% |
| Scan mode | Smart Scan gallery fixture |
| Title OCR confidence | 86.00% |
| Axis OCR confidence | 78.00% |
| Tick OCR confidence | 76.00% |
| Manual adjustment | no |
| Preprocessing | document-page crop required; phone UI and article text excluded; graph region normalized |

## 3. Axis calibration

| Axis | Label | Unit | Min | Max | Major ticks |
| --- | --- | --- | --- | --- | --- |
| X | Time | min | 8.0000 min | 57.00 min | 10.00 min, 15.00 min, 20.00 min, 25.00 min, 30.00 min, 35.00 min, 40.00 min, 45.00 min, 50.00 min, 55.00 min |
| Y | Abundance | counts | 0.0000 counts | 10000 counts | 0.0000 counts, 1000 counts, 2000 counts, 3000 counts, 4000 counts, 5000 counts, 6000 counts, 7000 counts, 8000 counts, 9000 counts, 10000 counts |

- Calibration confidence: 76.00%
- Pixel transform: fixture-linear-axis-estimate: x=0.0875*px+8.0000, y=-24.80*px+10000

## 4. Peak table

| # | RT | Height | Area | Area % | FWHM | W_base | S/N | Asymmetry | Compound | Formula | C# | Kovats | Confidence | Flags |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 8.4000 min | 9500 counts | 1140 counts*min | 18.00 % | 0.0800 min | 0.1800 min | 135.71 | 1.1500 | early dominant peak | not calculated | not calculated | not calculated | 35.00% | visual_fixture_estimate; anomaly_candidate; reference_text_discrepancy |
| 2 | 10.20 min | 400 counts | 48.00 counts*min | 4.0000 % | 0.0800 min | 0.1800 min | 5.7143 | 1.1500 | toluene | not calculated | C7 | not calculated | 35.00% | reference_format_candidate; not_numeric_truth |
| 3 | 13.20 min | 600 counts | 72.00 counts*min | 6.0000 % | 0.0800 min | 0.1800 min | 8.5714 | 1.1500 | ethylbenzene | not calculated | C8 | not calculated | 35.00% | reference_format_candidate; not_numeric_truth |
| 4 | 35.50 min | 1600 counts | 192.00 counts*min | 12.00 % | 0.0800 min | 0.1800 min | 22.86 | 1.1500 | alkylbenzene homolog candidate | not calculated | C15 | not calculated | 35.00% | visual_fixture_estimate; compound_assignment_unverified |
| 5 | 49.00 min | 1100 counts | 132.00 counts*min | 8.0000 % | 0.0800 min | 0.1800 min | 15.71 | 1.1500 | late alkylbenzene candidate | not calculated | C20 | not calculated | 35.00% | visual_fixture_estimate; reference_claim_requires_extraction_check |

## 5. Interactive or rendered graph

- Extracted signal points: 560
- Corrected signal available: no
- Baseline method: visual fixture baseline
- Peak markers: 5
- Integration boundaries: available
- Anomaly markers: 1

## 6. Chromatographic quality

| Metric | Value |
| --- | --- |
| Total peaks | 35 |
| Significant peaks | 30 |
| S/N threshold | 3.0000 |
| Mean S/N | 18.00 |
| Median S/N | 12.00 |
| Maximum height | 9500 counts |
| Dominant peak | #1 |
| Baseline quality | low baseline with slight late drift |
| Average Rs | not calculated |
| Minimum Rs | not calculated |
| Theoretical plates | not calculated |
| HETP | not calculated |
| Global area | not calculated |
| Area normalization | not locked in fixture |

| Anomaly | Peak | Severity |
| --- | --- | --- |
| Very tall early peak is visible and must be checked as anomaly/internal standard/co-elution. | #1 | WARNING |

## 7. Kovats index analysis

- Status: NOT_CALCULATED
- Formula: I = 100*z + 100*(RT(x)-RT(z))/(RT(z+1)-RT(z))
- Reference series: n-paraffins
- Trend linearity R2: not calculated

| Peak | Compound | C# | RT | Kovats | Literature | Delta | Kind | Confidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| #2 | toluene | C7 | 10.20 | not calculated | 762.00-780.00 | not calculated | NOT_CALCULABLE | 30.00% |
| #4 | ethylbenzene | C8 | 13.20 | not calculated | 855.00-870.00 | not calculated | NOT_CALCULABLE | 30.00% |

- n-paraffin reference retention times are not available in this fixture.
- Kovats values must remain not calculable until the reference series is supplied or detected.

## 8. Distribution and chemical interpretation

- Likely compound class: alkylbenzene homolog series

| Carbon bucket | Area | Area % | Peaks |
| --- | --- | --- | --- |
| C7-C10 | not calculated | 22.00 % | 10 |
| C11-C15 | not calculated | 34.00 % | 12 |
| C16-C20 | not calculated | 36.00 % | 10 |
| C21+ | not calculated | 8.0000 % | 3 |

Homolog series notes:
- m/z 92 supports an alkylbenzene-oriented interpretation when confirmed by local knowledge and peak pattern.
- The report must distinguish calculated values from compound assignment hypotheses.

Domain context notes:
- Belyi Tigr context is useful as interpretation metadata, not as a source of numeric peak values.
- The early dominant peak should be explained as possible internal standard, contamination, or co-elution until verified.

Unresolved assignments:
- Exact compound assignments require validated peak extraction and a local domain knowledge pack.

## 9. Warnings and red flags

| Severity | Code | Stage | Graph | Peak | Message |
| --- | --- | --- | --- | --- | --- |
| INFO | fixture.numeric_truth_not_locked | fixture | 1 |  | This fixture validates report shape and completeness; numeric values are not locked ground truth. |
| WARNING | fixture.dominant_peak_reference_discrepancy | fixture | 1 |  | Reference text claims a dominant peak near 49 min, while the supplied screenshot visually shows a dominant early peak. |

## 10. Technical appendix

| Field | Value |
| --- | --- |
| App version | fixture |
| Schema version | 1.0.0-phase-1.3 |
| Started | 0 |
| Completed | 900000 |
| Duration | 900000 ms |
| Input source | TEST_FIXTURE |
| Device | fixture-device |
| Processing mode | FULL_ANALYSIS |
| Selected model | Fixture selected vision model (LITERT) |
| Executed model | Fixture executed vision model (LITERT) |
| Executed runtime | MIXED |

### Graph 1 calculation settings

- Smoothing: not applied in fixture
- Smoothing parameters: not calculated
- Baseline: visual fixture baseline
- Baseline parameters: not calculated
- Noise method: visual estimate
- Signal extraction confidence: 55.00%
