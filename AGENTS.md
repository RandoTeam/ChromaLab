# ChromaLab Agent Rules

These rules are project-specific and override generic speed/shortcut preferences.

## Commit Discipline

- Make a focused git commit after every completed work slice, even if the slice is small.
- Keep commits scoped: stage only files that belong to the completed slice.
- Do not include unrelated dirty files in a commit. If unrelated changes already exist, leave them alone and mention them in the final status.
- Run the most relevant available validation before committing. If validation cannot be run, state why.
- Use concise commit messages that describe the product change, not the implementation trivia.

## Analysis Quality Bar

- ChromaLab is a serious chromatogram analysis tool, not a demo. Prefer correct, honest, deeply validated results over faster but weaker results.
- Do not silently reduce analysis depth to support weaker devices. If a device cannot complete the required neural vision stage, stop with a clear error instead of continuing with a deterministic-only approximation.
- Do not produce or save a final chromatogram report from partial/low-confidence AI results unless the user explicitly chooses a diagnostic/manual mode.
- It is acceptable for a full analysis to take 10-15 minutes on weak hardware if that preserves correctness.
- Calculation stages must stay explicit and auditable: image normalization, graph detection, axis/ION extraction, calibration, curve extraction, peak integration, and report generation should each expose enough state/logging to diagnose failures.
- When changing prompts, parsers, model settings, memory limits, or fallbacks, treat analysis correctness as the primary requirement.

## Model And Pipeline Separation

- Keep model use cases separate: chromatography/vision analysis models, general chat models, and imported/custom models.
- Chromatogram photo analysis requires a working vision model with image input. GGUF analysis requires the base model plus the correct mmproj.
- LiteRT may be the reference stable engine for quality work, but GGUF/VLM support must be tested against the same expected analysis behavior.
- Device optimization must not change scientific meaning. Tune context, batch, threading, and loading strategy before weakening prompts or skipping stages.

## Documentation

- Update project documentation when behavior, user flows, pipeline stages, model support, or release scope changes.
- Keep README user-facing and polished.
- Keep pipeline/roadmap docs current enough that another agent or developer can understand what is done, what is experimental, and what is next.
- Prefer adding short focused docs over burying important project state only in chat history.
