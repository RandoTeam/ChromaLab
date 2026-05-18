# Runtime Evidence Real-Device Validation Checklist

Commit under validation: `00c447b6`.

This checklist is for proving the Android runtime path on a real device. Do not
claim runtime success until a real Android `RuntimeEvidencePackage` passes the
validator and the referenced artifact files are available for review.

## 1. Prepare The Device

- Install a build from commit `00c447b6` or later.
- Use a real Android device, not a desktop fixture, for this validation.
- Confirm the app can access camera/gallery and storage export.
- Confirm the intended chromatogram/vision model is selected and available on
  device before starting the run.
- Keep the device awake and connected to power for `FULL_ANALYSIS`.

Optional ADB context:

```powershell
adb devices
adb shell pm list packages | findstr chromalab
```

Expected package id:

```text
com.chromalab.app
```

## 2. Run FULL_ANALYSIS On Android

1. Open ChromaLab.
2. Use the chromatogram analysis flow, not the general chat flow.
3. Capture a chromatogram through the camera flow or import it through the scanner/gallery entry.
4. Let the full processing pipeline finish. Do not stop at a diagnostic/manual preview unless the app explicitly blocks scientific output.
5. Wait until the final report screen is available.
6. Open the report preview and check that the report metadata says the processing mode/runtime/model are present.

Required run notes to record manually:

- Device model and Android version.
- Build type: debug/release/beta.
- Input source: camera, gallery through scanner, or screenshot import.
- Selected model id/name.
- Executed model id/name.
- Executed runtime.
- Total analysis duration.
- Whether the run ended in scientific, review, or diagnostic state.

## 3. Export Required Files

On the final report screen:

1. Tap `Export or share`.
2. Save `Runtime evidence package (JSON)`.
3. Save `Validate runtime evidence (JSON)`.
4. Save `Validate runtime evidence (Markdown)`.
5. Also save the normal structured report if review needs user-facing report context:
   - `Structured report (Markdown)`
   - `Final report (HTML / PDF-ready)`
   - `Report UI contract (JSON)`

Android save location:

```text
Downloads/ChromaLab/
```

Typical exported filenames:

```text
runtime_evidence_package_<runId>.json
runtime_evidence_validation_<runId>.json
runtime_evidence_validation_<runId>.md
chromatogram_report_<runId>.md
chromatogram_report_<runId>.html
chromatogram_report_ui_contract_<runId>.json
```

ADB pull example:

```powershell
adb pull /sdcard/Download/ChromaLab ./device-validation/00c447b6
```

## 4. Attach Artifact Files For Review

The exported JSON contains paths to internal runtime artifacts. The JSON alone is
not enough for visual review. Attach all files referenced by:

- `artifactPaths.originalImagePath`
- `artifactPaths.normalizedImagePath`
- `artifactPaths.graphPanelOverlayPath`
- `artifactPaths.plotAreaOverlayPath`
- `artifactPaths.axisOverlayPath`
- `artifactPaths.tickOverlayPath`
- `artifactPaths.calibrationAnchorsOverlayPath`
- `artifactPaths.ocrCropPaths`
- `artifactPaths.peakLabelCropPaths`
- `artifactPaths.peakLabelCropBoundsOverlayPath`
- `artifactPaths.peakLabelTextClassificationOverlayPath`
- `artifactPaths.rawPlotAreaCropPath`
- `artifactPaths.rawCurveMaskPath`
- `artifactPaths.cleanCurveMaskPath`
- `artifactPaths.textSuppressionOverlayPath`
- `artifactPaths.rejectedComponentsOverlayPath`
- `artifactPaths.selectedTraceOverlayPath`
- `artifactPaths.skeletonOrCenterlineOverlayPath`
- `artifactPaths.finalPeakOverlayPath`

For debug builds, app-private artifacts can usually be pulled with `run-as`:

```powershell
adb shell run-as com.chromalab.app ls files
adb exec-out run-as com.chromalab.app tar -C files -cf - captures > chromalab-captures.tar
```

If `run-as` is not available, use Android Studio Device Explorer or add a manual
share/export step for the referenced artifacts before claiming validation.

## 5. Validate The Package

Use the in-app `Validate runtime evidence (JSON/Markdown)` exports first. That
validation runs on the device context where artifact paths are expected to exist.

The validator emits:

- `PASS`: no blocking issues or warnings.
- `REVIEW`: no blocking issues, but warnings exist. Scientific claims must be
  treated as review-grade until the warnings are inspected.
- `FAIL`: at least one blocking issue exists. Do not claim runtime success and do
  not use the report as production-quality scientific output.

Blocking examples:

- Missing original/normalized image.
- Missing graph/plot/axis/tick overlays.
- Missing model/runtime/device metadata.
- `FIXTURE_HINT` appears as runtime evidence.
- Runtime label evidence has no local crop path.
- Label evidence has neither parsed RT nor rejection reason.
- Recovered peak has no local signal window or no local maximum/rejection reason.
- Recovered peak duplicates an existing validated peak.
- Raw/validated/reportable counts are missing.

Review examples:

- Calibration status is `INVALID`.
- Title/channel text is inside plot area but geometry is marked review/diagnostic.
- VLM evidence is present but not clearly marked as text-only.

## 6. Minimum Evidence For A Real Success Claim

A real Android runtime run can be considered validated only when all of the
following are true:

- `runtime_evidence_validation_<runId>.json` verdict is `PASS`, or `REVIEW` with
  documented non-blocking reasons.
- The evidence package contains real runtime sources only:
  - `ML_KIT`
  - `VLM`
  - `BOTH`
- `FIXTURE_HINT` is absent from production-reportable recovery.
- Every runtime recovered peak has:
  - `sourceEvidenceId`
  - `sourceEvidence.localCropPath`
  - `labelRt`
  - local signal window
  - nearest local maximum or explicit rejection reason
  - flags
  - `ACCEPTED`, `REVIEW`, or `REJECTED` status
- The artifact files referenced by the package are attached and visually reviewable.
- Peak overlays distinguish normal detected peaks from recovered review peaks and rejected candidates.

For bench_03-like screenshots with visible labels such as `5.610` and `8.560`:

- If ML Kit/VLM reads the labels and local signal verification accepts them, they
  must appear as runtime recovered review-grade peaks.
- If OCR cannot read the labels, the report must show missing/rejected evidence,
  not fake recovery.
- If local signal verification fails, the candidate must be rejected with a
  deterministic reason.

## 7. Review Packet To Attach

Attach these files together:

- `runtime_evidence_package_<runId>.json`
- `runtime_evidence_validation_<runId>.json`
- `runtime_evidence_validation_<runId>.md`
- `chromatogram_report_<runId>.md`
- `chromatogram_report_<runId>.html`
- `chromatogram_report_ui_contract_<runId>.json`
- all referenced image/crop/mask/overlay artifacts
- short manual notes from section 2

Do not claim runtime success from screenshots of the final report alone. The
review packet must include the validator output and the referenced artifacts.
