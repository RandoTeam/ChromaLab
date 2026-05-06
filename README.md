# ChromaLab

**Мобильное Android-приложение для анализа хроматограмм**

ChromaLab — offline-first инструмент для оцифровки и расчёта хроматографических данных. Позволяет фотографировать бумажные хроматограммы через встроенную камеру с направляющей рамкой, загружать изображения из галереи, импортировать цифровые файлы (CSV, PDF, mzML) и выполнять детерминированные расчёты пиков, площадей, ion ratio, калибровок.

---

## Назначение

- **Лаборатория:** вспомогательный инструмент для экспресс-анализа хроматограмм с высоким уровнем валидации
- **Обучение:** учебный инструмент для студентов аналитической химии

> ⚠️ Результаты расчётов носят вспомогательный характер и требуют верификации квалифицированным специалистом.

---

## Ключевые возможности

### Источники данных
| Режим | Описание |
|-------|----------|
| 📷 Камера | Съёмка хроматограммы с полупрозрачной рамкой-оверлеем для точного позиционирования |
| 🖼️ Галерея | Загрузка фото/скана с интерактивной подгонкой под рамку |
| 📄 Файлы | Импорт CSV, TXT, PDF, mzML, netCDF |

### Обработка изображений
- Автоматическая коррекция перспективы (OpenCV)
- Определение области графика и осей
- Извлечение кривой хроматограммы
- OCR числовых значений осей (ML Kit)
- Индикатор качества оцифровки
- Ручная коррекция осей и пиков

### Расчёты
- **Базовые:** время удерживания, высота, площадь, ширина пика, S/N
- **Baseline correction:** ALS, SNIP
- **Peak detection:** prominence-based с настраиваемым S/N threshold
- **Ion ratio:** сравнение нескольких ионных каналов, допуски по WADA
- **Калибровка:** линейная/квадратичная регрессия, LOD/LOQ
- **QC:** blank, control, duplicate, spike recovery

### Детерминированность
Каждый расчёт сохраняет полный набор параметров:
```
Algorithm version: 1.0.0
Baseline: ALS (λ=1e6, p=0.01)
Smoothing: Savitzky-Golay (window=11, poly=3)
Peak detection: S/N ≥ 3
Integration: trapezoidal
RT tolerance: ±0.15 min
Ion ratio tolerance: ±20%
```

### Экспорт
- PDF-отчёт с графиком, таблицей пиков, параметрами расчёта
- CSV (данные, пики, результаты)
- JSON (машиночитаемый формат)
- Аудит-лог всех действий

---

## Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Камера | CameraX |
| Обработка изображений | OpenCV Android SDK 4.9+ |
| OCR | ML Kit Text Recognition v2 |
| Расчётное ядро | Kotlin + Apache Commons Math |
| База данных | Room (SQLCipher для шифрования) |
| DI | Hilt |
| Навигация | Navigation Compose |
| Фоновые задачи | Coroutines + WorkManager |
| Экспорт PDF | iText |

---

## Архитектура

```
┌─────────────────────────────────────┐
│            UI Layer                 │
│   Jetpack Compose + ViewModels      │
├─────────────────────────────────────┤
│          Domain Layer               │
│   UseCases + Domain Models          │
├─────────────────────────────────────┤
│           Data Layer                │
│   Room DB │ File I/O │ API (opt)    │
├─────────────────────────────────────┤
│       Processing Engine             │
│   OpenCV │ Peak Detector │ ALS      │
│   Integrator │ Ion Ratio │ Calib    │
└─────────────────────────────────────┘
```

**Паттерн:** Clean Architecture + Repository.  
**Offline-first:** Room DB — единственный источник истины.  
**Опциональный онлайн:** справочные API для идентификации ионов и веществ (PubChem, ChemSpider).

---

## Структура проекта

```
chromalab/
├── app/                        — Application, MainActivity, навигация
├── core/
│   ├── ui/                     — Дизайн-система, общие компоненты
│   ├── data/                   — Room DB, entities, DAOs, repositories
│   ├── domain/                 — UseCases, доменные модели
│   └── common/                 — Утилиты, extensions
├── feature/
│   ├── camera/                 — CameraX, рамка-оверлей, захват
│   ├── processing/             — OpenCV pipeline, извлечение кривой
│   ├── calculation/            — Пики, интеграция, ion ratio, калибровки
│   ├── projects/               — Проекты, образцы, CRUD
│   ├── reports/                — Генерация и экспорт отчётов
│   └── settings/               — Настройки, методы
└── assets/
    └── i18n/                   — Локализация (ru, en, fr, cs, ...)
```

---

## Локализация

| Язык | Код | Статус |
|------|-----|--------|
| Русский | `ru` | Основной |
| English | `en` | Planned |
| Français | `fr` | Planned |
| Čeština | `cs` | Planned |
| Deutsch | `de` | Planned |
| Español | `es` | Planned |
| Italiano | `it` | Planned |
| Polski | `pl` | Planned |

---

## Требования

- Android 8.0+ (API 26+)
- Камера (для режима съёмки)
- ~80 МБ свободного места (включая OpenCV)

---

## Разработка

### Предварительные требования
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android SDK 34
- NDK (для OpenCV)

### Сборка
```bash
git clone https://github.com/AstraVlasta/ChromaLab.git
cd ChromaLab
./gradlew assembleDebug
```

### Тестирование
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

---

## Лицензия

Proprietary. © 2026 Ilia Vlasov. All rights reserved.

---

## Контакт

**Ilia Vlasov** — разработчик  
GitHub: [@AstraVlasta](https://github.com/AstraVlasta)
