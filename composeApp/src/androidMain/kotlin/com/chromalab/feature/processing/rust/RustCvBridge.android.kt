package com.chromalab.feature.processing.rust

object RustCvBridge {
    private val loadResult: Result<Unit> = runCatching {
        System.loadLibrary("chromalab_cv_core")
    }

    fun probeJson(): String {
        loadResult.getOrThrow()
        return nativeProbeJson()
    }

    fun planAxisElementCropsJson(
        imageWidth: Int,
        imageHeight: Int,
        axisElementGraphJson: String,
    ): String {
        loadResult.getOrThrow()
        return nativePlanAxisElementCropsJson(
            imageWidth,
            imageHeight,
            axisElementGraphJson,
        )
    }

    fun turboVecAppPrivateProbeJson(
        appPrivateRoot: String,
        cleanup: Boolean,
    ): String {
        loadResult.getOrThrow()
        return nativeTurboVecAppPrivateProbeJson(appPrivateRoot, cleanup)
    }

    fun turboVecKnowledgeIndexGateJson(
        appPrivateRoot: String,
        indexRelativePath: String,
        sidecarRelativePath: String,
        queryVectorsRelativePath: String,
        cleanup: Boolean,
    ): String {
        loadResult.getOrThrow()
        return nativeTurboVecKnowledgeIndexGateJson(
            appPrivateRoot,
            indexRelativePath,
            sidecarRelativePath,
            queryVectorsRelativePath,
            cleanup,
        )
    }

    fun loadError(): Throwable? = loadResult.exceptionOrNull()

    private external fun nativeProbeJson(): String

    private external fun nativePlanAxisElementCropsJson(
        imageWidth: Int,
        imageHeight: Int,
        axisElementGraphJson: String,
    ): String

    private external fun nativeTurboVecAppPrivateProbeJson(
        appPrivateRoot: String,
        cleanup: Boolean,
    ): String

    private external fun nativeTurboVecKnowledgeIndexGateJson(
        appPrivateRoot: String,
        indexRelativePath: String,
        sidecarRelativePath: String,
        queryVectorsRelativePath: String,
        cleanup: Boolean,
    ): String
}
