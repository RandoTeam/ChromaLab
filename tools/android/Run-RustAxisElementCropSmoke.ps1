param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.chromalab.app.validation",
    [string]$ActivityName = "com.chromalab.app.MainActivity",
    [string]$Action = "com.chromalab.app.DEBUG_RUST_AXIS_ELEMENT_CROPS",
    [string]$ApkPath = "androidApp\build\outputs\apk\validation\androidApp-validation.apk",
    [string]$ArtifactsRoot = "artifacts\dr2f-rust-axis-element-crops",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedApkPath = Join-Path $repoRoot $ApkPath
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

function Find-LatestSmokeJsonPath {
    $pathText = Invoke-AdbText -CommandArgs @(
        "shell",
        "find /sdcard/Download/ChromaLab/runtime/rust-axis-element-crops -name 'rust_axis_element_crop_smoke_*.json' -type f 2>/dev/null | sort | tail -n 1"
    )
    return $pathText.Trim()
}

$devices = (& adb devices) -join "`n"
if (-not $devices.Contains("device")) {
    throw "No Android device is attached."
}

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        .\gradlew.bat :androidApp:assembleValidation --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Validation APK build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
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
$deadline = (Get-Date).AddSeconds(45)
while ((Get-Date) -lt $deadline -and -not $runId) {
    Start-Sleep -Seconds 2
    $logcat = Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustAxis:I", "ChromaLabMain:I", "*:S")
    $match = [regex]::Match($logcat, "runId=(rust_axis_element_crops_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $latestJsonPath = Find-LatestSmokeJsonPath
    $match = [regex]::Match($latestJsonPath, "(rust_axis_element_crops_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $logPath = Join-Path $resolvedArtifactsRoot "rust_axis_element_crop_smoke_logcat_no_runid.txt"
    Invoke-AdbText -CommandArgs @("logcat", "-d") | Set-Content -Path $logPath -Encoding UTF8
    throw "Rust axis element crop smoke run id was not found. Log saved to $logPath"
}

$deviceDir = "/sdcard/Download/ChromaLab/runtime/rust-axis-element-crops/$runId"
$deviceSummaryJson = "$deviceDir/rust_axis_element_crop_smoke_$runId.json"
$deviceSummaryMarkdown = "$deviceDir/rust_axis_element_crop_smoke_$runId.md"
$deviceRustResponseJson = "$deviceDir/rust_axis_element_crop_response_$runId.json"

$jsonText = $null
$deadline = (Get-Date).AddSeconds(45)
while ((Get-Date) -lt $deadline -and -not $jsonText) {
    Start-Sleep -Seconds 2
    $candidate = Invoke-AdbText -CommandArgs @("shell", "cat", $deviceSummaryJson)
    if ($candidate.Trim().StartsWith("{")) {
        $jsonText = $candidate
    }
}

if (-not $jsonText) {
    throw "Rust axis element crop smoke JSON was not readable on device: $deviceSummaryJson"
}

$runArtifactDir = Join-Path $resolvedArtifactsRoot $runId
New-Item -ItemType Directory -Force -Path $runArtifactDir | Out-Null

$localSummaryJson = Join-Path $runArtifactDir "rust_axis_element_crop_smoke_$runId.json"
$localSummaryMarkdown = Join-Path $runArtifactDir "rust_axis_element_crop_smoke_$runId.md"
$localRustResponseJson = Join-Path $runArtifactDir "rust_axis_element_crop_response_$runId.json"
$localLogcat = Join-Path $runArtifactDir "rust_axis_element_crop_smoke_$runId.logcat.txt"

$jsonText | Set-Content -Path $localSummaryJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceSummaryMarkdown) |
    Set-Content -Path $localSummaryMarkdown -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceRustResponseJson) |
    Set-Content -Path $localRustResponseJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustAxis:I", "ChromaLabMain:I", "*:S") |
    Set-Content -Path $localLogcat -Encoding UTF8

$summary = $jsonText | ConvertFrom-Json
$decision = [string]$summary.decision
$rustStatus = [string]$summary.rustStatus
$contract = [string]$summary.ffiContract
$acceptedCropCount = [int]$summary.acceptedCropCount
$sourceLabelBandCount = [int]$summary.sourceLabelBandCount
$graphIndex = [int]$summary.graphIndex

if (
    $decision -ne "PASS" -or
    $rustStatus -ne "OK" -or
    $contract -ne "DR2F_AXIS_ELEMENT_CROP_PLAN_V1" -or
    $graphIndex -ne 1 -or
    $sourceLabelBandCount -ne 3 -or
    $acceptedCropCount -lt 1
) {
    throw "Rust axis element crop smoke failed: decision=$decision rustStatus=$rustStatus contract=$contract graphIndex=$graphIndex sourceBands=$sourceLabelBandCount accepted=$acceptedCropCount"
}

[pscustomobject]@{
    runId = $runId
    packageName = $PackageName
    deviceDirectory = $deviceDir
    localDirectory = $runArtifactDir
    decision = $decision
    rustStatus = $rustStatus
    contract = $contract
    graphIndex = $graphIndex
    sourceLabelBandCount = $sourceLabelBandCount
    acceptedCropCount = $acceptedCropCount
}
