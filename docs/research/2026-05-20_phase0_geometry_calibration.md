# Phase 0 Research - Geometry / Calibration

Scope: graphPanel, plotArea, axes, ticks, calibration gates, and what AUTO_DIAGNOSTIC may claim.

## Sources Checked

- OpenCV Hough Line Transform:
  https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
  - Relevant because axes, frames, and grid lines should be deterministic geometry evidence.
  - Decision affected: VLM remains advisory for geometry; deterministic CV owns pixel geometry.
  - Do not adopt: accepting Hough lines without ROI/candidate scoring and calibration residual checks.

- WebPlotDigitizer documentation:
  https://automeris.io/docs/digitize/
  - Relevant because plot digitization relies on calibration points and visual confirmation.
  - Decision affected: `GUIDED_PRODUCTION` will confirm graphPanel, plotArea, and calibration anchors.
  - Do not adopt: manual-only workflow as the main mobile path.

- PlotDigitizer documentation:
  https://plotdigitizer.com/docs
  - Relevant because graph extraction tools separate calibration markers from digitized data.
  - Decision affected: calibration evidence must store anchors and residuals.
  - Do not adopt: treating two endpoints as sufficient release-quality evidence by default.

- OpenCV contours/connected components family docs:
  https://docs.opencv.org/4.x/d3/dc0/group__imgproc__shape.html
  - Relevant to graphPanel and white-panel detection.
  - Decision affected: candidate generation may be expanded in later phases, but Phase 0 only gates claims.
  - Do not adopt: new detector rewrites in Phase 0.

## Phase 0 Decisions

- Release-ready requires valid or user-confirmed graphPanel and plotArea.
- X and Y calibration must be valid or user-confirmed.
- If calibration is missing, invalid, or only implied by VLM/OCR text without deterministic positions, AUTO remains diagnostic.

## Explicit Non-Adoptions

- No new ROI detector.
- No homography rewrite.
- No CalculationEngine change.
