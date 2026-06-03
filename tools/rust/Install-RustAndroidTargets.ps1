param(
    [string[]]$Targets = @("aarch64-linux-android", "x86_64-linux-android")
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\Enter-AndroidNdkRustEnvironment.ps1" | Out-Host

Push-Location (Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path "rust")
try {
    foreach ($target in $Targets) {
        rustup target add $target
        if ($LASTEXITCODE -ne 0) {
            throw "rustup target add $target failed with exit code $LASTEXITCODE"
        }
    }

    rustup target list --installed
}
finally {
    Pop-Location
}
