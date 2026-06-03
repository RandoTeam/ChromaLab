# DR-2F: Rust Axis Element Crop Block Transfer

## Scope

DR-2F moves the existing `axis_element_graph.json -> OCR crop plan` block into the Android Rust JNI bridge.

This is a safe transfer only:

- Rust receives a real axis element graph JSON and image dimensions.
- Rust returns crop-planning evidence for axis/OCR label bands.
- Rust does not create graph geometry, calibration anchors, peak metrics, report gates, or chromatographic calculations.
- `CalculationEngine` is untouched.

## JNI Contract

Kotlin entrypoint:

```kotlin
RustCvBridge.planAxisElementCropsJson(
    imageWidth = 576,
    imageHeight = 1280,
    axisElementGraphJson = graphJson,
)
```

Native contract:

```text
DR2F_AXIS_ELEMENT_CROP_PLAN_V1
```

The Rust response wraps the existing crop-plan report:

```json
{
  "status": "OK",
  "ffiContract": "DR2F_AXIS_ELEMENT_CROP_PLAN_V1",
  "algorithmAuthority": "crop_planning_only",
  "pixelGeometryAuthority": false,
  "calibrationAuthority": false,
  "peakMetricAuthority": false,
  "calculationEngineTouched": false,
  "report": {
    "graph_index": 1,
    "source_label_band_count": 3,
    "crop_plan": {
      "accepted": [],
      "rejected": []
    }
  }
}
```

Errors return structured JSON with `status = ERROR`; they do not crash the app.

## Android Diagnostic Entrypoint

ADB command:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CROPS
```

Automated runner:

```powershell
.\tools\android\Run-RustAxisElementCropSmoke.ps1
```

The diagnostic uses this real DR-1R fixture package:

```text
composeApp/src/androidMain/assets/validation/rust_axis_element/white_tiger_ion71_axis_element_graph.json
```

Source package:

```text
build/dr1r-axis-label-crop-sweep/white_tiger_ion71_fixture/graph_1/axis_element_graph.json
```

## Exported Artifacts

Device path:

```text
/sdcard/Download/ChromaLab/runtime/rust-axis-element-crops/<run_id>/
```

Local pulled path:

```text
artifacts/dr2f-rust-axis-element-crops/<run_id>/
```

Files:

- `rust_axis_element_crop_smoke_<run_id>.json`
- `rust_axis_element_crop_smoke_<run_id>.md`
- `rust_axis_element_crop_response_<run_id>.json`
- `rust_axis_element_crop_smoke_<run_id>.logcat.txt`

## Latest Real Android Result

Run id:

```text
rust_axis_element_crops_1780491586790
```

Result:

- Decision: `PASS`
- Rust status: `OK`
- Contract: `DR2F_AXIS_ELEMENT_CROP_PLAN_V1`
- Graph index: `1`
- Source label bands: `3`
- Accepted crops: `3`

## Build Packaging Fix

DR-2F also fixes the Android packaging dependency for the Rust JNI library.

Problem found during smoke:

- `build/generated/rustJniLibs/.../libchromalab_cv_core.so` had the new JNI symbol.
- `merged_jni_libs` and the APK still used a stale native library.

Fix:

- native merge tasks now depend on the Rust bridge build;
- native merge tasks track `libchromalab_cv_core.so` as an input;
- validation packaging reruns when the Rust library changes.

This prevents future APKs from shipping stale Rust JNI symbols.
