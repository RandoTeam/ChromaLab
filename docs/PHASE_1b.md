# ChromaLab — Фаза 1b: Интеграция и стыковка pipeline (Коннектор)

> **Правила проекта:**
> 1. Выполняем строго **ОДИН подпункт** за раз. Следующий — только по прямой команде.
> 2. **Коммит** (`git commit`) — после каждой подфазы.
> 3. **Push** (`git push`) — только после завершения всей фазы целиком.
> 4. После каждой подфазы — обновление статуса в этом документе и краткий отчёт.

---

## Контекст

В Фазе 1 были реализованы все модули **по отдельности**:
- 10 image-процессоров (quality, crop, perspective, graph detection, axis, OCR, mask, curve, preprocessor, normalizer)
- 14 UI-экранов (quality screen, crop review, perspective review, и т.д.)
- Модели данных (CropResult, AxesResult, GraphRegion, CurvePoint, DigitalSignal, etc.)
- Room DB + Entities + DAOs
- Экспорт (CSV/JSON/Share)

В Фазе 2 были реализованы алгоритмы расчёта (baseline, peaks, integration, metrics) — 37 подфаз.

**Проблема**: все модули были реализованы как изолированные компоненты. `ProcessingFlowScreen` не передавал данные правильно между шагами, сигнал не сохранялся в Room, калибровка не применялась, AnalysisFlowScreen — заглушка.

**Фаза 1b** исправляет все точки разрыва и создаёт **сквозной рабочий pipeline**.

---

## Подфазы

| # | Подфаза | Статус |
|---|---------|--------|
| 1b.1 | Аудит точек разрыва и граф зависимостей | ✅ |
| 1b.2 | Нормализация при входе в pipeline | ✅ |
| 1b.3 | Правильная передача CropResult → currentImagePath | ✅ |
| 1b.4 | Передача PerspectiveResult → currentImagePath | ✅ |
| 1b.5 | Image Preprocessing stage (grayscale→CLAHE→binary) | ✅ |
| 1b.6 | Калибровка: применение XCalibration + YCalibration → PixelCalibration | ✅ |
| 1b.7 | SignalConverter: curvePoints + calibration → DigitalSignal с реальными единицами | ✅ |
| 1b.8 | Сглаживание сигнала (SignalSmoother) | ✅ |
| 1b.9 | Quality Report (DigitizationQualityReport) по итогам pipeline | ✅ |
| 1b.10 | ExportScreen: подключение PointExporter + SessionWriter + FileSharer | ✅ |
| 1b.11 | Сохранение DigitalSignal в Room (ChromatogramEntity) | ✅ |
| 1b.12 | Phase 1→2 bridge: передача signalId из Room в AnalysisFlowScreen | ✅ |
| 1b.13 | AnalysisFlowScreen: загрузка сигнала из Room | ✅ |
| 1b.14 | AnalysisFlowScreen: подключение реальных Phase 2 алгоритмов | ✅ |
| 1b.15 | Multi-graph loop: повтор pipeline для каждого графика | ⬜ |
| 1b.16 | Error handling и recovery на каждом шаге | ⬜ |
| 1b.17 | Desktop actual stubs (устранение compilation error) | ⬜ |
| 1b.18 | End-to-end тест на реальном фото | ⬜ |
| 1b.19 | Версия 0.0.3 + signed APK + GitHub Release | ⬜ |

---

## Подробное описание

---

### 1b.1 — Аудит точек разрыва и граф зависимостей

Формальная фиксация всех разрывов. Результат — мермайд-диаграмма data flow.

**Чеклист:**
- [ ] Составить граф: какой шаг что порождает (input → output)
- [ ] Отметить все точки, где данные теряются или не передаются
- [ ] Записать граф в `docs/DATA_FLOW.md`

---

### 1b.2 — Нормализация при входе в pipeline

`ImageNormalizer.normalize()` вызывается сейчас **только** для получения размеров. Нужно вызвать его **первым шагом** — до quality analysis — чтобы EXIF ориентация была исправлена.

**Чеклист:**
- [ ] Вызвать `imageNormalizer.normalize(imagePath, outputDir)` при входе в `ProcessingFlowScreen`
- [ ] Сохранить `NormalizedImageResult` в state
- [ ] Все последующие шаги работают с `normalizedPath`, не с `imagePath`
- [ ] Ширина/высота нормализованного изображения доступны во всех шагах

---

### 1b.3 — Правильная передача CropResult → currentImagePath

Сейчас `cropResult` создаётся, но `currentImagePath` обновляется **только если crop был успешным**. Если `DocumentDetector.detect()` вернул `null`, используется `fallbackCropResult` — но `currentImagePath` не обновляется.

**Чеклист:**
- [ ] Убедиться: после CROP_REVIEW `currentImagePath` **всегда** указывает на актуальный файл
- [ ] Если пользователь нажал «Переобрезать» — пересоздать crop и обновить path
- [ ] Если crop не создал файл — использовать исходный normalized path

