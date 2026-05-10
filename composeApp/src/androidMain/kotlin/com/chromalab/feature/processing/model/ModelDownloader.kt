package com.chromalab.feature.processing.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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
) {
    val fraction: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    val speedMbPerSec: Float
        get() = speedBytesPerSec / (1024f * 1024f)
    val percentInt: Int
        get() = (fraction * 100).toInt()
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
        onProgress: suspend (DownloadProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        targetDir.mkdirs()

        val totalSize = model.totalSizeBytes
        var cumulativeDownloaded = 0L

        for ((index, file) in model.files.withIndex()) {
            val targetFile = File(targetDir, file.fileName)
            val tempFile = File(targetDir, "${file.fileName}.download")

            println("MODEL[DL] Downloading ${index + 1}/${model.files.size}: ${file.fileName}")

            downloadSingleFile(
                url = file.downloadUrl,
                targetFile = targetFile,
                tempFile = tempFile,
                expectedSize = file.sizeBytes,
            ) { bytesThisFile, speedBps ->
                onProgress(
                    DownloadProgress(
                        bytesDownloaded = cumulativeDownloaded + bytesThisFile,
                        totalBytes = totalSize,
                        speedBytesPerSec = speedBps,
                        fileIndex = index,
                        totalFiles = model.files.size,
                    )
                )
            }

            cumulativeDownloaded += targetFile.length()
            println("MODEL[DL] Completed: ${file.fileName} (${targetFile.length()} bytes)")
        }

        println("MODEL[DL] All files downloaded for: ${model.displayName}")
    }

    /**
     * Download a single file with resume support.
     */
    private suspend fun downloadSingleFile(
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
                        downloadSingleFile(redirectUrl, targetFile, tempFile, expectedSize, onFileProgress)
                        return
                    }
                    throw RuntimeException("Redirect without Location header: $responseCode")
                }
                else -> {
                    throw RuntimeException("HTTP error $responseCode for $url")
                }
            }

            val inputStream = connection.inputStream
            val outputStream = tempFile.outputStream().let { fos ->
                if (downloadedBytes > 0 && responseCode == 206) {
                    // Append mode for resume
                    java.io.FileOutputStream(tempFile, true)
                } else {
                    // Fresh start
                    downloadedBytes = 0L
                    fos
                }
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
            tempFile.renameTo(targetFile)

            // Final progress
            onFileProgress(downloadedBytes, 0L)

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Cancel is handled by coroutine cancellation — no explicit cancel needed.
     * The download loop checks ensureActive() on each buffer read.
     */
}
