# 2026-06-03 DR-2C Android NDK And Rust Target Research

Scope: current official references for preparing Rust Android target compilation in ChromaLab.

## Sources Checked

- Rust Android platform support:
  https://doc.rust-lang.org/rustc/platform-support/android.html
  - Rust Android targets are cross-compiled and use the Android NDK.
  - The page lists `aarch64-linux-android` and `x86_64-linux-android` among supported targets.
  - It states Rust supports the most recent LTS Android NDK.

- Android NDK downloads:
  https://developer.android.com/ndk/downloads
  - Latest LTS version: r27d, `27.3.13750724`.
  - Latest stable version: r29, `29.0.14206865`.
  - Latest pre-release version: r30 beta 1, `30.0.14518594-beta1`.

- Android `sdkmanager`:
  https://developer.android.com/tools/sdkmanager
  - `sdkmanager` installs packages like `"ndk;major.minor.build"` and CMake.
  - Package paths must be quoted.
  - Licenses must be accepted for installed packages.

- Android Gradle Plugin NDK configuration:
  https://developer.android.com/studio/projects/configure-agp-ndk
  - For reproducible projects, set `ndkVersion`.
  - Android SDK installs side-by-side NDK versions under `android-sdk/ndk/<version>`.

## Decision

Use NDK LTS r27d (`27.3.13750724`) for ChromaLab's current Android/Rust setup because:

1. Rust Android support references the most recent LTS NDK;
2. r27d is already installed locally;
3. using LTS reduces churn before JNI/Rust packaging work;
4. Gradle already uses Android native build with CMake, so pinning `ndkVersion` keeps builds reproducible.

Stable r29 is documented for future evaluation, but not adopted in DR-2C. A stable-NDK upgrade should be its own phase with Android build, device, and native runtime checks.
