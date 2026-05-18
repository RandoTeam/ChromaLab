package com.chromalab.feature.processing.flow

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisionAnalysisGuardTest {
    @Test
    fun visionRequirementErrorsCannotBeSkipped() {
        assertTrue(
            blocksFullAnalysisSkip(
                "AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first.",
            ),
        )
        assertTrue(blocksFullAnalysisSkip("Vision projector is missing for GGUF image input."))
        assertFalse(blocksFullAnalysisSkip("AI graph detection failed: deterministic ROI rescue will continue."))
        assertTrue(blocksFullAnalysisSkip("No deterministic graph ROI candidate passed geometry checks."))
        assertTrue(blocksFullAnalysisSkip("Axis calibration requires at least two X tick labels before signal conversion."))
        assertTrue(blocksFullAnalysisSkip("Axis calibration is required before building a signal preview."))
    }

    @Test
    fun ordinaryProcessingErrorsCanStillBeSkipped() {
        assertFalse(blocksFullAnalysisSkip("Image quality warning: low contrast."))
        assertFalse(blocksFullAnalysisSkip("Peak integration warning: baseline confidence is low."))
    }
}
