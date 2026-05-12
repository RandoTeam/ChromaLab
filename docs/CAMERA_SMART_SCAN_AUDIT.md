# Camera And Smart Scan Audit

Phase: 3.1
Last updated: 2026-05-12

This document records the current Android camera/photo ingestion path before Phase 3 changes. It is an implementation audit, not a target design.

## Current Entry Points

### Capture hub

- `CaptureHubScreen` exposes two product choices: camera capture and file import.
- The camera choice navigates to `Route.Camera`.
- The file import choice is separate and does not enter the photo processing pipeline.

Relevant files:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/capture/CaptureHubScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/app/App.kt`

### Smart Scan camera path

Android `CameraScreen` launches Google ML Kit Document Scanner immediately on entry.

Flow:

1. `CameraScreen.android.kt` gets a scanner from `MlKitDocumentScanner.getScanner()`.
2. It launches the scanner with `ActivityResultContracts.StartIntentSenderForResult`.
3. On success, it reads `GmsDocumentScanningResult`.
4. It copies the first page image URI into app storage under `filesDir/captures`.
5. It calls `onImageCaptured(path)`, which routes to `ProcessingFlowScreen`.

Relevant files:

- `composeApp/src/androidMain/kotlin/com/chromalab/feature/capture/CameraScreen.android.kt`
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/document/MlKitDocumentScanner.kt`

Scanner options currently used:

- Gallery import allowed: `true`.
- Page limit: `1`.
- Result format: JPEG.
- Scanner mode: `SCANNER_MODE_FULL`.

Implication:

- The scanner UI can use camera capture or gallery import.
- ML Kit owns the visible crop/editor/filter step.
- Current code stores only the final scanner JPEG. It does not store ML Kit crop rectangle, selected filter, deskew parameters, or whether the user entered through camera or gallery inside the scanner UI.

### Legacy manual fallback path

Before Phase 3.2, ML Kit scanner startup failure switched `CameraScreen.android.kt` to `ManualCameraScreen`.
That fallback is now disconnected from the normal camera route because it bypasses Smart Scan preparation.

Manual capture features:

- CameraX preview.
- Back camera.
- `ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY`.
- 4:3 aspect ratio fallback strategy.
- Flash mode toggle.
- Exposure control.
- Pinch-to-zoom.
- Tap-to-focus.
- EXIF target rotation tracking.

Manual image output:

- Captured photos are saved as `original_*.jpg`.
- The gallery icon uses `ActivityResultContracts.PickVisualMedia`.
- Picked gallery images are copied as `gallery_*.jpg`.

Important gap:

- Manual gallery import does not pass through ML Kit Smart Scan.
- It does not get ML Kit crop, deskew, shadow removal, or scanner filters.
- This path can therefore feed a full screenshot/document page into `ProcessingFlowScreen`.

Relevant file:

- `composeApp/src/androidMain/kotlin/com/chromalab/feature/capture/ManualCameraScreen.kt`

## Processing After Capture

All image paths from the camera route enter `ProcessingFlowScreen`.

Current stages:

1. Ensure a chromatography VLM is ready. If no usable vision model is available, full analysis stops.
2. Normalize EXIF orientation through `ImageNormalizer`.
3. Skip internal document crop and use `fallbackCropResult`.
4. Skip internal perspective correction and use `fallbackPerspectiveResult`.
5. Run auto-sweep:
   - VLM graph region detection.
   - CV graph detection/refinement.
   - axis OCR.
   - axis detection.
   - eight preprocessing variants for curve extraction scoring.
6. Convert the best extracted curve into a signal.
7. Save the result to Room and attach report metadata.

Relevant files:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/flow/ProcessingFlowScreen.kt`
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/normalize/ImageNormalizer.android.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/sweep/AutoSweepEngine.kt`

## Metadata Currently Saved

Photo analysis currently saves:

- source image bounds;
- detected graph bounds;
- crop confidence derived from graph confidence;
- selected and executed model metadata;
- runtime/backend/device metadata;
- stage timings;
- graph warnings;
- preprocessing steps from the ChromaLab pipeline.

Current limitation:

