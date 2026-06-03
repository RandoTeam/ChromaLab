param(
    [string]$InputRoot = "build\dr1r-axis-label-crop-sweep",
    [string]$OutputRoot = "build\setup1-rust-bridge-corpus"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedInputRoot = if ([System.IO.Path]::IsPathRooted($InputRoot)) {
    $InputRoot
} else {
    Join-Path $repoRoot $InputRoot
}
$resolvedOutputRoot = if ([System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot
} else {
    Join-Path $repoRoot $OutputRoot
}

& (Join-Path $PSScriptRoot "Enter-MsvcRustEnvironment.ps1") | Out-Host

Push-Location (Join-Path $repoRoot "rust")
try {
    cargo build --bin chromalab_cv_bridge
    if ($LASTEXITCODE -ne 0) {
        throw "cargo build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$bridgeExe = Join-Path $repoRoot "rust\target\debug\chromalab_cv_bridge.exe"
if (-not (Test-Path $bridgeExe)) {
    throw "Rust bridge executable not found: $bridgeExe"
}

New-Item -ItemType Directory -Force -Path $resolvedOutputRoot | Out-Null

$rows = @()
Get-ChildItem -Path $resolvedInputRoot -Recurse -Filter axis_element_graph.json | ForEach-Object {
    $graphJson = $_.FullName
    $fixture = $_.Directory.Parent.Name
    $graphDir = $_.Directory.Name
    $auditPath = Join-Path $_.Directory.Parent.FullName "audit.json"
    if (-not (Test-Path $auditPath)) {
        throw "Audit JSON not found for $graphJson"
    }

    $audit = Get-Content -LiteralPath $auditPath -Raw | ConvertFrom-Json
    $imageWidth = [int]$audit.perspectiveGeometry.imageWidth
    $imageHeight = [int]$audit.perspectiveGeometry.imageHeight
    $fixtureOut = Join-Path $resolvedOutputRoot $fixture
    New-Item -ItemType Directory -Force -Path $fixtureOut | Out-Null
    $reportOut = Join-Path $fixtureOut ($graphDir + "_rust_crop_plan.json")

    & $bridgeExe $graphJson $imageWidth $imageHeight | Set-Content -LiteralPath $reportOut -Encoding UTF8
    if ($LASTEXITCODE -ne 0) {
        throw "Rust bridge failed for $graphJson"
    }

    $report = Get-Content -LiteralPath $reportOut -Raw | ConvertFrom-Json
    $rows += [pscustomobject]@{
        fixture = $fixture
        graph = $graphDir
        imageWidth = $imageWidth
        imageHeight = $imageHeight
        sourceBands = $report.source_label_band_count
        accepted = $report.crop_plan.accepted.Count
        rejected = $report.crop_plan.rejected.Count
        report = $reportOut
    }
}

$summaryPath = Join-Path $resolvedOutputRoot "rust_bridge_corpus_summary.csv"
$rows | Sort-Object fixture, graph | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $summaryPath
$rows | Sort-Object fixture, graph | Format-Table -AutoSize
Write-Host "Summary: $summaryPath"
