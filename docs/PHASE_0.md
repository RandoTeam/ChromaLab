# ChromaLab — Фаза 0: Фундамент

> **Правила проекта:**
> 1. Выполняем строго **ОДИН подпункт** за раз. Следующий — только по прямой команде. Валидация в конце подпункта не считается отдельным шагом.
> 2. **Коммит** (`git commit`) — после каждой подфазы.
> 3. **Push** (`git push`) — только после завершения всей фазы целиком.
> 4. После каждой подфазы — обновление статуса в этом документе и краткий отчёт.

---

## Подфазы

| # | Подфаза | Статус |
|---|---------|--------|
| 0.1 | Инициализация KMP-проекта | ✅ |
| 0.2 | Настройка модулей (пакетов) | ✅ |
| 0.3 | Подключение зависимостей | ✅ |
| 0.4 | Дизайн-система | ✅ |
| 0.5 | Room DB: entities | ⬜ |
| 0.6 | Навигация | ⬜ |
| 0.7 | Локализация | ⬜ |
| 0.8 | CI: build check | ⬜ |

---

## 0.1 — Инициализация KMP-проекта

**Что делаем:**
- Создаём корневой Gradle-проект (Kotlin DSL — `build.gradle.kts`)
- `settings.gradle.kts` с именем проекта `ChromaLab`
- `gradle.properties` (JVM args, AndroidX, Compose, KMP)
- Gradle wrapper (8.11+)
- Подключение плагинов: Kotlin Multiplatform, Compose Multiplatform, AGP
- `composeApp/` — KMP-модуль с targets: `androidMain`, `desktopMain`, `commonMain`
- `composeApp/src/commonMain/` — общий Compose UI + доменная логика
- `composeApp/src/androidMain/` — MainActivity, AndroidManifest, platform-specific код
- `composeApp/src/desktopMain/` — main() для Desktop (JVM)
- Минимальный `App.kt` в `commonMain` с Compose `MaterialTheme {}`
- Минимальный `Theme.kt` в `commonMain`

**Почему KMP:**
- Расчётное ядро (математика) — пишем один раз в `commonMain`, работает на Android + Desktop
- UI (Compose Multiplatform) — единый UI для Android и Desktop
- Room — поддерживает KMP с 2025
- Камера/OpenCV — остаются в `androidMain` (platform-specific)
- В 2026 KMP + CMP полностью stable для Android и Desktop

**Параметры:**
```
applicationId = "com.chromalab.app"
minSdk = 26  (Android 8.0)
targetSdk = 35
compileSdk = 35
versionCode = 1
versionName = "0.1.0"
kotlinVersion = 2.3.21
composeMultiplatformVersion = 1.10.0
agpVersion = 9.2.1
jvmTarget = 17
```

**Структура после инициализации:**
```
ChromaLab/
├── build.gradle.kts              ← корневой (плагины)
├── settings.gradle.kts           ← include modules
├── gradle.properties
├── gradle/
│   └── libs.versions.toml        ← version catalog
└── composeApp/
    ├── build.gradle.kts          ← KMP + CMP конфигурация
    └── src/
        ├── commonMain/kotlin/com/chromalab/
        │   └── App.kt            ← общий Compose entry point
        ├── androidMain/kotlin/com/chromalab/
        │   └── MainActivity.kt   ← Android entry
        ├── androidMain/AndroidManifest.xml
        └── desktopMain/kotlin/com/chromalab/
            └── Main.kt           ← Desktop entry (JVM)
```

**Валидация:**
- `./gradlew :composeApp:assembleDebug` — Android собирается
- `./gradlew :composeApp:run` — Desktop запускается

---

## 0.2 — Настройка модулей

**Что делаем:**
Создаём Gradle-модули для Clean Architecture.

```
settings.gradle.kts:
  include(":app")
  include(":core:ui")
  include(":core:data")
  include(":core:domain")
  include(":core:common")
  include(":feature:camera")
  include(":feature:processing")
  include(":feature:calculation")
  include(":feature:projects")
  include(":feature:reports")
  include(":feature:settings")
```

