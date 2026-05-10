# ChromaLab — Технический Pipeline

Полный цикл обработки данных: от ввода хроматограммы до генерации отчёта.

---

## Общая схема

```
ВВОД → ОБРАБОТКА ИЗОБРАЖЕНИЯ → РАСЧЁТ → ВЫВОД → ХРАНЕНИЕ
```

---

## Pipeline 1: Фото (камера / галерея)

### 1.1 Захват
- **Камера:** CameraX Preview → OverlayView (рамка) → ImageCapture → crop по координатам рамки
- **Галерея:** Intent → URI → экран подгонки под рамку → crop

### 1.2 Коррекция перспективы
1. GaussianBlur → Canny edge → findContours → approxPolyDP (4 угла)
2. getPerspectiveTransform → warpPerspective
3. **Fallback:** ручная расстановка 4 углов

### 1.3 Предобработка
1. Grayscale → CLAHE (контраст) → adaptiveThreshold (бинаризация)
2. morphologyEx: CLOSE (закрытие разрывов) + OPEN (удаление шума)

### 1.4 Определение области графика
- Hough lines → кластеризация → оси X/Y → bounding box
- **Fallback:** пользователь тапает 4 угла графика

### 1.5 OCR осей
- ML Kit Text Recognition → числа на осях + заголовки ("Ion 217.00", "Abundance")
- Линейная интерполяция pixel → real units
- **Fallback:** пользователь вводит min/max вручную

### 1.6 Извлечение кривой
- **Column scan:** для каждого столбца → centroid чёрных пикселей → точка (x, y)
- **Contour:** findContours → самый длинный → упорядочение по x
- Savitzky-Golay сглаживание (window=11, polyorder=3)
- Калибровка pixel → real units

### 1.7 Оценка качества
| Метрика | 🟢 Хорошо | 🟡 Средне | 🔴 Плохо |
|---------|-----------|-----------|----------|
| Контрастность | > 0.7 | 0.4–0.7 | < 0.4 |
| Резкость | > 100 | 50–100 | < 50 |
| Наклон | < 2° | 2–5° | > 5° |
| Полнота точек | > 90% | 60–90% | < 60% |

---

## Pipeline 2: Цифровые файлы

### CSV / TXT
Auto-detect разделителя → парсинг колонок (time, intensity) → валидация

### PDF
- Векторный: извлечение path elements → координаты кривых
- Растровый: рендеринг → Pipeline 1

### mzML / netCDF
XML/HDF парсинг → выбор хроматограммы (TIC/XIC/SIM) → массив (time, intensity)

---

## Pipeline 3: Расчёт

Одинаковый для всех источников. Работает с массивом `(time, intensity)`.

### 3.1 Baseline correction
- **ALS:** λ=1e6, p=0.01, до 50 итераций
- **SNIP:** 40 итераций (альтернатива)
- `corrected = intensity - baseline`

### 3.2 Peak detection
1. Noise: σ = MAD(corrected) / 0.6745
2. Threshold: min_height = S/N_min × σ (default S/N ≥ 3)
3. find_peaks → для каждого: RT, height, left_base, right_base, width, S/N

### 3.3 Integration
- Трапецеидальное правило: `area = Σ (y[i]+y[i+1])/2 × Δx`
- В границах [left_base, right_base] на baseline-corrected сигнале

### 3.4 Ion ratio
1. RT matching: |RT_sample − RT_ref| ≤ tolerance
2. IR = area_qualifier / area_quantifier × 100%
3. Deviation vs reference → CONFIRMED / DOUBTFUL / NOT_CONFIRMED

### 3.5 Калибровка
- Линейная/квадратичная регрессия (с весами 1/x, 1/x²)
- R², LOD = 3.3σ/slope, LOQ = 10σ/slope

### 3.6 Статистика распределения (Phase 13)
- **Индекс доминирования:** Area(max) / Σ Area — монокомпонентность
- **Индекс Шеннона (H'):** −Σ(pᵢ · ln(pᵢ)) — разнообразие состава
- **Равномерность Пиелу (J):** H / ln(N) — однородность распределения
- **Средневзвешенный RT:** Σ(RTᵢ × Areaᵢ) / Σ Area — центр масс
- **Медианный RT:** RT при 50% кумулятивной площади
- **Асимметрия / Эксцесс:** Skewness, Kurtosis по площадям
- **Плотность пиков:** N / (RT_max − RT_min) пиков/мин
- Все метрики с автоматической текстовой интерпретацией

### 3.7 Паттерн-анализ (Phase 14)
- **Гомологический ряд:** CV(ΔRT) < 0.15 = HIGH, < 0.30 = MEDIUM
- **Чёт/нечёт преобладание:** Σ Area(нечёт) / Σ Area(чёт) — зрелость ОВ
- **UCM (неразрешённая смесь):** (total_area − resolved) / total_area — биодеградация
- **Огибающая:** UNIMODAL / BIMODAL / FLAT / DECREASING — форма распределения
- **Кластеризация:** ΔRT < mean×0.5 → кластер, иначе изолированный пик

### 3.8 Качество метода (Phase 15)
- **Средние теор. тарелки (N̄):** эффективность колонки
- **Пиковая ёмкость (nc):** 1 + (√N̄ / 4) × ln(t_last/t_first)
- **Использование ёмкости:** N_peaks / nc × 100%
- **Средняя Rs, % Rs < 1.5:** качество разделения
- **Средний Tailing, % T > 2.0:** симметрия пиков
- **Drift baseline, RMS шума:** стабильность детектора
- **Оценка:** EXCELLENT (≥7 баллов) / GOOD (≥4) / ACCEPTABLE (≥2) / POOR

---

## Pipeline 4: Отчёт

1. Сборка: метаданные + график + таблица пиков + ion ratio + параметры алгоритма
2. Генерация: PDF (iText), CSV, JSON
3. Метаданные: SHA-256 hash входных данных, algorithm_version, timestamp

---

## Производительность (оценка)

| Операция | Время |
|----------|-------|
| Захват фото | < 1 сек |
| Perspective warp + предобработка | 1–3 сек |
| OCR осей | 1–3 сек |
| Извлечение кривой | 0.5–2 сек |
| Baseline + Peaks + Integration | 0.1–0.5 сек |
| PDF генерация | 1–3 сек |
| **Итого (фото → отчёт)** | **3–12 сек** |

Все операции — вне UI-потока (Coroutines). Прогресс отображается пользователю.

---

## Детерминированность

Каждый расчёт сохраняет конфиг:
```json
{
  "algorithm_version": "1.0.0",
  "baseline_method": "ALS",
  "baseline_params": {"lambda": 1e6, "p": 0.01},
  "smoothing": {"method": "Savitzky-Golay", "window": 11, "polyorder": 3},
  "peak_detection": {"min_sn": 3, "min_prominence": "auto"},
  "integration": "trapezoidal",
  "rt_tolerance_min": 0.15,
  "ion_ratio_tolerance_pct": 20
}
```

Одинаковые входные данные + одинаковый конфиг = одинаковый результат. Всегда.
