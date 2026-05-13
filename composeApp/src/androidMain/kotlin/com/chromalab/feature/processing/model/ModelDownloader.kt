package com.chromalab.feature.processing.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/**
 * Progress info for an ongoing download.
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val fileIndex: Int,       // which file in the model (0-based)
    val totalFiles: Int,
    val currentFileName: String = "",
    val phase: DownloadPhase = DownloadPhase.DOWNLOADING,
    val error: String? = null,
) {
    val fraction: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    val speedMbPerSec: Float
        get() = speedBytesPerSec / (1024f * 1024f)
    val percentInt: Int
        get() = (fraction * 100).toInt()
}

enum class DownloadPhase {
    CONNECTING,
    DOWNLOADING,
    VALIDATING,
    COMPLETE,
    ERROR,
}

/**
 * Downloads model files from HuggingFace CDN.
 * Supports:
 *  - Resumable downloads (Range header)
 *  - Progress callbacks with speed
 *  - Multi-file models (base + mmproj)
 *  - Cancellation via coroutine cancellation
 */
class ModelDownloader {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MIN_PARALLEL_FILE_SIZE_BYTES = 16L * 1024L * 1024L
        private val SUPPORTED_PARALLELISM = intArrayOf(1, 2, 4, 8, 10, 12, 16)
    }

    /**
     * Download all files for a model.
     *
     * @param model the model to download
     * @param targetDir directory to save files into
     * @param onProgress callback with download progress
     * @throws CancellationException if cancelled
     * @throws Exception on network/IO errors
     */
    suspend fun downloadModel(
        model: ModelInfo,
        targetDir: File,
        parallelism: Int = 1,
        onProgress: suspend (DownloadProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        targetDir.mkdirs()

        val totalSize = model.totalSizeBytes
        var cumulativeDownloaded = 0L

        for ((index, file) in model.files.withIndex()) {
            val targetFile = File(targetDir, file.fileName)
            val tempFile = File(targetDir, "${file.fileName}.download")

            println("MODEL[DL] Downloading ${index + 1}/${model.files.size}: ${file.fileName}")

            // Report connecting phase
            onProgress(
                DownloadProgress(
                    bytesDownloaded = cumulativeDownloaded,
                    totalBytes = totalSize,
                    speedBytesPerSec = 0L,
                    fileIndex = index,
                    totalFiles = model.files.size,
                    currentFileName = file.fileName,
                    phase = DownloadPhase.CONNECTING,
                )
            )

            downloadSingleFile(
                url = file.downloadUrl,
                targetFile = targetFile,
                tempFile = tempFile,
                expectedSize = file.sizeBytes,
                parallelism = parallelism,
            ) { bytesThisFile, speedBps ->
                onProgress(
                    DownloadProgress(
                        bytesDownloaded = cumulativeDownloaded + bytesThisFile,
                        totalBytes = totalSize,
                        speedBytesPerSec = speedBps,
                        fileIndex = index,
                        totalFiles = model.files.size,
                        currentFileName = file.fileName,
                        phase = DownloadPhase.DOWNLOADING,
                    )
                )
            }

            // Validate downloaded file
            onProgress(
                DownloadProgress(
                    bytesDownloaded = cumulativeDownloaded + targetFile.length(),
                    totalBytes = totalSize,
                    speedBytesPerSec = 0L,
                    fileIndex = index,
                    totalFiles = model.files.size,
                    currentFileName = file.fileName,
                    phase = DownloadPhase.VALIDATING,
                )
            )

            val validationError = validateDownloadedFile(targetFile, file)
            if (validationError != null) {
                println("MODEL[DL] Validation FAILED: ${file.fileName}: $validationError")
                // Clean up corrupt file
                targetFile.delete()
                tempFile.delete()
                onProgress(
                    DownloadProgress(
                        bytesDownloaded = cumulativeDownloaded,
                        totalBytes = totalSize,
                        speedBytesPerSec = 0L,
                        fileIndex = index,
                        totalFiles = model.files.size,
                        currentFileName = file.fileName,
                        phase = DownloadPhase.ERROR,
                        error = validationError,
                    )
                )
                throw RuntimeException("Файл ${file.fileName}: $validationError")
            }

            cumulativeDownloaded += targetFile.length()
            println("MODEL[DL] Completed + validated: ${file.fileName} (${targetFile.length()} bytes)")
        }

        // Final complete callback
        onProgress(
            DownloadProgress(
                bytesDownloaded = totalSize,
                totalBytes = totalSize,
                speedBytesPerSec = 0L,
                fileIndex = model.files.size - 1,
                totalFiles = model.files.size,
                phase = DownloadPhase.COMPLETE,
            )
        )

        println("MODEL[DL] All files downloaded and validated for: ${model.displayName}")
    }

    /**
     * Download a single file with resume support.
     */
    private suspend fun downloadSingleFile(
        url: String,
        targetFile: File,
        tempFile: File,
        expectedSize: Long,
        parallelism: Int,
        onFileProgress: suspend (bytesDownloaded: Long, speedBps: Long) -> Unit,
    ) {
        if (targetFile.exists() && targetFile.length() >= expectedSize * 0.9) {
            println("MODEL[DL] File already exists, skipping: ${targetFile.name}")
            onFileProgress(targetFile.length(), 0L)
            return
        }

        val rangeParallelism = normalizeParallelism(parallelism)
        if (rangeParallelism > 1 && expectedSize >= MIN_PARALLEL_FILE_SIZE_BYTES) {
            val completed = downloadParallelFileIfSupported(
                url = url,
                targetFile = targetFile,
                tempFile = tempFile,
                expectedSize = expectedSize,
                parallelism = rangeParallelism,
                onFileProgress = onFileProgress,
            )
            if (completed) return
        }

        downloadSequentialFile(url, targetFile, tempFile, expectedSize, onFileProgress)
    }

    /**
     * Download a single file with sequential resume support.
     */
    private suspend fun downloadSequentialFile(
        url: String,
        targetFile: File,
        tempFile: File,
        expectedSize: Long,
        onFileProgress: suspend (bytesDownloaded: Long, speedBps: Long) -> Unit,
    ) {
        // Check existing temp file for resume
        var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        // If target already exists and looks complete, skip
        if (targetFile.exists() && targetFile.length() >= expectedSize * 0.9) {
            println("MODEL[DL] File already exists, skipping: ${targetFile.name}")
            onFileProgress(targetFile.length(), 0L)
            return
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "ChromaLab/1.0")
            setRequestProperty("Accept-Encoding", "identity")

            // Resume from where we left off
            if (downloadedBytes > 0) {
                setRequestProperty("Range", "bytes=$downloadedBytes-")
                println("MODEL[DL] Resuming from byte $downloadedBytes")
            }
        }

        try {
            connection.connect()
            val responseCode = connection.responseCode

            // Handle response codes
            when {
                responseCode == 200 -> {
                    // Fresh download — reset
                    downloadedBytes = 0L
                }
                responseCode == 206 -> {
                    // Partial content — resume OK
                }
                responseCode == 416 -> {
                    // Range not satisfiable — file is complete
                    if (tempFile.exists()) {
                        tempFile.renameTo(targetFile)
                    }
                    return
                }
                responseCode in 300..399 -> {
                    // Redirect — follow it
                    val redirectUrl = connection.getHeaderField("Location")
                    if (redirectUrl != null) {
                        connection.disconnect()
                        downloadSequentialFile(
                            resolveRedirectUrl(url, redirectUrl),
                            targetFile,
                            tempFile,
                            expectedSize,
                            onFileProgress,
                        )
                        return
                    }
                    throw RuntimeException("Redirect without Location header: $responseCode")
                }
                else -> {
                    throw RuntimeException("HTTP error $responseCode for $url")
                }
            }

            val inputStream = connection.inputStream
            val outputStream = if (downloadedBytes > 0 && responseCode == 206) {
                java.io.FileOutputStream(tempFile, true)
            } else {
                downloadedBytes = 0L
                java.io.FileOutputStream(tempFile, false)
            }

            val buffer = ByteArray(BUFFER_SIZE)
            var lastProgressTime = System.currentTimeMillis()
            var speedBytes = 0L
            var speedStartTime = System.currentTimeMillis()

            try {
                while (true) {
                    coroutineContext.ensureActive() // Check cancellation

                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    speedBytes += bytesRead

                    // Report progress periodically
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= PROGRESS_INTERVAL_MS) {
                        val elapsed = (now - speedStartTime).coerceAtLeast(1)
                        val bps = speedBytes * 1000L / elapsed
                        onFileProgress(downloadedBytes, bps)
                        lastProgressTime = now

                        // Reset speed measurement every 2 seconds
                        if (elapsed > 2000) {
                            speedBytes = 0
                            speedStartTime = now
                        }
                    }
                }
            } finally {
                outputStream.close()
                inputStream.close()
            }

            // Rename temp to final
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                throw RuntimeException("Could not finalize downloaded file: ${targetFile.name}")
            }

            // Final progress
            onFileProgress(downloadedBytes, 0L)

        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadParallelFileIfSupported(
        url: String,
        targetFile: File,
        tempFile: File,
        expectedSize: Long,
        parallelism: Int,
        onFileProgress: suspend (bytesDownloaded: Long, speedBps: Long) -> Unit,
    ): Boolean = coroutineScope {
        if (!supportsRangeRequests(url)) {
            println("MODEL[DL] Server does not support range chunks, using sequential download")
            return@coroutineScope false
        }

        println("MODEL[DL] Downloading ${targetFile.name} with $parallelism HTTP range chunks")
        tempFile.delete()
        RandomAccessFile(tempFile, "rw").use { it.setLength(expectedSize) }

        val downloadedBytes = AtomicLong(0L)
        var lastReportedBytes = 0L
        var lastReportTime = System.currentTimeMillis()

        val reporter = launch {
            while (isActive) {
                delay(PROGRESS_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val current = downloadedBytes.get().coerceAtMost(expectedSize)
                val elapsed = (now - lastReportTime).coerceAtLeast(1)
                val speedBps = (current - lastReportedBytes).coerceAtLeast(0L) * 1000L / elapsed
                onFileProgress(current, speedBps)
                lastReportedBytes = current
                lastReportTime = now
            }
        }

        try {
            buildRanges(expectedSize, parallelism)
                .map { range ->
                    async(Dispatchers.IO) {
                        downloadRangeChunk(url, tempFile, range, downloadedBytes)
                    }
                }
                .awaitAll()
        } finally {
            reporter.cancelAndJoin()
        }

        if (downloadedBytes.get() != expectedSize) {
            throw RuntimeException(
                "Incomplete range download: ${downloadedBytes.get()} of $expectedSize bytes",
            )
        }

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            throw RuntimeException("Could not finalize downloaded file: ${targetFile.name}")
        }

        onFileProgress(expectedSize, 0L)
        true
    }

    private suspend fun downloadRangeChunk(
        url: String,
        tempFile: File,
        range: ByteRange,
        downloadedBytes: AtomicLong,
    ) {
        var requestUrl = url
        var redirects = 0

        while (true) {
            coroutineContext.ensureActive()
            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "ChromaLab/1.0")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Range", "bytes=${range.start}-${range.endInclusive}")
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode in 300..399) {
                    val redirectUrl = connection.getHeaderField("Location")
                        ?: throw RuntimeException("Redirect without Location header: $responseCode")
                    redirects += 1
                    if (redirects > 5) {
                        throw RuntimeException("Too many redirects for ${tempFile.name}")
                    }
                    requestUrl = resolveRedirectUrl(requestUrl, redirectUrl)
                    continue
                }

                if (responseCode != 206) {
                    throw RuntimeException(
                        "HTTP error $responseCode for range ${range.start}-${range.endInclusive}",
                    )
                }

                connection.inputStream.use { input ->
                    RandomAccessFile(tempFile, "rw").use { output ->
                        output.seek(range.start)
                        val buffer = ByteArray(BUFFER_SIZE)
                        var remaining = range.length

                        while (remaining > 0) {
                            coroutineContext.ensureActive()
                            val bytesToRead = minOf(buffer.size.toLong(), remaining).toInt()
                            val bytesRead = input.read(buffer, 0, bytesToRead)
                            if (bytesRead == -1) {
                                throw RuntimeException(
                                    "Unexpected end of stream for range ${range.start}-${range.endInclusive}",
                                )
                            }
                            output.write(buffer, 0, bytesRead)
                            remaining -= bytesRead
                            downloadedBytes.addAndGet(bytesRead.toLong())
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun supportsRangeRequests(url: String, redirects: Int = 0): Boolean {
        if (redirects > 5) return false
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "ChromaLab/1.0")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Range", "bytes=0-0")
        }

        return try {
            connection.connect()
            when (val responseCode = connection.responseCode) {
                206 -> true
                in 300..399 -> {
                    val redirectUrl = connection.getHeaderField("Location") ?: return false
                    supportsRangeRequests(resolveRedirectUrl(url, redirectUrl), redirects + 1)
                }
                else -> {
                    println("MODEL[DL] Range probe returned HTTP $responseCode")
                    false
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRanges(totalBytes: Long, parallelism: Int): List<ByteRange> {
        val chunkCount = minOf(parallelism.toLong(), totalBytes.coerceAtLeast(1L)).toInt()
        val baseSize = totalBytes / chunkCount
        val remainder = totalBytes % chunkCount
        var start = 0L

        return (0 until chunkCount).map { index ->
            val size = baseSize + if (index < remainder) 1L else 0L
            val end = start + size - 1
            ByteRange(start = start, endInclusive = end).also {
                start = end + 1
            }
        }
    }

    private fun normalizeParallelism(value: Int): Int =
        SUPPORTED_PARALLELISM.minBy { kotlin.math.abs(it - value) }

    private fun resolveRedirectUrl(baseUrl: String, redirectUrl: String): String =
        URL(URL(baseUrl), redirectUrl).toString()

    private data class ByteRange(
        val start: Long,
        val endInclusive: Long,
    ) {
        val length: Long get() = endInclusive - start + 1
    }

    /**
     * Validate a downloaded file for integrity.
     * Returns error message or null if valid.
     */
    private fun validateDownloadedFile(file: File, modelFile: ModelFile): String? {
        if (!file.exists()) {
            return "Файл не найден после загрузки"
        }

        // Size check (allow 5% tolerance for HF size estimates)
        val minExpected = (modelFile.sizeBytes * 0.95).toLong()
        if (file.length() < minExpected) {
            val actualMb = file.length() / (1024 * 1024)
            val expectedMb = modelFile.sizeBytes / (1024 * 1024)
            return "Неполная загрузка: ${actualMb} MB из ${expectedMb} MB"
        }

        // GGUF magic bytes check
        if (file.name.endsWith(".gguf")) {
            try {
                java.io.RandomAccessFile(file, "r").use { raf ->
                    val buf = java.nio.ByteBuffer.allocate(4)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    raf.channel.read(buf)
                    buf.flip()
                    val magic = buf.int
                    if (magic != 0x46554747) { // "GGUF"
                        return "Повреждённый файл: неверные magic bytes (не GGUF)"
                    }
                }
            } catch (e: Exception) {
                return "Ошибка проверки: ${e.message}"
            }
        }

        // LiteRT bundle check — non-fatal (format may vary)
        if (file.name.endsWith(".litertlm")) {
            try {
                java.io.RandomAccessFile(file, "r").use { raf ->
                    val b1 = raf.read()
                    val b2 = raf.read()
                    if (b1 != 0x50 || b2 != 0x4B) { // PK (zip)
                        // Not a zip, but may still be valid LiteRT-LM
                        println("MODEL[DL] Warning: ${file.name} is not ZIP-based (magic=0x${"%02X%02X".format(b1, b2)})")
                    }
                }
            } catch (e: Exception) {
                println("MODEL[DL] Warning: LiteRT check failed: ${e.message}")
            }
        }

        return null // valid
    }

    /**
     * Cancel is handled by coroutine cancellation — no explicit cancel needed.
     * The download loop checks ensureActive() on each buffer read.
     */
}
