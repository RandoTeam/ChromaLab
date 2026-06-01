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

- `androidApp/libs/llama.cpp` was upgraded to `b9464` on 2026-06-01
  (`5dcb71166686799f0d873eab7386234302d05ecf`).
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

### 2026-05-13: GGUF-R1.2

Status: implemented native backend diagnostics and Xiaomi Mi 8 backend gate.

- The Android GGUF build now includes llama.cpp Vulkan backend support, including
  the host shader generator setup required by the Android cross-build.
- Vulkan build compatibility is kept in the app CMake layer; the upstream
  `llama.cpp` submodule remains unmodified.
- The app now logs llama.cpp/ggml warn/error messages into logcat and keeps
  detailed backend/timing logs at the ChromaLab bridge boundary.
- GGUF diagnostics now accept an explicit backend selector:
  `--es backend cpu` or `--es backend accelerated`.
- CPU sessions are now forced with `LLAMA_SPLIT_MODE_NONE` and `main_gpu=-1`.
  This is required because, once Vulkan is registered, llama.cpp otherwise keeps
  GPU devices in the model device list even when `n_gpu_layers=0`.
- Production GGUF loading defaults to CPU until a validated backend selector is
  added. This avoids a hidden switch to an unvalidated accelerator path.
- Vulkan backend availability is now gated by a native preflight for the ggml
  requirement `storageBuffer16BitAccess`.

Xiaomi Mi 8 result with `qwen3vl-2b-q4km`:

| Probe | Backend | Result |
|---|---:|---|
| Text sanity, `Reply with exactly OK.` | CPU | Pass: `OK`, about 31-32 s total |
| Text sanity, same model | Vulkan/accelerated | Blocked before load: Adreno 630 reports `storageBuffer16BitAccess=0` |

Conclusion:

- The Qwen3-VL GGUF file on the phone is valid; the model can load and answer in
  the ChromaLab bridge when the session is truly CPU-only.
- The Xiaomi Mi 8 Vulkan path is not usable for llama.cpp GGUF because ggml
  Vulkan requires 16-bit storage and the device does not expose it.
- This is not a prompt issue and not a corrupt-model issue. It is a backend
  compatibility issue for this device.
- The next GGUF work must test the same diagnostics on the stronger phone, then
  run the image/mmproj probe there. If the stronger phone passes text but stalls
  on image, continue into GGUF-R3/GGUF-R4 prompt-template and mtmd-runtime
  repair.

Example backend-specific commands:

```bash
adb shell am start -n com.chromalab.app/.MainActivity \
  -a com.chromalab.app.DEBUG_GGUF_PARITY \
  --es modelId qwen3vl-2b-q4km \
  --es backend cpu

adb shell am start -n com.chromalab.app/.MainActivity \
  -a com.chromalab.app.DEBUG_GGUF_PARITY \
  --es modelId qwen3vl-2b-q4km \
  --es backend accelerated
```

## 2026-05-19 MTP Chat Runtime Audit

Live log capture on the stronger phone showed that the Qwen3.5 MTP path is
functional after the speculative-context cleanup, but it was not using the
fastest available path:

- The loaded model was `qwen35-mtp-9b-ud-q4kxl`
  (`Qwen3.5-MTP-9B-UD_Q4_K_XL.gguf`).
- The native load line reported `backendCode=0` while
  `accelerated_available=1`, so chat AUTO still loaded CPU-only.
- The same run used `ctx=4096`, `batch=512`, and `mtpDraftTokens=10`.
- Prompt evaluation took about 34.8 s for 274 prompt tokens.
- Earlier MTP stats showed `drafted=768`, `accepted=128`, and about
  182 s for 172 generated tokens, which means MTP was active but acceptance was
  low and the CPU path was too slow for this 9B quant.
- Android system logs reported sustained high CPU load for
  `com.chromalab.app`.

Implemented follow-up:

- GGUF chat accelerator default is now AUTO, and AUTO is allowed to request the
  accelerated llama.cpp backend instead of forcing CPU.
- Native accelerated loads now try 16, then 8, then 4 GPU layers before falling
  back at load failure.
- Native context cap is raised from 8192 to 32768 so the UI context slider and
  runtime limit no longer contradict each other.
- MTP generation logs now include acceptance percentage and tokens/second.
- Qwen3.5 MTP 4B and 9B expose all verified Unsloth GGUF quants in the model
  registry.
