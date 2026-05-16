package com.chromalab.feature.processing.flow

import com.chromalab.feature.processing.model.ModelAssistedAnalysisContract

internal fun blocksFullAnalysisSkip(error: String): Boolean {
    val normalized = error.lowercase()
    return BlockingVisionErrorMarkers.any { marker -> marker in normalized }
}

private val BlockingVisionErrorMarkers =
    ModelAssistedAnalysisContract.blockingErrorMarkers() + listOf(
        "axis calibration requires",
        "axis calibration is required",
        "axis calibration is incomplete",
    )
