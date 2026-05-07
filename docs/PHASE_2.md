# Фаза 2: Расчетное ядро + анализ пиков (Неделя 7–9)

> Цель: взять ExtractedSignal из Фазы 1 и получить воспроизводимый расчет пиков:
> RT, height, area, width, S/N, baseline, confidence, warnings, manual corrections.

---

## Roadmap

| # | Подфаза | Статус |
|---|---------|--------|
| 2.1 | Базовая цель фазы | ✅ |
| 2.2 | Пользовательский сценарий | ✅ |
| 2.3 | Минимальные экраны | ✅ |
| 2.4 | UX-принцип | ⬜ |
| 2.5 | Формальный вход | ⬜ |
| 2.6 | Модель сигналов | ⬜ |
| 2.7 | Порядок расчетного pipeline | ⬜ |
| 2.8 | Savitzky-Golay smoothing | ⬜ |
| 2.9 | Baseline correction: общий модуль | ⬜ |
| 2.10 | Manual linear baseline | ⬜ |
| 2.11 | ALS baseline | ⬜ |
| 2.12 | SNIP baseline | ⬜ |
| 2.13 | Baseline quality metrics | ⬜ |
| 2.14 | Noise estimation | ⬜ |
| 2.15 | Signal-to-noise ratio | ⬜ |
| 2.16 | Peak detection | ⬜ |
| 2.17 | Peak boundaries | ⬜ |
| 2.18 | Overlapping / shoulder peaks | ⬜ |
| 2.19 | Peak integration | ⬜ |
| 2.20 | Peak metrics | ⬜ |
| 2.21 | Confidence score пика | ⬜ |
| 2.22 | Интерактивный график Canvas Compose | ⬜ |
| 2.23 | Карточка пика / PeakDetailsBottomSheet | ⬜ |
| 2.24 | Таблица пиков | ⬜ |
| 2.25 | Ручная коррекция пиков | ⬜ |
| 2.26 | Algorithm Settings UI | ⬜ |
| 2.27 | Presets алгоритма | ⬜ |
| 2.28 | Warnings | ⬜ |
| 2.29 | Room storage | ⬜ |
| 2.30 | Export результатов | ⬜ |
| 2.31 | Детерминированность | ⬜ |
| 2.32 | Unit-тесты расчетного ядра | ⬜ |
| 2.33 | Synthetic validation tests | ⬜ |
| 2.34 | Проверка на реальных данных | ⬜ |
| 2.35 | Метрики качества | ⬜ |
| 2.36 | Что не входит в Фазу 2 | ⬜ |
| 2.37 | Definition of Done | ⬜ |

---

## 2.1 — Базовая цель фазы

- [x] Реализовать путь: ExtractedSignal → baseline → corrected signal → noise → peaks → boundaries → integration → metrics
- [x] Считать RT, height, area, width, S/N
- [x] Показывать результаты на интерактивном графике
- [x] Дать пользователю вручную исправлять пики и границы
- [x] Сохранять все параметры расчета
- [x] Сохранять ручные корректировки
- [x] Сохранять результаты в Room
- [x] Обеспечить повторяемость: один и тот же сигнал + параметры + правки = тот же результат
- [x] Не делать вывод "вещество найдено / не найдено" в этой фазе
- [x] Все выводы по фото помечать как расчетные / ориентировочные

---

## 2.2 — Пользовательский сценарий Фазы 2

- [x] Пользователь открывает оцифрованный сигнал из Фазы 1
- [x] Видит график хроматограммы
- [x] Видит переключатели слоев: raw / smoothed / baseline / corrected / peaks
- [x] Нажимает "Найти пики"
- [x] Приложение показывает найденные пики
- [x] Пользователь нажимает на пик
- [x] Открывается карточка пика с RT, height, area, width, S/N
- [x] Пользователь может двигать левую и правую границу пика
- [x] Пользователь может добавить пик вручную
- [x] Пользователь может отклонить ошибочный пик
- [x] Пользователь может выбрать noise region
- [x] Пользователь может сменить baseline method
- [x] После каждой правки метрики пересчитываются
- [x] Пользователь сохраняет CalculationRun
- [x] Пользователь экспортирует CSV/JSON результата

