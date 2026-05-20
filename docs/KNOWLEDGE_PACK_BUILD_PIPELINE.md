# Knowledge Pack Build Pipeline

Date: 2026-05-20

Phase 6C introduces a computer-side build scaffold under `tools/knowledge-builder/`.

## Pipeline

1. Maintain reviewed source metadata in `tools/knowledge-builder/sources.yaml`.
2. Validate source license status, bundle permission, transform permission, and attribution requirements.
3. Reject or quarantine sources marked `NEEDS_REVIEW`, `REJECTED`, or `PROPRIETARY_FORBIDDEN`.
4. Normalize curated entries into the Knowledge Pack schema.
5. Deduplicate aliases after normalization.
6. Validate required fields and source references.
7. Generate app-ready JSON.
8. Generate `knowledge_build_manifest_v2.json`.
9. Generate `rejected_sources_v2.md`.
10. Run Knowledge Pack tests before committing pack updates.

## Current Builder Artifacts

- `tools/knowledge-builder/sources.yaml`
- `tools/knowledge-builder/build_knowledge_pack.py`
- `tools/knowledge-builder/output/knowledge_build_manifest_v2.json`
- `tools/knowledge-builder/output/rejected_sources_v2.md`

The Phase 6C builder is stdlib-only and intentionally has no download connector. Future connectors must be added source by source after the license register is updated.

## Android Packaging

Seed v2 is packaged as deterministic Kotlin data and mirrored as JSON under `docs/knowledge/chromalab_knowledge_seed_v2.json`. Larger future packs may compile to SQLite FTS5/Room FTS5, but Phase 6C keeps retrieval in memory because the seed is small and deterministic tests are more important than premature indexing infrastructure.
