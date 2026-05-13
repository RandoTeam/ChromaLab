# GGUF Model Capability Audit

Date: 2026-05-13

This document records the current GGUF model capabilities before the next runtime fixes.
The goal is to avoid treating every `.gguf + mmproj` package as the same product feature.

## Product Rules

- A model that can load an image is not automatically valid for full chromatogram analysis.
- A document/OCR model is not automatically a normal text chat model.
- Full chromatogram photo analysis requires a real VLM that can inspect the graph image and return structured graph/axis data. If that stage fails, the app must fail honestly instead of continuing with a deterministic-only approximation.
- LiteRT/Gemma remains the working reference engine, but GGUF models must be tested against their own documented prompt/runtime requirements.

## Current Local Test State

Downloaded on the connected device:

| Model ID | Files present | Current classification |
|---|---|---|
| `gemma4-e2b` | `.litertlm` plus LiteRT cache files | Working LiteRT reference, not part of this GGUF audit |
| `qwen3vl-2b-q4km` | base GGUF + Qwen3-VL mmproj | General VLM candidate; currently stalls in `mtmd_helper_eval_chunks` during graph-region detection |
| `paddleocr-vl-q80` | base GGUF + PaddleOCR mmproj | OCR/document/chart-recognition specialist, not normal chat |
| `deepseek-ocr-q80` | base GGUF + DeepSeek-OCR mmproj | OCR/document specialist, not normal chat |
| `moondream2-f16` | base GGUF + Moondream mmproj | Lightweight VLM candidate; needs text-only and image sanity tests |

Latest observed Qwen3-VL run:

- Selected and executed model matched: `qwen3vl-2b-q4km`.
- Runtime was `llama.cpp CPU`; LiteRT was not used.
- Base model and mmproj loaded successfully.
- Image was already normalized to `537x476`.
- Execution stalled before generation, inside `mtmd_helper_eval_chunks`, for more than five minutes.

Update after external parity check:

- The same small Qwen3-VL 2B GGUF model returned normal text and image answers in LM Studio 0.4.12 on the same chromatogram screenshot class.
- This means the current ChromaLab failure is a GGUF runtime/integration problem until proven otherwise, not a model capability failure.
- The runtime repair plan is tracked in `docs/GGUF_RUNTIME_RESEARCH_PLAN.md`.

## Registry Capability Matrix

| Registry family | Built-in variants | Normal text chat | Image/VQA | OCR/document parsing | Chart/SVG parsing | Strict chromatogram candidate | Notes |
|---|---:|---|---|---|---|---|---|
| `qwen3-vl` | 2B/4B/8B, Q2-Q8 | Yes | Yes | General capability, not OCR-specialist | General capability | Yes, after runtime validation | Officially a multimodal VLM series with strong text capability. Requires family-correct chat formatting and validated `mmproj` runtime. |
| `qwen3.5-vl` | 9B Q4_K_M | Yes | Yes | General capability, not OCR-specialist | General capability | Yes, but too heavy for weak/mobile baseline | Model card expects roughly 7-8 GB RAM for full VLM inference. Treat as high-end only. |
| `smolvlm2` | 2.2B Q4_K_M/Q8_0 | Yes | Yes | Moderate OCR | Some chart/image understanding | Candidate, after prompt-template fix and validation | Current app config marks it `RAW`, but the model card exposes a specific chat template. This is a likely cause of bad/empty GGUF chat behavior. |
| `moondream` | 2B F16 | Limited/general completion; primarily visual query | Yes | OCR/document text transcription supported | Chart understanding improved in newer releases | Candidate for auxiliary VQA, not yet primary | Current F16 package is large. Use as a focused VLM test target only after text and image sanity checks. |
| `paddleocr-vl` | 0.9B Q8_0/BF16 | No normal assistant chat | Yes, task-triggered | Yes | Chart recognition trigger | No, auxiliary only | Prompt is task-trigger based, e.g. OCR/chart recognition. It should not appear in normal chat. |
| `deepseek-ocr` | Q8_0 | No normal assistant chat | Yes, task-specific | Yes | Document/table/structured OCR | No, auxiliary only | Uses special OCR/grounding prompts. Keep out of normal chat and strict chromatogram model selection. |
| `dots-mocr` | 1.8B Q5/Q8/BF16 | Task-focused conversational, not default assistant | Yes | Yes | Yes, strong chart/scientific figure to SVG/structure target | No as primary; possible auxiliary parser | Requires recent llama.cpp support. Useful for chart structure/SVG extraction experiments, not a direct replacement for chromatogram calculation. |

## Current App Mismatches

1. `supportsVision` is too coarse.
   It currently mixes general VLMs, OCR-only models, chart parsers, and normal chat-capable models.

2. Chat activation can load the vision projector.
   `ModelManagerController.activate()` calls `llamaShouldLoadVisionProjector()` even for normal chat activation. Text chat should load GGUF text-only unless the chat message actually includes image input.

