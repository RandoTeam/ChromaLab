# ChromaLab Chromatogram Pipeline Full Trace Report

Date: 2026-05-18

Scope: factual full-trace review of the current chromatogram photo/screenshot pipeline, from capture or image input to final calculation report and export.

## Full Trace По Текущему Pipeline

Основано на текущем коде и документации проекта, без предположений "как должно быть". Основные файлы:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/flow/ProcessingFlowScreen.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/sweep/AutoSweepEngine.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/calculation/core/CalculationEngine.kt`
- `docs/CHROMATOGRAM_END_TO_END_PIPELINE_REVIEW.md`

```text
Smart Scan / image input
 -> EXIF + orientation normalize
 -> quality check
 -> crop/perspective stage currently no-op in runtime
 -> VLM graph-region hint
 -> deterministic CV graph region detection
 -> graph boundary correction
 -> VLM + ML Kit axis OCR
 -> deterministic axis line detection
 -> X/Y pixel-to-unit calibration
 -> preprocessing sweep
 -> curve mask
 -> curve extraction
 -> signal conversion
 -> signal smoothing
 -> Room save
 -> CalculationEngine
 -> distribution/pattern/method/geochemical analysis
 -> structured report
 -> export/share
```

| Этап | Метод / библиотека | Вход | Выход | Проверка качества / ошибки | Детерм. | Проблемы |
|---|---|---|---|---|---|---|
| 1. Capture / input | Android ML Kit Document Scanner через GMS; CameraX есть как manual/fallback код | Камера или галерея внутри Smart Scan | JPEG в app storage | Если Smart Scan недоступен, release-quality путь блокируется | Частично | Не сохраняются crop/filter/deskew metadata ML Kit |
| 2. Normalize | `ExifInterface`, `BitmapFactory`, Android `Matrix` | JPEG | `normalized.jpg`, размеры, EXIF rotation | Ошибка при decode/нулевых размерах | Да | Нет полной provenance записи исходных размеров/пути |
| 3. Orientation | Custom Kotlin: long horizontal/vertical dark runs | Normalized image | 0 или -90 градусов | Логи `PIPELINE[ORIENTATION]`; warnings при невозможности sample | Да | Работает только для грубого right-angle, не полноценный deskew |
| 4. Image Quality | Custom Kotlin: Laplacian blur, brightness, contrast, glare, shadow, skew | Image path | `ImageQualityReport` | Stage score/warnings | Да | Сейчас скорее диагностический, не управляет rectification |
| 5. Crop Review | Сейчас `fallbackCropResult` | Normalized image | Full-image crop | Почти нет реальной проверки | Да, но no-op | P0: runtime не делает свой crop, полагается на ML Kit |
| 6. Perspective | Сейчас `fallbackPerspectiveResult` в runtime | Current image | Identity homography | `isExcessiveWarp=false` всегда | Да, но no-op | P0: может скрыть невыправленную перспективу |
| 7. VLM Graph Hint | LiteRT/GGUF VLM через `InferenceEngine`, JSON prompt | Full image | `GraphBounds` percent + graph count | Strict: если VLM обязателен и не дал bounds, abort | Нет | Модель может вернуть мусор/пусто; timeout до 300s |
| 8. CV Graph Detection | Custom Kotlin: Sobel/projections, contours, density, margin fallback | Image + dimensions | `GraphRegionResult` regions | Confidence HIGH/MED/LOW/MANUAL, warnings | Да | Может брать неполный ROI или слишком широкий panel |
| 9. Boundary Correction | `GraphRegionBoundaryCorrector`, sampler/refiner | Selected region | Corrected region | Warnings, preserve panel labels mode | Да | Еще нет идеального plot-bound detector для плохих фото |
| 10. Multi-graph split | CV regions + VLM count/order | One image/page | Per-graph loop | Top-to-bottom region order, snapshots | Да/нет | Если initial regions плохие, все downstream плохо |
| 11. Axis OCR | VLM JSON first, retry, merge; ML Kit Text Recognition supplement | Image + graph region | X/Y tick values, units, text boxes | Requires both X/Y in strict mode; logs OCR values | Нет + да | OCR/VLM иногда читают значения без надежных tick positions |
| 12. Axis Detection | Custom Sobel projections inside ROI | Image + ROI | X axis, Y axis, origin | Confidence 0.9/0.5/0.1 | Да | Сильнейшая линия может быть grid/frame/peak, не axis |
| 13. Pixel Calibration | First/last OCR anchors or axis endpoints | OCR values + axes + ROI | `PixelCalibration` | Requires >=2 X and >=2 Y values; invalid scale blocks | Да | Нет robust fit по всем tick anchors и residual thresholds |
| 14. Preprocessing Sweep | 14 configs: CLAHE-like contrast, adaptive threshold, sharpen, binary, scan style | Image | Variant images + scores | Score graph/axis/OCR/curve | Да | Полезно, но зависит от уже выбранного ROI/calibration |
| 15. Curve Mask | Custom Canny, adaptive threshold, axis/grid/text/blob suppression, largest component | Preprocessed ROI | raw/clean mask PNG | Pixel counts, suppressions, warnings | Да | Largest component может выбросить слабые/фрагментированные участки |
| 16. Curve Extraction | Skeletonization, per-column clusters, interpolation, baseline fill, audit overlays | Mask | `CurveExtractionResult` points | Coverage, interpolation, centerline parity overlays | Да | Для hard photos часть centerline кандидатов audit-only |
| 17. Signal Conversion | Linear pixel->time/intensity transform | Curve points + calibration | `DigitalSignal` | Dedup time, gap count, sort validity | Да | Если calibration неверна, сигнал численно стабилен, но научно неверен |
| 18. Signal Smoothing | Savitzky-Golay | DigitalSignal | `SmoothedSignal` | Point count/gaps/confidence | Да | Может сгладить пики; это фиксируется в расчетах |
| 19. Auto-save | Room DB + JSON serialization | Signal + metadata | `ChromatogramEntity` | `PIPELINE[REPORT_AUDIT]`, stage timings | Да | Time unit сейчас не полностью first-class в entity |
| 20. CalculationEngine | Pure Kotlin deterministic core | Saved signal | `CalculationRun` peaks/signals/warnings | Validation, noise, confidence, overlap | Да | Сильная часть, но зависит от upstream geometry |
| 21. Peak math | SG, ALS/SNIP/manual baseline, noise, peak detection, boundary, trapezoidal integration | Signal points | RT, height, area, FWHM, S/N, area %, confidence | Warnings per stage | Да | Не исправляет неверную оцифровку |
| 22. Domain analytics | Distribution, pattern, method quality, geochemical/Kovats if series detected | Peaks + signal | Interpretation blocks | Missing-data notes | Да | Kovats не считается без reference series |
| 23. Report | Report mapper, validator, structured UI, Markdown/HTML exporters | CalculationRun + metadata | Professional report contract | Validator finds missing sections/peaks/axis data | Да | UI может выглядеть красиво, но source metadata еще неполная |
| 24. Export/share | CSV/JSON/HTML/Markdown/UI contract via FileSharer | Report/export data | Saved/shared files | Android MediaStore/FileProvider | Да | Native PDF пока фактически HTML/PDF-ready, не полноценный PDF renderer |

## Ключевые Узкие Места

1. Самый критичный разрыв: runtime Android сейчас пропускает собственный crop/perspective и доверяет ML Kit Smart Scan. На GMS-poor устройстве или прямом файле это ломает весь pipeline.

2. VLM используется слишком рано в geometry chain. Он должен помогать распознать, где график/подписи/ION, но не должен быть источником пиксельной геометрии.

3. ROI пока смешивает два разных понятия: полный graph panel с подписями и plot area для чисел. Для точности нужны оба прямоугольника отдельно.

4. Calibration слабая: берутся первые/последние anchors или axis endpoints. Нужен multi-anchor fit по всем tick marks с residual error, rejection outliers и блокировкой при плохой геометрии.

5. OCR должен читать значения из маленьких rectified crops около уже найденных tick marks. Сейчас OCR/VLM может давать значения и позиции вместе, что снижает проверяемость.

6. Curve extraction уже имеет серьезный audit слой, но hard-photo centerline кандидаты местами остаются diagnostic-only. Значит численный отчет нельзя считать стабильным на плохих фото без дополнительной геометрии/trace extraction.

7. Report infrastructure уже есть, но полнота отчета зависит от того, насколько processing metadata заполнена. Сейчас часть source/provenance/calibration evidence неполная.

## Что Усиливать Первым

1. Сделать общий deterministic CV geometry core для Android/Desktop: OpenCV/BoofCV adapter или строго общий contract поверх текущего Kotlin CV.

2. Реально включить page/plot rectification: document/page quadrilateral, plot-frame quadrilateral, homography, residual straightness/skew metrics.

3. Разделить `graphPanelBounds` и `plotAreaBounds`: panel хранит title/ION/labels, plot area идет в calibration/curve.

4. Добавить tick geometry до OCR: CV находит tick pixel positions, OCR/VLM только читает text value.

5. Переписать calibration на robust linear regression по всем anchors: residuals, confidence, outlier rejection, nonlinearity score.

6. VLM использовать как judge/assistant: проверить "виден ли полный график", прочитать ION/title/axis labels, сравнить OCR candidates, объяснить warnings. Не использовать VLM для area/height/RT/FWHM.

7. Сохранять полный trace: original/normalized/warped image, ROI candidates, masks, overlays, calibration anchors, rejected peaks, timings, model/runtime.

## Итог

Расчетный движок уже технически сильный и детерминированный. Главная проблема не в `CalculationEngine`, а в участке `фото -> ROI -> axes/ticks -> calibration -> curve`. Пока эта часть не станет строго геометрической и проверяемой, финальный отчет может быть красивым, но получать неверные числа из неверно оцифрованного сигнала.

## Дополнительный Технический Review

Этот раздел фиксирует архитектурный вывод по текущему состоянию проекта как основу для следующего перепроектирования pipeline. Он не заменяет full trace выше, а дополняет его выводами по рискам.

### 1. Текущий Android Runtime Path

Нормальный Android-вход сейчас начинается с ML Kit Document Scanner. Это хороший UX для пользователя: камера, импорт из галереи, auto crop, deskew и фильтры выполняются до передачи изображения в ChromaLab. Но это внешний provider подготовки, а не доказательство, что геометрия действительно пригодна для научного расчета.

Критический момент: `ProcessingFlowScreen` далее создает `fallbackCropResult` и `fallbackPerspectiveResult`. То есть внутренний runtime pipeline сообщает, что crop и perspective stage завершены, но фактически не выполняет собственное кадрирование и перспективную коррекцию. Это приемлемо только если вход пришел из Smart Scan и мы явно сохраняем provenance Smart Scan. Для прямого файла, GMS-poor устройства или будущей desktop/mobile общей логики это P0-разрыв.

### 2. Правильное Разделение Ответственности

Детерминированная CV-часть должна владеть:

- границами страницы;
- границами graph panel;
- границами plot area;
- линиями осей;
- tick pixel positions;
- homography/perspective correction;
- trace/curve centerline;
- pixel-to-unit transform;
- peak detection, integration и numeric metrics.

VLM/LLM может помогать только там, где требуется семантика:

- понять, что на изображении хроматограмма;
- найти rough graph-region hint;
- прочитать title, Ion/m/z, sample name, axis captions;
- помочь OCR при сложных подписях;
- дать human-readable explanation warnings;
- помочь с химическим контекстом при наличии локальной базы.

VLM не должен создавать или исправлять численные значения пиков: RT, height, area, FWHM, baseline, S/N, Kovats.

### 3. Главный Архитектурный Дефект Сейчас

Pipeline частично смешивает три разных слоя:

1. `graph panel` - весь блок графика вместе с title, ION, подписями осей и tick labels.
2. `plot area` - только прямоугольник координатной области, где находятся линия сигнала и пиксельная шкала.
3. `signal trace` - собственно кривая/линия хроматограммы внутри plot area.

Для точного расчета эти сущности нельзя заменять друг другом. Если ROI выбран как слишком узкий plot area, теряются подписи и tick labels. Если ROI выбран как слишком широкий panel/page crop, curve extraction может захватить текст, рамки, соседние графики или фоновые артефакты.

### 4. Axis OCR И Calibration

Текущий подход уже честно блокирует отчет, если не удалось получить X/Y calibration. Это правильно. Но способ получения calibration пока недостаточно научный:

- OCR/VLM может давать значения без надежных tick pixel positions.
- X/Y calibration часто строится по двум крайним значениям.
- Нет полноценного robust fit по всем найденным tick anchors.
- Нет обязательных residual thresholds.
- Нет отдельной оценки нелинейности, skew и перспективного остатка.

Целевой вариант:

```text
CV detects axis lines
 -> CV detects tick pixel positions
 -> OCR/VLM reads nearby tick label values
 -> matcher pairs tick positions with values
 -> robust linear regression fits axis transform
 -> residuals decide accept/block
