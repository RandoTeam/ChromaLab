package com.chromalab.feature.processing.geometry

actual class CvQuadrilateralCandidateDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
        graphRegions: List<CvQuadrilateralInputRegion>,
    ): CvQuadrilateralCandidateResult =
        CvQuadrilateralCandidateResult(
            candidates = emptyList(),
            warnings = listOf("opencv_quadrilateral.android_backend_not_enabled"),
        )
}
