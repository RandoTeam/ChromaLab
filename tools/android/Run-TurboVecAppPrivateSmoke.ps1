param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.chromalab.app.validation",
    [string]$ActivityName = "com.chromalab.app.MainActivity",
    [string]$Action = "com.chromalab.app.DEBUG_TURBOVEC_APP_PRIVATE",
    [string]$ApkPath = "androidApp\build\outputs\apk\validation\androidApp-validation.apk",
    [string]$ArtifactsRoot = "artifacts\tv7-turbovec-app-private",
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
        "find /sdcard/Download/ChromaLab/runtime/turbovec-app-private -name 'turbovec_app_private_smoke_*.json' -type f 2>/dev/null | sort | tail -n 1"
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
    $logcat = Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabTurboVec:I", "ChromaLabMain:I", "*:S")
    $match = [regex]::Match($logcat, "runId=(turbovec_app_private_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $latestJsonPath = Find-LatestSmokeJsonPath
    $match = [regex]::Match($latestJsonPath, "(turbovec_app_private_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $logPath = Join-Path $resolvedArtifactsRoot "turbovec_app_private_smoke_logcat_no_runid.txt"
    Invoke-AdbText -CommandArgs @("logcat", "-d") | Set-Content -Path $logPath -Encoding UTF8
    throw "TurboVec app-private smoke run id was not found. Log saved to $logPath"
}

$deviceDir = "/sdcard/Download/ChromaLab/runtime/turbovec-app-private/$runId"
$deviceSummaryJson = "$deviceDir/turbovec_app_private_smoke_$runId.json"
$deviceSummaryMarkdown = "$deviceDir/turbovec_app_private_smoke_$runId.md"
$deviceRustResponseJson = "$deviceDir/turbovec_app_private_response_$runId.json"

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
    throw "TurboVec app-private smoke JSON was not readable on device: $deviceSummaryJson"
}

$runArtifactDir = Join-Path $resolvedArtifactsRoot $runId
New-Item -ItemType Directory -Force -Path $runArtifactDir | Out-Null

$localSummaryJson = Join-Path $runArtifactDir "turbovec_app_private_smoke_$runId.json"
$localSummaryMarkdown = Join-Path $runArtifactDir "turbovec_app_private_smoke_$runId.md"
$localRustResponseJson = Join-Path $runArtifactDir "turbovec_app_private_response_$runId.json"
$localLogcat = Join-Path $runArtifactDir "turbovec_app_private_smoke_$runId.logcat.txt"

$jsonText | Set-Content -Path $localSummaryJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceSummaryMarkdown) |
    Set-Content -Path $localSummaryMarkdown -Encoding UTF8
Invoke-AdbText -CommandArgs @("shell", "cat", $deviceRustResponseJson) |
    Set-Content -Path $localRustResponseJson -Encoding UTF8
Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabTurboVec:I", "ChromaLabMain:I", "*:S") |
    Set-Content -Path $localLogcat -Encoding UTF8

$summary = $jsonText | ConvertFrom-Json
$decision = [string]$summary.decision
$status = [string]$summary.status
$contract = [string]$summary.ffiContract
$pathClass = [string]$summary.pathClass
$top1Ok = [bool]$summary.top1Ok
$allIdsValid = [bool]$summary.allIdsValid
$allEntryIdsValid = [bool]$summary.allEntryIdsValid
$queryTimedOut = [bool]$summary.queryTimedOut
$indexExistsAfterCleanup = [bool]$summary.indexExistsAfterCleanup
$runtimePromotion = [bool]$summary.runtimePromotion
$activeOwnerUnchanged = [bool]$summary.activeRetrievalOwnerUnchanged

if (
    $decision -ne "PASS" -or
    $status -ne "PASS" -or
    $contract -ne "TV7_TURBOVEC_APP_PRIVATE_PROVIDER_V1" -or
    $pathClass -ne "APP_PRIVATE" -or
    -not $top1Ok -or
    -not $allIdsValid -or
    -not $allEntryIdsValid -or
    $queryTimedOut -or
    $indexExistsAfterCleanup -or
    $runtimePromotion -or
    -not $activeOwnerUnchanged
) {
    throw "TurboVec app-private smoke failed: decision=$decision status=$status contract=$contract pathClass=$pathClass top1Ok=$top1Ok allIdsValid=$allIdsValid allEntryIdsValid=$allEntryIdsValid queryTimedOut=$queryTimedOut indexExistsAfterCleanup=$indexExistsAfterCleanup runtimePromotion=$runtimePromotion activeOwnerUnchanged=$activeOwnerUnchanged"
}

[pscustomobject]@{
    runId = $runId
    packageName = $PackageName
    deviceDirectory = $deviceDir
    localDirectory = $runArtifactDir
    decision = $decision
    status = $status
    contract = $contract
    pathClass = $pathClass
    indexBytes = [long]$summary.indexBytes
    loadMs = [long]$summary.loadMs
    queryMs = [long]$summary.queryMs
    queryTimedOut = $queryTimedOut
    topIds = ($summary.topIds -join ",")
    topEntryIds = ($summary.topEntryIds -join ",")
}
