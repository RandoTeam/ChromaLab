package com.chromalab.feature.processing.sweep

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionBoundaryCorrector
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.graph.requiresGraphPanelBoundaryMode
import com.chromalab.feature.processing.inference.ChartAnalysisReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.preprocess.ImagePreprocessor
import com.chromalab.feature.processing.preprocess.PreprocessingParams
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.curve.scaledCoordinates
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Auto-Sweep Engine: tests multiple preprocessing configurations
 * on a single image and selects the best one based on measured graph, axis,
 * OCR, and curve quality.
 *
 * Flow:
 * 1. Graph detection — run ONCE (same image → same result)
 * 2. OCR — run ONCE with multi-level scan (already handles label detection)
 * 3. For each preprocessing config:
 *    a. Preprocess image
 *    b. Extract curve from preprocessed result
 *    c. Score: graph crop + axis geometry + OCR quality + curve integrity
 * 4. Return results sorted by score
 */
class AutoSweepEngine {

    data class SweepConfig(
        val name: String,
        val params: PreprocessingParams,
        val inputVariant: SweepInputVariant = SweepInputVariant.CONTRAST,
        val description: String,
    )

    enum class SweepInputVariant {
        ORIGINAL,
        GRAYSCALE,
        CONTRAST,
        SHARPENED,
        BINARY,
        SCAN_STYLE,
    }

    data class SweepResult(
        val config: SweepConfig,
        val preprocessingResult: PreprocessingResult?,
        val graphResult: GraphRegionResult?,
        val selectedRegion: GraphRegion?,
        val ocrResult: AxisOcrResult?,
        val axesResult: AxesResult?,
        val curveResult: CurveExtractionResult?,
        val score: Float,
        val scoreBreakdown: String,
    )

    data class SweepProgress(
        val currentConfig: Int,
        val totalConfigs: Int,
        val configName: String,
        val phase: String,
    )

