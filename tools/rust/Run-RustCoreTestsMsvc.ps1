param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
& (Join-Path $PSScriptRoot "Enter-MsvcRustEnvironment.ps1") | Out-Host

Push-Location (Join-Path $repoRoot "rust")
try {
    cargo test --workspace
    if ($LASTEXITCODE -ne 0) {
        throw "cargo test failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
