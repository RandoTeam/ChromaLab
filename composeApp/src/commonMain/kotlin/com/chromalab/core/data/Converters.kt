package com.chromalab.core.data

import androidx.room.TypeConverter
import com.chromalab.core.data.model.*

class Converters {
    // SourceType
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    // IntegrationStatus
    @TypeConverter
    fun fromIntegrationStatus(value: IntegrationStatus): String = value.name

    @TypeConverter
    fun toIntegrationStatus(value: String): IntegrationStatus = IntegrationStatus.valueOf(value)

    // CalculationType
    @TypeConverter
    fun fromCalculationType(value: CalculationType): String = value.name

    @TypeConverter
    fun toCalculationType(value: String): CalculationType = CalculationType.valueOf(value)

    // ResultStatus
    @TypeConverter
    fun fromResultStatus(value: ResultStatus): String = value.name

    @TypeConverter
    fun toResultStatus(value: String): ResultStatus = ResultStatus.valueOf(value)

    // AuditAction
    @TypeConverter
    fun fromAuditAction(value: AuditAction): String = value.name

    @TypeConverter
    fun toAuditAction(value: String): AuditAction = AuditAction.valueOf(value)
}
