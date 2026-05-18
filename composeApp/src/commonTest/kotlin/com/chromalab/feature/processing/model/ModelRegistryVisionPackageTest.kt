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
    fun strictChromatogramVisionRejectsGgufWithoutMmprojAndOcrOnlyFamilies() {
        val baseOnlyQwen = ggufModel(
            family = "qwen3-vl",
            files = listOf(ModelFile("qwen.gguf", 100L, ModelFileType.GGUF_BASE, "")),
        )
        val ocrOnly = ggufModel(
            family = "paddleocr-vl",
            files = listOf(
                ModelFile("paddle.gguf", 100L, ModelFileType.GGUF_BASE, ""),
                ModelFile("mmproj-paddle.gguf", 10L, ModelFileType.GGUF_MMPROJ, ""),
            ),
        )

        assertFalse(ModelRegistry.isChromatogramVisionModel(baseOnlyQwen))
        assertFalse(ModelRegistry.isChromatogramVisionModel(ocrOnly))
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

    @Test
    fun gemma4E4BUsesCurrentLiteRtBundleSize() {
        val model = assertNotNull(ModelRegistry.findById("gemma4-e4b"))

        assertEquals(ModelRuntime.LITERT_LM, model.runtime)
        assertEquals("gemma-4-E4B-it.litertlm", model.files.single().fileName)
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            model.files.single().downloadUrl,
        )
        assertEquals(3_659_530_240L, model.files.single().sizeBytes)
    }

    @Test
    fun qwen35MtpModelsAreTextOnlyChatEntries() {
        val fourB = assertNotNull(ModelRegistry.findById("qwen35-mtp-4b-q4km"))
        val nineB = assertNotNull(ModelRegistry.findById("qwen35-mtp-9b-ud-q4kxl"))
        val fourBGroup = assertNotNull(
            ModelRegistry.groupsForRuntime(ModelRuntime.LLAMA_CPP).firstOrNull { it.groupId == "qwen35-mtp-4b" },
        )
        val nineBGroup = assertNotNull(
            ModelRegistry.groupsForRuntime(ModelRuntime.LLAMA_CPP).firstOrNull { it.groupId == "qwen35-mtp-9b" },
        )

        listOf(fourB, nineB).forEach { model ->
            assertEquals(ModelRuntime.LLAMA_CPP, model.runtime)
            assertTrue(ModelRegistry.isChatModel(model))
            assertFalse(model.supportsVision)
            assertFalse(ModelRegistry.isChromatogramVisionModel(model))
            assertEquals(1, model.files.size)
            assertEquals(ModelFileType.GGUF_BASE, model.files.single().type)
            assertTrue(model.supportsMtp)
        }

        assertEquals(21, fourBGroup.variants.size)
        assertEquals(21, nineBGroup.variants.size)
        assertNotNull(ModelRegistry.findById("qwen35-mtp-4b-bf16"))
        assertNotNull(ModelRegistry.findById("qwen35-mtp-9b-bf16"))

        assertEquals(
            "https://huggingface.co/unsloth/Qwen3.5-4B-MTP-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf",
            fourB.files.single().downloadUrl,
        )
        assertEquals(2_834_975_040L, fourB.files.single().sizeBytes)

        assertEquals(
            "https://huggingface.co/unsloth/Qwen3.5-9B-MTP-GGUF/resolve/main/Qwen3.5-9B-UD-Q4_K_XL.gguf",
            nineB.files.single().downloadUrl,
        )
        assertEquals(6_135_034_208L, nineB.files.single().sizeBytes)
    }

    @Test
    fun gptOss20BIsTextOnlyChatTestModel() {
        val model = assertNotNull(ModelRegistry.findById("gpt-oss-20b-q4km"))

        assertEquals(ModelRuntime.LLAMA_CPP, model.runtime)
        assertTrue(ModelRegistry.isChatModel(model))
        assertFalse(model.supportsVision)
        assertFalse(ModelRegistry.isChromatogramVisionModel(model))
        assertEquals("gpt-oss-20b-Q4_K_M.gguf", model.files.single().fileName)
        assertEquals(
            "https://huggingface.co/unsloth/gpt-oss-20b-GGUF/resolve/main/gpt-oss-20b-Q4_K_M.gguf",
            model.files.single().downloadUrl,
        )
        assertEquals(11_624_759_488L, model.files.single().sizeBytes)
    }

    private fun ggufModel(
        files: List<ModelFile>,
        family: String = "custom",
    ) = ModelInfo(
        id = "custom",
        displayName = "Custom",
        family = family,
        runtime = ModelRuntime.LLAMA_CPP,
        files = files,
        minRamMb = 4096,
        isBuiltin = false,
        supportsVision = true,
    )
}
