package com.chromalab.app

import com.chromalab.feature.processing.bench.OfflineAnalysisAudit
import com.chromalab.feature.processing.bench.OfflineAnalysisAuditArtifacts
import com.chromalab.feature.processing.bench.OfflineAnalysisInput
import com.chromalab.feature.processing.bench.OfflineAnalysisRunner
import com.chromalab.feature.processing.bench.OfflineAxisCalibrationPointAudit
import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import kotlin.system.exitProcess

private const val OfflineAnalysisCommand = "--offline-analysis"

fun runDesktopOfflineAnalysisCli(args: Array<String>): Boolean {
    if (args.none { it == OfflineAnalysisCommand || it == "offline-analysis" }) return false

    val options = DesktopOfflineAnalysisOptions.parse(args.toList())
    if (options.help) {
        printDesktopOfflineAnalysisUsage()
        return true
    }

    val imagePath = options.imagePath?.let { Path.of(it).toAbsolutePath().normalize() }
        ?: failCli("Missing --image <path>.")
    if (!Files.isRegularFile(imagePath)) {
        failCli("Image file does not exist: $imagePath")
    }

    val sourceId = options.sourceId ?: imagePath.fileName.toString()
        .substringBeforeLast('.')
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .ifBlank { "desktop_image" }
    val outputDir = options.outputDir?.let { Path.of(it).toAbsolutePath().normalize() }
        ?: Path.of("build", "desktop-offline-analysis", sourceId).toAbsolutePath().normalize()
    Files.createDirectories(outputDir)

    val audit = runBlocking {
        OfflineAnalysisRunner().run(
            OfflineAnalysisInput(
                sourceId = sourceId,
                imagePath = imagePath.toString(),
                outputDir = outputDir.toString(),
                expectedGraphCount = options.expectedGraphCount,
            ),
        )
    }

    DesktopOfflineAnalysisArtifactWriter.write(
        audit = audit,
        sourceImagePath = imagePath,
        outputDir = outputDir,
    )

    println("ChromaLab desktop offline analysis")
    println("Source: ${audit.sourceId}")
    println("Output: $outputDir")
    println("Detected graphs: ${audit.detectedGraphCount}")
    println("Ready for calculation: ${audit.readyForCalculation}")
    println("Blocked at stage: ${audit.blockedAtStage ?: "not blocked"}")
    println("Summary: ${outputDir.resolve("audit_summary.md")}")
    println("Report draft: ${outputDir.resolve("calibrated_report.md")}")

    return true
}

private data class DesktopOfflineAnalysisOptions(
    val imagePath: String?,
    val outputDir: String?,
    val sourceId: String?,
    val expectedGraphCount: Int?,
    val help: Boolean,
) {
    companion object {
        fun parse(args: List<String>): DesktopOfflineAnalysisOptions {
            val values = mutableMapOf<String, String>()
            val flags = mutableSetOf<String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                if (!arg.startsWith("--")) {
                    index++
                    continue
                }
                val keyValue = arg.removePrefix("--").split("=", limit = 2)
                val key = keyValue[0]
                val inlineValue = keyValue.getOrNull(1)
                if (key == "help" || key == "h" || key == OfflineAnalysisCommand.removePrefix("--")) {
                    flags += key
                    index++
                    continue
                }
                val value = inlineValue ?: args.getOrNull(index + 1)?.takeUnless { it.startsWith("--") }
                if (value == null) {
                    failCli("Missing value for --$key.")
                }
                values[key] = value
                index += if (inlineValue == null) 2 else 1
            }

            return DesktopOfflineAnalysisOptions(
                imagePath = values["image"],
                outputDir = values["out"],
                sourceId = values["source"],
                expectedGraphCount = values["expected-graphs"]?.toIntOrNull()
                    ?: values["expectedGraphCount"]?.toIntOrNull(),
                help = flags.contains("help") || flags.contains("h"),
            )
        }
    }
}

private object DesktopOfflineAnalysisArtifactWriter {
    fun write(
        audit: OfflineAnalysisAudit,
        sourceImagePath: Path,
        outputDir: Path,
    ) {
        Files.createDirectories(outputDir)
        Files.writeString(outputDir.resolve("audit.json"), OfflineAnalysisAuditArtifacts.toJson(audit))
        Files.writeString(outputDir.resolve("audit_summary.md"), OfflineAnalysisAuditArtifacts.toSummaryMarkdown(audit))
        Files.writeString(
            outputDir.resolve("calibrated_report_ui_contract.json"),
            OfflineAnalysisAuditArtifacts.toCalibratedReportUiContractJson(audit),
        )
        Files.writeString(outputDir.resolve("calibrated_report.md"), OfflineAnalysisAuditArtifacts.toCalibratedReportMarkdown(audit))

        val overlayImagePath = audit.orientationCorrection?.imagePath?.let { Path.of(it) } ?: sourceImagePath
        writeGraphCandidateOverlay(audit, overlayImagePath, outputDir.resolve("graph_candidates.png"))
        writeSelectedPreprocessingCrops(audit, outputDir)
        writeGraphFocusArtifacts(audit, overlayImagePath, outputDir)
        writePeakOverlayArtifacts(audit, outputDir)
    }

