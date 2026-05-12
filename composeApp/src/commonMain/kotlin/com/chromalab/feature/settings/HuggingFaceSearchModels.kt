package com.chromalab.feature.settings

import com.chromalab.feature.processing.model.ModelInfo

enum class HuggingFaceSortOption(
    val label: String,
) {
    DOWNLOADS("Downloads"),
    LIKES("Likes"),
    UPDATED("Updated"),
}

data class HuggingFaceSearchResult(
    val repoId: String,
    val author: String,
    val displayName: String,
    val downloads: Long,
    val likes: Long,
    val lastModified: String,
    val tags: List<String>,
    val selectedFileName: String,
    val modelInfo: ModelInfo,
    val isCompatible: Boolean,
    val compatibilityLabel: String,
)

data class HuggingFaceSearchState(
    val query: String = "gguf chat",
    val sort: HuggingFaceSortOption = HuggingFaceSortOption.DOWNLOADS,
    val isSearching: Boolean = false,
    val results: List<HuggingFaceSearchResult> = emptyList(),
    val error: String? = null,
)