    /**
     * Preset configurations and image variants covering the common scan conditions.
     */
    val configs: List<SweepConfig> = listOf(
        SweepConfig(
            name = "default",
            params = PreprocessingParams(
                claheClipLimit = 2.0f, claheTileSize = 8,
                adaptiveBlockSize = 31, adaptiveC = 10,
                morphKernelSize = 3, morphIterations = 1, medianFilterSize = 3,
            ),
            description = "Стандартный",
        ),
        SweepConfig(
            name = "high_contrast",
            params = PreprocessingParams(
                claheClipLimit = 4.0f, claheTileSize = 8,
                adaptiveBlockSize = 21, adaptiveC = 8,
                morphKernelSize = 3, morphIterations = 1, medianFilterSize = 3,
            ),
            description = "Высокий контраст",
        ),
        SweepConfig(
            name = "soft_binary",
            params = PreprocessingParams(
                claheClipLimit = 1.5f, claheTileSize = 8,
                adaptiveBlockSize = 41, adaptiveC = 12,
                morphKernelSize = 3, morphIterations = 0, medianFilterSize = 3,
            ),
            description = "Мягкая бинаризация",
        ),
        SweepConfig(
            name = "no_morph",
            params = PreprocessingParams(
                claheClipLimit = 2.0f, claheTileSize = 8,
                adaptiveBlockSize = 31, adaptiveC = 10,
                morphKernelSize = 3, morphIterations = 0, medianFilterSize = 3,
            ),
            description = "Без морфологии",
        ),
        SweepConfig(
            name = "fine_detail",
            params = PreprocessingParams(
                claheClipLimit = 1.0f, claheTileSize = 12,
                adaptiveBlockSize = 15, adaptiveC = 5,
                morphKernelSize = 3, morphIterations = 2, medianFilterSize = 3,
            ),
            description = "Мелкие детали",
        ),
        SweepConfig(
            name = "ultra_contrast",
            params = PreprocessingParams(
                claheClipLimit = 6.0f, claheTileSize = 6,
                adaptiveBlockSize = 17, adaptiveC = 6,
                morphKernelSize = 3, morphIterations = 1, medianFilterSize = 5,
            ),
            description = "Ультра-контраст",
        ),
        SweepConfig(
            name = "large_tile",
            params = PreprocessingParams(
                claheClipLimit = 3.0f, claheTileSize = 16,
                adaptiveBlockSize = 51, adaptiveC = 15,
                morphKernelSize = 3, morphIterations = 1, medianFilterSize = 3,
            ),
            description = "Крупная плитка CLAHE",
        ),
        SweepConfig(
            name = "minimal",
            params = PreprocessingParams(
                claheClipLimit = 1.0f, claheTileSize = 8,
                adaptiveBlockSize = 61, adaptiveC = 20,
                morphKernelSize = 3, morphIterations = 0, medianFilterSize = 1,
            ),
            description = "Минимальная обработка",
        ),
        SweepConfig(
            name = "original",
            params = PreprocessingParams(),
            inputVariant = SweepInputVariant.ORIGINAL,
            description = "Original",
        ),
        SweepConfig(
            name = "grayscale",
            params = PreprocessingParams(),
            inputVariant = SweepInputVariant.GRAYSCALE,
            description = "Grayscale",
        ),
        SweepConfig(
            name = "contrast",
            params = PreprocessingParams(),
            inputVariant = SweepInputVariant.CONTRAST,
            description = "Contrast",
        ),
        SweepConfig(
            name = "sharpened",
            params = PreprocessingParams(),
            inputVariant = SweepInputVariant.SHARPENED,
            description = "Sharpened",
        ),
        SweepConfig(
            name = "binary",
            params = PreprocessingParams(),
            inputVariant = SweepInputVariant.BINARY,
            description = "Binary",
        ),
        SweepConfig(
            name = "scan_style",
            params = PreprocessingParams(
                claheClipLimit = 3.0f,
                adaptiveBlockSize = 35,
                adaptiveC = 12,
                morphIterations = 1,
            ),
            inputVariant = SweepInputVariant.SCAN_STYLE,
            description = "Scan-style",
        ),
    )

