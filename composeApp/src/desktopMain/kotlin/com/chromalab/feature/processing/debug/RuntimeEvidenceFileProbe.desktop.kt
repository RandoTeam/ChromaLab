package com.chromalab.feature.processing.debug

import java.io.File

actual object RuntimeEvidenceFileProbe {
    actual fun exists(path: String): Boolean = File(path).isFile
}