**Каждый модуль:**
- `build.gradle.kts` (android library для core/feature, application для app)
- `src/main/AndroidManifest.xml` (минимальный)
- `src/main/kotlin/com/chromalab/<module>/` (пустой пакет)

**Граф зависимостей модулей:**
```
app → feature:* → core:domain → core:common
                → core:data   → core:common
                → core:ui     → core:common
```

**Валидация:** `./gradlew assembleDebug` собирается со всеми модулями.

---

## 0.3 — Подключение зависимостей

**Что делаем:**
Создаём `libs.versions.toml` (Version Catalog) и подключаем библиотеки.

**Зависимости по модулям:**

| Библиотека | Версия | Модуль |
|-----------|--------|--------|
| Jetpack Compose BOM | 2025.01.01 | core:ui, app |
| Material 3 | BOM | core:ui |
| Navigation Compose | 2.8+ | app |
| Room (runtime + compiler) | 2.7+ | core:data |
| Hilt | 2.52+ | app, feature:* |
| CameraX (camera2, lifecycle, view) | 1.4+ | feature:camera |
| ML Kit Text Recognition | 16.0+ | feature:processing |
| OpenCV Android SDK | 4.9+ | feature:processing |
| Apache Commons Math | 3.6.1 | feature:calculation |
| iText Android | 7.2+ | feature:reports |
| Kotlin Coroutines | 1.9+ | core:common |
| Kotlinx Serialization | 1.7+ | core:common |
| Coil (Compose) | 2.7+ | core:ui |
| JUnit 5 + MockK | latest | all (test) |

**Валидация:** `./gradlew dependencies` без конфликтов, `assembleDebug` ok.

---

## 0.4 — Дизайн-система

**Что делаем:**
`core:ui` — тема, цвета, типографика, базовые компоненты.

### Цветовая палитра

Концепция: **«Лабораторный тёмный»** — тёмный фон с яркими акцентами для данных (как осциллограф / научное ПО).

**Тёмная тема (основная):**

| Роль | Цвет | HEX | Использование |
|------|------|-----|--------------|
| Background | Графит | `#0F1318` | Фон экранов |
| Surface | Тёмно-серый | `#1A1F27` | Карточки, панели |
| Surface Variant | Серый | `#252B35` | Второстепенные поверхности |
| Primary | Бирюзовый | `#00D4AA` | Кнопки, акценты, ссылки |
| Primary Container | Тёмный бирюзовый | `#003D32` | Фон выделенных элементов |
| Secondary | Голубой | `#64B5F6` | Ion channel 1, графики |
| Tertiary | Янтарный | `#FFB74D` | Ion channel 2, предупреждения |
| Error | Красный | `#EF5350` | Ошибки, NOT_CONFIRMED |
| Success | Зелёный | `#66BB6A` | CONFIRMED, успех |
| Warning | Жёлтый | `#FDD835` | DOUBTFUL, предупреждения |
| On Background | Белый | `#E8ECF0` | Основной текст |
| On Surface | Светло-серый | `#C5CBD3` | Вторичный текст |
| Outline | Серый | `#3A4250` | Разделители, границы |

**Светлая тема (дополнительная):**

| Роль | Цвет | HEX |
|------|------|-----|
| Background | Белый | `#FAFCFE` |
| Surface | Светло-серый | `#F0F3F7` |
| Primary | Бирюзовый тёмный | `#00897B` |
| On Background | Чёрный | `#1A1F27` |

**Цвета для графиков (фиксированные, не зависят от темы):**

