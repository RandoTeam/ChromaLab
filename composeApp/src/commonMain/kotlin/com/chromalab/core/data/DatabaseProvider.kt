package com.chromalab.core.data

/**
 * Platform-specific database provider.
 * Returns a singleton ChromaLabDatabase instance.
 */
expect object DatabaseProvider {
    fun getDatabase(): ChromaLabDatabase
}
