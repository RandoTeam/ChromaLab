package com.chromalab.feature.processing.flow

/**
 * Auto-advance policy for each pipeline step.
 */
enum class AutoAdvancePolicy {
    /** Always auto-skip — user never sees this step */
    ALWAYS,
    /** Auto-skip only if detection returned high confidence */
    IF_CONFIDENT,
    /** Always show to user — requires conscious confirmation */
    NEVER,
}

/**
 * Processing pipeline steps — the ordered flow through Phase 1.
 *
 * Step order: OCR is moved BEFORE calibration so that OCR results
 * can pre-fill the calibration fields.
 */
enum class ProcessingStep(
    val index: Int,
    val label: String,
    val autoAdvance: AutoAdvancePolicy,
) {
    IMAGE_QUALITY(0, "Качество фото", AutoAdvancePolicy.ALWAYS),
    CROP_REVIEW(1, "Обрезка", AutoAdvancePolicy.ALWAYS),
    PERSPECTIVE(2, "Перспектива", AutoAdvancePolicy.ALWAYS),
    GRAPH_SELECTION(3, "Область графика", AutoAdvancePolicy.IF_CONFIDENT),
    GRAPH_ROI(4, "ROI графика", AutoAdvancePolicy.IF_CONFIDENT),
    AXIS_DETECTION(5, "Определение осей", AutoAdvancePolicy.ALWAYS),
    OCR_SUGGESTION(6, "OCR осей", AutoAdvancePolicy.ALWAYS),
    X_CALIBRATION(7, "Калибровка X", AutoAdvancePolicy.NEVER),
    Y_CALIBRATION(8, "Калибровка Y", AutoAdvancePolicy.NEVER),
    CURVE_EXTRACTION(9, "Извлечение кривой", AutoAdvancePolicy.ALWAYS),
    CURVE_EDITOR(10, "Коррекция кривой", AutoAdvancePolicy.IF_CONFIDENT),
    SIGNAL_PREVIEW(11, "Цифровой график", AutoAdvancePolicy.NEVER),
    QUALITY_REPORT(12, "Качество оцифровки", AutoAdvancePolicy.NEVER),
    EXPORT(13, "Экспорт", AutoAdvancePolicy.NEVER),
    ;

    val totalSteps: Int get() = entries.size

    fun next(): ProcessingStep? = entries.getOrNull(ordinal + 1)
    fun prev(): ProcessingStep? = entries.getOrNull(ordinal - 1)

    companion object {
        val FIRST = IMAGE_QUALITY
    }
}