---

## 2.3 — Минимальные экраны Фазы 2

- [x] CalculationHomeScreen
- [x] ChromatogramAnalysisScreen
- [x] AlgorithmSettingsScreen
- [x] PeakDetailsBottomSheet
- [x] BaselineComparisonScreen
- [x] NoiseRegionScreen
- [x] PeakTableScreen
- [x] ManualCorrectionScreen
- [x] CalculationResultScreen
- [x] CalculationHistoryScreen
- [x] ExportCalculationScreen

---

## 2.4 — UX-принцип Фазы 2

- [ ] Основной экран — график, а не таблица
- [ ] Таблица пиков — отдельный блок или нижняя панель
- [ ] На экране не показывать сразу все технические параметры
- [ ] Технические параметры прятать в "Расширенные настройки"
- [ ] Для обычного пользователя дать presets: Conservative / Balanced / Sensitive / Manual Review
- [ ] Для сложных настроек использовать bottom sheet
- [ ] На графике использовать понятные маркеры:
      apex
      left boundary
      right boundary
      baseline
      noise region
      rejected peak
      manual peak
- [ ] У каждого пика должен быть статус:
      auto
      manual
      corrected
      rejected
      low confidence
- [ ] Все warning показывать человеческим языком
- [ ] Не перегружать экран формулами, но дать "Подробнее" для технического протокола

---

## 2.5 — Формальный вход Фазы 2

- [ ] Принимать ExtractedSignal из Фазы 1
- [ ] ExtractedSignal должен содержать массив точек:
      index, time, intensity, pixelX, pixelY, confidence, interpolated flag
- [ ] Проверять сортировку по time
- [ ] Проверять дубликаты time
- [ ] Проверять пропуски time
- [ ] Проверять NaN / Infinity
- [ ] Проверять отрицательные или невозможные значения intensity, если применимо
- [ ] Проверять равномерность шага time
- [ ] Если шаг time неравномерный — использовать реальный Δt при интеграции
- [ ] Не изменять rawSignal
- [ ] Все производные сигналы хранить отдельно

---

## 2.6 — Модель сигналов

- [ ] Хранить rawSignal
- [ ] Хранить smoothedSignal
- [ ] Хранить estimatedBaseline
- [ ] Хранить baselineCorrectedSignal
- [ ] Хранить signalUsedForDetection
- [ ] Хранить signalUsedForIntegration
- [ ] По умолчанию искать пики по smoothed corrected signal
- [ ] По умолчанию интегрировать по rawSignal - baseline
- [ ] Не использовать smoothedSignal для финальной площади, если пользователь явно это не включил
- [ ] В интерфейсе показывать, какой сигнал сейчас отображается
- [ ] В отчете сохранять, какой сигнал использовался для detection и integration

---

## 2.7 — Порядок расчетного pipeline

- [ ] Зафиксировать pipeline order:
      1. raw signal validation
      2. optional smoothing
      3. baseline estimation
      4. baseline correction
      5. noise estimation
      6. peak detection
      7. peak boundary detection
      8. peak integration
      9. peak metric calculation
      10. warnings/confidence
      11. manual corrections
      12. recalculation
- [ ] Сохранять pipelineVersion
- [ ] Сохранять algorithmVersion
- [ ] Сохранять все параметры pipeline
- [ ] Любой пересчет с новыми параметрами должен создавать новый CalculationRun
- [ ] Старый CalculationRun не перезаписывать

---

## 2.8 — Savitzky-Golay smoothing

- [ ] Реализовать Savitzky-Golay smoothing
- [ ] Параметры: windowSize, polynomialOrder, edgeHandlingMode
- [ ] windowSize должен быть нечетным
- [ ] polynomialOrder должен быть меньше windowSize
- [ ] Проверять, что точек достаточно для smoothing
- [ ] Хранить smoothedSignal отдельно
- [ ] Добавить переключатель smoothing on/off
- [ ] Добавить предупреждение: smoothing может менять высоту и ширину пика
- [ ] По умолчанию использовать smoothing только для detection
- [ ] Добавить unit tests для smoothing

