# DR-2G: Rust Android Corpus Parity

## Scope

DR-2G verifies that the Android Rust JNI crop-planning bridge matches the PC Rust CLI over the full DR-1R axis-element graph corpus.

This remains an infrastructure and parity phase:

- Rust receives existing `axis_element_graph.json` packages.
- Rust returns deterministic OCR crop plans.
- Rust does not detect graphs, calibrate axes, extract traces, integrate peaks, or touch `CalculationEngine`.
- Kotlin production analysis remains authoritative.

## Corpus

Source corpus:

```text
build/dr1r-axis-label-crop-sweep/
```

Android asset manifest:

```text
composeApp/src/androidMain/assets/validation/rust_axis_element/corpus_manifest.json
```

Included corpus:

- `18` graph packages.
- `10` fixture ids, including the 8 Android validation fixtures plus White Tiger and Ion92 extra cases.
- Each graph package carries image dimensions and expected source label-band counts from the DR-1R audit.

## Android Entrypoint

ADB:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CORPUS
```

Automated parity runner:

```powershell
.\tools\android\Run-RustAxisElementCropCorpusParity.ps1
```

The runner performs:

1. PC Rust CLI corpus run through `tools/rust/Run-RustBridgeCorpus.ps1`.
2. Android validation APK build.
3. APK install without uninstalling app data.
4. Android JNI corpus run.
5. Artifact pull from device.
6. PC/Android comparison.

## Parity Criteria

For each fixture graph, DR-2G compares:

- image width and height;
- source label-band count;
- accepted crop count;
- rejected crop count;
- accepted crop signature:
  - band kind;
  - clamped rectangle;
  - crop variant list.

The suite passes only if every Android result matches the PC Rust CLI result.

## Artifact Paths

Device:

```text
/sdcard/Download/ChromaLab/runtime/rust-axis-element-corpus/<run_id>/
```

Local:

```text
artifacts/dr2g-rust-axis-element-corpus-parity/<run_id>/
```

Files:

- `rust_axis_element_corpus_<run_id>.json`
- `rust_axis_element_corpus_<run_id>.md`
- `rust_axis_element_corpus_<run_id>.logcat.txt`
- `rust_axis_element_corpus_parity_<run_id>.csv`

## Current Result

Run id:

```text
rust_axis_element_corpus_1780492924296
```

Result:

- item count: `18`;
- Android decision: `PASS`;
- Android pass/fail: `18 / 0`;
- PC/Android parity: `PASS`;
- parity CSV:
  `artifacts/dr2g-rust-axis-element-corpus-parity/rust_axis_element_corpus_1780492924296/rust_axis_element_corpus_parity_rust_axis_element_corpus_1780492924296.csv`.

## Next Rust Transfer Gate

After DR-2G, the next Rust phase should be `DR-2H: Image buffer contract`.

Do not move graph detection, calibration, trace extraction, or peak integration into Rust before defining and validating image buffer ownership, format, stride, checksum, and Android memory constraints.
