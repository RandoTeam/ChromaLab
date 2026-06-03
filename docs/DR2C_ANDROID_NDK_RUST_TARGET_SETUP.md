# DR-2C Android NDK And Rust Android Target Setup

Status: `DR2C_ANDROID_TARGETS_READY`

Purpose: prepare ChromaLab's Rust CV core for Android target compilation without changing chromatographic algorithms or connecting JNI yet.

## Scope

This phase sets up Android target readiness only:

1. verify Android SDK/NDK;
2. align Gradle with the current Android NDK LTS release;
3. install Rust Android targets;
4. configure Cargo linkers through the NDK LLVM toolchain;
5. run Android-target Rust checks.

JNI, Kotlin bridge code, native packaging, and algorithm changes are intentionally deferred.

## Agents Activated

- Orchestrator: kept this as a setup-only phase and prevented JNI scope creep.
- Research Intelligence: checked current official Android/Rust requirements.
- Android Performance & On-Device AI: verified ABI and NDK implications for future device work.
- Geometry / Calibration Core: confirmed Rust CV core remains algorithmically unchanged.
- QA / Regression: added repeatable target-check scripts and ran validation.
- Security / Privacy: kept user-specific SDK paths out of committed Cargo config.
- Product Acceptance: accepted setup as infrastructure only, not product readiness.

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `android-runtime-profiling`
- `test-plan-authoring`
- `definition-of-done`
- `secure-export-review`

## Current Official Guidance

- Rust Android targets require Android NDK cross-compilation support.
- Rust states Android target builds need the most recent LTS Android NDK.
- Android NDK downloads currently list:
  - LTS r27d: `27.3.13750724`;
  - stable r29: `29.0.14206865`;
  - pre-release r30 beta 1: `30.0.14518594-beta1`.
- Android Gradle Plugin guidance recommends using `ndkVersion` for reproducible projects.

Sources:

- https://doc.rust-lang.org/rustc/platform-support/android.html
- https://developer.android.com/ndk/downloads
- https://developer.android.com/tools/sdkmanager
- https://developer.android.com/studio/projects/configure-agp-ndk

## Local Workstation Result

| Area | Result |
| --- | --- |
| Android SDK | `C:\Users\Ilia\AppData\Local\Android\Sdk` |
| NDK LTS | `27.3.13750724` installed |
| CMake | `3.22.1` installed |
| Gradle `ndkVersion` | updated to `27.3.13750724` |
| App ABI filter | still `arm64-v8a` |
| Rust phone target | `aarch64-linux-android` installed and checked |
| Rust emulator target | `x86_64-linux-android` installed and checked |
| Min Android API for Rust linker config | 26, matching app `minSdk` |

## Files Added

- `rust/.cargo/config.toml`
- `tools/rust/Enter-AndroidNdkRustEnvironment.ps1`
- `tools/rust/Install-RustAndroidTargets.ps1`
- `tools/rust/Run-RustAndroidTargetChecks.ps1`

## Repeatable Commands

Install Rust Android targets:

```powershell
.\tools\rust\Install-RustAndroidTargets.ps1
```

Run Android target checks:

```powershell
.\tools\rust\Run-RustAndroidTargetChecks.ps1
```

Run with target installation if a fresh machine is missing targets:

```powershell
.\tools\rust\Run-RustAndroidTargetChecks.ps1 -InstallMissingTargets
```

## Validation

Commands run:

```powershell
.\tools\rust\Install-RustAndroidTargets.ps1
.\tools\rust\Run-RustAndroidTargetChecks.ps1
.\tools\rust\Run-RustCoreChecks.ps1
.\tools\rust\Run-RustCoreTestsMsvc.ps1
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :androidApp:assembleValidation
git diff --check
```

Expected DR-2C acceptance:

- Gradle Android build still passes with NDK r27d.
- Rust core still passes desktop checks.
- Rust Android target checks pass for `aarch64-linux-android`.
- `x86_64-linux-android` also passes for emulator readiness.

## Next Phase

`DR-2D`: design a small, auditable JNI/FFI bridge contract for Rust CV outputs.

That phase must not move chromatographic calculations into Rust yet. It should first prove:

1. Kotlin can call a Rust native library;
2. Rust can return a deterministic structured result;
3. Android packaging includes the native library for `arm64-v8a`;
4. evidence export records Rust bridge version and status.
