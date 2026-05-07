package com.chromalab.feature.processing.graph

import kotlinx.serialization.Serializable

/**
 * User's selection of which graph to process from a multi-graph sheet.
 * Carries index, label, and region for downstream pipeline stages.
 *
 * The structure is designed to support ratio computation between
 * graphs later (e.g., Ion 217 / Ion 218).
 */
@Serializable
data class GraphSelection(
    val graphIndex: Int,
    val graphLabel: String,
    val region: GraphRegion,
    val totalGraphsFound: Int,
    val timestamp: Long,
) {
    companion object {
        /** Common label presets for chromatography */
        val LABEL_PRESETS = listOf(
            "Ion 217",
            "Ion 218",
            "Channel 1",
            "Channel 2",
            "TIC",
            "FID",
            "UV 254 nm",
            "UV 280 nm",
            "Верхний график",
            "Нижний график",
        )
    }
}

/**
 * Container for all graphs found on a sheet, with the user's selections.
 * Supports processing multiple graphs sequentially.
 */
@Serializable
data class MultiGraphResult(
    val allRegions: List<GraphRegion>,
    val selections: List<GraphSelection>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long,
) {
    /** The currently active selection (first by default) */
    val activeSelection: GraphSelection?
        get() = selections.firstOrNull()

    /** Whether more than one graph was found */
    val isMultiGraph: Boolean
        get() = allRegions.size > 1
}