| Назначение | HEX | Когда |
|-----------|-----|-------|
| Кривая (channel 1) | `#64B5F6` | Основной ионный канал |
| Кривая (channel 2) | `#FFB74D` | Второй канал |
| Кривая (channel 3) | `#CE93D8` | Третий канал |
| Baseline | `#EF5350` (dashed) | Базовая линия |
| Peak fill | `#00D4AA` (alpha 30%) | Заливка пика |
| Peak boundary | `#00D4AA` (alpha 60%) | Границы пика |
| Selected peak | `#FFFFFF` | Выбранный пик |

### Типографика

**Шрифт:** `Inter` (Google Fonts) — чистый, читаемый, научный.
**Моноширинный:** `JetBrains Mono` — для числовых данных и параметров.

| Стиль | Размер | Weight | Использование |
|-------|--------|--------|--------------|
| Display Large | 36sp | 700 | — |
| Headline Large | 28sp | 700 | Заголовок экрана |
| Headline Medium | 24sp | 600 | Заголовок секции |
| Title Large | 20sp | 600 | Заголовок карточки |
| Title Medium | 16sp | 600 | Подзаголовок |
| Body Large | 16sp | 400 | Основной текст |
| Body Medium | 14sp | 400 | Вторичный текст |
| Body Small | 12sp | 400 | Подписи |
| Label Large | 14sp | 600 | Кнопки |
| Label Medium | 12sp | 500 | Чипы, теги |
| Label Small | 10sp | 500 | Мелкие подписи |
| Data Mono | 14sp | 400 | Числовые значения (JetBrains Mono) |

### Отступы и размеры

| Токен | Значение | Использование |
|-------|----------|--------------|
| spacing.xs | 4dp | Минимальный зазор |
| spacing.sm | 8dp | Внутренний padding мелких элементов |
| spacing.md | 16dp | Стандартный padding |
| spacing.lg | 24dp | Между секциями |
| spacing.xl | 32dp | Большие отступы |
| radius.sm | 8dp | Мелкие элементы (чипы) |
| radius.md | 12dp | Карточки |
| radius.lg | 16dp | Модальные окна |
| elevation.card | 2dp | Карточки |
| elevation.modal | 8dp | Диалоги |

### Базовые компоненты (core:ui)

| Компонент | Описание |
|-----------|----------|
| `ChromaLabTheme` | Обёртка MaterialTheme с палитрой и типографикой |
| `CLTopBar` | TopAppBar с заголовком, навигацией, действиями |
| `CLCard` | Surface с radius.md, elevation, padding |
| `CLButton` | FilledTonalButton в стиле Primary |
| `CLOutlinedButton` | OutlinedButton |
| `CLIconButton` | Иконочная кнопка |
| `CLTextField` | OutlinedTextField со стилем |
| `CLChip` | AssistChip / FilterChip |
| `CLStatusBadge` | Индикатор статуса (🟢🟡🔴) |
| `CLEmptyState` | Заглушка для пустых списков |
| `CLLoadingOverlay` | Полупрозрачный overlay с прогрессом |

### Иконки

Material Symbols Outlined. Ключевые:
- `photo_camera` — камера
- `image` — галерея
- `upload_file` — импорт файла
- `analytics` — расчёты
- `science` — проекты
- `description` — отчёты
- `settings` — настройки
- `tune` — параметры
- `check_circle` — подтверждено
- `warning` — сомнительно
- `cancel` — не подтверждено

**Валидация:** тема применяется, компоненты рендерятся в Preview.

---

## 0.5 — Room DB: entities

**Что делаем:**
`core:data` — все Entity, DAO, Database, TypeConverters.

### Entities

**Project:**
```
id: Long (PK, autoGenerate)
name: String
clientName: String?
date: Long (epoch ms)
methodology: String?
notes: String?
createdAt: Long
updatedAt: Long
```

**Sample:**
```
id: Long (PK, autoGenerate)
projectId: Long (FK → Project)
name: String
vial: String?
analysisDate: Long?
operator: String?
instrument: String?
method: String?
matrix: String?
createdAt: Long
updatedAt: Long
```

