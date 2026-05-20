# Phase 1 Research Notes: Shared Contracts and Guided State

Date: 2026-05-20
Phase: 1 - Shared Contracts + GuidedDigitizationState

## Source Quality Triage

Phase 1 decisions use Tier A sources first: official Android, Kotlin, W3C, and FDA documentation. Maintained repositories and standards are secondary. Weak blogs, uncited forum posts, and model-generated claims are not used to drive implementation.

## Sources Reviewed

| Source | Tier | Why relevant | Decision affected | What not to adopt |
| --- | --- | --- | --- | --- |
| Android Developers, [Save UI state](https://developer.android.com/topic/libraries/architecture/saving-states) | A | Current Android guidance on process death, saved state, ViewModel, and persistent storage. | Guided state must be durable persisted model data, not only transient UI element state. | Do not store large image/evidence data in `SavedStateHandle` or Bundle. |
| Android Developers, [Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving) | A | Explains `rememberSaveable`, `SavedStateHandle`, and limitations for complex state. | Phase 1 contracts stay serializable and future UI should save only small cursor/step state transiently. | Do not serialize full images or heavy evidence artifacts into Compose saved state. |
| Android Developers, [Where to hoist state](https://developer.android.com/develop/ui/compose/state-hoisting) | A | Current state-holder and unidirectional data flow guidance. | Future Guided UI should use a screen-level state holder over these contracts. | Do not put business gate logic inside composables. |
| Android Developers, [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state) | A | Current state and observable holder guidance. | Contracts expose immutable serializable data; UI can observe derived state later. | Do not use non-observable mutable collections in UI state. |
| Kotlin, [Serialization documentation](https://kotlinlang.org/docs/serialization.html) | A | Official Kotlin serialization model used by this KMP project. | New contracts use `@Serializable` data classes/enums and schema versioning. | Avoid ad hoc JSON and unregistered polymorphic hierarchies for persisted state. |
| kotlinx.serialization API, [polymorphic discriminator behavior](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-p-o-l-y-m-o-r-p-h-i-c/) | A | Clarifies sealed/polymorphic serialization constraints. | Phase 1 avoids sealed polymorphic state where simple enums/data classes are sufficient. | Do not introduce polymorphic state persistence without a serializers module plan. |
| Android Developers, [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions) | A | Current migration strategy for persistent structured data. | Persistence plan recommends versioned JSON initially and Room integration only with explicit migration tests. | Do not alter Room schema in Phase 1 without a storage implementation requirement. |
| W3C, [PROV Overview](https://www.w3.org/TR/prov-overview/) and [PROV namespace](https://www.w3.org/ns/prov) | A | Provenance vocabulary for entity/activity/agent audit trails. | Confirmation contracts record image, user/session, timestamp, artifact path, and audit entries. | Do not claim full regulatory compliance just because provenance fields exist. |
| FDA, [Part 11 scope and application](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/part-11-electronic-records-electronic-signatures-scope-and-application) | A | Current guidance on electronic records, validation, audit trails, and risk-based controls. | Guided edits must be auditable and must not obscure previous evidence. | Do not market ChromaLab as Part 11 compliant without a dedicated validation program. |

## Decisions

- Phase 1 adds serializable commonMain contracts and tests only.
- Guided state is a durable domain/session model, not a Compose UI implementation.
- Future UI may keep lightweight UI element state in `rememberSaveable` / `SavedStateHandle`, but graph bounds, calibration anchors, trace confirmation, peak decisions, provenance, and evidence paths belong in persistent project/session storage.
- User-confirmed geometry, calibration, and trace may satisfy Phase 0 release gates only in `GUIDED_PRODUCTION` or `MANUAL_ADVANCED`.
- `AUTO_DIAGNOSTIC` state cannot convert user-confirmation objects into release evidence; it must use deterministic auto evidence.
- At least two accepted anchors per axis are required for a manual calibration set to be structurally valid. Two-anchor calibration remains review-grade until future robust validation has sufficient evidence or explicit policy.
- VLM remains outside numeric measurement. Phase 1 contracts do not add any VLM numeric authority.

## Agents Covered

- Research Intelligence Agent: source discovery and source-quality triage.
- Mobile UX Architect Agent: state-machine and future confirmation flow boundaries.
- Compose/KMP UI Agent: KMP serialization and future Compose state-holder constraints.
- Geometry / Calibration Core Agent: graphPanel, plotArea, anchor, residual, and monotonicity contracts.
- Scientific Reporting & Validation Agent: provenance, report gates, and release readiness mapping.
- QA / Regression Agent: transition, serialization, and release-gate tests.
- Product Acceptance Agent: confirms Phase 1 is contracts-only and does not claim production auto-analysis.
- Security & Privacy Agent: stores hashed user/device IDs and paths rather than raw personal identifiers.

## Open Follow-Up For Phase 2+

- Implement storage repository and Room/file migration tests only when Guided UI starts writing real sessions.
- Add real-device restoration tests once screens exist.
- Define redaction and export behavior for user/session provenance in reports.
