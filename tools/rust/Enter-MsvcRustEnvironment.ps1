param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$toolchainRoot = Join-Path $repoRoot "artifacts\rust-toolchain"
$vsRoot = "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools"
$vcVersionPath = Join-Path $vsRoot "VC\Auxiliary\Build\Microsoft.VCToolsVersion.default.txt"
$windowsKitRoot = "C:\Program Files (x86)\Windows Kits\10"

if (-not (Test-Path $vcVersionPath)) {
    throw "MSVC tools version file not found: $vcVersionPath"
}

$vcVersion = (Get-Content -LiteralPath $vcVersionPath -Raw).Trim()
$sdkVersion = (Get-ChildItem -LiteralPath (Join-Path $windowsKitRoot "Lib") -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1).Name

if (-not $sdkVersion) {
    throw "Windows SDK Lib directory was not found under $windowsKitRoot"
}

$msvcRoot = Join-Path $vsRoot "VC\Tools\MSVC\$vcVersion"
$msvcBin = Join-Path $msvcRoot "bin\Hostx64\x64"
$msvcInclude = Join-Path $msvcRoot "include"
$msvcLib = Join-Path $msvcRoot "lib\x64"
$sdkInclude = Join-Path $windowsKitRoot "Include\$sdkVersion"
$sdkLib = Join-Path $windowsKitRoot "Lib\$sdkVersion"

$requiredPaths = @(
    (Join-Path $msvcBin "cl.exe"),
    (Join-Path $msvcBin "link.exe"),
    $msvcInclude,
    $msvcLib,
    (Join-Path $sdkInclude "ucrt"),
    (Join-Path $sdkInclude "um"),
    (Join-Path $sdkLib "ucrt\x64"),
    (Join-Path $sdkLib "um\x64")
)

foreach ($path in $requiredPaths) {
    if (-not (Test-Path $path)) {
        throw "Required MSVC/SDK path not found: $path"
    }
}

$env:RUSTUP_HOME = Join-Path $toolchainRoot "rustup"
$env:CARGO_HOME = Join-Path $toolchainRoot "cargo"
$env:PATH = "$msvcBin;$($env:CARGO_HOME)\bin;$env:PATH"
$env:INCLUDE = @(
    $msvcInclude,
    (Join-Path $sdkInclude "ucrt"),
    (Join-Path $sdkInclude "shared"),
    (Join-Path $sdkInclude "um"),
    (Join-Path $sdkInclude "winrt"),
    (Join-Path $sdkInclude "cppwinrt")
) -join ";"
$env:LIB = @(
    $msvcLib,
    (Join-Path $sdkLib "ucrt\x64"),
    (Join-Path $sdkLib "um\x64")
) -join ";"
$env:LIBPATH = $env:LIB

[pscustomobject]@{
    repoRoot = $repoRoot
    rustupHome = $env:RUSTUP_HOME
    cargoHome = $env:CARGO_HOME
    msvcVersion = $vcVersion
    windowsSdkVersion = $sdkVersion
    msvcBin = $msvcBin
}
