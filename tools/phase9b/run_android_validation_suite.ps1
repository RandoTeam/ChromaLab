param(
    [string]$Package = "com.chromalab.app.validation",
    [string]$Activity = "com.chromalab.app.MainActivity",
    [string]$SuiteId = "phase9b_all",
    [string]$OutputRoot = "artifacts/phase9b-multi-fixture-android",
    [string[]]$Fixtures = @(
        "white_tiger_ion71",
        "bench_01_mz71_screenshot_page",
        "bench_02_mz92_belyi_tigr",
        "bench_03_small_tic_export",
        "bench_04_stacked_xic_resolution",
        "bench_05_tic_plus_ions",
        "bench_06_photo_two_graphs_page",
        "bench_07_rotated_page_photo"
    ),
    [string[]]$Modes = @("deterministic", "model_enabled"),
    [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    & adb @args
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Find-RunDirectory {
    param([string]$FixtureId)
    $value = (& adb shell "ls -dt /sdcard/Download/ChromaLab/validation/${FixtureId}_* 2>/dev/null | head -n 1") -join "`n"
    return $value.Trim()
}

function Wait-RunManifest {
    param([string]$FixtureId, [int]$TimeoutSeconds, [string]$PreviousRunDirectory)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Seconds 5
        $dir = Find-RunDirectory -FixtureId $FixtureId
        if ($dir -and $dir -ne $PreviousRunDirectory) {
            $manifest = (& adb shell "ls $dir/artifact_manifest_*.json 2>/dev/null | head -n 1") -join "`n"
            if ($manifest.Trim()) {
                return $dir
            }
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for validation manifest for fixture $FixtureId"
}

function Read-JsonFileOrNull {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
}

function Summarize-Run {
    param(
        [string]$FixtureId,
        [string]$Mode,
        [string]$RunId,
        [string]$LocalRunDir
    )
    $nested = Join-Path $LocalRunDir $RunId
    if (-not (Test-Path -LiteralPath $nested)) {
        $nested = $LocalRunDir
    }
    $validatorPath = Get-ChildItem -LiteralPath $nested -Filter "runtime_evidence_validation_*.json" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    $reportPath = Get-ChildItem -LiteralPath $nested -Filter "final_report_contract_*.json" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    $manifestPath = Get-ChildItem -LiteralPath $nested -Filter "artifact_manifest_*.json" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    $evidencePath = Get-ChildItem -LiteralPath $nested -Filter "runtime_evidence_package_*.json" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    $validator = if ($validatorPath) { Read-JsonFileOrNull $validatorPath.FullName } else { $null }
    $report = if ($reportPath) { Read-JsonFileOrNull $reportPath.FullName } else { $null }
    [pscustomobject]@{
        fixtureId = $FixtureId
        mode = $Mode
        runId = $RunId
        graphCount = if ($report -and $report.metadata) { $report.metadata.detectedGraphCount } else { $null }
        reportGate = if ($validator) { $validator.reportGateStatus } else { $null }
        validatorVerdict = if ($validator) { $validator.verdict } else { $null }
        runtimeFailureClass = if ($validator) { $validator.runtimeFailureClass } else { $null }
        blockingIssueCount = if ($validator -and $validator.blockingIssues) { @($validator.blockingIssues).Count } else { $null }
        runtimeEvidencePackageAvailable = [bool]$evidencePath
        validatorJsonAvailable = [bool]$validatorPath
        validatorMarkdownAvailable = [bool](Get-ChildItem -LiteralPath $nested -Filter "runtime_evidence_validation_*.md" -File -ErrorAction SilentlyContinue | Select-Object -First 1)
        finalReportJsonAvailable = [bool]$reportPath
        exportManifestAvailable = [bool]$manifestPath
        localPath = $nested
    }
}

$deviceLines = (& adb devices) -join "`n"
if ($deviceLines -notmatch "`tdevice") {
    throw "No attached Android device found."
}

$modelCheck = (& adb shell run-as $Package ls -l files/models/gemma4-e2b 2>&1) -join "`n"
if ($modelCheck -notmatch "gemma-4-E2B-it\.litertlm") {
    throw "E2B model precheck failed for $Package. Output: $modelCheck"
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
$summary = @()

foreach ($fixture in $Fixtures) {
    foreach ($mode in $Modes) {
        try {
            Invoke-Adb shell logcat -c
            $previousRunDirectory = Find-RunDirectory -FixtureId $fixture
            Invoke-Adb shell am start -S -n "$Package/$Activity" -a "com.chromalab.app.RUN_VALIDATION_FIXTURE" --es fixture $fixture --es modelMode $mode | Out-Null
            $deviceRunDir = Wait-RunManifest -FixtureId $fixture -TimeoutSeconds $TimeoutSeconds -PreviousRunDirectory $previousRunDirectory
            $runId = Split-Path -Leaf $deviceRunDir
            $localModeDir = Join-Path (Join-Path $OutputRoot $fixture) $mode
            New-Item -ItemType Directory -Force -Path $localModeDir | Out-Null
            Invoke-Adb pull $deviceRunDir $localModeDir | Out-Null
            $localRunDir = Join-Path $localModeDir $runId
            if (-not (Test-Path -LiteralPath $localRunDir)) {
                throw "Pulled artifact directory was not created: $localRunDir"
            }
            try {
                & adb exec-out screencap -p | Set-Content -Encoding Byte -LiteralPath (Join-Path $localRunDir "final_screen.png")
            } catch {
                $_.Exception.Message | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $localRunDir "final_screen_missing_reason.txt")
            }
            & adb logcat -d -t 800 > (Join-Path $localRunDir "logcat_excerpt.txt")
            $summary += Summarize-Run -FixtureId $fixture -Mode $mode -RunId $runId -LocalRunDir $localRunDir
        } catch {
            $runId = "${fixture}_$mode`_failed"
            $localRunDir = Join-Path (Join-Path (Join-Path $OutputRoot $fixture) $mode) $runId
            New-Item -ItemType Directory -Force -Path $localRunDir | Out-Null
            $_.Exception.Message | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $localRunDir "runner_failure.txt")
            & adb logcat -d -t 800 > (Join-Path $localRunDir "logcat_excerpt.txt")
            $summary += [pscustomobject]@{
                fixtureId = $fixture
                mode = $mode
                runId = $runId
                graphCount = $null
                reportGate = $null
                validatorVerdict = $null
                runtimeFailureClass = "UNKNOWN_FAILURE"
                blockingIssueCount = $null
                runtimeEvidencePackageAvailable = $false
                validatorJsonAvailable = $false
                validatorMarkdownAvailable = $false
                finalReportJsonAvailable = $false
                exportManifestAvailable = $false
                localPath = $localRunDir
                runnerError = $_.Exception.Message
            }
        }
    }
}

$summaryPath = Join-Path $OutputRoot "phase9b_suite_summary.json"
$summary | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -LiteralPath $summaryPath

$mdPath = Join-Path $OutputRoot "phase9b_suite_summary.md"
$lines = @(
    "# Phase 9B Android Validation Suite",
    "",
    "- Suite: ``$SuiteId``",
    "- Package: ``$Package``",
    "- Fixtures: $($Fixtures.Count)",
    "- Modes: $($Modes -join ', ')",
    "",
    "| Fixture | Mode | Graphs | Gate | Validator | Failure | Blocking | Export |",
    "| --- | --- | ---: | --- | --- | --- | ---: | --- |"
)
foreach ($row in $summary) {
    $exportOk = $row.runtimeEvidencePackageAvailable -and $row.validatorJsonAvailable -and $row.validatorMarkdownAvailable -and $row.finalReportJsonAvailable -and $row.exportManifestAvailable
    $lines += "| $($row.fixtureId) | $($row.mode) | $($row.graphCount) | $($row.reportGate) | $($row.validatorVerdict) | $($row.runtimeFailureClass) | $($row.blockingIssueCount) | $exportOk |"
}
$lines | Set-Content -Encoding UTF8 -LiteralPath $mdPath

Write-Host "Wrote $summaryPath"
Write-Host "Wrote $mdPath"
