param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.chromalab.app.validation",
    [string]$ActivityName = "com.chromalab.app.MainActivity",
    [string]$Action = "com.chromalab.app.DEBUG_TURBOVEC_KNOWLEDGE_INDEX_GATE",
    [string]$ApkPath = "androidApp\build\outputs\apk\validation\androidApp-validation.apk",
    [string]$ArtifactsRoot = "artifacts\tv8-turbovec-knowledge-index-gate",
    [string]$KnowledgeArtifactsRoot = "artifacts\tv2-turbovec-knowledge",
    [string]$Profile = "minilm",
    [string]$PythonPath = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resolvedApkPath = Join-Path $repoRoot $ApkPath
$resolvedArtifactsRoot = Join-Path $repoRoot $ArtifactsRoot
$resolvedKnowledgeArtifactsRoot = Join-Path $repoRoot $KnowledgeArtifactsRoot
$resolvedPythonPath = $PythonPath
if (-not $resolvedPythonPath.Trim()) {
    $venvPython = Join-Path $resolvedKnowledgeArtifactsRoot ".venv\Scripts\python.exe"
    $resolvedPythonPath = if (Test-Path $venvPython) { $venvPython } else { "python" }
}
$indexName = "chromalab_knowledge_v2_$Profile.tvim"
$sidecarName = "chromalab_knowledge_v2_${Profile}_sidecar.json"
$queryName = "chromalab_knowledge_v2_${Profile}_queries.json"
$indexPath = Join-Path $resolvedKnowledgeArtifactsRoot "$Profile\$indexName"
$sidecarPath = Join-Path $resolvedKnowledgeArtifactsRoot "$Profile\$sidecarName"
$queryPath = Join-Path $resolvedArtifactsRoot "$queryName"
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

function Copy-ToAppPrivate {
    param(
        [string]$LocalPath,
        [string]$RemoteFileName
    )
    $tmpPath = "/data/local/tmp/chromalab_tv8_$RemoteFileName"
    Invoke-Adb -CommandArgs @("push", $LocalPath, $tmpPath)
    Invoke-Adb -CommandArgs @("shell", "chmod", "644", $tmpPath)
    Invoke-Adb -CommandArgs @(
        "shell",
        "run-as",
        $PackageName,
        "mkdir",
        "-p",
        "files/chromalab_tv8_knowledge/$Profile"
    )
    Invoke-Adb -CommandArgs @(
        "shell",
        "run-as",
        $PackageName,
        "cp",
        $tmpPath,
        "files/chromalab_tv8_knowledge/$Profile/$RemoteFileName"
    )
    Invoke-Adb -CommandArgs @("shell", "rm", "-f", $tmpPath)
}

function Find-LatestGateJsonPath {
    $pathText = Invoke-AdbText -CommandArgs @(
        "shell",
        "find /sdcard/Download/ChromaLab/runtime/turbovec-knowledge-index-gate -name 'turbovec_knowledge_index_gate_*.json' -type f 2>/dev/null | sort | tail -n 1"
    )
    return $pathText.Trim()
}

$devices = (& adb devices) -join "`n"
if (-not $devices.Contains("device")) {
    throw "No Android device is attached."
}

