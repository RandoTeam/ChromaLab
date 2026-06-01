# OCR Model Download Pack Research

Date: 2026-06-01

## Scope

Current late-May 2026 OCR/document model download candidates were checked against Hugging Face repositories. The goal was to add download verification, not model runtime promotion.

## Included Repositories

- `nvidia/nemotron-ocr-v2`, last modified 2026-05-22, OCR weights approximately 0.51 GiB.
- `datalab-to/surya-ocr-2`, last modified 2026-05-27, safetensors package approximately 1.28 GiB plus tokenizer/config files.
- `datalab-to/surya-ocr-2-gguf`, last modified 2026-05-27, GGUF + mmproj approximately 1.37 GiB.
- `zai-org/GLM-OCR`, last modified 2026-05-19, safetensors package approximately 2.47 GiB plus tokenizer/config files.
- `lightonai/LightOnOCR-2-1B`, last modified 2026-05-04, safetensors package approximately 1.87 GiB plus tokenizer/config files.
- `PaddlePaddle/PP-OCRv5_mobile_det`, `PaddlePaddle/PP-OCRv5_mobile_rec`, `PaddlePaddle/PP-OCRv5_server_det`, `PaddlePaddle/PP-OCRv5_server_rec`; PP-OCRv5 support was published in the Hugging Face PaddleOCR Transformers blog on 2026-05-18.

Repository links:

- <https://huggingface.co/nvidia/nemotron-ocr-v2>
- <https://huggingface.co/datalab-to/surya-ocr-2>
- <https://huggingface.co/datalab-to/surya-ocr-2-gguf>
- <https://huggingface.co/zai-org/GLM-OCR>
- <https://huggingface.co/lightonai/LightOnOCR-2-1B>
- <https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_det>
- <https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_rec>
- <https://huggingface.co/PaddlePaddle/PP-OCRv5_server_det>
- <https://huggingface.co/PaddlePaddle/PP-OCRv5_server_rec>

## Excluded Repository

- `infly/Infinity-Parser2-Pro` / `Infinity-Parser2-35B-A3B`, last modified 2026-05-28, approximately 65.4 GiB safetensors total. Excluded from Android/weak-device download pack.

## Implementation Note

Every included file was resolved through Hugging Face `resolve/main` URLs and stored in `tools/model-downloads/ocr_models_may2026.json` with expected byte counts. The verification script checks remote sizes before any large download is attempted.