---

## 2.9 — Baseline correction: общий модуль

- [ ] Создать BaselineCorrector interface
- [ ] Реализовать BaselineMethod.NONE
- [ ] Реализовать BaselineMethod.MANUAL_LINEAR
- [ ] Реализовать BaselineMethod.ALS
- [ ] Реализовать BaselineMethod.SNIP
- [ ] Для каждого метода сохранять baseline array
- [ ] Для каждого метода сохранять параметры
- [ ] Для каждого метода сохранять warnings
- [ ] Дать пользователю сравнивать baseline methods
- [ ] Один метод должен быть активным для текущего расчета
- [ ] ALS сделать основным автоматическим методом
- [ ] Manual linear baseline сделать прозрачным контрольным методом
- [ ] SNIP сделать альтернативным / experimental методом

---

## 2.10 — Manual linear baseline

- [ ] Реализовать baseline между leftBoundary и rightBoundary
- [ ] Использовать линейную интерполяцию baseline под пиком
- [ ] Дать пользователю вручную двигать boundaries
- [ ] Показывать линию baseline на графике
- [ ] Интегрировать площадь относительно этой baseline
- [ ] Сохранять manual baseline points
- [ ] Использовать как fallback при спорной автоматической baseline
- [ ] Добавить warning, если baseline пересекает сигнал некорректно

---

## 2.11 — ALS baseline

- [ ] Реализовать ALS baseline
- [ ] Использовать модель: minimize Σ wi(yi - zi)² + λΣ(Δ²zi)²
- [ ] Параметры: lambda, p, iterations, diffOrder
- [ ] lambda управляет гладкостью baseline
- [ ] p управляет асимметрией весов
- [ ] Добавить presets: soft, medium, stiff
- [ ] Сохранять final weights
- [ ] Сохранять convergence status
- [ ] Сохранять baselineQualityReport
- [ ] Добавить warning, если baseline слишком агрессивная
- [ ] Добавить warning, если baseline выше сигнала на большом участке
- [ ] Добавить warning, если baseline заметно меняет area
- [ ] Добавить unit tests на synthetic baseline

---

## 2.12 — SNIP baseline

- [ ] Реализовать SNIP baseline
- [ ] Параметры: iterations, windowSize, smoothingMode
- [ ] Показывать SNIP как альтернативный метод
- [ ] Не делать SNIP единственным default
- [ ] Добавить сравнение SNIP vs ALS vs Manual Linear
- [ ] Добавить warning при широких пиках
- [ ] Добавить warning при сильном отличии площади от ALS/manual baseline
- [ ] Добавить unit tests на synthetic nonlinear baseline

---

## 2.13 — Baseline quality metrics

- [ ] Реализовать baselineResidualRMS в noise regions
- [ ] Реализовать baselineAboveSignalFraction
- [ ] Реализовать negativeCorrectedFraction
- [ ] Реализовать baselineCrossingCount
- [ ] Реализовать areaSensitivityToBaseline
- [ ] Если разные baseline methods дают сильно разные areas — показывать warning
- [ ] Сохранять baselineQualityReport
- [ ] Показывать пользователю простую оценку: good / acceptable / risky / failed

---

## 2.14 — Noise estimation

- [ ] Создать NoiseEstimator
- [ ] Поддержать ручной выбор noise region
- [ ] Поддержать автоматический выбор noise region вне пиков
- [ ] Поддержать noise region около ожидаемого пика
- [ ] Поддержать режим 20 × Wh
- [ ] Поддержать fallback режим 5 × Wh
- [ ] Исключать сам пик из noise region
- [ ] Исключать соседние пики из noise region
- [ ] Проверять, что noise region достаточно длинный
- [ ] Считать peak-to-peak noise: h = max(noiseRegion) - min(noiseRegion)
- [ ] Считать RMS noise
- [ ] Считать robust noise через MAD
- [ ] Сохранять noiseMethod, noiseRegionStart, noiseRegionEnd, noiseValue
- [ ] Сохранять warning, если noise region выбран вручную
- [ ] Сохранять warning, если noise region содержит подозрительный пик

