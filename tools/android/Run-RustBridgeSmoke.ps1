param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.chromalab.app.validation",
    [string]$ActivityName = "com.chromalab.app.MainActivity",
    [string]$Action = "com.chromalab.app.DEBUG_RUST_CV_BRIDGE",
    [string]$ApkPath = "androidApp\build\outputs\apk\validation\androidApp-validation.apk",
    [string]$ArtifactsRoot = "artifacts\dr2e-rust-bridge-smoke",
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
        "find /sdcard/Download/ChromaLab/runtime/rust-bridge-smoke -name 'rust_bridge_smoke_*.json' -type f 2>/dev/null | sort | tail -n 1"
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
    $logcat = Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustSmoke:I", "ChromaLabMain:I", "*:S")
    $match = [regex]::Match($logcat, "runId=(rust_bridge_smoke_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $latestJsonPath = Find-LatestSmokeJsonPath
    $match = [regex]::Match($latestJsonPath, "(rust_bridge_smoke_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $logPath = Join-Path $resolvedArtifactsRoot "rust_bridge_smoke_logcat_no_runid.txt"
    Invoke-AdbText -CommandArgs @("logcat", "-d") | Set-Content -Path $logPath -Encoding UTF8
    throw "Rust bridge smoke run id was not found in logcat or device artifacts. Log saved to $logPath"
}

$deviceDir = "/sdcard/Download/ChromaLab/runtime/rust-bridge-smoke/$runId"
$deviceJson = "$deviceDir/rust_bridge_smoke_$runId.json"
$deviceMarkdown = "$deviceDir/rust_bridge_smoke_$runId.md"

$jsonText = $null
$deadline = (Get-Date).AddSeconds(45)
while ((Get-Date) -lt $deadline -and -not $jsonText) {
    Start-Sleep -Seconds 2
    $candidate = Invoke-AdbText -CommandArgs @("shell", "cat", $deviceJson)
    if ($candidate.Trim().StartsWith("{")) {
        $jsonText = $candidate
    }
}

if (-not $jsonText) {
    throw "Rust bridge smoke JSON was not readable on device: $deviceJson"
}

$runArtifactDir = Join-Path $resolvedArtifactsRoot $runId
New-Item -ItemType Directory -Force -Path $runArtifactDir | Out-Null

$localJson = Join-Path $runArtifactDir "rust_bridge_smoke_$runId.json"
$localMarkdown = Join-Path $runArtifactDir "rust_bridge_smoke_$runId.md"
$localLogcat = Join-Path $runArtifactDir "rust_bridge_smoke_$runId.logcat.txt"

$jsonText | Set-Content -Path $localJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceMarkdown) | Set-Content -Path $localMarkdown -Encoding UTF8
Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabRustSmoke:I", "ChromaLabMain:I", "*:S") |
    Set-Content -Path $localLogcat -Encoding UTF8

$summary = $jsonText | ConvertFrom-Json
$loadResult = [string]$summary.diagnostic.loadResult
$decision = [string]$summary.decision
$source = [string]$summary.diagnostic.source

if ($decision -ne "PASS" -or $loadResult -ne "AVAILABLE" -or $source -ne "RUST_CV_BRIDGE") {
    throw "Rust bridge smoke failed: decision=$decision source=$source loadResult=$loadResult"
}

[pscustomobject]@{
    runId = $runId
    packageName = $PackageName
    deviceDirectory = $deviceDir
    localDirectory = $runArtifactDir
    decision = $decision
    diagnosticSource = $source
    loadResult = $loadResult
}
