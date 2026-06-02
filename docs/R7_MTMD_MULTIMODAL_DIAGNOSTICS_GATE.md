# R7 mtmd Multimodal Diagnostics And OCR Research Gate

Status: Implemented as debug/runtime diagnostics.

Commit target: `Add mtmd multimodal diagnostics gate`

## Scope

R7 adds a diagnostic-only mtmd gate for GGUF multimodal models. It does not
change chromatogram analysis, deterministic geometry, OCR authority, calibration,
trace extraction, peak detection, or `CalculationEngine`.

## Activated Agents

- Orchestrator: kept R7 scoped to diagnostics and research gate behavior.
- Research Intelligence Agent: reviewed current mtmd / DeepSeekOCR 2 upstream status.
- QA / Regression Agent: added contract tests for structured diagnostics and OCR advisory policy.
- Product Acceptance Agent: kept OCR models behind advisory/research gates.
- Android Performance & On-Device AI Agent: added load/tokenization/context-fit diagnostics.
- OCR / VLM / Text Semantics Agent: bounded crop OCR output to advisory text.
- VLM Evaluation Agent: enforced no numeric authority from mtmd/OCR output.
- Security & Privacy Agent: exported only path classes, not raw private model paths.
- Scientific Reporting & Validation Agent: preserved evidence/provenance boundaries.

## Debug Intent

Run on a validation/debug build:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity `
  -a com.chromalab.app.DEBUG_MTMD_DIAGNOSTICS `
  --es modelId deepseek-ocr-q80 `
  --es backend cpu `
  --ez ocr false
```

Optional image path:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity `
  -a com.chromalab.app.DEBUG_MTMD_DIAGNOSTICS `
  --es modelId deepseek-ocr-q80 `
  --es imagePath /sdcard/Download/ChromaLab/some_crop.png `
  --ez ocr true
```

If `imagePath` is omitted, the runner copies the bundled White Tiger validation
fixture into app cache for tokenization diagnostics. The exported summary still
stores only a path class.

## Exported Artifacts

Exports are written to:

```text
/sdcard/Download/ChromaLab/runtime/mtmd-diagnostics/<run_id>/
```

Files:

- `gguf_mtmd_diagnostics_<run_id>.json`
- `gguf_mtmd_diagnostics_<run_id>.md`

The summary includes:

- model id and family;
- backend label;
- base model and mmproj file names, existence, byte size, and path class;
- context and batch size;
- mtmd support flags: vision, audio, mrope;
- image token count;
- total token and position counts;
- chunk table;
- fit/context status;
- bitmap load and tokenize latency;
- optional crop OCR latency;
- OCR advisory-only policy result;
- DeepSeekOCR 2 research gate status.

## Native mtmd Probe

`LlamaEngine.probeMtmdDiagnostics(...)` calls a new JNI method that:

1. uses the already-loaded base model and mmproj;
2. loads the image through `mtmd_helper_bitmap_init_from_file`;
3. builds the prompt with the mtmd media marker;
4. calls `mtmd_tokenize`;
5. exports chunk, token, image-token, position, and context-fit metadata;
6. frees mtmd bitmap/chunk state.

It intentionally does not decode model output. Optional crop OCR is a separate
bounded debug probe and remains advisory-only.

## OCR Safety Boundary

mtmd/OCR output may assist:

- local crop OCR text;
- title / ion / label classification;
- warning explanations;
- research diagnostics.

mtmd/OCR output must not create:

- graph candidate geometry or pixel coordinates;
- plotArea geometry;
- calibration coefficients;
- RT, height, area, FWHM, S/N, baseline, Kovats, or peak metrics;
- compound identity without explicit supporting evidence.

The advisory policy flags forbidden numeric-authority fields if they appear in
OCR output, but it does not promote or consume them as scientific results.

## DeepSeekOCR 2 Research Gate

Current status as of 2026-06-02:

- llama.cpp PR `mtmd: Add DeepSeekOCR 2 Support` was merged on 2026-05-29:
  https://github.com/ggml-org/llama.cpp/pull/20975
- DeepSeek-OCR-2 is available on Hugging Face:
  https://huggingface.co/deepseek-ai/DeepSeek-OCR-2
- A GGUF test repository is available:
  https://huggingface.co/sabafallah/DeepSeek-OCR-2-GGUF
- ChromaLab registry currently includes DeepSeek-OCR v1 GGUF only:
  `deepseek-ocr-q80`.

R7 does not register DeepSeekOCR 2 as a production model. It remains
`RESEARCH_ONLY` until:

- base GGUF and mmproj file sizes are verified;
- Android download smoke checks pass;
- mtmd tokenization and load fit pass on target devices;
- crop OCR latency is measured;
- output safety is validated against forbidden numeric authority fields.

The exported OCR-2 research gate intentionally reports OCR-2
`modelAvailable=false` and `mmprojAvailable=false` until OCR-2 base/mmproj files
are registered and verified. Installed DeepSeek-OCR v1 files are recorded only as
context in the compatibility note.

## Validation

Required checks:

- `git diff --check`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.StructuredRuntimeDiagnosticMtmdTest"`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :androidApp:assembleValidation`

Android runtime probe can be run when a device is attached with the debug intent
above.
