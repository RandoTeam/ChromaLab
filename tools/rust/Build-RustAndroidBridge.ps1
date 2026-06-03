param(
    [string]$OutputJniLibs = "androidApp\build\generated\rustJniLibs",
    [string]$Target = "aarch64-linux-android",
    [string]$Abi = "arm64-v8a"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
& "$PSScriptRoot\Enter-AndroidNdkRustEnvironment.ps1" | Out-Host

Push-Location (Join-Path $repoRoot "rust")
try {
    rustup target add $Target
    if ($LASTEXITCODE -ne 0) {
        throw "rustup target add $Target failed with exit code $LASTEXITCODE"
    }

    cargo build --package chromalab-cv-core --target $Target
    if ($LASTEXITCODE -ne 0) {
        throw "cargo build --target $Target failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

$sourceLibrary = Join-Path $repoRoot "rust\target\$Target\debug\libchromalab_cv_core.so"
if (-not (Test-Path $sourceLibrary)) {
    throw "Rust Android bridge library was not produced: $sourceLibrary"
}

$resolvedOutput = Join-Path $repoRoot $OutputJniLibs
if ([System.IO.Path]::IsPathRooted($OutputJniLibs)) {
    $resolvedOutput = $OutputJniLibs
}

$abiOutput = Join-Path $resolvedOutput $Abi
New-Item -ItemType Directory -Force -Path $abiOutput | Out-Null
$targetLibrary = Join-Path $abiOutput "libchromalab_cv_core.so"
Copy-Item -Force -Path $sourceLibrary -Destination $targetLibrary

[pscustomobject]@{
    target = $Target
    abi = $Abi
    sourceLibrary = $sourceLibrary
    packagedLibrary = $targetLibrary
}
