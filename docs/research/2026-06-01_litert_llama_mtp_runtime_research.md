# LiteRT-LM / llama.cpp / MTP Runtime Research

Date: 2026-06-01

## Scope

This note reviews current upstream runtime changes that may improve ChromaLab
model loading, Android performance, MTP/speculative decoding, and local
semantic assistance.

It is a research-only slice. It does not start Phase 10, does not change
chromatographic math, and does not make VLM/MTP output a source of numeric
chromatogram evidence.

## Local Baseline Before This Research Note

The previous runtime upgrade slice already moved ChromaLab to the current
runtime baseline:

| Element | Local state | Notes |
| --- | --- | --- |
| `llama.cpp` | `b9464`, commit `5dcb71166686799f0d873eab7386234302d05ecf` | Latest local submodule state inspected on 2026-06-01 |
| `libmtmd` | Built from the same `llama.cpp b9464` tree | Includes current upstream mtmd sources |
| GGUF MTP | Enabled only for GGUF text-only chat | Still disabled for `mmproj`/vision analysis |
| LiteRT-LM Android | `com.google.ai.edge.litertlm:litertlm-android:0.12.0` | Latest Google Maven release observed in metadata |

Validation for that upgrade slice passed:

```powershell
.\gradlew.bat :androidApp:assembleValidation
.\gradlew.bat :composeApp:compileKotlinDesktop
.\gradlew.bat :composeApp:assembleAndroidMain
```

## Upstream Findings

### 1. llama.cpp MTP/speculative decoding

Relevant upstream items:

