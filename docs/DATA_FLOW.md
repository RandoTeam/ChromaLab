# ChromaLab — Data Flow Audit (Phase 1b.1)

> Формальная фиксация всех разрывов между модулями pipeline.
> Каждый модуль описан: что принимает, что отдаёт, что сломано.

---

## Граф потока данных

```mermaid
graph TD
    subgraph "Вход"
        A["imagePath<br/>(от камеры/галереи)"]
    end

    subgraph "ОТСУТСТВУЕТ"
        B["ImageNormalizer.normalize()"]
        B_OUT["normalizedPath<br/>+ width/height"]
    end

    subgraph "Шаг 0: IMAGE_QUALITY"
        Q["ImageQualityAnalyzer.analyze()"]
        Q_OUT["ImageQualityReport<br/>(7 метрик)"]
    end

    subgraph "Шаг 1: CROP_REVIEW"
        D["DocumentDetector.detect()"]
        CR["ImageCropper.crop()"]
        CR_OUT["CropResult<br/>+ croppedPath"]
    end

    subgraph "Шаг 2: PERSPECTIVE"
        PW["PerspectiveWarper.warp()"]
        PW_OUT["PerspectiveCorrectionResult<br/>+ correctedPath"]
    end

    subgraph "ОТСУТСТВУЕТ"
        PP["ImagePreprocessor.preprocess()"]
        PP_OUT["PreprocessingResult<br/>grayscale/binary paths"]
    end

    subgraph "Шаг 3-4: GRAPH"
        GR["GraphRegionDetector.detect()"]
        GR_OUT["GraphRegionResult<br/>regions + selectedRegion"]
    end

    subgraph "Шаг 5: AXIS_DETECTION"
        AD["AxisDetector.detect()"]
        AD_OUT["AxesResult<br/>xAxis/yAxis/origin"]
    end

    subgraph "Шаг 6-7: CALIBRATION"
        XC["XCalibrationScreen"]
        YC["YCalibrationScreen"]
        XC_OUT["XAxisCalibration"]
        YC_OUT["YAxisCalibration"]
    end

    subgraph "ОТСУТСТВУЕТ"
        PC["PixelCalibration.from()"]
        PC_OUT["PixelCalibration<br/>pixelToTime / pixelToIntensity"]
    end

    subgraph "Шаг 8: OCR_SUGGESTION"
        OCR["AxisOcrReader.readAxisLabels()"]
        OCR_OUT["AxisOcrResult"]
    end

    subgraph "Шаг 9: CURVE_EXTRACTION"
        CM["CurveMaskPreparer.prepare()"]
        CE["CurveExtractor.extract()"]
        CE_OUT["CurveExtractionResult<br/>+ List CurvePoint"]
    end

    subgraph "ОТСУТСТВУЕТ"
        SC["SignalConverter.convert()"]
        SC_OUT["DigitalSignal<br/>с реальными единицами"]
    end

    subgraph "ОТСУТСТВУЕТ"
        SM["SignalSmoother.smooth()"]
        SM_OUT["SmoothedSignal<br/>raw + smoothed"]
    end

    subgraph "Шаг 11: SIGNAL_PREVIEW"
        SP["SignalPreviewScreen"]
        SP_NOTE["Строит signal ВРУЧНУЮ<br/>time=pixelX, unit=px"]
    end

    subgraph "Шаг 12: QUALITY_REPORT"
        QR_PLACEHOLDER["StepPlaceholder"]
        QR_REAL["QualityCalculator.buildReport()"]
    end

    subgraph "Шаг 13: EXPORT"
        EX_PLACEHOLDER["StepPlaceholder"]
        EX_REAL["ExportScreen + PointExporter"]
    end

    subgraph "ОТСУТСТВУЕТ"
        ROOM["Room ChromatogramDao.insert()"]
        ROOM_OUT["chromatogramId"]
    end

    subgraph "Phase 2"
        AF["AnalysisFlowScreen"]
        AF_NOTE["Все 8 шагов — заглушки"]
    end

    %% Flow connections
    A --> Q
    A -.->|"❌ normalizer не вызывается"| B
    B --> B_OUT
    Q --> Q_OUT
    A --> D
    D --> CR
    CR --> CR_OUT
    CR_OUT -->|"currentImagePath"| PW
    PW --> PW_OUT
    PW_OUT -.->|"❌ preprocessor не вызывается"| PP
    PP --> PP_OUT
    PW_OUT -->|"currentImagePath"| GR
    GR --> GR_OUT
    GR_OUT -->|"selectedRegion"| AD
    AD --> AD_OUT
    AD_OUT -->|"axes"| XC
    XC --> XC_OUT
    XC_OUT --> YC
    YC --> YC_OUT
    XC_OUT -.->|"❌ PixelCalibration не строится"| PC
    YC_OUT -.->|"❌ PixelCalibration не строится"| PC
    AD_OUT -.->|"origin"| PC
    PC --> PC_OUT
    GR_OUT -->|"selectedRegion"| OCR
    OCR --> OCR_OUT
    AD_OUT -->|"axes"| CM
    CM --> CE
    CE --> CE_OUT
    CE_OUT -.->|"❌ SignalConverter не вызывается"| SC
    PC_OUT -.->|"❌ калибровка не передаётся"| SC
    SC --> SC_OUT
    SC_OUT -.->|"❌ Smoother не вызывается"| SM
    SM --> SM_OUT
    CE_OUT -->|"⚠️ raw pixels"| SP
    SP --> SP_NOTE
    QR_PLACEHOLDER -.->|"❌ placeholder"| QR_REAL
    EX_PLACEHOLDER -.->|"❌ placeholder"| EX_REAL
    SP -.->|"❌ не сохраняется"| ROOM
    ROOM --> ROOM_OUT
    ROOM_OUT -.->|"❌ фейковый ID"| AF
    AF --> AF_NOTE

    %% Styling
    style B fill:#ff6b6b,color:#fff
    style PP fill:#ff6b6b,color:#fff
    style PC fill:#ff6b6b,color:#fff
    style SC fill:#ff6b6b,color:#fff
    style SM fill:#ff6b6b,color:#fff
    style ROOM fill:#ff6b6b,color:#fff
    style QR_PLACEHOLDER fill:#ffa94d,color:#fff
    style EX_PLACEHOLDER fill:#ffa94d,color:#fff
    style AF_NOTE fill:#ffa94d,color:#fff
    style SP_NOTE fill:#ffa94d,color:#fff
```

