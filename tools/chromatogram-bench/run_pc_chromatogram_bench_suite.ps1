param(
    [string]$OutputRoot = "build\phase-pc-algorithm-audit",
    [string]$ReplayRoot = "",
    [switch]$SkipGradleBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$fixtureRoot = Join-Path $repoRoot "composeApp\src\desktopTest\resources\fixtures\chromatogram_bench"
$resolvedOutputRoot = Join-Path $repoRoot $OutputRoot
$resolvedReplayRoot = if ($ReplayRoot.Trim()) {
    if ([System.IO.Path]::IsPathRooted($ReplayRoot)) {
        $ReplayRoot
    } else {
        Join-Path $repoRoot $ReplayRoot
    }
} else {
    $null
}
$originalReplayFile = [Environment]::GetEnvironmentVariable("CHROMALAB_DESKTOP_VLM_RESPONSE_FILE", "Process")

$fixtures = @(
    @{ id = "bench_01_mz71_screenshot_page"; ext = "jpg"; graphs = 2 },
    @{ id = "bench_02_mz92_belyi_tigr"; ext = "jpg"; graphs = 1 },
    @{ id = "bench_03_small_tic_export"; ext = "jpg"; graphs = 1 },
    @{ id = "bench_04_stacked_xic_resolution"; ext = "png"; graphs = 4 },
    @{ id = "bench_05_tic_plus_ions"; ext = "png"; graphs = 4 },
    @{ id = "bench_06_photo_two_graphs_page"; ext = "jpg"; graphs = 2 },
    @{ id = "bench_07_rotated_page_photo"; ext = "jpg"; graphs = 1 },
    @{ id = "bench_08_mz71_duplicate_candidate"; ext = "jpg"; graphs = 1 }
)

$replayAliases = @{
    bench_07_rotated_page_photo = @("axis_vlm_replay_bench_07.json")
}

function Get-FixtureReplayFile {
    param(
        [string]$FixtureId
    )

    if (-not $resolvedReplayRoot) {
        return ""
    }

    $candidateNames = @("axis_vlm_replay_$FixtureId.json")
    if ($replayAliases.ContainsKey($FixtureId)) {
        $candidateNames += $replayAliases[$FixtureId]
    }

    foreach ($candidateName in $candidateNames) {
        $candidatePath = Join-Path $resolvedReplayRoot $candidateName
        if (Test-Path $candidatePath) {
            return (Resolve-Path $candidatePath).Path
        }
    }
    return ""
}

function Set-FixtureReplayFile {
    param(
        [string]$ReplayFile
    )

    if ($ReplayFile) {
        $env:CHROMALAB_DESKTOP_VLM_RESPONSE_FILE = $ReplayFile
        return $ReplayFile
    }

    if ($originalReplayFile) {
        $env:CHROMALAB_DESKTOP_VLM_RESPONSE_FILE = $originalReplayFile
        return $originalReplayFile
    }

    Remove-Item Env:\CHROMALAB_DESKTOP_VLM_RESPONSE_FILE -ErrorAction SilentlyContinue
    return ""
}

if (-not $SkipGradleBuild) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat :composeApp:compileKotlinDesktop
        if ($LASTEXITCODE -ne 0) {
            throw "compileKotlinDesktop failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

New-Item -ItemType Directory -Force -Path $resolvedOutputRoot | Out-Null

$rows = @()

Push-Location $repoRoot
try {
    foreach ($fixture in $fixtures) {
        $imagePath = Join-Path $fixtureRoot ($fixture.id + "." + $fixture.ext)
        $outputPath = Join-Path $resolvedOutputRoot $fixture.id
        $activeReplayFile = Set-FixtureReplayFile -ReplayFile (Get-FixtureReplayFile -FixtureId $fixture.id)

        & .\gradlew.bat :composeApp:run --args="--offline-analysis --image `"$imagePath`" --out `"$outputPath`" --source $($fixture.id) --expected-graphs $($fixture.graphs)"
        if ($LASTEXITCODE -ne 0) {
            throw "offline analysis failed for $($fixture.id) with exit code $LASTEXITCODE"
        }

        $auditPath = Join-Path $outputPath "audit.json"
        $audit = Get-Content -Raw -Path $auditPath | ConvertFrom-Json
        $rows += [pscustomobject]@{
            fixture = $audit.sourceId
            expectedGraphs = $audit.expectedGraphCount
            detectedGraphs = $audit.detectedGraphCount
            readyForCalculation = $audit.readyForCalculation
            blockedAtStage = if ($audit.blockedAtStage) { $audit.blockedAtStage } else { "not_blocked" }
            graphs = $audit.graphs.Count
            calibratedGraphs = @($audit.graphs | Where-Object { $_.axisCalibration.ready -eq $true }).Count
            signalReadyGraphs = @($audit.graphs | Where-Object { $_.signal.ready -eq $true }).Count
            peakReadyGraphs = @($audit.graphs | Where-Object { $_.peakDetection.ready -eq $true }).Count
            axisReplayFile = $activeReplayFile
            auditPath = $auditPath
        }
    }
} finally {
    if ($originalReplayFile) {
        $env:CHROMALAB_DESKTOP_VLM_RESPONSE_FILE = $originalReplayFile
    } else {
        Remove-Item Env:\CHROMALAB_DESKTOP_VLM_RESPONSE_FILE -ErrorAction SilentlyContinue
    }
    Pop-Location
}

$summaryPath = Join-Path $resolvedOutputRoot "pc_chromatogram_bench_summary.csv"
$rows | Export-Csv -Path $summaryPath -NoTypeInformation -Encoding UTF8
$rows | Format-Table -AutoSize
Write-Host "Summary: $summaryPath"