```

### 5. Curve Extraction

Текущий `CurveMaskPreparer` и `CurveExtractor` уже делают много правильных вещей: Canny, adaptive threshold, suppression axes/grid/text blobs, largest component, skeletonization, centerline audit overlays, interpolation and coverage checks.

Оставшийся риск: hard-photo cases могут быть фрагментированы. Если largest component или top-envelope logic выбрасывает часть сигнала, downstream `CalculationEngine` посчитает красивую, но неполную кривую. Поэтому до принятия scientific report надо сохранять и проверять:

- raw mask;
- clean mask;
- rejected components;
- selected component;
- centerline candidate;
- branch-pruned/trunk-path/fragment reconstruction candidates;
- overlay с причиной выбора или отказа.

### 6. Calculation Engine

`CalculationEngine` сейчас является самой надежной частью pipeline. Он выполняет:

- input validation;
- optional Savitzky-Golay smoothing;
- baseline estimation: NONE, manual linear, ALS, SNIP;
- baseline correction;
- noise estimation;
- peak detection;
- boundary detection;
- overlap classification;
- trapezoidal or interpolated trapezoidal integration;
- clamp negative handling;
- max width filtering;
- peak metrics;
- confidence and warnings.

Слабое место не в математике calculation core, а в том, что он принимает уже оцифрованный сигнал. Если сигнал построен из неверного ROI, неверной оси или неверной кривой, calculation core выдаст воспроизводимый, но научно неверный результат.

### 7. Report Layer

Структурный report layer уже есть:

- `CalculationRunReportMapper`;
- `ReportContractValidator`;
- `StructuredReportPreview`;
- `ReportMarkdownRenderer`;
- `ReportHtmlRenderer`;
- export/share layer.

Проблема не в наличии renderer, а в полноте upstream evidence. Чтобы отчет соответствовал референсу и был научным, он должен получать не только peaks table, но и полный trace:

- источник изображения;
- модель и runtime;
- время анализа;
- этапы и timings;
- graph bounds;
- plot bounds;
- axis calibration anchors;
- OCR confidence;
- preprocessing variants;
- curve extraction artifacts;
- baseline/noise/peak calculation params;
- warning provenance.

### 8. Рекомендованный Next Architecture

Целевой порядок должен быть таким:

```text
image input
 -> source provenance
 -> decode / EXIF / orientation
 -> document/page quadrilateral
 -> perspective correction / homography
 -> graph panel detection and split
 -> plot area detection
 -> axis line detection
 -> tick geometry detection
 -> OCR/VLM tick value reading
 -> robust pixel-to-unit calibration
 -> preprocessing variants
 -> artifact masks
 -> curve extraction candidates
 -> selected calibrated signal
 -> deterministic calculation
 -> report contract validation
 -> final professional report
```

### 9. Что Нельзя Делать

- Нельзя продолжать full scientific report после неудачного VLM/OCR/calibration как deterministic-only approximation.
- Нельзя подменять perspective correction identity/no-op stage без явной пометки.
- Нельзя использовать VLM output как источник пиксельных координат для расчета.
- Нельзя принимать отчет, где нет axis calibration evidence.
- Нельзя скрывать низкую уверенность красивым UI.

### 10. Практический Вывод

Проект уже имеет правильную основу: строгий расчетный движок, structured report contract, audit metadata и fixture bench. Но чтобы получить "докторский уровень" по фото/скриншотам, следующий главный фокус должен быть не на промптах модели, а на deterministic CV geometry core и полном evidence trace.

Иначе любая модель, даже сильная, будет только маскировать проблемы ROI, perspective, tick detection и calibration.
