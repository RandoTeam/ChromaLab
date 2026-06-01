package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelDownloadPreflightPolicyTest {
    @Test
    fun preflightAcceptsValidModelWhenStorageHasHeadroom() {
        val model = model(sizeBytes = 1_000L)

        val result = ModelDownloadPreflightPolicy.validateModelBeforeDownload(
            model = model,
            availableFreeBytes = ModelDownloadPreflightPolicy.requiredFreeBytes(model),
        )

        assertTrue(result.canProceed)
        assertEquals(1_000L + ModelDownloadPreflightPolicy.DOWNLOAD_HEADROOM_BYTES, result.requiredFreeBytes)
    }

    @Test
    fun preflightRejectsMissingUrlInvalidSizeAndInsufficientStorage() {
        val model = model(
            file = ModelFile(
                fileName = "bad.litertlm",
                sizeBytes = 0L,
                type = ModelFileType.LITERT_BUNDLE,
                downloadUrl = "",
            ),
        )

        val result = ModelDownloadPreflightPolicy.validateModelBeforeDownload(
            model = model,
            availableFreeBytes = 1L,
        )

        assertFalse(result.canProceed)
        assertTrue(result.issues.any { it.code == ModelDownloadPreflightIssueCode.MISSING_DOWNLOAD_URL })
        assertTrue(result.issues.any { it.code == ModelDownloadPreflightIssueCode.INVALID_EXPECTED_SIZE })
        assertTrue(result.issues.any { it.code == ModelDownloadPreflightIssueCode.INSUFFICIENT_STORAGE })
    }

    @Test
    fun observedRemoteSizeAllowsSmallHfHeaderVariance() {
        val file = ModelFile(
            fileName = "gemma.litertlm",
            sizeBytes = 2_588_147_712L,
            type = ModelFileType.LITERT_BUNDLE,
            downloadUrl = "https://huggingface.co/model/resolve/main/gemma.litertlm",
        )

        assertNull(ModelDownloadPreflightPolicy.validateObservedRemoteSize(file, 2_588_147_712L))
        assertNull(ModelDownloadPreflightPolicy.validateObservedRemoteSize(file, 2_588_147_712L + 1_000_000L))

        val mismatch = ModelDownloadPreflightPolicy.validateObservedRemoteSize(file, 2_000_000_000L)
        assertEquals(ModelDownloadPreflightIssueCode.OBSERVED_SIZE_MISMATCH, mismatch?.code)
    }

    @Test
    fun partialDownloadNameMatchesDownloaderTempName() {
        val file = ModelFile(
            fileName = "gemma.litertlm",
            sizeBytes = 100L,
            type = ModelFileType.LITERT_BUNDLE,
            downloadUrl = "https://example.com/gemma.litertlm",
        )

        assertEquals("gemma.litertlm.download", ModelDownloadPreflightPolicy.partialDownloadFileName(file))
    }

    private fun model(
        sizeBytes: Long = 100L,
        file: ModelFile = ModelFile(
            fileName = "model.litertlm",
            sizeBytes = sizeBytes,
            type = ModelFileType.LITERT_BUNDLE,
            downloadUrl = "https://example.com/model.litertlm",
        ),
    ) = ModelInfo(
        id = "test-model",
        displayName = "Test Model",
        family = "gemma-4",
        runtime = ModelRuntime.LITERT_LM,
        files = listOf(file),
        minRamMb = 4096,
        isBuiltin = true,
        supportsVision = true,
    )
}
