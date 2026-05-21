# Phase 9F Axis Scale Resolver Research

Date: 2026-05-21

## Sources Reviewed

| Source | Quality tier | Relevant finding | Decision |
| --- | --- | --- | --- |
| Google ML Kit Text Recognition for Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Official vendor documentation | ML Kit returns recognized text from images and supports bundled/unbundled models; image resolution/focus materially affects OCR accuracy. | Treat OCR text and bounding boxes as evidence with quality/provenance, not as unconditional calibration authority. |
| OpenCV Hough Line Transform: https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html | Official library documentation | Hough transforms detect straight lines after edge preprocessing; probabilistic Hough returns segment endpoints. | Use line/grid/frame evidence as deterministic geometric support for axis scale, especially when explicit tick marks are weak. |
| PlotDigitizer documentation: https://plotdigitizer.com/docs | Maintained reference tool documentation | XY graph calibration uses known points on axes/plot and supports linear/nonlinear axes; distant calibration points improve accuracy. | Require multi-anchor/residual-backed calibration and keep label-only calibration review-grade unless geometry is strong. |

## Synthesis

Real screenshots/photos often lack reliable tick marks. A production calibration layer needs a candidate resolver over multiple evidence sources: OCR label boxes, grid/frame lines, explicit ticks, monotonic label sequences, and fit residuals. VLM may assist text interpretation but must not provide final pixel geometry or numeric chromatographic metrics.

## Rejected Inputs

- Weak blogs and forum posts were not used as implementation drivers.
- Plot digitizer UI behavior was used only as calibration workflow context, not as a source of ChromaLab scientific policy.
