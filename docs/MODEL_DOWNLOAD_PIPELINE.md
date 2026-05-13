# Model Download Pipeline

Date: 2026-05-13

## Phase 1 Status

Model downloads now use per-model jobs in the in-app model manager.

- Multiple model downloads can run at the same time.
- Each model row reads progress by `modelId`.
- Cancelling one model download does not cancel other downloads.
- The existing downloader still downloads each model file sequentially.
- Downloads still depend on the app process and current controller scope.

## Not In Phase 1

- Foreground/background Android download service.
- Parallel HTTP range chunks inside one file.
- Global speed limiter.
- Persistent restart after process death.

These are intentionally left for the next phases so the first change only removes the single-download bottleneck without changing storage format or runtime loading.
