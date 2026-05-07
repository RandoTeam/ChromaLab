package com.chromalab.core.data.model

/**
 * Source type of chromatogram data.
 */
enum class SourceType {
    PHOTO,
    GALLERY,
    PDF,
    CSV,
    MZML,
    MANUAL,
}

/**
 * Integration method used for a peak.
 */
enum class IntegrationStatus {
    AUTO,
    MANUAL,
    EDITED,
}

/**
 * Type of calculation performed.
 */
enum class CalculationType {
    ION_RATIO,
    QUANTITATIVE,
    QUALITATIVE,
    QC,
}

/**
 * Status of a calculation result.
 */
enum class ResultStatus {
    CONFIRMED,
    DOUBTFUL,
    NOT_CONFIRMED,
    PENDING,
}

/**
 * Audit log action type.
 */
enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
}
