# ChromaLab Chromatogram Report Specification

This document defines the target report contract for ChromaLab chromatogram analysis. It is the phase 1.1 reference for future report UI, export, calculation, and model-integration work.

The reference analysis document supplied for the Belyi Tigr chromatogram is treated as a format and depth reference, not as guaranteed numeric truth. Numeric values must come from the current image/file analysis pipeline or from user-provided digital data.

## Scope

This specification covers the report that is produced after one or more chromatogram graphs are detected, calibrated, calculated, and interpreted.

Inputs may include:

- Android camera capture.
- Smart Scan gallery import through the camera flow.
- Future file import such as CSV, TXT, PDF raster, mzML, or netCDF.

The report must support one image or page containing multiple graphs. Each graph receives its own graph block and its own report block:

```text
Source image or file
  -> Graph 1
     -> Report 1
  -> Graph 2
     -> Report 2
  -> Graph 3
     -> Report 3
```

## Non-Negotiable Rules

- The final report is not free-form LLM output.
- The final report is assembled from structured data.
- LLM or VLM output may help with recognition and interpretation, but it must not invent numeric values.
- Missing data must be shown as `not calculated`, `not detected`, or `insufficient confidence`.
- A report must not hide failed stages. Failed crop, axis extraction, calibration, peak integration, or model execution must be visible.
- A deterministic-only approximation must not be presented as a completed neural vision analysis.
- The report must show the actual runtime that was used, not only the model selected in the UI.
- Any mismatch between selected model and executed runtime must be flagged.

## Report Data Contract

The implementation should converge on a strict data model with these logical groups.

### Persisted Report Metadata

`ChromatogramEntity.algorithmConfig` may store a typed JSON envelope with:

- `kind`: `chromalab.report-metadata`;
- `version`: `1`;
- top-level source, timing, model, runtime, device, graph-count, and processing-mode metadata;
- per-graph source preparation metadata, identification, axis calibration, and warnings.

This payload is metadata only. It must not replace calculated signal points or peak metrics, and unrelated legacy `algorithmConfig` JSON must be ignored by report export.

### Report Metadata

Required fields:

- report id;
- app version;
- report schema version;
- analysis started at;
- analysis completed at;
- total analysis duration;
- input source type;
- source filename or capture id when available;
- number of detected graphs;
- selected model id;
- selected model name;
- executed model id;
- executed model name;
- executed runtime: `LiteRT`, `GGUF`, `OCR`, `deterministic`, or `mixed`;
- device name when available;
- processing mode;
- warnings.

### Graph Source Metadata

Required fields for every graph:

- graph index;
- source image bounds;
- detected graph bounds;
- crop confidence;
- preprocessing steps;
- filter or scan mode used;
- OCR confidence for title, axis labels, and tick labels;
- whether the graph was manually adjusted.

### Chromatogram Identification

Required fields:

- analysis type, for example `GC-MS`;
- chromatogram mode, for example `EIC` or `TIC`;
- ion or channel, for example `m/z 92.00`;
- ion range, for example `91.70-92.70`;
- sample name;
- sample path or instrument label when visible;
- matrix when known;
- target compound class when known;
- confidence for each extracted identity field.

### Axis Calibration

Required X-axis fields:

- axis label;
- unit;
- visible minimum;
- visible maximum;
- major ticks;
- minor ticks when available;
- pixel-to-unit transform;
- calibration confidence.

Required Y-axis fields:

- axis label;
- unit or count type;
- visible minimum;
- visible maximum;
- major ticks;
- pixel-to-unit transform;
- calibration confidence.

### Signal And Baseline

Required fields:

- extracted digital signal points;
- signal point count;
- smoothing method and parameters;
- baseline method and parameters;
- baseline points or baseline function;
- baseline mean;
- baseline drift;
- RMS noise;
- noise window or estimation method;
- corrected signal availability;
- signal extraction confidence.

### Peak Table

Every detected peak must expose:

