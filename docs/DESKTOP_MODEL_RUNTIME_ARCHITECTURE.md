# Desktop Model Runtime Architecture

Status: planning contract.

This document defines how ChromaLab should run local models on desktop without
weakening the Android runtime strategy or the chromatogram analysis contract.

## Core Decision

Not every model is suitable for every workflow.

Model selection must be gated by:

- platform: Android, Windows desktop, Linux desktop, macOS desktop;
- format: `.litertlm`, GGUF base, GGUF `mmproj`, external server model id;
- role: text chat, image/VLM chat, strict chromatogram VLM, OCR/document-only,
  local knowledge helper;
- acceleration: CPU, CUDA, Vulkan, Metal, NPU, or server-managed;
- validation: text smoke test, image smoke test, structured JSON compliance, and
  chromatogram contract stages.

The deterministic chromatogram pipeline remains platform-neutral. Models can help with
recognition and interpretation hints, but numeric peak data still comes from the
deterministic calculation pipeline.

## Desktop Runtime Layers

### Layer 1 - External Local Server Adapter

First desktop implementation target:

- LM Studio OpenAI-compatible API at `http://localhost:1234/v1`.
- Future-compatible with other local OpenAI-compatible servers when they pass the same
  smoke tests.

Why first:

- fastest path to use powerful desktop models;
- lets LM Studio handle model download, loading, CUDA/Vulkan/CPU selection, context,
  and VRAM/RAM tuning;
- avoids bundling large native desktop runtimes before the app contract is stable.

Required adapter behavior:

- configure base URL, optional token, and model id;
- `GET /v1/models` health check;
- text generation through `/v1/chat/completions` or `/v1/responses`;
- image/VLM generation through the same OpenAI-compatible image-capable endpoints;
- optional LM Studio v1 REST integration for load/unload when token/server settings are
  available;
- report runtime as `LOCAL_SERVER` in future metadata, with backend label such as
  `LM Studio / llama.cpp / CUDA` when known.

Strict limitation:

- A local server model is not accepted for chromatogram photo analysis until it passes
  the same required VLM stages: `model.graph_region` and `model.title_ion_axis`.

### Layer 2 - Managed Desktop llama.cpp

Second desktop implementation target:

- app-managed `llama.cpp` process or library for GGUF models;
- prefer a managed `llama-server` process first because it isolates crashes and keeps
  the app JVM simpler;
- later consider direct JNI/JNA only if process overhead becomes a real issue.

Build profiles:

- CPU baseline: always available, AVX2/AVX512/native optimizations where supported;
- Vulkan: cross-vendor GPU path for NVIDIA, AMD, Intel, and some mobile/embedded GPUs;
- CUDA: best target for NVIDIA GPUs when CUDA drivers/toolkit are available;
- Metal: macOS Apple Silicon target if/when macOS desktop support is prioritized.

Runtime selection order on Windows desktop:

1. CUDA, when NVIDIA GPU and CUDA-capable build are available.
2. Vulkan, when CUDA is unavailable or user chooses cross-vendor GPU.
3. CPU, when GPU backend is unavailable or the model cannot fit GPU memory.

Large-model behavior:

- 4 GB VRAM should be treated as partial-offload territory, not full 20 GB model VRAM.
- 32 GB system RAM makes larger GGUF models possible through CPU or CPU+GPU hybrid
  inference, but slower.
- The app must expose actual backend, layer/offload mode, context, prompt tokens,
  generation tokens, and timings in report/chat telemetry.

### Layer 3 - Desktop LiteRT

LiteRT stays the primary Android runtime for now.

Desktop LiteRT should remain experimental until there is a supported desktop package
that can be distributed and validated like the Android runtime. Do not make desktop
LiteRT the default desktop plan.

## Android Runtime Stays Separate

Android remains:

- LiteRT-LM as the preferred stable mobile runtime;
- llama.cpp/GGUF as an alternative runtime only when base GGUF plus matching `mmproj`
  are present and the device can actually load the pair;
