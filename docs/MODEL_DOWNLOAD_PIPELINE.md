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

## Not In Phase 4

- Persisted per-chunk completion maps for parallel range downloads after process death.
- Android 13+ runtime notification permission request before long downloads.
- Full model memory lifecycle changes, role separation, and per-workflow model loading.

These are intentionally left for later phases so download throttling stays separate from model lifecycle changes.
