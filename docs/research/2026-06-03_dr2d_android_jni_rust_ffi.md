# 2026-06-03 DR-2D Android JNI And Rust FFI Research

Scope: official references used for the minimal Rust Android JNI bridge.

## Sources Checked

- Android JNI tips:
  https://developer.android.com/training/articles/perf-jni
  - JNI should keep the interface small and minimize marshalling.
  - `System.loadLibrary` loads native shared libraries.
  - JNI references and thread rules matter once callbacks or long-running calls are introduced.
  - DR-2D follows the small-interface rule with one diagnostic string method.

- Android NDK CMake/Gradle guidance:
  https://developer.android.com/ndk/guides/cmake
  - Android Gradle Plugin integrates native code through `externalNativeBuild`.
  - DR-2D keeps the existing CMake llama bridge untouched and packages Rust separately through `jniLibs`.

- Rust FFI guidance:
  https://doc.rust-lang.org/nomicon/ffi.html
  - FFI boundaries require explicit C-compatible contracts and careful string handling.
  - DR-2D avoids passing image buffers or structs and returns only a UTF-8 diagnostic JSON string through JNI.

- Rust Android target support:
  https://doc.rust-lang.org/rustc/platform-support/android.html
  - Android targets are cross-compiled through the NDK.
  - DR-2C already prepared `aarch64-linux-android`; DR-2D uses it for the packaged bridge.

## Decision

Use a minimal Rust JNI diagnostic method instead of a large FFI surface.

Reason:

1. proves Android can load Rust;
2. proves Gradle can package the Rust library;
3. avoids image/pixel/calculation marshalling before the contract is reviewed;
4. keeps `CalculationEngine` and scientific outputs unchanged.
