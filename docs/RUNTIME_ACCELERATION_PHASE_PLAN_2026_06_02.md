# Runtime Acceleration Phase Plan

Date: 2026-06-02

Status: planning document only.

This plan converts the LiteRT-LM / llama.cpp / mtmd / MTP research findings into
small executable phases. It does not start Phase 10 and does not change
chromatographic math, `CalculationEngine`, graph detection, calibration, trace
extraction, or peak detection.

## Operating Rules

Only one phase may be active at a time.

For each phase, the expected working pattern is:

1. Inspect the current code and docs for the affected runtime path.
2. Break the phase into concrete work items before implementation.
3. Implement only the scoped runtime/model-management change.
4. Validate with targeted checks first, then broader build checks when needed.
5. Update docs and, when applicable, runtime evidence examples.
6. Commit the completed slice with a focused commit message.
7. Report what changed, what was validated, and what remains blocked.

Global constraints:

- Do not start Phase 10 from any runtime acceleration phase.
- Do not modify `CalculationEngine`.
- Do not change chromatographic math.
- Do not let VLM, MTP, GGUF, or LiteRT models create numeric chromatographic
  metrics.
- Do not enable MTP for `mmproj` or vision analysis.
- Do not silently reduce scientific correctness for weaker devices.
- E2B remains the supported FAST/weaker-device production baseline.
- E2B and VLM may assist OCR/semantic/warning text only; deterministic evidence
  remains authoritative for graph geometry, calibration, trace, and peaks.

## Phase R1 - Gemma 4 LiteRT Model Catalog And Download Smoke Tests

Goal:

Add explicit catalog support for current Gemma 4 LiteRT-LM model files without
blindly downloading or enabling them.

Scope:

- Generic Gemma 4 E2B LiteRT baseline.
- Device-specific E2B bundles:
  - Qualcomm SM8750;
  - Qualcomm QCS8275;
  - Google Tensor G5.
- Generic Gemma 4 E4B LiteRT full-analysis candidate.
- Metadata: URL, expected size, runtime, intended device family, mode.
- HEAD/check-size smoke test before any model appears as downloadable.
- Free-space precheck and partial-download cleanup validation.

Out of scope:

- No model is made mandatory.
- No graph-analysis behavior changes.
- No Phase 9 acceptance claim.

Deliverables:

- Model catalog metadata updates.
- Download smoke-test helper or tests.
- Docs for supported model variants and sizes.

Validation:

- Catalog metadata tests.
- Download HEAD/check-size tests.
- Android build if Android code is touched.

Suggested commit message:

`Add Gemma LiteRT catalog download smoke tests`

## Phase R2 - LiteRT Device Selection And Model Discovery

Goal:

Select the correct LiteRT model for the current device while preserving a stable
generic E2B fallback.

Scope:

- Device family detection for supported bundles.
- App-private model location and validation package location rules.
- Discovery diagnostics:
  - selected model id;
  - expected path;
  - exists yes/no;
  - file size;
  - selected mode: FAST or FULL_ANALYSIS;
  - fallback reason.
- Generic E2B fallback when device-specific match is absent.

Out of scope:

- No automatic switch to a lower-quality analysis mode.
- No cloud dependency.

Deliverables:

- Device-selection code or diagnostics.
- Model discovery docs.
- Tests for generic fallback and exact device match.

Validation:

- Unit tests for selection rules.
- Android validation build.

Suggested commit message:

`Add LiteRT model discovery and device selection`

## Phase R3 - LiteRT-LM MTP Capability And Performance Probe

Goal:

Determine whether the current LiteRT-LM API exposes Gemma 4 MTP controls or
diagnostics, and record performance without changing scientific behavior.

Scope:

- Capability probe:
  - model supports MTP yes/no/unknown;
  - runtime exposes MTP yes/no;
  - MTP enabled/disabled;
  - reason if unavailable.
- Timing:
  - model load time;
  - first response latency;
  - total response duration;
  - timeout status.
- FAST mode E2B remains production baseline for weaker devices.

Out of scope:

- No VLM numeric authority.
- No graph/tick/calibration fixes.

Deliverables:

- LiteRT runtime diagnostics.
- MTP capability report.
- Tests for unavailable/unknown capability handling.

Validation:

- LiteRT diagnostics tests.
- Android run if device and model are available.

Suggested commit message:

`Add LiteRT MTP capability diagnostics`

## Phase R4 - Structured Runtime Diagnostics Export

Goal:

Make runtime/model behavior auditable in exported JSON instead of relying on
logcat-only evidence.

Scope:

- Common runtime diagnostics model for:
  - LiteRT-LM;
  - GGUF/llama.cpp;
  - mtmd/mmproj;
  - GGUF MTP text-only;
  - Vulkan preflight.
- Export fields:
  - model id;
  - model path class, not private absolute path in user report;
  - backend;
  - load result;
  - load time;
  - first-token/first-response time;
  - timeout;
  - fallback reason;
  - privacy class.
- Include diagnostics in technical evidence, not normal user report unless
  summarized safely.

Out of scope:

- No algorithm fixes.
- No validator weakening.

Deliverables:

- Runtime diagnostics JSON schema/model.
- Export integration.
- Privacy-safe user-report summary.

Validation:

- Export/manifest tests.
- Privacy tests.
- Android validation build.

Suggested commit message:

`Export structured runtime diagnostics`

## Phase R5 - GGUF Text-Only MTP A/B Benchmark And Gating

