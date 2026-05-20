# ChromaLab Knowledge Builder

This folder contains the computer-side build scaffold for future ChromaLab Knowledge Pack releases.

Phase 6C deliberately does not add an in-app downloader and does not commit large external databases. The builder is the controlled path for converting reviewed, licensed, curated source definitions into app-ready JSON or a future SQLite FTS seed.

## Responsibilities

1. Read source definitions from `sources.yaml`.
2. Fail closed when license metadata is missing or marked rejected.
3. Normalize entries into the ChromaLab Knowledge Pack schema.
4. Deduplicate aliases.
5. Validate entry policies and source references.
6. Generate a versioned pack.
7. Generate an attribution manifest.
8. Generate a rejected-source report.
9. Generate small test fixtures.
10. Export deterministic JSON for Android packaging.

## Current Scope

The Phase 6C scaffold supports curated internal entries and reviewed source metadata. External source connectors are placeholders until licensing and attribution are explicitly cleared.

Do not add PubChem, NIST, AMDIS, or ChEBI bulk imports directly to the app. Add source definitions first, then run license review, then add transforms in a separate focused phase.

## Example

```powershell
python tools/knowledge-builder/build_knowledge_pack.py `
  --sources tools/knowledge-builder/sources.yaml `
  --output docs/knowledge/chromalab_knowledge_seed_v2.json `
  --manifest tools/knowledge-builder/output/knowledge_build_manifest_v2.json `
  --rejected tools/knowledge-builder/output/rejected_sources_v2.md
```

The script is intentionally stdlib-only. The initial `sources.yaml` parser is conservative and validates the current Phase 6C source register shape rather than implementing a general YAML parser.
