# 2026-06-03 SETUP-1 Workstation And Pipeline Research

Scope: official/current references used to prepare the workstation and define the next ChromaLab execution process.

## Sources Checked

- Rust install:
  https://www.rust-lang.org/tools/install
  - Relevant because Rust/Cargo must be installed through rustup.
  - Decision: keep local ignored Rust toolchain for repeatability in this repo session.

- Microsoft Rust setup on Windows:
  https://learn.microsoft.com/windows/dev-environment/rust/setup
  - Relevant because Rust on Windows requires Microsoft C++ Build Tools.
  - Decision: use MSVC Build Tools, not workaround-only compile checks.

- Visual Studio Build Tools workload/component IDs:
  https://learn.microsoft.com/visualstudio/install/workload-component-id-vs-build-tools?view=vs-2022
  - Relevant because `Microsoft.VisualStudio.Workload.VCTools` is the C++ command-line tool workload.
  - Decision: verify Build Tools and VCTools are installed.

- Visual Studio command-line install parameters:
  https://learn.microsoft.com/visualstudio/install/use-command-line-parameters-to-install-visual-studio?view=vs-2022
  - Relevant for repeatable installation if the toolchain is missing on another machine.
  - Decision: document installation path and use scripts for local environment setup.

- MSVC command-line build tools:
  https://learn.microsoft.com/cpp/build/building-on-the-command-line?view=msvc-170
  - Relevant because `cl.exe`/`link.exe` need a configured command-line environment.
  - Decision: create a PowerShell MSVC environment script because `vcvars64.bat` fails in this shell.

- Android NDK architectures:
  https://developer.android.com/ndk/guides/arch
  - Relevant for future Rust Android ABI builds.
  - Decision: Android NDK setup becomes the next phase, not part of SETUP-1.

## Workstation Finding

Visual Studio Build Tools 2022 was already installed:

- version `17.14.33`;
- MSVC `14.44.35207`;
- Windows SDK `10.0.26100.0`.

`vcvars64.bat` and `LaunchDevCmd.bat` failed in this shell with `\Arm was unexpected at this time`, so the reliable local path is a PowerShell environment setup that directly sets:

- `PATH`;
- `INCLUDE`;
- `LIB`;
- `LIBPATH`;
- `RUSTUP_HOME`;
- `CARGO_HOME`.

This enabled real `cargo test --workspace`.
