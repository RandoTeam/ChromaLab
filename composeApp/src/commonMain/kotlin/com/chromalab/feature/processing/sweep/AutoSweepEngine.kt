package com.chromalab.feature.processing.sweep

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.inference.ChartAnalysisReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.preprocess.ImagePreprocessor
import com.chromalab.feature.processing.preprocess.PreprocessingParams
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Auto-Sweep Engine: tests multiple preprocessing configurations
 * on a single image and selects the best one based on OCR + curve quality.
 *
 * Flow:
 * 1. Graph detection — run ONCE (same image → same result)
 * 2. OCR — run ONCE with multi-level scan (already handles label detection)
 * 3. For each preprocessing config:
 *    a. Preprocess image
 *    b. Extract curve from preprocessed result
 *    c. Score: curve point count + continuity + coverage + OCR quality
 * 4. Return results sorted by score
 */
class AutoSweepEngine {

    data class SweepConfig(
        val name: String,
        val params: PreprocessingParams,
        val description: String,
    )

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
     * 8 preset configurations covering the full range of image conditions.
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
        onProgress: (SweepProgress) -> Unit = {},
    ): List<SweepResult> {
        val preprocessor = ImagePreprocessor()
        val graphDetector = GraphRegionDetector()
        val ocrReader = ChartAnalysisReader()
        val axisDetector = AxisDetector()
        val curveMaskPreparer = CurveMaskPreparer()
        val curveExtractor = CurveExtractor()

        // === SHARED: run once ===
        val w = imageWidth.takeIf { it > 0 } ?: 1920
        val h = imageHeight.takeIf { it > 0 } ?: 1080

        // === Strategy A: VLM-first graph detection (always try) ===
        // VLM provides structural understanding of the image;
        // CV refines with pixel-level precision.
        var vlmBounds: com.chromalab.feature.processing.inference.GraphBounds? = null
        var vlmRegion: GraphRegion? = null
        try {
            onProgress(SweepProgress(0, configs.size, "VLM: определение графика", "vlm_region"))
            vlmBounds = ocrReader.detectGraphRegion(imagePath, w, h)
            if (vlmBounds != null) {
                // Convert percentage bounds to pixel coordinates
                val left = (vlmBounds.leftPct / 100f * w).toInt().coerceIn(0, w)
                val top = (vlmBounds.topPct / 100f * h).toInt().coerceIn(0, h)
                val right = (vlmBounds.rightPct / 100f * w).toInt().coerceIn(left + 1, w)
                val bottom = (vlmBounds.bottomPct / 100f * h).toInt().coerceIn(top + 1, h)
                vlmRegion = GraphRegion(
                    x = left, y = top,
                    width = right - left, height = bottom - top,
                    label = "VLM detected",
                )
                println("SWEEP[VLM] Graph region: ${vlmRegion.x},${vlmRegion.y} ${vlmRegion.width}x${vlmRegion.height} (${vlmBounds.numGraphs} graphs)")
            }
        } catch (e: Exception) {
            println("SWEEP[VLM] Graph detection failed: ${e.message}")
        }

        // CV graph detection — always run for precision refinement
        val graphRes = cachedGraphResult ?: run {
            onProgress(SweepProgress(0, configs.size, "CV: определение графика", "detect"))
            try {
                graphDetector.detect(imagePath, w, h)
            } catch (e: Exception) {
                println("SWEEP[GRAPH] CV detection failed: ${e.message}")
                null
            }
        }

        // Region selection priority: override > VLM > CV
        val region = overrideRegion ?: vlmRegion ?: graphRes?.selectedRegion
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
        println("SWEEP[OCR] x=${ocrResult?.suggestedXValues}, y=${ocrResult?.suggestedYValues}")

        onProgress(SweepProgress(0, configs.size, "Определение осей", "axes"))
        val axesRes = try {
            axisDetector.detect(imagePath, region)
        } catch (e: Exception) {
            println("SWEEP[AXES] failed: ${e.message}")
            null
        }

        // OCR base score (same for all configs)
        val ocrBaseScore = calculateOcrScore(ocrResult, graphRes, region)
        println("SWEEP[OCR] base score=${ocrBaseScore.first} (${ocrBaseScore.second})")

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
            val curveInputPath = prepResult?.contrastEnhancedPath ?: imagePath
            val curveResult = try {
                val axes = axesRes ?: fallbackAxes()
                val mask = curveMaskPreparer.prepare(curveInputPath, region, axes, configDir)
                val maskPath = mask.cleanMaskPath ?: mask.rawMaskPath ?: curveInputPath
                curveExtractor.extract(maskPath, region.width, region.height, configDir)
            } catch (e: Exception) {
                println("SWEEP[${config.name}] curve extraction failed: ${e.message}")
                null
            }

            // Score: OCR base + curve quality
            val curveScore = calculateCurveScore(curveResult, region)
            val totalScore = ocrBaseScore.first + curveScore.first
            val breakdown = "${ocrBaseScore.second}, ${curveScore.second}"
            println("SWEEP[${config.name}] total=$totalScore (ocr=${ocrBaseScore.first} + curve=${curveScore.first})")

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
     * Curve extraction quality score: point count, continuity, coverage.
     */
    private fun calculateCurveScore(
        curve: CurveExtractionResult?,
        region: GraphRegion,
    ): Pair<Float, String> {
        if (curve == null) return 0f to "curve[none]"

        val parts = mutableListOf<String>()
        var score = 0f
        val points = curve.points

        // Point count (1 pt per 10 points, max 50 pts)
        val pointScore = (points.size / 10f).coerceAtMost(50f)
        score += pointScore
        parts.add("pts=${points.size}")

        // Coverage: what fraction of X range is covered (0-30 pts)
        if (points.size >= 2) {
            val xRange = (points.maxOf { it.pixelX } - points.minOf { it.pixelX }).toFloat()
            val coverage = (xRange / region.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val coveragePts = coverage * 30f
            score += coveragePts
            parts.add("cov=${(coverage * 100).roundToInt()}%")
        }

        // Continuity: percentage of columns that have a point (0-20 pts)
        if (points.isNotEmpty() && region.width > 0) {
            val columnsWithData = points.map { it.pixelX }.distinct().size
            val continuity = columnsWithData.toFloat() / region.width.coerceAtLeast(1)
            val contPts = continuity.coerceIn(0f, 1f) * 20f
            score += contPts
            parts.add("cont=${(continuity * 100).roundToInt()}%")
        }

        // Interpolation penalty: high interpolation means gaps (0 to -10 pts)
        val interpolated = curve.interpolatedColumns
        if (points.isNotEmpty() && interpolated > 0) {
            val interpolationRatio = interpolated.toFloat() / (points.size + interpolated).coerceAtLeast(1)
            val penalty = interpolationRatio * 10f
            score -= penalty
            parts.add("interp=-${penalty.roundToInt()}")
        }

        return score to "curve[${parts.joinToString(",")}]"
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

    private fun fallbackAxes(): AxesResult = AxesResult(
        xAxis = null, yAxis = null,
        origin = null,
        detectionMethod = com.chromalab.feature.processing.pipeline.DetectionMethod.AUTO,
        confidence = 0f,
        timestamp = System.currentTimeMillis(),
    )
}
