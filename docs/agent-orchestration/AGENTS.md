# ChromaLab Orchestration Agents

- `AGENT_00_ORCHESTRATOR`: Orchestrator / Lead Architect -> `agents/AGENT_00_ORCHESTRATOR.md`
- `AGENT_01_GUIDED_WORKFLOW_UI`: Guided Workflow / UI / State Machine -> `agents/AGENT_01_GUIDED_WORKFLOW_UI.md`
- `AGENT_02_GEOMETRY_CALIBRATION`: Geometry / Calibration Core -> `agents/AGENT_02_GEOMETRY_CALIBRATION.md`
- `AGENT_03_OCR_VLM_TEXT`: OCR / VLM / Text Semantics -> `agents/AGENT_03_OCR_VLM_TEXT.md`
- `AGENT_04_TRACE_PEAK_REVIEW`: Trace Extraction / Peak Review -> `agents/AGENT_04_TRACE_PEAK_REVIEW.md`
- `AGENT_05_QA_EVIDENCE_RELEASE`: QA / Evidence / Benchmark / Release Gate -> `agents/AGENT_05_QA_EVIDENCE_RELEASE.md`
- `AGENT_06_ANDROID_RUNTIME_PERFORMANCE`: Android Runtime / Performance / Storage -> `agents/AGENT_06_ANDROID_RUNTIME_PERFORMANCE.md`
- `AGENT_07_SCIENTIFIC_REPORT_PROVENANCE`: Scientific Report / Provenance -> `agents/AGENT_07_SCIENTIFIC_REPORT_PROVENANCE.md`
- `AGENT_08_SECURITY_PRIVACY`: Security / Privacy / Data Handling -> `agents/AGENT_08_SECURITY_PRIVACY.md`

## Mandatory Selection Protocol

Every non-trivial future task must start with the universal agent/skill selection protocol:

- `protocols/AGENT_SKILL_SELECTION_PROTOCOL.md`

The Orchestrator must classify the task, select required agents and skills, list skipped agents with rationale, define scope boundaries, and define validation/regression obligations before implementation begins.

This is an orchestration document. Do not modify application logic, CalculationEngine, geometry, OCR, VLM runtime, Android runtime, report rendering, UI implementation, tests, or chromatographic math unless a later active phase explicitly allows it.
