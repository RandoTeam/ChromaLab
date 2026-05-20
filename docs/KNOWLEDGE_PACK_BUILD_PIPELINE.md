# Knowledge Pack Build Pipeline

Date: 2026-05-20

Phase 6C introduces a computer-side build scaffold under `tools/knowledge-builder/`.

## Pipeline

1. Maintain reviewed source metadata in `tools/knowledge-builder/sources.yaml`.
2. Validate source license status, bundle permission, transform permission, and attribution requirements.
3. Validate source trust tiers.
4. Reject or quarantine sources marked `NEEDS_REVIEW`, `REJECTED`, or `PROPRIETARY_FORBIDDEN`.
5. Normalize curated entries into the Knowledge Pack schema.
6. Normalize aliases and detect duplicates.
7. Validate claim scopes and required fields.
8. Generate attribution manifest.
9. Generate `knowledge_build_manifest_v2.json` with version/checksum fields in the future.
10. Generate `rejected_sources_v2.md`.
11. Export app-ready JSON or future SQLite FTS output.
12. Run Knowledge Pack tests before committing pack updates.

## Current Builder Artifacts

- `tools/knowledge-builder/sources.yaml`
- `tools/knowledge-builder/build_knowledge_pack.py`
- `tools/knowledge-builder/output/knowledge_build_manifest_v2.json`
- `tools/knowledge-builder/output/rejected_sources_v2.md`

The Phase 6C builder is stdlib-only and intentionally has no download connector. Future connectors must be added source by source after the license register is updated.

## OPSIN Enrichment Plan

OPSIN may be added as a builder-side optional enrichment tool because current sources list it as MIT licensed. It may only normalize chemical names into synonym/structure metadata when license policy permits. OPSIN output is not compound identification proof and must not create measured chromatographic evidence.

## Android Packaging

Seed v2 is packaged as deterministic Kotlin data and mirrored as JSON under `docs/knowledge/chromalab_knowledge_seed_v2.json`. Larger future packs may compile to SQLite FTS5/Room FTS5, but Phase 6C keeps retrieval in memory because the seed is small and deterministic tests are more important than premature indexing infrastructure.