    /**
     * Run the sweep pipeline.
     *
     * 1. Graph detect + OCR + axis detect: run ONCE (image-dependent, not config-dependent)
     * 2. Per config: preprocess → curve mask → curve extract → score
     * 3. Return sorted results (best first)
     *
     * @param cachedGraphResult Reuse graph detection from first graph (multi-graph mode)
     * @param overrideRegion Use specific region instead of auto-selected (multi-graph mode)
     */
    suspend fun sweep(
        imagePath: String,
        outputDir: String,
        imageWidth: Int,
        imageHeight: Int,
        cachedGraphResult: com.chromalab.feature.processing.graph.GraphRegionResult? = null,
        overrideRegion: com.chromalab.feature.processing.graph.GraphRegion? = null,
        requireVlmForAnalysis: Boolean = false,
        preservePanelLabels: Boolean = false,
        onProgress: (SweepProgress) -> Unit = {},
    ): List<SweepResult> {
        val preprocessor = ImagePreprocessor()
        val graphDetector = GraphRegionDetector()
        val graphBoundaryCorrector = GraphRegionBoundaryCorrector()
        val ocrReader = ChartAnalysisReader()
        val axisDetector = AxisDetector()
        val curveMaskPreparer = CurveMaskPreparer()
        val curveExtractor = CurveExtractor()

        // === SHARED: run once ===
        require(imageWidth > 0 && imageHeight > 0) {
            "Image dimensions are required before graph detection."
        }
        val w = imageWidth
        val h = imageHeight

        // === Strategy A: VLM-first graph detection (always try) ===
        // VLM provides structural understanding of the image;
        // CV refines with pixel-level precision.
        var vlmBounds: com.chromalab.feature.processing.inference.GraphBounds? = null
        var vlmRegion: GraphRegion? = null
        var vlmGraphResult: GraphRegionResult? = null
        if (cachedGraphResult == null && overrideRegion == null) try {
            onProgress(SweepProgress(0, configs.size, "VLM: определение графика", "vlm_region"))
            vlmBounds = ocrReader.detectGraphRegion(imagePath, w, h)
            if (vlmBounds != null) {
                vlmGraphResult = buildVlmGraphResult(vlmBounds, w, h)
                vlmRegion = vlmGraphResult.selectedRegion
                if (vlmRegion != null) {
                    println("SWEEP[VLM] Graph region: ${vlmRegion.x},${vlmRegion.y} ${vlmRegion.width}x${vlmRegion.height} (${vlmBounds.numGraphs} graphs)")
                }
            }
        } catch (e: Exception) {
            println("SWEEP[VLM] Graph detection failed: ${e.message}")
            if (requireVlmForAnalysis) return emptyList()
        }

        // CV graph detection — always run for precision refinement
        if (requireVlmForAnalysis && cachedGraphResult == null && overrideRegion == null && vlmBounds == null) {
            println("SWEEP[ABORT] VLM graph detection is required but did not return bounds")
            return emptyList()
        }

        val cvGraphRes = cachedGraphResult ?: run {
            onProgress(SweepProgress(0, configs.size, "CV: определение графика", "detect"))
            try {
                graphDetector.detect(imagePath, w, h)
            } catch (e: Exception) {
                println("SWEEP[GRAPH] CV detection failed: ${e.message}")
                null
            }
        }
        val graphRes = selectGraphResult(cvGraphRes, vlmGraphResult)

        // Region selection priority: override > CV > VLM fallback.
        val detectedRegion = overrideRegion ?: graphRes?.selectedRegion ?: vlmRegion
        val preservePanelLabelsForRun = preservePanelLabels ||
            graphRes?.filteredRegions.orEmpty().any { it.requiresGraphPanelBoundaryMode(w, h) } ||
            detectedRegion?.requiresGraphPanelBoundaryMode(w, h) == true
        val region = detectedRegion?.let { selected ->
            val correction = graphBoundaryCorrector.correct(
                imagePath = imagePath,
                region = selected,
                imageWidth = w,
                imageHeight = h,
                preservePanelLabels = preservePanelLabelsForRun,
            )
            if (correction.changed) {
                println("SWEEP[GRAPH_BOUNDARY] ${correction.warnings.joinToString()} corrected=${correction.correctedRegion}")
            }
            correction.correctedRegion
        }
        println("SWEEP[GRAPH] detected ${graphRes?.sortedRegions?.size ?: 0} CV regions, VLM=${vlmRegion != null}, selected=$region")

        if (region == null) {
            println("SWEEP[ABORT] No graph region found (both VLM and CV failed)")
            return emptyList()
        }

        onProgress(SweepProgress(0, configs.size, "OCR осей", "ocr"))
        val ocrResult = try {
            ocrReader.readAxisLabels(imagePath, region)
        } catch (e: Exception) {
            println("SWEEP[OCR] failed: ${e.message}")
            null
        }
        if (requireVlmForAnalysis && ocrResult == null) {
            println("SWEEP[ABORT] VLM axis OCR is required but failed")
            return emptyList()
        }
        println("SWEEP[OCR] x=${ocrResult?.suggestedXValues}, y=${ocrResult?.suggestedYValues}")

        onProgress(SweepProgress(0, configs.size, "Определение осей", "axes"))
        val axesRes = try {
            axisDetector.detect(imagePath, region)
        } catch (e: Exception) {
            println("SWEEP[AXES] failed: ${e.message}")
            null
        }

        // Shared quality scores (same for all configs, but included in the final
        // preparation score so the selected variant remains auditable end-to-end).
        val graphScore = calculateGraphScore(graphRes, region, w, h)
        val axisScore = calculateAxisScore(axesRes, region)
        val ocrBaseScore = calculateOcrScore(ocrResult, graphRes, region)
        println("SWEEP[GRAPH] score=${graphScore.first} (${graphScore.second})")
        println("SWEEP[AXES] score=${axisScore.first} (${axisScore.second})")
        println("SWEEP[OCR] score=${ocrBaseScore.first} (${ocrBaseScore.second})")

        // === PER-CONFIG: preprocess → curve extract → score ===
        val results = mutableListOf<SweepResult>()

        for ((index, config) in configs.withIndex()) {
            println("SWEEP[${index + 1}/${configs.size}] config='${config.name}'")
            onProgress(SweepProgress(index + 1, configs.size, config.description, "preprocess"))

            val configDir = "$outputDir/sweep_${config.name}"
            val prepResult = try {
                preprocessor.preprocess(imagePath, configDir, config.params)
            } catch (e: Exception) {
                println("SWEEP[${config.name}] preprocess failed: ${e.message}")
                null
            }

            // Curve extraction: use preprocessed image (contrast-enhanced)
            // This is where configs actually differ — different CLAHE/threshold/morphology
            // produces different edges for Canny detection
            onProgress(SweepProgress(index + 1, configs.size, config.description, "curve"))
            val curveInputPath = curveInputPath(imagePath, prepResult, config.inputVariant)
            val curveResult = try {
                val axes = axesRes ?: fallbackAxes()
                val mask = curveMaskPreparer.prepare(curveInputPath, region, axes, configDir)
                val maskPath = mask.cleanMaskPath ?: mask.rawMaskPath ?: curveInputPath
                curveExtractor.extract(maskPath, region.width, region.height, configDir)
                    .scaledCoordinates(mask.coordinateScale)
            } catch (e: Exception) {
                println("SWEEP[${config.name}] curve extraction failed: ${e.message}")
                null
            }

            // Score: measured graph + axis + OCR + curve quality.
            val curveScore = calculateCurveScore(curveResult, region)
            val totalScore = graphScore.first + axisScore.first + ocrBaseScore.first + curveScore.first
            val breakdown = "${graphScore.second}, ${axisScore.second}, ${ocrBaseScore.second}, variant=${config.inputVariant.name.lowercase()}, ${curveScore.second}"
            println(
                "SWEEP[${config.name}] total=$totalScore " +
                    "(graph=${graphScore.first} + axes=${axisScore.first} + ocr=${ocrBaseScore.first} + curve=${curveScore.first})",
            )

            results.add(
                SweepResult(
                    config = config,
                    preprocessingResult = prepResult,
                    graphResult = graphRes,
                    selectedRegion = region,
                    ocrResult = ocrResult,
                    axesResult = axesRes,
                    curveResult = curveResult,
                    score = totalScore,
                    scoreBreakdown = breakdown,
                )
            )
        }

        val sorted = results.sortedByDescending { it.score }
        println("SWEEP[DONE] Best: '${sorted.first().config.name}' score=${sorted.first().score}")
        println("SWEEP[DONE] Ranking: ${sorted.joinToString { "'${it.config.name}'=${it.score}" }}")

        return sorted
    }

