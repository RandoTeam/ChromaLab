# Phase 9G Calibration Strategy Ensemble Research

Date: 2026-05-21

## Source Quality Triage

| Source | Quality | Decision |
| --- | --- | --- |
| OpenCV Hough Line Transform documentation: https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html | Official library docs | Accepted for line/grid evidence framing. |
| Google ML Kit Text Recognition v2 Android docs: https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Official platform docs | Accepted for OCR bounding-box provenance and crop text extraction behavior. |
| W3C PROV overview: https://www.w3.org/TR/prov-overview/ | W3C standard overview | Accepted for preserving provenance of selected and rejected evidence. |
| PlotDigitizer documentation: https://plotdigitizer.com/docs | Product documentation, not a standard | Used only as a weak comparison point for multi-point calibration concepts. |

## Synthesis

- Line/grid detection should be treated as evidence for candidate calibration, not as a numeric source by itself.
- OCR bounding boxes are useful only when their coordinate frame is preserved and the text is classified as axis-label evidence rather than title, ion, SIM, or method text.
- Provenance must include both selected and rejected calibration candidates; otherwise a later validator cannot explain why a weaker path replaced a previously successful path.
- A calibration resolver should arbitrate multiple deterministic candidates by residuals, anchor count, monotonicity, confidence, and evidence type. VLM text can assist OCR semantics, but cannot provide pixel geometry or numeric calibration.

## Phase 9G Engineering Decisions

- Preserve the legacy tick-localization calibration path as one strategy in an ensemble.
- Keep `AxisScaleResolver` available, but no longer allow it to replace a valid/review legacy fit without arbitration evidence.
- Disable frame-endpoint fallback as a selected strategy for this slice until Y-axis direction and endpoint semantics are proven by tests and Android evidence.
- Export selected and rejected calibration strategy evidence into graph failure packages and validator Markdown/JSON.
