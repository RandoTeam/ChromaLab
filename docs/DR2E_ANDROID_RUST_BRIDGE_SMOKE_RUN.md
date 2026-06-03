# DR-2E Android Rust Bridge Smoke Run

Status: `DR2E_DEVICE_SMOKE_PASSED`

Purpose: verify on a real Android device that the validation package can load `libchromalab_cv_core.so`, call the Rust JNI probe, export a diagnostic artifact, and prove the bridge is available.

## Scope

Implemented:

1. debug intent `com.chromalab.app.DEBUG_RUST_CV_BRIDGE`;
2. Android smoke diagnostic exporter;
3. JSON and Markdown artifacts under `/sdcard/Download/ChromaLab/runtime/rust-bridge-smoke/<run_id>/`;
4. automated install/run/verify script.

Not implemented:

1. chromatogram image analysis through Rust;
2. Rust graph detection;
3. Rust calibration;
4. Rust peak metrics;
5. any `CalculationEngine` change.

## ADB Command

Manual smoke run:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.DEBUG_RUST_CV_BRIDGE
```

Automated smoke run:

```powershell
.\tools\android\Run-RustBridgeSmoke.ps1
```

The script:

1. builds `:androidApp:assembleValidation`;
2. installs the validation APK with `adb install -r`;
3. launches the debug intent;
4. reads logcat for the run id;
5. reads the device JSON artifact;
6. verifies:
   - `decision = PASS`;
   - `diagnostic.source = RUST_CV_BRIDGE`;
   - `diagnostic.loadResult = AVAILABLE`;
7. copies JSON, Markdown, and logcat excerpt to `artifacts/dr2e-rust-bridge-smoke/<run_id>/`.

## Acceptance

DR-2E passes only if:

- a real attached Android device is used;
- validation APK installs without data-destructive uninstall;
- Rust bridge smoke JSON is exported;
- JSON reports `RUST_CV_BRIDGE`;
- load result is `AVAILABLE`;
- local artifact copy exists.

## Result

Real device run completed.

| Field | Result |
| --- | --- |
| Device | `10AF5M15FY003YL` |
| Package | `com.chromalab.app.validation` |
| Install | `adb install -r` succeeded |
| Run id | `rust_bridge_smoke_1780490538430` |
| Decision | `PASS` |
| Diagnostic source | `RUST_CV_BRIDGE` |
| Load attempted | `true` |
| Load result | `AVAILABLE` |
| Contract | `DR2D_JNI_PROBE_V1` |
| Native load time | 24 ms |
| Device artifact path | `/sdcard/Download/ChromaLab/runtime/rust-bridge-smoke/rust_bridge_smoke_1780490538430/` |
| Local artifact path | `artifacts/dr2e-rust-bridge-smoke/rust_bridge_smoke_1780490538430/` |

Local artifacts:

- `rust_bridge_smoke_rust_bridge_smoke_1780490538430.json`
- `rust_bridge_smoke_rust_bridge_smoke_1780490538430.md`
- `rust_bridge_smoke_rust_bridge_smoke_1780490538430.logcat.txt`

The smoke run proves Android packaging and JNI loading only. It does not move graph detection, calibration, trace extraction, peak metrics, or chromatographic calculations into Rust.
