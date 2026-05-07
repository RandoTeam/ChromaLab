package com.chromalab.feature.processing.quality

import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurvePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QualityCalculatorTest {

    @Test
    fun imageQuality_goodImage() {
        val q = QualityCalculator.imageQuality(sharpness = 90f, contrast = 90f)
        assertEquals(QualityStatus.GOOD, q.status)
        assertTrue(q.warnings.isEmpty())
    }

    @Test
    fun imageQuality_lowSharpness() {
        val q = QualityCalculator.imageQuality(sharpness = 20f, contrast = 70f)
        assertTrue(q.warnings.any { "резкость" in it.lowercase() })
    }

    @Test
    fun imageQuality_lowContrast() {
        val q = QualityCalculator.imageQuality(sharpness = 80f, contrast = 30f)
        assertTrue(q.warnings.any { "контраст" in it.lowercase() })
    }

    @Test
    fun documentDetection_perfect() {
        val q = QualityCalculator.documentDetection(
            detected = true, perspectiveAngle = 5f,
            hasGlare = false, unevenLighting = false,
        )
        assertEquals(QualityStatus.GOOD, q.status)
    }

    @Test
    fun documentDetection_allProblems() {
        val q = QualityCalculator.documentDetection(
            detected = false, perspectiveAngle = 25f,
            hasGlare = true, unevenLighting = true,
        )
        assertTrue(q.status == QualityStatus.RISKY || q.status == QualityStatus.FAILED)
        assertTrue(q.warnings.size >= 3)
    }

    @Test
    fun graphDetection_auto() {
        val q = QualityCalculator.graphDetection(autoDetected = true, graphCount = 1)
        assertEquals(QualityStatus.GOOD, q.status)
    }

    @Test
    fun graphDetection_manual() {
        val q = QualityCalculator.graphDetection(autoDetected = false, graphCount = 1)
        assertTrue(q.warnings.any { "вручную" in it })
    }

    @Test
    fun axisCalibration_noCalibration() {
        val q = QualityCalculator.axisCalibration(
            xCalibrated = false, yCalibrated = false,
            ocrUsed = false, ocrCorrected = false, manualAxes = false,
        )
        assertTrue(q.score < 0.2f)
    }

    @Test
    fun curveExtraction_goodCoverage() {
        val result = CurveExtractionResult(
            points = (0 until 100).map { CurvePoint(it, 50f, 1f) },
            maskImagePath = null,
            totalColumns = 100,
            extractedColumns = 95,
            interpolatedColumns = 5,
            outlierCount = 2,
            warnings = emptyList(),
            timestamp = 0L,
        )
        val q = QualityCalculator.curveExtraction(result)
        assertEquals(QualityStatus.GOOD, q.status)
    }

    @Test
    fun overall_weightedAverage() {
        val image = StageQuality("image", 1f, emptyList())
        val doc = StageQuality("document", 1f, emptyList())
        val graph = StageQuality("graph", 1f, emptyList())
        val cal = StageQuality("calibration", 1f, emptyList())
        val curve = StageQuality("curve", 1f, emptyList())
        val overall = QualityCalculator.overall(image, doc, graph, cal, curve)
        assertTrue(overall.score > 0.95f, "All perfect should be ~1.0, got ${overall.score}")
    }

    @Test
    fun overall_curveWeightDominant() {
        val image = StageQuality("image", 1f, emptyList())
        val doc = StageQuality("document", 1f, emptyList())
        val graph = StageQuality("graph", 1f, emptyList())
        val cal = StageQuality("calibration", 1f, emptyList())
        val curve = StageQuality("curve", 0f, emptyList()) // Curve failed
        val overall = QualityCalculator.overall(image, doc, graph, cal, curve)
        // Curve weight is 40%, so overall should be ~0.60
        assertTrue(overall.score < 0.65f, "Bad curve should pull overall down, got ${overall.score}")
    }

    @Test
    fun qualityStatus_thresholds() {
        assertEquals(QualityStatus.GOOD, StageQuality("", 0.85f, emptyList()).status)
        assertEquals(QualityStatus.ACCEPTABLE, StageQuality("", 0.6f, emptyList()).status)
        assertEquals(QualityStatus.RISKY, StageQuality("", 0.3f, emptyList()).status)
        assertEquals(QualityStatus.FAILED, StageQuality("", 0.1f, emptyList()).status)
    }
}
