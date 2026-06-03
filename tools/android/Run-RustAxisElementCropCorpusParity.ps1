param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.chromalab.app.validation",
    [string]$ActivityName = "com.chromalab.app.MainActivity",
    [string]$Action = "com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CORPUS",
    [string]$ApkPath = "androidApp\build\outputs\apk\validation\androidApp-validation.apk",
    [string]$InputRoot = "build\dr1r-axis-label-crop-sweep",
    [string]$PcCorpusRoot = "build\dr2g-pc-rust-corpus",
    [string]$ArtifactsRoot = "artifacts\dr2g-rust-axis-element-corpus-parity",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedApkPath = Join-Path $repoRoot $ApkPath
$resolvedPcCorpusRoot = Join-Path $repoRoot $PcCorpusRoot
$resolvedArtifactsRoot = Join-Path $repoRoot $ArtifactsRoot
New-Item -ItemType Directory -Force -Path $resolvedArtifactsRoot | Out-Null

function Invoke-Adb {
    param([string[]]$CommandArgs)
    $adbArgs = @()
    if ($DeviceId.Trim()) {
        $adbArgs += @("-s", $DeviceId.Trim())
    }
    $adbArgs += $CommandArgs
    & adb @adbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($adbArgs -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Invoke-AdbText {
    param([string[]]$CommandArgs)
    $adbArgs = @()
    if ($DeviceId.Trim()) {
        $adbArgs += @("-s", $DeviceId.Trim())
    }
    $adbArgs += $CommandArgs
    $output = & adb @adbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($adbArgs -join ' ') failed with exit code $LASTEXITCODE"
    }
    return ($output -join "`n")
}

function Get-CropSignature {
    param([object[]]$Crops)
    if (-not $Crops -or $Crops.Count -eq 0) {
        return ""
    }
    return (($Crops | ForEach-Object {
        $rect = $_.clamped_rect
        $variants = @($_.variants) -join "+"
        "$($_.band_kind):$($rect.x),$($rect.y),$($rect.width),$($rect.height):$variants"
    }) -join "|")
}

function Find-LatestCorpusJsonPath {
    $pathText = Invoke-AdbText -CommandArgs @(
        "shell",
        "find /sdcard/Download/ChromaLab/runtime/rust-axis-element-corpus -name 'rust_axis_element_corpus_*.json' -type f 2>/dev/null | sort | tail -n 1"
    )
    return $pathText.Trim()
}

$devices = (& adb devices) -join "`n"
if (-not $devices.Contains("device")) {
    throw "No Android device is attached."
}

Push-Location $repoRoot
try {
    .\tools\rust\Run-RustBridgeCorpus.ps1 -InputRoot $InputRoot -OutputRoot $PcCorpusRoot | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "PC Rust corpus run failed with exit code $LASTEXITCODE"
    }

    if (-not $SkipBuild) {
        .\gradlew.bat :androidApp:assembleValidation --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Validation APK build failed with exit code $LASTEXITCODE"
        }
    }
}
finally {
    Pop-Location
}

if (-not (Test-Path $resolvedApkPath)) {
    throw "Validation APK was not found: $resolvedApkPath"
}

Invoke-Adb -CommandArgs @("install", "-r", $resolvedApkPath)
Invoke-Adb -CommandArgs @("logcat", "-c")
Invoke-Adb -CommandArgs @(
    "shell",
    "am",
    "start",
    "-S",
    "-n",
    "$PackageName/$ActivityName",
    "-a",
    $Action
)

