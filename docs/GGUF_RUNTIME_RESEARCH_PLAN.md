# GGUF Runtime Research And Rewrite Plan

Date: 2026-05-13

Scope: GGUF / llama.cpp / mtmd startup, text chat, and vision inference.
This plan does not change the LiteRT/Gemma path. LiteRT remains the stable
reference engine for chromatogram work while GGUF is repaired behind its own
runtime boundary.

## Diagnosis Update

The LM Studio 0.4.12 test changes the working diagnosis.

The same small Qwen3-VL 2B GGUF model can answer a text greeting and can
describe the chromatogram screenshot in LM Studio. Therefore the current
ChromaLab failure must not be explained as "the model is too weak" or "CPU is
too slow" without further proof. The likely failure surface is our GGUF
integration: model/session mode selection, chat-template rendering, mtmd setup,
projector offload settings, prompt evaluation, or Android native bridge
behavior.

Product rule: if the neural vision stage is required and fails, ChromaLab must
stop with a clear GGUF runtime error. It must not silently continue into a
deterministic-only chromatogram report.

## Local Evidence

Current vendored llama.cpp:

- `androidApp/libs/llama.cpp` is at `b9101`
  (`389ff61d77b5c71cec0cf92fe4e5d01ace80b797`).
- Qwen3-VL architecture and mtmd support exist in the vendored tree:
  `src/llama-arch.cpp`, `src/models/qwen3vl.cpp`,
  `tools/mtmd/models/qwen3vl.cpp`.
- Vendored `tools/mtmd/tests.sh` includes
  `ggml-org/Qwen3-VL-2B-Instruct-GGUF:Q8_0` as a vision test target.

Current ChromaLab GGUF bridge differences:

- `androidApp/CMakeLists.txt` disables Vulkan, OpenCL, CUDA, examples, tools,
  and server builds. The current app builds only the custom JNI bridge.
- `llama_bridge.cpp` forces `model_params.n_gpu_layers = 0`.
- `llama_bridge.cpp` forces `mtmd_params.use_gpu = false`.
- `nativeGetAvailableBackends()` reports CPU only.
- Normal model activation can load `mmproj` even for text chat, because
  `ModelManagerController.activate()` uses the same broad vision decision for
  normal chat activation.
- Text completion manually tokenizes and decodes prompts. It does not use the
  model metadata chat template via llama.cpp common/chat-template helpers.
- Image inference manually builds a prompt and calls `mtmd_tokenize` /
  `mtmd_helper_eval_chunks`, while official llama.cpp usage routes through
  `llama-mtmd-cli` or `llama-server` semantics.

Observed app failure:

- Qwen3-VL base model and `mmproj` load successfully.
- Normalized test image is small enough for a sanity test.
- `mtmd_tokenize` completes.
- Runtime stalls before generation inside `mtmd_helper_eval_chunks`.

This points at prompt/session/mtmd evaluation setup or runtime build settings,
not at model file corruption.

## External Research Findings

Official llama.cpp multimodal documentation states that multimodal input is
handled through `libmtmd`, currently via `llama-mtmd-cli` or `llama-server`.
It also documents the two-file model setup: base GGUF plus `--mmproj`, and says
the multimodal projector is GPU-offloaded by default unless disabled.

The official llama.cpp server path supports OpenAI-compatible chat completions,
multimodal input, schema-constrained JSON output, and timing/context reporting.
Its server context forwards `mmproj_use_gpu`, `warmup`, `image_min_tokens`,
`image_max_tokens`, `flash_attn_type`, and the media marker into mtmd. It also
disables context shifting and cache reuse when `mmproj` is loaded.

Qwen's llama.cpp documentation recommends using the GGUF metadata chat template
with `--jinja`, GPU layer offload when available, and `--no-context-shift` for
Qwen3. It also notes that Qwen3 thinking/non-thinking behavior can require
chat-template control.

