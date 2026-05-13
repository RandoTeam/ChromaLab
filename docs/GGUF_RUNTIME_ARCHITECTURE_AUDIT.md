# GGUF Runtime Architecture Audit

Date: 2026-05-13
Scope: GGUF chat and GGUF vision path only. LiteRT remains the stable reference runtime and is out of scope for changes.

## Current Evidence

- The Qwen3-VL 2B Q4_K_M GGUF package is valid outside ChromaLab. The same model answered text and image prompts in LM Studio.
- On Xiaomi Mi 8, ChromaLab loaded the model on CPU and produced one short chat response (`pleasant` in Russian) after about 180.5 seconds.
- The app-reported stats for that run were prompt 14 tokens, answer 2 tokens, 180.5 seconds, and 0.011 tok/s. The UI rounded the rate to `0.0 tok/s`.
- The native log reported `Text first token` only after the prompt prefill stage had already completed, so the current "first token" timing is misleading. The visible delay is mostly before the decode loop.
- Vulkan acceleration is unavailable on Mi 8 for this GGUF path because the Adreno 630 preflight reported no `storageBuffer16BitAccess`. That is a device/backend limitation, not the primary architecture issue.

## Current ChromaLab Path

The current GGUF path is a custom JNI bridge around llama.cpp and mtmd:

- `ChatPlatform.android.kt` builds a single plain text prompt from chat history, then calls `InferenceEngine.inferRawStreaming(...)` with a fake text-only image path.
- `InferenceEngine.inferRawStreaming(...)` has a fallback that calls full `inferRaw(...)` first and only chunks text after the complete response exists. This is not real token streaming.
- `LlamaEngine.inferRaw(...)` wraps text prompts again through `formatGgufTextPrompt(...)` for ChatML-style models.
- `llama_bridge.cpp` then tokenizes the final prompt string and runs manual `llama_decode` / `llama_sampler_sample` loops.
- `nativeInferWithImage(...)` injects an mtmd media marker into the prompt string and evaluates mtmd chunks, but it is still not driven by an OpenAI-style `messages` contract or the model metadata chat template.
- `ChartAnalysisReader.android.kt` uses the same `inferRaw(...)` contract for graph region, axis extraction, and axis structure stages.

This means chat and chromatogram VLM analysis share the same weak abstraction: `prompt string + optional image path -> raw string`.

## Reference Implementations

### PocketPal AI

PocketPal uses `llama.rn` rather than a hand-written prompt bridge.

Important patterns:

- `LocalCompletionEngine` delegates directly to `context.completion(params, callback)`.
- Chat state is converted into structured `{ role, content }` messages.
- Multimodal content is represented as message parts: text plus `image_url`.
- Streaming updates are driven by native completion callbacks.
- Final results include native timings and token counts: `tokens_predicted`, `tokens_evaluated`, and `timings`.
- Chat template handling is delegated to llama.cpp / `llama.rn` through `getFormattedChat(...)` and model metadata when available.

### llama.rn 0.12.0

The current npm latest is `llama.rn` 0.12.0. Its public contract is closer to llama.cpp server semantics:

- `context.completion({ messages, n_predict, stop, ... }, callback)`
- `context.getFormattedChat(messages, ...)`
- multimodal initialization through `context.initMultimodal({ path: mmproj, use_gpu, image_min_tokens, image_max_tokens })`
- structured multimodal messages with `image_url`
- native result timings and token counters
- explicit stop words, cancellation, and partial completion callbacks

Directly importing `llama.rn` is not a good fit because ChromaLab is Kotlin/Compose/KMP, not React Native. But its API shape and native behavior are the correct reference.

### Google AI Edge Gallery

Gallery uses a runtime-level interface instead of exposing raw prompt strings to UI code:

- `LlmModelHelper` owns initialization, reset, cleanup, inference, and cancellation.
- LiteRT-LM is used through `Engine`, `Conversation`, `ConversationConfig`, and `sendMessageAsync(...)`.
- Streaming is callback-first: `MessageCallback.onMessage(...)` emits partial content and `onDone()` closes the response.
- Images and audio are structured content items, not special text markers.
- Models declare image support and compatible accelerators in the allowlist/model metadata.
- Chat UI updates incrementally and stores sessions separately from runtime internals.

This confirms that ChromaLab should keep LiteRT's conversation-style path as the quality reference and make GGUF match that shape.

### llama.cpp

Modern llama.cpp supports multimodal input through `libmtmd`. The supported high-level tools are:

- `llama-mtmd-cli`
- `llama-server` via OpenAI-compatible `/chat/completions`

The current llama.cpp common layer already contains chat-template and Jinja support in `common/chat.*`. ChromaLab currently bypasses most of that by manually building prompt strings.

