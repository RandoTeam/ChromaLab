package com.chromalab.feature.processing.debug

expect object RuntimeEvidenceFileProbe {
    fun exists(path: String): Boolean
}
