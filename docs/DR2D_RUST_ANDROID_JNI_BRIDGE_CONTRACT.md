# DR-2D Rust Android JNI Bridge Contract

Status: `DR2D_JNI_BRIDGE_PACKAGING_READY`

Purpose: prove that Android can package and call ChromaLab's Rust CV core through a minimal JNI contract without moving chromatographic math or analysis authority into Rust.

## Scope

Implemented:

1. Rust `cdylib` build for `chromalab-cv-core`;
2. Android `arm64-v8a` Rust `.so` packaging through Gradle;
3. Kotlin Android probe that calls Rust through JNI;
4. runtime evidence diagnostic that records Rust bridge status;
5. tests proving desktop evidence reports the bridge as Android-only and non-authoritative.

Not implemented:

1. image transfer through JNI;
2. graph detection through Rust;
3. axis/tick/calibration through Rust;
4. trace or peak calculations through Rust;
5. any change to `CalculationEngine`.

## Agents Activated

- Orchestrator: kept the phase limited to JNI proof and packaging.
- Research Intelligence: checked Android JNI and Rust FFI official guidance.
- Android Performance & On-Device AI: owned APK packaging and native library loading constraints.
- Geometry / Calibration Core: verified Rust bridge has no geometry/calibration authority.
- QA / Regression: added Rust and Kotlin tests plus Android build validation.
- Scientific Reporting / Validation: ensured runtime evidence labels the bridge as diagnostic.
- Security / Privacy: ensured diagnostics do not include private filesystem paths.
- Product Acceptance: accepted this as infrastructure only, not analysis readiness.

## Contract

Rust exports one JNI method:

```text
com.chromalab.feature.processing.rust.RustCvBridge.nativeProbeJson(): String
```

The returned JSON declares:

- bridge id;
- bridge version;
- FFI contract id;
- native load status;
- no algorithm authority;
- no pixel geometry authority;
- no calibration authority;
- no peak metric authority;
- no `CalculationEngine` change.

## Runtime Evidence

`RuntimeEvidencePackageBuilder` now includes a `StructuredRuntimeDiagnostic` with:

- `source = RUST_CV_BRIDGE`;
- `backend = Rust chromalab-cv-core`;
- `runtimeExposesMtp = DR2D_JNI_PROBE_V1`;
- `mtpEnabled = NOT_APPLICABLE`;
- technical-evidence privacy class.

On Android the diagnostic attempts to load and call the Rust library.

On desktop the diagnostic reports `not_packaged_on_desktop` and does not attempt native loading.

## Build Flow

Gradle task:

```text
:androidApp:buildRustAndroidBridge
```

Runs:

```powershell
.\tools\rust\Build-RustAndroidBridge.ps1
```

Output:

```text
androidApp/build/generated/rustJniLibs/arm64-v8a/libchromalab_cv_core.so
```

The Android app packages that library through `jniLibs`.

## Official References

- Android JNI tips: https://developer.android.com/training/articles/perf-jni
- Android NDK CMake/Gradle guidance: https://developer.android.com/ndk/guides/cmake
- Rust FFI guidance: https://doc.rust-lang.org/nomicon/ffi.html
- Rust Android target support: https://doc.rust-lang.org/rustc/platform-support/android.html

## Validation

Commands run:

```powershell
.\tools\rust\Run-RustAndroidTargetChecks.ps1
.\tools\rust\Run-RustCoreTestsMsvc.ps1
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :androidApp:assembleValidation --no-daemon --console=plain
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RustCvBridgeRuntimeProbeTest"
git diff --check
```

Results:

- Rust core checks passed.
- Rust MSVC tests passed: 7 tests.
- Rust Android target checks passed for `aarch64-linux-android` and `x86_64-linux-android`.
- Kotlin desktop compile passed.
- Targeted desktop probe test passed.
- Android validation APK build passed.
- APK inspection confirmed:
  - `lib/arm64-v8a/libchromalab_cv_core.so`;
  - `lib/arm64-v8a/libllama_bridge.so`.

## Next Phase

`DR-2E`: add an Android-only bridge smoke run that installs the validation APK, launches a debug intent, and exports Rust bridge status from a real device.

That phase still must not transfer chromatogram analysis into Rust.
