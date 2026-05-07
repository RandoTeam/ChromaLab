package com.chromalab.feature.processing.pipeline

/**
 * Processing pipeline version.
 * Changing this version indicates that results may differ
 * even with the same input and parameters.
 */
const val PIPELINE_VERSION = "1.0.0-alpha"

/**
 * Ordered stages of the digitization pipeline.
 * Each stage must complete before the next can begin.
 */
enum class PipelineStage(val index: Int, val label: String) {
    CAPTURE(0, "Захват"),
    QUALITY_CHECK(1, "Проверка качества"),
    NORMALIZE(2, "Нормализация"),
    CROP(3, "Обрезка"),
    DOCUMENT_DETECT(4, "Определение листа"),
    PERSPECTIVE(5, "Коррекция перспективы"),
    PREPROCESS(6, "Предобработка"),
    GRAPH_REGION(7, "Определение графика"),
    AXIS_DETECT(8, "Определение осей"),
    AXIS_CALIBRATE(9, "Калибровка осей"),
    CURVE_EXTRACT(10, "Извлечение кривой"),
    CURVE_REVIEW(11, "Проверка кривой"),
    SIGNAL_CONVERT(12, "Цифровой сигнал"),
    SIGNAL_SMOOTH(13, "Сглаживание"),
    COMPLETE(14, "Завершено"),
}

/**
 * Validation level of the result.
 * ALL photo-based results start as ESTIMATED until explicitly validated.
 */
enum class ValidationLevel {
    /** Result from photo — preliminary, not validated. */
    ESTIMATED,

    /** Result reviewed and accepted by user. */
    USER_ACCEPTED,

    /** Result validated against reference data. */
    VALIDATED,
}

/**
 * Source of a processing decision (auto vs manual).
 */
enum class DetectionMethod {
    AUTO,
    MANUAL,
    AUTO_CORRECTED,
}
