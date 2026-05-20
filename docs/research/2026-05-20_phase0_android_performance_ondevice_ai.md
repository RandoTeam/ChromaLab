# Phase 0 Research - Android Performance / On-Device AI

Date: 2026-05-20
Phase: Phase 0
Agents: Android Performance & On-Device AI Agent, VLM Evaluation Agent
Skills: `current-web-research-deep`, `source-quality-triage`, `runtime-validation`, `vlm-boundary-enforcement`
Research confidence: HIGH for runtime-evidence requirements; MEDIUM for later model/runtime tuning

## Research Question

What Android/on-device AI runtime evidence must Phase 0 require without changing model algorithms?

## Sources Reviewed

| Source | Tier | Relevance | Limitation | Decision impact |
| --- | --- | --- | --- | --- |
| [Android ANR vitals](https://developer.android.com/topic/performance/vitals/anr) | Official docs | Long blocking stages must be visible and diagnosable. | Play Console metrics are not local validation by themselves. | Evidence packages must record stage timings and terminal reasons. |
| [Perfetto Android docs](https://developer.android.com/tools/perfetto) | Official docs | Device traces can support later real-device performance validation. | Phase 0 does not add tracing implementation. | Real-device validation checklist should require timings/logs for long stages. |
| [Android Thermal API / ADPF](https://developer.android.com/games/optimize/adpf/thermal) | Official docs | Sustained on-device analysis is affected by thermal state. | Game-focused docs; still applicable to thermal risk. | Future runtime phases should capture thermal/memory warnings; Phase 0 records risk. |
| [LiteRT delegates](https://ai.google.dev/edge/litert/performance/delegates) | Official docs | Acceleration may improve latency/power but depends on model/device support. | Does not prove scientific quality. | Runtime/delegate metadata belongs in evidence; acceleration cannot weaken gates. |
| [LiteRT inference](https://ai.google.dev/edge/litert/inference) | Official docs | Modern on-device inference uses runtime/backend selection. | Phase 0 does not change runtime. | Evidence must preserve executed runtime/model/backend. |
| [MediaPipe LLM Inference Android](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android) | Official docs | On-device LLM/VLM helpers have platform/runtime constraints. | Not ChromaLab-specific. | VLM timeout/failure cannot become hidden numeric evidence. |

## Findings

- Runtime speed is not a substitute for scientific correctness.
- Long VLM/ROI/model stages need stage timings, timeout/warning status, and exported evidence.
- Accelerator/backend metadata is part of provenance, not proof of correct chromatogram analysis.

## Phase 0 Decision

Every terminal state must expose device/model/runtime metadata, stage timings, terminal state, report gate status, and validator output when available.

## Rejected Approaches

- Weakening analysis quality because a phone is slow.
- Treating LiteRT/GPU/NPU availability as validation of chromatographic metrics.
- Letting VLM timeout block evidence export.

## Required Validation

- RuntimeEvidencePackage validation tests.
- Real-device evidence package export in future runtime phases.
- Regression row for VLM timeout / slow graph selection.
