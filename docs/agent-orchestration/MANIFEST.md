# ChromaLab Orchestration Manifest

Lists all orchestration files under `docs/agent-orchestration/` and `docs/agent-orchestration/expansion/`.

Product modes:
- AUTO_DIAGNOSTIC: automatic attempt, diagnostic by default, every terminal state exports evidence.
- GUIDED_PRODUCTION: reliable target workflow, user confirms graphPanel, plotArea, calibration anchors, trace, and peaks before release-quality output.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.

This is an orchestration document. Do not modify application logic, CalculationEngine, geometry, OCR, VLM runtime, Android runtime, report rendering, UI implementation, tests, or chromatographic math unless a later active phase explicitly allows it.
