# ChromaLab Agent System

## Core agents

1. **Orchestrator / Lead Architect**
   - Владение архитектурой, контрактами, фазами, merge order, regression gates.

2. **Agent 1 — Guided Workflow / UI / State Machine**
   - Guided flow, Compose UI, zoom/pan, draggable ROI, state persistence.

3. **Agent 2 — Geometry / Calibration Core**
   - graphPanel, plotArea, axes, ticks, calibration anchors, robust fits, residuals.

4. **Agent 3 — OCR / VLM / Text Semantics**
   - ML Kit OCR, VLM local crop OCR, text classification, prompt contracts, safety boundaries.

5. **Agent 4 — Trace Extraction / Peak Review**
   - trace masks, text suppression, trace confirmation, peak overlay, user peak edits.

6. **Agent 5 — QA / Evidence / Benchmark / Release Gate**
   - evidence packages, validators, regression matrix, Android artifact validation, release gates.

## Support agents, activated when needed

7. **Agent 6 — Android Runtime / Performance / Storage**
   - timeouts, threading, storage, Downloads export, logcat, device validation, performance.

8. **Agent 7 — Scientific Report / Provenance**
   - report contract, audit trail, provenance, diagnostic vs release report, user-facing clarity.

9. **Agent 8 — Security / Privacy / Data Handling**
   - local files, private storage, export hygiene, no sensitive leakage, cleanup policies.

## Common rule

Каждый агент обязан:

- выбрать нужные skills из `skills/SKILL_INDEX.md`;
- выполнить web research до реализации;
- записать research notes;
- добавить tests;
- обновить evidence/validator при изменении runtime behavior;
- не закрывать задачу без acceptance evidence.
