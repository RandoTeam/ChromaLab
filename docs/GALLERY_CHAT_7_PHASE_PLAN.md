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

- Current completed phase: Phase 7.2.
- Next phase to start: Phase 7.3.
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

Status: completed.

- [x] Phase 3.1: Define chat runtime UI state for backend, accelerator, thinking support, and model capability flags.
- [x] Phase 3.2: Add capability-gated accelerator controls for LiteRT/GGUF where runtime support is real.
- [x] Phase 3.3: Add thinking toggle only for models that actually support it.
- [x] Phase 3.4: Disable unsupported model/runtime choices with clear UI reasons instead of hiding them silently.
- [x] Phase 3.5: Keep selection separate from loading; loading still starts only on first message or explicit runtime action.
- [x] Phase 3.6: Validate that chromatogram model/runtime flows are not affected.
- [x] Phase 3.7: Commit Phase 3 work slices separately.

### Phase 3 Commit Record

- `bdd3ef1` - Phase 3.1: chat runtime capability state.
- `9db7533` - Phase 3.2: accelerator controls.
- `fedc45a` - Phase 3.3: thinking capability gating.
- `ac2f068` - Phase 3.4: unsupported model/runtime reasons.
- `c04b25e` - Phase 3.5: selection separated from loading.
- `9925298` - Phase 3.6: chromatogram runtime separation validation.

### Phase 3.6 Validation Notes

- Chat model picker selection stays inside `ChatController.setChatModel()` and updates only
  the selected chat session. It does not call the app-level model manager or load a model.
- Chat runtime loading still starts from `AndroidChatTextGenerator.generate()` through
  `ModelManagerController.activateForChat()` when a message is sent.
- Chromatogram navigation still calls `prepareForChromatogramWorkflow()` on capture,
  camera, file import, processing, and analysis routes, so an already loaded chat/text
  runtime is released before chromatogram work unless it is a reusable chromatogram VLM.
- Chromatogram VLM loading still goes through `ChartAnalysisReader.ensureModelLoaded()`
  and `ModelManagerController.activateForPipeline()`, which reads
  `manager.getChromatogramModelId()`, requires real image input, and loads GGUF with
  `mmproj` only for vision-capable packages.
- Phase 3 chat UI changes did not touch calculation, graph detection, crop, OCR, curve,
  or report-generation modules.

## Phase 4 - Message Layout And Telemetry

Status: completed.

- [x] Phase 4.1: Rework message rows toward Gallery's calmer chat hierarchy.
- [x] Phase 4.2: Use lighter assistant response presentation and preserve user/assistant alignment.
- [x] Phase 4.3: Keep token/time stats under completed assistant messages.
- [x] Phase 4.4: Extend stats with model name, backend, accelerator, duration, and tokens/sec where data is available.
- [x] Phase 4.5: Preserve readable scientific output, tables, and markdown-like report fragments.
- [x] Phase 4.6: Validate long messages and small-screen wrapping.
- [x] Phase 4.7: Commit Phase 4 work slices separately.

### Phase 4 Commit Record

- `2c0bbbc` - Phase 4.1: calmer message row layout.
- `b5dc65c` - Phase 4.2: lighter assistant messages.
- `e7e3943` - Phase 4.3: assistant telemetry under completed messages.
- `aba2fe0` - Phase 4.4-4.6: model/backend/accelerator telemetry,
  structured scientific output preservation, and build validation.

## Phase 5 - Streaming Text And Thinking Block

Status: completed.

- [x] Phase 5.1: Replace simple character reveal with buffered/fading streaming behavior inspired by Gallery.
- [x] Phase 5.2: Keep streaming markdown/table output readable without excessive reparse jank.
- [x] Phase 5.3: Add thinking block UI only when runtime emits thinking separately.
- [x] Phase 5.4: Auto-expand thinking while generation is active, then allow collapse/expand.
- [x] Phase 5.5: Validate generation speed, scroll behavior, and long-answer stability.
- [x] Phase 5.6: Commit Phase 5 work slices separately.

### Phase 5 Commit Record

- `a90236e` - Phase 5.1: buffered/fading chat streaming.
- `70877e8` - Phase 5.2: structured output streaming stability.
- `7ce34d8` - Phase 5.3: separate thinking block contract.
- `5a3a38f` - Phase 5.4: thinking block expand/collapse behavior.
- `c6e6df5` - Phase 5.5: throttled streaming scroll validation/fix.

### Phase 5.5 Validation Notes

- Streaming text rendering is buffered at 120 ms and crossfade is capped to short,
  non-structured answers, so long scientific/table output does not reanimate every
  chunk.
