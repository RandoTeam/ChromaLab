# ChromaLab Product Modes

Phase 0 freezes the old fully automatic photo/screenshot flow as `AUTO_DIAGNOSTIC`.

## Modes

| Mode | Purpose | Release-quality allowed? | Phase 0 implementation |
| --- | --- | --- | --- |
| `AUTO_DIAGNOSTIC` | Automatic attempt for quick diagnostics, candidate geometry, OCR, trace and peak suggestions. | Only if every release gate passes. Otherwise diagnostic/review only. | Explicit metadata and gate contract. |
| `GUIDED_PRODUCTION` | Future reliable workflow where the user confirms graphPanel, plotArea, calibration anchors, trace and peaks. | Yes, after evidence or user confirmation gates pass. | Contract only. UI is not implemented in Phase 0. |
| `MANUAL_ADVANCED` | Future fallback for hard images where the user defines geometry and calibration manually. | Yes, after manual evidence is captured. | Contract only. UI is not implemented in Phase 0. |

## AUTO_DIAGNOSTIC Rules

- May run automatically from camera, gallery, screenshot, or file input.
- May produce diagnostic reports and suggestions.
- Must not look like release-quality production output unless all gates pass.
- Must export RuntimeEvidencePackage for every terminal state.
- Must label missing geometry, invalid calibration, sparse traces, or weak model evidence as review/diagnostic.

## Future Guided Rules

`GUIDED_PRODUCTION` and `MANUAL_ADVANCED` are allowed to satisfy gates with `USER_CONFIRMED` evidence. That confirmation must be explicit and stored in the report/evidence package.

## VLM Boundary

VLM/LLM can assist with OCR crop reading, text classification, overlay judging, graph hints, and warning summaries. VLM/LLM must not produce numeric RT, height, area, FWHM, S/N, baseline, Kovats, or exact pixel geometry used in calculations.