- Chat settings now expose selected-model context limits and a live RAM
  estimate that changes with the context window and MTP draft setting.

Open validation:

- Re-run the same 9B MTP prompt on the phone and confirm that AUTO reports an
  accelerated backend when the device supports it.
- Compare `acceptance` and `tps` before/after. If Vulkan loads but is slower or
  unstable on a device, select CPU explicitly and keep the evidence in logcat.
- For weak devices, prefer 4B low/medium quants and smaller context windows; do
  not weaken model output quality to hide hardware limits.

## 2026-05-19 MTP Vulkan Freeze Audit

The follow-up real-device run disproved the previous assumption that the main
problem was only CPU fallback. The model loaded as:

- `llama.cpp Vulkan (16 layers) + MTP draft-mtp(n=10)`.
- Prompt was small: 341 chars / 85 tokens, `ctx=4096`, `batch=512`.
- The run stalled in `llama_mtp_prompt_eval` before the first token; watchdog
  logs continued past 90 seconds and the phone UI became nearly unresponsive.

Upstream/current findings checked against llama.cpp and Unsloth on 2026-05-19:

- llama.cpp names the method `draft-mtp` and exposes it via
  `--spec-type draft-mtp`.
- Unsloth Qwen3.5 4B/9B MTP model cards recommend `-fa on -np 1` and
  `--spec-draft-n-max 6`.
- Unsloth Qwen3.6 27B MTP currently recommends a smaller
  `--spec-draft-n-max 2`, confirming that large draft windows are not a safe
  universal default.
- llama.cpp notes `--mmproj` is not supported with MTP, which matches the app's
  text-only GGUF chat restriction.
- Android Vulkan does not expose a usable ggml backend abort callback in this
  llama.cpp tree, so a bad Vulkan prompt-prefill cannot be reliably interrupted
  from the current JNI bridge.

Implemented follow-up:

- Qwen3.5 MTP default draft window is reduced to 6, matching Unsloth's current
  4B/9B command line instead of the earlier app default of 10.
- Chat `AUTO` for llama.cpp no longer silently selects Vulkan. Users can still
  select Vulkan explicitly, but CPU is now the safe default for MTP models on
  Android until a device/backend profile proves Vulkan stable.
- Native MTP contexts now force `flash_attn=enabled`, matching the upstream
  `-fa on` recommendation.
- Native logs now include `n_ubatch` and `flash_attn` for target and MTP draft
  contexts.
- MTP prompt prefill is chunked through a bounded `n_ubatch` path to avoid a
  single large prefill dispatch dominating UI responsiveness.

Next validation:

- Run Qwen3.5 MTP 4B/9B with CPU default first and record first-token latency,
  acceptance, and tokens/second.
- Then explicitly select Vulkan and compare. If Vulkan stalls before first token
  again, keep it marked experimental for MTP on that device.
- A future native profiling slice should add a dedicated short MTP backend probe
  per device/model/quant before allowing AUTO to pick Vulkan.

## 2026-05-19 MTP Device-Safe Profile

Follow-up source scan after the phone freeze changed the Android defaults again.
The important upstream/forum points are:

- llama.cpp MTP PR #22673 reports good steady-state speed with small draft
  windows, especially `--spec-draft-n-max 2` or `3`, and explicitly notes that
  MTP hurts prompt processing speed because of device-to-host embedding
  transfers.
- Unsloth Qwen3.5 4B/9B MTP examples still show `-fa on -np 1` and
  `--spec-draft-n-max 6`, but those examples target desktop/server runtimes.
- Current user reports for Qwen3.6 MTP commonly use draft windows around `2` or
  `3`; some looping/stability reports were fixed only after newer llama.cpp
  builds and smaller/lower quant settings.
- Android Vulkan can be useful for plain GGUF decode on newer devices, but the
  available evidence is for small non-MTP models and does not prove that
  Android Vulkan MTP prompt prefill is safe.
- llama.cpp issue #22867 confirms MTP + multimodal/mmproj remains a dangerous
  combination; ChromaLab must keep MTP text-only.

Implemented Android profile:

- Default chat MTP draft window is now `3`, not `6` or `10`.
- Built-in MTP model registry exposes a maximum draft slider value of `6`, but
  Android runtime caps the effective value:
  - normal mobile profile: max `3`;
  - explicit Vulkan profile: max `2`;
  - conservative/low-RAM device profile: max `2`.