- Chat scrolling no longer starts a new animated scroll on every text delta.
  New bottom targets animate once; active generation uses a throttled bottom-anchor
  scroll every 250 ms to keep long answers visible without per-token animation churn.
- Thinking UI remains contract-only: it renders only when `thinkingContent` is
  populated separately by the runtime path.

## Phase 6 - Composer And Input UX

Status: completed.

- [x] Phase 6.1: Rework composer toward Gallery input panel spacing, radius, and button hierarchy.
- [x] Phase 6.2: Preserve keyboard-safe behavior so the input stays visible above the keyboard.
- [x] Phase 6.3: Add proper disabled/loading/stop states while generation is active.
- [x] Phase 6.4: Keep text-only composer until image chat input is implemented for real.
- [x] Phase 6.5: Validate small-screen, rotated-screen, and long-input behavior.
- [x] Phase 6.6: Commit Phase 6 work slices separately.

### Phase 6 Commit Record

- `02fe7a8` - Phase 6.1: Gallery-style composer panel.
- `3c42c7f` - Phase 6.2: keyboard-safe composer behavior.
- `9b2c848` - Phase 6.3: generation stop state.
- `544fe79` - Phase 6.4: text-only composer boundary.
- `7c9a18b` - Phase 6.5: responsive composer validation.
- Current Phase 6.6 documentation commit closes the phase and keeps the work-slice
  record separate from Phase 7 implementation.

### Phase 6.4 Validation Notes

- Chat messages remain text-only: `ChatMessage` stores `content` and optional
  `thinkingContent`, but no image attachment or media payload fields.
- Composer remains text-only: `ChatComposer` exposes text send and generation stop
  actions only, with no camera/gallery/media preview entry points.
- The Gallery media-preview contract is intentionally deferred until chat image
  input is supported end to end by message storage, runtime routing, and model
  capability gating.

### Phase 6.5 Validation Notes

- Small-width behavior is protected by the composer row structure: the text input
  uses `Modifier.weight(1f)`, the action button keeps a fixed `44.dp` touch target,
  and placeholder text is constrained to one ellipsized line.
- Rotated/keyboard behavior stays inside the native Compose inset path:
  `imePadding()` lifts the composer above the keyboard and
  `navigationBarsPadding()` preserves bottom safe area spacing.
- Long input is bounded by `BasicTextField(maxLines = 3)`, so pasted prompts can
  wrap without letting the composer consume the whole chat viewport.
- No media or image attachment UI was added in this validation phase; that remains
  blocked on real chat-image storage, model gating, and runtime routing.
- Real-device visual QA is still reserved for Phase 7.4, where the full chat
  polish pass can be checked on hardware.

## Phase 7 - Chat Visual Tokens, Assets, And QA

Status: in progress.

- [x] Phase 7.1: Define chat-local colors based on Gallery neutral surfaces while preserving ChromaLab's scientific identity.
- [x] Phase 7.2: Decide whether Nunito or any Gallery assets are needed; if copied, preserve Apache 2.0 license notes.
- [ ] Phase 7.3: Polish spacing, typography, icon sizes, touch targets, empty states, loading states, and error states.
- [ ] Phase 7.4: Run mobile visual QA on at least one real device or emulator.
- [ ] Phase 7.5: Update README/roadmap/pipeline docs if user-facing chat behavior changed.
- [ ] Phase 7.6: Commit Phase 7 work slices separately.

### Phase 7.1 Implementation Notes

- Chat colors now flow through local `ChatColorTokens` in `ChatScreen`, so Gallery-style
  neutral layers are scoped to chat and do not migrate the global ChromaLab palette.
- Chat background, composer panel, model chip, message rows, thinking block, stats
  badge, picker rows, disabled states, and stop/error surfaces use the local tokens.
- Scientific identity stays tied to the existing ChromaLab primary/accent color for
  send actions, selected model states, and active model indicators.

### Phase 7.2 Asset Decision

- Do not import Nunito in this phase. The chat should first be polished with the
  existing ChromaLab Material typography so the redesign does not create a
  separate, heavier font pipeline for one screen.
- Do not copy Gallery image/vector assets in this phase. Current chat controls use
  Compose Material icons and local shapes/colors, so there is no asset gap that
  justifies adding external files.
- No Apache 2.0 asset notice is required for Phase 7.2 because no Gallery source
  asset or font file was copied into the repository.
- If a later visual QA pass proves that a direct Gallery font or asset is necessary,
  add the copied file, its source path, and Apache 2.0 attribution in the same
  focused commit that introduces it.

## Do Not Start Yet

These items are intentionally outside this 7-phase chat redesign unless the plan is
updated explicitly:

- Full app redesign outside the chat surface.
- New GGUF runtime architecture.
- Chromatogram analysis/calculation changes.
- Report-format implementation.
- Release packaging.