    private fun writeGraphCandidateOverlay(
        audit: OfflineAnalysisAudit,
        imagePath: Path,
        outputPath: Path,
    ) {
        val source = ImageIO.read(imagePath.toFile()) ?: return
        val image = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.drawImage(source, 0, 0, null)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 240f).coerceAtLeast(2f))
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (source.width / 34).coerceIn(12, 28))

            audit.graphCandidates.forEach { candidate ->
                graphics.color = if (candidate.accepted) {
                    Color(0x1B, 0x8A, 0x3A, 220)
                } else {
                    Color(0xD3, 0x2F, 0x2F, 220)
                }
                val region = candidate.region
                graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
                graphics.drawString(
                    "#${candidate.graphIndex} ${if (candidate.accepted) "accepted" else "rejected"}",
                    (region.x + 8).coerceAtMost(source.width - 1),
                    (region.y + 24).coerceIn(24, source.height - 1),
                )
            }

            graphics.color = Color(0x15, 0x65, 0xC0, 230)
            graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 140f).coerceAtLeast(3f))
            audit.graphs.forEach { graph ->
                val region = graph.region
                graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
            }

            graphics.color = Color(0xFF, 0x8F, 0x00, 230)
            graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 180f).coerceAtLeast(2f))
            audit.graphs.forEach { graph ->
                val region = graph.plotArea.region ?: return@forEach
                graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
            }
        } finally {
            graphics.dispose()
            source.flush()
        }
        ImageIO.write(image, "png", outputPath.toFile())
        image.flush()
    }

    private fun writeSelectedPreprocessingCrops(audit: OfflineAnalysisAudit, outputDir: Path) {
        audit.graphs.forEach { graph ->
            val imagePath = graph.selectedPreprocessingImagePath ?: return@forEach
            val source = ImageIO.read(Path.of(imagePath).toFile()) ?: return@forEach
            val region = graph.region.clampedTo(source.width, source.height)
            val crop = BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = crop.createGraphics()
            try {
                graphics.drawImage(
                    source,
                    0,
                    0,
                    region.width,
                    region.height,
                    region.x,
                    region.y,
                    region.x + region.width,
                    region.y + region.height,
                    null,
                )
            } finally {
                graphics.dispose()
                source.flush()
            }
            ImageIO.write(crop, "png", outputDir.resolve("selected_preprocessing_graph_${graph.graphIndex}.png").toFile())
            crop.flush()
        }
    }

    private fun writeGraphFocusArtifacts(
        audit: OfflineAnalysisAudit,
        imagePath: Path,
        outputDir: Path,
    ) {
        val source = ImageIO.read(imagePath.toFile()) ?: return
        try {
            audit.graphs.forEach { graph ->
                val region = graph.region.clampedTo(source.width, source.height)
                val focus = BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = focus.createGraphics()
                try {
                    graphics.drawImage(
                        source,
                        0,
                        0,
                        region.width,
                        region.height,
                        region.x,
                        region.y,
                        region.x + region.width,
                        region.y + region.height,
                        null,
                    )
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    graphics.stroke = BasicStroke((region.width.coerceAtLeast(region.height) / 160f).coerceAtLeast(2f))
                    graph.plotArea.region?.let { plot ->
                        val x = (plot.x - region.x).coerceIn(0, region.width - 1)
                        val y = (plot.y - region.y).coerceIn(0, region.height - 1)
                        val right = (plot.x + plot.width - region.x).coerceIn(0, region.width)
                        val bottom = (plot.y + plot.height - region.y).coerceIn(0, region.height)
                        graphics.color = Color(0x15, 0x65, 0xC0, 230)
                        graphics.drawRect(x, y, (right - x).coerceAtLeast(1), (bottom - y).coerceAtLeast(1))
                        graphics.color = Color(0x4C, 0xAF, 0x50, 210)
                        graphics.drawLine(x, bottom, right, bottom)
                        graphics.color = Color(0x21, 0x96, 0xF3, 210)
                        graphics.drawLine(x, y, x, bottom)
                    }
                } finally {
                    graphics.dispose()
                }
                ImageIO.write(focus, "png", outputDir.resolve("graph_focus_graph_${graph.graphIndex}.png").toFile())
                focus.flush()
            }
        } finally {
            source.flush()
        }
    }

    private fun writePeakOverlayArtifacts(audit: OfflineAnalysisAudit, outputDir: Path) {
        audit.graphs
            .filter { it.peakMetrics.ready && it.peakDetection.peaks.isNotEmpty() }
            .forEach { graph ->
                val focusPath = outputDir.resolve("graph_focus_graph_${graph.graphIndex}.png")
                if (!Files.exists(focusPath)) return@forEach
                val focus = ImageIO.read(focusPath.toFile()) ?: return@forEach
                val overlay = BufferedImage(focus.width, focus.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = overlay.createGraphics()
                try {
                    graphics.drawImage(focus, 0, 0, null)
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    graphics.stroke = BasicStroke((focus.width.coerceAtLeast(focus.height) / 260f).coerceAtLeast(2f))
                    graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (focus.width / 44).coerceIn(10, 18))

                    val plot = graph.plotArea.region ?: return@forEach
                    val plotLocalX = (plot.x - graph.region.x).coerceIn(0, focus.width - 1)
                    val plotLocalY = (plot.y - graph.region.y).coerceIn(0, focus.height - 1)
                    val plotRight = (plotLocalX + plot.width).coerceIn(0, focus.width)
                    val plotBottom = (plotLocalY + plot.height).coerceIn(0, focus.height)
                    val xPair = graph.axisCalibration.xCandidates.maxPixelSpanPair() ?: return@forEach
                    val yPair = graph.axisCalibration.yCandidates.maxPixelSpanPair() ?: return@forEach

                    graph.peakDetection.peaks.forEach { peak ->
                        val left = (plotLocalX + xPair.valueToPixel(peak.leftBoundaryTime)).roundToInt()
                            .coerceIn(plotLocalX, plotRight)
                        val apex = (plotLocalX + xPair.valueToPixel(peak.rtApex)).roundToInt()
                            .coerceIn(plotLocalX, plotRight)
                        val right = (plotLocalX + xPair.valueToPixel(peak.rightBoundaryTime)).roundToInt()
                            .coerceIn(plotLocalX, plotRight)
                        val apexY = (plotLocalY + yPair.valueToPixel(peak.height)).roundToInt()
                            .coerceIn(plotLocalY, plotBottom)

                        graphics.color = Color(0x15, 0x65, 0xC0, 170)
                        graphics.drawLine(left, plotLocalY, left, plotBottom)
                        graphics.drawLine(right, plotLocalY, right, plotBottom)
                        graphics.color = Color(0xD3, 0x2F, 0x2F, 210)
                        graphics.drawLine(apex, plotLocalY, apex, plotBottom)
                        graphics.fillOval(apex - 4, apexY - 4, 8, 8)
                        graphics.drawString(
                            peak.peakNumber.toString(),
                            (apex + 4).coerceAtMost(focus.width - 18),
                            (apexY - 6).coerceAtLeast(12),
                        )
                    }
                } finally {
                    graphics.dispose()
                    focus.flush()
                }
                ImageIO.write(overlay, "png", outputDir.resolve("peak_overlay_graph_${graph.graphIndex}.png").toFile())
                overlay.flush()
            }
    }
}

