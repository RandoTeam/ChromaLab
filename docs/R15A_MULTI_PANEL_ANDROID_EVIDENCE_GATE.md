# R15A - Multi-Panel Android Evidence Gate

Date: 2026-06-08

Status: `R15A_RETRY_BLOCKED_NO_ADB_TARGET`

Verdict: `PHASE_9_REMAINS_BLOCKED_ON_VALIDATION_COVERAGE`

## Scope

R15A is an evidence gate for the R15 multi-panel runtime propagation work. It
does not change graph detection, calibration, trace extraction, peak metrics,
validators, E2B authority, report gates, chromatographic math, or
`CalculationEngine`.

The gate was intended to prove, on Android, that R15 preserves physical
multi-panel graph units and report/evidence propagation for:

| Fixture | Expected graph units | Gate requirement |
|---|---:|---|
| `bench_04_stacked_xic_resolution` | 4 | Four resolved physical graph packages and report graph sections, or explicit unsupported aggregation warning. |
| `bench_05_tic_plus_ions` | 4 | Four resolved physical graph packages; TIC+ion semantics remain text-supported only and cannot create count. |
| `bench_06_photo_two_graphs_page` | 2 | Two resolved physical graph packages in reading order. |

Regression witnesses for the gate:

- `white_tiger_ion71`;
- `bench_03_small_tic_export`;
- `bench_07_rotated_page_photo`.

Each fixture must be run in deterministic and E2B modes before the gate can
advance to R16 trace extraction evidence.

## Pre-Flight Result

| Check | Result |
|---|---|
| Repository head | `6537f2c75bedec7a66d1db4df2776a8366dd4b8d` (`Close multi-panel graph layout propagation`) |
| Working tree before docs | Clean |
| Git CLI | Not on PATH, but available at `C:\Program Files\Git\cmd\git.exe` |
| GitHub CLI | Not on PATH, but available at `C:\Program Files\GitHub CLI\gh.exe` |
| Java | Not on PATH; JDK found at `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` |
| Android SDK | Not configured in `local.properties`; SDK found at `C:\Users\Ilia V\AppData\Local\Android\Sdk` |
| adb target | `adb devices -l` returned no connected device or emulator |

## 2026-06-14 Retry Result

The R15A retry was started from current `master` after the latest synced commit:

```text
c36d650991f5c62f6a1fa1ad25250796d5ebe762
Improve Android validation failure evidence export
```

Pre-flight checks:

| Check | Result |
|---|---|
| Working tree | Clean before retry |
| GitHub sync | `HEAD == origin/master` |
| `adb devices` | No connected device or emulator listed |
| `.\gradlew.bat :androidApp:assembleValidation` | PASS |
| Validation APK | `androidApp/build/outputs/apk/validation/androidApp-validation.apk` |

This retry closes the previous validation APK build/toolchain blocker on this
machine. The remaining R15A blocker is Android device coverage: adb does not see
a target, so no deterministic/E2B fixture reruns or R15 evidence artifacts could
be collected.

## Previous Validation APK Build Result

The first validation APK build attempt failed because `JAVA_HOME` was not set.
After setting `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` for the
command, Gradle reached native CMake build configuration but failed in the
llama/Vulkan shader-generator host toolchain step:

```text
Execution failed for task ':androidApp:buildCMakeDebug[arm64-v8a]'.
vulkan-shaders-gen configure failed.
clang.exe is not able to compile a simple Windows host test program.
lld: error: unable to find library -lkernel32
lld: error: unable to find library -luser32
...
```

No MinGW/MSYS host toolchain was found in the checked common install locations.

This previous toolchain blocker did not reproduce on 2026-06-14:
`:androidApp:assembleValidation` passed.

## Android Rerun Result

No R15A fixture reruns were executed because no adb target was connected.

No new artifacts were produced under:

```text
artifacts/r15a-multi-panel-android/
```

The current Android product truth therefore remains the existing Phase 9J/R12
truth plus the R15 implementation closeout. R15A did not create new
deterministic/E2B comparison evidence.

## Evidence Audit Status

| Evidence item | R15A status |
|---|---|
| RuntimeEvidencePackage | Not produced for R15A |
| Validator JSON/Markdown | Not produced for R15A |
| Final report JSON/HTML/Markdown | Not produced for R15A |
| Graph failure packages | Not produced for R15A |
| Per-graph packages for `bench_04`/`bench_05`/`bench_06` | Not produced for R15A |
| Report graph count vs physical graph count | Not measured on Android |
| `multi_panel_report_aggregation_unsupported` warning | Not measured on Android |
| Deterministic vs E2B regression | Not measured on Android |

## Decision

R15A cannot pass until both conditions are met:

1. A real adb device or emulator is connected and visible to adb.
2. The validation APK builds with a working host toolchain for the native
   llama/Vulkan shader-generator step.

Because no Android evidence was collected, the next executable analyzer work is
still an R15A retry after adb device readiness, not R16.

If the next R15A retry proves complete multi-panel report/evidence propagation,
the next implementation phase becomes:

```text
R16 - Trace Extraction Evidence Candidate
```

If the retry proves that multi-panel reports still collapse into incomplete
one-section output, the next implementation phase becomes:

```text
R15B - Multi-Graph Report Aggregation Closure
```

## Validation Commands Attempted

```text
git diff --check
adb devices
.\gradlew.bat :androidApp:assembleValidation
```

`git diff --check` is recorded in the final R15A task validation after this
document update.
