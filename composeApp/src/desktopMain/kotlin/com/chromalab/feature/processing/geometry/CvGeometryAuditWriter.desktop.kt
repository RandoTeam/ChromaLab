package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class CvGeometryInputGraph(
    val graphIndex: Int,
    val region: GraphRegion,
)

@Serializable
data class CvGeometryAudit(
    val sourceImagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val methodFamily: String,
    val methodReferences: List<String>,
    val graphs: List<CvGeometryGraphAudit>,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CvGeometryGraphAudit(
    val graphIndex: Int,
    val region: GraphRegion,
    val threshold: Int,
    val darkPixelRatio: Float,
    val horizontalLineCount: Int,
    val verticalLineCount: Int,
    val componentCount: Int,
    val contourCandidateCount: Int,
    val frameCandidateCount: Int,
    val topHorizontalLines: List<CvLineSegmentAudit>,
    val topVerticalLines: List<CvLineSegmentAudit>,
    val topComponents: List<CvComponentAudit>,
    val contourCandidates: List<CvComponentAudit>,
    val frameCandidates: List<CvFrameCandidateAudit>,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CvLineSegmentAudit(
    val orientation: CvLineOrientation,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val length: Int,
    val lengthRatio: Float,
    val darkDensity: Float,
    val score: Float,
)

@Serializable
enum class CvLineOrientation {
    HORIZONTAL,
    VERTICAL,
}

@Serializable
data class CvComponentAudit(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val pixelCount: Int,
    val areaRatio: Float,
    val fillRatio: Float,
    val aspectRatio: Float,
    val score: Float,
)

@Serializable
data class CvFrameCandidateAudit(
    val region: GraphRegion,
    val horizontalSupport: Int,
    val verticalSupport: Int,
    val areaRatioWithinGraph: Float,
    val score: Float,
)

object CvGeometryAuditWriter {
    private const val MaxLinesPerOrientation = 80
    private const val MaxComponents = 80
    private const val MaxContourCandidates = 24
    private const val MaxFrameCandidates = 12

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun write(
        imagePath: Path,
        graphRegions: List<CvGeometryInputGraph>,
        outputDir: Path,
    ): CvGeometryAudit? {
        val source = ImageIO.read(imagePath.toFile()) ?: return null
        return try {
            val audit = analyze(source, imagePath, graphRegions)
            Files.writeString(outputDir.resolve("cv_geometry_audit.json"), json.encodeToString(audit))
            writeOverlay(source, audit, outputDir.resolve("cv_geometry_overlay.png"))
            audit
        } finally {
            source.flush()
        }
    }

    private fun analyze(
        source: BufferedImage,
        imagePath: Path,
        graphRegions: List<CvGeometryInputGraph>,
    ): CvGeometryAudit {
        val warnings = mutableListOf<String>()
        val inputs = graphRegions.ifEmpty {
            warnings += "cv_geometry.no_graph_regions_supplied"
            listOf(CvGeometryInputGraph(1, GraphRegion(0, 0, source.width, source.height, "full image")))
        }

        val graphAudits = inputs.mapNotNull { input ->
            val region = input.region.clampedTo(source.width, source.height)
                ?: return@mapNotNull null.also {
                    warnings += "cv_geometry.graph_${input.graphIndex}.region_out_of_image"
                }
            analyzeGraph(source, input.graphIndex, region)
        }

        return CvGeometryAudit(
            sourceImagePath = imagePath.toString(),
            imageWidth = source.width,
            imageHeight = source.height,
            methodFamily = "desktop_spike_classical_cv_run_components_contours",
            methodReferences = listOf(
                "OpenCV HoughLinesP / LineSegmentDetector for line candidates",
                "OpenCV connectedComponentsWithStats for component statistics",
                "OpenCV findContours / shape descriptors for contour candidates",
                "WebPlotDigitizer / Plot Extractor style axis-first graph digitizing",
            ),
            graphs = graphAudits,
            warnings = warnings,
        )
    }

    private fun analyzeGraph(
        source: BufferedImage,
        graphIndex: Int,
        region: GraphRegion,
    ): CvGeometryGraphAudit {
        val gray = readGray(source, region)
        val threshold = estimateDarkThreshold(gray)
        val dark = BooleanArray(gray.size)
        var darkCount = 0
        gray.forEachIndexed { index, value ->
            if (value <= threshold) {
                dark[index] = true
                darkCount++
            }
        }

        val horizontalLines = findLineSegments(
            dark = dark,
            width = region.width,
            height = region.height,
            region = region,
            orientation = CvLineOrientation.HORIZONTAL,
        )
        val verticalLines = findLineSegments(
            dark = dark,
            width = region.width,
            height = region.height,
            region = region,
            orientation = CvLineOrientation.VERTICAL,
        )
        val components = findComponents(dark, region.width, region.height, region)
        val contourCandidates = components
            .filter { it.areaRatio >= 0.0015f && it.fillRatio in 0.015f..0.92f }
            .sortedByDescending { it.score }
            .take(MaxContourCandidates)
        val frameCandidates = (
            proposeFrameCandidates(
                region = region,
                horizontalLines = horizontalLines,
                verticalLines = verticalLines,
            ) + proposeComponentFrameCandidates(
                region = region,
                contourCandidates = contourCandidates,
            )
        )
            .distinctBy { "${it.region.x}:${it.region.y}:${it.region.width}:${it.region.height}" }
            .sortedByDescending { it.score }
            .take(MaxFrameCandidates)

        val warnings = buildList {
            if (horizontalLines.isEmpty()) add("cv_geometry.no_horizontal_line_candidates")
            if (verticalLines.isEmpty()) add("cv_geometry.no_vertical_line_candidates")
            if (components.isEmpty()) add("cv_geometry.no_connected_components")
            if (frameCandidates.isEmpty()) add("cv_geometry.no_frame_candidates")
        }

        return CvGeometryGraphAudit(
            graphIndex = graphIndex,
            region = region,
            threshold = threshold,
            darkPixelRatio = darkCount.toFloat() / dark.size.coerceAtLeast(1).toFloat(),
            horizontalLineCount = horizontalLines.size,
            verticalLineCount = verticalLines.size,
            componentCount = components.size,
            contourCandidateCount = contourCandidates.size,
            frameCandidateCount = frameCandidates.size,
            topHorizontalLines = horizontalLines.take(24),
            topVerticalLines = verticalLines.take(24),
            topComponents = components.take(24),
            contourCandidates = contourCandidates,
            frameCandidates = frameCandidates,
            warnings = warnings,
        )
    }

    private fun readGray(source: BufferedImage, region: GraphRegion): IntArray {
        val gray = IntArray(region.width * region.height)
        for (y in 0 until region.height) {
            for (x in 0 until region.width) {
                val rgb = source.getRGB(region.x + x, region.y + y)
                gray[y * region.width + x] = (
                    0.299 * ((rgb shr 16) and 0xFF) +
                        0.587 * ((rgb shr 8) and 0xFF) +
                        0.114 * (rgb and 0xFF)
                    ).roundToInt()
            }
        }
        return gray
    }

    private fun estimateDarkThreshold(gray: IntArray): Int {
        if (gray.isEmpty()) return 150
        val histogram = IntArray(256)
        gray.forEach { value -> histogram[value.coerceIn(0, 255)]++ }
        val p18 = percentile(histogram, gray.size, 0.18f)
        val p35 = percentile(histogram, gray.size, 0.35f)
        return (p18 + ((p35 - p18) * 0.45f).roundToInt() + 24).coerceIn(72, 186)
    }

    private fun percentile(histogram: IntArray, total: Int, percentile: Float): Int {
        val target = (total * percentile).roundToInt().coerceIn(1, total.coerceAtLeast(1))
        var seen = 0
        for (i in histogram.indices) {
            seen += histogram[i]
            if (seen >= target) return i
        }
        return 255
    }

    private fun findLineSegments(
        dark: BooleanArray,
        width: Int,
        height: Int,
        region: GraphRegion,
        orientation: CvLineOrientation,
    ): List<CvLineSegmentAudit> {
        if (width <= 4 || height <= 4) return emptyList()
        val segments = mutableListOf<CvLineSegmentAudit>()
        val major = if (orientation == CvLineOrientation.HORIZONTAL) width else height
        val minor = if (orientation == CvLineOrientation.HORIZONTAL) height else width
        val minLength = (major * if (orientation == CvLineOrientation.HORIZONTAL) 0.22f else 0.18f)
            .roundToInt()
            .coerceAtLeast(18)
        val maxGap = (major * 0.012f).roundToInt().coerceIn(2, 18)

        for (minorIndex in 0 until minor) {
            var start = -1
            var lastDark = -1
            var darkPixels = 0
            var gap = 0
            for (majorIndex in 0 until major) {
                val isDark = if (orientation == CvLineOrientation.HORIZONTAL) {
                    dark[minorIndex * width + majorIndex]
                } else {
                    dark[majorIndex * width + minorIndex]
                }
                if (isDark) {
                    if (start < 0) start = majorIndex
                    lastDark = majorIndex
                    darkPixels++
                    gap = 0
                } else if (start >= 0) {
                    gap++
                    if (gap > maxGap) {
                        addLineSegment(
                            segments = segments,
                            orientation = orientation,
                            region = region,
                            minorIndex = minorIndex,
                            start = start,
                            end = lastDark,
                            darkPixels = darkPixels,
                            majorLength = major,
                            minLength = minLength,
                        )
                        start = -1
                        lastDark = -1
                        darkPixels = 0
                        gap = 0
                    }
                }
            }
            if (start >= 0) {
                addLineSegment(
                    segments = segments,
                    orientation = orientation,
                    region = region,
                    minorIndex = minorIndex,
                    start = start,
                    end = lastDark,
                    darkPixels = darkPixels,
                    majorLength = major,
                    minLength = minLength,
                )
            }
        }

        return mergeAdjacentLineSegments(segments, orientation)
            .sortedByDescending { it.score }
            .take(MaxLinesPerOrientation)
    }

    private fun addLineSegment(
        segments: MutableList<CvLineSegmentAudit>,
        orientation: CvLineOrientation,
        region: GraphRegion,
        minorIndex: Int,
        start: Int,
        end: Int,
        darkPixels: Int,
        majorLength: Int,
        minLength: Int,
    ) {
        val length = end - start + 1
        if (length < minLength) return
        val density = darkPixels.toFloat() / length.coerceAtLeast(1).toFloat()
        if (density < 0.42f) return
        val lengthRatio = length.toFloat() / majorLength.coerceAtLeast(1).toFloat()
        val score = (lengthRatio * 0.72f + density * 0.28f).coerceIn(0f, 1.5f)
        val segment = if (orientation == CvLineOrientation.HORIZONTAL) {
            CvLineSegmentAudit(
                orientation = orientation,
                x1 = region.x + start,
                y1 = region.y + minorIndex,
                x2 = region.x + end,
                y2 = region.y + minorIndex,
                length = length,
                lengthRatio = lengthRatio,
                darkDensity = density,
                score = score,
            )
        } else {
            CvLineSegmentAudit(
                orientation = orientation,
                x1 = region.x + minorIndex,
                y1 = region.y + start,
                x2 = region.x + minorIndex,
                y2 = region.y + end,
                length = length,
                lengthRatio = lengthRatio,
                darkDensity = density,
                score = score,
            )
        }
        segments += segment
    }

    private fun mergeAdjacentLineSegments(
        segments: List<CvLineSegmentAudit>,
        orientation: CvLineOrientation,
    ): List<CvLineSegmentAudit> {
        if (segments.size <= 1) return segments
        val coordinateTolerance = 2
        val overlapTolerance = 8
        val sorted = segments.sortedWith(
            if (orientation == CvLineOrientation.HORIZONTAL) {
                compareBy<CvLineSegmentAudit> { it.y1 }.thenBy { it.x1 }
            } else {
                compareBy<CvLineSegmentAudit> { it.x1 }.thenBy { it.y1 }
            },
        )
        val merged = mutableListOf<CvLineSegmentAudit>()
        sorted.forEach { segment ->
            val previous = merged.lastOrNull()
            if (previous != null && canMerge(previous, segment, orientation, coordinateTolerance, overlapTolerance)) {
                merged[merged.lastIndex] = merge(previous, segment, orientation)
            } else {
                merged += segment
            }
        }
        return merged
    }

    private fun canMerge(
        a: CvLineSegmentAudit,
        b: CvLineSegmentAudit,
        orientation: CvLineOrientation,
        coordinateTolerance: Int,
        overlapTolerance: Int,
    ): Boolean =
        if (orientation == CvLineOrientation.HORIZONTAL) {
            abs(a.y1 - b.y1) <= coordinateTolerance && b.x1 <= a.x2 + overlapTolerance
        } else {
            abs(a.x1 - b.x1) <= coordinateTolerance && b.y1 <= a.y2 + overlapTolerance
        }

    private fun merge(
        a: CvLineSegmentAudit,
        b: CvLineSegmentAudit,
        orientation: CvLineOrientation,
    ): CvLineSegmentAudit {
        val x1 = min(a.x1, b.x1)
        val y1 = min(a.y1, b.y1)
        val x2 = max(a.x2, b.x2)
        val y2 = max(a.y2, b.y2)
        val length = if (orientation == CvLineOrientation.HORIZONTAL) x2 - x1 + 1 else y2 - y1 + 1
        val density = ((a.darkDensity + b.darkDensity) / 2f).coerceIn(0f, 1f)
        val major = if (orientation == CvLineOrientation.HORIZONTAL) {
            max(a.length / a.lengthRatio.coerceAtLeast(0.001f), b.length / b.lengthRatio.coerceAtLeast(0.001f))
        } else {
            max(a.length / a.lengthRatio.coerceAtLeast(0.001f), b.length / b.lengthRatio.coerceAtLeast(0.001f))
        }
        val lengthRatio = (length.toFloat() / major.coerceAtLeast(1f)).coerceIn(0f, 1.5f)
        return CvLineSegmentAudit(
            orientation = orientation,
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            length = length,
            lengthRatio = lengthRatio,
            darkDensity = density,
            score = (lengthRatio * 0.72f + density * 0.28f).coerceIn(0f, 1.5f),
        )
    }

    private fun findComponents(
        dark: BooleanArray,
        width: Int,
        height: Int,
        region: GraphRegion,
    ): List<CvComponentAudit> {
        if (width <= 0 || height <= 0 || dark.isEmpty()) return emptyList()
        val visited = BooleanArray(dark.size)
        val queue = IntArray(dark.size)
        val components = mutableListOf<CvComponentAudit>()
        val imageArea = (width.toFloat() * height.toFloat()).coerceAtLeast(1f)

        for (index in dark.indices) {
            if (!dark[index] || visited[index]) continue
            var head = 0
            var tail = 0
            queue[tail++] = index
            visited[index] = true
            var minX = index % width
            var maxX = minX
            var minY = index / width
            var maxY = minY
            var count = 0

            while (head < tail) {
                val current = queue[head++]
                val x = current % width
                val y = current / width
                count++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                for (ny in max(0, y - 1)..min(height - 1, y + 1)) {
                    for (nx in max(0, x - 1)..min(width - 1, x + 1)) {
                        if (nx == x && ny == y) continue
                        val next = ny * width + nx
                        if (dark[next] && !visited[next]) {
                            visited[next] = true
                            queue[tail++] = next
                        }
                    }
                }
            }

            if (count < 6) continue
            val componentWidth = maxX - minX + 1
            val componentHeight = maxY - minY + 1
            val boxArea = (componentWidth * componentHeight).coerceAtLeast(1)
            val areaRatio = boxArea.toFloat() / imageArea
            val fillRatio = count.toFloat() / boxArea.toFloat()
            val aspect = componentWidth.toFloat() / componentHeight.coerceAtLeast(1).toFloat()
            val slenderBonus = when {
                aspect >= 8f || aspect <= 0.125f -> 0.22f
                aspect >= 4f || aspect <= 0.25f -> 0.12f
                else -> 0f
            }
            val score = (areaRatio * 4f + fillRatio * 0.35f + slenderBonus).coerceIn(0f, 2f)
            components += CvComponentAudit(
                x = region.x + minX,
                y = region.y + minY,
                width = componentWidth,
                height = componentHeight,
                pixelCount = count,
                areaRatio = areaRatio,
                fillRatio = fillRatio,
                aspectRatio = aspect,
                score = score,
            )
        }

        return components
            .sortedByDescending { it.score }
            .take(MaxComponents)
    }

    private fun proposeFrameCandidates(
        region: GraphRegion,
        horizontalLines: List<CvLineSegmentAudit>,
        verticalLines: List<CvLineSegmentAudit>,
    ): List<CvFrameCandidateAudit> {
        val h = horizontalLines
            .filter { it.lengthRatio >= 0.30f }
            .sortedByDescending { it.score }
            .take(16)
        val v = verticalLines
            .filter { it.lengthRatio >= 0.22f }
            .sortedByDescending { it.score }
            .take(16)
        if (h.isEmpty() || v.isEmpty()) return emptyList()

        val candidates = mutableListOf<CvFrameCandidateAudit>()
        val graphArea = region.area.toFloat().coerceAtLeast(1f)
        val hGroups = h.groupByCloseCoordinate { it.y1 }
        val vGroups = v.groupByCloseCoordinate { it.x1 }

        hGroups.forEachIndexed { topIndex, topGroup ->
            hGroups.drop(topIndex + 1).forEach { bottomGroup ->
                val topY = topGroup.coordinate
                val bottomY = bottomGroup.coordinate
                if (bottomY - topY < region.height * 0.16f) return@forEach
                vGroups.forEachIndexed { leftIndex, leftGroup ->
                    vGroups.drop(leftIndex + 1).forEach { rightGroup ->
                        val leftX = leftGroup.coordinate
                        val rightX = rightGroup.coordinate
                        if (rightX - leftX < region.width * 0.24f) return@forEach
                        val candidateRegion = GraphRegion(
                            x = leftX,
                            y = topY,
                            width = rightX - leftX + 1,
                            height = bottomY - topY + 1,
                            label = "cv frame candidate",
                        ).clampedTo(region.x + region.width, region.y + region.height, minX = region.x, minY = region.y)
                            ?: return@forEach
                        val areaRatio = candidateRegion.area.toFloat() / graphArea
                        if (areaRatio !in 0.04f..0.96f) return@forEach
                        val hSupport = topGroup.segments.size + bottomGroup.segments.size
                        val vSupport = leftGroup.segments.size + rightGroup.segments.size
                        val coverage = (
                            (topGroup.maxLengthRatio + bottomGroup.maxLengthRatio) +
                                (leftGroup.maxLengthRatio + rightGroup.maxLengthRatio)
                            ) / 4f
                        candidates += CvFrameCandidateAudit(
                            region = candidateRegion,
                            horizontalSupport = hSupport,
                            verticalSupport = vSupport,
                            areaRatioWithinGraph = areaRatio,
                            score = (coverage * 0.75f + areaRatio.coerceAtMost(0.75f) * 0.25f)
                                .coerceIn(0f, 1.5f),
                        )
                    }
                }
            }
        }

        return candidates
            .distinctBy { "${it.region.x}:${it.region.y}:${it.region.width}:${it.region.height}" }
            .sortedByDescending { it.score }
            .take(MaxFrameCandidates)
    }

    private fun proposeComponentFrameCandidates(
        region: GraphRegion,
        contourCandidates: List<CvComponentAudit>,
    ): List<CvFrameCandidateAudit> {
        val graphArea = region.area.toFloat().coerceAtLeast(1f)
        return contourCandidates
            .filter { component ->
                component.areaRatio in 0.035f..1.02f &&
                    component.aspectRatio in 0.45f..6.0f &&
                    component.fillRatio in 0.02f..0.80f
            }
            .map { component ->
                val candidateRegion = GraphRegion(
                    x = component.x,
                    y = component.y,
                    width = component.width,
                    height = component.height,
                    label = "cv component frame candidate",
                ).clampedTo(region.x + region.width, region.y + region.height, minX = region.x, minY = region.y)
                    ?: return@map null
                val areaRatio = candidateRegion.area.toFloat() / graphArea
                CvFrameCandidateAudit(
                    region = candidateRegion,
                    horizontalSupport = 0,
                    verticalSupport = 0,
                    areaRatioWithinGraph = areaRatio,
                    score = (0.55f + areaRatio.coerceAtMost(0.82f) * 0.35f + component.fillRatio * 0.10f)
                        .coerceIn(0f, 1.5f),
                )
            }
            .filterNotNull()
            .take(MaxFrameCandidates)
    }

    private fun List<CvLineSegmentAudit>.groupByCloseCoordinate(
        coordinate: (CvLineSegmentAudit) -> Int,
    ): List<LineCoordinateGroup> {
        if (isEmpty()) return emptyList()
        val sorted = sortedBy(coordinate)
        val groups = mutableListOf<MutableList<CvLineSegmentAudit>>()
        sorted.forEach { line ->
            val group = groups.lastOrNull()
            if (group != null && abs(coordinate(group.last()) - coordinate(line)) <= 4) {
                group += line
            } else {
                groups += mutableListOf(line)
            }
        }
        return groups.map { lines ->
            LineCoordinateGroup(
                coordinate = lines.map(coordinate).average().roundToInt(),
                segments = lines,
            )
        }
    }

    private fun writeOverlay(
        source: BufferedImage,
        audit: CvGeometryAudit,
        outputPath: Path,
    ) {
        val overlay = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = overlay.createGraphics()
        try {
            graphics.drawImage(source, 0, 0, null)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val baseStroke = (source.width.coerceAtLeast(source.height) / 360f).coerceAtLeast(1.5f)
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (source.width / 44).coerceIn(11, 20))

            audit.graphs.forEach { graph ->
                graphics.stroke = BasicStroke(baseStroke * 2.2f)
                graphics.color = Color(0x15, 0x65, 0xC0, 220)
                graphics.drawRegion(graph.region)
                graphics.drawString(
                    "cv graph ${graph.graphIndex}",
                    (graph.region.x + 8).coerceAtMost(source.width - 1),
                    (graph.region.y + 22).coerceIn(18, source.height - 1),
                )

                graphics.stroke = BasicStroke(baseStroke)
                graph.topHorizontalLines.take(28).forEach {
                    graphics.color = Color(0xD3, 0x2F, 0x2F, 185)
                    graphics.drawLine(it.x1, it.y1, it.x2, it.y2)
                }
                graph.topVerticalLines.take(28).forEach {
                    graphics.color = Color(0x00, 0x96, 0x88, 185)
                    graphics.drawLine(it.x1, it.y1, it.x2, it.y2)
                }

                graphics.stroke = BasicStroke(baseStroke)
                graph.contourCandidates.take(14).forEach {
                    graphics.color = Color(0xFF, 0x8F, 0x00, 150)
                    graphics.drawRect(it.x, it.y, it.width.coerceAtLeast(1), it.height.coerceAtLeast(1))
                }

                graphics.stroke = BasicStroke(baseStroke * 1.8f)
                graph.frameCandidates.take(4).forEachIndexed { index, candidate ->
                    graphics.color = if (index == 0) {
                        Color(0x1B, 0x8A, 0x3A, 230)
                    } else {
                        Color(0x7B, 0x1F, 0xA2, 190)
                    }
                    graphics.drawRegion(candidate.region)
                }
            }

            graphics.color = Color(255, 255, 255, 210)
            graphics.fillRect(8, 8, 520.coerceAtMost(source.width - 16), 58)
            graphics.color = Color(0x20, 0x20, 0x20, 230)
            graphics.drawString("CV geometry spike: blue graph, red H, teal V, orange components, green frame", 18, 32)
            graphics.drawString("Evidence only: mirrors OpenCV Hough/LSD + components/contours before production swap", 18, 54)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(overlay, "png", outputPath.toFile())
        overlay.flush()
    }

    private fun java.awt.Graphics2D.drawRegion(region: GraphRegion) {
        drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
    }

    private data class LineCoordinateGroup(
        val coordinate: Int,
        val segments: List<CvLineSegmentAudit>,
    ) {
        val maxLengthRatio: Float = segments.maxOfOrNull { it.lengthRatio } ?: 0f
    }
}

private fun GraphRegion.clampedTo(
    imageWidth: Int,
    imageHeight: Int,
    minX: Int = 0,
    minY: Int = 0,
): GraphRegion? {
    if (imageWidth <= minX || imageHeight <= minY) return null
    val left = x.coerceIn(minX, imageWidth - 1)
    val top = y.coerceIn(minY, imageHeight - 1)
    val right = (x + width).coerceIn(left + 1, imageWidth)
    val bottom = (y + height).coerceIn(top + 1, imageHeight)
    if (right <= left || bottom <= top) return null
    return copy(
        x = left,
        y = top,
        width = right - left,
        height = bottom - top,
    )
}
