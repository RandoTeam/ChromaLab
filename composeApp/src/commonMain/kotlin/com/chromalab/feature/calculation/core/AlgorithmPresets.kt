package com.chromalab.feature.calculation.core

/**
 * Algorithm presets / profiles (§2.27).
 *
 * Each preset is a complete AlgorithmProfile with all parameters.
 * Users can create custom profiles on top of built-in presets.
 *
 * Built-in:
 * - Conservative: fewer false positives (higher thresholds)
 * - Balanced: default settings
 * - Sensitive: more candidates (lower thresholds)
 * - Manual Review: minimal auto-detection, focus on manual work
 */

data class AlgorithmProfile(
    val name: String,
    val description: String,
    val isBuiltIn: Boolean = true,

    // Smoothing
    val smoothingEnabled: Boolean = true,
    val smoothingWindowSize: Int = 7,
    val smoothingPolyOrder: Int = 3,

    // Baseline
    val baselineMethod: String = "ALS",
    val alsLambda: Double = 1e6,
    val alsPenalty: Double = 0.01,
    val alsIterations: Int = 10,
    val snipIterations: Int = 40,

    // Peak detection
    val noiseK: Double = 3.0,
    val minDistance: Int = 5,
    val minWidth: Int = 3,
    val maxWidth: Int = Int.MAX_VALUE,

    // Boundaries
    val boundaryMethod: String = "LOCAL_MINIMA",
    val percentHeight: Double = 0.01,

    // Integration
    val clampNegative: Boolean = false,
    val useInterpolatedBoundaries: Boolean = true,

    // Noise
    val noiseMethod: String = "MAD",
    val noiseAutoRegion: Boolean = true,
)

/**
 * Registry of algorithm profiles — built-in + user-created.
 */
object AlgorithmPresets {

    val CONSERVATIVE = AlgorithmProfile(
        name = "Conservative",
        description = "Меньше ложных срабатываний. Выше пороги детекции, строже фильтрация.",
        noiseK = 5.0,
        minDistance = 10,
        minWidth = 5,
        smoothingWindowSize = 9,
        alsLambda = 1e7,
    )

    val BALANCED = AlgorithmProfile(
        name = "Balanced",
        description = "Настройки по умолчанию. Баланс между чувствительностью и точностью.",
    )

    val SENSITIVE = AlgorithmProfile(
        name = "Sensitive",
        description = "Больше кандидатов для ручной проверки. Ниже пороги детекции.",
        noiseK = 2.0,
        minDistance = 3,
        minWidth = 2,
        smoothingWindowSize = 5,
        alsLambda = 1e5,
    )

    val MANUAL_REVIEW = AlgorithmProfile(
        name = "Manual Review",
        description = "Минимум авто-детекции. Акцент на ручное добавление и коррекцию пиков.",
        noiseK = 10.0,
        minDistance = 20,
        minWidth = 5,
        smoothingWindowSize = 11,
        alsLambda = 1e8,
    )

    /** All built-in presets. */
    val builtIn: List<AlgorithmProfile> = listOf(
        CONSERVATIVE, BALANCED, SENSITIVE, MANUAL_REVIEW,
    )

    /** Find preset by name. */
    fun byName(name: String): AlgorithmProfile? =
        builtIn.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

/**
 * User profile storage — manages custom profiles alongside built-in ones.
 *
 * Immutable: each mutation returns a new instance.
 */
data class ProfileRegistry(
    val customProfiles: List<AlgorithmProfile> = emptyList(),
) {
    /** All profiles: built-in + custom. */
    val allProfiles: List<AlgorithmProfile>
        get() = AlgorithmPresets.builtIn + customProfiles

    /** Add a custom profile. Built-in names are forbidden. */
    fun addCustom(profile: AlgorithmProfile): ProfileRegistry {
        val safeName = if (AlgorithmPresets.byName(profile.name) != null) {
            "${profile.name} (custom)"
        } else profile.name
        return copy(customProfiles = customProfiles + profile.copy(
            name = safeName, isBuiltIn = false,
        ))
    }

    /** Remove a custom profile by name. Built-in profiles cannot be removed. */
    fun removeCustom(name: String): ProfileRegistry =
        copy(customProfiles = customProfiles.filter { it.name != name })

    /** Find any profile by name. */
    fun byName(name: String): AlgorithmProfile? =
        allProfiles.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