The Hugging Face Qwen3-VL documentation uses structured multimodal messages and
`apply_chat_template(...)` before generation. That is materially different from
manually concatenating generic strings unless the exact model template is
reproduced.

`llama.rn`, used by PocketPal AI and other mobile apps, is explicitly modeled
after llama.cpp server-style APIs. Its multimodal documentation initializes the
base context first, then calls `initMultimodal(...)` with `mmproj`, recommends
`n_gpu_layers`, disables context shifting for multimodal, and uses structured
messages with image content.

Google AI Edge Gallery is useful as a LiteRT/Gemma UI and model-management
reference. It is not a GGUF runtime reference and should not be mixed into this
repair except at the UI/UX boundary.

## Root Cause Buckets

### P0. Upstream Path Divergence

The app is not using the tested `llama-mtmd-cli` / `llama-server` runtime path.
It hand-rolls a native bridge around low-level llama.cpp and mtmd calls. That
means ChromaLab can fail even when the same model works in LM Studio or
llama.cpp tools.

### P0. Chat Template Mismatch

Qwen3-VL is a chat-template-sensitive VLM. Current text prompts are manually
formatted and only partially mapped by model family. The app needs to render
messages through llama.cpp's template support or through an equivalent upstream
common layer, not through Kotlin string templates.

### P0. CPU-Only GGUF Build

The current GGUF bridge forces CPU for both base model layers and mtmd. This is
not equivalent to LM Studio, official llama.cpp defaults, or mobile wrappers
that enable offload where possible. CPU-only must remain a supported diagnostic
mode, but it must not be the only runtime path.

### P1. Text And Vision Session Mixing

Text chat and image/VLM analysis need separate load modes. A normal chat model
must be able to load base GGUF only, without `mmproj`. A chromatogram vision
session must load base GGUF plus the matching `mmproj`, and must be blocked if
the projector is missing or incompatible.

### P1. Missing Reference Harness

We currently do not have a same-device parity test that compares:

1. official llama.cpp command semantics,
2. the app native bridge,
3. the same prompt/image/model files.

Without that harness, every fix is guesswork.

### P2. Model-Specific Profiles

Qwen3-VL, SmolVLM2, Moondream, PaddleOCR-VL, DeepSeek-OCR, and dots.mocr are not
interchangeable. OCR/document models need task prompts and should not be normal
chat models. General VLMs need chat templates and image-question flow. Only
models passing chromatogram-specific tests can be primary chromatogram models.

## Rewrite Strategy

Do not rewrite the full app. Replace the GGUF runtime boundary while keeping
existing app surfaces stable:

- Keep `InferenceEngine` as the app-facing interface.
- Keep LiteRT/Gemma unchanged.
- Introduce a GGUF runtime v2 behind the llama.cpp implementation.
- Keep the old bridge only as a diagnostic fallback until parity tests pass.
- Make text chat, vision chat, and chromatogram pipeline separate session
  types.

Target session model:

| Session | Loaded files | Allowed use | Must not do |
|---|---|---|---|
| `GgufTextSession` | base GGUF only | normal chat, text sanity tests | load `mmproj` implicitly |
| `GgufVisionSession` | base GGUF + matching `mmproj` | image questions, graph-region/axis extraction | run without image capability |
| `GgufDiagnosticSession` | exact selected files | parity tests, logs, one-off probes | produce final user reports |

## Repair Phases

### Phase GGUF-R1: Build A Reference Harness

Goal: prove what works before changing production logic.

Tasks:

1. Add a debug-only GGUF parity runner that uses the same model files and image
   file as the app pipeline.
2. Run four probes:
   - text: `Reply with exactly OK.`
   - image: `What is shown in this image? Answer in one sentence.`
   - graph region JSON only,
   - axis/title/ion text extraction only.
3. Log rendered prompt length, chat template source, media marker count, image
   size, image token count if available, context size, batch size, threads,
   backend/offload settings, first-token time, total time, output length, and
   failure stage.
