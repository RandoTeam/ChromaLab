package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelRegistryVisionPackageTest {
    @Test
    fun ggufVisionModelRequiresBaseAndMmprojFiles() {
        val complete = ggufModel(
            files = listOf(
                ModelFile("model.gguf", 100L, ModelFileType.GGUF_BASE, ""),
                ModelFile("mmproj.gguf", 10L, ModelFileType.GGUF_MMPROJ, ""),
            ),
        )
        val baseOnly = ggufModel(
            files = listOf(ModelFile("model.gguf", 100L, ModelFileType.GGUF_BASE, "")),
        )
        val mmprojOnly = ggufModel(
            files = listOf(ModelFile("mmproj.gguf", 10L, ModelFileType.GGUF_MMPROJ, "")),
        )

        assertTrue(ModelRegistry.hasGgufVisionFilePair(complete))
        assertFalse(ModelRegistry.hasGgufVisionFilePair(baseOnly))
        assertFalse(ModelRegistry.hasGgufVisionFilePair(mmprojOnly))
    }

    @Test
    fun liteRtBundleIsNotAGgufVisionPair() {
        val liteRt = ModelInfo(
            id = "litert",
            displayName = "LiteRT",
            family = "gemma",
            runtime = ModelRuntime.LITERT_LM,
            files = listOf(ModelFile("model.litertlm", 100L, ModelFileType.LITERT_BUNDLE, "")),
            minRamMb = 4096,
            isBuiltin = false,
            supportsVision = true,
        )

        assertFalse(ModelRegistry.hasGgufVisionFilePair(liteRt))
    }

    private fun ggufModel(files: List<ModelFile>) = ModelInfo(
        id = "custom",
        displayName = "Custom",
        family = "custom",
        runtime = ModelRuntime.LLAMA_CPP,
        files = files,
        minRamMb = 4096,
        isBuiltin = false,
        supportsVision = true,
    )
}