$runId = $null
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline -and -not $runId) {
    Start-Sleep -Seconds 2
    $logcat = Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustAxisCorpus:I", "ChromaLabMain:I", "*:S")
    $match = [regex]::Match($logcat, "runId=(rust_axis_element_corpus_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $latestJsonPath = Find-LatestCorpusJsonPath
    $match = [regex]::Match($latestJsonPath, "(rust_axis_element_corpus_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $logPath = Join-Path $resolvedArtifactsRoot "rust_axis_element_corpus_logcat_no_runid.txt"
    Invoke-AdbText -CommandArgs @("logcat", "-d") | Set-Content -Path $logPath -Encoding UTF8
    throw "Rust axis element corpus run id was not found. Log saved to $logPath"
}

$deviceDir = "/sdcard/Download/ChromaLab/runtime/rust-axis-element-corpus/$runId"
$deviceSummaryJson = "$deviceDir/rust_axis_element_corpus_$runId.json"
$deviceSummaryMarkdown = "$deviceDir/rust_axis_element_corpus_$runId.md"

$jsonText = $null
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline -and -not $jsonText) {
    Start-Sleep -Seconds 2
    $candidate = Invoke-AdbText -CommandArgs @("shell", "cat", $deviceSummaryJson)
    if ($candidate.Trim().StartsWith("{")) {
        $jsonText = $candidate
    }
}

if (-not $jsonText) {
    throw "Rust axis element corpus JSON was not readable on device: $deviceSummaryJson"
}

$runArtifactDir = Join-Path $resolvedArtifactsRoot $runId
New-Item -ItemType Directory -Force -Path $runArtifactDir | Out-Null

$localSummaryJson = Join-Path $runArtifactDir "rust_axis_element_corpus_$runId.json"
$localSummaryMarkdown = Join-Path $runArtifactDir "rust_axis_element_corpus_$runId.md"
$localLogcat = Join-Path $runArtifactDir "rust_axis_element_corpus_$runId.logcat.txt"
$localParityCsv = Join-Path $runArtifactDir "rust_axis_element_corpus_parity_$runId.csv"

$jsonText | Set-Content -Path $localSummaryJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceSummaryMarkdown) |
    Set-Content -Path $localSummaryMarkdown -Encoding UTF8
Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustAxisCorpus:I", "ChromaLabMain:I", "*:S") |
    Set-Content -Path $localLogcat -Encoding UTF8

$summary = $jsonText | ConvertFrom-Json
if ([string]$summary.decision -ne "PASS") {
    throw "Android Rust axis element corpus failed: decision=$($summary.decision) pass=$($summary.passCount) fail=$($summary.failCount)"
}

$pcRows = Import-Csv (Join-Path $resolvedPcCorpusRoot "rust_bridge_corpus_summary.csv")
$androidRows = @($summary.results)
$parityRows = foreach ($pcRow in $pcRows) {
    $androidRow = $androidRows | Where-Object {
        $_.fixtureId -eq $pcRow.fixture -and $_.graphId -eq $pcRow.graph
    } | Select-Object -First 1
    if (-not $androidRow) {
        [pscustomobject]@{
            fixture = $pcRow.fixture
            graph = $pcRow.graph
            parity = "FAIL_MISSING_ANDROID_ROW"
            pcAccepted = $pcRow.accepted
            androidAccepted = ""
            pcSignature = ""
            androidSignature = ""
        }
        continue
    }

    $pcReport = Get-Content -LiteralPath $pcRow.report -Raw | ConvertFrom-Json
    $pcSignature = Get-CropSignature -Crops @($pcReport.crop_plan.accepted)
    $match = [int]$pcRow.imageWidth -eq [int]$androidRow.imageWidth -and
        [int]$pcRow.imageHeight -eq [int]$androidRow.imageHeight -and
        [int]$pcRow.sourceBands -eq [int]$androidRow.sourceLabelBandCount -and
        [int]$pcRow.accepted -eq [int]$androidRow.acceptedCropCount -and
        [int]$pcRow.rejected -eq [int]$androidRow.rejectedCropCount -and
        $pcSignature -eq [string]$androidRow.acceptedCropSignature

    [pscustomobject]@{
        fixture = $pcRow.fixture
        graph = $pcRow.graph
        parity = if ($match) { "PASS" } else { "FAIL" }
        imageWidth = $pcRow.imageWidth
        imageHeight = $pcRow.imageHeight
        pcBands = $pcRow.sourceBands
        androidBands = $androidRow.sourceLabelBandCount
        pcAccepted = $pcRow.accepted
        androidAccepted = $androidRow.acceptedCropCount
        pcRejected = $pcRow.rejected
        androidRejected = $androidRow.rejectedCropCount
        pcSignature = $pcSignature
        androidSignature = $androidRow.acceptedCropSignature
    }
}

$parityRows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $localParityCsv
$failedParity = @($parityRows | Where-Object { $_.parity -ne "PASS" })
if ($failedParity.Count -gt 0) {
    throw "PC/Android Rust corpus parity failed for $($failedParity.Count) rows. See $localParityCsv"
}

[pscustomobject]@{
    runId = $runId
    packageName = $PackageName
    deviceDirectory = $deviceDir
    localDirectory = $runArtifactDir
    pcCorpusDirectory = $resolvedPcCorpusRoot
    decision = [string]$summary.decision
    itemCount = [int]$summary.itemCount
    passCount = [int]$summary.passCount
    failCount = [int]$summary.failCount
    parity = "PASS"
    parityCsv = $localParityCsv
}
