package com.example.detectionexample.wear.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.wear.AssociateWithGlanceWearWidget
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteText
import com.example.detectionexample.wear.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val ACTION_REFRESH_WIDGET = "com.example.detectionexample.wear.ACTION_REFRESH_WIDGET"

/**
 * Service that exposes FaceRecognitionWearWidget to the Wear OS system.
 * Annotated with @AssociateWithGlanceWearWidget for static provider resolution.
 */
@AssociateWithGlanceWearWidget(FaceRecognitionWearWidget::class)
class FaceRecognitionWearWidgetService : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = FaceRecognitionWearWidget()
}

/**
 * Wear OS Widget providing quick biometric status and detection controls
 * built with Remote Compose and GlanceWearWidget.
 */
class FaceRecognitionWearWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData {
        val isLarge = params.containerType == ContainerInfo.CONTAINER_TYPE_LARGE

        return WearWidgetDocument(
            background = WearWidgetBrush.color(Color(0xFF141218).rc)
        ) {
            FaceRecognitionWidgetContent(isLarge = isLarge)
        }
    }
}

/**
 * Remote Compose layout evaluated directly inside the watch system host process.
 */
@RemoteComposable
@Composable
fun FaceRecognitionWidgetContent(
    isLarge: Boolean,
) {
    val enrolledCount = rememberMutableRemoteInt(4)

    val launchAppAction = pendingIntentAction { ctx ->
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            ctx,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    val refreshAction = pendingIntentAction { ctx ->
        val intent = Intent(ctx, FaceRecognitionWidgetReceiver::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        PendingIntent.getBroadcast(
            ctx,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    if (isLarge) {
        LargeWidgetLayout(
            enrolledCount = enrolledCount,
            onLaunchApp = launchAppAction,
            onRefresh = refreshAction,
        )
    } else {
        SmallWidgetLayout(
            enrolledCount = enrolledCount,
            onLaunchApp = launchAppAction,
        )
    }
}

@RemoteComposable
@Composable
private fun SmallWidgetLayout(
    enrolledCount: MutableRemoteInt,
    onLaunchApp: Action,
) {
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxSize()
            .padding(6.rdp),
        verticalArrangement = RemoteArrangement.Center,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteText(
            text = "Face Recon".rs,
            color = Color(0xFFE6E1E5).rc,
        )
        RemoteText(
            text = "Active • ".rs + enrolledCount.toRemoteString() + " faces".rs,
            color = Color(0xFFD0BCFF).rc,
            modifier = RemoteModifier.padding(top = 2.rdp),
        )
        RemoteRow(
            modifier = RemoteModifier.padding(top = 4.rdp),
            horizontalArrangement = RemoteArrangement.spacedBy(6.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteButton(onClick = valueChange(enrolledCount, enrolledCount + 1)) {
                RemoteText(text = "+".rs)
            }
            RemoteButton(onClick = onLaunchApp) {
                RemoteText(text = "Open".rs)
            }
        }
    }
}

@RemoteComposable
@Composable
private fun LargeWidgetLayout(
    enrolledCount: MutableRemoteInt,
    onLaunchApp: Action,
    onRefresh: Action,
) {
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxSize()
            .padding(10.rdp),
        verticalArrangement = RemoteArrangement.Center,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        // Header
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalArrangement = RemoteArrangement.SpaceBetween,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteText(
                text = "Face Recognition".rs,
                color = Color(0xFFE6E1E5).rc,
            )
            RemoteText(
                text = "Live • 98%".rs,
                color = Color(0xFF80D99C).rc,
            )
        }

        // Subtitle with host-evaluated RemoteInt
        RemoteText(
            text = "Identities: ".rs + enrolledCount.toRemoteString() + " verified in real-time".rs,
            color = Color(0xFFCAC4D0).rc,
            modifier = RemoteModifier.padding(top = 2.rdp),
        )

        // Enrolled person initial chips
        RemoteRow(
            modifier = RemoteModifier.padding(top = 6.rdp),
            horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            val chips = listOf("AS", "BJ", "CB")
            for (chip in chips) {
                RemoteBox(
                    modifier = RemoteModifier
                        .size(32.rdp)
                        .background(Color(0xFF4F378B).rc)
                        .clickable(onLaunchApp),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(
                        text = chip.rs,
                        color = Color.White.rc,
                    )
                }
            }
        }

        // Action controls
        RemoteRow(
            modifier = RemoteModifier.padding(top = 8.rdp),
            horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteButton(onClick = valueChange(enrolledCount, enrolledCount + 1)) {
                RemoteText(text = "+ Add Face".rs)
            }
            RemoteButton(onClick = onLaunchApp) {
                RemoteText(text = "Open App".rs)
            }
        }
    }
}

/**
 * BroadcastReceiver to trigger updates for all active widget instances.
 */
class FaceRecognitionWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                FaceRecognitionWearWidget().triggerUpdateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