    /**
     * Graph crop quality score: confidence, plausible plot area, aspect ratio, and bounds.
     */
    private fun calculateGraphScore(
        graphResult: GraphRegionResult?,
        region: GraphRegion?,
        imageWidth: Int,
        imageHeight: Int,
    ): Pair<Float, String> {
        if (graphResult == null || region == null) return 0f to "graph[none]"

        val parts = mutableListOf<String>()
        var score = when (graphResult.confidence) {
            DetectionConfidence.HIGH -> 40f
            DetectionConfidence.MEDIUM -> 28f
            DetectionConfidence.MANUAL -> 24f
            DetectionConfidence.LOW -> 12f
        }
        parts.add("conf=${graphResult.confidence}")

        val imageArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
        val areaRatio = (region.area.toFloat() / imageArea).coerceIn(0f, 1f)
        val areaScore = when {
            areaRatio < 0.03f -> 0f
            areaRatio > 0.9f -> 5f
            else -> 20f
        }
        score += areaScore
        parts.add("area=${(areaRatio * 100f).roundToInt()}%")

        val aspectScore = when (region.aspectRatio) {
            in 1.0f..4.0f -> 15f
            in 0.7f..5.0f -> 8f
            else -> 0f
        }
        score += aspectScore
        parts.add("aspect=${(region.aspectRatio * 100f).roundToInt() / 100f}")

        val inBounds = region.x >= 0 &&
            region.y >= 0 &&
            region.right <= imageWidth &&
            region.bottom <= imageHeight
        if (inBounds) {
            score += 10f
            parts.add("bounds=ok")
        } else {
            parts.add("bounds=bad")
        }

        val warningPenalty = graphResult.warnings.size * 3f
        if (warningPenalty > 0f) {
            score -= warningPenalty
            parts.add("warn=-${warningPenalty.roundToInt()}")
        }

        return score.coerceAtLeast(0f) to "graph[${parts.joinToString(",")}]"
    }

