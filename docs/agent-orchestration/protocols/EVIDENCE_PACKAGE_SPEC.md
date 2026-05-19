# Runtime Evidence Package Spec

Every terminal state must export evidence.

## Terminal states

- PASS
- REVIEW
- DIAGNOSTIC_ONLY
- ROI_FAILURE
- CALIBRATION_FAILURE
- TRACE_FAILURE
- FATAL_PIPELINE_ERROR

## Required sections

- source provenance;
- normalized image;
- graphPanel candidates and selected panel;
- plotArea candidates and selected area;
- axis/tick attempts;
- calibration anchors/fits/residuals;
- OCR/VLM crops/results;
- text suppression regions;
- curve masks before/after suppression;
- selected trace overlay;
- peak overlay;
- user confirmations/edits;
- report contract;
- warnings;
- timings;
- model/runtime metadata;
- device metadata.

Missing evidence should fail validator unless stage was not reached and failure package explains why.
