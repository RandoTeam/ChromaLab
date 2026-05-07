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

- [ ] Камера: CameraX Preview + ImageCapture, рамка, фокус, вспышка, зум
- [ ] Проверка качества фото: blur, brightness, contrast, glare, shadow
- [ ] Импорт из галереи + подгонка под рамку
- [ ] Нормализация: EXIF, ориентация, единый формат
- [ ] Crop по рамке + CoordinateTransform
- [ ] Определение листа: OpenCV contours + manual fallback
- [ ] Perspective correction: homography + ручная коррекция углов
- [ ] Предобработка: grayscale → CLAHE → threshold → morphology
- [ ] Определение области графика: Hough lines + manual ROI
- [ ] Поддержка нескольких графиков на листе
- [ ] Определение осей X/Y: auto + manual editor
- [ ] Калибровка осей: 2-point linear transform
- [ ] OCR осей: ML Kit как подсказка + подтверждение пользователя
- [ ] Извлечение кривой: column scan + contour + skeletonization
- [ ] Ручная проверка и коррекция кривой
- [ ] Преобразование в цифровой сигнал: GraphPoint[]
- [ ] Savitzky-Golay smoothing
- [ ] Предпросмотр цифрового графика
- [ ] Индикатор качества оцифровки + предупреждения
- [ ] Сохранение промежуточных файлов
- [ ] Экспорт массива точек: CSV + JSON
- [ ] Debug mode
- [ ] Детерминированность: pipelineVersion, fixed params, determinism test
- [ ] Тестирование: синтетические данные + 20–30 реальных фото

---

## Фаза 2: Расчётное ядро (Неделя 7–9)

> Baseline, пики, площади, S/N.

- [ ] Baseline correction (ALS + SNIP)
- [ ] Peak detection (prominence-based)
- [ ] Peak integration (trapezoidal)
- [ ] Таблица пиков: RT, height, area, width, S/N
- [ ] Интерактивный график хроматограммы (Canvas Compose)
- [ ] Ручная коррекция пиков (drag boundaries)
- [ ] Параметры алгоритма: UI настройки
- [ ] Сохранение результатов в Room
- [ ] Unit-тесты расчётного ядра

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