---

## 2.15 — Signal-to-noise ratio

- [ ] Создать SnrCalculator
- [ ] Реализовать S/N по peak-to-peak noise: S/N = 2H / h
- [ ] Реализовать engineering mode: S/N = H / RMS_noise
- [ ] Реализовать robust mode: S/N = H / robustNoise
- [ ] В таблице явно показывать S/N method
- [ ] Не смешивать разные S/N methods без обозначения
- [ ] Добавить ориентировочные флаги: <3 low, 3–10 detectable-like, ≥10 quantitation-like
- [ ] Не называть эти флаги юридическим или лабораторным заключением
- [ ] Сохранять formula description, noise window, S/N warnings

---

## 2.16 — Peak detection

- [ ] Создать PeakDetector
- [ ] Работать по baselineCorrectedSignal
- [ ] Для detection можно использовать smoothed corrected signal
- [ ] Реализовать поиск локальных максимумов
- [ ] Реализовать prominence-based filtering
- [ ] Параметры: minHeight, minProminence, minDistance, minWidth, maxWidth, prominenceWindow, minSnr
- [ ] minProminence задавать в absolute units и как k × noise
- [ ] Добавить default k = 3 для чувствительного поиска
- [ ] Для каждого кандидата сохранять: apexTime, apexIntensity, prominence, width, detectionScore, accepted/rejected, rejectReason
- [ ] В debug mode показывать rejected peak candidates
- [ ] Не делать обнаружение вещества на основании одного найденного пика

---

## 2.17 — Peak boundaries

- [ ] Создать PeakBoundaryDetector
- [ ] Реализовать boundary method: prominence bases
- [ ] Реализовать boundary method: local minima left/right
- [ ] Реализовать boundary method: baseline intersection
- [ ] Реализовать boundary method: percent of height (1% или 5%)
- [ ] Реализовать ручные boundaries
- [ ] Сохранять boundaryMethod, leftBoundaryTime, rightBoundaryTime, boundaryConfidence
- [ ] Добавить warnings: пересечение с соседним пиком, обрезан краем, baseline не пересечена, ручные границы

---

## 2.18 — Overlapping / shoulder peaks

- [ ] Детектировать близкие пики
- [ ] Детектировать shoulder peaks
- [ ] Детектировать отсутствие valley между соседними пиками
- [ ] Ввести peakOverlapStatus: isolated, partially_overlapped, shoulder, unresolved
- [ ] Для unresolved peaks разрешить только ручное уточнение boundaries
- [ ] Не делать сложную deconvolution как обязательную функцию Фазы 2
- [ ] Помечать area unresolved peaks как low confidence
- [ ] Добавить warning: overlapping peak

---

## 2.19 — Peak integration

- [ ] Создать PeakIntegrator
- [ ] Реализовать trapezoidal integration: area = Σ 0.5 × (y_i + y_{i+1}) × (t_{i+1} - t_i)
- [ ] Интегрировать baseline-corrected raw signal
- [ ] Не использовать smoothed signal для финальной площади по умолчанию
- [ ] Поддержать irregular time spacing
- [ ] Поддержать linear interpolation на start/end boundaries
- [ ] Считать totalArea, positiveArea, negativeArea
- [ ] Не обнулять отрицательные участки без явного параметра
- [ ] Сохранять integrationMethod, units (intensity × minute / second)
- [ ] Добавить unit tests: rectangle, triangle, Gaussian peak, irregular spacing

---

## 2.20 — Peak metrics

- [ ] Создать PeakMetricsCalculator
- [ ] Считать RT apex, RT centroid (optional), height, area, widthBase, widthHalfHeight, widthHalfProminence, prominence, leftBase/rightBase, S/N, confidence score
- [ ] Сохранять warnings, manual/auto status

