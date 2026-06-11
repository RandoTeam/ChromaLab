param(
    [string]$Package = "com.chromalab.app.validation",
    [string]$Activity = "com.chromalab.app.MainActivity",
    [string]$SuiteId = "phase9b_all",
    [string]$OutputRoot = "artifacts/phase9b-multi-fixture-android",
    [string]$SummaryPrefix = "phase9b",
    [switch]$SkipModelCheck,
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
    [int]$TimeoutSeconds = 360
)

$ErrorActionPreference = "Stop"

function Trim-OrEmpty {
    param([object]$Value)
    if ($null -eq $Value) { return "" }
    return "$Value".Trim()
}

function Get-AdbDeviceArgs {
    if ($global:AdbDevice) {
        return @("-s", $global:AdbDevice)
    }
    return @()
}

function Invoke-Adb {
    param([string[]]$AdbArgs)
    if ($global:AdbDevice) {
        & adb -s $global:AdbDevice @AdbArgs
    } else {
        & adb @AdbArgs
    }
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($AdbArgs -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Find-RunDirectory {
    param([string]$FixtureId)
    $deviceArgs = Get-AdbDeviceArgs
    $value = (& adb @deviceArgs shell "ls -dt /sdcard/Download/ChromaLab/validation/${FixtureId}_* 2>/dev/null | head -n 1") -join "`n"
    return (Trim-OrEmpty $value)
}

function Wait-RunManifest {
    param([string]$FixtureId, [int]$TimeoutSeconds, [string]$PreviousRunDirectory)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Seconds 5
        $dir = Find-RunDirectory -FixtureId $FixtureId
        if (-not [string]::IsNullOrWhiteSpace($dir) -and $dir -ne $PreviousRunDirectory) {
            $deviceArgs = Get-AdbDeviceArgs
            $manifest = (& adb @deviceArgs shell "ls $dir/artifact_manifest_*.json 2>/dev/null | head -n 1") -join "`n"
            if (-not [string]::IsNullOrWhiteSpace((Trim-OrEmpty $manifest))) {
                return $dir
            }
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for validation manifest for fixture $FixtureId"
}

function Read-JsonFileOrNull {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Read-FixtureMetadataOrNull {
    param([string]$FixtureId)
    $metadataPath = Join-Path "composeApp/src/androidMain/assets/validation" "$FixtureId.metadata.json"
    if ($FixtureId -eq "white_tiger_ion71") {
        $metadataPath = "composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.metadata.json"
    }
    return Read-JsonFileOrNull $metadataPath
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
    $evidence = if ($evidencePath) { Read-JsonFileOrNull $evidencePath.FullName } else { $null }
    $metadata = Read-FixtureMetadataOrNull -FixtureId $FixtureId
    $reportGraphCount = if ($report -and $report.graphs) { @($report.graphs).Count } else { 0 }
    $graphFailurePackageCount = if ($evidence -and $evidence.graphFailurePackages) { @($evidence.graphFailurePackages).Count } else { 0 }
    $metadataGraphCount = if ($report -and $report.metadata) { $report.metadata.detectedGraphCount } else { $null }
    $graphCount = if ($reportGraphCount -gt 0) {
        $reportGraphCount
    } elseif ($graphFailurePackageCount -gt 0) {
        $graphFailurePackageCount
    } else {
        $metadataGraphCount
    }
    $graphFailures = if ($evidence -and $evidence.graphFailurePackages -and @($evidence.graphFailurePackages).Count -gt 0) {
        @($evidence.graphFailurePackages)
    } else {
        @()
    }
    $firstFailure = if ($graphFailures.Count -gt 0) {
        $graphFailures[0]
    } else { $null }
    $firstGraph = if ($evidence -and $evidence.graphs -and @($evidence.graphs).Count -gt 0) {
        @($evidence.graphs)[0]
    } else { $null }
    $layoutClass = if ($firstFailure -and $firstFailure.layoutClass) {
        $firstFailure.layoutClass
    } elseif ($firstGraph -and $firstGraph.multiplicityResolution -and $firstGraph.multiplicityResolution.layoutClassification) {
        $firstGraph.multiplicityResolution.layoutClassification.layoutClass
    } else { $null }
    $subreasons = if ($firstFailure -and $firstFailure.tickSummary -and $firstFailure.tickSummary.subreasons) {
        @($firstFailure.tickSummary.subreasons)
    } else { @() }
    $scaleSubreasons = if ($firstFailure -and $firstFailure.scaleSummary -and $firstFailure.scaleSummary.subreasons) {
        @($firstFailure.scaleSummary.subreasons)
    } else { @() }
    $xScaleEvidenceTypes = if ($firstFailure -and $firstFailure.scaleSummary -and $firstFailure.scaleSummary.xEvidenceTypes) {
        @($firstFailure.scaleSummary.xEvidenceTypes)
    } else { @() }
    $yScaleEvidenceTypes = if ($firstFailure -and $firstFailure.scaleSummary -and $firstFailure.scaleSummary.yEvidenceTypes) {
        @($firstFailure.scaleSummary.yEvidenceTypes)
    } else { @() }
    $calibration = if ($firstFailure) { $firstFailure.calibrationSummary } else { $null }
    $allFailureClasses = @($graphFailures | ForEach-Object { $_.failureClass } | Where-Object { $_ } | Select-Object -Unique)
    $allTickSubreasons = @($graphFailures | ForEach-Object { $_.tickSummary.subreasons } | Where-Object { $_ } | Select-Object -Unique)
    $allScaleSubreasons = @($graphFailures | ForEach-Object { $_.scaleSummary.subreasons } | Where-Object { $_ } | Select-Object -Unique)
    $allSelectedXStrategies = @($graphFailures | ForEach-Object { $_.calibrationSummary.selectedXStrategy } | Where-Object { $_ } | Select-Object -Unique)
    $allSelectedYStrategies = @($graphFailures | ForEach-Object { $_.calibrationSummary.selectedYStrategy } | Where-Object { $_ } | Select-Object -Unique)
    [pscustomobject]@{
        fixtureId = $FixtureId
        mode = $Mode
        runId = $RunId
        expectedGraphCount = if ($metadata) { $metadata.expectedGraphCount } else { $null }
        graphCount = $graphCount
        reportGraphCount = $reportGraphCount
        graphFailurePackageCount = $graphFailurePackageCount
        metadataDetectedGraphCount = $metadataGraphCount
        layoutClass = $layoutClass
        xAnchorCount = if ($firstFailure) { $firstFailure.ocrSummary.acceptedXAnchorCount } else { $null }
        yAnchorCount = if ($firstFailure) { $firstFailure.ocrSummary.acceptedYAnchorCount } else { $null }
        xCalibrationStatus = if ($calibration) { $calibration.xStatus } else { $null }
        yCalibrationStatus = if ($calibration) { $calibration.yStatus } else { $null }
        selectedXCalibrationStrategy = if ($calibration) { $calibration.selectedXStrategy } else { $null }
        selectedYCalibrationStrategy = if ($calibration) { $calibration.selectedYStrategy } else { $null }
        calibrationStrategyCount = if ($calibration) { $calibration.strategyCount } else { $null }
        allFailureClasses = $allFailureClasses
        allSelectedXCalibrationStrategies = $allSelectedXStrategies
        allSelectedYCalibrationStrategies = $allSelectedYStrategies
        allTickSubreasons = $allTickSubreasons
        allScaleSubreasons = $allScaleSubreasons
        tickSubreasons = $subreasons
        scaleSubreasons = $scaleSubreasons
        xScaleEvidenceTypes = $xScaleEvidenceTypes
        yScaleEvidenceTypes = $yScaleEvidenceTypes
        axisScaleStatus = if ($firstFailure -and $firstFailure.scaleSummary) { $firstFailure.scaleSummary.status } else { $null }
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
$defaultDevice = (& adb devices | Select-String -Pattern "	device$" | Select-Object -First 1).ToString().Split("`t")[0]
if ([string]::IsNullOrWhiteSpace($defaultDevice)) {
    throw "Could not resolve active adb device id."
}
$global:AdbDevice = $defaultDevice

if (-not $SkipModelCheck) {
    $modelCheck = (& adb @((Get-AdbDeviceArgs) + @("shell", "run-as $Package ls -l files/models/gemma4-e2b 2>&1"))) -join "`n"
    if ($modelCheck -notmatch "gemma-4-E2B-it\.litertlm") {
        throw "E2B model precheck failed for $Package. Output: $modelCheck"
    }
    Write-Host "[INFO] E2B model precheck passed for $Package."
} else {
    Write-Host "[INFO] E2B model precheck skipped (SkipModelCheck=true)."
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
$summary = @()

foreach ($fixture in $Fixtures) {
    foreach ($mode in $Modes) {
        try {
            Invoke-Adb -AdbArgs @("shell", "logcat", "-c")
            $previousRunDirectory = Find-RunDirectory -FixtureId $fixture
            Write-Host "[INFO] Starting fixture=$fixture mode=$mode (previousDir=$previousRunDirectory)"
            Invoke-Adb -AdbArgs @(
                "shell",
                "am",
                "start",
                "-S",
                "-n",
                "$Package/$Activity",
                "-a",
                "com.chromalab.app.RUN_VALIDATION_FIXTURE",
                "--es",
                "fixture",
                $fixture,
                "--es",
                "modelMode",
                $mode
            ) | Out-Null
            Start-Sleep -Seconds 2
            $deviceRunDir = Wait-RunManifest -FixtureId $fixture -TimeoutSeconds $TimeoutSeconds -PreviousRunDirectory $previousRunDirectory
            $runId = Split-Path -Leaf $deviceRunDir
            $localModeDir = Join-Path (Join-Path $OutputRoot $fixture) $mode
            New-Item -ItemType Directory -Force -Path $localModeDir | Out-Null
            Invoke-Adb -AdbArgs @("pull", $deviceRunDir, $localModeDir) | Out-Null
            $localRunDir = Join-Path $localModeDir $runId
            if (-not (Test-Path -LiteralPath $localRunDir)) {
                throw "Pulled artifact directory was not created: $localRunDir"
            }
            try {
                & adb -s $global:AdbDevice exec-out screencap -p | Set-Content -Encoding Byte -LiteralPath (Join-Path $localRunDir "final_screen.png")
            } catch {
                $_.Exception.Message | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $localRunDir "final_screen_missing_reason.txt")
            }
            & adb -s $global:AdbDevice logcat -d -t 800 > (Join-Path $localRunDir "logcat_excerpt.txt")
            $summary += Summarize-Run -FixtureId $fixture -Mode $mode -RunId $runId -LocalRunDir $localRunDir
        } catch {
            $runId = "${fixture}_$mode`_failed"
            $localRunDir = Join-Path (Join-Path (Join-Path $OutputRoot $fixture) $mode) $runId
            New-Item -ItemType Directory -Force -Path $localRunDir | Out-Null
            $_.Exception | Out-String | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $localRunDir "runner_failure.txt")
            & adb -s $global:AdbDevice logcat -d -t 800 > (Join-Path $localRunDir "logcat_excerpt.txt")
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

$summaryPath = Join-Path $OutputRoot "${SummaryPrefix}_suite_summary.json"
$summary | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -LiteralPath $summaryPath

$mdPath = Join-Path $OutputRoot "${SummaryPrefix}_suite_summary.md"
$lines = @(
    "# $($SummaryPrefix.ToUpperInvariant()) Android Validation Suite",
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

$phase9eMdPath = Join-Path $OutputRoot "${SummaryPrefix}_suite_summary_phase9e.md"
$phase9eLines = @(
    "# $($SummaryPrefix.ToUpperInvariant()) Android Validation Suite - Phase 9E Fields",
    "",
    "| Fixture | Mode | Layout | Expected graphs | Detected graphs | Report graphs | Failure packages | X anchors | Y anchors | X cal | Y cal | Gate | Validator | Failure | Subreason | Export |",
    "| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- | --- |"
)
foreach ($row in $summary) {
    $exportOk = $row.runtimeEvidencePackageAvailable -and $row.validatorJsonAvailable -and $row.validatorMarkdownAvailable -and $row.finalReportJsonAvailable -and $row.exportManifestAvailable
    $phase9eLines += "| $($row.fixtureId) | $($row.mode) | $($row.layoutClass) | $($row.expectedGraphCount) | $($row.metadataDetectedGraphCount) | $($row.reportGraphCount) | $($row.graphFailurePackageCount) | $($row.xAnchorCount) | $($row.yAnchorCount) | $($row.xCalibrationStatus) | $($row.yCalibrationStatus) | $($row.reportGate) | $($row.validatorVerdict) | $($row.runtimeFailureClass) | $(@($row.tickSubreasons) -join '<br>') | $exportOk |"
}
$phase9eLines | Set-Content -Encoding UTF8 -LiteralPath $phase9eMdPath

Write-Host "Wrote $summaryPath"
Write-Host "Wrote $mdPath"
Write-Host "Wrote $phase9eMdPath"

$phase9fMdPath = Join-Path $OutputRoot "${SummaryPrefix}_suite_summary_phase9f.md"
$phase9fLines = @(
    "# $($SummaryPrefix.ToUpperInvariant()) Android Validation Suite - Phase 9F Axis Scale Fields",
    "",
    "| Fixture | Mode | Layout | Expected graphs | Detected graphs | Scale status | X strategy | Y strategy | Strategies | X scale evidence | Y scale evidence | X anchors | Y anchors | Gate | Validator | Failure | Tick subreason | Scale subreason | Export |",
    "| --- | --- | --- | ---: | ---: | --- | --- | --- | ---: | --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- |"
)
foreach ($row in $summary) {
    $exportOk = $row.runtimeEvidencePackageAvailable -and $row.validatorJsonAvailable -and $row.validatorMarkdownAvailable -and $row.finalReportJsonAvailable -and $row.exportManifestAvailable
    $phase9fLines += "| $($row.fixtureId) | $($row.mode) | $($row.layoutClass) | $($row.expectedGraphCount) | $($row.metadataDetectedGraphCount) | $($row.axisScaleStatus) | $($row.selectedXCalibrationStrategy) | $($row.selectedYCalibrationStrategy) | $($row.calibrationStrategyCount) | $(@($row.xScaleEvidenceTypes) -join '+') | $(@($row.yScaleEvidenceTypes) -join '+') | $($row.xAnchorCount) | $($row.yAnchorCount) | $($row.reportGate) | $($row.validatorVerdict) | $($row.runtimeFailureClass) | $(@($row.tickSubreasons) -join '<br>') | $(@($row.scaleSubreasons) -join '<br>') | $exportOk |"
}
$phase9fLines | Set-Content -Encoding UTF8 -LiteralPath $phase9fMdPath
Write-Host "Wrote $phase9fMdPath"

$phase9gMdPath = Join-Path $OutputRoot "${SummaryPrefix}_suite_summary_phase9g.md"
$phase9gLines = @(
    "# $($SummaryPrefix.ToUpperInvariant()) Android Validation Suite - Phase 9G Calibration Ensemble",
    "",
    "| Fixture | Mode | Report graphs | Failure packages | Failure classes | X strategies | Y strategies | Tick subreasons | Scale subreasons | Gate | Validator | Export |",
    "| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- |"
)
foreach ($row in $summary) {
    $exportOk = $row.runtimeEvidencePackageAvailable -and $row.validatorJsonAvailable -and $row.validatorMarkdownAvailable -and $row.finalReportJsonAvailable -and $row.exportManifestAvailable
    $phase9gLines += "| $($row.fixtureId) | $($row.mode) | $($row.reportGraphCount) | $($row.graphFailurePackageCount) | $(@($row.allFailureClasses) -join '<br>') | $(@($row.allSelectedXCalibrationStrategies) -join '<br>') | $(@($row.allSelectedYCalibrationStrategies) -join '<br>') | $(@($row.allTickSubreasons) -join '<br>') | $(@($row.allScaleSubreasons) -join '<br>') | $($row.reportGate) | $($row.validatorVerdict) | $exportOk |"
}
$phase9gLines | Set-Content -Encoding UTF8 -LiteralPath $phase9gMdPath
Write-Host "Wrote $phase9gMdPath"