---

### 1b.4 — Передача PerspectiveResult → currentImagePath

Аналогично crop: если `PerspectiveWarper.warp()` вернул `null` или `fallbackPerspectiveResult`, `currentImagePath` не обновляется правильно.

**Чеклист:**
- [ ] Проверить: `perspectiveResult.correctedPath` — это реальный файл, или тот же путь?
- [ ] Если warp пропущен (нет перспективного искажения) — `currentImagePath` остаётся от crop
- [ ] Убрать повторный вызов `documentDetector.detect()` в PERSPECTIVE (уже вызывался в CROP)

---

### 1b.5 — Image Preprocessing stage

`ImagePreprocessor` (242 строки) **нигде не вызывается** в pipeline. Он превращает цветное изображение в бинаризованное, что **критически важно** для `CurveMaskPreparer` и `CurveExtractor`.

**Чеклист:**
- [ ] Добавить вызов `preprocessor.preprocess(currentImagePath, outputDir)` после PERSPECTIVE
- [ ] Сохранить `PreprocessingResult` в state
- [ ] `CurveMaskPreparer.prepare()` использует `preprocessingResult.binaryPath` вместо `currentImagePath`
- [ ] Опционально: показать пользователю результат бинаризации (debug overlay)

---

### 1b.6 — Калибровка: XCalibration + YCalibration → PixelCalibration

Сейчас `xCalibration` и `yCalibration` сохраняются в state, но **никогда не объединяются** в `PixelCalibration` и не применяются к сигналу.

**Чеклист:**
- [ ] После Y_CALIBRATION: построить `PixelCalibration.from(xCal, yCal, originX, originY)`
- [ ] Сохранить `PixelCalibration` в state
- [ ] `originPixelX/Y` берутся из `axesResult.origin`
- [ ] Добавить fallback калибровку (1:1 pixel) если пользователь пропустил

---

### 1b.7 — SignalConverter: curvePoints + calibration → DigitalSignal

`SignalConverter.convert()` уже полностью реализован (136 строк), но **нигде не вызывается**. Сейчас `SIGNAL_PREVIEW` строит сигнал с `time = pixelX` и `intensity = pixelY` — без калибровки.

**Чеклист:**
- [ ] Заменить ручное построение `GraphPoint` в SIGNAL_PREVIEW на `SignalConverter.convert(curvePoints, pixelCalibration, currentImagePath)`
- [ ] Сигнал теперь имеет реальные единицы (мин/mAU) вместо пикселей
- [ ] Проверить: `timeUnit` и `intensityUnit` из калибровки

---

### 1b.8 — Сглаживание сигнала

`SignalSmoother.smooth()` (138 строк, Savitzky-Golay) реализован, но не вызывается.

**Чеклист:**
- [ ] Вызвать `SignalSmoother.smooth(signal)` после конвертации
- [ ] `SmoothedSignal` содержит и raw, и smoothed
- [ ] `SignalPreviewScreen` получает реальный SmoothedSignal
- [ ] Пользователь может переключить raw/smoothed (если UI поддерживает)

---

### 1b.9 — Quality Report

Шаг QUALITY_REPORT — placeholder. Нужно подключить `QualityCalculator` + `WarningCollector` → `DigitizationQualityReport`.

**Чеклист:**
- [ ] Собрать `DigitizationQualityReport` из всех стадий pipeline:
  - `imageQuality` — из `qualityReport`
  - `documentDetection` — из `cropResult` + `perspectiveResult`
  - `graphDetection` — из `graphResult`
  - `axisCalibration` — из `axesResult` + `pixelCalibration`
  - `curveExtraction` — из `curveExtractionResult`
- [ ] Подключить `WarningsScreen` или создать экран `DigitizationReportScreen`
- [ ] Показать итоговый score + все предупреждения

---

### 1b.10 — ExportScreen

Шаг EXPORT — placeholder. `ExportScreen` уже реализован (192 строки), но требует `DigitalSignal`, `ExportBundle`, и `SessionWriter`.

**Чеклист:**
- [ ] Создать `ExportBundle` из текущего state:
  - `signal` — финальный `DigitalSignal`
  - `calibration` — `PixelCalibration`
  - `processingParams` — собрать из всех шагов
- [ ] Создать `SessionWriter(outputDir)` 
- [ ] Заменить placeholder на реальный `ExportScreen`
- [ ] CSV/JSON экспорт работает, Share sheet открывается

---

### 1b.11 — Сохранение в Room

`DigitalSignal` не сохраняется в Room. `ChromatogramEntity` готова (37 строк), `ChromatogramDao` готов, но никогда не вызывается.

**Чеклист:**
- [ ] После EXPORT или при завершении pipeline:
  - Создать `SampleEntity` (или использовать default)
  - Создать `ChromatogramEntity` из `DigitalSignal`:
    - `dataPoints` = JSON-сериализация points
    - `timeRangeStart/End` из signal
    - `intensityUnit` из calibration
    - `ionChannel` из `selectedRegion.label`
    - `qualityScore` из quality report