4. Compare the result with a host-side `llama-mtmd-cli` or `llama-server` run
   where possible.

Pass criteria:

- Qwen3-VL 2B returns non-empty text in text mode.
- Qwen3-VL 2B returns non-empty image description for the cropped chromatogram.
- The stall point is identified as either prompt/template, mtmd eval, backend,
  context, or model/projector mismatch.

### Phase GGUF-R2: Split Text And Vision Activation

Goal: make chat tests clean.

Tasks:

1. Normal chat activation loads base GGUF only.
2. Image chat and chromatogram analysis load `mmproj` only when image input is
   present and the selected model is image-capable.
3. OCR-only models are physically hidden from normal chat.
4. `VlmEngineHolder` records selected session mode, not only selected model.

Pass criteria:

- Qwen3-VL can be tested in text chat without `mmproj`.
- PaddleOCR-VL and DeepSeek-OCR do not appear as normal chat models.
- LiteRT activation behavior is unchanged.

### Phase GGUF-R3: Replace Manual Prompt Formatting

Goal: render prompts the same way llama.cpp expects.

Tasks:

1. Use llama.cpp metadata chat templates through `llama_chat_apply_template` or
   the upstream common chat-template helpers.
2. Enable Jinja/minja when the model metadata requires it.
3. Support template kwargs such as `enable_thinking=false` where the model
   family requires a non-thinking assistant mode.
4. For images, build structured message content and insert the mtmd media marker
   according to the rendered template, not by ad hoc string insertion.
5. Add template snapshot tests for Qwen3-VL, SmolVLM2, Moondream, and the OCR
   families that remain task-only.

Pass criteria:

- Rendered prompt is inspectable in debug logs.
- Text chat returns a non-empty answer for Qwen3-VL.
- Vision prompt has exactly one media marker for one image.

### Phase GGUF-R4: Replace The Native Runtime Path

Goal: stop duplicating fragile mtmd logic.

Preferred implementation:

1. Move the bridge toward llama.cpp `common` / server-style request handling in
   process, without exposing a local HTTP server to the user.
2. Reuse upstream sampler, chat-template, multimodal marker, prompt eval, and
   structured output behavior where possible.
3. Preserve a small JNI surface from Kotlin:
   - load text session,
   - load vision session,
   - complete messages,
   - complete multimodal messages,
   - unload,
   - get runtime diagnostics.
4. Pass through `n_gpu_layers`, `mmproj_use_gpu`, `warmup`,
   `image_min_tokens`, `image_max_tokens`, `ctx_shift=false`,
   `cache_reuse=0`, and backend selection.

Alternative implementation if in-process common/server reuse becomes too large:

- Use `llama.rn` and PocketPal as implementation references for Android
  multimodal behavior, but do not import a React Native dependency into the app.
  Port only the relevant native runtime pattern if licensing and build shape are
  acceptable.

Pass criteria:

- The same Qwen3-VL probe that works in LM Studio works in ChromaLab.
- Runtime emits timings similar in structure to llama.cpp server timings.
- Old bridge remains available only behind a diagnostic flag until removed.

### Phase GGUF-R5: Add Backend Profiles

Goal: make performance tunable without weakening analysis quality.

Tasks:

1. Add CPU, Vulkan/GPU, and future experimental NPU profile slots.
2. Detect available backends and expose them as runtime capability, not as a
   fake UI toggle.
3. Keep CPU-only as a valid fallback for diagnostics and weak devices.
4. Prefer backend tuning, batch tuning, context tuning, and image-token tuning
   before weakening prompts or skipping analysis stages.

Pass criteria:

- GGUF diagnostics show the selected backend and whether `mmproj` is offloaded.
- Qwen3-VL no longer always reports `llama.cpp CPU` unless CPU was explicitly
  selected or no accelerated backend exists.

### Phase GGUF-R6: Model Validation Matrix

Goal: decide which GGUF models belong where.

