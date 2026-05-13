package com.chromalab.feature.processing.model

import kotlinx.coroutines.delay

/**
 * Shared token-bucket limiter for model downloads.
 *
 * One instance is shared by the foreground download service, so the configured limit applies to
 * all active model downloads and to every parallel range chunk together.
 */
class DownloadSpeedLimiter(
    private val limitBytesPerSecond: () -> Long,
) {
    private val lock = Any()
    private var windowStartedAtMs = System.currentTimeMillis()
    private var bytesInWindow = 0L

    suspend fun throttle(bytes: Int) {
        if (bytes <= 0) return

        while (true) {
            val delayMs = synchronized(lock) {
                val limit = limitBytesPerSecond()
                if (limit <= 0L) {
                    bytesInWindow = 0L
                    windowStartedAtMs = System.currentTimeMillis()
                    0L
                } else {
                    val now = System.currentTimeMillis()
                    val elapsed = now - windowStartedAtMs
                    if (elapsed >= WINDOW_MS) {
                        windowStartedAtMs = now
                        bytesInWindow = 0L
                    }

                    if (bytesInWindow + bytes <= limit) {
                        bytesInWindow += bytes
                        0L
                    } else {
                        (WINDOW_MS - (now - windowStartedAtMs)).coerceAtLeast(1L)
                    }
                }
            }

            if (delayMs <= 0L) return
            delay(delayMs)
        }
    }

    private companion object {
        private const val WINDOW_MS = 1_000L
    }
}