## Root Problems

1. The GGUF contract is too low-level.
   Chat and VLM analysis should not call `inferRaw(prompt)`. They need a completion request with structured messages, model capabilities, stop words, streaming callbacks, and native timings.

2. Prompt formatting is duplicated and fragile.
   ChromaLab builds a text chat transcript, then `LlamaEngine` may wrap it again in ChatML. For GGUF models, template application should come from the model metadata or llama.cpp common chat template support.

3. Streaming is not native for GGUF.
   The current default streaming path waits for full `inferRaw` completion, then chunks the final answer. On slow devices this looks like a hang and hides where time is spent.

4. Timing and token stats are not trustworthy.
   Kotlin estimates token counts, while the native layer does not return prompt eval timing, prediction timing, or real counters. The "first token" native log currently starts after prompt prefill, so it misses the slowest stage in the Mi 8 test.

5. GGUF vision is not modeled as a first-class multimodal session.
   The bridge loads mtmd and inserts media markers, but lacks a full session contract for media paths, image token budgets, context-shift handling, message templates, and runtime capabilities.

6. Chat and chromatogram analysis are coupled to the same raw inference primitive.
   If chat is unstable, the chromatogram VLM stages inherit the same instability. The chromatogram pipeline should call a stricter vision-analysis API, not generic chat-style raw inference.

7. Failure modes are still hard to diagnose.
   Logs show load and decode stages, but they do not return structured errors, timing sections, backend selection, prefill duration, or exact stop reason to the Kotlin layer.

## Required Runtime Direction

Do not add more prompt-specific patches to the current `inferRaw` path. The next implementation phase should create GGUF Runtime V2 beside LiteRT, then migrate callers deliberately.

Minimum Runtime V2 contract:

- `loadTextModel(modelSpec, backendSpec): LoadedRuntime`
- `loadVisionModel(modelSpec, mmprojSpec, backendSpec): LoadedRuntime`
- `complete(request: ChatCompletionRequest, onToken: TokenCallback): CompletionResult`
- `analyzeImage(request: VisionCompletionRequest, onToken: TokenCallback?): CompletionResult`
- `stop()`
- `unload()`
- `capabilities(): RuntimeCapabilities`

Minimum request shape:

- structured messages with `system`, `user`, and `assistant` roles
- multimodal content parts for image paths
- sampling options
- stop words
- max output tokens
- task id, e.g. `chat`, `chromatogram.graph_region`, `chromatogram.axis_ocr`
- strictness flag: chromatogram analysis must fail clearly when required vision inference fails

Minimum result shape:

- final text
- optional parsed content
- prompt token count
- generated token count
- prompt eval ms
- time to first visible token ms
- generation ms
- predicted tok/s
- stop reason
- backend label
- model id and runtime id
- structured warning/error list

## Implementation Rules For The Next Phase

- Keep LiteRT behavior unchanged.
- Do not route failed GGUF vision analysis into deterministic final reports.
- Do not keep adding model-specific prompt hacks as the primary fix.
- Do not direct-import React Native `llama.rn`; use it as an architectural reference.
- Prefer llama.cpp common chat/template facilities over hand-written prompt templates.
- Add native callbacks before UI polish. A nice chat UI cannot be validated while the runtime only emits final strings.
- Separate text-chat capability from chromatogram-vision capability in the model registry and activation flow.

## Proposed Next Work Slice

One slice only:

1. Add a GGUF Runtime V2 design contract document or interfaces with no behavior migration.
2. Define `ChatCompletionRequest`, `VisionCompletionRequest`, `CompletionResult`, `RuntimeCapabilities`, and `RuntimeTiming`.
3. Add a debug parity checklist for:
   - text-only "Reply exactly OK"
   - normal chat "привет"
   - image question on the reference chromatogram screenshot
   - graph-region JSON task
   - cancellation during prefill and decode
4. No changes to LiteRT, CalculationEngine, report generation, or UI flows in this slice.

## Acceptance Gates Before GGUF Is Used For Chromatogram Reports

- Text chat streams partial output through native callbacks.
- Native result stats are used instead of Kotlin token estimation.
- Model template application is metadata/template based, not double-wrapped.
- Vision model load reports base GGUF, mmproj, modality support, backend, and image token budget.
- A GGUF vision stage either returns parseable structured output or fails with a clear stage error.
- Strict chromatogram mode never writes a final report from partial GGUF output.

## External References

- PocketPal AI: https://github.com/a-ghorbani/pocketpal-ai
- llama.rn: https://github.com/mybigday/llama.rn
- Google AI Edge Gallery: https://github.com/google-ai-edge/gallery
- Google AI Edge Android LLM inference guide: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
- llama.cpp multimodal docs: https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md
