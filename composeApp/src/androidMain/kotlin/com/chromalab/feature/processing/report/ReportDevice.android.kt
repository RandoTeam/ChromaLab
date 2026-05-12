package com.chromalab.feature.processing.report

import android.os.Build

actual fun currentReportDeviceName(): String? {
    val manufacturer = Build.MANUFACTURER.orEmpty().trim()
    val model = Build.MODEL.orEmpty().trim()
    val device = listOf(manufacturer, model)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { return null }
    return "$device (Android ${Build.VERSION.SDK_INT})"
}
