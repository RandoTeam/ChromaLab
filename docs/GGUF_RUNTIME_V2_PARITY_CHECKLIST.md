# GGUF Runtime V2 Parity Checklist

Date: 2026-05-13
Purpose: define the minimum tests GGUF Runtime V2 must pass before it can replace the current `inferRaw` GGUF path.

## Scope

This checklist is for GGUF only.

Do not change LiteRT behavior while running these checks. LiteRT remains the reference runtime for chromatogram analysis until GGUF passes the gates below.

## Implementation Status

- Text GGUF inference now has a native chat-template path for chat-style prompts.
- Text streaming now comes from the native decode loop through a JNI token callback.
- General GGUF activation is text-only; chromatogram analysis loads `mmproj` through the dedicated pipeline path.
- Vision GGUF inference still uses the existing mtmd image path and must pass T6 through T10 before chromatogram reports can rely on it.

## Test Matrix

| ID | Test | Input | Required result |
| --- | --- | --- | --- |
| T1 | Text sanity | `Reply with exactly OK.` | Final text is exactly `OK` or a documented equivalent after trimming. |
| T2 | Normal chat | Russian greeting (`privet` / `hello`) | First visible token streams to UI; final answer is natural text, not empty or one malformed token. |
| T3 | Streaming latency | Any short chat prompt | Runtime reports load, prompt eval, first visible token, generation, total timing. UI receives token chunks before final completion. |
| T4 | Stop request during prefill | Start a slow prompt, cancel before first token | Runtime returns `CANCELLED`, clears active generation state, and leaves no stuck streaming message. |
| T5 | Stop request during decode | Start generation, cancel after first token | Runtime stops without crashing and returns partial text with `CANCELLED`. |
| T6 | Vision load | Base GGUF + matching mmproj | Runtime reports `supportsVision=true`, projector path, image token budget, backend label, and no silent text-only downgrade. |
| T7 | Image question | Reference chromatogram screenshot + `What is shown?` | Runtime returns a meaningful image-grounded answer and uses the image path as structured media input. |
| T8 | Graph-region JSON | Reference chromatogram screenshot + graph bounds prompt | Runtime returns parseable graph bounds JSON or a structured stage error. |
| T9 | Axis OCR JSON | Cropped chromatogram plot + axis prompt | Runtime returns parseable axis labels or a structured stage error. |
| T10 | Strict chromatogram failure | Any required VLM stage fails | No final chromatogram report is saved from partial deterministic fallback output. |

## Runtime Output Requirements

Every test run must record:

- model id
- base GGUF path
- mmproj path when used
- backend label
- requested backend
- device backend availability
- context size
- batch size
- image token budget when used
- prompt token count
- generated token count
- prompt eval ms
- time to first visible token ms
- generation ms
- total ms
- generated tokens per second
- stop reason
- warning/error codes

## Acceptance Gates

GGUF Runtime V2 can be connected to the chat UI when:

- T1 through T5 pass on at least one modern test phone.
- Streaming is native, not post-generation chunking.
- Token counts and timings come from native runtime data, not Kotlin estimates.
- Prompt formatting uses model metadata or llama.cpp common chat templates.

GGUF Runtime V2 can be used for chromatogram VLM stages when:

- T1 through T10 pass with the intended vision model package.
- The runtime rejects missing/invalid mmproj files before analysis starts.
- Strict mode produces no final report after low-confidence or unparseable GGUF vision output.

## Notes From Current Mi 8 Run

- The model loaded on CPU and returned a two-token answer after about 180.5 seconds.
- The UI rounded `0.011 tok/s` to `0.0 tok/s`; token count itself was not zero.
- The current native "first token" log does not include prompt prefill time, so it is not enough for performance diagnosis.
- Vulkan is not available for the current GGUF path on Adreno 630 because the preflight reported missing `storageBuffer16BitAccess`.