Goal:

Benchmark llama.cpp GGUF MTP safely and gate it by measured benefit.

Scope:

- A/B benchmark:
  - no MTP;
  - MTP draft token profiles;
  - CPU;
  - explicit Vulkan only if preflight passes.
- Metrics:
  - prompt tokens;
  - generated tokens;
  - first-token latency;
  - total tokens/sec;
  - drafted tokens;
  - accepted tokens;
  - acceptance rate;
  - timeout/failure reason.
- Model/profile gating based on evidence.

Out of scope:

- No MTP for `mmproj` or vision.
- No MTP in strict chromatogram numeric analysis.

Deliverables:

- Debug benchmark runner/export.
- Gating rules doc.
- Tests for MTP-disabled vision path.

Validation:

- Native Android build.
- Benchmark dry-run on an installed text-only GGUF model if available.

Suggested commit message:

`Add GGUF MTP benchmark diagnostics`

## Phase R6 - llama.cpp Vulkan Runtime Matrix

Goal:

Verify whether recent llama.cpp Vulkan improvements help real Android devices.

Scope:

- Vulkan preflight result export:
  - device name;
  - required feature flags;
  - selected backend;
  - fallback reason.
- Benchmark CPU vs explicit Vulkan vs AUTO for text-only GGUF.
- Confirm old unsupported devices fall back cleanly to CPU.

Out of scope:

- No forced Vulkan on unsupported devices.
- No graph analysis changes.

Deliverables:

- Vulkan probe evidence.
- Runtime matrix doc.
- Tests for fallback reason propagation.

Validation:

- Android validation build.
- Device run where available.

Suggested commit message:

`Add GGUF Vulkan runtime probe matrix`

## Phase R7 - mtmd Multimodal Diagnostics And OCR Research Gate

Goal:

Use current mtmd improvements for better diagnostics and OCR experiments without
making multimodal output a numeric authority.

Scope:

- mtmd diagnostics:
  - base model;
  - mmproj;
  - image token count;
  - fit/context status;
  - support vision/audio flags;
  - crop OCR latency.
- Research-gate DeepSeekOCR 2 support:
  - model availability;
  - mmproj availability;
  - download size;
  - compatibility;
  - safety boundaries.

Out of scope:

- No automatic replacement of deterministic OCR/geometry.
- No model-derived calibration or peak metrics.

Deliverables:

- mtmd diagnostics export.
- OCR research gate doc.
- Tests for advisory-only OCR handling.

Validation:

- Native build.
- Diagnostic export tests.

Suggested commit message:

`Add mtmd multimodal diagnostics gate`

## Phase R8 - Optional Qwen3.6 27B MTP Desktop/Server Track

Goal:

Treat Qwen3.6-27B-MTP as an optional large-model path, not an Android baseline.

Scope:

- Document desktop/server-only assumptions.
- Optional catalog metadata with clear size warnings.
- Download smoke tests only, no automatic install.
- GGUF MTP benchmark only on capable hardware.

Out of scope:

- Not a weak-device model.
- Not a replacement for E2B FAST.
- Not required for Phase 9 acceptance.

Deliverables:

- Optional model catalog doc/update.
- Size/free-space checks.
- Desktop/server benchmark plan.

Validation:

- Metadata tests.
- No Android requirement unless explicitly requested.

Suggested commit message:

`Document optional Qwen MTP large model track`

## Phase R9 - Runtime Evidence Integration Rerun

Goal:

After R1-R7, prove that runtime improvements did not hide or worsen the current
Phase 9 fixture truth.

Scope:

- Rerun runtime evidence on existing Android validation fixtures when requested.
- Compare deterministic vs E2B.
- Confirm:
  - no missing runtime evidence;
  - no missing validator output;
  - no E2B regression;
  - no MTP/mtmd numeric authority;
  - no user-report privacy leak.

Out of scope:

- No new graph/tick/calibration repairs.
- No Phase 10 start unless the product acceptance gate separately allows it.

Deliverables:

- Runtime evidence comparison report.
- Updated truth-audit references.

Validation:

- Android fixture/suite run if device is attached.
- Export/privacy checks.

Suggested commit message:

`Validate runtime acceleration evidence integration`

## Recommended Execution Order

1. R1 - Model catalog and download smoke tests.
2. R2 - Device selection and model discovery.
3. R4 - Structured runtime diagnostics export.
4. R3 - LiteRT-LM MTP capability and performance probe.
5. R5 - GGUF text-only MTP A/B benchmark and gating.
6. R6 - Vulkan runtime matrix.
7. R7 - mtmd multimodal diagnostics and OCR research gate.
8. R8 - Optional Qwen3.6 27B desktop/server track.
9. R9 - Runtime evidence integration rerun.

Reasoning:

- R1 and R2 make model availability deterministic.
- R4 creates the evidence layer needed by all later runtime phases.
- R3/R5/R6 then measure acceleration safely.
- R7 keeps multimodal research bounded.
- R8 is intentionally last because it is not a mobile baseline.
- R9 checks that runtime improvements did not obscure Phase 9 product truth.

## Per-Phase Closeout Template

Each phase closeout should include:

1. Phase name.
2. Scope completed.
3. Files changed.
4. Runtime/product behavior changed.
5. Safety boundaries preserved.
6. Tests/builds run.
7. Android/device runs, if any.
8. Documentation updated.
9. Remaining risks.
10. Whether the next runtime phase may start.
11. Commit hash.
