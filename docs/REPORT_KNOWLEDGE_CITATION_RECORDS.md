# Report Knowledge Citation Records

Status: Phase 7B hardening contract

## Purpose

Knowledge Pack and VLM-grounded explanations in a report must be auditable. Phase 7B makes Knowledge Pack use first-class report data instead of rendering it only as flattened interpretation text.

## Contract

Every `ChromatogramReport` may include `knowledgeCitations`.

Each citation records:

- `knowledgePackVersion`
- `usedEntryIds`
- `usedEntryRecords`
- `entryId`
- `entryType`
- `claimScope`
- `allowedUse`
- `forbiddenUse`
- `sourceRefs`
- `trustTier`
- `explanationTarget`
- `generatedBy`
- `unsupportedClaims`
- `rejectionReason`

Valid explanation targets are report summary, warning, caveat, compound hypothesis, axis-label explanation, peak warning, and method note. Valid generators are deterministic mapper, Knowledge Pack, and VLM with Knowledge Pack context.

## Rules

1. Scientific/domain explanations must cite Knowledge Pack entry IDs.
2. VLM-generated explanations without `usedEntryIds` are review evidence only and are validator-visible.
3. Unsupported claims are not shown as facts.
4. Knowledge Pack entries cannot create measured RT, height, area, FWHM, S/N, baseline, Kovats/RI, calibration, or peak metrics.
5. User-facing reports may show concise citation labels. Technical evidence exports show full citation records.

## Validation

`ReportContractValidator` emits:

- `knowledge.used_entry_ids_missing` when VLM-with-knowledge output lacks entry IDs.
- `knowledge.used_entry_record_missing` when an entry ID lacks a full record.
- `knowledge.unsupported_claims_present` when unsupported claims remain.
- `knowledge.explanation_rejected` when a grounded explanation was rejected.
- `knowledge.numeric_metric_forbidden` when a knowledge citation attempts numeric metric creation.