**Chromatogram:**
```
id: Long (PK, autoGenerate)
sampleId: Long (FK → Sample)
sourceType: SourceType (PHOTO, GALLERY, PDF, CSV, MZML, MANUAL)
filePath: String?
ionChannel: String?
timeRangeStart: Double?
timeRangeEnd: Double?
intensityUnit: String?
qualityScore: Float?
dataPoints: String? (JSON: массив [time, intensity])
algorithmConfig: String? (JSON)
createdAt: Long
updatedAt: Long
```

**Peak:**
```
id: Long (PK, autoGenerate)
chromatogramId: Long (FK → Chromatogram)
peakNumber: Int
retentionTime: Double
startTime: Double
endTime: Double
height: Double
area: Double
width: Double
snRatio: Double?
integrationStatus: IntegrationStatus (AUTO, MANUAL, EDITED)
createdAt: Long
```

**Calculation:**
```
id: Long (PK, autoGenerate)
sampleId: Long (FK → Sample)
type: CalculationType (ION_RATIO, QUANTITATIVE, QUALITATIVE, QC)
formula: String?
result: Double?
units: String?
tolerance: Double?
status: ResultStatus (CONFIRMED, DOUBTFUL, NOT_CONFIRMED, PENDING)
algorithmConfig: String? (JSON)
createdAt: Long
```

**AuditEntry:**
```
id: Long (PK, autoGenerate)
entityType: String
entityId: Long
action: AuditAction (CREATE, UPDATE, DELETE)
field: String?
oldValue: String?
newValue: String?
reason: String?
timestamp: Long
```

### DAOs
- `ProjectDao` — CRUD + getAll(Flow), getById, search
- `SampleDao` — CRUD + getByProjectId(Flow)
- `ChromatogramDao` — CRUD + getBySampleId(Flow)
- `PeakDao` — CRUD + getByChromaId(Flow)
- `CalculationDao` — CRUD + getBySampleId(Flow)
- `AuditDao` — insert, getByEntity(Flow)

### Database
- `ChromaLabDatabase` — version 1, exportSchema = true

### TypeConverters
- `SourceType` ↔ String
- `IntegrationStatus` ↔ String
- `CalculationType` ↔ String
- `ResultStatus` ↔ String
- `AuditAction` ↔ String

**Валидация:** компиляция Room без ошибок, schema export генерируется.

---

## 0.6 — Навигация

**Что делаем:**
Navigation Compose с type-safe routes.

### Bottom Navigation (4 таба)

| Таб | Иконка | Route | Экран |
|-----|--------|-------|-------|
| Проекты | `science` | `projects` | ProjectListScreen |
| Съёмка | `photo_camera` | `capture` | CaptureScreen (выбор режима) |
| Расчёты | `analytics` | `calculations` | CalculationsScreen |
| Ещё | `more_horiz` | `more` | MoreScreen (отчёты, настройки) |

### Полный граф навигации

```
BottomNavHost
├── projects/
│   ├── list                    → ProjectListScreen
│   ├── {projectId}             → ProjectDetailScreen
│   ├── {projectId}/samples/{sampleId} → SampleDetailScreen
│   └── new                     → NewProjectScreen
│
├── capture/
│   ├── mode                    → CaptureModePicker (камера / галерея / файл)
│   ├── camera                  → CameraScreen
│   ├── gallery                 → GalleryFrameScreen
│   ├── import                  → FileImportScreen
│   └── processing/{imageUri}   → ProcessingScreen
│
├── calculations/
│   ├── list                    → CalculationListScreen
│   ├── {chromatogramId}        → ChromatogramViewScreen
│   ├── ion-ratio               → IonRatioScreen
│   └── calibration             → CalibrationScreen
│
└── more/
    ├── reports                 → ReportListScreen
    ├── settings                → SettingsScreen
    └── about                   → AboutScreen
```

### Анимации переходов
- Горизонтальный slide для forward/back
- Fade для bottom nav switch
- Shared element transitions для карточка → детали (Compose 1.7+)

