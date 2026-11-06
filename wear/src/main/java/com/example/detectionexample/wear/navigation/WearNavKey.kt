package com.example.detectionexample.wear.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface WearNavKey : NavKey {
    @Serializable
    data object Home : WearNavKey

    @Serializable
    data object Identities : WearNavKey

    @Serializable
    data class Detail(val id: String, val name: String, val confidence: Float) : WearNavKey
}
