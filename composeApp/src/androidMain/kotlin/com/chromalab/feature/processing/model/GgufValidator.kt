package com.chromalab.feature.processing.model

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * GGUF file format validator.
 * Reads header magic, version, and key metadata to validate model files.
 *
 * GGUF spec: https://github.com/ggerganov/ggml/blob/master/docs/gguf.md
 *
 * Magic bytes: 0x46554747 ("GGUF" in little-endian)
 * Header layout:
 *   [4 bytes] magic
 *   [4 bytes] version (uint32)
 *   [8 bytes] tensor_count (uint64)
 *   [8 bytes] metadata_kv_count (uint64)
 *   [N×...] metadata key-value pairs
 */
object GgufValidator {

    /** GGUF magic bytes: "GGUF" = 0x47 0x47 0x55 0x46 (LE) */
    private const val GGUF_MAGIC = 0x46554747

    /** Minimum valid file size (header at minimum ~24 bytes) */
    private const val MIN_FILE_SIZE = 24L

    /**
     * Validation result for a GGUF file.
     */
    data class GgufInfo(
        val isValid: Boolean,
        val version: Int = 0,
        val tensorCount: Long = 0,
        val metadataCount: Long = 0,
        val architecture: String? = null,
        val quantization: String? = null,
        val hasVisionEncoder: Boolean = false,
        val fileSizeBytes: Long = 0,
        val error: String? = null,
    ) {
        /** Human-readable summary. */
        val summary: String
            get() = when {
                !isValid -> "Невалидный файл: ${error ?: "неизвестная ошибка"}"
                else -> buildString {
                    append("GGUF v$version")
                    architecture?.let { append(" · $it") }
                    quantization?.let { append(" · $it") }
                    append(" · ${tensorCount} tensors")
                    if (hasVisionEncoder) append(" · 👁 vision")
                }
            }
    }

    /**
     * Validate a GGUF file and extract key metadata.
     *
     * @param file the GGUF file to validate
     * @return GgufInfo with validation results
     */
    fun validate(file: File): GgufInfo {
        if (!file.exists()) {
            return GgufInfo(isValid = false, error = "Файл не найден")
        }
        if (file.length() < MIN_FILE_SIZE) {
            return GgufInfo(isValid = false, error = "Файл слишком мал (${file.length()} bytes)")
        }

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)

                // Read header
                raf.channel.read(buf)
                buf.flip()

                val magic = buf.int
                if (magic != GGUF_MAGIC) {
                    return GgufInfo(
                        isValid = false,
                        error = "Не GGUF файл (magic: 0x${Integer.toHexString(magic)})",
                    )
                }

                val version = buf.int
                if (version < 2 || version > 4) {
                    return GgufInfo(
                        isValid = false,
                        error = "Неподдерживаемая версия GGUF: v$version (нужна v2-v4)",
                    )
                }

                val tensorCount = buf.long
                val metadataCount = buf.long

                // Try to read metadata keys for architecture info
                val metadata = readMetadataKeys(raf, metadataCount.toInt().coerceAtMost(100))

                val architecture = metadata["general.architecture"]
                val quantization = metadata["general.quantization_version"]
                    ?: guessQuantFromFilename(file.name)

                // Detect vision encoder
                val hasVision = detectVisionEncoder(file.name, metadata)

