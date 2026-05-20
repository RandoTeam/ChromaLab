# Knowledge Pack Compiler Design

Date: 2026-05-20

The compiler is a computer-side pipeline under `tools/knowledge-builder/`. It is not an in-app downloader.

## Inputs

- `sources.yaml`
- curated source snapshots
- transform scripts
- license metadata

## Required Stages

1. Source license validation.
2. Source tier validation.
3. Alias normalization.
4. Duplicate detection.
5. Claim-scope validation.
6. Attribution manifest generation.
7. Rejected-source report generation.
8. Version and checksum generation.
9. App-ready JSON export.
10. Optional SQLite FTS output for larger future packs.

## Output Formats

Phase 6C-2 commits app-ready JSON only. SQLite FTS/Room FTS remains a future output path for larger packs.

## Checksums

Future builder output should include SHA-256 checksums for:

- source snapshots;
- normalized intermediate files;
- final JSON;
- optional SQLite database.

## Fail-Closed Rules

- Missing license metadata fails the build.
- `TIER_3_LINK_ONLY_RESTRICTED` and `TIER_4_REJECTED` cannot create bundled entries.
- Duplicate aliases must be resolved before export.
- Any entry missing `NOT_MEASUREMENT` fails validation.