---

## Реестр разрывов

### 🔴 Критические (данные теряются / не создаются)

| # | Разрыв | Где | Что происходит | Что должно быть |
|---|--------|-----|---------------|-----------------|
| B1 | **ImageNormalizer** не вызывается | `ProcessingFlowScreen:L118-231` | `imagePath` используется напрямую, EXIF ориентация не исправлена | Вызвать первым, все шаги работают с `normalizedPath` |
| B2 | **ImagePreprocessor** не вызывается | `ProcessingFlowScreen:L118-231` | `CurveMaskPreparer` получает цветное фото вместо бинаризованного | Вызвать после PERSPECTIVE, передать `binaryPath` в mask preparer |
| B3 | **PixelCalibration** не строится | `ProcessingFlowScreen:L111-112` | `xCalibration` и `yCalibration` сохраняются, но не объединяются | `PixelCalibration.from(xCal, yCal, origin.x, origin.y)` |
| B4 | **SignalConverter** не вызывается | `ProcessingFlowScreen:L206-228` | Сигнал строится вручную: `time=pixelX, unit="px"` | `SignalConverter.convert(curvePoints, calibration, path)` |
| B5 | **SignalSmoother** не вызывается | `ProcessingFlowScreen:L224-227` | `smoothed = signal` (одинаковый raw и smoothed) | `SignalSmoother.smooth(signal, params)` |
| B6 | **Room** не используется | нигде | `DigitalSignal` не сохраняется в DB | `ChromatogramDao.insert(entity)` |
| B7 | **Phase 1→2 bridge** фейковый | `App.kt:L140` | `signalId = "signal_${timestamp}"` | Реальный `chromatogramId` из Room |

### 🟡 Средние (placeholder вместо реального экрана)

| # | Разрыв | Что сейчас | Что должно быть |
|---|--------|-----------|-----------------|
| M1 | **QUALITY_REPORT** | `StepPlaceholder` | `QualityCalculator.buildReport()` → `DigitizationQualityReport` → UI |
| M2 | **EXPORT** | `StepPlaceholder` | `ExportScreen(signal, bundle, sessionWriter)` |
| M3 | **AnalysisFlowScreen** | 8 текстовых заглушек | Загрузка из Room + реальные Phase 2 алгоритмы |

### 🟢 Минорные (работает, но с оговорками)

