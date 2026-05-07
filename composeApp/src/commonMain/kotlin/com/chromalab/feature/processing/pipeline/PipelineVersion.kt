package com.chromalab.feature.processing.pipeline

import kotlinx.serialization.Serializable

/**
 * Pipeline version — bumped on any change that could alter output.
 *
 * Contract:
 *   same image + same PipelineVersion + same ProcessingParams + same ManualEdits
 *   = identical DigitalSignal output (bit-exact)
 *
 * Guarantees:
 * - No random operations (no RNG without fixed seed)
 * - No cloud API calls during processing
 * - No non-deterministic neural networks for calculation
 * - OCR is hint-only (user-confirmed values are deterministic input)
 * - All parameters explicitly saved in ProcessingParams
 * - All manual corrections saved in ManualEditLog
 */
@Serializable
data class PipelineVersion(
    /** Major: breaking changes to output format or algorithm */
    val major: Int,
    /** Minor: algorithm improvements that change results */
    val minor: Int,
    /** Patch: bug fixes, no result change */
    val patch: Int,
) {
    val versionString: String get() = "$major.$minor.$patch"

    companion object {
        /** Current pipeline version */
        val CURRENT = PipelineVersion(major = 1, minor = 0, patch = 0)
    }
}

/**
 * Determinism contract marker.
 * Any class implementing this guarantees deterministic output.
 *
 * Rules:
 * 1. No System.currentTimeMillis() in computation (only in metadata)
 * 2. No Random without explicit seed
 * 3. No network calls
 * 4. No mutable global state
 * 5. Floating-point operations use the same order of operations
 */
interface Deterministic {
    /** Returns the pipeline version this component was built for */
    val pipelineVersion: PipelineVersion
}

/**
 * Pipeline fingerprint — uniquely identifies a processing run.
 * Two runs with same fingerprint MUST produce identical results.
 */
@Serializable
data class PipelineFingerprint(
    val pipelineVersion: PipelineVersion,
    val imageHash: String,
    val paramsHash: String,
    val manualEditsHash: String,
) {
    /**
     * Combined hash — if two fingerprints match, outputs are identical.
     */
    val combinedHash: String
        get() = "${pipelineVersion.versionString}:$imageHash:$paramsHash:$manualEditsHash"
}
