package com.chromalab.feature.calculation.core

import com.chromalab.feature.processing.signal.DigitalSignal
import kotlin.math.abs

/**
 * Converts Phase 1 DigitalSignal → Phase 2 CalculationSignal with full validation.
 *
 * Pure function: no side effects, no randomness.
 */
object SignalValidator {

    /**
     * Convert and validate a DigitalSignal from Phase 1.
     * Returns the validated CalculationSignal + ValidationResult.
     */
    fun validate(signal: DigitalSignal, sourceId: String): Pair<CalculationSignal, ValidationResult> {
        val points = signal.points.map { p ->
            SignalPoint(
                index = p.index,
                time = p.time.toDouble(),
                intensity = p.intensity.toDouble(),
                pixelX = p.pixelX,
                pixelY = p.pixelY.toDouble(),
                confidence = p.confidence.toDouble(),
                isInterpolated = p.isInterpolated,
            )
        }

        val sorted = points.sortedBy { it.time }
        val isSorted = points == sorted

        // Duplicate time check
        val duplicateTimeCount = sorted.groupBy { it.time }
            .count { it.value.size > 1 }

        // NaN / Infinity check
        val nanCount = sorted.count { it.time.isNaN() || it.intensity.isNaN() }
        val infinityCount = sorted.count {
            it.time.isInfinite() || it.intensity.isInfinite()
        }

        // Negative intensity check
        val negativeIntensityCount = sorted.count { it.intensity < 0.0 }

        // Time spacing analysis
        val timeSteps = if (sorted.size >= 2) {
            sorted.zipWithNext { a, b -> b.time - a.time }
        } else emptyList()

        val avgStep = if (timeSteps.isNotEmpty()) timeSteps.average() else 0.0
        val maxDeviation = if (timeSteps.isNotEmpty() && avgStep > 0.0) {
            timeSteps.maxOf { abs(it - avgStep) } / avgStep
        } else 0.0

        val isUniform = maxDeviation < 0.05 // <5% deviation = uniform

        // Gap detection (missing points where step > 2× average)
        val gapCount = if (avgStep > 0.0) {
            timeSteps.count { it > avgStep * 2.0 }
        } else 0

        // Build warnings
        val warnings = mutableListOf<String>()
        if (!isSorted) warnings.add("Сигнал не отсортирован по времени — исправлено автоматически")
        if (duplicateTimeCount > 0) warnings.add("Обнаружено $duplicateTimeCount дублей по времени")
        if (nanCount > 0) warnings.add("Обнаружено $nanCount точек с NaN")
        if (infinityCount > 0) warnings.add("Обнаружено $infinityCount точек с Infinity")
        if (negativeIntensityCount > 0) warnings.add("Обнаружено $negativeIntensityCount точек с отрицательной интенсивностью")
        if (!isUniform) warnings.add("Шаг времени неравномерный (отклонение ${(maxDeviation * 100).toInt()}%) — используется реальный Δt")
        if (gapCount > 0) warnings.add("Обнаружено $gapCount пропусков во временном ряде")
        if (sorted.size < 20) warnings.add("Мало точек (${sorted.size}) — результат может быть ненадёжным")

        val isValid = nanCount == 0 && infinityCount == 0 && sorted.size >= 10

        val validationResult = ValidationResult(
            isValid = isValid,
            pointCount = sorted.size,
            isSorted = isSorted,
            duplicateTimeCount = duplicateTimeCount,
            gapCount = gapCount,
            nanCount = nanCount,
            infinityCount = infinityCount,
            negativeIntensityCount = negativeIntensityCount,
            isUniformSpacing = isUniform,
            avgTimeStep = avgStep,
            maxTimeStepDeviation = maxDeviation,
            warnings = warnings,
        )

        // Use sorted points (auto-fix sorting)
        val calcSignal = CalculationSignal(
            points = sorted,
            timeUnit = signal.timeUnit,
            intensityUnit = signal.intensityUnit,
            sourceId = sourceId,
        )

        return calcSignal to validationResult
    }
}