- [ ] Вернуть `chromatogramId` как `signalId` для навигации

---

### 1b.12 — Phase 1→2 bridge с реальным ID

Сейчас `onFinish` генерирует `signal_${timestamp}` — фейковый ID. Нужно передать реальный `chromatogramId` из Room.

**Чеклист:**
- [ ] `ProcessingFlowScreen.onFinish` принимает `signalId: String` (Room ID)
- [ ] `App.kt` навигирует: `Route.Analysis(signalId = chromatogramId.toString())`
- [ ] `AnalysisFlowScreen` загружает сигнал по ID

---

### 1b.13 — AnalysisFlowScreen: загрузка сигнала из Room

`AnalysisFlowScreen` получает `signalId`, но показывает текстовые заглушки. Нужно загрузить сигнал из Room.

**Чеклист:**
- [ ] Через `ChromatogramDao.getById(signalId.toLong())` загрузить entity
- [ ] Десериализовать `dataPoints` → `DigitalSignal`
- [ ] Передать в реальный signal chart

---

### 1b.14 — AnalysisFlowScreen: подключение Phase 2 алгоритмов

`AnalysisStepContent` — текстовые заглушки. Phase 2 алгоритмы (37 подфаз) реализованы, но не подключены к UI.

**Чеклист:**
- [ ] SIGNAL_OVERVIEW → Загрузка + чарт сигнала (Canvas/DrawScope)
- [ ] LAYER_SELECTION → Переключатели: raw/smoothed/baseline/corrected
- [ ] PEAK_DETECTION → `PeakDetector.findPeaks(signal)` → отображение пиков
- [ ] PEAK_REVIEW → Нажатие на пик → PeakDetails (RT, height, area, width, S/N)
- [ ] PEAK_CORRECTION → Drag boundaries, add/reject peaks
- [ ] NOISE_BASELINE → Выбор noise region + baseline method
- [ ] RESULTS → Таблица пиков с метриками
- [ ] EXPORT → Сохранение `CalculationRun` + CSV/JSON

---

### 1b.15 — Multi-graph loop

Пользователь может выбрать 2+ графиков (Ion 217 + Ion 218). Сейчас обрабатывается только один.

**Чеклист:**
- [ ] После GRAPH_SELECTION: сохранить `List<GraphSelection>` (не только один)
- [ ] После EXPORT первого графика: спросить «Обработать следующий?»
- [ ] Если да → вернуться к GRAPH_ROI с `selectedIndex = next`
- [ ] Каждый сигнал сохраняется в Room отдельно
- [ ] Связь между графиками — через `SampleEntity`

---

### 1b.16 — Error handling и recovery

Сейчас: `try/catch` с `e.printStackTrace()`. Пользователь видит бесконечный спиннер если процессор упал.

**Чеклист:**
- [ ] Таймаут для каждого процессора (30 сек max)
- [ ] При ошибке: показать Snackbar с описанием + кнопка «Повторить»
- [ ] Fallback: пользователь может пропустить шаг и перейти к следующему
- [ ] Логирование ошибок в `DebugConfig`

---

### 1b.17 — Desktop actual stubs

`App.kt` использует `backStackEntry.arguments?.getString()` — Android-only API. Desktop не компилируется.

**Чеклист:**
- [ ] Заменить `arguments?.getString()` на type-safe navigation
- [ ] Или: добавить desktop-only стубы для expect classes
- [ ] `desktopTest` должен проходить

---

### 1b.18 — End-to-end тест

Проверка полного пути: фото → цифровой сигнал → пики → экспорт.

**Чеклист:**
- [ ] Взять реальное фото хроматограммы (Ion 217/218)
- [ ] Пройти все 14 шагов Phase 1
- [ ] Проверить: сигнал в Room, единицы мин/mAU, CSV содержит реальные значения
- [ ] Перейти в Phase 2: пики найдены, RT/area корректны
- [ ] Скриншоты каждого шага для документации

---

### 1b.19 — Версия 0.0.3 + Release

**Чеклист:**
- [ ] Bump version: `versionCode = 3`, `versionName = "0.0.3"`
- [ ] Signed release APK
- [ ] Tag `v0.0.3-alpha`
- [ ] GitHub Release с changelog
- [ ] Обновить ROADMAP.md

---

## Definition of Done

Фаза 1b считается завершённой когда:

1. ✅ Полный сквозной путь работает: фото → 14 шагов → сигнал в Room → пики → экспорт
2. ✅ Калибровка применяется: время в минутах, интенсивность в mAU
3. ✅ Multi-graph: 2+ графика на одном листе обрабатываются последовательно
4. ✅ Каждый шаг обрабатывает ошибки без crash
5. ✅ CSV/JSON экспорт содержит реальные значения, не пиксели
6. ✅ Desktop target компилируется
7. ✅ End-to-end тест пройден на реальном фото
8. ✅ APK 0.0.3 опубликован в GitHub
