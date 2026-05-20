# Phase 8D Tick/OCR/Calibration Research Notes

Date: 2026-05-20

## Sources

| Source | URL | Quality | Decision |
| --- | --- | --- | --- |
| Google ML Kit Text Recognition v2 for Android | https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Official, current Google documentation | Used for the rule that OCR should run on local `InputImage` crops and return text; geometry remains deterministic. |
| ML Kit `TextRecognizer` reference | https://developers.google.com/android/reference/com/google/mlkit/vision/text/TextRecognizer | Official API reference | Used to confirm `TextRecognizer.process(InputImage)` is asynchronous text recognition, not geometry/calibration authority. |
| Android Perfetto documentation | https://developer.android.com/tools/perfetto | Official Android documentation | Used as the Phase 8D performance reference for bounded Android runtime profiling and stage timing evidence. |

## Applied Decisions

- Keep ML Kit as local OCR for tick-label crop text only.
- Do not allow OCR/VLM text to create tick pixel geometry.
- Add deterministic label-band projection rescue for tick candidate pixels: the image raster can suggest rows/columns where tick labels exist, but numeric values are still accepted only after pairing to deterministic tick candidates.
- Require graph-level failure evidence for terminal axis/tick/calibration failures so Android validation artifacts are diagnosable even when no final report graph is produced.