- `ProcessingFlowScreen` saves every camera-route image as `SourceType.PHOTO`.
- Smart Scan camera, Smart Scan gallery import, manual CameraX capture, and manual gallery import are not structurally distinguished.
- Report `scanMode` therefore falls back to `photo-processing-flow` for all of them.
- ML Kit scanner filter/crop/editor details are not persisted because current code only receives the final JPEG path.

Relevant files:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/report/ProcessingReportMetadataBuilder.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/StoredReportMetadata.kt`

## Unused Or Bypassed Local Preparers

The project still contains local document/crop/perspective components:

- `DocumentDetector`
- `ImageCropper`
- `PerspectiveWarper`
- review screens for crop and perspective

In the current automatic photo pipeline, these are effectively bypassed because Smart Scan is treated as already cropped and deskewed.

This is acceptable only for the ML Kit Smart Scan path. It is not acceptable for manual fallback gallery/camera images unless they are explicitly marked as diagnostic/manual mode.

## Findings For Next Phases

1. The strongest existing preparation path is ML Kit Smart Scan with gallery import allowed.
2. The weak duplicate path was the manual fallback gallery/camera route, because it bypassed Smart Scan preparation.
3. The processing pipeline currently assumes that incoming images are already prepared, then records no-op crop and perspective stages.
4. Provenance is not strong enough: the report cannot tell Smart Scan camera vs Smart Scan gallery vs manual fallback.
5. The app does not persist the ML Kit crop/filter choices visible in the scanner UI.
6. If a non-Smart-Scan screenshot is reintroduced through any manual path, VLM/CV stages may analyze a full phone screenshot instead of a clean graph image.

## Phase 3.2 Recommendation

Phase 3.2 should make the photo path single-source and explicit:

- Keep Smart Scan as the normal camera/photo route.
- Do not expose the manual gallery copy path as a release-quality chromatogram input.
- If ML Kit is unavailable, show a clear compatibility/error path or a separate diagnostic/manual mode instead of silently continuing with weaker preparation.
- Add structured source provenance so reports can distinguish scanner camera, scanner gallery, and manual diagnostic inputs.

## Phase 3.2 Implementation Notes

Implemented on 2026-05-12:

- `CameraScreen.android.kt` no longer falls back from failed Smart Scan startup into `ManualCameraScreen`.
- Smart Scan startup failure now stays on a scanner error screen with retry/back actions.
- The release-quality camera/photo route therefore requires the ML Kit scanner preparation step before `ProcessingFlowScreen`.

Remaining later work:

- `ManualCameraScreen` still exists in the codebase as a potential diagnostic/manual capture screen, but it is no longer reached from the normal Smart Scan route.
- Source provenance still needs a structured model so reports can distinguish Smart Scan camera, Smart Scan gallery, and any future diagnostic/manual capture.

## Phase 3.3 Implementation Notes

Implemented on 2026-05-12:

- `ProcessingFlowScreen` now treats image normalization as mandatory for photo analysis.
- Normalized image width and height must be positive before crop, perspective, graph detection, VLM bounds conversion, and report metadata.
- `AutoSweepEngine` rejects missing dimensions instead of guessing `1920x1080`.
- Desktop normalization now records real image dimensions when copying the source image.

Remaining later work:

- Android normalization still decodes the full source bitmap. Large-image memory behavior should be reviewed separately from correctness.
- Source provenance still needs to record whether the normalized input came from Smart Scan camera or Smart Scan gallery.

## Phase 3.4 Implementation Notes

Implemented on 2026-05-12:

- Preprocessing now writes explicit `sharpened` and `scan_style` image artifacts in addition to grayscale, contrast, binary, and morphology outputs.
- Auto-sweep now tests curve extraction against original, grayscale, contrast, sharpened, binary, and scan-style variants.
- Sweep scoring records the selected variant in the score breakdown, so the winning path can be audited.
- Desktop preprocessing stubs now expose the same variant file contract and preserve image dimensions.

## Phase 3.5 Implementation Notes

Implemented on 2026-05-12:

- Auto-sweep selection now scores each preparation with explicit graph, axis, OCR, and curve components.
- Graph scoring includes crop confidence, plausible plot area, aspect ratio, image bounds, and graph warnings.
- Axis scoring includes detector confidence, both-axis presence, origin presence, axis length coverage, alignment, and warnings.
- Curve scoring now includes point density, extracted-column coverage, X coverage, continuity, high-confidence ratio, vertical signal span, interpolation penalty, outlier penalty, warnings, and usability penalty.
- `scoreBreakdown` now records all components so the selected variant is explainable from logs and saved metadata once ranking persistence is added.

Remaining later work:

## Phase 3.6 Implementation Notes

Implemented on 2026-05-12:

- Report metadata now has structured `selectedPreparationVariant` and `rejectedPreparationVariants` fields on `GraphSourceMetadata`.
- `ProcessingFlowScreen` converts the sorted auto-sweep results into ranked metadata entries with config name, image input variant, score, selected flag, and score breakdown.
- Multi-graph processing stores preparation variant rankings per graph before saving each chromatogram record.
- `ProcessingReportMetadataBuilder` writes the selected variant and rejected rankings into `algorithmConfig`.
- Metadata round-trip and builder tests now cover selected/rejected preparation variants.

Remaining later work:

- Later report UI/export phases can render these structured fields as a dedicated audit table instead of relying on the preprocessing summary line.

## Phase 4.1 Implementation Notes

Implemented on 2026-05-12:

- `GraphRegionResult` now exposes `qualityEvaluations` for every detected region.
- `filteredRegions` now contains only regions that pass graph quality filters; fallback/manual guesses are no longer promoted into the accepted multi-graph list.
- Graph region quality checks include image bounds, minimum width/height/area, maximum broad-region area, low-confidence broad-region rejection, and plausible aspect ratio.
- Auto-sweep can now compare CV and VLM region counts using accepted regions only, so multi-graph selection is based on regions that passed quality filters.

Remaining later work:

- Phase 4.2 should formalize natural reading order beyond simple top-to-bottom sorting, especially for pages with multiple columns.

## Phase 4.2 Implementation Notes

Implemented on 2026-05-12:

- `GraphRegionResult.sortedRegions` now groups detected regions into visual rows before sorting.
- Regions in the same row are ordered left-to-right; rows are ordered top-to-bottom.
- Row grouping uses vertical overlap, so pages with two chromatograms side-by-side are ordered before moving to the next row.
- `filteredRegions`, `qualityEvaluations`, and the default selected region now inherit the same natural reading order.

Remaining later work:

- Phase 4.3 should run graph preparation, OCR, calibration, curve extraction, and calculation independently for each ordered graph.

## Phase 4.3 Implementation Notes

Implemented on 2026-05-12:

- The photo pipeline now captures a separate processing snapshot for each graph before moving to the next graph.
- Each snapshot preserves that graph's selected ROI, OCR result, preprocessing result, selected sweep config, preparation ranking, warnings, and smoothed signal.
- Auto-save now writes each graph from its own snapshot instead of using the last graph's OCR/preprocessing state for every saved chromatogram.
- Per-graph fallback curve extraction now writes into the same `graph_N` output directory as that graph's auto-sweep artifacts.

Remaining later work:

- Phase 4.4 should present saved multi-graph output as graph 1/report 1, graph 2/report 2, graph 3/report 3 in the user-facing result flow.

## Phase 4.4 Implementation Notes

Implemented on 2026-05-12:

- `AnalysisFlowScreen` now discovers sibling chromatogram records from the same multi-graph photo analysis.
- Multi-graph results show a graph switcher so the user can open graph 1/report 1, graph 2/report 2, graph 3/report 3 from the same result screen.
- Switching graphs reloads the selected chromatogram record and recalculates the report for that specific signal.
- Report options now use the graph index stored in the current chromatogram record instead of always preferring graph 1 metadata.

Remaining later work:

- Phase 4.5 should add stronger regression checks that metadata, peaks, warnings, and model responses cannot cross between graph records.

## Phase 4.5 Implementation Notes

Implemented on 2026-05-12:

- Processing metadata now forces all stored graph warnings onto the current graph index instead of preserving stale indexes from upstream state.
- Report mapping now forces additional graph warnings onto the report graph being rendered.
- Regression coverage documents two separate graph records with distinct ROI, preparation variant, warning, and signal-point metadata.
- Regression coverage also documents that report peaks and calculation/model-stage warnings are bound to the current graph index.

Remaining later work:

- Phase 5.1 should start structured extraction for title, ion/channel, sample label, axis labels, and tick labels.
