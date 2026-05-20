# Phase 6C: Full Knowledge Pack Expansion And Data Acquisition Pipeline

Date: 2026-05-20

## Goal

Phase 6C expands the local/offline ChromaLab Knowledge Pack so VLM/OCR/report layers can use sourced terminology, text-classification rules, and caveats without hallucinating measurements.

## Product Boundary

Knowledge can:

- explain terminology;
- classify OCR/VLM text;
- ground warnings;
- provide report caveats;
- preserve source provenance;
- support synonym lookup.

Knowledge cannot:

- fabricate RT, height, area, FWHM, S/N, baseline, Kovats, or integration boundaries;
- override calibration or deterministic integration;
- identify compounds without explicit evidence;
- hide missing evidence gates.

## Deliverables

- `ChromaLabKnowledgeSeedV2`
- `docs/knowledge/chromalab_knowledge_seed_v2.json`
- expanded retrieval filters for alias/type/language/allowed use;
- source/license register;
- builder scaffold under `tools/knowledge-builder/`;
- validation tests for seed v2, retrieval, VLM grounding, and builder artifacts.
- deterministic rule layer for ion/mz/channel/title classification, caveats, and forbidden-use checks.
- source trust tiers and evidence claim scopes.
- compact Gemma-ready retrieval cards.
- optional OPSIN builder-side enrichment plan for name normalization only.

## Source Policy

Seed v2 is ChromaLab-authored and source-referenced. ChEBI is marked as a future CC BY 4.0 candidate; PubChem is marked source-reviewed/API-only until contributor licenses are resolved; NIST/AMDIS/WebBook database content is rejected for Phase 6C bundling.

## Acceptance

Phase 6C is accepted only when the v2 pack validates, tests confirm retrieval and forbidden-use enforcement, no cloud dependency is introduced, and CalculationEngine remains untouched.

## Phase 6C-2 Amendment

The Knowledge Pack is explicitly Rules + Retrieval + Provenance. Retrieval augments the deterministic rule layer; it does not replace it. OPSIN is documented only as a future builder-side normalization tool and cannot create compound identification evidence.
