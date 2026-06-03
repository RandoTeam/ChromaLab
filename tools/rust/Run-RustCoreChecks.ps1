param(
    [switch]$SkipFormat
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$localToolchainRoot = Join-Path $repoRoot "artifacts\rust-toolchain"
$localCargo = Join-Path $localToolchainRoot "cargo\bin\cargo.exe"
$localRustup = Join-Path $localToolchainRoot "cargo\bin\rustup.exe"

if (Test-Path $localCargo) {
    $env:RUSTUP_HOME = Join-Path $localToolchainRoot "rustup"
    $env:CARGO_HOME = Join-Path $localToolchainRoot "cargo"
    $cargo = $localCargo
    $rustup = $localRustup
} else {
    $cargoCommand = Get-Command cargo -ErrorAction SilentlyContinue
    if (-not $cargoCommand) {
        throw "Cargo was not found. Install Rust with rustup or run the local DR-2A rustup setup first."
    }
    $cargo = $cargoCommand.Source
    $rustupCommand = Get-Command rustup -ErrorAction SilentlyContinue
    $rustup = if ($rustupCommand) { $rustupCommand.Source } else { "" }
}

Push-Location (Join-Path $repoRoot "rust")
try {
    if ($rustup) {
        & $rustup component add rustfmt
        if ($LASTEXITCODE -ne 0) {
            throw "rustup component add rustfmt failed with exit code $LASTEXITCODE"
        }
    }

    if (-not $SkipFormat) {
        & $cargo fmt --all -- --check
        if ($LASTEXITCODE -ne 0) {
            throw "cargo fmt check failed with exit code $LASTEXITCODE"
        }
    }

    & $cargo check --workspace --all-targets
    if ($LASTEXITCODE -ne 0) {
        throw "cargo check failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