                GgufInfo(
                    isValid = true,
                    version = version,
                    tensorCount = tensorCount,
                    metadataCount = metadataCount,
                    architecture = architecture,
                    quantization = quantization,
                    hasVisionEncoder = hasVision,
                    fileSizeBytes = file.length(),
                )
            }
        } catch (e: Exception) {
            GgufInfo(isValid = false, error = "Ошибка чтения: ${e.message}")
        }
    }

    /**
     * Quick check: is this file a valid GGUF? (Just magic bytes, fast.)
     */
    fun isGguf(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                raf.channel.read(buf)
                buf.flip()
                buf.int == GGUF_MAGIC
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a file is a valid LiteRT-LM bundle.
     * LiteRT-LM files are zip-based containers with a specific structure.
     * We check for zip magic bytes (PK).
     */
    fun isLiteRTBundle(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        if (!file.name.endsWith(".litertlm")) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val b1 = raf.read()
                val b2 = raf.read()
                // ZIP magic: PK (0x50 0x4B)
                b1 == 0x50 && b2 == 0x4B
            }
        } catch (e: Exception) {
            // Even without zip check, trust the extension
            true
        }
    }

    /**
     * Attempt to match an imported file to a known builtin model family.
     * Returns family ID or null.
     */
    fun matchFamily(fileName: String, architecture: String?): String? {
        val lower = fileName.lowercase()
        return when {
            lower.contains("qwen") && lower.contains("vl") -> "qwen3.5-vl"
            lower.contains("gemma") -> "gemma"
            lower.contains("llava") -> "llava"
            lower.contains("internvl") -> "internvl"
            lower.contains("phi") && lower.contains("vision") -> "phi-vision"
            architecture == "qwen2vl" || architecture == "qwen2_vl" -> "qwen3.5-vl"
            architecture == "gemma" || architecture == "gemma2" -> "gemma"
            architecture == "llava" -> "llava"
            else -> null
        }
    }

    // ===== Internal =====

    /**
     * Read metadata key-value pairs from GGUF header.
     * GGUF KV format:
     *   [string key] [uint32 value_type] [value...]
     *
     * We only read string-type values for the keys we care about.
     * This is best-effort — stops on any parse error.
     */
    private fun readMetadataKeys(raf: RandomAccessFile, count: Int): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val keysOfInterest = setOf(
            "general.architecture",
            "general.quantization_version",
            "general.name",
            "general.file_type",
        )

        try {
            for (i in 0 until count) {
                // Read key string
                val key = readGgufString(raf) ?: break

                // Read value type (uint32)
                val typeBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                if (raf.channel.read(typeBuf) != 4) break
                typeBuf.flip()
                val valueType = typeBuf.int

                // We only care about string values (type 8) for our keys
                if (valueType == 8 && key in keysOfInterest) {
                    val value = readGgufString(raf)
                    if (value != null) result[key] = value
                } else {
                    // Skip value we don't need
                    if (!skipGgufValue(raf, valueType)) break
                }

                // Stop early if we found everything
                if (result.size >= keysOfInterest.size) break
            }
        } catch (_: Exception) {
            // Best-effort — return what we got
        }

        return result
    }

    /** Read a GGUF string: [uint64 length][bytes]. */
    private fun readGgufString(raf: RandomAccessFile): String? {
        val lenBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        if (raf.channel.read(lenBuf) != 8) return null
        lenBuf.flip()
        val len = lenBuf.long
        if (len <= 0 || len > 1024) return null // sanity check

        val strBuf = ByteArray(len.toInt())
        if (raf.read(strBuf) != len.toInt()) return null
        return String(strBuf, Charsets.UTF_8)
    }

    /** Skip a GGUF value based on type. Returns false if cannot skip. */
    private fun skipGgufValue(raf: RandomAccessFile, type: Int): Boolean {
        val fixedSizes = mapOf(
            0 to 1,   // UINT8
            1 to 1,   // INT8
            2 to 2,   // UINT16
            3 to 2,   // INT16
            4 to 4,   // UINT32
            5 to 4,   // INT32
            6 to 4,   // FLOAT32
            7 to 1,   // BOOL
            10 to 8,  // UINT64
            11 to 8,  // INT64
            12 to 8,  // FLOAT64
        )

        val fixedSize = fixedSizes[type]
        if (fixedSize != null) {
            raf.skipBytes(fixedSize)
            return true
        }

        when (type) {
            8 -> { // STRING
                readGgufString(raf) ?: return false
                return true
            }
            9 -> { // ARRAY
                val headerBuf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                if (raf.channel.read(headerBuf) != 12) return false
                headerBuf.flip()
                val elemType = headerBuf.int
                val elemCount = headerBuf.long
                val elemFixed = fixedSizes[elemType]
                if (elemFixed != null) {
                    raf.skipBytes((elemFixed * elemCount).toInt())
                    return true
                }
                // Complex array — skip element by element
                for (j in 0 until elemCount.toInt().coerceAtMost(1000)) {
                    if (!skipGgufValue(raf, elemType)) return false
                }
                return true
            }
            else -> return false
        }
    }

    /** Detect vision encoder from filename or metadata. */
    private fun detectVisionEncoder(fileName: String, metadata: Map<String, String>): Boolean {
        val lower = fileName.lowercase()
        if (lower.contains("mmproj")) return true
        if (lower.contains("vision")) return true
        if (lower.contains("-vl")) return true

        val arch = metadata["general.architecture"]?.lowercase() ?: ""
        return arch.contains("clip") || arch.contains("vl") || arch.contains("vision")
    }

    /** Guess quantization from filename (e.g. "Q4_K_M", "Q8_0"). */
    private fun guessQuantFromFilename(fileName: String): String? {
        val quantPattern = Regex("""[Qq](\d+)_([A-Za-z]+)(_[A-Za-z]+)?""")
        return quantPattern.find(fileName)?.value?.uppercase()
    }
}
