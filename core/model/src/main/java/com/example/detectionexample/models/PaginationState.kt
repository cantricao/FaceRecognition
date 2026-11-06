package com.example.detectionexample.models

import kotlinx.serialization.Serializable

@Serializable
data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val hasMore: Boolean = false
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
