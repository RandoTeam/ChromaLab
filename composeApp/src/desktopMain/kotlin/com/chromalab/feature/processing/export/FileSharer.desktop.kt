package com.chromalab.feature.processing.export

actual object FileSharer {
    actual fun share(filePath: String, mimeType: String) {
        // Desktop: stub — no share sheet available
        println("FileSharer.share() — desktop stub: $filePath ($mimeType)")
    }
}
