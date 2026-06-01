# R1 - Gemma 4 LiteRT Model Catalog And Download Smoke Tests

Date: 2026-06-02

Status: implemented as a runtime/model-management slice.

## Scope

R1 adds explicit catalog support and pre-download safety checks for current
Gemma 4 LiteRT-LM bundles. It does not start Phase 10 and does not change
chromatographic analysis, `CalculationEngine`, graph geometry, calibration,
trace extraction, or peak detection.

## Catalog Entries Added Or Confirmed

The following Hugging Face URLs were verified with `curl -L -I` on 2026-06-02.
The downloader reads `X-Linked-Size` or `Content-Length` before starting large
smoke-checked downloads.

| Model id | File | Target | Mode | Size |
| --- | --- | --- | --- | ---: |
| `gemma4-e2b` | `gemma-4-E2B-it.litertlm` | Generic Android | FAST | 2,588,147,712 bytes |
| `gemma4-e2b-qualcomm-sm8750` | `gemma-4-E2B-it_qualcomm_sm8750.litertlm` | Qualcomm SM8750 | FAST | 3,016,294,400 bytes |
| `gemma4-e2b-qualcomm-qcs8275` | `gemma-4-E2B-it_qualcomm_qcs8275.litertlm` | Qualcomm QCS8275 / Dragonwing IQ8 | FAST | 3,294,593,024 bytes |
| `gemma4-e2b-google-tensor-g5` | `gemma-4-E2B-it_Google_Tensor_G5.litertlm` | Google Tensor G5 | FAST | 3,953,110,901 bytes |
| `gemma4-e4b` | `gemma-4-E4B-it.litertlm` | Generic Android | FULL_ANALYSIS | 3,659,530,240 bytes |

Repository metadata observed:

| Repository | Last modified | SHA |
| --- | --- | --- |
| `litert-community/gemma-4-E2B-it-litert-lm` | 2026-06-01T05:06:43Z | `3f250541aff494231036164d89603de72cb6dc70` |
| `litert-community/gemma-4-E4B-it-litert-lm` | 2026-06-01T04:48:53Z | `f7ad3343bd6ebc9607f4dc3bc4f2398bd5749bc5` |

## Implementation

### Model metadata

`ModelInfo` now carries runtime catalog metadata:

- `deploymentMode`: `GENERAL`, `FAST`, or `FULL_ANALYSIS`;
- `deviceTarget`: `GENERIC`, `QUALCOMM_SM8750`, `QUALCOMM_QCS8275`, or
  `GOOGLE_TENSOR_G5`;
- `requiresDownloadSmokeCheck`: enables strict remote-size preflight for large
  catalog entries.

The E2B generic package remains the FAST/weaker-device baseline. Device-specific
E2B packages are available as explicit catalog entries, but R1 does not
implement automatic device selection; that is R2.

### Download preflight policy

`ModelDownloadPreflightPolicy` validates:

- non-empty package files;
- HTTPS download URLs;
- positive expected sizes;
- free storage including 512 MiB download headroom;
- observed remote size against expected metadata with a small tolerance;
- partial download file naming used by the Android downloader.

### Android downloader smoke check

For models with `requiresDownloadSmokeCheck = true`, `ModelDownloader` now:

1. checks metadata and available storage before opening the download stream;
2. performs HEAD probes for remote file size;
3. compares remote size against registry metadata;
4. cleans partial `.download` files when preflight fails;
5. starts the actual resumable/range download only after preflight passes.

Existing non-smoke-checked catalog entries keep their previous download behavior
to avoid broad runtime risk in this slice.

## Safety Boundaries

R1 does not:

- make any new model mandatory;
- alter chromatogram graph detection;
- alter tick localization or calibration;
- alter trace/peak calculation;
- change report gates;
- let LiteRT/Gemma/VLM create numeric chromatographic metrics;
- auto-select device-specific models.

## Validation

Required validation for this slice:

```powershell
git diff --check
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.model.*"
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
```

Run `:androidApp:assembleValidation` when Android validation package build time
is acceptable.

## Sources

- Hugging Face Gemma 4 E2B LiteRT-LM:
  https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Hugging Face Gemma 4 E4B LiteRT-LM:
  https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm
- Runtime phase plan:
  `docs/RUNTIME_ACCELERATION_PHASE_PLAN_2026_06_02.md`
