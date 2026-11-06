package com.example.detectionexample.feature.video.playback

import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility functions for Android for Cars (Android Automotive OS) integration.
 */
object CarUtils {

    const val FEATURE_DISPLAY_COMPATIBILITY = "android.software.car.display_compatibility"

    /**
     * Returns true if the device is running Android Automotive OS.
     */
    fun isAutomotive(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }

    /**
     * Returns true if the device supports display compatibility mode.
     */
    fun isDisplayCompatibility(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(FEATURE_DISPLAY_COMPATIBILITY)
    }

    /**
     * Determines whether explicit in-app back affordance must be displayed (AN-1).
     * On Android Automotive OS vehicles without display compatibility mode,
     * no system or hardware back affordance is guaranteed, so in-app back is required.
     * When compatibility mode is present and active, the system provides a back bar,
     * so duplicate in-app controls must be hidden.
     */
    fun shouldShowInAppBack(context: Context): Boolean {
        return if (isAutomotive(context)) {
            !isDisplayCompatibility(context)
        } else {
            false
        }
    }
}
