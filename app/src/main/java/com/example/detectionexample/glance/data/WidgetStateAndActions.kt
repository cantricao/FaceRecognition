package com.example.detectionexample.glance.data

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.detectionexample.glance.ImageTextListAppWidget

/**
 * Mutates widget state in DataStore and immediately triggers widget recomposition.
 */
suspend fun pushWidgetRevisionUpdate(context: Context) {
    val widget = ImageTextListAppWidget()
    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ImageTextListAppWidget::class.java)
    for (glanceId in glanceIds) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ImageTextListAppWidget.KEY_REVISION] =
                (prefs[ImageTextListAppWidget.KEY_REVISION] ?: 0) + 1
        }
        widget.update(context, glanceId)
    }
}

/**
 * Background worker that performs periodic or on-demand widget sync and pushes revision updates.
 */
class SyncWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        pushWidgetRevisionUpdate(applicationContext)
        return Result.success()
    }
}

/**
 * Action callback with a public zero-argument constructor for ProGuard / R8 compatibility.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[KEY_ITEM_ID] ?: "default"
        val request = OneTimeWorkRequestBuilder<SyncWidgetWorker>()
            .setInputData(workDataOf("item_id" to itemId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_sync",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        val KEY_ITEM_ID = ActionParameters.Key<String>("item_id")
    }
}
