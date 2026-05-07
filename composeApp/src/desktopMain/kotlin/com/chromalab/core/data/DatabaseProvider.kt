package com.chromalab.core.data

/**
 * Desktop stub for DatabaseProvider.
 * Room is not used on desktop yet — throws on access.
 */
actual object DatabaseProvider {
    actual fun getDatabase(): ChromaLabDatabase {
        error("Room database is not available on desktop target yet")
    }
}
