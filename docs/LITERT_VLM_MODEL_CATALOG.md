# LiteRT VLM Model Catalog

Date: 2026-05-13

This catalog records non-default LiteRT-LM vision-language models added to the in-app model manager. They are candidates for chromatogram photo analysis, but they still need device and chromatogram-specific validation before they can replace the current stable Gemma LiteRT path.

## Added Models

| App id | Hugging Face repo | File | Size | Vision support | License | Created | Updated |
| --- | --- | --- | ---: | --- | --- | --- | --- |
| `fastvlm-05b-litert` | `litert-community/FastVLM-0.5B` | `FastVLM-0.5B.litertlm` | 1,156,349,952 bytes | Yes, LiteRT-LM image input | Apple AMLR | 2025-12-02 | 2026-04-29 |
| `qwen35-08b-litert-vlm` | `GabrieleConte/Qwen3.5-0.8B-LiteRT` | `qwen35_mm_q8_ekv2048.litertlm` | 1,159,757,824 bytes | Yes, bundled vision encoder/adapter | Apache-2.0 | 2026-03-07 | 2026-03-07 |

## Runtime Rules

- Both entries use `ModelRuntime.LITERT_LM` and `ModelFileType.LITERT_BUNDLE`.
- Both entries are marked `supportsVision=true` and are eligible for chromatogram vision selection.
- LiteRT-LM receives image/text through `Content.ImageFile` and `Content.Text`; these models must not be wrapped in GGUF ChatML text.
- FastVLM license terms must be reviewed before any public release that distributes or promotes it as a bundled/default option.

## Validation Required

1. Download from the in-app model manager.
2. Confirm file size and LiteRT bundle validation pass.
3. Activate in chat and verify text response.
4. Run a basic image question with a cropped chromatogram.
5. Run the strict graph-region, axis, and axis-structure VLM stages.
6. Record backend, time, token stats, and whether the output is parseable JSON.