| Upstream item | Date/status | ChromaLab impact |
| --- | --- | --- |
| [`speculative : fix n_outputs_max and remove draft-simple auto-enable`](https://github.com/ggml-org/llama.cpp/pull/23988) | Merged 2026-06-01, present in local `b9464` | Important correctness cleanup for speculative paths. Already included locally. |
| [`llama: limit max outputs of llama_context`](https://github.com/ggml-org/llama.cpp/pull/23861) | Merged 2026-06-01, present in local `b9464` | Relevant to safer output limits in generation contexts. Already included locally. |
| [`server-bench: add speed-bench for speculative decoding benchmarking`](https://github.com/ggml-org/llama.cpp/pull/23869) | Merged 2026-05-29 | Not directly used by Android, but it is a useful pattern for ChromaLab's own MTP A/B benchmark output. |
| [`llama + spec: MTP Support`](https://github.com/ggml-org/llama.cpp/pull/22673) | Merged 2026-05-16 | The base feature ChromaLab uses for GGUF text-only MTP. |
| [`Bug: MTP + Vision (multimodal) causes slot position corruption and OOM`](https://github.com/ggml-org/llama.cpp/issues/22867) | Closed 2026-05-10 | Confirms ChromaLab's current safety rule: do not run MTP with `mmproj` vision analysis. |

Local code already has the right high-level guard:

- MTP is initialized with `LLAMA_CONTEXT_TYPE_MTP`.
- MTP is used only for text-only GGUF chat.
- The native bridge rejects MTP when `mmproj` vision analysis is requested.
- MTP logs drafted/accepted token counts and acceptance percentage.

Recommended next ChromaLab work:

1. Keep `llama.cpp b9464` as the current pinned baseline.
2. Add a structured MTP benchmark export for Android debug runs:
   - model id;
   - backend: CPU/Vulkan;
   - prompt tokens;
   - generated tokens;
   - draft tokens max;
   - drafted/accepted tokens;
   - acceptance rate;
   - first-token latency;
   - total tokens/sec;
   - timeout/failure reason.
3. Do not auto-enable GGUF MTP globally. Earlier app validation showed MTP can
   be slower on some Android profiles. It should remain model/profile gated.
4. Keep MTP disabled for GGUF vision/mtmd chromatogram analysis until upstream
   explicitly supports safe MTP + multimodal use and ChromaLab verifies it.

### 2. llama.cpp Android/Vulkan acceleration

Relevant upstream items already present in local `b9464`:

| Upstream item | ChromaLab impact |
| --- | --- |
| [`vulkan: don't hold the device mutex while compiling pipelines`](https://github.com/ggml-org/llama.cpp/pull/23641) | Should reduce Vulkan pipeline compilation contention on supported devices. |
| [`vulkan: reduce host memory lock contention`](https://github.com/ggml-org/llama.cpp/pull/23376) | May improve Android Vulkan runtime behavior, especially around multi-threaded host interaction. |
| [`vulkan: Block-load Q3_K/Q6_K block data and subtract on 32b ints`](https://github.com/ggml-org/llama.cpp/pull/23056) | Potential quantized-model speed benefit for Vulkan paths using affected quantizations. |

ChromaLab already has a Vulkan preflight in the native bridge. The main product
implication is not another library update, but a new device-matrix validation:

- old Adreno devices may still fail required Vulkan features;
- newer Qualcomm/Tensor devices should be retested because b9464 may improve
  Vulkan startup and pipeline compilation behavior;
- CPU must remain the reliable fallback for GGUF text/chat paths.

Recommended next ChromaLab work:

1. Add a "Vulkan runtime probe" result to the model diagnostics export:
   - physical device name;
   - required feature pass/fail;
   - selected backend;
   - initialization time;
   - first-token latency;
   - failure reason if CPU fallback is selected.
2. Run GGUF text-only benchmark on at least:
   - CPU;
   - explicit Vulkan on a supported device;
   - AUTO profile.

### 3. mtmd / multimodal runtime changes

Relevant upstream items:

| Upstream item | ChromaLab impact |
| --- | --- |
| [`mtmd: fix gemma 4 projector pre_norm`](https://github.com/ggml-org/llama.cpp/pull/23822) | Useful for Gemma 4 style multimodal projector correctness in GGUF/mtmd paths. Already included in `b9464`. |
| [`mtmd: Add DeepSeekOCR 2 Support`](https://github.com/ggml-org/llama.cpp/pull/20975) | Adds another OCR-capable model family path. Research-only for ChromaLab until model download, mmproj, and safety validation exist. |
| [`mtmd: DeepSeek-OCR image processing fixes, img_tool::resize padding refactor`](https://github.com/ggml-org/llama.cpp/pull/23345) | Relevant to OCR/crop quality experiments, not a replacement for deterministic graph geometry. |
| [`mtmd: fit_params now take into account mmproj`](https://github.com/ggml-org/llama.cpp/pull/21489) | Improves context-fit calculations for multimodal inputs. Already included through submodule update. |

ChromaLab interpretation:

- mtmd is useful for local crop OCR, semantic warnings, and explanation support.
- mtmd/VLM must not create graph pixels, calibration coefficients, RT, height,
  area, FWHM, S/N, baseline, or Kovats values.
- DeepSeekOCR 2 support is interesting, but it is a model-family research item,
  not a Phase 9 acceptance fix by itself.

Recommended next ChromaLab work:

1. Add mtmd model diagnostics for:
   - base model path;
   - mmproj path;
   - image token count;
   - fit/context status;
   - model family;
   - first crop OCR latency.
2. Keep all mtmd OCR outputs advisory until deterministic geometry has produced
   pixel evidence.

### 4. LiteRT-LM and Gemma 4 MTP

Sources:

- [LiteRT-LM repository](https://github.com/google-ai-edge/LiteRT-LM)
- [Google Developers Blog: LiteRT-LM](https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/)
- [Google Blog: Multi-token prediction in Gemma 4](https://blog.google/innovation-and-ai/technology/developers-tools/multi-token-prediction-gemma-4/)
- [Hugging Face: `litert-community/gemma-4-E2B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
- [Hugging Face: `litert-community/gemma-4-E4B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm)

Observed metadata on 2026-06-01:

| Model repo | Last modified | SHA | Downloads observed |
| --- | --- | --- | --- |
| `litert-community/gemma-4-E2B-it-litert-lm` | 2026-06-01T05:06:43Z | `3f250541aff494231036164d89603de72cb6dc70` | 1,101,381 |
| `litert-community/gemma-4-E4B-it-litert-lm` | 2026-06-01T04:48:53Z | `f7ad3343bd6ebc9607f4dc3bc4f2398bd5749bc5` | 570,863 |

Verified file sizes via `curl -L -I` / `X-Linked-Size`:

| File | Size |
| --- | ---: |
| `gemma-4-E2B-it.litertlm` | 2,588,147,712 bytes / 2.410 GiB |
| `gemma-4-E2B-it_qualcomm_sm8750.litertlm` | 3,016,294,400 bytes / 2.809 GiB |
| `gemma-4-E2B-it_qualcomm_qcs8275.litertlm` | 3,294,593,024 bytes / 3.068 GiB |
| `gemma-4-E2B-it_Google_Tensor_G5.litertlm` | 3,953,110,901 bytes / 3.682 GiB |
| `gemma-4-E4B-it.litertlm` | 3,659,530,240 bytes / 3.408 GiB |

ChromaLab interpretation:

- E2B must remain the baseline production model for FAST/weaker-device mode.
- E4B remains the higher-quality FULL_ANALYSIS path when device memory allows.
- The device-specific LiteRT bundles are the most interesting short-term
  performance opportunity because they may improve runtime on known chipsets.
- The app should not blindly download every device-specific bundle. It should
  select by device family and expose a clear fallback to the generic E2B/E4B
  bundle.

Recommended next ChromaLab work:

1. Add a model catalog distinction:
   - generic E2B;
   - Qualcomm SM8750 E2B;
   - Qualcomm QCS8275 E2B;
   - Google Tensor G5 E2B;
   - generic E4B.
2. Add a HEAD/download smoke test before enabling each catalog entry:
   - URL resolves;
   - `X-Linked-Size` or `Content-Length` captured;
   - free space check passes;
   - checksum/ETag recorded when available;
   - partial download failure cleans up.
3. Add runtime selection:
   - use device-specific E2B only when device match is explicit;
   - otherwise use generic E2B baseline;
   - never fall back from deterministic geometry to VLM-only analysis.
4. Add LiteRT-LM MTP capability diagnostics if the public API exposes it:
   - MTP enabled/disabled;
   - model supports MTP yes/no;
   - first-token latency;
   - total decode latency;
   - timeout status.

### 5. Qwen3.6-27B MTP GGUF

Source:

- [Hugging Face: `unsloth/Qwen3.6-27B-MTP-GGUF`](https://huggingface.co/unsloth/Qwen3.6-27B-MTP-GGUF)

Observed metadata on 2026-06-01:

| Model repo | Last modified | SHA | Downloads observed |
| --- | --- | --- | --- |
| `unsloth/Qwen3.6-27B-MTP-GGUF` | 2026-05-26T08:45:52Z | `5cb35eb3dcbf52dbce5f87dbc64df6aaffadcace` | 952,188 |

Verified selected file sizes:

| File | Size |
| --- | ---: |
| `Qwen3.6-27B-IQ4_XS.gguf` | 15,705,859,200 bytes / 14.627 GiB |
| `Qwen3.6-27B-Q4_K_M.gguf` | 17,106,773,120 bytes / 15.932 GiB |

ChromaLab interpretation:

- This is an interesting MTP model family for desktop/server research.
- It is not a weak-device Android baseline.
- It should not replace the E2B FAST path.
- It may be added later as an optional imported/chat model only after:
  - free-space checks;
  - download resume/cleanup checks;
  - CPU/Vulkan benchmark;
  - MTP acceptance-rate benchmark;
  - memory pressure validation.

## Priority Recommendations For ChromaLab

### P0: Keep current safety boundaries

Do not change these rules:

- VLM/MTP cannot create chromatographic numeric metrics.
- MTP cannot run with `mmproj` vision analysis.
- E2B is supported production FAST/weaker-device mode, but it is advisory for
  geometry, graph count, calibration, trace, and peak metrics.
- Deterministic graph/calibration evidence remains primary.

### P1: Add LiteRT device-specific model catalog entries behind download smoke tests

The most practical improvement is not another library bump. It is robust model
selection for Gemma 4 LiteRT-LM bundles:

- generic E2B as default FAST baseline;
- device-specific E2B when the Android chipset matches;
- generic E4B for FULL_ANALYSIS when memory allows.

Each entry should pass HEAD/download validation before appearing as "available"
in-app.

### P1: Add structured LiteRT/GGUF performance diagnostics

Current logs are useful, but product decisions need exported JSON:

- model id;
- runtime;
- backend;
- model load time;
- first-token latency;
- total generation time;
- OCR crop time;
- model timeout;
- memory warning if available;
- MTP drafted/accepted/acceptance rate for GGUF text paths.

### P2: Re-run Android model validation after b9464

Because b9464 includes Vulkan contention changes and speculative fixes, run a
new device validation slice:

- E2B generic LiteRT baseline;
- E2B device-specific LiteRT if device matches;
- E4B generic LiteRT if memory allows;
- GGUF text-only MTP CPU;
- GGUF text-only MTP explicit Vulkan if preflight passes.

This should be a model/runtime validation slice, not a graph algorithm repair
slice.

### P2: Keep Qwen3.6-27B-MTP as optional desktop/server research

Qwen3.6-27B MTP is current and interesting, but its practical quant sizes are
far above weak-device Android assumptions. It should be documented as a
large-model optional path, not the default student-device model.

## Suggested Next Work Slice

Title:

`Phase 9K - Runtime Model Catalog and Download Smoke Tests`

Scope:

1. Add catalog metadata for Gemma 4 E2B device-specific LiteRT bundles.
2. Add HEAD/check-size/download-resume smoke tests.
3. Add model diagnostics export for LiteRT-LM and GGUF MTP.
4. Do not alter graph/tick/calibration algorithms.
5. Do not accept Phase 9 product runtime until Android evidence still shows
   fixture truth tables and no E2B regressions.

## Sources

- llama.cpp PR #23988: <https://github.com/ggml-org/llama.cpp/pull/23988>
- llama.cpp PR #23861: <https://github.com/ggml-org/llama.cpp/pull/23861>
- llama.cpp PR #23869: <https://github.com/ggml-org/llama.cpp/pull/23869>
- llama.cpp PR #23641: <https://github.com/ggml-org/llama.cpp/pull/23641>
- llama.cpp PR #23376: <https://github.com/ggml-org/llama.cpp/pull/23376>
- llama.cpp PR #23822: <https://github.com/ggml-org/llama.cpp/pull/23822>
- llama.cpp PR #20975: <https://github.com/ggml-org/llama.cpp/pull/20975>
- llama.cpp issue #22867: <https://github.com/ggml-org/llama.cpp/issues/22867>
- LiteRT-LM repository: <https://github.com/google-ai-edge/LiteRT-LM>
- Google Developers Blog, LiteRT-LM: <https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/>
- Google Blog, Gemma 4 MTP: <https://blog.google/innovation-and-ai/technology/developers-tools/multi-token-prediction-gemma-4/>
- Hugging Face, Gemma 4 E2B LiteRT-LM: <https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm>
- Hugging Face, Gemma 4 E4B LiteRT-LM: <https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm>
- Hugging Face, Qwen3.6-27B MTP GGUF: <https://huggingface.co/unsloth/Qwen3.6-27B-MTP-GGUF>
