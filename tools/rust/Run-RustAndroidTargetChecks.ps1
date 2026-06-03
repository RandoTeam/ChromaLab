param(
    [string[]]$Targets = @("aarch64-linux-android", "x86_64-linux-android"),
    [switch]$InstallMissingTargets
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\Enter-AndroidNdkRustEnvironment.ps1" | Out-Host

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

Push-Location (Join-Path $repoRoot "rust")
try {
    if ($InstallMissingTargets) {
        foreach ($target in $Targets) {
            rustup target add $target
            if ($LASTEXITCODE -ne 0) {
                throw "rustup target add $target failed with exit code $LASTEXITCODE"
            }
        }
    }

    $installedTargets = rustup target list --installed
    foreach ($target in $Targets) {
        if ($installedTargets -notcontains $target) {
            throw "Rust target is not installed: $target. Run tools\rust\Install-RustAndroidTargets.ps1."
        }

        cargo check --workspace --target $target
        if ($LASTEXITCODE -ne 0) {
            throw "cargo check --target $target failed with exit code $LASTEXITCODE"
        }
    }
}
finally {
    Pop-Location
}
