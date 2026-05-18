package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.crop.CropRect
import com.chromalab.feature.processing.crop.CropResult
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.perspective.HomographyMatrix
import com.chromalab.feature.processing.perspective.PerspectiveCorrectionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GeometryStageStatusContractTest {

    @Test
    fun cropNoOpCanBeStoredAsSkippedNotConfident() {
        val result = CropResult(
            croppedPath = "normalized.jpg",
            sourcePath = "normalized.jpg",
            sourceWidth = 1200,
            sourceHeight = 800,
            cropRect = CropRect(0, 0, 1200, 800),
            croppedWidth = 1200,
            croppedHeight = 800,
            timestamp = 1L,
            status = GeometryStageStatus.SKIPPED_NOT_CONFIDENT,
            warnings = listOf("crop.no_runtime_document_quad_identity_preserved"),
        )

        assertEquals(GeometryStageStatus.SKIPPED_NOT_CONFIDENT, result.status)
        assertEquals(listOf("crop.no_runtime_document_quad_identity_preserved"), result.warnings)
    }

    @Test
    fun perspectiveIdentityNoOpCanBeStoredWithoutFakeAppliedStatus() {
        val result = PerspectiveCorrectionResult(
            correctedPath = "normalized.jpg",
            sourcePath = "normalized.jpg",
            homography = HomographyMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
            sourceCorners = DocumentCorners(
                topLeft = ImagePoint(0f, 0f),
                topRight = ImagePoint(1200f, 0f),
                bottomLeft = ImagePoint(0f, 800f),
                bottomRight = ImagePoint(1200f, 800f),
            ),
            outputWidth = 1200,
            outputHeight = 800,
            maxWarpDistance = 0f,
            correctedAspectRatio = 1.5f,
            isExcessiveWarp = false,
            timestamp = 1L,
            status = GeometryStageStatus.SKIPPED_NOT_NEEDED,
            warnings = listOf("perspective.identity_no_warp_needed"),
        )

        assertEquals(GeometryStageStatus.SKIPPED_NOT_NEEDED, result.status)
        assertFalse(result.isExcessiveWarp)
        assertEquals(listOf("perspective.identity_no_warp_needed"), result.warnings)
    }
}
