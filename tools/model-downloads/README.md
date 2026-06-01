# OCR Model Download Pack

This folder contains a download-verification catalog for late-May 2026 OCR/document models.

The catalog intentionally excludes `infly/Infinity-Parser2-Pro` because the safetensors package is approximately 65.4 GiB and belongs in a separate server-class evaluation path.

## Verify URLs and Sizes

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1
```

The script checks each Hugging Face `resolve/main` URL with HTTP `HEAD` / range fallback and writes:

```text
artifacts/model-downloads/ocr_model_download_check_summary.json
```

## Verify One Model

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1 -ModelId paddleocr-v5-mobile
```

## Download One Small Model Package

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1 -ModelId paddleocr-v5-mobile -Download
```

Large files over 512 MiB are refused unless explicitly allowed:

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1 -ModelId nemotron-ocr-v2 -Download -AllowLargeDownloads
```

Downloaded files go under `artifacts/model-downloads/<model-id>/<huggingface-owner>/<repo>/`, which is ignored by git.
