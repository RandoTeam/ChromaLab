package com.chromalab.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for ChromaLab.
 */
sealed interface Route {

    // --- Bottom Nav Tabs ---
    @Serializable data object ProjectList : Route
    @Serializable data object Capture : Route
    @Serializable data object Calculations : Route
    @Serializable data object More : Route

    // --- Projects ---
    @Serializable data class ProjectDetail(val projectId: Long) : Route
    @Serializable data class SampleDetail(val projectId: Long, val sampleId: Long) : Route
    @Serializable data object NewProject : Route

    // --- Capture ---
    @Serializable data object Camera : Route
    @Serializable data object GalleryFrame : Route
    @Serializable data object FileImport : Route
    @Serializable data class Processing(val imageUri: String) : Route

    // --- Calculations ---
    @Serializable data class ChromatogramView(val chromatogramId: Long) : Route
    @Serializable data object IonRatio : Route
    @Serializable data object Calibration : Route

    // --- More ---
    @Serializable data object Reports : Route
    @Serializable data object Settings : Route
    @Serializable data object Language : Route
    @Serializable data object About : Route
}
