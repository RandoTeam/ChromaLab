package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    @Test
    fun fastVlmLiteRtBundleIsRegisteredAsVisionModel() {
        val model = assertNotNull(ModelRegistry.findById("fastvlm-05b-litert"))

        assertEquals(ModelRuntime.LITERT_LM, model.runtime)
        assertTrue(model.supportsVision)
        assertTrue(ModelRegistry.isChromatogramVisionModel(model))
        assertEquals(1, model.files.size)
        assertEquals("FastVLM-0.5B.litertlm", model.files.single().fileName)
        assertEquals(ModelFileType.LITERT_BUNDLE, model.files.single().type)
        assertEquals(
            "https://huggingface.co/litert-community/FastVLM-0.5B/resolve/main/FastVLM-0.5B.litertlm",
            model.files.single().downloadUrl,
        )
        assertEquals(1_156_349_952L, model.files.single().sizeBytes)
    }

    @Test
    fun qwen35LiteRtBundleUsesMultimodalVisionFile() {
        val model = assertNotNull(ModelRegistry.findById("qwen35-08b-litert-vlm"))

        assertEquals(ModelRuntime.LITERT_LM, model.runtime)
        assertTrue(model.supportsVision)
        assertTrue(ModelRegistry.isChromatogramVisionModel(model))
        assertEquals(1, model.files.size)
        assertEquals("qwen35_mm_q8_ekv2048.litertlm", model.files.single().fileName)
        assertEquals(ModelFileType.LITERT_BUNDLE, model.files.single().type)
        assertEquals(
            "https://huggingface.co/GabrieleConte/Qwen3.5-0.8B-LiteRT/resolve/main/qwen35_mm_q8_ekv2048.litertlm",
            model.files.single().downloadUrl,
        )
        assertEquals(1_159_757_824L, model.files.single().sizeBytes)
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
