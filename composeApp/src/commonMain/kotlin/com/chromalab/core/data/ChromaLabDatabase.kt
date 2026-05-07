package com.chromalab.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.chromalab.core.data.dao.*
import com.chromalab.core.data.entity.*
import com.chromalab.feature.calculation.data.*

@Database(
    entities = [
        ProjectEntity::class,
        SampleEntity::class,
        ChromatogramEntity::class,
        PeakEntity::class,
        CalculationEntity::class,
        AuditEntity::class,
        // Phase 2 calculation
        CalculationRunEntity::class,
        CalculationPeakEntity::class,
        BaselineResultEntity::class,
        NoiseRegionEntity::class,
        ManualEditEntity::class,
        ExportRecordEntity::class,
    ],
    version = 2,
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
    // Phase 2 calculation
    abstract fun calculationRunDao(): CalculationRunDao
    abstract fun calculationPeakDao(): CalculationPeakDao
    abstract fun baselineResultDao(): BaselineResultDao
    abstract fun noiseRegionDao(): NoiseRegionDao
    abstract fun manualEditDao(): ManualEditDao
    abstract fun exportRecordDao(): ExportRecordDao
}

// Room KMP generates this via KSP
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ChromaLabDatabaseConstructor : RoomDatabaseConstructor<ChromaLabDatabase>
