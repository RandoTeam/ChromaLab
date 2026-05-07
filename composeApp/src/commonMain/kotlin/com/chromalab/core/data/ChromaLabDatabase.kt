package com.chromalab.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.chromalab.core.data.dao.*
import com.chromalab.core.data.entity.*

@Database(
    entities = [
        ProjectEntity::class,
        SampleEntity::class,
        ChromatogramEntity::class,
        PeakEntity::class,
        CalculationEntity::class,
        AuditEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(ChromaLabDatabaseConstructor::class)
abstract class ChromaLabDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sampleDao(): SampleDao
    abstract fun chromatogramDao(): ChromatogramDao
    abstract fun peakDao(): PeakDao
    abstract fun calculationDao(): CalculationDao
    abstract fun auditDao(): AuditDao
}

// Room KMP generates this via KSP
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ChromaLabDatabaseConstructor : RoomDatabaseConstructor<ChromaLabDatabase>
