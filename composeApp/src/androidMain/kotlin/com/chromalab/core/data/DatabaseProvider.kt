package com.chromalab.core.data

import android.app.Application
import androidx.room.Room

/**
 * Android-specific database provider.
 * Uses a singleton pattern with lazy initialization.
 */
actual object DatabaseProvider {

    @Volatile
    private var instance: ChromaLabDatabase? = null
    private var app: Application? = null

    /**
     * Must be called once from Application.onCreate() or Activity.
     */
    fun init(application: Application) {
        app = application
    }

    actual fun getDatabase(): ChromaLabDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase().also { instance = it }
        }
    }

    private fun buildDatabase(): ChromaLabDatabase {
        val context = requireNotNull(app) {
            "DatabaseProvider.init(app) must be called before getDatabase()"
        }
        return Room.databaseBuilder(
            context,
            ChromaLabDatabase::class.java,
            "chromalab.db",
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }
}
