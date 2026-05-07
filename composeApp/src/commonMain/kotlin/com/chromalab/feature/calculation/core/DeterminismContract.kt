package com.chromalab.feature.calculation.core

/**
 * Determinism contract (§2.31).
 *
 * Guarantees:
 * 1. All calculation functions are PURE — no side effects, no randomness, no ML.
 * 2. All numeric calculations use Double (not Float) for precision.
 * 3. signal + params + manual edits = SAME result (byte-for-byte identical).
 * 4. No dependency on system time, locale, or platform in calculations.
 * 5. numericPrecisionMode and algorithmVersion are saved with every run.
 *
 * Verification:
 * - DeterminismVerifier.verify() runs the pipeline twice with identical inputs
 *   and asserts all outputs match exactly.
 */

/**
 * Numeric precision mode — tracked per run for reproducibility.
 */
enum class NumericPrecisionMode(val label: String) {
    DOUBLE_64("Double (64-bit IEEE 754)"),
}

/**
 * Determinism metadata — saved with every CalculationRun.
 */
data class DeterminismInfo(
    val algorithmVersion: String = CalculationPipeline.ALGORITHM_VERSION,
    val pipelineVersion: String = CalculationPipeline.PIPELINE_VERSION,
    val numericPrecision: NumericPrecisionMode = NumericPrecisionMode.DOUBLE_64,
    val usesRandom: Boolean = false,
    val usesML: Boolean = false,
    val usesPlatformDependentCode: Boolean = false,
) {
    /**
     * Validate determinism contract at runtime.
     * Returns list of violations (empty = contract holds).
     */
    fun validate(): List<String> {
        val violations = mutableListOf<String>()
        if (usesRandom) violations += "Pipeline uses random — violates determinism"
        if (usesML) violations += "Pipeline uses ML — violates determinism"
        if (usesPlatformDependentCode) violations += "Pipeline uses platform-dependent code — may violate determinism"
        if (numericPrecision != NumericPrecisionMode.DOUBLE_64) {
            violations += "Numeric precision is not Double 64-bit"
        }
        return violations
    }

    companion object {
        /** Default determinism info for Phase 2. */
        fun phase2() = DeterminismInfo()
    }
}

/**
 * Determinism verifier — runs pipeline twice, compares results.
 *
 * Usage:
 * ```
 * val result = DeterminismVerifier.verify(signal, params) { sig, par ->
 *     runPipeline(sig, par)
 * }
 * assert(result.isIdentical) { result.differences }
 * ```
 */
object DeterminismVerifier {

    data class VerificationResult(
        val isIdentical: Boolean,
        val differences: List<String>,
        val run1Hash: Int,
        val run2Hash: Int,
    )

    /**
     * Run the pipeline twice and compare outputs.
     *
     * @param signal Input signal points (time, intensity pairs)
     * @param params Algorithm parameters as a stable string representation
     * @param pipeline The pipeline function to verify
     * @return VerificationResult with comparison details
     */
    fun <T> verify(
        signal: List<Double>,
        params: String,
        pipeline: (List<Double>, String) -> T,
    ): VerificationResult {
        val result1 = pipeline(signal, params)
        val result2 = pipeline(signal, params)

        val hash1 = result1.hashCode()
        val hash2 = result2.hashCode()
        val str1 = result1.toString()
        val str2 = result2.toString()

        val differences = mutableListOf<String>()

        if (hash1 != hash2) {
            differences += "Hash mismatch: $hash1 != $hash2"
        }
        if (str1 != str2) {
            // Find first divergence point
            val divergeAt = str1.zip(str2).indexOfFirst { (a, b) -> a != b }
            val context = if (divergeAt >= 0) {
                val start = maxOf(0, divergeAt - 20)
                val end = minOf(str1.length, divergeAt + 20)
                "at char $divergeAt: '${str1.substring(start, end)}' vs '${str2.substring(start, minOf(str2.length, end))}'"
            } else {
                "length mismatch: ${str1.length} vs ${str2.length}"
            }
            differences += "Output mismatch $context"
        }

        return VerificationResult(
            isIdentical = differences.isEmpty(),
            differences = differences,
            run1Hash = hash1,
            run2Hash = hash2,
        )
    }
}
