param(
    [string]$NdkVersion = "27.3.13750724",
    [int]$MinSdk = 26
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$toolchainRoot = Join-Path $repoRoot "artifacts\rust-toolchain"

$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) {
    $sdkRoot = $env:ANDROID_SDK_ROOT
}
if (-not $sdkRoot) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not (Test-Path $sdkRoot)) {
    throw "Android SDK was not found. Set ANDROID_HOME or install Android Studio command-line tools."
}

$ndkRoot = Join-Path $sdkRoot "ndk\$NdkVersion"
if (-not (Test-Path $ndkRoot)) {
    throw "Android NDK $NdkVersion was not found at $ndkRoot. Install it with sdkmanager --install `"ndk;$NdkVersion`"."
}

$toolchainBin = Join-Path $ndkRoot "toolchains\llvm\prebuilt\windows-x86_64\bin"
if (-not (Test-Path $toolchainBin)) {
    throw "Android NDK LLVM toolchain was not found at $toolchainBin."
}

$requiredTools = @(
    "aarch64-linux-android$MinSdk-clang.cmd",
    "x86_64-linux-android$MinSdk-clang.cmd",
    "llvm-ar.exe"
)

foreach ($tool in $requiredTools) {
    $toolPath = Join-Path $toolchainBin $tool
    if (-not (Test-Path $toolPath)) {
        throw "Required Android NDK tool was not found: $toolPath"
    }
}

$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_NDK_HOME = $ndkRoot
$env:NDK_HOME = $ndkRoot
$env:RUSTUP_HOME = Join-Path $toolchainRoot "rustup"
$env:CARGO_HOME = Join-Path $toolchainRoot "cargo"
$env:PATH = "$toolchainBin;$($env:CARGO_HOME)\bin;$env:PATH"

[pscustomobject]@{
    repoRoot = $repoRoot
    androidSdkRoot = $sdkRoot
    androidNdkRoot = $ndkRoot
    ndkVersion = $NdkVersion
    minSdk = $MinSdk
    toolchainBin = $toolchainBin
    rustupHome = $env:RUSTUP_HOME
    cargoHome = $env:CARGO_HOME
}
