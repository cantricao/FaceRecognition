package com.example.detectionexample.glance.utils

import androidx.compose.runtime.Composable
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import com.example.detectionexample.MainActivity

/**
 * Utility functions for creating Glance [Action]s.
 */
object ActionUtils {
    @Composable
    fun actionStartMainActivity(): Action =
        actionStartActivity<MainActivity>()
}