- no deterministic-only scientific report when required VLM analysis fails.

Desktop improvements must not change Android model loading or Android report audit
behavior.

## Capability Gates

### Chat Text

Allowed:

- text-only GGUF;
- text-capable `.litertlm`;
- external local server models that respond to a text smoke test.

Rejected:

- OCR-only models that cannot respond conversationally;
- VLM packages that fail text generation.

### Chat With Images

Allowed only when:

- runtime accepts image content;
- a test image request returns a meaningful answer;
- the model is marked as image-capable in local metadata or server capability probe.

### Strict Chromatogram Photo Analysis

Allowed only when:

- image input works;
- model returns parseable structured output for graph-region detection;
- model returns parseable title/ION/axis hints;
- GGUF models have base + matching `mmproj`;
- OCR-only families stay excluded unless a future validated adapter is added.

The model still cannot produce peak heights, areas, FWHM, baseline, S/N, Kovats values,
or final compound assignments. Those remain deterministic/local-knowledge outputs.

## LM Studio Integration Plan

LM Studio should be treated as a desktop local-server provider, not as a library inside
ChromaLab.

Implementation phases:

1. Add `LocalServerModelRuntimeProvider`.
2. Add a desktop settings section:
   - enabled;
   - base URL, default `http://localhost:1234/v1`;
   - optional API token;
   - selected model id;
   - role: chat, image chat, chromatogram VLM candidate.
3. Add server probes:
   - list models;
   - text smoke test;
   - image smoke test;
   - strict chromatogram structured-output smoke test.
4. Add report metadata:
   - selected model id;
   - executed model id;
   - runtime `LOCAL_SERVER`;
   - backend label;
   - server URL host only, not full sensitive URL/token;
   - timings and warnings.
5. Add UX:
   - server reachable/unreachable state;
   - loaded/unloaded state if LM Studio REST v1 is available;
   - clear warning when the server model is text-only or not validated for chromatograms.

## llama.cpp Desktop Integration Plan

Implementation phases:

1. Add desktop runtime abstraction separate from Android JNI.
2. Ship or locate backend binaries:
   - CPU baseline;
   - Vulkan build;
   - CUDA build for NVIDIA systems.
3. Add hardware probes:
   - CPU features;
   - NVIDIA/CUDA availability;
   - Vulkan device availability;
   - free VRAM estimate where available.
4. Add GGUF package validation:
   - base file exists;
   - `mmproj` exists for image/VLM;
   - prompt template/family preset exists;
   - file hash/size validation when downloaded by ChromaLab.
5. Add smoke tests identical to LM Studio adapter.
6. Record backend, offload, context, batch, tokens, and timings in audit.

## Source Notes

- LM Studio docs state that it is available for macOS, Windows, and Linux, can serve
  local models on OpenAI-like endpoints, supports llama.cpp GGUF runtimes, and provides
  REST/OpenAI-compatible APIs.
- LM Studio OpenAI compatibility supports `/v1/models`, `/v1/responses`,
  `/v1/chat/completions`, `/v1/embeddings`, and text/image chat requests.
- LM Studio v1 REST API includes model load/unload endpoints and llama.cpp-specific
  load parameters such as context length, eval batch size, flash attention, and KV-cache
  offload.
- llama.cpp supports CUDA, Vulkan, Metal, HIP, SYCL, BLAS/native CPU, and hybrid
  CPU+GPU inference. For the user's Windows desktop direction, CUDA is the NVIDIA path,
  Vulkan is the cross-vendor fallback, and CPU remains the reliable universal fallback.

References:

- https://lmstudio.ai/docs/app
- https://lmstudio.ai/docs/developer/openai-compat
- https://lmstudio.ai/docs/developer/rest
- https://lmstudio.ai/docs/app/system-requirements
- https://www.mintlify.com/ggml-org/llama.cpp/concepts/backends
- https://github.com/ggml-org/llama.cpp/blob/master/docs/build.md
