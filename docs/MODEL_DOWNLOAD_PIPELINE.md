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

## Not In Phase 2

- Foreground/background Android download service.
- Global speed limiter.
- Persistent restart after process death.

These are intentionally left for the next phases so chunked downloading does not change storage format or runtime loading.
