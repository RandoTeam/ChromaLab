# Phase 8C Model Availability and Android Fallback Research

Date: 2026-05-20

## Sources Reviewed

| Source | Status | Relevant decision |
| --- | --- | --- |
| Google AI Edge LiteRT for Android, `https://ai.google.dev/edge/litert/android` | Official, current. | LiteRT Android uses local model files and a runtime load step. ChromaLab should report model path/load status explicitly instead of treating load failure as generic graph failure. |
| Android app-specific storage, `https://developer.android.com/training/data-storage/app-specific` | Official, current. | App-private model files belong under internal/app-specific storage; uninstall removes these files. Validation docs must avoid data-destructive reinstall guidance and document package-specific model placement. |
| Android data storage overview, `https://developer.android.com/training/data-storage/` | Official, current. | Scoped storage remains the default. Validation exports should keep user-shareable artifacts separate from internal model/runtime state and avoid broad storage permissions. |

## Implementation Decisions

- Do not bundle large Gemma/GGUF model files in the repository.
- Missing LiteRT/GGUF assets are explicit model-availability diagnostics, not successful VLM evidence.
- Deterministic graphPanel, plotArea, axis, tick, calibration, trace, and peak stages must be attempted even when the VLM semantic layer is unavailable.
- Validation artifacts include sanitized model availability diagnostics without exposing raw prompts or private model internals.
