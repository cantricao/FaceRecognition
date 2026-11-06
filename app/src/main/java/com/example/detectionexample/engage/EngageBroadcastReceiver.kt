package com.example.detectionexample.engage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.engage.service.BroadcastReceiverPermissions
import com.google.android.engage.service.Intents

/**
 * Listens for AppEngageService publish triggers from Google Play.
 *
 * Implements:
 * - Dynamic registration via [EngageBroadcastReceiver.register].
 * - Static registration via AndroidManifest.xml.
 * - Single-cluster targeted publishing (only triggering the cluster corresponding to the received intent).
 */
class EngageBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        when (intent.action) {
            Intents.ACTION_PUBLISH_RECOMMENDATION ->
                EngagePublisher.publishOneTime(context, Constants.PUBLISH_TYPE_RECOMMENDATIONS)

            Intents.ACTION_PUBLISH_FEATURED ->
                EngagePublisher.publishOneTime(context, Constants.PUBLISH_TYPE_FEATURED)

            Intents.ACTION_PUBLISH_CONTINUATION ->
                EngagePublisher.publishOneTime(context, Constants.PUBLISH_TYPE_CONTINUATION)
        }
    }

    companion object {
        /**
         * Dynamically registers the receiver.
         * Required in addition to static registration in AndroidManifest.xml.
         * Must be called in Application's onCreate() or MainActivity's onCreate().
         */
        fun register(context: Context) {
            val appContext = context.applicationContext
            val receiver = EngageBroadcastReceiver()

            val filter = IntentFilter().apply {
                addAction(Intents.ACTION_PUBLISH_RECOMMENDATION)
                addAction(Intents.ACTION_PUBLISH_FEATURED)
                addAction(Intents.ACTION_PUBLISH_CONTINUATION)
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                BroadcastReceiverPermissions.BROADCAST_REQUEST_DATA_PUBLISH_PERMISSION,
                null,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }
}