3. Prompt formatting is incomplete.
   `formatGgufTextPrompt()` only transforms ChatML families. SmolVLM2 needs its own template; Moondream and document OCR families need task-specific flows instead of a generic `User:` transcript.

4. OCR/document models must stay excluded from normal chat.
   PaddleOCR-VL and DeepSeek-OCR are VLM/OCR systems, not general chat assistants. dots.mocr is conversational in a task-focused document parsing sense, so it needs a separate "document/chart parser" surface before appearing as a normal assistant.

5. GGUF text runtime needs the same watchdog visibility as image runtime.
   Current native logs are strongest for image stages. Text-only chat needs explicit tokenization, prompt decode, first-token, decode, timeout, and empty-response diagnostics.

6. GGUF image runtime is CPU-only right now.
   The JNI bridge sets `n_gpu_layers = 0`, `mtmd_params.use_gpu = false`, and reports only CPU. That does not prove every stall is CPU-only, but it does prove the current app does not use GPU/NPU acceleration for GGUF.

## Required Capability Model

Replace the single `supportsVision` decision with explicit capabilities:

| Capability | Meaning |
|---|---|
| `TEXT_CHAT` | Safe to show in normal assistant chat without image input |
| `IMAGE_VQA` | Can answer visual questions about an image |
| `DOCUMENT_OCR` | Specialized for OCR/document extraction |
| `CHART_STRUCTURE` | Specialized for chart/table/SVG/structure extraction |
| `CHROMATOGRAM_STRICT` | Validated for strict graph-region, axis, ion/channel, and report pipeline |
| `AUXILIARY_ONLY` | May support a substage, but must not be selected as the primary full-analysis model |

Initial assignments:

| Family | Capabilities |
|---|---|
| `gemma-4` | `TEXT_CHAT`, `IMAGE_VQA`, `CHROMATOGRAM_STRICT` after LiteRT reference validation |
| `qwen3-vl` | `TEXT_CHAT`, `IMAGE_VQA`; `CHROMATOGRAM_STRICT` only after GGUF runtime validation |
| `qwen3.5-vl` | `TEXT_CHAT`, `IMAGE_VQA`; high-end only |
| `smolvlm2` | `TEXT_CHAT`, `IMAGE_VQA`; chromatogram candidate after prompt/runtime tests |
| `moondream` | `IMAGE_VQA`, possible `TEXT_CHAT`; auxiliary until validated |
| `paddleocr-vl` | `DOCUMENT_OCR`, `CHART_STRUCTURE`, `AUXILIARY_ONLY` |
| `deepseek-ocr` | `DOCUMENT_OCR`, `AUXILIARY_ONLY` |
| `dots-mocr` | `DOCUMENT_OCR`, `CHART_STRUCTURE`, `AUXILIARY_ONLY`; task-focused chat only |

## Validation Plan

Run the same staged matrix for each downloaded GGUF family before changing the production pipeline:

1. Text-only load, without mmproj, with `maxTokens=64`.
   Prompt: "Reply with exactly OK."

2. Text-only chat template validation.
   Verify first token, non-empty response, stop token behavior, and output timing.

3. Minimal image sanity with mmproj.
   Use a tiny controlled image and ask for a one-sentence description.

4. OCR/chart sanity if the model claims document/chart capability.
   Use a cropped axis/title image and verify whether it can read visible text.

5. Graph-region JSON sanity.
   Use the normalized chromatogram image and require only graph bounds JSON.

6. Full chromatogram stage trial.
   Only run after steps 1-5 pass for that family.

7. Record runtime profile.
   Log context, batch, threads, prompt style, mmproj path, image size, stage timings, peak RSS, and blank/stall/failure status.

8. Assign final app visibility.
   Only models that pass text-only tests appear in normal chat. Only models that pass graph-region and axis extraction can become primary chromatogram models.

## Source References

- Qwen3-VL Hugging Face documentation: https://huggingface.co/docs/transformers/model_doc/qwen3_vl
- Unsloth Qwen3-VL GGUF model card: https://huggingface.co/unsloth/Qwen3-VL-2B-Instruct-GGUF
- Qwen3.5 9B VLM GGUF model card: https://huggingface.co/jc-builds/Qwen3.5-9B-VLM-Q4_K_M-GGUF
- PaddleOCR-VL 1.5 GGUF model card: https://huggingface.co/PaddlePaddle/PaddleOCR-VL-1.5-GGUF
- DeepSeek-OCR model card: https://huggingface.co/deepseek-ai/DeepSeek-OCR
- SmolVLM2 model card and chat template: https://huggingface.co/HuggingFaceTB/SmolVLM2-2.2B-Instruct
- Moondream2 model card: https://huggingface.co/vikhyatk/moondream2
- Moondream2 GGUF model card: https://huggingface.co/ggml-org/moondream2-20250414-GGUF
- dots.mocr model card: https://huggingface.co/rednote-hilab/dots.mocr-svg
- dots.mocr GGUF model card: https://huggingface.co/lodrick-the-lafted/dots.mocr-gguf
- llama.cpp multimodal documentation: https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md
