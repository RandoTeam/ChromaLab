package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chromalab.core.data.model.SourceType

@Entity(
    tableName = "chromatograms",
    foreignKeys = [
        ForeignKey(
            entity = SampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sampleId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sampleId")],
)
data class ChromatogramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sampleId: Long,
    val sourceType: SourceType,
    val filePath: String? = null,
    val ionChannel: String? = null,
    val timeRangeStart: Double? = null,
    val timeRangeEnd: Double? = null,
    val intensityUnit: String? = null,
    val qualityScore: Float? = null,
    val dataPoints: String? = null,
    val algorithmConfig: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
