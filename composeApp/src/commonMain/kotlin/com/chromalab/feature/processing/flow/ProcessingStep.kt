package com.chromalab.feature.processing.flow

/**
 * Processing pipeline steps — the ordered flow through Phase 1.
 */
enum class ProcessingStep(
    val index: Int,
    val label: String,
) {
    IMAGE_QUALITY(0, "Качество фото"),
    CROP_REVIEW(1, "Обрезка"),
    PERSPECTIVE(2, "Перспектива"),
    GRAPH_SELECTION(3, "Область графика"),
    GRAPH_ROI(4, "ROI графика"),
    AXIS_DETECTION(5, "Определение осей"),
    X_CALIBRATION(6, "Калибровка X"),
    Y_CALIBRATION(7, "Калибровка Y"),
    OCR_SUGGESTION(8, "OCR осей"),
    CURVE_EXTRACTION(9, "Извлечение кривой"),
    CURVE_EDITOR(10, "Коррекция кривой"),
    SIGNAL_PREVIEW(11, "Цифровой график"),
    QUALITY_REPORT(12, "Качество оцифровки"),
    EXPORT(13, "Экспорт"),
    ;

    val totalSteps: Int get() = entries.size

    fun next(): ProcessingStep? = entries.getOrNull(ordinal + 1)
    fun prev(): ProcessingStep? = entries.getOrNull(ordinal - 1)

    companion object {
        val FIRST = IMAGE_QUALITY
    }
}
