# Model Download Pipeline

Date: 2026-05-13

## Phase 1 Status

Model downloads now use per-model jobs in the in-app model manager.

- Multiple model downloads can run at the same time.
- Each model row reads progress by `modelId`.
- Cancelling one model download does not cancel other downloads.
- The existing downloader still downloads each model file sequentially.
- Downloads still depend on the app process and current controller scope.

## Phase 2 Status

Single model files can now use HTTP range chunk downloading.

- The user can choose 1x, 2x, 4x, 8x, 10x, 12x, or 16x download parallelism in More settings.
- New downloads use the selected value; already running downloads keep the value they started with.
- Large files first probe HTTP range support with `Range: bytes=0-0`.
- If the server does not return partial content, the downloader falls back to the older sequential/resumable path.
- File validation still runs after each file, so a completed download is checked before the model is treated as available.

## Phase 3 Status

Downloads now run in a native Android foreground service.

- Download execution is owned by `ModelDownloadForegroundService`, not by a Compose UI scope.
- The service shows a persistent low-priority notification while downloads are active.
- UI controllers observe service state through a process-wide `StateFlow`.
- Active download requests are persisted with model metadata and selected parallelism.
- If the service/controller is recreated, pending requests are resumed from preferences.
- Cancelling still targets one model id and does not cancel other model downloads.

## Phase 4 Status

Model downloads now support a global user-configured speed limit.

- The user can choose unlimited mode or a total limit from 1 MB/s to 50 MB/s in More settings.
- The setting is persisted in model preferences.
- The foreground download service owns one shared limiter instance.
- Sequential downloads, parallel range chunks, and several simultaneous model downloads all use that shared limiter.
- Progress speed is measured after throttling, so the displayed speed reflects the effective limited throughput.
- Download validation and partial-download behavior are unchanged.

## Phase 5.1 Status

The shared model manager is now a storage/download/import screen.

- Built-in, Hugging Face, and imported model cards no longer show Activate actions.
- The model manager route no longer receives activation state or activation callbacks from app navigation.
- Existing download, cancel, delete, import, export, and compatibility displays remain available.
- Chat and chromatogram analysis still keep their older runtime activation paths until Phase 5.2+ adds explicit workflow model selection and loaders.

## Phase 5.2 Status

Model roles are now separated enough for chromatogram analysis to have its own model choice.

- The model manager can mark a downloaded compatible vision model as the chromatogram analysis model.
- The selection is persisted separately from `active_model_id`, so chromatogram analysis no longer changes the chat/global active model.
- Built-in model cards expose Chat/Chroma role indicators.
- Imported models carry separate text-chat and vision capability flags into the common UI.
- The chromatogram pipeline first tries the selected chromatogram model, and stops the neural stage if that selected model is missing or incompatible instead of silently replacing it.
- If no chromatogram model is selected, the pipeline still uses the existing ranked auto-pick behavior.

## Phase 5.3 Status

Chat model selection is now separate from immediate runtime loading.

- Choosing a model inside chat only saves that chat's selected model.
- Sending a message lazily loads the selected model if it is not already loaded.
- GGUF chat loading uses the base model text-only and does not load `mmproj`.
- LiteRT chat loading disables vision mode; chromatogram analysis still has its own vision loader.
- Leaving the chat route schedules the existing auto-unload timer; returning to chat cancels the pending timer.
- The model manager state reports an active model only when an engine is actually loaded.

## Phase 5.4 Status

Camera and chromatogram workflows now get a memory handoff before analysis.

- Entering capture, camera, file import, image processing, or analysis routes cancels pending auto-unload timers and prepares model memory explicitly.
- Loaded chat/text engines are unloaded before chromatogram work starts.
- A compatible already-loaded chromatogram vision model is kept for reuse if it matches the selected chromatogram model.
- The handoff never loads a neural model early; the processing pipeline still lazy-loads the selected VLM only when the neural stage starts.
- If inference is already running, the handoff does not unload the active engine mid-generation.

## Phase 5.5 Status

Chromatogram vision models are released when the report workflow no longer needs them.

- After image processing auto-saves the signal/report and navigates to calculation analysis, the active chromatogram VLM is unloaded.
- Leaving the calculation analysis screen also runs the same cleanup as a fallback.
- Cleanup only unloads a loaded image-capable chromatogram model; unrelated text/chat engines are ignored.
- If inference is still running, cleanup does not unload mid-generation and falls back to the configured auto-unload timer.

## Still Not In Current Download/Lifecycle Scope

- Persisted per-chunk completion maps for parallel range downloads after process death.
- Android 13+ runtime notification permission request before long downloads.
- Full idle no-load guarantees.

These are intentionally left for later phases so download throttling stays separate from model lifecycle changes.
