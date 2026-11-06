package com.example.detectionexample.navigation

import androidx.navigation3.runtime.NavKey
import com.example.detectionexample.R
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    val resourceId: Int

    @Serializable
    data object Camera : Screen {
        override val resourceId: Int = R.string.camera
    }

    @Serializable
    data object Video : Screen {
        override val resourceId: Int = R.string.video
    }

    @Serializable
    data object Chat : Screen {
        override val resourceId: Int = R.string.chat
    }
}
