package com.chromalab.feature.calculation.flow

/**
 * Calculation analysis steps — the user scenario flow.
 *
 * Flow: open signal → view chart → find peaks → inspect/correct → save → export
 */
enum class AnalysisStep(
    val index: Int,
    val label: String,
) {
    SIGNAL_OVERVIEW(0, "Обзор сигнала"),
    LAYER_SELECTION(1, "Слои графика"),
    PEAK_DETECTION(2, "Поиск пиков"),
    PEAK_REVIEW(3, "Просмотр пиков"),
    PEAK_CORRECTION(4, "Коррекция пиков"),
    NOISE_BASELINE(5, "Шум и baseline"),
    RESULTS(6, "Результаты"),
    EXPORT(7, "Экспорт"),
    ;

    val totalSteps: Int get() = entries.size
    fun next(): AnalysisStep? = entries.getOrNull(ordinal + 1)
    fun prev(): AnalysisStep? = entries.getOrNull(ordinal - 1)

    companion object {
        val FIRST = SIGNAL_OVERVIEW
    }
}
