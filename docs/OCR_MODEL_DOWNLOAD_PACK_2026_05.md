# OCR Model Download Pack - May 2026

Date: 2026-06-01

## Decision

Add download verification for every late-May OCR/document candidate except `Infinity-Parser2-Pro`.

Excluded:

- `infly/Infinity-Parser2-Pro` / `Infinity-Parser2-35B-A3B`: approximately 65.4 GiB of safetensors weights. This is a server-class parser, not an Android/weak-device download candidate.

Included:

| Package id | Model | Total bytes | Role |
| --- | --- | ---: | --- |
| `nemotron-ocr-v2` | NVIDIA Nemotron OCR v2 | 551,237,466 | OCR benchmark asset |
| `surya-ocr-2` | Surya OCR 2 safetensors | 1,374,033,916 | OCR/layout benchmark asset |
| `surya-ocr-2-gguf` | Surya OCR 2 GGUF + mmproj | 1,471,387,552 | GGUF OCR benchmark asset |
| `glm-ocr` | GLM-OCR | 2,657,425,847 | OCR/document benchmark asset |
| `lightonocr-2-1b` | LightOnOCR-2 1B | 2,022,801,518 | OCR/document benchmark asset |
| `paddleocr-v5-mobile` | PP-OCRv5 mobile det+rec | 22,103,475 | first Android crop-OCR benchmark candidate |
| `paddleocr-v5-server` | PP-OCRv5 server det+rec | 173,555,060 | desktop/server OCR benchmark candidate |

## Important Runtime Boundary

These packages are added for download and benchmark preparation. They are not promoted to ChromaLab final chromatogram analysis authority.

Rules remain:

- OCR/VLM may read local crop text.
- OCR/VLM may classify title, ion, label, warning text.
- OCR/VLM must not create graph coordinates.
- OCR/VLM must not create calibration coefficients.
- OCR/VLM must not create RT, height, area, FWHM, S/N, baseline, Kovats, or final compound identity.

## Files Added

```text
tools/model-downloads/ocr_models_may2026.json
tools/model-downloads/Verify-OcrModelDownloads.ps1
tools/model-downloads/README.md
```

## Verification

Run:

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1
```

This checks all files with HTTP `HEAD` / range fallback and compares remote `Content-Length` with the manifest.

Actual large downloads are opt-in:

```powershell
.\tools\model-downloads\Verify-OcrModelDownloads.ps1 -ModelId paddleocr-v5-mobile -Download
.\tools\model-downloads\Verify-OcrModelDownloads.ps1 -ModelId nemotron-ocr-v2 -Download -AllowLargeDownloads
```

Output goes under ignored `artifacts/model-downloads/`.
