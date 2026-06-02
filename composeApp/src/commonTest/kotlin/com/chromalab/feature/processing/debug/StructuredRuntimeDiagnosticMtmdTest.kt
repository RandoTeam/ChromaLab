package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.inference.GgufMtmdChunkDiagnostic
import com.chromalab.feature.processing.inference.GgufMtmdCropOcrProbe
import com.chromalab.feature.processing.inference.GgufMtmdDiagnosticsMarkdownRenderer
import com.chromalab.feature.processing.inference.GgufMtmdDiagnosticsSummary
import com.chromalab.feature.processing.inference.GgufMtmdFitStatus
import com.chromalab.feature.processing.inference.GgufMtmdModelFileDiagnostic
import com.chromalab.feature.processing.inference.GgufMtmdNativeProbeResult
import com.chromalab.feature.processing.inference.GgufMtmdOcrAdvisoryPolicy
import com.chromalab.feature.processing.inference.GgufMtmdOcrResearchGate
import com.chromalab.feature.processing.inference.GgufMtmdResearchGateDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuredRuntimeDiagnosticMtmdTest {
    @Test
    fun mtmdDiagnosticsMapToTechnicalEvidenceWithoutPrivatePaths() {
        val summary = sampleSummary()

        val diagnostic = StructuredRuntimeDiagnosticMapper.fromGgufMtmdDiagnostics(summary)

        assertEquals(RuntimeDiagnosticSource.MTMD_MMPROJ, diagnostic.source)
        assertEquals(RuntimeModelPathClass.APP_PRIVATE_MODEL, diagnostic.modelPathClass)
        assertEquals("DISABLED_FOR_MULTIMODAL", diagnostic.mtpEnabled)
        assertFalse(StructuredRuntimeDiagnosticMapper.containsPrivatePathLeak(diagnostic))
    }

    @Test
    fun advisoryPolicyRejectsForbiddenNumericAuthorityFields() {
        val policy = GgufMtmdOcrAdvisoryPolicy.evaluate(
            """{"text":"Ion 71", "rt": 12.4, "area": 99.1, "x_pixel": 340}""",
        )

        assertTrue(policy.advisoryOnly)
        assertTrue(policy.forbiddenNumericFieldDetected)
        assertTrue("rt" in policy.forbiddenNumericFields)
        assertTrue("area" in policy.forbiddenNumericFields)
        assertTrue("x_pixel" in policy.forbiddenNumericFields)
    }

    @Test
    fun markdownExportsImageTokenFitAndResearchGate() {
        val markdown = GgufMtmdDiagnosticsMarkdownRenderer.render(sampleSummary())

        assertTrue("Image tokens" in markdown)
        assertTrue("FITS_CONTEXT" in markdown)
        assertTrue("DeepSeekOCR 2" !in markdown || "deepseek-ocr-2" in markdown)
        assertTrue("OCR/VLM cannot create graph pixels" in markdown)
    }

    private fun sampleSummary(): GgufMtmdDiagnosticsSummary =
        GgufMtmdDiagnosticsSummary(
            runId = "mtmd-test",
            generatedAtEpochMillis = 1L,
            modelId = "deepseek-ocr-q80",
            modelFamily = "deepseek-ocr",
            backend = "llama.cpp CPU",
            contextTokens = 4096,
            batchTokens = 256,
            imagePathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL.name,
            promptChars = 42,
            loadAttempted = true,
            loadResult = "loaded",
            loadTimeMillis = 1000,
            baseModel = GgufMtmdModelFileDiagnostic(
                role = "base",
                fileName = "DeepSeek-OCR-Q8_0.gguf",
                exists = true,
                sizeBytes = 3_126_139_712L,
                pathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL.name,
            ),
            mmproj = GgufMtmdModelFileDiagnostic(
                role = "mmproj",
                fileName = "mmproj-DeepSeek-OCR-Q8_0.gguf",
                exists = true,
                sizeBytes = 447_856_768L,
                pathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL.name,
            ),
            nativeProbe = GgufMtmdNativeProbeResult(
                available = true,
                supportsVision = true,
                promptChars = 80,
                mediaMarkerCount = 1,
                chunkCount = 2,
                imageChunkCount = 1,
                textChunkCount = 1,
                imageTokenCount = 576,
                textTokenCount = 24,
                totalTokenCount = 600,
                totalPositionCount = 600,
                contextTokens = 4096,
                batchTokens = 256,
                fitStatus = GgufMtmdFitStatus.FITS_CONTEXT,
                bitmapLoadMillis = 12,
                tokenizeMillis = 100,
                chunks = listOf(
                    GgufMtmdChunkDiagnostic(0, "text", 24, 24),
                    GgufMtmdChunkDiagnostic(1, "image", 576, 576, "image0"),
                ),
            ),
            cropOcrProbe = GgufMtmdCropOcrProbe(
                attempted = true,
                latencyMillis = 1500,
                outputChars = 16,
            ),
            ocrResearchGate = GgufMtmdOcrResearchGate(
                modelId = "deepseek-ocr-2",
                modelAvailable = false,
                mmprojAvailable = false,
                expectedBaseFileName = "deepseek-ocr-2-Q4_K_M.gguf",
                expectedMmprojFileName = "mmproj-deepseek-ocr-2-q8_0.gguf",
                compatibility = "research only",
                decision = GgufMtmdResearchGateDecision.RESEARCH_ONLY,
                safetyBoundaries = listOf(
                    "OCR/VLM cannot create graph pixels, calibration coefficients, RT, height, area, FWHM, S/N, baseline, or Kovats values.",
                ),
            ),
            gateDecision = GgufMtmdResearchGateDecision.ADVISORY_DIAGNOSTICS_ALLOWED,
            gateReasons = listOf("diagnostics_only"),
        )
}