| # | Проблема | Описание |
|---|----------|----------|
| G1 | `fallbackCropResult` не обновляет `currentImagePath` | Строка 146: `cropResult = fallbackCropResult(imagePath)` — но `currentImagePath` не обновляется |
| G2 | `DocumentDetector` вызывается **дважды** | Один раз в CROP (L130), второй в PERSPECTIVE (L153) — лишняя работа |
| G3 | `selectedRegion` инициализирован хардкодом | L109: `GraphRegion(0, 0, 1920, 1080)` — не знает реальный размер |
| G4 | Multi-graph не поддерживается | Выбирается один регион, pipeline проходит один раз |
| G5 | Ошибки молча проглатываются | L234-236: `catch (e) { e.printStackTrace() }` — пользователь видит бесконечный спиннер |
| G6 | Desktop не компилируется | `App.kt`: `arguments?.getString()` — Android API |

---

## Правильный порядок потока данных

```
imagePath
  │
  ├──► ImageNormalizer.normalize()          → normalizedPath, width, height
  │
  ├──► ImageQualityAnalyzer.analyze()       → ImageQualityReport
  │
  ├──► DocumentDetector.detect()            → DocumentBounds (corners)
  │     └── bounds хранятся для crop И perspective
  │
  ├──► ImageCropper.crop()                  → CropResult, croppedPath
  │
  ├──► PerspectiveWarper.warp()             → correctedPath
  │       (используются corners из detect, НЕ вызывать detect повторно)
  │
  ├──► ImagePreprocessor.preprocess()       → grayscale, binary, morphology paths
  │
  ├──► GraphRegionDetector.detect()         → regions[], selectedRegion
  │
  ├──► AxisDetector.detect()                → xAxis, yAxis, origin
  │
  ├──► XCalibrationScreen → xCalibration
  ├──► YCalibrationScreen → yCalibration
  ├──► PixelCalibration.from(xCal, yCal, origin)  ← ОТСУТСТВУЕТ
  │
  ├──► AxisOcrReader.readAxisLabels()       → AxisOcrResult (hint)
  │
  ├──► CurveMaskPreparer.prepare(binaryPath, ...)  ← сейчас получает цветное фото
  ├──► CurveExtractor.extract()             → CurveExtractionResult, curvePoints
  │
  ├──► SignalConverter.convert(curvePoints, calibration)  ← ОТСУТСТВУЕТ
  │       → DigitalSignal (мин/mAU)
  │
  ├──► SignalSmoother.smooth(signal)         ← ОТСУТСТВУЕТ
  │       → SmoothedSignal
  │
  ├──► QualityCalculator.buildReport(...)    ← placeholder
  │
  ├──► ExportScreen(signal, bundle)          ← placeholder
  │
  ├──► ChromatogramDao.insert()              ← ОТСУТСТВУЕТ
  │       → chromatogramId
  │
  └──► Route.Analysis(chromatogramId)        ← фейковый ID
```

---

## Сводка: что реализовано vs что подключено

| Модуль | Реализован | Подключён | Строки кода |
|--------|:----------:|:---------:|:-----------:|
| ImageQualityAnalyzer | ✅ | ✅ | 324 |
| ImageNormalizer | ✅ | ⚠️ только для width/height | ~60 |
| DocumentDetector | ✅ | ✅ (но дважды) | 304 |
| ImageCropper | ✅ | ✅ | 46 |
| PerspectiveWarper | ✅ | ✅ | 171 |
| ImagePreprocessor | ✅ | ❌ **не вызывается** | 242 |
| GraphRegionDetector | ✅ | ✅ | 310 |
| AxisDetector | ✅ | ✅ | 173 |
| AxisOcrReader | ✅ | ✅ | 170 |
| CurveMaskPreparer | ✅ | ✅ (неверный input) | 316 |
| CurveExtractor | ✅ | ✅ | 278 |
| PixelCalibration | ✅ | ❌ **не строится** | 71 |
| SignalConverter | ✅ | ❌ **не вызывается** | 136 |
| SignalSmoother | ✅ | ❌ **не вызывается** | 138 |
| QualityCalculator | ✅ | ❌ **placeholder** | 199 |
| ExportScreen | ✅ | ❌ **placeholder** | 192 |
| PointExporter | ✅ | ❌ **не вызывается** | 62 |
| IntermediateFileSaver | ✅ | ❌ **не вызывается** | 105 |
| SessionWriter | ✅ | ❌ **не вызывается** | 29 |
| FileSharer | ✅ | ❌ **не вызывается** | 14 |
| ChromatogramDao | ✅ | ❌ **не вызывается** | ~50 |
| AnalysisFlowScreen | ✅ (shell) | ❌ **заглушки** | 190 |

**Итого**: 21 модуль реализован, 11 из них **не подключены** (52%).