Run every built-in and imported candidate through:

1. text-only sanity,
2. model-template sanity,
3. one-image description,
4. OCR/title/axis extraction,
5. graph-region JSON,
6. full chromatogram strict pipeline,
7. token/time/RAM profile.

Visibility rules:

- Normal chat: only `TEXT_CHAT`.
- Image chat: only `IMAGE_VQA`.
- Chromatogram pipeline: only models that pass graph-region, axis, ion/channel,
  and curve extraction sanity.
- OCR/document models: auxiliary only until a separate OCR/document surface is
  implemented.

### Phase GGUF-R7: Production Rollout

Goal: ship safely without breaking Google models.

Tasks:

1. Keep LiteRT/Gemma as the default stable chromatogram engine.
2. Gate GGUF vision behind a validated-model badge.
3. Add clear user-facing errors for:
   - missing projector,
   - wrong projector,
   - unsupported chat template,
   - unsupported backend,
   - image eval timeout,
   - empty model response.
4. Remove old bridge only after GGUF-R1 through GGUF-R6 pass on at least:
   - the strong test phone,
   - Xiaomi Mi 8 or another weak-device baseline.

Pass criteria:

- LiteRT/Gemma still completes the previous chromatogram flow.
- Qwen3-VL GGUF completes text and image sanity probes.
- No final chromatogram report is generated from partial GGUF results.

## Immediate Next Work Slice

Start with Phase GGUF-R1.1:

1. Add a debug-only parity harness around the current GGUF bridge.
2. Add a fixed Qwen3-VL text probe and one-image probe.
3. Log prompt/template/media/backend details before the stall point.
4. Commit the harness separately.

Only after GGUF-R1 identifies the exact divergence should we change production
chat or chromatogram behavior.

## Implementation Log

### 2026-05-13: GGUF-R1.1

Status: implemented as a debug-only diagnostics entry point.

- Android debug builds now accept the intent action
  `com.chromalab.app.DEBUG_GGUF_PARITY`.
- The runner selects the requested downloaded GGUF model, or the active GGUF
  model, and runs a text-only probe with base GGUF only.
- If `imagePath` is supplied, the runner loads a separate vision session with
  base GGUF plus `mmproj` and runs a one-image sanity prompt.
- The native bridge now logs current CPU-only/offload settings, prompt token
  counts, media marker counts, mtmd chunk counts, image token/position counts,
  and first-token timing.
- This diagnostics path does not produce chromatogram reports and does not
  change LiteRT/Gemma behavior.

Example adb command:

```bash
adb shell am start -n com.chromalab.app/.MainActivity \
  -a com.chromalab.app.DEBUG_GGUF_PARITY \
  --es modelId qwen3vl-2b-q4km \
  --es imagePath /sdcard/Download/chromatogram.jpg
```

## Do Not Do

- Do not weaken chromatogram prompts to make weak devices look successful.
- Do not use deterministic-only calculation as a hidden fallback after VLM
  failure.
- Do not load OCR/document models in normal chat.
- Do not mix LiteRT and GGUF fixes in one commit.
- Do not redesign the chat UI while repairing the native runtime.

## Sources

- llama.cpp multimodal documentation:
  https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md
- llama.cpp server documentation:
  https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md
- Qwen llama.cpp documentation:
  https://qwen.readthedocs.io/en/latest/run_locally/llama.cpp.html
- Hugging Face Qwen3-VL model documentation:
  https://huggingface.co/docs/transformers/model_doc/qwen3_vl
- Unsloth Qwen3-VL 2B GGUF model card:
  https://huggingface.co/unsloth/Qwen3-VL-2B-Instruct-GGUF
- llama.rn multimodal documentation:
  https://github.com/mybigday/llama.rn
- PocketPal AI repository:
  https://github.com/a-ghorbani/pocketpal-ai
- Google AI Edge Gallery repository:
  https://github.com/google-ai-edge/gallery
