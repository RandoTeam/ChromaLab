# Phase 6C Mobile Retrieval And Packaging Research

Date: 2026-05-20

## Sources

| Topic | Source | Relevance | Decision |
|---|---|---|---|
| SQLite FTS5 / BM25 | https://www.sqlite.org/fts5.html | Official SQLite FTS5 docs describe BM25 ranking, snippets, highlighting, and tokenizer behavior. | SQLite FTS5/BM25 is the preferred future Android retrieval backend for larger packs. |
| Room FTS5 | https://developer.android.com/reference/androidx/room3/Fts5 | Android Room exposes FTS5 annotations and options, with `unicode61`, `porter`, `ascii`, and `trigram` tokenizer options. | Room FTS5 is the preferred future app database integration if/when the JSON seed grows. |
| Android app-specific files | https://developer.android.com/training/data-storage/app-specific | Official Android docs say app-specific internal files are private to the app and suitable for sensitive data. | Knowledge packs and generated indexes should live in packaged assets or app-specific storage; no broad storage permission. |
| Android storage overview | https://developer.android.com/training/data-storage/ | Official Android docs emphasize purpose-based storage and privacy. | Pack updates should use explicit user/local package import or app-specific update path, not hidden cloud download. |
| W3C PROV-O | https://www.w3.org/TR/prov-o/ | Provides provenance model for source and build tracking. | Build manifests should capture source entities, transform activities, builder version, and output artifacts. |

## Retrieval Decision

Phase 6C keeps the runtime retrieval simple and deterministic:

- exact alias lookup;
- lexical token search;
- type filtering;
- language filtering;
- allowed-use filtering;
- top-K stable scoring;
- returned forbidden-use and source references.

Future larger packs can compile to SQLite FTS5 / Room FTS5 with BM25, but Phase 6C does not add a new Android database dependency because the seed is small and tests need deterministic behavior.

## Packaging Decision

- The committed seed is local JSON plus Kotlin runtime model.
- No cloud dependency is introduced.
- No app-side uncontrolled downloader is added.
- Computer-side builder scaffolding prepares future reviewed pack creation.
- Future update packs should be signed/versioned and stored in app-specific storage only after explicit user or release-channel action.

## What Not To Adopt

- No local vector search until lexical retrieval cannot satisfy reviewed use cases.
- No remote RAG.
- No hidden chemical database downloads.
- No user image/report upload for knowledge lookup.
