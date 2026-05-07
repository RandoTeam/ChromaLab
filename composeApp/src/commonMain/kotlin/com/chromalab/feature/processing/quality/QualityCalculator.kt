package com.chromalab.feature.processing.quality

import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.signal.DigitalSignal

/**
 * Calculates quality scores for each pipeline stage.
 * All thresholds are explicitly defined — no hidden heuristics.
 */
object QualityCalculator {

    /**
     * Image quality from sharpness/contrast scores (from ImageQualityAnalyzer).
     */
    fun imageQuality(sharpness: Float, contrast: Float): StageQuality {
        val warnings = mutableListOf<String>()
        if (sharpness < 30f) warnings.add("Низкая резкость фото (${sharpness.toInt()})")
        if (contrast < 40f) warnings.add("Низкий контраст (${contrast.toInt()})")

        val score = ((sharpness / 100f).coerceIn(0f, 1f) * 0.6f +
            (contrast / 100f).coerceIn(0f, 1f) * 0.4f)

        return StageQuality("image", score, warnings)
    }

    /**
     * Document detection quality.
     */
    fun documentDetection(
        detected: Boolean,
        perspectiveAngle: Float,
        hasGlare: Boolean,
        unevenLighting: Boolean,
    ): StageQuality {
        val warnings = mutableListOf<String>()
        var score = if (detected) 1f else 0.3f

        if (!detected) warnings.add("Документ не определён автоматически")
        if (perspectiveAngle > 15f) {
            warnings.add("Сильная перспектива (${perspectiveAngle.toInt()}°)")
            score *= 0.7f
        }
        if (hasGlare) {
            warnings.add("Обнаружены блики")
            score *= 0.8f
        }
        if (unevenLighting) {
            warnings.add("Неравномерное освещение")
            score *= 0.85f
        }

        return StageQuality("document", score.coerceIn(0f, 1f), warnings)
    }

    /**
     * Graph region detection quality.
     */
    fun graphDetection(
        autoDetected: Boolean,
        graphCount: Int,
    ): StageQuality {
        val warnings = mutableListOf<String>()
        var score = if (autoDetected) 0.9f else 0.5f

        if (!autoDetected) warnings.add("Область графика выбрана вручную")
        if (graphCount > 1) warnings.add("Найдено несколько графиков ($graphCount)")

        return StageQuality("graph", score, warnings)
    }

    /**
     * Axis calibration quality.
     */
    fun axisCalibration(
        xCalibrated: Boolean,
        yCalibrated: Boolean,
        ocrUsed: Boolean,
        ocrCorrected: Boolean,
        manualAxes: Boolean,
    ): StageQuality {
        val warnings = mutableListOf<String>()
        var score = 1f

        if (!xCalibrated || !yCalibrated) {
            score = 0.1f
            warnings.add("Оси не откалиброваны")
        }
        if (!ocrUsed) {
            warnings.add("OCR не использован")
            score *= 0.9f
        }
        if (ocrCorrected) {
            warnings.add("OCR исправлен пользователем")
            // Not a penalty — correction improves quality
        }
        if (manualAxes) {
            warnings.add("Оси заданы вручную")
            score *= 0.85f
        }

        return StageQuality("calibration", score.coerceIn(0f, 1f), warnings)
    }

    /**
     * Curve extraction quality.
     */
    fun curveExtraction(result: CurveExtractionResult): StageQuality {
        val warnings = mutableListOf<String>()
        var score = result.coverage

        if (result.interpolatedColumns > result.totalColumns * 0.2f) {
            warnings.add("Часть кривой интерполирована (${(result.interpolatedColumns * 100f / result.totalColumns.coerceAtLeast(1)).toInt()}%)")
            score *= 0.8f
        }
        if (result.outlierCount > 20) {
            warnings.add("Найдено несколько кандидатов линии (выбросов: ${result.outlierCount})")
            score *= 0.85f
        }
        if (!result.isUsable) {
            warnings.add("Качество оцифровки недостаточно для надёжного расчёта")
            score *= 0.3f
        }

        val gapWarnings = result.warnings
        warnings.addAll(gapWarnings)

        return StageQuality("curve", score.coerceIn(0f, 1f), warnings)
    }

    /**
     * Signal-level quality checks.
     */
    fun signalQuality(signal: DigitalSignal): StageQuality {
        val warnings = mutableListOf<String>()
        var score = 1f

        if (signal.metadata.gapCount > 0) {
            warnings.add("Найдены разрывы кривой (${signal.metadata.gapCount})")
            score *= 0.8f
        }
        if (signal.interpolatedCount > signal.points.size * 0.3f) {
            warnings.add("Значительная часть данных интерполирована")
            score *= 0.7f
        }
        if (signal.highConfidenceRatio < 0.5f) {
            warnings.add("Менее 50% точек с высокой уверенностью")
            score *= 0.6f
        }

        return StageQuality("signal", score.coerceIn(0f, 1f), warnings)
    }

    /**
     * Overall quality: weighted average of all stage scores.
     */
    fun overall(
        image: StageQuality,
        document: StageQuality,
        graph: StageQuality,
        calibration: StageQuality,
        curve: StageQuality,
    ): StageQuality {
        val weightedScore = (
            image.score * 0.10f +
                document.score * 0.10f +
                graph.score * 0.15f +
                calibration.score * 0.25f +
                curve.score * 0.40f
            )

        val allWarnings = listOf(image, document, graph, calibration, curve)
            .flatMap { it.warnings }

        return StageQuality("overall", weightedScore.coerceIn(0f, 1f), allWarnings)
    }

    /**
     * Build complete report from all stages.
     */
    fun buildReport(
        image: StageQuality,
        document: StageQuality,
        graph: StageQuality,
        calibration: StageQuality,
        curve: StageQuality,
    ): DigitizationQualityReport {
        val overallStage = overall(image, document, graph, calibration, curve)
        return DigitizationQualityReport(
            imageQuality = image,
            documentDetection = document,
            graphDetection = graph,
            axisCalibration = calibration,
            curveExtraction = curve,
            overall = overallStage,
            timestamp = System.currentTimeMillis(),
        )
    }
}