- MTP context is capped on Android before load:
  - normal mobile profile: max `4096`;
  - conservative/low-RAM profile: max `2048`.
- MTP prompt-prefill batch is capped on Android:
  - normal mobile profile: max `128`;
  - conservative/low-RAM profile: max `64`.
- CPU remains the safe default for MTP models. Vulkan remains user-selectable,
  but it is bounded and logged as experimental.

Rationale:

- The previous real-device stall happened inside `llama_mtp_prompt_eval`, before
  first token output, so increasing GPU offload or widening draft windows is the
  wrong first response.
- Smaller draft/context/batch settings reduce prompt-prefill pressure and phone
  thermal load without changing model output semantics.
- This is still not the final optimization layer. The next runtime slice should
  add a short per-device MTP probe that records first-token latency, acceptance,
  and tokens/second before allowing Vulkan/MTP to be treated as stable.

2026-05-19 real-device A/B result:

- Device/model: Android `qwen35-mtp-4b-q4km`, CPU, `ctx=4096`,
  `batch=128`, `maxTokens=32`.
- No-MTP baseline: first token about `2.0s`, total about `11.3s`.
- MTP draft-mtp `n=3`: first token about `5.4s`, total about `30.8s`,
  acceptance about `45%`.
- Verdict: CPU MTP is slower on this device/model. Production chat must not
  auto-enable MTP for CPU/AUTO profiles. MTP remains available for explicit
  Vulkan diagnostics and future per-device probes.

Implemented follow-up:

- Added debug intent `com.chromalab.app.DEBUG_MTP_AB` for one-command A/B
  testing of the same prompt without MTP and with MTP.
- Android runtime now allows automatic MTP only for explicit non-conservative
  Vulkan profile. CPU/AUTO MTP is disabled by runtime profile and logged with
  `reason=cpu_auto_mtp_ab_slow`.
- The A/B diagnostic now treats an explicitly requested `modelId` as strict:
  if the exact model is not downloaded, the run aborts instead of silently
  falling back to another MTP model.
- The A/B diagnostic no longer applies the production safety clamp to an
  explicitly requested draft value. This allows probing upstream-recommended
  Qwen3.5 MTP values such as `draft=6` while production CPU/AUTO remains
  guarded by the runtime profile.
- Explicit Vulkan MTP now permits up to `6` draft tokens so that the app can
  match the Unsloth/llama.cpp Qwen3.5 MTP guidance during device probes. This
  must still be treated as experimental on Android until a per-device A/B run
  proves speedup and no Vulkan driver crash.

2026-06-02 R5 follow-up:

- `com.chromalab.app.DEBUG_MTP_AB` now exports structured JSON/Markdown under
  `/sdcard/Download/ChromaLab/runtime/mtp-ab/<run_id>/`.
- The exported schema is `gguf-mtp-benchmark-1.0` and records no-MTP vs
  draft-MTP timings, generated-token callback counts, gate decision, and
  missing native metric reasons.
- MTP promotion remains gated. The Kotlin bridge still does not return native
  `drafted` / `accepted` token counters, so R5 records timing evidence but
  keeps automatic enablement in review unless acceptance statistics are present.
- The safety policy rejects MTP for `mmproj` vision and strict chromatogram
  numeric analysis.

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
- llama.cpp speculative decoding documentation:
  https://github.com/ggml-org/llama.cpp/blob/master/docs/speculative.md
- Unsloth Qwen3.5 9B MTP GGUF model card:
  https://huggingface.co/unsloth/Qwen3.5-9B-MTP-GGUF
- Unsloth Qwen3.5 4B MTP GGUF model card:
  https://huggingface.co/unsloth/Qwen3.5-4B-MTP-GGUF
- Unsloth Qwen3.6 27B MTP GGUF model card:
  https://huggingface.co/unsloth/Qwen3.6-27B-MTP-GGUF
- llama.cpp MTP support PR #22673:
  https://github.com/ggml-org/llama.cpp/pull/22673
- llama.cpp MTP + multimodal issue #22867:
  https://github.com/ggml-org/llama.cpp/issues/22867
- Android llama.cpp Vulkan field report:
  https://www.reddit.com/r/termux/comments/1tfhppq/guide_running_llamacpp_with_vulkan_gpu/
- Qwen3.6 MTP looping/stability field report:
  https://www.reddit.com/r/unsloth/comments/1tct6df/looping_issue_with_mtp_on_qwen36/
