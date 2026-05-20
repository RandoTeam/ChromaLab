# Guided State Persistence Plan

Phase 1 creates serializable contracts only. It does not implement the storage repository or UI restoration.

## Persistence Goals

Guided state persistence must support:

- restoration after process death;
- continuation after app restart;
- audit trail of user changes;
- evidence package export;
- report provenance;
- future migrations.

## Storage Strategy

Recommended future layers:

1. In-memory screen state holder for live editing.
2. Small transient UI element state in `rememberSaveable` or `SavedStateHandle`.
3. Durable guided session state as serialized JSON or Room-backed entity.
4. Large artifacts stored as files referenced by path, not embedded in saved state.

Phase 1 adds the durable model only.

## Serialization

The contracts use `kotlinx.serialization` and a schema version:

- `CURRENT_GUIDED_DIGITIZATION_STATE_SCHEMA = "1.0.0-phase-1"`

Persistence should use JSON with:

- `ignoreUnknownKeys = true`;
- `encodeDefaults = true`;
- explicit migration tests when fields change.

The current model avoids polymorphic sealed persistence so future migrations remain straightforward.

## Room Integration Plan

Do not change Room schema until a real storage implementation is required.

Future Room entity shape:

- `stateId` primary key;
- `schemaVersion`;
- `mode`;
- `currentStep`;
- `updatedAtEpochMillis`;
- `jsonPayload`;
- optional project/sample/report foreign keys.

Migration rule:

- every schema version bump requires migration notes and serialization regression tests.

## Process Death Restoration

Future UI should restore:

- current state ID from navigation or saved state;
- current step from durable `GuidedDigitizationState`;
- lightweight viewport/selection state from `SavedStateHandle` or `rememberSaveable`;
- artifact images from paths.

Do not store full images, masks, overlays, or evidence packages in Bundle-backed saved state.

## Privacy Constraints

Guided state may contain user edits and paths to user images. It must:

- avoid raw user identifiers;
- use hashed user/device/session IDs where needed;
- keep artifact paths explicit for export review;
- support redaction in evidence package export;
- not include full image bytes in JSON state.

## Compatibility With Report Contract

Guided state maps to Phase 0 release gates through `GuidedReportGateMapper`.

Future report mapping should preserve:

- processing mode;
- user confirmation status;
- graphPanel and plotArea evidence;
- calibration anchors and residuals;
- trace confirmation evidence;
- peak review decisions;
- audit trail reference.

## Validation Required In Future Implementation

When storage is implemented, add tests for:

- JSON roundtrip;
- unknown field tolerance;
- migration from previous schema;
- process death restoration;
- report-gate preservation;
- privacy redaction in exports.