private fun GraphRegion.clampedTo(imageWidth: Int, imageHeight: Int): GraphRegion {
    val x = this.x.coerceIn(0, imageWidth - 1)
    val y = this.y.coerceIn(0, imageHeight - 1)
    val width = this.width.coerceIn(1, imageWidth - x)
    val height = this.height.coerceIn(1, imageHeight - y)
    return GraphRegion(x = x, y = y, width = width, height = height, label = label)
}

private fun List<OfflineAxisCalibrationPointAudit>.maxPixelSpanPair():
    Pair<OfflineAxisCalibrationPointAudit, OfflineAxisCalibrationPointAudit>? {
    if (size < 2) return null
    val sorted = sortedBy { it.pixel }
    return sorted.first() to sorted.last()
}

private fun Pair<OfflineAxisCalibrationPointAudit, OfflineAxisCalibrationPointAudit>.valueToPixel(value: Double): Double {
    val pixelSpan = second.pixel - first.pixel
    val valueSpan = second.value - first.value
    if (pixelSpan == 0f || valueSpan == 0f) return first.pixel.toDouble()
    return first.pixel + ((value - first.value) / valueSpan) * pixelSpan
}

private fun printDesktopOfflineAnalysisUsage() {
    println(
        """
        ChromaLab desktop offline analysis

        Usage:
          --offline-analysis --image <path> [--out <dir>] [--source <id>] [--expected-graphs <n>]

        Outputs:
          audit.json
          audit_summary.md
          calibrated_report_ui_contract.json
          calibrated_report.md
          graph_candidates.png
          graph_focus_graph_N.png
          selected_preprocessing_graph_N.png
          peak_overlay_graph_N.png when calculation reaches peak metrics
        """.trimIndent(),
    )
}

private fun failCli(message: String): Nothing {
    System.err.println(message)
    printDesktopOfflineAnalysisUsage()
    exitProcess(2)
}