**Валидация:** навигация между всеми табами и экранами-заглушками работает.

---

## 0.7 — Локализация

**Что делаем:**
`values/strings.xml` (ru — default), `values-en/strings.xml`.

### Ключевые строки (первая партия)

| Ключ | RU | EN |
|------|----|----|
| app_name | ChromaLab | ChromaLab |
| tab_projects | Проекты | Projects |
| tab_capture | Съёмка | Capture |
| tab_calculations | Расчёты | Calculations |
| tab_more | Ещё | More |
| btn_new_project | Новый проект | New Project |
| btn_take_photo | Сфотографировать | Take Photo |
| btn_from_gallery | Из галереи | From Gallery |
| btn_import_file | Импорт файла | Import File |
| btn_calculate | Рассчитать | Calculate |
| btn_export | Экспорт | Export |
| label_retention_time | Время удерживания | Retention Time |
| label_peak_area | Площадь пика | Peak Area |
| label_peak_height | Высота пика | Peak Height |
| label_sn_ratio | S/N | S/N |
| label_ion_ratio | Ионное отношение | Ion Ratio |
| status_confirmed | Подтверждено | Confirmed |
| status_doubtful | Сомнительно | Doubtful |
| status_not_confirmed | Не подтверждено | Not Confirmed |
| quality_good | Хорошее качество | Good Quality |
| quality_medium | Среднее качество | Medium Quality |
| quality_poor | Низкое качество | Poor Quality |
| empty_projects | Нет проектов | No Projects |
| empty_samples | Нет образцов | No Samples |
| hint_place_chromatogram | Поместите хроматограмму в рамку | Place chromatogram in frame |

**Валидация:** переключение языка системы → текст меняется.

---

## 0.8 — CI: build check

**Что делаем:**
GitHub Actions workflow для автоматической проверки сборки.

### `.github/workflows/build.yml`
```yaml
Trigger: push to master, pull_request
Steps:
  1. checkout
  2. setup JDK 17
  3. setup Gradle cache
  4. ./gradlew assembleDebug
  5. ./gradlew test
```

**Валидация:** пуш в master → зелёная галочка в GitHub.

---

## Дизайн экранов (ключевые для Фазы 0)

Во время Фазы 0 создаются экраны-заглушки (placeholder), но с правильной дизайн-системой.

### Главный экран (Bottom Navigation)

```
┌──────────────────────────────┐
│  ChromaLab            [⚙️]   │  ← TopBar
├──────────────────────────────┤
│                              │
│  Контент текущего таба       │
│                              │
│                              │
│                              │
├──────────────────────────────┤
│ 🔬Проекты │📷Съёмка│📊Расчёт│•••│  ← BottomNav
└──────────────────────────────┘
```

### ProjectListScreen (заглушка)

```
┌──────────────────────────────┐
│  Проекты              [🔍]   │
├──────────────────────────────┤
│                              │
│       🔬                     │
│   Нет проектов               │
│   Создайте первый проект     │
│                              │
│   [ + Новый проект ]         │
│                              │
├──────────────────────────────┤
│ 🔬       │ 📷     │ 📊  │ ••• │
└──────────────────────────────┘
```

### CaptureModePicker

```
┌──────────────────────────────┐
│  Съёмка                      │
├──────────────────────────────┤
│                              │
│  ┌────────────────────────┐  │
│  │  📷  Сфотографировать  │  │  ← CLCard, primary accent
│  │  Камера с рамкой       │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │  🖼️  Из галереи        │  │  ← CLCard
│  │  Загрузить фото/скан   │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │  📄  Импорт файла      │  │  ← CLCard
│  │  CSV, PDF, mzML        │  │
│  └────────────────────────┘  │
│                              │
├──────────────────────────────┤
│ 🔬       │ 📷     │ 📊  │ ••• │
└──────────────────────────────┘
```

Каждая карточка: Surface с `radius.md`, `elevation.card`, иконка слева, текст справа, ripple эффект.