- peak number from left to right;
- retention time;
- apex X pixel;
- apex Y pixel;
- absolute apex intensity;
- baseline at apex;
- height above baseline;
- start RT;
- end RT;
- width at base;
- FWHM;
- integrated area;
- area percent;
- S/N;
- asymmetry or tailing factor;
- overlap class;
- boundary method;
- integration method;
- confidence;
- warnings.

If compound assignment is available, each peak also exposes:

- probable compound name;
- chemical formula;
- compound class;
- carbon number;
- Kovats index;
- literature Kovats range;
- match delta;
- assignment confidence;
- assignment basis.

### Chromatographic Quality Metrics

Required fields when calculable:

- total detected peaks;
- significant peak count by S/N threshold;
- mean S/N;
- median S/N;
- maximum peak height;
- dominant peak;
- baseline quality;
- average resolution between adjacent significant peaks;
- minimum resolution;
- theoretical plates when calculable;
- HETP when column length is known;
- global integration area;
- area normalization status;
- anomaly list.

### Kovats Index Results

Required fields when the needed reference series exists:

- formula used;
- reference compound series;
- reference RT values;
- target compound RT values;
- calculated Kovats index per assigned compound;
- literature comparison;
- delta from literature range;
- trend linearity;
- R squared when applicable;
- notes about missing reference compounds.

The report must clearly distinguish:

- directly calculated index;
- interpolated index;
- literature lookup;
- not calculable.

### Value Provenance

Every structured report value that can influence interpretation must carry status and source
metadata. The report must distinguish:

- `CALCULATED` values from deterministic algorithms or imported numeric files;
- `DETECTED` values from OCR or vision extraction;
- `INFERRED` values from local rules, local knowledge, or model suggestions;
- `LOCAL_KNOWLEDGE` facts from the offline knowledge pack;
- `MODEL_SUGGESTED` hypotheses from LLM/VLM output.

Model-suggested values are never equivalent to calculated values. They must remain auditable in the
technical appendix and must not become final compound assignments unless later validation stages
raise their confidence with calculation, retention-index, spectrum, or user/library evidence.

### Local Domain Knowledge

The report should use a local offline knowledge pack rather than rely only on model memory.

The knowledge pack should eventually include:

- chromatogram types: GC-MS, EIC, TIC;
- common ion channels and fragments;
- `m/z 92` interpretation for alkylbenzenes;
- `m/z 91` and related aromatic fragment notes;
- alkylbenzene homolog series;
- n-paraffin reference series;
- compound names, formulas, carbon numbers, and classes;
- Kovats literature ranges;
- oil, condensate, and gas-related interpretation notes;
- warning rules for contamination, co-elution, internal standards, weak baseline, and weak axis calibration.

## Required Report Sections

The user-facing report must render these sections in this order unless a future UI spec changes the presentation.

### 1. Overview

Purpose: short professional summary of what was analyzed.

Required content:

- analysis type and mode;
- ion or channel;
- sample name;
- RT range;
- intensity range;
- detected peak count;
- dominant peak;
- main warnings;
- total analysis time;
- model and runtime used.

### 2. Source And Graph Preparation

Purpose: make the image preparation auditable.

Required content:

- original image or file preview;
- detected graph crop;
- preprocessing steps;
- crop confidence;
- OCR confidence;
- axis extraction status;
- graph count on the page or image.

### 3. Axis Calibration

Purpose: show how pixels became real chromatographic units.

Required content:

- X-axis label, unit, range, ticks;
- Y-axis label, unit, range, ticks;
- calibration confidence;
- visible issues, for example tilted image, weak labels, missing ticks.

### 4. Peak Table

Purpose: provide the main analytical data.

Required columns:

- number;
- RT;
- height;
- area;
- area percent;
- FWHM;
- width at base;
- S/N;
- asymmetry;
- probable compound;
- formula;
- carbon number;
- Kovats index;
- confidence;
- flags.

