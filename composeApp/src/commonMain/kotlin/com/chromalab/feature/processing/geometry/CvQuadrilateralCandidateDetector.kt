package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.graph.GraphRegion

data class CvQuadrilateralInputRegion(
    val graphIndex: Int,
    val panelRegion: GraphRegion,
    val plotRegion: GraphRegion?,
)

data class CvQuadrilateralCandidateResult(
    val candidates: List<CvQuadrilateralCandidate>,
    val warnings: List<String> = emptyList(),
)

data class CvQuadrilateralCandidate(
    val kind: CvQuadrilateralCandidateKind,
    val graphIndex: Int?,
    val source: String,
    val corners: DocumentCorners,
    val accepted: Boolean,
    val score: Float,
    val warnings: List<String> = emptyList(),
)

enum class CvQuadrilateralCandidateKind {
    DOCUMENT,
    PLOT_AREA,
}

expect class CvQuadrilateralCandidateDetector() {
    fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
        graphRegions: List<CvQuadrilateralInputRegion>,
    ): CvQuadrilateralCandidateResult
}
