package com.chromalab.feature.processing.calibration

import kotlinx.serialization.Serializable

/**
 * A calibration point: pixel position → real-world value.
 */
@Serializable
data class CalibrationPoint(
    val pixelPos: Float,
    val realValue: Float,
)

/**
 * Linear calibration: maps pixel position to real-world value.
 * realValue = scale * pixelPos + offset
 */
@Serializable
data class LinearCalibration(
    val point1: CalibrationPoint,
    val point2: CalibrationPoint,
) {
    val scale: Float
        get() {
            val dPixel = point2.pixelPos - point1.pixelPos
            if (dPixel == 0f) return 0f
            return (point2.realValue - point1.realValue) / dPixel
        }

    val offset: Float
        get() = point1.realValue - scale * point1.pixelPos

    /** Convert pixel coordinate to real-world value */
    fun pixelToReal(pixel: Float): Float = scale * pixel + offset

    /** Convert real-world value to pixel coordinate */
    fun realToPixel(real: Float): Float {
        if (scale == 0f) return 0f
        return (real - offset) / scale
    }

    /** Whether the calibration is valid (non-zero span, correct direction) */
    val isValid: Boolean
        get() = point1.pixelPos != point2.pixelPos &&
            point1.realValue != point2.realValue
}

/**
 * X axis calibration result.
 * Converts pixelX → time (minutes).
 */
@Serializable
data class XAxisCalibration(
    val calibration: LinearCalibration,
    val unit: String = "мин",
    val timestamp: Long,
) {
    val warnings: List<String>
        get() {
            val w = mutableListOf<String>()
            if (!calibration.isValid) {
                w.add("Калибровка недействительна — точки совпадают")
            }
            if (calibration.point2.pixelPos <= calibration.point1.pixelPos) {
                w.add("Вторая точка должна быть правее первой")
            }
            if (calibration.point2.realValue <= calibration.point1.realValue) {
                w.add("Значение времени должно возрастать слева направо")
            }
            return w
        }

    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

/**
 * Y axis calibration result.
 * Converts pixelY → intensity.
 * Note: pixelY grows DOWN, intensity grows UP.
 */
@Serializable
data class YAxisCalibration(
    val calibration: LinearCalibration,
    val unit: String = "mAU",
    val timestamp: Long,
) {
    val warnings: List<String>
        get() {
            val w = mutableListOf<String>()
            if (!calibration.isValid) {
                w.add("Калибровка недействительна — точки совпадают")
            }
            // For Y axis: pixel goes down but intensity goes up → scale should be negative
            if (calibration.scale > 0) {
                w.add("Проверьте направление оси Y: интенсивность должна расти вверх")
            }
            return w
        }

    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}
