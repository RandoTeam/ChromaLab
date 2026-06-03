package com.chromalab.feature.processing.rust

object RustCvBridge {
    private val loadResult: Result<Unit> = runCatching {
        System.loadLibrary("chromalab_cv_core")
    }

    fun probeJson(): String {
        loadResult.getOrThrow()
        return nativeProbeJson()
    }

    fun loadError(): Throwable? = loadResult.exceptionOrNull()

    private external fun nativeProbeJson(): String
}
