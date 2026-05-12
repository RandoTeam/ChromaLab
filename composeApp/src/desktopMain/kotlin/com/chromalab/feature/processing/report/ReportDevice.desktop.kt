package com.chromalab.feature.processing.report

actual fun currentReportDeviceName(): String? {
    val os = System.getProperty("os.name").orEmpty().trim()
    val arch = System.getProperty("os.arch").orEmpty().trim()
    return listOf(os, arch)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .takeIf { it.isNotBlank() }
}
