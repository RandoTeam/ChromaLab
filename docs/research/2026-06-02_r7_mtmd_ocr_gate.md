# R7 mtmd / OCR Current Research Notes

Date: 2026-06-02

## Sources Checked

| Source | Finding | ChromaLab decision |
| --- | --- | --- |
| https://github.com/ggml-org/llama.cpp/pull/20975 | `mtmd: Add DeepSeekOCR 2 Support` merged on 2026-05-29. The PR adds DeepSeek-OCR-2 mtmd support, multi-tile dynamic-resolution preprocessing, Qwen2 vision encoder handling, and llama-mtmd-cli / llama-server verification notes. | Useful for future OCR experiments; not enough for production registration without Android model/download smoke checks. |
| https://github.com/ggml-org/llama.cpp/pull/21489 | `fit_params now take into account mmproj` merged on 2026-05-20. Adds mmproj memory accounting for fit behavior in llama-server / llama-cli. | R7 exports context-fit/token metadata so Android diagnostics can catch overlarge multimodal inputs. |
| https://github.com/ggml-org/llama.cpp/pull/23345 | DeepSeek-OCR image processing fixes merged on 2026-05-20. The PR notes DeepSeek-OCR sensitivity to resize/padding parity and changes regression tests to OCR metrics. | Treat OCR results as research/advisory until app-side crop preprocessing and safety validation are proven. |
| https://huggingface.co/deepseek-ai/DeepSeek-OCR-2 | DeepSeek-OCR-2 model exists, Apache-2.0, image-text-to-text, custom code, Transformers/vLLM/SGLang instructions. | Research candidate only; original HF model is not directly an Android GGUF package. |
| https://huggingface.co/sabafallah/DeepSeek-OCR-2-GGUF | GGUF test files are listed for DeepSeek-OCR-2 base and mmproj. | Candidate source for future download-size and Android smoke checks; not registered in R7. |
| https://huggingface.co/ggml-org/DeepSeek-OCR-GGUF | Existing DeepSeek-OCR v1 GGUF is available with Q8_0 base/mmproj and llama.cpp instructions. | Already represented in the app registry as `deepseek-ocr-q80`. |

## Synthesis

The useful R7 change is diagnostics, not model replacement.

mtmd can now expose the evidence ChromaLab lacked for GGUF multimodal debugging:

- whether mmproj loads;
- whether vision/audio/mrope are supported;
- how many image tokens are created;
- whether the prompt fits the configured context;
- how expensive crop OCR is.

DeepSeekOCR 2 is interesting because upstream mtmd support landed very recently,
but it must stay behind a research gate. Its output is OCR/semantic text, not
chromatographic evidence. It cannot replace deterministic graph detection,
axis geometry, calibration anchors, trace extraction, peak integration, or report
scientific provenance.

## R7 Boundaries

Accepted:

- add a debug intent and exports for mtmd diagnostics;
- expose image token count, chunk table, and context fit;
- optionally measure crop OCR latency;
- flag forbidden numeric authority fields in OCR output;
- document DeepSeekOCR 2 as research-only.

Rejected:

- registering DeepSeekOCR 2 as production without smoke checks;
- using OCR/VLM text to create pixel coordinates;
- using OCR/VLM text to create RT/height/area/FWHM/S/N/baseline/Kovats;
- replacing deterministic OCR/geometry/calibration with mtmd output.
