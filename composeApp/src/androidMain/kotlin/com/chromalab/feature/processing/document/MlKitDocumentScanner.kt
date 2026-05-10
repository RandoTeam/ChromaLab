package com.chromalab.feature.processing.document

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

/**
 * ML Kit Document Scanner wrapper.
 *
 * Provides a production-quality document scanning experience:
 * - Auto edge detection + crop
 * - Perspective correction (deskew)
 * - Shadow/stain removal
 * - Brightness/contrast enhancement
 *
 * Replaces our naive DocumentDetector + PerspectiveWarper with
 * Google's ML-powered solution (runs via Google Play Services, 0 MB in APK).
 */
object MlKitDocumentScanner {

    private var scanner: GmsDocumentScanner? = null

    fun getScanner(): GmsDocumentScanner {
        if (scanner == null) {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(1) // Chromatograms are single-page
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            scanner = GmsDocumentScanning.getClient(options)
        }
        return scanner!!
    }

    /**
     * Copy the scanned result from ML Kit's cache URI to our persistent storage.
     * ML Kit URIs are temporary and get cleaned up by the system.
     */
    fun copyResultToStorage(
        context: Context,
        result: GmsDocumentScanningResult,
        outputDir: String,
    ): String? {
        val pages = result.pages ?: return null
        if (pages.isEmpty()) return null

        val sourceUri = pages[0].imageUri
        val dir = File(outputDir).also { it.mkdirs() }
        val outFile = File(dir, "mlkit_scanned.jpg")

        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
