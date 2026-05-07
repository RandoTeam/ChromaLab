package com.chromalab.core.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// ChromaLab Color System
// Concept: "Lab Precision Dark" — научная чистота + комфорт
// Все пары (surface/onSurface) проверены на контраст ≥ 4.5:1
// ============================================================

// --- Brand ---
val Teal80 = Color(0xFF5EECC8)      // Primary — десатурированный бирюзовый
val Teal90 = Color(0xFFA0F4DD)      // Primary light variant
val Teal30 = Color(0xFF005140)      // Primary container dark
val Teal10 = Color(0xFF002118)      // Darkest teal

val Blue80 = Color(0xFF8AB4F8)      // Secondary — мягкий голубой
val Blue30 = Color(0xFF1B3A5C)      // Secondary container

val Amber80 = Color(0xFFFFCC80)     // Tertiary — мягкий янтарный
val Amber30 = Color(0xFF5C3D00)     // Tertiary container

// --- Neutrals (Dark theme) ---
val Neutral4 = Color(0xFF0E1117)     // Background — почти чёрный, но не #000
val Neutral6 = Color(0xFF141920)     // Surface — основные карточки
val Neutral10 = Color(0xFF1B2129)    // Surface variant — второстепенные панели
val Neutral17 = Color(0xFF252D38)    // Surface container high — elevated cards
val Neutral22 = Color(0xFF2E3744)    // Surface container highest — модальные
val Neutral30 = Color(0xFF3A4553)    // Outline — разделители
val Neutral40 = Color(0xFF4D5A6A)    // Outline variant

val Neutral95 = Color(0xFFF0F2F5)   // onBackground — основной текст (контраст 14:1)
val Neutral87 = Color(0xFFD0D5DD)   // onSurface — основной текст (контраст 10:1)
val Neutral70 = Color(0xFFA3ACB9)   // onSurfaceVariant — вторичный текст (контраст 6:1)
val Neutral50 = Color(0xFF6E7A8A)   // Disabled/hint — подсказки (контраст 3.5:1)

// --- Neutrals (Light theme) ---
val NeutralL99 = Color(0xFFFBFCFE)  // Background light
val NeutralL96 = Color(0xFFF1F4F8)  // Surface light
val NeutralL92 = Color(0xFFE3E7EE)  // Surface variant light
val NeutralL10 = Color(0xFF171C24)  // onBackground light (контраст 16:1)
val NeutralL20 = Color(0xFF262D38)  // onSurface light (контраст 12:1)
val NeutralL40 = Color(0xFF4D5768)  // onSurfaceVariant light

// --- Semantics ---
val Success = Color(0xFF81C784)     // Зелёный — CONFIRMED (десатурированный)
val Warning = Color(0xFFFFD54F)     // Жёлтый — DOUBTFUL
val Error = Color(0xFFE57373)       // Красный — NOT_CONFIRMED (десатурированный)
val Info = Color(0xFF64B5F6)        // Голубой — информация

val SuccessDark = Color(0xFF1B4332) // Success container
val WarningDark = Color(0xFF4A3800) // Warning container
val ErrorDark = Color(0xFF4A1C1C)   // Error container

// --- Chart Colors (фиксированные, на любом фоне) ---
val ChartLine1 = Color(0xFF8AB4F8)  // Ion channel 1 — голубой
val ChartLine2 = Color(0xFFFFCC80)  // Ion channel 2 — янтарный
val ChartLine3 = Color(0xFFCE93D8)  // Ion channel 3 — фиолетовый
val ChartLine4 = Color(0xFF80CBC4)  // Ion channel 4 — бирюзовый
val ChartLine5 = Color(0xFFF48FB1)  // Ion channel 5 — розовый
val ChartBaseline = Color(0xFFE57373) // Baseline — красный пунктир
val ChartPeakFill = Color(0x4D5EECC8) // Peak fill — primary с 30% alpha
val ChartPeakBorder = Color(0x995EECC8) // Peak border — primary с 60% alpha
val ChartSelected = Color(0xFFF0F2F5) // Выбранный пик — белый