    /**
     * Axis quality score: both axes, origin, geometric alignment, and length.
     */
    private fun calculateAxisScore(
        axes: AxesResult?,
        region: GraphRegion,
    ): Pair<Float, String> {
        if (axes == null) return 0f to "axes[none]"

        val parts = mutableListOf<String>()
        var score = (axes.confidence.coerceIn(0f, 1f) * 30f)
        parts.add("conf=${(axes.confidence * 100f).roundToInt()}%")

        axes.xAxis?.let { xAxis ->
            score += 15f
            val lengthRatio = (xAxis.length / region.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            score += lengthRatio * 10f
            if (xAxis.isHorizontal) score += 10f
            parts.add("x=${(lengthRatio * 100f).roundToInt()}%")
        } ?: parts.add("x=missing")

        axes.yAxis?.let { yAxis ->
            score += 15f
            val lengthRatio = (yAxis.length / region.height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            score += lengthRatio * 10f
            if (yAxis.isVertical) score += 10f
            parts.add("y=${(lengthRatio * 100f).roundToInt()}%")
        } ?: parts.add("y=missing")

        if (axes.origin != null) {
            score += 10f
            parts.add("origin=ok")
        } else {
            parts.add("origin=missing")
        }

        val warningPenalty = axes.warnings.size * 3f
        if (warningPenalty > 0f) {
            score -= warningPenalty
            parts.add("warn=-${warningPenalty.roundToInt()}")
        }

        return score.coerceAtLeast(0f) to "axes[${parts.joinToString(",")}]"
    }

    /**
     * OCR quality score: label count, step regularity, dual-axis, units.
     */
    private fun calculateOcrScore(
        ocr: AxisOcrResult?,
        graphResult: GraphRegionResult?,
        region: GraphRegion?,
    ): Pair<Float, String> {
        if (ocr == null || region == null) return 0f to "no_ocr"

        val xValues = ocr.suggestedXValues
        val yValues = ocr.suggestedYValues
        val parts = mutableListOf<String>()
        var score = 0f

        // Label counts (15 pts each, max 120)
        val xN = xValues.size.coerceAtMost(8)
        val yN = yValues.size.coerceAtMost(8)
        score += xN * 15f + yN * 15f
        parts.add("xN=$xN")
        parts.add("yN=$yN")

        // Step regularity (0-30 per axis)
        val xStep = stepRegularityScore(xValues)
        val yStep = stepRegularityScore(yValues)
        score += xStep * 30f + yStep * 30f
        parts.add("xR=${(xStep * 30).roundToInt()}")
        parts.add("yR=${(yStep * 30).roundToInt()}")

        // Both axes (20 pts)
        if (xValues.size >= 2 && yValues.size >= 2) {
            score += 20f
            parts.add("both=20")
        }

        // Units (5 pts each)
        if (ocr.xUnit != null) { score += 5f; parts.add("xU=5") }
        if (ocr.yUnit != null) { score += 5f; parts.add("yU=5") }

        // Graph confidence (5 pts)
        if (graphResult?.confidence?.name == "HIGH") { score += 5f; parts.add("conf=5") }

        return score to "ocr[${parts.joinToString(",")}]"
    }

    /**
     * Curve extraction quality score: coverage, continuity, confidence,
     * vertical signal span, interpolation, and outliers.
     */
    private fun calculateCurveScore(
        curve: CurveExtractionResult?,
        region: GraphRegion,
    ): Pair<Float, String> {
        if (curve == null) return 0f to "curve[none]"

        val parts = mutableListOf<String>()
        var score = 0f
        val points = curve.points

        // Point density relative to graph width (0-25 pts).
        val pointDensity = if (region.width > 0) {
            points.size.toFloat() / region.width.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        score += pointDensity * 25f
        parts.add("pts=${points.size}")

        // Extracted-column coverage from the curve extractor (0-30 pts).
        val extractionCoverage = curve.coverage.coerceIn(0f, 1f)
        score += extractionCoverage * 30f
        parts.add("extract=${(extractionCoverage * 100).roundToInt()}%")

        // X coverage: what fraction of graph width is spanned (0-25 pts).
        if (points.size >= 2) {
            val xRange = (points.maxOf { it.pixelX } - points.minOf { it.pixelX }).toFloat()
            val coverage = (xRange / region.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val coveragePts = coverage * 25f
            score += coveragePts
            parts.add("cov=${(coverage * 100).roundToInt()}%")
        }

        // Continuity: percentage of columns that have a point (0-20 pts).
        if (points.isNotEmpty() && region.width > 0) {
            val columnsWithData = points.map { it.pixelX }.distinct().size
            val continuity = columnsWithData.toFloat() / region.width.coerceAtLeast(1)
            val contPts = continuity.coerceIn(0f, 1f) * 20f
            score += contPts
            parts.add("cont=${(continuity * 100).roundToInt()}%")
        }

        if (points.isNotEmpty()) {
            val highConfidenceRatio = points.count { it.confidence >= CurvePoint.HIGH_CONFIDENCE }.toFloat() / points.size
            score += highConfidenceRatio.coerceIn(0f, 1f) * 10f
            parts.add("hi=${(highConfidenceRatio * 100).roundToInt()}%")

            val ySpan = points.maxOf { it.pixelY } - points.minOf { it.pixelY }
            val ySpanRatio = (ySpan / region.height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            score += (ySpanRatio / 0.15f).coerceIn(0f, 1f) * 10f
            parts.add("yspan=${(ySpanRatio * 100).roundToInt()}%")
        }

        // Interpolation penalty: high interpolation means gaps (0 to -20 pts).
        val interpolated = curve.interpolatedColumns
        if (points.isNotEmpty() && interpolated > 0) {
            val interpolationRatio = interpolated.toFloat() / (points.size + interpolated).coerceAtLeast(1)
            val penalty = interpolationRatio * 20f
            score -= penalty
            parts.add("interp=-${penalty.roundToInt()}")
        }

        if (curve.outlierCount > 0) {
            val outlierRatio = curve.outlierCount.toFloat() / (points.size + curve.outlierCount).coerceAtLeast(1)
            val penalty = outlierRatio.coerceIn(0f, 1f) * 15f
            score -= penalty
            parts.add("out=-${penalty.roundToInt()}")
        }

        if (curve.warnings.isNotEmpty()) {
            val penalty = curve.warnings.size * 3f
            score -= penalty
            parts.add("warn=-${penalty.roundToInt()}")
        }

        if (!curve.isUsable) {
            score *= 0.25f
            parts.add("usable=false")
        }

        return score.coerceAtLeast(0f) to "curve[${parts.joinToString(",")}]"
    }

    /**
     * Step regularity: 1.0 = perfectly evenly spaced.
     */
    private fun stepRegularityScore(values: List<Float>): Float {
        if (values.size < 3) return if (values.size == 2) 0.5f else 0f
        val sorted = values.sorted()
        val diffs = (1 until sorted.size).map { sorted[it] - sorted[it - 1] }
        if (diffs.isEmpty()) return 0f
        val sortedDiffs = diffs.sorted()
        val median = sortedDiffs[sortedDiffs.size / 2]
        if (median <= 0f) return 0f
        val consistent = diffs.count { abs(it - median) < median * 0.15f }
        return consistent.toFloat() / diffs.size
    }

    private fun curveInputPath(
        originalPath: String,
        prep: PreprocessingResult?,
        variant: SweepInputVariant,
    ): String = when (variant) {
        SweepInputVariant.ORIGINAL -> originalPath
        SweepInputVariant.GRAYSCALE -> prep?.grayscalePath ?: originalPath
        SweepInputVariant.CONTRAST -> prep?.contrastEnhancedPath ?: originalPath
        SweepInputVariant.SHARPENED -> prep?.sharpenedPath ?: prep?.contrastEnhancedPath ?: originalPath
        SweepInputVariant.BINARY -> prep?.binaryPath ?: originalPath
        SweepInputVariant.SCAN_STYLE -> prep?.scanStylePath ?: prep?.morphologyPath ?: originalPath
    }

    private fun fallbackAxes(): AxesResult = AxesResult(
        xAxis = null, yAxis = null,
        origin = null,
        detectionMethod = com.chromalab.feature.processing.pipeline.DetectionMethod.AUTO,
        confidence = 0f,
        timestamp = System.currentTimeMillis(),
    )

    private fun selectGraphResult(
        cv: GraphRegionResult?,
        vlm: GraphRegionResult?,
    ): GraphRegionResult? {
        if (cv == null) return vlm
        if (vlm == null) return cv

        val cvCount = cv.filteredRegions.size
        val vlmCount = vlm.filteredRegions.size
        val shouldUseVlmSplit = vlmCount > cvCount && cv.confidence != DetectionConfidence.HIGH
        return if (shouldUseVlmSplit) {
            println("SWEEP[MULTI] Using VLM split: cv=$cvCount, vlm=$vlmCount")
            vlm
        } else {
            cv
        }
    }

    private fun buildVlmGraphResult(
        bounds: com.chromalab.feature.processing.inference.GraphBounds,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult {
        val left = (bounds.leftPct / 100f * imageWidth).toInt().coerceIn(0, imageWidth)
        val top = (bounds.topPct / 100f * imageHeight).toInt().coerceIn(0, imageHeight)
        val right = (bounds.rightPct / 100f * imageWidth).toInt().coerceIn(left + 1, imageWidth)
        val bottom = (bounds.bottomPct / 100f * imageHeight).toInt().coerceIn(top + 1, imageHeight)
        val count = bounds.numGraphs.coerceIn(1, 6)
        val fullWidth = (right - left).coerceAtLeast(1)
        val fullHeight = (bottom - top).coerceAtLeast(1)

        val regions = if (count == 1) {
            listOf(GraphRegion(left, top, fullWidth, fullHeight, "VLM detected"))
        } else {
            (0 until count).map { index ->
                val y0 = top + (fullHeight * index / count)
                val y1 = top + (fullHeight * (index + 1) / count)
                GraphRegion(
                    x = left,
                    y = y0,
                    width = fullWidth,
                    height = (y1 - y0).coerceAtLeast(1),
                    label = "Graph ${index + 1}",
                )
            }
        }

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = if (count > 1) DetectionConfidence.MEDIUM else DetectionConfidence.LOW,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("Graph regions are based on VLM layout fallback; verify boundaries if results look wrong."),
            timestamp = System.currentTimeMillis(),
        )
    }
}
