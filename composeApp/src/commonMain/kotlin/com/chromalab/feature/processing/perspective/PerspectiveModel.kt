package com.chromalab.feature.processing.perspective

import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.geometry.GeometryStageStatus
import kotlinx.serialization.Serializable

/**
 * 3×3 homography matrix stored as flat array [h00..h22].
 */
@Serializable
data class HomographyMatrix(val values: FloatArray) {
    init {
        require(values.size == 9) { "Homography must have 9 elements" }
    }

    override fun equals(other: Any?): Boolean =
        other is HomographyMatrix && values.contentEquals(other.values)

    override fun hashCode(): Int = values.contentHashCode()
}

/**
 * Result of perspective correction.
 */
@Serializable
data class PerspectiveCorrectionResult(
    val correctedPath: String,
    val sourcePath: String,
    val homography: HomographyMatrix,
    val sourceCorners: DocumentCorners,
    val outputWidth: Int,
    val outputHeight: Int,
    val maxWarpDistance: Float,
    val correctedAspectRatio: Float,
    val isExcessiveWarp: Boolean,
    val timestamp: Long,
    val status: GeometryStageStatus = GeometryStageStatus.APPLIED,
    val warnings: List<String> = emptyList(),
)

/**
 * Computes a 3×3 homography matrix that maps [src] quadrilateral
 * to a rectangle [0,0]–[dstW, dstH].
 *
 * Uses Direct Linear Transform (DLT) — solves Ah=0 via Gaussian elimination.
 */
fun computeHomography(src: DocumentCorners, dstW: Float, dstH: Float): HomographyMatrix {
    val srcPts = listOf(src.topLeft, src.topRight, src.bottomRight, src.bottomLeft)
    val dstPts = listOf(
        ImagePoint(0f, 0f),
        ImagePoint(dstW, 0f),
        ImagePoint(dstW, dstH),
        ImagePoint(0f, dstH),
    )

    // Build 8×9 matrix A for Ah=0
    val a = Array(8) { FloatArray(9) }
    for (i in 0 until 4) {
        val sx = srcPts[i].x; val sy = srcPts[i].y
        val dx = dstPts[i].x; val dy = dstPts[i].y
        val row1 = i * 2
        val row2 = row1 + 1

        a[row1][0] = sx; a[row1][1] = sy; a[row1][2] = 1f
        a[row1][3] = 0f; a[row1][4] = 0f; a[row1][5] = 0f
        a[row1][6] = -dx * sx; a[row1][7] = -dx * sy; a[row1][8] = -dx

        a[row2][0] = 0f; a[row2][1] = 0f; a[row2][2] = 0f
        a[row2][3] = sx; a[row2][4] = sy; a[row2][5] = 1f
        a[row2][6] = -dy * sx; a[row2][7] = -dy * sy; a[row2][8] = -dy
    }

    // Solve via Gaussian elimination (reduce to row echelon)
    for (col in 0 until 8) {
        // Find pivot
        var maxVal = 0f; var maxRow = col
        for (row in col until 8) {
            val v = kotlin.math.abs(a[row][col])
            if (v > maxVal) { maxVal = v; maxRow = row }
        }
        // Swap rows
        val tmp = a[col]; a[col] = a[maxRow]; a[maxRow] = tmp

        val pivot = a[col][col]
        if (kotlin.math.abs(pivot) < 1e-10f) continue

        // Scale pivot row
        for (j in col until 9) a[col][j] /= pivot

        // Eliminate column
        for (row in 0 until 8) {
            if (row == col) continue
            val factor = a[row][col]
            for (j in col until 9) a[row][j] -= factor * a[col][j]
        }
    }

    // Extract h (last column, negated, with h22=1)
    val h = FloatArray(9)
    for (i in 0 until 8) h[i] = -a[i][8]
    h[8] = 1f

    return HomographyMatrix(h)
}

/**
 * Compute the maximum warp distance — how far corners moved.
 * High values indicate excessive perspective correction.
 */
fun maxWarpDistance(src: DocumentCorners, dstW: Float, dstH: Float): Float {
    val srcPts = listOf(src.topLeft, src.topRight, src.bottomRight, src.bottomLeft)
    val dstPts = listOf(
        ImagePoint(0f, 0f),
        ImagePoint(dstW, 0f),
        ImagePoint(dstW, dstH),
        ImagePoint(0f, dstH),
    )

    var maxDist = 0f
    for (i in 0 until 4) {
        val dx = srcPts[i].x - dstPts[i].x
        val dy = srcPts[i].y - dstPts[i].y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist > maxDist) maxDist = dist
    }
    return maxDist
}
