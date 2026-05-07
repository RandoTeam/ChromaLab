package com.chromalab.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chromalab.core.data.model.CalculationType
import com.chromalab.core.data.model.ResultStatus

@Entity(
    tableName = "calculations",
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
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sampleId: Long,
    val type: CalculationType,
    val formula: String? = null,
    val result: Double? = null,
    val units: String? = null,
    val tolerance: Double? = null,
    val status: ResultStatus,
    val algorithmConfig: String? = null,
    val createdAt: Long,
)
