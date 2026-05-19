# Phase 0 Research - Trace Extraction / Peak Review

Scope: trace evidence, sparse/fragmented trace status, and peak review gates.

## Sources Checked

- scikit-image skeletonize example:
  https://scikit-image.org/docs/stable/auto_examples/edges/plot_skeleton.html
  - Relevant because raster curve extraction often reduces masks to skeleton/centerline evidence.
  - Decision affected: selected trace overlay and mask artifacts are release gates.
  - Do not adopt: Python library dependency in Android/KMP code.

- SciPy `find_peaks`:
  https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html
  - Relevant because peak detection depends on prominence, width, threshold, and local maxima.
  - Decision affected: recovered labels can only become review peaks after local signal evidence.
  - Do not adopt: replacing CalculationEngine in Phase 0.

- pyOpenMS PeakPickerHiRes:
  https://pyopenms.readthedocs.io/en/release-3.4.0/apidocs/_autosummary/pyopenms/pyopenms.PeakPickerHiRes.html
  - Relevant as an established mass-spectrometry peak-picking reference.
  - Decision affected: future phases should compare peak workflows against known approaches.
  - Do not adopt: pyOpenMS runtime inside the mobile app.

- MOCCA chromatography documentation:
  https://mocca.readthedocs.io/en/latest/
  - Relevant for chromatographic peak/deconvolution workflows.
  - Decision affected: peak review must distinguish raw, validated, review, and rejected peaks.
  - Do not adopt: adding new peak math now.

## Phase 0 Decisions

- Sparse or fragmented trace cannot silently produce a release-quality report.
- Runtime recovered peaks are review-grade unless explicitly user-confirmed in a future guided flow.
- Fixture hints remain test-only and never production evidence.

## Explicit Non-Adoptions

- No new peak detector.
- No new baseline/integration math.
- No dense-series retuning.
