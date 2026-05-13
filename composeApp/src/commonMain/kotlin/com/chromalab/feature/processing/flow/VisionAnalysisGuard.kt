package com.chromalab.feature.processing.flow

internal fun blocksFullAnalysisSkip(error: String): Boolean {
    val normalized = error.lowercase()
    return BlockingVisionErrorMarkers.any { marker -> marker in normalized }
}

private val BlockingVisionErrorMarkers = listOf(
    "ai vision model is required",
    "ai vision model is not loaded",
    "ai graph detection",
    "ai axis extraction",
    "ai axis structure",
    "ai vision analysis did not produce",
    "chromatography vlm",
    "vision projector is missing",
    "vision support",
    "image input",
)
