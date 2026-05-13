# Model Download Plan

Date: 2026-05-13

This document is the canonical five-phase plan for model downloads and model loading.
The phase order follows the user's original numbered requirements, not the accidental implementation order.

## Why The Order Was Broken

I incorrectly treated "allow several model downloads at once" as Phase 1 and "parallel chunks for one file" as Phase 2.
That was a planning mistake: in the user's list, the first requirement was single-file download acceleration with 2/4/8/10/12/16 parallel chunks, and the second requirement was downloading several models at once.

No code rollback is needed because both original Phase 1 and original Phase 2 are now implemented, but their commit order is reversed:

- Original Phase 2 was implemented first in commit `109fda7` (`Support multiple model downloads`).
- Original Phase 1 was implemented second in commit `fae241c` (`Add parallel chunk model downloads`).

From this point, the project should follow the phase order below.

## Phase 1 - Accelerate One Model File Download

Goal: make one model file download faster by using HTTP range chunks when the server supports it.

Status: completed, needs real-device download verification.

Subpoints:

- Done: support selectable parallelism values `1`, `2`, `4`, `8`, `10`, `12`, `16`.
- Done: store download parallelism in model preferences.
- Done: expose download parallelism in settings.
- Done: apply selected parallelism to new downloads.
- Done: probe server support with `Range: bytes=0-0`.
- Done: use multi-range chunk download for large files when HTTP `206 Partial Content` is supported.
- Done: fall back to the previous sequential/resumable downloader when range requests are unsupported.
- Done: keep final file validation after download completion.
- Not done: measure real speed and stability on device for 1x/4x/8x/16x.
- Not done: tune default value after device testing.

Implementation reference:

- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/model/ModelDownloader.kt`
- `composeApp/src/androidMain/kotlin/com/chromalab/feature/processing/model/ModelManager.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/settings/MoreScreen.kt`

## Phase 2 - Download Several Models At Once

Goal: pressing download on another model must start another independent download instead of doing nothing.

Status: completed, needs real-device concurrent download verification.

Subpoints:

- Done: replace single global download job with per-model jobs.
- Done: allow several active download jobs at the same time.
- Done: track progress by `modelId`.
- Done: show per-model progress in built-in model cards.
- Done: show per-model progress in Hugging Face search results.
- Done: cancelling one model download does not cancel other model downloads.
- Done: deleting a model cancels only that model's active download.
- Partial: global legacy progress fields still exist for compatibility.
- Partial: per-model failure state exists in data shape, but user-facing per-row error handling is still minimal.
- Not done: verify multiple simultaneous downloads on a real device with large LiteRT and GGUF files.

Implementation reference:

- `composeApp/src/androidMain/kotlin/com/chromalab/feature/settings/ModelManagerController.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/settings/ModelManagerState.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/settings/ModelManagerScreen.kt`

## Phase 3 - Background Downloads

Goal: downloads must continue when the user leaves the screen or minimizes the app.

Status: not started.

Subpoints:

- Not done: move download execution out of UI/composition scope.
- Not done: implement Android foreground download service with a visible notification for long model downloads.
- Not done: keep downloads running after navigating to another screen.
- Not done: keep downloads running when the app is minimized, within Android foreground service limits.
- Not done: reconnect UI state to active service downloads after returning to the app.
- Not done: persist enough download metadata to recover status after process recreation.
- Not done: preserve cancellation by model id from the UI.
- Not done: document behavior when Android kills the app process.

Technical rule:

- Use native Android foreground-service behavior for long downloads. Do not fake background work through hidden UI scopes.

## Phase 4 - Download Speed Limit

Goal: allow the user to limit total download speed, from about 1 MB/s to 50 MB/s, plus unlimited mode.

Status: not started.

Subpoints:

- Not done: add persisted speed-limit setting.
- Not done: add UI control for `1..50 MB/s` and unlimited.
- Not done: apply speed limiting to sequential downloads.
- Not done: apply speed limiting across parallel range chunks for one file.
- Not done: apply speed limiting across several simultaneous model downloads.
- Not done: make progress speed display reflect throttled effective speed.
- Not done: verify that limiting does not corrupt partial downloads or validation.

Technical rule:

- The limiter must throttle bytes, not weaken validation or skip download integrity checks.

## Phase 5 - Model Loading, Roles, And Lifecycle

Goal: downloaded models are stored on disk, but not loaded into memory until the active workflow needs them.

Status: not started for the new lifecycle contract; existing model activation logic remains older behavior.

Subpoints:

- Not done: remove the Activation action from the model download/settings section.
- Not done: make the model manager a download/import/storage screen, not a memory-loading screen.
- Not done: separate chat-capable models from chromatogram/vision analysis models.
- Not done: add dedicated "use for chromatograms" selection.
- Not done: load the selected chat model only when chat needs inference.
- Not done: allow model selection inside chat without forcing a global always-loaded model.
- Not done: unload chat model after leaving chat according to an auto-unload timer.
- Not done: unload chat model when entering camera/chromatogram analysis if memory is needed.
- Not done: load the selected chromatogram model only when image/photo analysis reaches the neural stage.
- Not done: unload the chromatogram model after the analysis report is produced.
- Not done: ensure no background model loading happens while the app is idle.
- Not done: keep local import/export model flows intact.

Technical rule:

- Model lifecycle changes must not weaken chromatogram analysis quality. If a required vision model cannot load, the analysis must stop with a clear error rather than producing a deterministic-only report.

## Current Next Phase

The next phase to work on is Phase 3 - Background Downloads.

Before starting Phase 3, verify Phase 1 and Phase 2 on a real device:

- Start two model downloads at the same time.
- Start one large download with 4x or 8x parallelism.
- Cancel only one active download and confirm the other continues.
- Confirm finished files pass validation and appear as downloaded models.