---

## 2.21 — Confidence score пика

- [ ] Создать PeakConfidenceCalculator
- [ ] Учитывать S/N, качество baseline, качество boundaries, confidence точек из Фазы 1, overlapStatus, ручные правки, интерполированные участки
- [ ] Выдавать статус: high, medium, low, failed
- [ ] Не скрывать причину низкого confidence
- [ ] Показывать пользователю список причин

---

## 2.22 — Интерактивный график Canvas Compose

- [ ] Реализовать Canvas Compose chart
- [ ] Показывать raw / smoothed / baseline / corrected signal / пики
- [ ] Показывать apex markers, left/right boundaries, shaded integration area, noise region
- [ ] Поддержать zoom, pan, tap по пику, long press для ручного пика, drag boundaries
- [ ] Поддержать layer toggles, reset zoom, fit to peak
- [ ] Оптимизировать отрисовку для больших массивов точек (downsampling только для отображения)

---

## 2.23 — Карточка пика / PeakDetailsBottomSheet

- [ ] Показывать peakId, RT apex, height, area, width, S/N, prominence
- [ ] Показывать baseline/integration/noise method, confidence, warnings
- [ ] Кнопки: исправить границы, выбрать noise region, сменить baseline, принять/отклонить пик, технические детали

---

## 2.24 — Таблица пиков

- [ ] Колонки: status, RT apex, height, area, width, S/N, prominence, confidence, warnings
- [ ] Сортировка по RT, фильтр по статусу
- [ ] Быстрый переход от строки к пику на графике
- [ ] Экспорт таблицы в CSV

---

## 2.25 — Ручная коррекция пиков

- [ ] Drag left/right boundary
- [ ] Ручное добавление/удаление/восстановление пика
- [ ] Ручной выбор apex, noise region, baseline method
- [ ] После каждой правки пересчитывать metrics
- [ ] Сохранять oldValue, newValue, timestamp, editType, editReason
- [ ] Все правки в ManualEditLog

---

## 2.26 — Algorithm Settings UI

- [ ] Разделы: Smoothing, Baseline, Peak Detection, Boundaries, Integration, Noise/S/N, Confidence, Advanced
- [ ] Presets: Conservative, Balanced, Sensitive, Manual Review
- [ ] Reset to defaults, save/apply profile
- [ ] Изменение настроек → "Пересчитать" (не молчаливая перезапись)

---

## 2.27 — Presets алгоритма

- [ ] Conservative: выше minProminence/minSNR, меньше false positives
- [ ] Balanced: средние настройки
- [ ] Sensitive: ниже minProminence/minSNR, больше candidates
- [ ] Manual Review: авто-поиск минимальный, акцент на ручные правки
- [ ] Каждый preset = AlgorithmProfile, пользователь может создать свой

---

## 2.28 — Warnings

- [ ] Warning: низкий S/N, пик после smoothing, перекрытие, shoulder, ручные границы
- [ ] Warning: baseline влияет на area, noise region проблемы, baseline выше/пересекает сигнал
- [ ] Warning: отрицательная площадь, низкое качество оцифровки, interpolated points, низкий confidence
- [ ] Warning: результат ориентировочный
- [ ] Severity: info, caution, serious, failed

---

## 2.29 — Room storage

- [ ] Таблицы: CalculationRun, AlgorithmProfile, SignalEntity, BaselineResult, PeakEntity, NoiseRegionEntity, ManualEditEntity, CalculationWarningEntity, ExportRecord
- [ ] Relation: one ExtractedSignal → many CalculationRuns
- [ ] Не перезаписывать старый расчет, новый = новый CalculationRun

---

## 2.30 — Export результатов

- [ ] Export: peaks.csv, calculation.json, corrected_signal.csv, baseline.csv, warnings.json
- [ ] CSV пиков: peakId, status, rtApex, height, area, widthBase, widthHalfHeight, prominence, snr, snrMethod, baselineMethod, integrationMethod, confidence, warnings
- [ ] JSON: metadata, sourceSignalId, pipelineVersion, algorithmParams, baseline, peaks, noiseRegions, manualEdits, warnings
- [ ] Share через системное меню Android

