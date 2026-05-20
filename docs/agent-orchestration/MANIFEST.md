# ChromaLab Orchestration Manifest

Lists all orchestration files under `docs/agent-orchestration/` and `docs/agent-orchestration/expansion/`.

Product modes:
- AUTONOMOUS_PRODUCTION: primary target, automatic evidence may become release-ready only when all gates pass.
- AUTO_DIAGNOSTIC: automatic incomplete/review attempt, diagnostic by default, every terminal state exports evidence.
- ASSISTED_REVIEW: user reviews or corrects only failed/low-confidence autonomous stages.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.
- GUIDED_PRODUCTION: deprecated compatibility alias for earlier guided docs.

This is an orchestration document. Do not modify application logic, CalculationEngine, geometry, OCR, VLM runtime, Android runtime, report rendering, UI implementation, tests, or chromatographic math unless a later active phase explicitly allows it.
