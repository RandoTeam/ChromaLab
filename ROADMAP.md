# ChromaLab — Roadmap

---

## Фаза 0: Фундамент (Неделя 1–2) ✅

> KMP архитектура, дизайн-система, навигация, Room DB, 8 языков, CI.

- [x] Инициализация KMP-проекта (Kotlin 2.3.21, AGP 9.2.1, CMP 1.10.3)
- [x] Настройка модулей (пакетная структура в composeApp/commonMain)
- [x] Подключение зависимостей (Navigation, Lifecycle, Koin, Coil, Room, Coroutines, Serialization)
- [x] Дизайн-система: Lab Precision Dark — WCAG AA, 8 chart colors, typography
- [x] Room DB: 6 entities, 6 DAOs, KSP + sqlite-bundled
- [x] Навигация: 15 type-safe routes, 4-tab bottom bar
- [x] Локализация: 8 языков (RU, EN, FR, DE, ES, IT, PL, CS), instant switch
- [x] CI: GitHub Actions build check

---

## Фаза 1: Камера + Оцифровка (Неделя 3–6)

> Ядро приложения — от фото до массива точек (35 секций, 250+ задач).

- [x] Камера: CameraX Preview + ImageCapture, рамка, фокус, вспышка, зум
- [x] Камера: Smart Scan auto-launch (ML Kit Document Scanner, убрана ручная съёмка)
- [x] Проверка качества фото: blur, brightness, contrast, glare, shadow
- [x] Импорт из галереи + подгонка под рамку
- [x] Нормализация: EXIF, ориентация, единый формат
- [x] Crop по рамке + CoordinateTransform
- [x] Определение листа: OpenCV contours + manual fallback
- [x] Perspective correction: homography + ручная коррекция углов
- [x] Предобработка: grayscale → CLAHE → threshold → morphology
- [x] Определение области графика: Hough lines + manual ROI
- [x] Поддержка нескольких графиков на листе
- [x] Определение осей X/Y: auto + manual editor
- [x] Калибровка осей: 2-point linear transform
- [x] OCR осей: ML Kit + 3-level filter (spatial + IQR) + 2-pass targeted crop
- [x] Извлечение кривой: column scan + contour + skeletonization
- [x] Ручная проверка и коррекция кривой
- [x] Преобразование в цифровой сигнал: GraphPoint[]
- [x] Savitzky-Golay smoothing
- [x] Предпросмотр цифрового графика
- [x] Индикатор качества оцифровки + предупреждения
- [x] Сохранение промежуточных файлов
- [x] Экспорт массива точек: CSV + JSON
- [x] Debug mode
- [ ] Детерминированность: pipelineVersion, fixed params, determinism test
- [ ] Тестирование: синтетические данные + 20–30 реальных фото

---

## Фаза 2: Расчётное ядро (Неделя 7–9) ✅

> Baseline, пики, площади, S/N — 37 подфаз, 36 завершено.

- [x] Baseline correction: Manual Linear, ALS, SNIP (3 метода)
- [x] Peak detection (prominence-based + overlap/shoulder)
- [x] Peak integration (trapezoidal, positive/negative area)
- [x] Таблица пиков: RT, height, area, width, S/N, confidence
- [x] Интерактивный график хроматограммы (Canvas Compose, layers)
- [x] Ручная коррекция пиков (drag boundaries, add/reject, audit trail)
- [x] Параметры алгоритма: UI настройки + 4 preset profile
- [x] Warning system: 16 кодов, 4 severity
- [x] Сохранение результатов в Room (6 новых таблиц)
- [x] Export: peaks.csv, calculation.json, signal.csv, baseline.csv, warnings.json
- [x] Детерминированность: DeterminismContract + DeterminismVerifier
- [x] Unit-тесты расчётного ядра (25 тестов)
- [x] Synthetic validation tests (18 тестов)
- [x] Quality metrics: PeakAccuracy, DetectionMetrics, BaselineMetrics

---

## Фаза 3: Импорт файлов (Неделя 10–11)

> CSV, PDF, mzML.

- [ ] Парсер CSV/TXT (auto-detect разделитель, колонки)
- [ ] Парсер PDF (векторный + растровый fallback)
- [ ] Парсер mzML (XML → chromatogram data)
- [ ] Единый интерфейс FileImporter
- [ ] Экран выбора файла + предпросмотр

---

## Фаза 4: Multi-channel + Ion ratio (Неделя 12–13)

> Работа с несколькими ионными каналами.

- [ ] Привязка нескольких хроматограмм к одному образцу
- [ ] RT matching между каналами
- [ ] Ion ratio (area и height)
- [ ] Допуски WADA TD2023IDCR
- [ ] Статус: CONFIRMED / DOUBTFUL / NOT_CONFIRMED
- [ ] UI: сравнительный вид двух каналов

---

## Фаза 5: Проекты + Отчёты (Неделя 14–16)

> Полная иерархия данных и экспорт.

- [ ] CRUD: проекты, образцы
- [ ] Привязка хроматограмм к образцам
- [ ] Экспорт PDF (iText): график + таблица + параметры + заключение
- [ ] Экспорт CSV
- [ ] Экспорт JSON
- [ ] Аудит-лог действий
- [ ] Детерминированность: сохранение конфига в каждом расчёте
- [ ] Калибровочные кривые (линейная/квадратичная регрессия)
- [ ] QC-модуль: blank, control, spike recovery

---

## Фаза 6: Полировка (Неделя 17–18)

> UX, локализация, тестирование.

- [ ] Тёмная тема
- [ ] Локализация: fr, cs, de, es, it, pl
- [ ] Онлайн-справка: API PubChem/ChemSpider для идентификации ионов
- [ ] Onboarding / tutorial
- [ ] Валидация на тестовых хроматограммах (эталонные результаты)
- [ ] Performance profiling
- [ ] Accessibility
- [ ] Подготовка к публикации: иконка, описание, скриншоты

---

## Фаза 7: Валидация + Релиз (Неделя 19–20)

> Проверка точности и выпуск.

- [ ] Тестирование на 30+ хроматограммах разного качества
- [ ] Сравнение результатов с эталонными расчётами
- [ ] Документирование точности и ограничений
- [ ] Beta-тестирование
- [ ] Google Play: internal testing → closed beta → production
- [ ] Документация пользователя

---

## Будущее (после релиза)

- Монетизация (подписки, тарифы)
- Серверная часть (аккаунты, облачное хранение)
- LIMS-интеграция
- Электронная подпись отчётов
- Роли пользователей
- API для сторонних систем
- iOS-версия