---

## 2.31 — Детерминированность

- [ ] Pure functions, Double для расчетов (не Float), no random, no ML
- [ ] signal + params + manual edits = same result
- [ ] Determinism test, сохранять numericPrecisionMode + algorithmVersion

---

## 2.32 — Unit-тесты расчетного ядра

- [ ] Тесты: input validation, sorting, smoothing, manual/ALS/SNIP baseline
- [ ] Тесты: peak detection (single, close, noise-only), prominence, boundaries
- [ ] Тесты: trapezoidal integration (rectangle, triangle, Gaussian, irregular)
- [ ] Тесты: S/N (peak-to-peak, RMS), manual correction, rejected peak
- [ ] Тесты: CalculationRun save/load, identical input → identical output

---

## 2.33 — Synthetic validation tests

- [ ] Генерировать: flat/linear/polynomial/sinusoidal baseline drift
- [ ] Генерировать: Gaussian/skewed/overlapping/shoulder/low-S/N peaks
- [ ] Генерировать: white noise, spike noise
- [ ] Для synthetic: знать true RT/height/area/width
- [ ] Считать: RT error, height/area/width relative error, false positives/negatives, baseline RMSE
- [ ] Golden test suite

---

## 2.34 — Проверка на реальных данных

- [ ] Реальные ExtractedSignal из Фазы 1
- [ ] Если возможно: цифровой экспорт прибора (CSV/TXT/mzML/netCDF)
- [ ] Если возможно: отчет родного ПО прибора (RT, area, height, S/N)
- [ ] Сравнивать отдельно: ядро vs экспорт, фото+расчет vs отчет прибора
- [ ] Не смешивать ошибку оцифровки и ошибку расчетного ядра

---

## 2.35 — Метрики качества Фазы 2

- [ ] RT absolute/relative error
- [ ] Height/Area/Width relative error
- [ ] S/N relative difference
- [ ] Peak detection precision/recall
- [ ] False positive/negative peaks
- [ ] Baseline RMSE, area sensitivity to baseline method
- [ ] Repeatability, determinism pass/fail

---

## 2.36 — Что НЕ входит в Фазу 2

- [ ] Не делать определение вещества
- [ ] Не делать финальное экспертное заключение
- [ ] Не делать судебно значимый отчет
- [ ] Не делать полноценную калибровочную кривую концентрации
- [ ] Не делать LOD/LOQ как валидированную лабораторную функцию
- [ ] Не делать deconvolution overlapping peaks как обязательный блок
- [ ] Не делать cloud sync, коммерческие лимиты/тарифы
- [ ] Не делать ML-поиск пиков
- [ ] Не делать автоматическое "положительно/отрицательно"

---

## 2.37 — Definition of Done для Фазы 2

- [ ] Приложение принимает ExtractedSignal из Фазы 1
- [ ] Приложение показывает интерактивный график
- [ ] Приложение показывает raw/smoothed/baseline/corrected layers
- [ ] Приложение считает baseline минимум manual linear и ALS
- [ ] SNIP реализован как alternative/experimental метод
- [ ] Приложение оценивает noise region
- [ ] Приложение считает S/N с указанием метода
- [ ] Приложение находит peak candidates
- [ ] Приложение определяет boundaries
- [ ] Приложение считает RT, height, area, width, prominence, S/N
- [ ] Приложение показывает таблицу пиков
- [ ] Приложение позволяет вручную двигать границы пика
- [ ] Приложение позволяет вручную добавить/отклонить пик
- [ ] После ручной коррекции метрики пересчитываются
- [ ] Все параметры алгоритма сохраняются
- [ ] Все ручные правки сохраняются
- [ ] Результаты сохраняются в Room
- [ ] Есть экспорт CSV/JSON
- [ ] Есть unit tests расчетного ядра
- [ ] Есть synthetic validation tests
- [ ] Один и тот же signal + params + manual edits дает тот же результат
