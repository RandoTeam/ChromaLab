param(
    [string]$Toolchain = "stable",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$toolchainRoot = Join-Path $repoRoot "artifacts\rust-toolchain"
$rustupInit = Join-Path $toolchainRoot "rustup-init.exe"
$cargo = Join-Path $toolchainRoot "cargo\bin\cargo.exe"

if ((Test-Path $cargo) -and -not $Force) {
    Write-Host "Local Rust toolchain already exists: $cargo"
    & $cargo --version
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path $toolchainRoot | Out-Null

if (-not (Test-Path $rustupInit)) {
    & curl.exe -L --fail --output $rustupInit https://static.rust-lang.org/rustup/dist/x86_64-pc-windows-msvc/rustup-init.exe
    if ($LASTEXITCODE -ne 0) {
        throw "rustup-init download failed with exit code $LASTEXITCODE"
    }
}

$env:RUSTUP_HOME = Join-Path $toolchainRoot "rustup"
$env:CARGO_HOME = Join-Path $toolchainRoot "cargo"

& $rustupInit -y --no-modify-path --profile minimal --default-toolchain $Toolchain
if ($LASTEXITCODE -ne 0) {
    throw "rustup-init failed with exit code $LASTEXITCODE"
}

& $cargo --version
