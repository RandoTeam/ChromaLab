# R1 Graph/Layout Image Preparation References

Date: 2026-06-06

Status: `R1_REFERENCE_NOTES`

Scope: external reference notes for the R1 replacement contract. These sources
inform method direction only. They do not prove ChromaLab runtime behavior and
do not change implementation.

## Source Quality

| Source | Type | R1 use |
|---|---|---|
| WebPlotDigitizer documentation | Maintained graph digitizer documentation | Confirms ROI, axis calibration, and extraction must be explicit steps. |
| OpenCV Hough line transform documentation | Official CV documentation | Confirms line detection depends on edge preprocessing and deterministic image evidence. |
| Rust `imageproc` Hough documentation | Rust crate documentation | Confirms Rust-side line detection primitives are available for parity prototypes. |
| Plot2Spectra paper | Peer-reviewed/arXiv research paper | Confirms plot-region detection, edge-based axis refinement, text/tick interpretation, and line extraction as staged work. |

## Notes

### WebPlotDigitizer

Reference: <https://automeris.io/docs/digitize/>

Relevant method direction:

- graph digitization starts by loading an image and choosing/calibrating axes;
- ROI/mask selection constrains automatic extraction;
- automatic extraction is downstream of calibration and ROI, not a replacement
  for them.

R1 implication:

- ChromaLab's autonomous mode must generate visible ROI, graphPanel, plotArea,
  and calibration evidence automatically;
- a final report without those intermediate artifacts is not acceptable.

### OpenCV Hough Line Transform

Reference: <https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html>

Relevant method direction:

- line detection is a deterministic CV step;
- edge preprocessing is expected before Hough line detection;
- standard and probabilistic Hough outputs can support line/frame candidates.

R1 implication:

- frame, axis, separator, and panel-boundary evidence must stay deterministic;
- VLM/E2B can describe uncertainty but cannot create pixel geometry.

### Rust imageproc Hough

Reference: <https://docs.rs/imageproc/latest/imageproc/hough/>

Relevant method direction:

- `imageproc` includes Hough line detection over binary input images;
- detected polar lines and intersection helpers can support Rust-side
  graph/frame primitives.

R1 implication:

- Rust CV can become a deterministic primitive owner for Stage 1-3 after
  parity, but should not be promoted before fixture evidence.

### Plot2Spectra

Reference: <https://arxiv.org/abs/2107.02827>

Relevant method direction:

- automatic plot extraction can be staged as plot-region detection, edge-based
  axis refinement, tick/text interpretation, then line extraction;
- plot region and axis alignment are upstream of trace extraction.

R1 implication:

- ChromaLab should stabilize Stage 1-3 before returning to trace and peak work;
- trace/peak failures should not be debugged as math failures until graph/layout
  evidence is reliable.

## R1 Research Decision

The external method direction agrees with the internal Phase 9J truth audit:

1. Build deterministic image/graph/layout evidence first.
2. Run replacement logic in shadow parity against the fixture corpus.
3. Promote only after graph count, graphPanel, plotArea, and evidence artifacts
   are stable.
4. Keep model outputs advisory for geometry.
