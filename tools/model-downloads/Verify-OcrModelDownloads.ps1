param(
    [string]$ManifestPath = "tools/model-downloads/ocr_models_may2026.json",
    [string]$ModelId = "",
    [string]$OutputRoot = "artifacts/model-downloads",
    [switch]$Download,
    [switch]$AllowLargeDownloads
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

function Get-HfResolveUrl([string]$repo, [string]$path) {
    $escaped = ($path -split "/") | ForEach-Object { [System.Uri]::EscapeDataString($_) }
    return "https://huggingface.co/$repo/resolve/main/$($escaped -join '/')"
}

function Get-DownloadTargetPath([string]$root, [string]$packageId, [string]$repo, [string]$path) {
    $repoPath = $repo -replace "/", [System.IO.Path]::DirectorySeparatorChar
    return Join-Path $root (Join-Path $packageId (Join-Path $repoPath $path))
}

function Get-RemoteSize([string]$url) {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $true
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(60)
    try {
        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Head, $url)
        $request.Headers.UserAgent.ParseAdd("ChromaLab-download-check/1.0")
        $request.Headers.AcceptEncoding.ParseAdd("identity")
        $response = $client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "HEAD failed with HTTP $([int]$response.StatusCode)"
        }
        $length = $response.Content.Headers.ContentLength
        if ($null -ne $length -and $length -gt 0) {
            return [int64]$length
        }
    } finally {
        $client.Dispose()
        $handler.Dispose()
    }

    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $url)
    $request.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::new(0, 0)
    $request.Headers.UserAgent.ParseAdd("ChromaLab-download-check/1.0")
    $client = [System.Net.Http.HttpClient]::new()
    $client.Timeout = [TimeSpan]::FromSeconds(60)
    try {
        $response = $client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Range probe failed with HTTP $([int]$response.StatusCode)"
        }
        if ($response.Content.Headers.ContentRange -and $response.Content.Headers.ContentRange.Length) {
            return [int64]$response.Content.Headers.ContentRange.Length
        }
        if ($response.Content.Headers.ContentLength) {
            return [int64]$response.Content.Headers.ContentLength
        }
        throw "No Content-Length or Content-Range length returned"
    } finally {
        $client.Dispose()
        $request.Dispose()
    }
}

function Invoke-Download([string]$url, [string]$targetPath, [int64]$expectedBytes) {
    $targetDir = Split-Path -Parent $targetPath
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if (-not $curl) {
        throw "curl.exe is required for resumable downloads on Windows"
    }
    & $curl.Source -L --fail --continue-at - --output $targetPath $url
    if ($LASTEXITCODE -ne 0) {
        throw "curl download failed for $url"
    }
    $actual = (Get-Item -LiteralPath $targetPath).Length
    if ($actual -ne $expectedBytes) {
        throw "Downloaded size mismatch for $targetPath`: expected $expectedBytes, got $actual"
    }
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
$packages = @($manifest.packages)
if ($ModelId.Trim().Length -gt 0) {
    $packages = $packages | Where-Object { $_.id -eq $ModelId }
    if ($packages.Count -eq 0) {
        throw "No model package found with id '$ModelId'"
    }
}

$largeThreshold = 512MB
$summary = [System.Collections.Generic.List[object]]::new()
$failed = $false

foreach ($package in $packages) {
    Write-Host "== $($package.id): $($package.displayName) =="
    $packageTotal = 0L
    foreach ($file in @($package.files)) {
        $url = Get-HfResolveUrl $file.repository $file.path
        $target = Get-DownloadTargetPath $OutputRoot $package.id $file.repository $file.path
        $remoteSize = Get-RemoteSize $url
        $expected = [int64]$file.expectedBytes
        $ok = ($remoteSize -eq $expected)
        $packageTotal += $remoteSize
        $status = if ($ok) { "OK" } else { "SIZE_MISMATCH" }
        Write-Host ("  {0} {1} bytes {2}" -f $status, $remoteSize, $file.path)
        if (-not $ok) { $failed = $true }

        if ($Download) {
            if ($remoteSize -gt $largeThreshold -and -not $AllowLargeDownloads) {
                throw "Refusing large download $($file.path) ($remoteSize bytes) without -AllowLargeDownloads"
            }
            Invoke-Download $url $target $expected
        }

        $summary.Add([pscustomobject]@{
            packageId = $package.id
            repository = $file.repository
            path = $file.path
            expectedBytes = $expected
            remoteBytes = $remoteSize
            status = $status
            url = $url
            localPath = $target
        })
    }
    $packageOk = ($packageTotal -eq [int64]$package.expectedTotalBytes)
    Write-Host ("  total {0} bytes expected {1} => {2}" -f $packageTotal, $package.expectedTotalBytes, $(if ($packageOk) { "OK" } else { "SIZE_MISMATCH" }))
    if (-not $packageOk) { $failed = $true }
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
$summaryPath = Join-Path $OutputRoot "ocr_model_download_check_summary.json"
$summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
Write-Host "Summary: $summaryPath"

if ($failed) {
    throw "One or more model download checks failed"
}
