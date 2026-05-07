package com.chromalab.feature.calculation.algorithm

/**
 * Peak confidence calculator (§2.21).
 *
 * Computes an overall confidence score for a peak result
 * based on multiple quality factors.
 *
 * Factors:
 * - S/N quality
 * - Baseline quality
 * - Boundary confidence
 * - Overlap status
 * - Manual edits
 * - Negative area fraction
 *
 * Output: score 0..1 + grade (high/medium/low/failed) + reasons list.
 * Reasons are NEVER hidden — user always sees why confidence is low.
 *
 * Pure function: deterministic, no side effects.
 */
object PeakConfidenceCalculator {

    /**
     * Calculate confidence for a peak.
     *
     * @param metrics        Peak metrics
     * @param baselineGrade  Baseline quality grade
     * @param negativeAreaFraction  |negativeArea| / positiveArea
     * @return PeakConfidence with score, grade, and reason list
     */
    fun calculate(
        metrics: PeakMetrics,
        baselineGrade: BaselineGrade = BaselineGrade.GOOD,
        negativeAreaFraction: Double = 0.0,
    ): PeakConfidence {
        val factors = mutableListOf<ConfidenceFactor>()

        // 1. S/N factor (weight: 0.25)
        val snrScore = when (metrics.snrFlag) {
            SnrFlag.QUANTITATION -> 1.0
            SnrFlag.DETECTABLE -> 0.6
            SnrFlag.LOW -> 0.2
        }
        factors.add(ConfidenceFactor("S/N", snrScore, 0.25, snrReason(metrics.snrFlag)))

        // 2. Baseline quality (weight: 0.20)
        val baselineScore = when (baselineGrade) {
            BaselineGrade.GOOD -> 1.0
            BaselineGrade.ACCEPTABLE -> 0.7
            BaselineGrade.RISKY -> 0.3
            BaselineGrade.FAILED -> 0.0
        }
        factors.add(ConfidenceFactor("Baseline", baselineScore, 0.20, baselineReason(baselineGrade)))

        // 3. Boundary confidence (weight: 0.20)
        val boundaryScore = metrics.boundaryConfidence.coerceIn(0.0, 1.0)
        factors.add(ConfidenceFactor(
            "Границы", boundaryScore, 0.20,
            if (boundaryScore < 0.5) "Низкая уверенность в границах пика" else null
        ))

        // 4. Overlap status (weight: 0.20)
        val overlapScore = when (metrics.overlapStatus) {
            OverlapStatus.ISOLATED -> 1.0
            OverlapStatus.PARTIALLY_OVERLAPPED -> 0.6
            OverlapStatus.SHOULDER -> 0.3
            OverlapStatus.UNRESOLVED -> 0.1
        }
        factors.add(ConfidenceFactor("Перекрытие", overlapScore, 0.20, overlapReason(metrics.overlapStatus)))

        // 5. Manual edits (weight: 0.05)
        val manualScore = if (metrics.isManuallyEdited) 0.7 else 1.0
        factors.add(ConfidenceFactor(
            "Ручные правки", manualScore, 0.05,
            if (metrics.isManuallyEdited) "Пик изменён вручную — уверенность снижена" else null
        ))

        // 6. Negative area (weight: 0.10)
        val negAreaScore = when {
            negativeAreaFraction < 0.01 -> 1.0
            negativeAreaFraction < 0.05 -> 0.7
            negativeAreaFraction < 0.20 -> 0.4
            else -> 0.1
        }
        factors.add(ConfidenceFactor(
            "Отрицательная площадь", negAreaScore, 0.10,
            if (negativeAreaFraction > 0.05) {
                "Отрицательная площадь ${(negativeAreaFraction * 100).toInt()}% — проверьте baseline"
            } else null
        ))

        // Weighted score
        val totalWeight = factors.sumOf { it.weight }
        val score = if (totalWeight > 0) {
            factors.sumOf { it.score * it.weight } / totalWeight
        } else 0.0

        // Grade
        val grade = when {
            score >= 0.8 -> ConfidenceGrade.HIGH
            score >= 0.5 -> ConfidenceGrade.MEDIUM
            score >= 0.2 -> ConfidenceGrade.LOW
            else -> ConfidenceGrade.FAILED
        }

        // Collect reasons (never hidden)
        val reasons = factors.mapNotNull { it.reason }

        return PeakConfidence(
            score = score,
            grade = grade,
            factors = factors,
            reasons = reasons,
        )
    }

    private fun snrReason(flag: SnrFlag): String? = when (flag) {
        SnrFlag.LOW -> "S/N < 3 — сигнал может быть неотличим от шума"
        SnrFlag.DETECTABLE -> "S/N 3–10 — количественное определение ненадёжно"
        SnrFlag.QUANTITATION -> null
    }

    private fun baselineReason(grade: BaselineGrade): String? = when (grade) {
        BaselineGrade.GOOD -> null
        BaselineGrade.ACCEPTABLE -> "Baseline приемлема, но есть незначительные артефакты"
        BaselineGrade.RISKY -> "Baseline сомнительна — результаты могут быть неточными"
        BaselineGrade.FAILED -> "Baseline ошибочна — результаты ненадёжны"
    }

    private fun overlapReason(status: OverlapStatus): String? = when (status) {
        OverlapStatus.ISOLATED -> null
        OverlapStatus.PARTIALLY_OVERLAPPED -> "Пик частично перекрыт соседним"
        OverlapStatus.SHOULDER -> "Плечевой пик — площадь ненадёжна"
        OverlapStatus.UNRESOLVED -> "Неразрешённый пик — требуется ручная коррекция"
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class ConfidenceGrade(val label: String) {
    HIGH("Высокая"),
    MEDIUM("Средняя"),
    LOW("Низкая"),
    FAILED("Ошибка"),
}

data class ConfidenceFactor(
    val name: String,
    val score: Double,
    val weight: Double,
    val reason: String?,
)

data class PeakConfidence(
    val score: Double,
    val grade: ConfidenceGrade,
    val factors: List<ConfidenceFactor>,
    val reasons: List<String>,
)
