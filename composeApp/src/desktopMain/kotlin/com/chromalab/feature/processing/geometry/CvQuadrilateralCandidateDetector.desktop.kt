package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.graph.GraphRegion
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

actual class CvQuadrilateralCandidateDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
        graphRegions: List<CvQuadrilateralInputRegion>,
    ): CvQuadrilateralCandidateResult {
        return try {
            loadOpenCv()
            val source = Imgcodecs.imread(imagePath)
            if (source.empty()) {
                return CvQuadrilateralCandidateResult(
                    candidates = emptyList(),
                    warnings = listOf("opencv_quadrilateral.image_not_readable"),
                )
            }

            val candidates = mutableListOf<CvQuadrilateralCandidate>()
            val warnings = mutableListOf<String>()
            candidates += detectDocumentCandidates(source, imageWidth, imageHeight)
            graphRegions.forEach { region ->
                detectPlotCandidate(source, region)?.let { candidates += it }
                    ?: warnings.add("opencv_quadrilateral.graph_${region.graphIndex}.plot_candidate_missing")
            }
            source.release()

            CvQuadrilateralCandidateResult(
                candidates = candidates,
                warnings = warnings,
            )
        } catch (error: Throwable) {
            CvQuadrilateralCandidateResult(
                candidates = emptyList(),
                warnings = listOf("opencv_quadrilateral.backend_failed:${error::class.simpleName}"),
            )
        }
    }

    private fun detectDocumentCandidates(
        source: Mat,
        imageWidth: Int,
        imageHeight: Int,
    ): List<CvQuadrilateralCandidate> {
        val gray = source.toGray()
        val blurred = Mat()
        val edges = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(blurred, edges, 50.0, 150.0)
        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        val contourInput = edges.clone()
        Imgproc.findContours(contourInput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        val imageArea = (imageWidth.toDouble() * imageHeight.toDouble()).coerceAtLeast(1.0)
        val candidates = contours.mapNotNull { contour ->
            val area = Imgproc.contourArea(contour)
            val areaRatio = area / imageArea
            if (areaRatio < 0.04 || areaRatio > 0.99) {
                contour.release()
                return@mapNotNull null
            }
            val contour2f = MatOfPoint2f(*contour.toArray())
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx, Imgproc.arcLength(contour2f, true) * 0.02, true)
            val points = approx.toArray().toList()
            contour2f.release()
            approx.release()
            contour.release()
            if (points.size != 4) return@mapNotNull null

            val corners = points.sortedCorners().toDocumentCorners()
            val skew = corners.maxSkewAngleDegrees()
            CvQuadrilateralCandidate(
                kind = CvQuadrilateralCandidateKind.DOCUMENT,
                graphIndex = null,
                source = "opencv_contours_approx_polydp",
                corners = corners,
                accepted = areaRatio in 0.10..0.98 && skew <= 12f,
                score = (areaRatio.toFloat() * (1f - (skew / 90f).coerceIn(0f, 1f))).coerceIn(0f, 1f),
                warnings = buildList {
                    if (skew > 12f) add("opencv_quadrilateral.document_skew_review")
                    if (areaRatio !in 0.10..0.98) add("opencv_quadrilateral.document_area_review")
                },
            )
        }.sortedByDescending { it.score }.take(3)

        gray.release()
        blurred.release()
        edges.release()
        contourInput.release()
        hierarchy.release()
        return candidates
    }

    private fun detectPlotCandidate(
        source: Mat,
        input: CvQuadrilateralInputRegion,
    ): CvQuadrilateralCandidate? {
        val panel = input.panelRegion.clampedTo(source.width(), source.height()) ?: return null
        val panelRect = Rect(panel.x, panel.y, panel.width, panel.height)
        val panelMat = Mat(source, panelRect)
        val gray = panelMat.toGray()
        val edges = Mat()
        Imgproc.Canny(gray, edges, 40.0, 140.0)

        val lines = Mat()
        val minLineLength = min(panel.width, panel.height) * 0.20
        Imgproc.HoughLinesP(edges, lines, 1.0, PI / 180.0, 32, minLineLength, 18.0)
        val support = houghSupport(lines)
        val plotRegion = input.plotRegion ?: panel
        val accepted = input.plotRegion != null && support.horizontalCount > 0 && support.verticalCount > 0
        val corners = plotRegion.toDocumentCorners()

        panelMat.release()
        gray.release()
        edges.release()
        lines.release()

        return CvQuadrilateralCandidate(
            kind = CvQuadrilateralCandidateKind.PLOT_AREA,
            graphIndex = input.graphIndex,
            source = "opencv_hough_plot_frame_benchmark",
            corners = corners,
            accepted = accepted,
            score = support.score,
            warnings = buildList {
                if (support.horizontalCount == 0) add("opencv_quadrilateral.plot_horizontal_support_missing")
                if (support.verticalCount == 0) add("opencv_quadrilateral.plot_vertical_support_missing")
                if (input.plotRegion == null) add("opencv_quadrilateral.plot_region_missing")
            },
        )
    }

    private fun houghSupport(lines: Mat): HoughSupport {
        var horizontal = 0
        var vertical = 0
        for (row in 0 until lines.rows()) {
            val values = lines.get(row, 0) ?: continue
            if (values.size < 4) continue
            val dx = abs(values[2] - values[0])
            val dy = abs(values[3] - values[1])
            if (dx >= dy * 8.0 && dx > 20.0) horizontal++
            if (dy >= dx * 8.0 && dy > 20.0) vertical++
        }
        return HoughSupport(
            horizontalCount = horizontal,
            verticalCount = vertical,
            score = ((min(horizontal, 8) + min(vertical, 8)) / 16f).coerceIn(0f, 1f),
        )
    }

    private fun Mat.toGray(): Mat {
        if (channels() == 1) return clone()
        val gray = Mat()
        Imgproc.cvtColor(this, gray, Imgproc.COLOR_BGR2GRAY)
        return gray
    }

    private fun List<Point>.sortedCorners(): List<Point> {
        val centerX = sumOf { it.x } / size
        val centerY = sumOf { it.y } / size
        return sortedBy { atan2(it.y - centerY, it.x - centerX) }
            .let { sorted ->
                val topLeftIndex = sorted.indices.minBy { sorted[it].x + sorted[it].y }
                List(sorted.size) { offset -> sorted[(topLeftIndex + offset) % sorted.size] }
            }
    }

    private fun List<Point>.toDocumentCorners(): DocumentCorners =
        DocumentCorners(
            topLeft = this[0].toImagePoint(),
            topRight = this[1].toImagePoint(),
            bottomRight = this[2].toImagePoint(),
            bottomLeft = this[3].toImagePoint(),
        )

    private fun Point.toImagePoint(): ImagePoint =
        ImagePoint(x.toFloat(), y.toFloat())

    private fun GraphRegion.toDocumentCorners(): DocumentCorners =
        DocumentCorners(
            topLeft = ImagePoint(x.toFloat(), y.toFloat()),
            topRight = ImagePoint(right.toFloat(), y.toFloat()),
            bottomRight = ImagePoint(right.toFloat(), bottom.toFloat()),
            bottomLeft = ImagePoint(x.toFloat(), bottom.toFloat()),
        )

    private fun GraphRegion.clampedTo(width: Int, height: Int): GraphRegion? {
        val left = x.coerceIn(0, width - 1)
        val top = y.coerceIn(0, height - 1)
        val right = right.coerceIn(left + 1, width)
        val bottom = bottom.coerceIn(top + 1, height)
        if (right <= left || bottom <= top) return null
        return copy(x = left, y = top, width = right - left, height = bottom - top)
    }

    private fun DocumentCorners.maxSkewAngleDegrees(): Float {
        val horizontalAngles = listOf(
            lineAngleDegrees(topLeft, topRight),
            lineAngleDegrees(bottomLeft, bottomRight),
        ).map { normalizeAngleDistance(it, 0f) }
        val verticalAngles = listOf(
            lineAngleDegrees(topLeft, bottomLeft),
            lineAngleDegrees(topRight, bottomRight),
        ).map { normalizeAngleDistance(it, 90f) }
        return (horizontalAngles + verticalAngles).maxOrNull() ?: 0f
    }

    private fun lineAngleDegrees(a: ImagePoint, b: ImagePoint): Float =
        (atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()) * 180.0 / PI).toFloat()

    private fun normalizeAngleDistance(angle: Float, target: Float): Float {
        var delta = abs(angle - target) % 180f
        if (delta > 90f) delta = 180f - delta
        return delta
    }

    private data class HoughSupport(
        val horizontalCount: Int,
        val verticalCount: Int,
        val score: Float,
    )

    private companion object {
        private var loaded = false

        private fun loadOpenCv() {
            if (loaded) return
            OpenCV.loadLocally()
            loaded = true
            check(Core.NATIVE_LIBRARY_NAME.isNotBlank())
        }
    }
}
