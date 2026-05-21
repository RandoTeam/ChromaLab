# Phase 9E Graph Layout and Tick Localization Research

Date: 2026-05-21

## Sources

| Source | URL | Phase 9E relevance | Decision |
| --- | --- | --- | --- |
| ML Kit `TextRecognizer` reference | https://developers.google.com/android/reference/com/google/mlkit/vision/text/TextRecognizer | ML Kit OCR returns recognized text from supplied `InputImage` inputs and can report model unavailability. Phase 9E keeps OCR as text-only evidence from local crops; it is not a source of tick pixel coordinates. | Use for local crop OCR behavior and unavailable-model diagnostics. |
| Android app data and file storage overview | https://developer.android.com/training/data-storage | Android scoped storage and app-specific/shared storage constraints matter for validation artifacts and user report privacy. | Keep diagnostic artifacts under validation/export locations and keep user reports separate from diagnostic bundles. |
| Android app-specific files | https://developer.android.google.cn/training/data-storage/app-specific?hl=en | Documents app-specific storage lifecycle and Android 10+ scoped access behavior. | Do not rely on private app paths as user-facing export evidence; copy or mark missing explicitly. |
| LiteRT for Android | https://ai.google.dev/edge/litert/android | Confirms LiteRT Android runtime and local model packaging/discovery path remain current. | Keep Gemma/LiteRT advisory layer local and never a geometry/calibration authority. |
| Android Perfetto tool docs | https://developer.android.com/tools/perfetto | Perfetto can collect Android performance information via ADB. | Keep Phase 9E stage timing summaries and leave deeper trace capture as follow-up unless runtime spikes persist. |

## Implementation Notes

- Deterministic geometry must own graph layout, axis, tick pixel positions, calibration anchors, and residual-backed fit status.
- ML Kit/VLM may read text from prepared crop regions but cannot create tick pixels or calibration coordinates.
- Android validation exports must distinguish app-private diagnostic paths from pullable public artifacts; missing overlays now need explicit reasons.