The table must be sortable or at least consistently ordered by RT.

### 5. Interactive Or Rendered Graph

Purpose: let the user visually verify calculations.

Required overlays:

- extracted signal;
- baseline;
- peak markers;
- integration boundaries;
- anomaly markers;
- optional compound or carbon-number labels.

### 6. Chromatographic Quality

Purpose: explain whether the calculation can be trusted.

Required content:

- peak count quality;
- S/N summary;
- baseline quality;
- resolution summary;
- area normalization status;
- calibration confidence;
- anomaly list.

### 7. Kovats Index Analysis

Purpose: show the retention-index calculation, not just final names.

Required content:

- formula;
- reference series used;
- calculated table;
- literature comparison;
- trend linearity;
- missing-data notes.

### 8. Distribution And Chemical Interpretation

Purpose: explain the chemical pattern without overstating certainty.

Required content:

- distribution by carbon number;
- homolog-series notes;
- likely compound class;
- oil, condensate, or gas-context notes when supported;
- unresolved or low-confidence assignments.

### 9. Warnings And Red Flags

Purpose: prevent false confidence.

Required warning examples:

- fewer than expected significant peaks;
- failed or low-confidence crop;
- failed axis extraction;
- baseline crosses peaks;
- all areas are suspiciously similar;
- dominant peak not checked for anomaly;
- selected model differs from executed runtime;
- GGUF model used without required vision adapter or mmproj;
- OCR-only model attempted for text chat or full chromatogram analysis;
- Kovats index differs from literature beyond threshold.

The implementation must also generate structured rule-based warning codes for release-critical
states. These warnings are part of the report data, not free-form model prose:

- `graph.crop_confidence_low` and `graph.crop_review_required` for weak graph preparation;
- `peak.coelution_suspected` and `separation.minimum_resolution_low` for overlap or poor separation;
- `peak.dominant_peak_review` and `peak.dominant_height_review` for dominant peaks that may represent contamination, internal standards, solvent/front artifacts, or co-elution;
- `baseline.quality_poor`, `baseline.correction_missing`, `baseline.noise_high`, and `baseline.drift_high` for weak baseline conditions;
- `runtime.full_analysis_without_neural_vision`, `runtime.ocr_only_full_analysis`, `runtime.executed_unknown`, and `runtime.selected_gguf_not_executed` for unsupported or mismatched model/runtime execution.

Serious or failed graph-level warnings must also appear in the chromatographic quality anomaly
list so the user-facing report can summarize them without hiding the technical audit trail.

### 10. Technical Appendix

Purpose: make results reproducible.

Required content:

- model and runtime metadata;
- calculation parameters;
- boundary method;
- integration method;
- clamp negative setting;
- smoothing and baseline settings;
- raw warnings;
- value provenance audit: status, source, and confidence for calculated, detected, inferred, local-knowledge, and model-suggested values;
- stage timings;
- export schema version.

## Reference Chromatogram Notes

For the supplied Belyi Tigr image and reference document:

- The reference document is accepted as the target report depth and format.
- The reference document is not accepted as a locked numeric answer.
- The supplied image is a screenshot or photo of a document page, so the graph must be cropped before analysis.
- The graph title indicates `Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\data.ms`.
- The X-axis is time in minutes.
- The Y-axis is abundance, visually from 0 to about 10000.
- A very tall early peak is visible near the left side of the graph.
- If a later report claims a dominant peak near 49 minutes, that claim must be supported by extracted signal data, not only by the reference text.

## Acceptance Criteria For Phase 1.1

This subphase is complete when:

- the report sections are documented;
- the required data fields are documented;
- missing-data behavior is documented;
- model/runtime metadata is documented;
- local domain knowledge requirements are documented;
- multiple-graph report structure is documented;
- the reference chromatogram is recorded as a format reference, not a numeric fixture.

Implementation, UI rendering, calculation updates, and knowledge-pack data files belong to later subphases.
