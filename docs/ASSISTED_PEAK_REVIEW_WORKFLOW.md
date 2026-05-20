# Assisted Peak Review Workflow

## Role

Assisted Peak Review is a fallback workflow. It appears when autonomous peak evidence is review-grade, invalid, incomplete, or when a user explicitly opens evidence review. It is not the primary product path.

## Entry Conditions

Open Assisted Peak Review when:

- peak gate is `REVIEW` or `INVALID`;
- a peak has low S/N, weak baseline, shoulder, overlap, missing apex, or artifact flags;
- recovered label evidence produces review-grade peaks;
- the user wants to inspect or correct the evidence table.

## Supported Decisions

The Phase 1 contracts already support future actions:

- `ACCEPT_AUTO`
- `MARK_REVIEW`
- `REMOVE`
- `ADD`
- `MERGE`
- `SPLIT`
- `ADJUST_BOUNDARY`
- `MARK_SHOULDER`

Phase 5 does not implement the manual peak editor UI. It defines evidence and gate semantics so that a later UI cannot hide user edits.

## Provenance Rules

Every user peak decision must record:

- decision id;
- action;
- target peak id(s);
- timestamp;
- user/session provenance;
- reason or warning where available;
- overlay artifact path when available.

Reports must label user-confirmed and user-edited peak sets explicitly.

## Release Rules

- Assisted Review may satisfy peak gates only with explicit user-confirmed or user-edited evidence.
- User-rejected peaks are excluded from reportable peaks.
- Review-grade peaks remain visibly review-grade unless a future release policy explicitly accepts them.
