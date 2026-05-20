# VLM/OCR Stage Judge Contracts

## Stage Types

- `GRAPH_PANEL_CANDIDATE_JUDGE`
- `PLOT_AREA_CANDIDATE_JUDGE`
- `AXIS_TICK_VISIBILITY_JUDGE`
- `OCR_CROP_READ`
- `TEXT_REGION_CLASSIFY`
- `TRACE_OVERLAY_JUDGE`
- `PEAK_EVIDENCE_JUDGE`
- `REPORT_WARNING_SUMMARY`

## Verdicts

- `PASS`
- `REVIEW`
- `FAIL`
- `INCONCLUSIVE`
- `TIMEOUT`
- `MODEL_UNAVAILABLE`

## Allowed Retry Recommendations

- expand graphPanel left/right/up/down;
- expand plotArea;
- retry OCR with larger crop;
- retry OCR with contrast-enhanced crop;
- run VLM local crop OCR;
- mark title/channel as non-peak;
- request Assisted Review.

## Forbidden Retry Recommendations

- create final metrics;
- create peaks from text;
- accept invalid calibration;
- override deterministic residuals.

## Provenance

VLM-backed stage results require:

- local crop or overlay path;
- model runtime profile id;
- timeout and duration;
- rejected forbidden fields;
- raw OCR text when reading crops;
- task id linked back to graph, trace, peak, or report evidence.

