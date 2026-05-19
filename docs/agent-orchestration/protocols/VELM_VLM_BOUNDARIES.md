# VLM Boundaries

## Allowed VLM tasks

- local crop OCR;
- text classification;
- title/channel/axis label reading;
- overlay judging;
- warning explanation;
- crop-too-tight / crop-too-wide judgement.

## Forbidden VLM tasks

- exact pixel coordinates used for calculation;
- RT measurement;
- height measurement;
- area measurement;
- FWHM;
- S/N;
- baseline;
- Kovats numeric calculation;
- direct peak creation without signal verification.

## Evidence rules

Every VLM result must reference:

- localCropPath or overlayPath;
- prompt schema version;
- raw output;
- parsed output;
- confidence;
- status: ACCEPTED / REVIEW / REJECTED.

If VLM and deterministic checks disagree, deterministic geometry/calibration wins. VLM disagreement becomes warning, not override.
