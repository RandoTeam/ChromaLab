# Gallery Chat Phase 1 Contract

Status: phase 1 contract completed; phase 2 top bar/model picker shell implemented.

This document fixes the exact Gallery chat references that ChromaLab will use for the
chat redesign. The goal is to avoid approximate visual copying and make later phases
auditable: each visual or behavioral decision below maps to a concrete Gallery file,
token, asset, or component.

## Scope

Phase 1 defines the target chat surface only:

- top app bar and in-chat model chip;
- model picker sheet;
- runtime/settings controls for LiteRT chat models;
- message layout, streaming text, thinking block, and stats;
- composer layout and keyboard-safe behavior;
- colors, radius, spacing, typography, and available assets.

Phase 1 does not change ChromaLab UI code. Implementation starts in phase 2.

## Source Inventory

Local Gallery clone used for this audit:

- `C:/Users/Ilia/AppData/Local/Temp/google-ai-edge-gallery`

Primary source files:

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/ModelPageAppBar.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/ModelPickerChip.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/ModelPicker.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/ConfigDialog.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/ChatView.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/ChatPanel.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageInputText.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageBodyText.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageBodyThinking.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/BufferedFadingMarkdownText.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Config.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Model.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/ModelAllowlist.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Types.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/theme/Color.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/theme/Theme.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/theme/Type.kt`
- `Android/src/app/src/main/res/values/dimens.xml`

Useful assets found in Gallery:

- Fonts: `Android/src/app/src/main/res/font/nunito_*.ttf`.
- Task/decorative vectors: `chat_spark.xml`, `text_spark.xml`, `image_spark.xml`,
  `agent.xml`, `skill.xml`, `circle.xml`, `double_circle.xml`, `pantegon.xml`,
  `four_circle.xml`, `gemini_star.xml`.
- LLM chat task icons in code are mostly Material icons, not custom SVG:
  `Icons.Outlined.Forum`, `Icons.Outlined.Mms`, `Icons.Outlined.Mic`.

If any Gallery source or asset is copied directly later, preserve the Apache 2.0
license header and document the copied file.

## Code Anchors

Use these anchors when implementing later phases:

- Top app bar: `ModelPageAppBar.kt:93`, `ModelPageAppBar.kt:97`,
  `ModelPageAppBar.kt:119`, `ModelPageAppBar.kt:158`, `ModelPageAppBar.kt:174`.
- Model chip: `ModelPickerChip.kt:81`, `ModelPickerChip.kt:99`,
  `ModelPickerChip.kt:100`, `ModelPickerChip.kt:105`, `ModelPickerChip.kt:123`,
  `ModelPickerChip.kt:135`, `ModelPickerChip.kt:138`, `ModelPickerChip.kt:141`,
  `ModelPickerChip.kt:152`.
- Config controls: `ConfigDialog.kt:155`, `ConfigDialog.kt:158`,
  `ConfigDialog.kt:379`, `ConfigDialog.kt:470`, `ConfigDialog.kt:481`,
  `ConfigDialog.kt:493`, `ConfigDialog.kt:534`, `ConfigDialog.kt:564`,
  `ConfigDialog.kt:567`, `ConfigDialog.kt:587`.
- Chat panel: `ChatPanel.kt:332`, `ChatPanel.kt:354`, `ChatPanel.kt:360`,
  `ChatPanel.kt:385`, `ChatPanel.kt:516`.
- Streaming text: `BufferedFadingMarkdownText.kt:41`,
  `BufferedFadingMarkdownText.kt:54`, `BufferedFadingMarkdownText.kt:75`.
- Thinking block: `MessageBodyThinking.kt:50`, `MessageBodyThinking.kt:58`.
- Composer: `MessageInputText.kt:366`, `MessageInputText.kt:368`,
  `MessageInputText.kt:380`, `MessageInputText.kt:400`.
- Dimens: `res/values/dimens.xml:20`, `res/values/dimens.xml:21`.

## Gallery Pixel Contract

### Top App Bar

Gallery source: `ModelPageAppBar.kt`.

Required structure:

- `CenterAlignedTopAppBar`.
- Center title is a vertical `Column` with `4.dp` vertical spacing.
- First row: task icon `24.dp`, task label, `10.dp` horizontal spacing.
- Second row: model picker chip, centered under the task label.
- Back button disabled while model is initializing or response is in progress.
- Right actions: tune/settings and history. When both are visible, tune shifts left by `40.dp`.
- Settings icon size: `20.dp`.

ChromaLab target:

- Use the same two-level structure, but title text should be ChromaLab-specific:
  chat title on top, selected model chip below.
- Keep history/new-chat actions separate from model settings.
- Do not put dense model controls directly in the app bar.

### Model Chip

Gallery source: `ModelPickerChip.kt`.

Required visual details:

- Full-width host `Box`, content centered.
- Chip shape: `CircleShape`.
- Chip background: `MaterialTheme.colorScheme.surfaceContainerHigh`.
- Padding: start `8.dp`, end `2.dp`, vertical `4.dp`.
- Icon/status area: `21.dp`.
- Initializing overlay: `CircularProgressIndicator` `24.dp`, stroke `2.dp`, alpha `0.5`.
- Text style: `MaterialTheme.typography.labelLarge`.
- Model name max width: `screenWidthDp - 250.dp`.
- Overflow: `TextOverflow.MiddleEllipsis`.
- Dropdown icon: `Icons.Rounded.ArrowDropDown`, `20.dp`.
- Disabled alpha: `0.6`.
- Picker opens a `ModalBottomSheet` with `skipPartiallyExpanded = true`.

ChromaLab target:

- The chip must select a chat model, not activate/load it immediately.
- Chip must expose status: missing, downloaded, selected, loading, initialized, incompatible.
- Runtime/backend summary should be visible or discoverable: `LiteRT GPU`, `LiteRT CPU`,
  `GGUF Vulkan`, `GGUF CPU`, etc.

### Model Picker Sheet

Gallery source: `ModelPicker.kt`.

Required visual details:

- Sheet title row padding: horizontal `16.dp`, top `4.dp`, bottom `4.dp`.
- Title icon: `16.dp`.
- Model row padding: horizontal `16.dp`, vertical `8.dp`.
- Selected row background: `MaterialTheme.colorScheme.surfaceContainer`.
- Selected icon: `Icons.Filled.CheckCircle`, `16.dp`.
- Each model row shows name plus status/size/path.
- Low-memory selection is guarded by memory warning.

ChromaLab target:

- Filter options by capability before display:
  chat-capable, vision-capable, chromatography-capable, OCR-only, imported/custom.
- OCR-only or chromatography-only models must not be selectable for general chat.
- Unsupported accelerators must be disabled, not hidden without explanation.
- Selecting a model only binds it to the chat/session. Loading happens on first send.

### Runtime Config Dialog

Gallery source: `ConfigDialog.kt`, `Config.kt`, `ModelAllowlist.kt`.

Required controls:

- Dialog card radius: `16.dp`.
- Dialog content padding: `20.dp`.
- Internal vertical spacing: `16.dp`.
- Tabs when system prompt editing is available.
- Slider row has label, slider, and precise numeric input.
- Slider height: `24.dp`.
- Accelerator is a segmented control from model-supported accelerators.
- Bottom sheet selectors use `CircleShape`, height `40.dp`, border `1.dp`.
- Thinking appears as `BooleanSwitchConfig(ConfigKeys.ENABLE_THINKING)` only when the model
  capability allows it.

ChromaLab target:

- Per-chat settings remain in `ChatSettings`, not global model state.
- LiteRT first implementation must support at least CPU/GPU selection if the selected
  model declares both.
- NPU can only be shown when runtime/device support is real and validated.
- Thinking toggle must be capability-gated. No fake thinking UI for unsupported models.

### Messages

Gallery source: `ChatPanel.kt`, `MessageBodyText.kt`, `MessageSender.kt`,
`MessageBubbleShape.kt`, `dimens.xml`.

Required layout:

- Chat root uses `MaterialTheme.colorScheme.surface`.
- Main column uses `imePadding()`.
- Message list is vertically scrollable.
- Downward scroll clears text-field focus.
- Message row padding:
  - start: `16.dp + extraPaddingStart`;
  - end: `12.dp + extraPaddingEnd`;
  - top/bottom: `6.dp`.
- User alignment: end.
- Agent alignment: start.
- User bubble color: `MaterialTheme.customColors.userBubbleBgColor`.
- Agent bubble color: `MaterialTheme.customColors.agentBubbleBgColor`.
- Bubble radius from resource: `chat_bubble_corner_radius = 24dp`.
- Agent text responses do not use a heavy bubble by default.
- User text is white.
- Agent markdown response uses `BufferedFadingMarkdownText`.
- Latency/stat row appears below agent responses.

ChromaLab target:

- Keep ChromaLab scientific/minimal tone, but adopt Gallery's calmer chat hierarchy.
- Keep stats under each completed assistant message:
  prompt tokens, completion tokens, total tokens, duration, tokens/sec, backend, accelerator.
- Add generated model name and runtime to the sender label or stats row.
- Preserve current typewriter-like streaming, but move toward Gallery's buffered fade behavior.

### Streaming Text

Gallery source: `BufferedFadingMarkdownText.kt`.

Required behavior:

- New streamed markdown is rendered through a two-layer text buffer.
- Fade interval: `120 ms`.
- Uses crossfade between previous and new text to avoid abrupt large jumps.
- Overlay is hidden after generation completes.

ChromaLab target:

- For phase 5, replace the current simple character reveal with a buffered streaming
  renderer that remains readable for markdown/code/table output.
- Avoid re-parsing large markdown blocks on every tiny token if it causes jank.

### Thinking Block

Gallery source: `MessageBodyThinking.kt`, `LlmChatViewModel.kt`.

Required visual details:

- Container padding: horizontal `12.dp`, vertical `8.dp`.
- Header row is clickable and uses `4.dp` spacing.
- Auto-expanded while thinking is in progress.
- Collapses/expands with `expandVertically()` / `shrinkVertically()`.
- Body has a left vertical line with `2.dp` stroke.
- Body padding: top `8.dp`, bottom `4.dp`, start `8.dp`, inner start `12.dp`.
- Thinking text color: `MaterialTheme.colorScheme.onSurfaceVariant`.

ChromaLab target:

- Add only after runtime can emit thinking separately.
- If LiteRT returns thinking via extra context, keep it as a separate message type.
- If a model does not support thinking, the toggle and block must not appear.

### Composer

Gallery source: `MessageInputText.kt`.

Required visual details:

- Composer min height: `76.dp`.
- Outer horizontal padding: `12.dp`.
- Outer vertical padding: `8.dp`.
- Input panel border: `1.dp`, `outlineVariant`, radius `16.dp`.
- Text input max lines: `3`.
- Add button: `OutlinedIconButton`.
- Send button: filled `IconButton`, container color follows task/accent color.
- Stop button replaces send while generation is in progress.
- Picked media preview uses `80.dp` height, `8.dp` radius, `1.dp` outline.

ChromaLab target:

- Keep simple text-only composer first.
- Preserve fixed keyboard-safe behavior already added with `imePadding()` and
  `navigationBarsPadding()`.
- Add attachment entry points only after chat image input is enabled.

## Gallery Colors To Capture

Gallery light color tokens:

- `primary = 0xFF0B57D0`
- `primaryContainer = 0xFFD3E3FD`
- `secondaryContainer = 0xFFC2E7FF`
- `background = 0xFFFFFFFF`
- `surface = 0xFFFFFFFF`
- `surfaceContainerLow = 0xFFF8FAFD`
- `surfaceContainer = 0xFFF0F4F9`
- `surfaceContainerHigh = 0xFFE9EEF6`
- `surfaceContainerHighest = 0xFFDDE3EA`
- `outline = 0xFF747775`
- `outlineVariant = 0xFFC4C7C5`
- `agentBubbleBgColor = 0xFFE9EEF6`
- `userBubbleBgColor = 0xFF32628D`
- `linkColor = 0xFF32628D`

Gallery dark color tokens:

- `primary = 0xFFA8C7FA`
- `primaryContainer = 0xFF0842A0`
- `background = 0xFF131314`
- `surface = 0xFF131314`
- `surfaceContainerLow = 0xFF1B1B1B`
- `surfaceContainer = 0xFF1E1F20`
- `surfaceContainerHigh = 0xFF282A2C`
- `surfaceContainerHighest = 0xFF333537`
- `outline = 0xFF8E918F`
- `outlineVariant = 0xFF444746`
- `agentBubbleBgColor = 0xFF1B1C1D`
- `userBubbleBgColor = 0xFF1F3760`
- `linkColor = 0xFF9DCAFC`

ChromaLab integration rule:

- Do not replace the global ChromaLab palette in the chat phase.
- Define chat-local tokens that map Gallery's neutral chat surfaces onto ChromaLab's
  existing teal scientific theme.
- A later full-app redesign may migrate broader app colors after the chat flow is stable.

## Typography

Gallery source: `Type.kt`.

Gallery uses Nunito for Material typography:

- `nunito_regular`
- `nunito_medium`
- `nunito_semibold`
- `nunito_bold`
- `nunito_extrabold`
- `nunito_black`
- `nunito_light`
- `nunito_extralight`

ChromaLab target:

- Do not import the font automatically in phase 2.
- First make layout match Gallery using current ChromaLab typography.
- If Gallery's typography feel is still required in phase 5, import Nunito with license
  tracking and apply it through chat-specific typography tokens.

## ChromaLab Gap Map

Current ChromaLab files:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/chat/ChatScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/chat/ChatModels.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/chat/ChatController.kt`
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/chat/ChatPlatform.android.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/core/ui/theme/*`

Main gaps:

- Top bar currently has title plus separate chip/settings icons, but not Gallery's centered
  two-line hierarchy.
- Model selection currently lives inside settings sheet, not as the primary model chip.
- Runtime selector is not first-class in chat UI.
- Thinking mode is not modeled as a capability-gated chat setting/message.
- Message bubbles are `8.dp`; Gallery chat uses `24.dp` bubble radius and lighter agent text.
- Stats exist, but need runtime/backend/accelerator clarity.
- Current streaming is character reveal; Gallery uses buffered fade for markdown.

## Phase 2 Entry Criteria

Phase 2 may start only after this contract is committed.

Phase 2 should implement only:

- Gallery-style top app bar structure;
- model chip shell;
- model picker sheet shell;
- no runtime loading behavior changes beyond selecting a chat model.

Do not mix phase 2 with thinking mode, runtime backend work, composer redesign, or full
palette migration.

## Phase 2 Implementation Status

Status: completed.

Implemented in:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/chat/ChatScreen.kt`

Completed scope:

- Gallery-style `CenterAlignedTopAppBar` structure for chat sessions.
- Centered chat title plus in-chat model chip.
- Separate model picker `ModalBottomSheet` opened from the chip.
- Model selection binds the chosen model to the current chat session only.
- Settings sheet now contains generation settings only, not model selection.

Explicitly not changed in phase 2:

- Model loading/runtime behavior.
- LiteRT/GGUF accelerator selection.
- Thinking mode.
- Composer redesign.
- Message bubble redesign.
- Full chat palette migration.