if (-not (Test-Path $indexPath) -or -not (Test-Path $sidecarPath)) {
    Push-Location $repoRoot
    try {
        & $resolvedPythonPath tools/knowledge-retrieval/build_turbovec_indexes.py `
            --pack docs/knowledge/chromalab_knowledge_seed_v2.json `
            --out $resolvedKnowledgeArtifactsRoot `
            --models $Profile
        if ($LASTEXITCODE -ne 0) {
            throw "TV-2 index build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Push-Location $repoRoot
try {
    & $resolvedPythonPath tools/knowledge-retrieval/build_tv8_android_query_vectors.py `
        --pack docs/knowledge/chromalab_knowledge_seed_v2.json `
        --sidecar $sidecarPath `
        --out $queryPath `
        --profile $Profile
    if ($LASTEXITCODE -ne 0) {
        throw "TV-8 query vector build failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
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
Invoke-Adb -CommandArgs @("shell", "run-as", $PackageName, "rm", "-rf", "files/chromalab_tv8_knowledge")
Copy-ToAppPrivate -LocalPath $indexPath -RemoteFileName $indexName
Copy-ToAppPrivate -LocalPath $sidecarPath -RemoteFileName $sidecarName
Copy-ToAppPrivate -LocalPath $queryPath -RemoteFileName $queryName
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
    $logcat = Invoke-AdbText -CommandArgs @("logcat", "-d", "ChromaLabTurboVec:I", "ChromaLabMain:I", "*:S")
    $match = [regex]::Match($logcat, "runId=(turbovec_knowledge_gate_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $latestJsonPath = Find-LatestGateJsonPath
    $match = [regex]::Match($latestJsonPath, "(turbovec_knowledge_gate_[0-9]+)")
    if ($match.Success) {
        $runId = $match.Groups[1].Value
    }
}

if (-not $runId) {
    $logPath = Join-Path $resolvedArtifactsRoot "turbovec_knowledge_index_gate_logcat_no_runid.txt"
    Invoke-AdbText -CommandArgs @("logcat", "-d") | Set-Content -Path $logPath -Encoding UTF8
    throw "TurboVec Knowledge index gate run id was not found. Log saved to $logPath"
}

$deviceDir = "/sdcard/Download/ChromaLab/runtime/turbovec-knowledge-index-gate/$runId"
$deviceSummaryJson = "$deviceDir/turbovec_knowledge_index_gate_$runId.json"
$deviceSummaryMarkdown = "$deviceDir/turbovec_knowledge_index_gate_$runId.md"
$deviceRustResponseJson = "$deviceDir/turbovec_knowledge_index_gate_response_$runId.json"

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
    throw "TurboVec Knowledge index gate JSON was not readable on device: $deviceSummaryJson"
}

$runArtifactDir = Join-Path $resolvedArtifactsRoot $runId
New-Item -ItemType Directory -Force -Path $runArtifactDir | Out-Null

$localSummaryJson = Join-Path $runArtifactDir "turbovec_knowledge_index_gate_$runId.json"
$localSummaryMarkdown = Join-Path $runArtifactDir "turbovec_knowledge_index_gate_$runId.md"
$localRustResponseJson = Join-Path $runArtifactDir "turbovec_knowledge_index_gate_response_$runId.json"
$localLogcat = Join-Path $runArtifactDir "turbovec_knowledge_index_gate_$runId.logcat.txt"

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
$realIndexGatePassed = [bool]$summary.realIndexGatePassed
$localAndroidEmbeddingAvailable = [bool]$summary.localAndroidEmbeddingAvailable
$queryTimedOut = [bool]$summary.queryTimedOut
$filesExistAfterCleanup = [bool]$summary.filesExistAfterCleanup
$runtimePromotion = [bool]$summary.runtimePromotion
$activeOwnerUnchanged = [bool]$summary.activeRetrievalOwnerUnchanged

if (
    $status -ne "PASS" -or
    $contract -ne "TV8_TURBOVEC_REAL_KNOWLEDGE_INDEX_GATE_V1" -or
    $pathClass -ne "APP_PRIVATE" -or
    -not $realIndexGatePassed -or
    $localAndroidEmbeddingAvailable -or
    $queryTimedOut -or
    $filesExistAfterCleanup -or
    $runtimePromotion -or
    -not $activeOwnerUnchanged
) {
    throw "TurboVec Knowledge index gate failed unexpectedly: decision=$decision status=$status contract=$contract pathClass=$pathClass realIndexGatePassed=$realIndexGatePassed localAndroidEmbeddingAvailable=$localAndroidEmbeddingAvailable queryTimedOut=$queryTimedOut filesExistAfterCleanup=$filesExistAfterCleanup runtimePromotion=$runtimePromotion activeOwnerUnchanged=$activeOwnerUnchanged"
}

[pscustomobject]@{
    runId = $runId
    packageName = $PackageName
    deviceDirectory = $deviceDir
    localDirectory = $runArtifactDir
    decision = $decision
    status = $status
    contract = $contract
    profile = [string]$summary.profileKey
    model = [string]$summary.modelId
    entryCount = [int]$summary.entryCount
    indexBytes = [long]$summary.indexBytes
    loadMs = [long]$summary.loadMs
    queryMs = [long]$summary.queryMs
    realIndexGatePassed = $realIndexGatePassed
    localAndroidEmbeddingAvailable = $localAndroidEmbeddingAvailable
    queryEmbeddingRuntime = [string]$summary.queryEmbeddingRuntime
}
