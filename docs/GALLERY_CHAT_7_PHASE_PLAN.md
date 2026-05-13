# Gallery Chat Redesign - 7 Phase Plan

Status: active tracking plan.

This file is the source of truth for the Google AI Edge Gallery-style chat redesign.
After each completed phase or subphase, update the checkboxes here before committing.

Project rules for this plan:

- Work on only one phase at a time.
- If a phase is large, complete only one subphase per work slice.
- Do not mix chat UI, runtime/model loading, chromatogram analysis, reports, and release work in one commit.
- Commit after each completed work slice.
- Do not weaken model/runtime correctness to make UI progress faster.

## Current Position

- Current completed phase: Phase 3.2.
- Next phase to start: Phase 3.3.
- Phase 1 technical contract: `docs/GALLERY_CHAT_PHASE_1_CONTRACT.md`.

## Phase 1 - Gallery Audit And Pixel Contract

Status: completed.

- [x] Clone/inspect Google AI Edge Gallery source and chat mockups.
- [x] Identify source files for top app bar, model chip, picker, config dialog, chat panel, streaming text, thinking block, composer, colors, typography, and assets.
- [x] Record exact code anchors and visual rules in `docs/GALLERY_CHAT_PHASE_1_CONTRACT.md`.
- [x] Define ChromaLab-specific integration rules so Gallery style does not break scientific/minimal app behavior.
- [x] Commit the contract before implementation.

## Phase 2 - Top App Bar, Model Chip, Model Picker Shell

Status: completed.

- [x] Replace old chat top bar with Gallery-style `CenterAlignedTopAppBar`.
- [x] Add centered chat title plus selected-model chip.
- [x] Move model selection out of the settings sheet.
- [x] Add separate model picker `ModalBottomSheet`.
- [x] Make picker selection bind the model to the current chat session only.
- [x] Keep model loading/runtime behavior unchanged.
- [x] Update the contract with Phase 2 status.
- [x] Validate with Android debug assembly.
- [x] Commit Phase 2 separately.

## Phase 3 - Runtime Controls And Capability Gating

Status: in progress.

- [x] Phase 3.1: Define chat runtime UI state for backend, accelerator, thinking support, and model capability flags.
- [x] Phase 3.2: Add capability-gated accelerator controls for LiteRT/GGUF where runtime support is real.
- [ ] Phase 3.3: Add thinking toggle only for models that actually support it.
- [ ] Phase 3.4: Disable unsupported model/runtime choices with clear UI reasons instead of hiding them silently.
- [ ] Phase 3.5: Keep selection separate from loading; loading still starts only on first message or explicit runtime action.
- [ ] Phase 3.6: Validate that chromatogram model/runtime flows are not affected.
- [ ] Phase 3.7: Commit Phase 3 work slices separately.

## Phase 4 - Message Layout And Telemetry

Status: not started.

- [ ] Phase 4.1: Rework message rows toward Gallery's calmer chat hierarchy.
- [ ] Phase 4.2: Use lighter assistant response presentation and preserve user/assistant alignment.
- [ ] Phase 4.3: Keep token/time stats under completed assistant messages.
- [ ] Phase 4.4: Extend stats with model name, backend, accelerator, duration, and tokens/sec where data is available.
- [ ] Phase 4.5: Preserve readable scientific output, tables, and markdown-like report fragments.
- [ ] Phase 4.6: Validate long messages and small-screen wrapping.
- [ ] Phase 4.7: Commit Phase 4 work slices separately.

## Phase 5 - Streaming Text And Thinking Block

Status: not started.

- [ ] Phase 5.1: Replace simple character reveal with buffered/fading streaming behavior inspired by Gallery.
- [ ] Phase 5.2: Keep streaming markdown/table output readable without excessive reparse jank.
- [ ] Phase 5.3: Add thinking block UI only when runtime emits thinking separately.
- [ ] Phase 5.4: Auto-expand thinking while generation is active, then allow collapse/expand.
- [ ] Phase 5.5: Validate generation speed, scroll behavior, and long-answer stability.
- [ ] Phase 5.6: Commit Phase 5 work slices separately.

## Phase 6 - Composer And Input UX

Status: not started.

- [ ] Phase 6.1: Rework composer toward Gallery input panel spacing, radius, and button hierarchy.
- [ ] Phase 6.2: Preserve keyboard-safe behavior so the input stays visible above the keyboard.
- [ ] Phase 6.3: Add proper disabled/loading/stop states while generation is active.
- [ ] Phase 6.4: Keep text-only composer until image chat input is implemented for real.
- [ ] Phase 6.5: Validate small-screen, rotated-screen, and long-input behavior.
- [ ] Phase 6.6: Commit Phase 6 work slices separately.

## Phase 7 - Chat Visual Tokens, Assets, And QA

Status: not started.

- [ ] Phase 7.1: Define chat-local colors based on Gallery neutral surfaces while preserving ChromaLab's scientific identity.
- [ ] Phase 7.2: Decide whether Nunito or any Gallery assets are needed; if copied, preserve Apache 2.0 license notes.
- [ ] Phase 7.3: Polish spacing, typography, icon sizes, touch targets, empty states, loading states, and error states.
- [ ] Phase 7.4: Run mobile visual QA on at least one real device or emulator.
- [ ] Phase 7.5: Update README/roadmap/pipeline docs if user-facing chat behavior changed.
- [ ] Phase 7.6: Commit Phase 7 work slices separately.

## Do Not Start Yet

These items are intentionally outside this 7-phase chat redesign unless the plan is
updated explicitly:

- Full app redesign outside the chat surface.
- New GGUF runtime architecture.
- Chromatogram analysis/calculation changes.
- Report-format implementation.
- Release packaging.
