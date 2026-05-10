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


    @Serializable data object Camera : Route
    @Serializable data object FileImport : Route
    @Serializable data class Processing(val imageUri: String) : Route

    @Serializable data class Analysis(val signalId: String) : Route

    // --- More ---
    @Serializable data object Language : Route
    @Serializable data object About : Route
    @Serializable data object ModelManager : Route
}
