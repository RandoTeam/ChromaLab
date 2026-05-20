# Phase 7 Blocker Matrix

Date: 2026-05-20

| Blocker ID | Requirement | Status | Owner | Resolution |
| --- | --- | --- | --- | --- |
| P7-B01 | Report must show release gate status and evidence gates in every primary surface. | DONE | Scientific Reporting / Compose / QA | UI contract v2 exposes `reportGateEvidence`; HTML, Markdown, and Compose render gate status/evidence. |
| P7-B02 | RELEASE_READY cannot be claimed without explicit evidence package status. | DONE | Orchestrator / QA | Builder accepts `evidencePackageStatus`; default remains `MISSING`, which keeps exports diagnostic/review. |
| P7-B03 | Peak table must expose evidence and gate state. | DONE | Trace/Peak / Chromatography SME | HTML, Markdown, and Compose peak tables include Evidence and Gate columns. |
| P7-B04 | User-facing exports need privacy classification. | DONE | Security & Privacy | Export artifacts include privacy class, diagnostic-only marker, and redaction policy. |
| P7-B05 | Knowledge/VLM must not make unsupported compound identity look confirmed. | DONE | VLM Evaluation / Chromatography SME | Renderers label knowledge/model-only compound names as candidate hypotheses; validator warns. |
| P7-B06 | Kovats/RI cannot be calculated without reference-series evidence. | DONE | Chromatography SME | Validator errors on calculated RI without reference retention times. |
| P7-B07 | Mobile report status cannot be derived only from warnings. | DONE | Mobile UX / Compose | Header and quality summary use `reportGateStatus` and gate reasons. |
| P7-B08 | Visual evidence status remains stringly typed. | DEFERRED | Product Acceptance / Visual Design | Current contract preserves compatibility; enum conversion is approved for Phase 8 because Phase 7 now displays gate evidence explicitly. |
| P7-B09 | Multi-graph mobile navigation is basic. | DEFERRED | Product Acceptance / Mobile UX | Multi-graph structure is supported; advanced graph selector is Phase 8 polish, not a Phase 7 evidence blocker. |

Phase 7 closeout verdict: no release-blocking Phase 7 blockers remain.
