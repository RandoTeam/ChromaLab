package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlinx.serialization.Serializable

/**
 * Baseline correction common module (§2.9).
 *
 * Architecture:
 * - BaselineEstimator interface for each method
 * - BaselineMethod enum for selection
 * - BaselineResult wraps array + params + warnings
 * - BaselineDispatcher routes to the correct estimator
 *
 * Rules:
 * - ALS = primary automatic method
 * - Manual Linear = transparent control method
 * - SNIP = alternative / experimental method
 * - NONE = no baseline correction
 * - One method active per CalculationRun
 * - User can compare methods via BaselineComparisonScreen
 */

// ─── Method Enum ────────────────────────────────────────────────

enum class BaselineMethod(val label: String, val description: String) {
    NONE("Без коррекции", "Baseline = 0"),
    MANUAL_LINEAR("Линейная", "Прямая между выбранными точками"),
    ALS("ALS", "Asymmetric Least Squares (основной авто-метод)"),
    SNIP("SNIP", "Statistics-sensitive Non-linear Iterative Peak-clipping (экспериментальный)"),
}

// ─── Result ─────────────────────────────────────────────────────

/**
 * Baseline estimation result — one per method attempt.
 */
@Serializable
data class BaselineResult(
    val method: String,
    val baseline: List<Double>,
    val params: Map<String, Double>,
    val warnings: List<String>,
    val quality: BaselineQuality?,
)

/**
 * Quality metrics for a baseline estimate.
 */
@Serializable
data class BaselineQuality(
    val flatnessScore: Double,
    val residualStd: Double,
    val negativeCount: Int,
    val maxNegativeDepth: Double,
)

// ─── Interface ──────────────────────────────────────────────────

/**
 * Common interface for baseline estimation algorithms.
 * All implementations must be pure functions.
 */
interface BaselineEstimator {
    val method: BaselineMethod

    /**
     * Estimate baseline for the given signal.
     *
     * @param points  Signal points (sorted by time)
     * @return BaselineResult with baseline array of same length as points
     */
    fun estimate(points: List<SignalPoint>): BaselineResult
}

// ─── NONE Estimator ─────────────────────────────────────────────

/**
 * No baseline correction — baseline = 0 everywhere.
 */
object NoneBaselineEstimator : BaselineEstimator {
    override val method = BaselineMethod.NONE

    override fun estimate(points: List<SignalPoint>): BaselineResult {
        return BaselineResult(
            method = method.name,
            baseline = List(points.size) { 0.0 },
            params = emptyMap(),
            warnings = emptyList(),
            quality = null,
        )
    }
}

// ─── Dispatcher ─────────────────────────────────────────────────

/**
 * Routes baseline estimation to the correct algorithm.
 *
 * Pure function: method + signal → BaselineResult.
 */
object BaselineDispatcher {

    private val estimators = mutableMapOf<BaselineMethod, BaselineEstimator>(
        BaselineMethod.NONE to NoneBaselineEstimator,
    )

    /**
     * Register a baseline estimator (called during init).
     */
    fun register(estimator: BaselineEstimator) {
        estimators[estimator.method] = estimator
    }

    /**
     * Estimate baseline using the specified method.
     */
    fun estimate(method: BaselineMethod, points: List<SignalPoint>): BaselineResult {
        val estimator = estimators[method]
            ?: return BaselineResult(
                method = method.name,
                baseline = List(points.size) { 0.0 },
                params = emptyMap(),
                warnings = listOf("Метод ${method.label} не зарегистрирован — используется baseline = 0"),
                quality = null,
            )
        return estimator.estimate(points)
    }

    /**
     * Compare all registered methods on the same signal.
     * Returns map of method → result for BaselineComparisonScreen.
     */
    fun compareAll(points: List<SignalPoint>): Map<BaselineMethod, BaselineResult> {
        return estimators.mapValues { (_, estimator) ->
            estimator.estimate(points)
        }
    }

    /**
     * Check if a method is registered.
     */
    fun isAvailable(method: BaselineMethod): Boolean = estimators.containsKey(method)
}
